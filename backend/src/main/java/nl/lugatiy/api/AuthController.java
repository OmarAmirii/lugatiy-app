package nl.lugatiy.api;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api/v1/auth")
class AuthController {
 private final JdbcTemplate db; private final PasswordEncoder passwords;
 AuthController(JdbcTemplate db,PasswordEncoder passwords){this.db=db;this.passwords=passwords;}
 record PasswordInput(@NotBlank String currentPassword,@NotBlank @Size(min=12,max=72) String newPassword) {}
 @GetMapping("/csrf") CsrfToken csrf(CsrfToken token) { return token; }
 @GetMapping("/me") Map<String,Object> me(Authentication auth) { return Map.of("email",auth.getName(), "roles", auth.getAuthorities().stream().map(Object::toString).toList()); }
 @PostMapping("/password") @ResponseStatus(HttpStatus.NO_CONTENT) void password(@Valid @RequestBody PasswordInput in,Authentication auth,HttpServletRequest request){if(in.newPassword().getBytes(StandardCharsets.UTF_8).length>72)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);String hash=db.queryForObject("select password_hash from app_users where email=?",String.class,auth.getName());if(hash==null||!passwords.matches(in.currentPassword(),hash))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);db.update("update app_users set password_hash=?,auth_version=auth_version+1 where email=?",passwords.encode(in.newPassword()),auth.getName());var session=request.getSession(false);if(session!=null)session.invalidate();SecurityContextHolder.clearContext();}
}
