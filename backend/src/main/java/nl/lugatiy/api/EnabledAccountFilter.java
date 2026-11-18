package nl.lugatiy.api;
import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
class EnabledAccountFilter extends OncePerRequestFilter {
 private final JdbcTemplate db;
 EnabledAccountFilter(JdbcTemplate db){this.db=db;}
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
  var auth=SecurityContextHolder.getContext().getAuthentication();
  if(auth!=null&&auth.isAuthenticated()&&!(auth instanceof AnonymousAuthenticationToken)){
   var session=request.getSession(false);Object version=session==null?null:session.getAttribute("auth_version");
   Integer active=version instanceof Integer ? db.queryForObject("select count(*) from app_users where email=? and enabled=true and auth_version=?",Integer.class,auth.getName(),version) : 0;
   if(active==null||active==0){if(session!=null)session.invalidate();SecurityContextHolder.clearContext();response.sendError(HttpServletResponse.SC_UNAUTHORIZED);return;}
  }
  chain.doFilter(request,response);
 }
}
