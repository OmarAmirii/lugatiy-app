package nl.lugatiy.api;

import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Optional, one-time initial administrator. Never replaces an existing account. */
@Component
class AdminBootstrap implements ApplicationRunner {
 private final JdbcTemplate jdbc;
 private final PasswordEncoder passwords;
 private final Environment env;
 AdminBootstrap(JdbcTemplate jdbc, PasswordEncoder passwords, Environment env) { this.jdbc=jdbc; this.passwords=passwords; this.env=env; }
 @Override @Transactional public void run(ApplicationArguments args) {
  String email=env.getProperty("BOOTSTRAP_ADMIN_EMAIL", "").trim();
  String password=env.getProperty("BOOTSTRAP_ADMIN_PASSWORD", "");
  if (email.isEmpty() && password.isEmpty()) return;
  if (email.isEmpty() || password.length()<12) throw new IllegalStateException("Set BOOTSTRAP_ADMIN_EMAIL and a BOOTSTRAP_ADMIN_PASSWORD of at least 12 characters");
  Integer count=jdbc.queryForObject("select count(*) from app_users where lower(email)=lower(?)", Integer.class, email);
  if (count != null && count > 0) return;
  Integer admins=jdbc.queryForObject("select count(*) from user_roles where role='ADMIN'", Integer.class);
  if (admins != null && admins > 0) return;
  UUID id=UUID.randomUUID();
  jdbc.update("insert into app_users (id,email,password_hash,display_name,enabled) values (?,?,?,?,true)",id,email,passwords.encode(password),"Beheerder");
  jdbc.update("insert into user_roles (user_id,role) values (?, 'ADMIN')",id);
 }
}
