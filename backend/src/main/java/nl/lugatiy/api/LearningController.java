package nl.lugatiy.api;
import java.security.Principal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1")
class LearningController {
 private final JdbcTemplate db;
 LearningController(JdbcTemplate db){this.db=db;}
 @GetMapping("/verbs") List<Map<String,Object>> verbs(@RequestParam(defaultValue="") String q){String term="%"+Arabic.normalize(q)+"%";return db.queryForList("select id,arabic,dutch,root,pattern,verification_status from verbs where verification_status in ('REVIEWED','NEEDS_REVIEW') and (normalized like ? or lower(dutch) like ?) order by dutch",term,term);}
 @GetMapping("/verbs/{id}/conjugations") List<Map<String,Object>> forms(@PathVariable UUID id){return db.queryForList("select f.category,f.pronoun,f.form,f.position from verb_forms f join verbs v on v.id=f.verb_id where f.verb_id=? and v.verification_status in ('REVIEWED','NEEDS_REVIEW') order by f.category,f.position",id);}
 @GetMapping("/favorites") List<Map<String,Object>> favorites(Principal p){return db.queryForList("select w.id,w.arabic,w.dutch from favorite_words f join words w on w.id=f.word_id join app_users u on u.id=f.user_id where u.email=? order by f.created_at desc",p.getName());}
 @PutMapping("/favorites/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void add(@PathVariable UUID id,Principal p){db.update("insert into favorite_words(user_id,word_id) select id,? from app_users where email=? on conflict do nothing",id,p.getName());}
 @DeleteMapping("/favorites/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void remove(@PathVariable UUID id,Principal p){db.update("delete from favorite_words where word_id=? and user_id=(select id from app_users where email=?)",id,p.getName());}
}
