package nl.lugatiy.api;

import java.io.IOException;
import java.nio.file.*;
import java.security.Principal;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.apache.pdfbox.Loader;
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
class BookController {
 private final JdbcTemplate db; private final Path root;
 BookController(JdbcTemplate db,@Value("${app.storage-path}") String storage){this.db=db;this.root=Path.of(storage).toAbsolutePath().normalize().resolve("books");}
 record PageInput(@Min(1) @Max(100000) int page) {}
 record PublishInput(boolean published) {}
 @GetMapping("/books") List<Map<String,Object>> books(@RequestParam(defaultValue="") String q){return db.queryForList("select id,title,description,category,level_id from books where published=true and (lower(title) like ? or lower(description) like ?) order by created_at desc", "%"+q.toLowerCase(Locale.ROOT)+"%","%"+q.toLowerCase(Locale.ROOT)+"%");}
 @GetMapping("/admin/books") List<Map<String,Object>> adminBooks(){return db.queryForList("select id,title,description,category,level_id,published from books order by created_at desc");}
 @PostMapping(value="/admin/books",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
 Map<String,Object> upload(@RequestParam @NotBlank @Size(max=200) String title,@RequestParam(defaultValue="") @Size(max=5000) String description,@RequestParam(defaultValue="") @Size(max=100) String category,@RequestParam MultipartFile file) throws IOException {
  if(title.isBlank()||title.length()>200||description.length()>5000||category.length()>100||file.isEmpty()||file.getSize()>25L*1024*1024)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid book or file size");
  byte[] bytes=file.getBytes(); if(bytes.length<5||bytes[0]!='%'||bytes[1]!='P'||bytes[2]!='D'||bytes[3]!='F'||bytes[4]!='-')throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only PDF files are accepted");
  try(var document=Loader.loadPDF(bytes)){if(document.isEncrypted()||document.getNumberOfPages()==0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"PDF cannot be read");}catch(IOException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid PDF");}
  Files.createDirectories(root);UUID id=UUID.randomUUID();String name=id+".pdf";Path path=root.resolve(name);Files.write(path,bytes,StandardOpenOption.CREATE_NEW);
  try{String original=Objects.toString(file.getOriginalFilename(),"document.pdf");if(original.length()>255)original=original.substring(original.length()-255);db.update("insert into books(id,title,description,category,storage_name,original_name) values (?,?,?,?,?,?)",id,title.trim(),description,category,name,original);}catch(RuntimeException e){Files.deleteIfExists(path);throw e;}
  return db.queryForMap("select id,title,description,category,published from books where id=?",id);
 }
 @PatchMapping("/admin/books/{id}/publication") void publish(@PathVariable UUID id,@RequestBody PublishInput input){if(db.update("update books set published=? where id=?",input.published(),id)==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
 @GetMapping("/books/{id}/content") ResponseEntity<Resource> content(@PathVariable UUID id,Authentication auth){
  boolean admin=auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN"));var rows=db.queryForList("select storage_name from books where id=? and (published=true or ?=true)",id,admin);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  Path path=root.resolve((String)rows.get(0).get("storage_name")).normalize();if(!path.startsWith(root)||!Files.isRegularFile(path))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").header("Content-Disposition","inline").body(new FileSystemResource(path));
 }
 @GetMapping("/books/{id}/progress") Map<String,Object> progress(@PathVariable UUID id,Principal p){ensurePublished(id);var rows=db.queryForList("select p.page from reading_progress p join app_users u on u.id=p.user_id where p.book_id=? and u.email=?",id,p.getName());return Map.of("page",rows.isEmpty()?1:rows.get(0).get("page"));}
 @PutMapping("/books/{id}/progress") @ResponseStatus(HttpStatus.NO_CONTENT) void progress(@PathVariable UUID id,@Valid @RequestBody PageInput input,Principal p){ensurePublished(id);db.update("insert into reading_progress(user_id,book_id,page) select id,?,? from app_users where email=? on conflict(user_id,book_id) do update set page=excluded.page,updated_at=now()",id,input.page(),p.getName());}
 private void ensurePublished(UUID id){Integer count=db.queryForObject("select count(*) from books where id=? and published=true",Integer.class,id);if(count==null||count==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
}
