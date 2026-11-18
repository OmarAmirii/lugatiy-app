package nl.lugatiy.api;
import java.security.Principal;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api/v1/admin/users")
class UserController {
 private final JdbcTemplate db;private final PasswordEncoder passwordEncoder;
 UserController(JdbcTemplate db,PasswordEncoder passwordEncoder){this.db=db;this.passwordEncoder=passwordEncoder;}
 record UserInput(@NotBlank @Email @Size(max=255) String email,@NotBlank @Size(max=120) String displayName,@NotBlank @Size(min=12,max=72) String password,@NotBlank @Pattern(regexp="ADMIN|TEACHER|STUDENT|PARENT") String role) {}
 record EnabledInput(boolean enabled) {}
 @GetMapping List<Map<String,Object>> users(){return db.queryForList("select u.id,u.email,u.display_name,u.enabled,u.created_at,string_agg(r.role,',' order by r.role) as roles from app_users u left join user_roles r on r.user_id=u.id group by u.id order by u.created_at desc limit 200");}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional Map<String,Object> create(@Valid @RequestBody UserInput in){if(in.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Password exceeds bcrypt byte limit");String email=in.email().trim().toLowerCase(Locale.ROOT);Integer count=db.queryForObject("select count(*) from app_users where lower(email)=?",Integer.class,email);if(count!=null&&count>0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Account exists");UUID id=UUID.randomUUID();db.update("insert into app_users(id,email,password_hash,display_name) values (?,?,?,?)",id,email,passwordEncoder.encode(in.password()),in.displayName().trim());db.update("insert into user_roles(user_id,role) values (?,?)",id,in.role());return db.queryForMap("select id,email,display_name,enabled from app_users where id=?",id);}
 @PatchMapping("/{id}/enabled") @ResponseStatus(HttpStatus.NO_CONTENT) void enabled(@PathVariable UUID id,@RequestBody EnabledInput input,Principal principal){Integer self=db.queryForObject("select count(*) from app_users where id=? and email=?",Integer.class,id,principal.getName());if(!input.enabled()&&self!=null&&self>0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Cannot disable own account");if(db.update("update app_users set enabled=?, auth_version=auth_version+1 where id=?",input.enabled(),id)==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
}
