package com.cart.ecom_proj.controller;
import com.cart.ecom_proj.dto.*;
import com.cart.ecom_proj.exception.ApiException;
import com.cart.ecom_proj.model.*;
import com.cart.ecom_proj.repo.UserRepo;
import com.cart.ecom_proj.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth")
public class AuthController {
 private final UserRepo users;private final PasswordEncoder passwords;private final AuthenticationManager authentication;private final JwtUtil jwt;
 public AuthController(UserRepo users,PasswordEncoder passwords,AuthenticationManager authentication,JwtUtil jwt){this.users=users;this.passwords=passwords;this.authentication=authentication;this.jwt=jwt;}
 @PostMapping("/register") public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request){
  if(users.existsByUsername(request.getUsername().trim()))throw new ApiException(HttpStatus.CONFLICT,"Username already taken");
  if(users.existsByEmail(request.getEmail().trim().toLowerCase()))throw new ApiException(HttpStatus.CONFLICT,"Email already registered");
  User user=new User();user.setUsername(request.getUsername().trim());user.setEmail(request.getEmail().trim().toLowerCase());user.setPassword(passwords.encode(request.getPassword()));user.setRole(Role.USER);users.save(user);
  return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(jwt.generateToken(user.getUsername(),user.getRole().name()),user.getUsername(),user.getRole().name()));
 }
 @PostMapping("/login") public AuthResponse login(@Valid @RequestBody LoginRequest request){
  try{authentication.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(),request.getPassword()));}catch(BadCredentialsException e){throw new ApiException(HttpStatus.UNAUTHORIZED,"Invalid username or password");}
  User user=users.findByUsername(request.getUsername()).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"Invalid username or password"));
  return new AuthResponse(jwt.generateToken(user.getUsername(),user.getRole().name()),user.getUsername(),user.getRole().name());
 }
}
