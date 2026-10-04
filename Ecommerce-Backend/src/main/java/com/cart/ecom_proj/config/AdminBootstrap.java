package com.cart.ecom_proj.config;
import com.cart.ecom_proj.model.*;
import com.cart.ecom_proj.repo.UserRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
@Configuration
public class AdminBootstrap {
 @Bean CommandLineRunner admin(UserRepo repo,PasswordEncoder encoder,@Value("${ADMIN_USERNAME:}") String name,@Value("${ADMIN_EMAIL:}") String email,@Value("${ADMIN_PASSWORD:}") String password){return args->{
  if(name.isBlank()||email.isBlank()||password.isBlank())return;
  if(password.length()<12)throw new IllegalArgumentException("ADMIN_PASSWORD must be at least 12 characters");
  if(repo.findByUsername(name).isEmpty() && !repo.existsByEmail(email)){User admin=new User();admin.setUsername(name);admin.setEmail(email);admin.setPassword(encoder.encode(password));admin.setRole(Role.ADMIN);repo.save(admin);}
 };}
}
