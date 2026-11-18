package nl.lugatiy.api;
import java.util.Arrays;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.cors.*;
@Configuration
class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
 @Bean UserDetailsService users(JdbcTemplate jdbc) { return email -> {
  var users=jdbc.query("select email,password_hash,enabled from app_users where lower(email)=lower(?)", (rs,n) -> new Object[]{rs.getString(1),rs.getString(2),rs.getBoolean(3)}, email);
  if(users.isEmpty()) throw new UsernameNotFoundException("User not found");
  var u=users.get(0); var roles=jdbc.queryForList("select role from user_roles where user_id=(select id from app_users where lower(email)=lower(?))", String.class, email);
  return User.withUsername((String)u[0]).password((String)u[1]).disabled(!(Boolean)u[2]).roles(roles.toArray(String[]::new)).build();
 }; }
 @Bean SecurityFilterChain security(HttpSecurity http, JdbcTemplate jdbc) throws Exception {
  return http.addFilterAfter(new EnabledAccountFilter(jdbc), AnonymousAuthenticationFilter.class).cors(c -> {}).csrf(c -> c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
   .authorizeHttpRequests(a -> a.requestMatchers(HttpMethod.OPTIONS,"/**").permitAll().requestMatchers("/api/v1/auth/csrf", "/api/v1/auth/login", "/v3/api-docs/**", "/swagger-ui/**").permitAll().requestMatchers(HttpMethod.GET,"/api/v1/words/**", "/api/v1/levels/**", "/api/v1/quizzes/**", "/api/v1/verbs/**").permitAll().requestMatchers("/api/v1/admin/**").hasRole("ADMIN").requestMatchers("/api/v1/levels/**").hasRole("ADMIN").requestMatchers("/api/v1/words/**").hasRole("ADMIN").anyRequest().authenticated())
   .formLogin(f -> f.loginProcessingUrl("/api/v1/auth/login").successHandler((req,res,auth)->{Integer version=jdbc.queryForObject("select auth_version from app_users where email=?",Integer.class,auth.getName());req.getSession(true).setAttribute("auth_version",version);res.setStatus(204);}).failureHandler((req,res,e)->res.sendError(401)))
   .logout(l -> l.logoutUrl("/api/v1/auth/logout").logoutSuccessHandler((req,res,auth)->res.setStatus(204)))
   .exceptionHandling(e -> e.authenticationEntryPoint((req,res,ex)->res.sendError(401))).build();
 }
 @Bean CorsConfigurationSource corsConfigurationSource(@Value("${app.cors-origins}") String origins) {
  var c=new CorsConfiguration(); c.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toList()); c.setAllowedMethods(java.util.List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS")); c.setAllowedHeaders(java.util.List.of("Content-Type","X-XSRF-TOKEN","X-Requested-With")); c.setAllowCredentials(true); var source=new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**",c); return source;
 }
}
