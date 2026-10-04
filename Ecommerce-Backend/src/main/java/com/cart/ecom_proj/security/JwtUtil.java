package com.cart.ecom_proj.security;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
@Component
public class JwtUtil {
 private final SecretKey key; private static final long EXPIRATION_MS=1000L*60*60*10;
 public JwtUtil(@Value("${app.jwt.secret}") String secret){if(secret.length()<32)throw new IllegalArgumentException("JWT_SECRET must be at least 32 characters");key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));}
 public String generateToken(String username,String role){return Jwts.builder().setSubject(username).claim("role",role).setIssuedAt(new Date()).setExpiration(new Date(System.currentTimeMillis()+EXPIRATION_MS)).signWith(key,SignatureAlgorithm.HS256).compact();}
 public String extractUsername(String token){return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getSubject();}
 public boolean isTokenValid(String token,UserDetails details){return extractUsername(token).equals(details.getUsername());}
}
