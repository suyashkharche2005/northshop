package com.cart.ecom_proj;

import com.cart.ecom_proj.repo.ProductRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:catalog_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.jwt.secret=catalog-test-secret-at-least-32-characters",
        "app.cors.origins=http://localhost:5173",
        "ADMIN_USERNAME=", "ADMIN_EMAIL=", "ADMIN_PASSWORD="
})
@ActiveProfiles("demo")
class DemoCatalogSeederTest {
    @Autowired ProductRepo products;

    @Test void demoProfileLoadsCatalogWithImages() {
        assertEquals(12, products.count());
        assertTrue(products.existsByName("Studio Wireless Headphones"));
        assertTrue(products.findAll().stream().allMatch(p -> p.getImageDate() != null
                && p.getImageDate().length > 1000 && p.getStockQuantity() > 0));
    }
}
