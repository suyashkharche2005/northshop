package com.cart.ecom_proj.security;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
import java.util.Arrays;
@Configuration
public class SecurityConfig {
 private final CustomUserDetailsService details;private final JwtAuthFilter jwtFilter;private final String origins;
 public SecurityConfig(CustomUserDetailsService details,JwtAuthFilter jwtFilter,@Value("${app.cors.origins}") String origins){this.details=details;this.jwtFilter=jwtFilter;this.origins=origins;}
 // The filter belongs only in Spring Security's chain. Auto-registering it as a servlet
 // filter runs it too early and OncePerRequestFilter then skips it in the chain.
 @Bean public FilterRegistrationBean<JwtAuthFilter> jwtFilterRegistration(JwtAuthFilter filter){
  FilterRegistrationBean<JwtAuthFilter> registration=new FilterRegistrationBean<>(filter);
  registration.setEnabled(false);
  return registration;
 }
 @Bean public PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean public DaoAuthenticationProvider authenticationProvider(){DaoAuthenticationProvider p=new DaoAuthenticationProvider();p.setUserDetailsService(details);p.setPasswordEncoder(passwordEncoder());return p;}
 @Bean public AuthenticationManager authenticationManager(AuthenticationConfiguration config)throws Exception{return config.getAuthenticationManager();}
 @Bean public CorsConfigurationSource corsConfigurationSource(){CorsConfiguration c=new CorsConfiguration();c.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toList());c.setAllowedMethods(java.util.List.of("GET","POST","PUT","DELETE","OPTIONS"));c.setAllowedHeaders(java.util.List.of("Authorization","Content-Type"));UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/**",c);return source;}
 @Bean public SecurityFilterChain securityFilterChain(HttpSecurity http)throws Exception{http.cors(cors->cors.configurationSource(corsConfigurationSource())).csrf(csrf->csrf.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).authorizeHttpRequests(a->a
  .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
  .requestMatchers("/api/auth/**","/actuator/health","/error").permitAll()
  .requestMatchers(HttpMethod.GET,"/api/products","/api/products/search","/api/product/*","/api/product/*/image").permitAll()
  .requestMatchers(HttpMethod.POST,"/api/product").hasRole("ADMIN")
  .requestMatchers(HttpMethod.PUT,"/api/product/*").hasRole("ADMIN")
  .requestMatchers(HttpMethod.DELETE,"/api/product/*").hasRole("ADMIN")
  .requestMatchers("/api/orders/**").authenticated().anyRequest().denyAll())
  .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->{res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"message\":\"Authentication required\"}");}).accessDeniedHandler((req,res,ex)->{res.setStatus(403);res.setContentType("application/json");res.getWriter().write("{\"message\":\"Access denied\"}");}))
  .authenticationProvider(authenticationProvider()).addFilterBefore(jwtFilter,UsernamePasswordAuthenticationFilter.class);return http.build();}
}
