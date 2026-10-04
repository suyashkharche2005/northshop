package com.cart.ecom_proj;

import com.cart.ecom_proj.dto.AuthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:jwt_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.jwt.secret=jwt-test-secret-must-have-at-least-32-characters",
        "app.cors.origins=http://localhost:5173",
        "ADMIN_USERNAME=", "ADMIN_EMAIL=", "ADMIN_PASSWORD="
})
class JwtAuthorizationTest {
    @Autowired TestRestTemplate http;

    @Test void registeredUserTokenAuthenticatesButCannotCreateProduct() {
        ResponseEntity<AuthResponse> registered = http.postForEntity("/api/auth/register",
                Map.of("username", "jwtbuyer", "email", "jwtbuyer@example.com", "password", "SecurePass123!"),
                AuthResponse.class);
        assertEquals(HttpStatus.CREATED, registered.getStatusCode());
        assertNotNull(registered.getBody());
        String token = registered.getBody().getToken();

        assertEquals(HttpStatus.UNAUTHORIZED,
                http.getForEntity("/api/orders/mine", String.class).getStatusCode());
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        assertEquals(HttpStatus.OK, http.exchange("/api/orders/mine", HttpMethod.GET,
                new HttpEntity<>(headers), String.class).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, http.exchange("/api/orders/admin", HttpMethod.GET,
                new HttpEntity<>(headers), String.class).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, http.exchange("/api/orders/1/status", HttpMethod.PUT,
                new HttpEntity<>(Map.of("status", "SHIPPED"), headers), String.class).getStatusCode());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        assertEquals(HttpStatus.FORBIDDEN, http.exchange("/api/product", HttpMethod.POST,
                new HttpEntity<>(headers), String.class).getStatusCode());
    }
}
