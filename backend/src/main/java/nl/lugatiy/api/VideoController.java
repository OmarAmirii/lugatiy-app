package nl.lugatiy.api;

import java.io.*;
import java.nio.file.*;
import java.security.Principal;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/v1")
class VideoController {
 private final JdbcTemplate db; private final Path root;
 VideoController(JdbcTemplate db,@Value("${app.storage-path}") String storage){this.db=db;root=Path.of(storage).toAbsolutePath().normalize().resolve("videos");}
 record PublishInput(boolean published) {}
 record SeriesInput(@NotBlank @Size(max=200) String title,@Size(max=5000) String description) {}
 record ProgressInput(@DecimalMin("0") @DecimalMax("604800") double seconds) {}
 @GetMapping("/videos") List<Map<String,Object>> list(@RequestParam(defaultValue="") String q){return db.queryForList("select v.id,v.title,v.description,v.category,v.series_id,v.episode_number,s.title as series_title from videos v left join video_series s on s.id=v.series_id where v.published=true and (v.series_id is null or s.published=true) and (lower(v.title) like ? or lower(v.description) like ?) order by v.created_at desc","%"+q.toLowerCase(Locale.ROOT)+"%","%"+q.toLowerCase(Locale.ROOT)+"%");}
 @GetMapping("/series") List<Map<String,Object>> series(){return db.queryForList("select id,title,description from video_series where published=true order by title");}
 @GetMapping("/admin/videos") List<Map<String,Object>> adminList(){return db.queryForList("select id,title,description,category,series_id,episode_number,published from videos order by created_at desc");}
 @GetMapping("/admin/series") List<Map<String,Object>> adminSeries(){return db.queryForList("select id,title,description,published from video_series order by title");}
 @PostMapping("/admin/series") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> createSeries(@Valid @RequestBody SeriesInput in){UUID id=UUID.randomUUID();db.update("insert into video_series(id,title,description) values (?,?,?)",id,in.title().trim(),Objects.toString(in.description(),""));return db.queryForMap("select id,title,description,published from video_series where id=?",id);}
 @PatchMapping("/admin/series/{id}/publication") @ResponseStatus(HttpStatus.NO_CONTENT) void publishSeries(@PathVariable UUID id,@RequestBody PublishInput input){if(db.update("update video_series set published=? where id=?",input.published(),id)==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
 @PostMapping(value="/admin/videos",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
 Map<String,Object> upload(@RequestParam String title,@RequestParam(defaultValue="") String description,@RequestParam(defaultValue="") String category,@RequestParam(required=false) UUID seriesId,@RequestParam(required=false) Integer episodeNumber,@RequestParam MultipartFile file) throws IOException {
  if(title.isBlank()||title.length()>200||description.length()>5000||category.length()>100||file.isEmpty()||file.getSize()>500L*1024*1024||(seriesId!=null&&(episodeNumber==null||episodeNumber<1))||(seriesId==null&&episodeNumber!=null))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid video metadata or size");
  try(InputStream in=file.getInputStream()){byte[] header=in.readNBytes(12);if(header.length<12||header[4]!='f'||header[5]!='t'||header[6]!='y'||header[7]!='p')throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only MP4 files are accepted");}
  Files.createDirectories(root);UUID id=UUID.randomUUID();String name=id+".mp4";Path path=root.resolve(name);
  try{try(InputStream in=file.getInputStream()){Files.copy(in,path);} if(!Mp4Validator.valid(path))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid MP4 container"); db.update("insert into videos(id,title,description,category,series_id,episode_number,storage_name) values (?,?,?,?,?,?,?)",id,title.trim(),description,category,seriesId,episodeNumber,name);}catch(IOException | RuntimeException e){Files.deleteIfExists(path);throw e;}
  return db.queryForMap("select id,title,description,category,series_id,episode_number,published from videos where id=?",id);
 }
 @PatchMapping("/admin/videos/{id}/publication") @ResponseStatus(HttpStatus.NO_CONTENT) void publish(@PathVariable UUID id,@RequestBody PublishInput input){if(db.update("update videos set published=? where id=?",input.published(),id)==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
 @GetMapping("/videos/{id}/content") ResponseEntity<Resource> content(@PathVariable UUID id,Authentication auth){boolean admin=auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN"));var rows=db.queryForList("select v.storage_name from videos v left join video_series s on s.id=v.series_id where v.id=? and (?=true or (v.published=true and (v.series_id is null or s.published=true)))",id,admin);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);Path path=root.resolve((String)rows.get(0).get("storage_name")).normalize();if(!path.startsWith(root)||!Files.isRegularFile(path))throw new ResponseStatusException(HttpStatus.NOT_FOUND);return ResponseEntity.ok().contentType(MediaType.parseMediaType("video/mp4")).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").body(new FileSystemResource(path));}
 @GetMapping("/videos/{id}/progress") Map<String,Object> progress(@PathVariable UUID id,Principal p){ensurePublished(id);var rows=db.queryForList("select p.seconds from watch_progress p join app_users u on u.id=p.user_id where p.video_id=? and u.email=?",id,p.getName());return Map.of("seconds",rows.isEmpty()?0:rows.get(0).get("seconds"));}
 @PutMapping("/videos/{id}/progress") @ResponseStatus(HttpStatus.NO_CONTENT) void progress(@PathVariable UUID id,@Valid @RequestBody ProgressInput input,Principal p){if(!Double.isFinite(input.seconds()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);ensurePublished(id);db.update("insert into watch_progress(user_id,video_id,seconds) select id,?,? from app_users where email=? on conflict(user_id,video_id) do update set seconds=excluded.seconds,updated_at=now()",id,input.seconds(),p.getName());}
 private void ensurePublished(UUID id){Integer count=db.queryForObject("select count(*) from videos v left join video_series s on s.id=v.series_id where v.id=? and v.published=true and (v.series_id is null or s.published=true)",Integer.class,id);if(count==null||count==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
}
