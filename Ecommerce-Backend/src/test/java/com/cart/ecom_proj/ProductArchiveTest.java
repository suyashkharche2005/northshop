package com.cart.ecom_proj;

import com.cart.ecom_proj.dto.OrderRequest;
import com.cart.ecom_proj.exception.ApiException;
import com.cart.ecom_proj.model.Product;
import com.cart.ecom_proj.model.Role;
import com.cart.ecom_proj.model.User;
import com.cart.ecom_proj.repo.ProductRepo;
import com.cart.ecom_proj.repo.UserRepo;
import com.cart.ecom_proj.service.OrderService;
import com.cart.ecom_proj.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:archive_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.jwt.secret=archive-test-secret-at-least-32-characters",
        "app.cors.origins=http://localhost:5173",
        "ADMIN_USERNAME=", "ADMIN_EMAIL=", "ADMIN_PASSWORD="
})
class ProductArchiveTest {
    @Autowired ProductRepo products;
    @Autowired UserRepo users;
    @Autowired ProductService productService;
    @Autowired OrderService orderService;

    @Test void removingPurchasedProductKeepsOrderButHidesProduct() {
        User buyer = new User();
        buyer.setUsername("archive_buyer");
        buyer.setEmail("archive@example.invalid");
        buyer.setPassword("unused-test-hash");
        buyer.setRole(Role.USER);
        users.save(buyer);

        Product product = new Product();
        product.setName("Purchased test item");
        product.setDescription("Archive regression");
        product.setBrand("Northshop Test");
        product.setCategory("Testing");
        product.setPrice(new BigDecimal("100.00"));
        product.setStockQuantity(2);
        product.setProductAvailable(true);
        int id = products.save(product).getId();

        int orderId = orderService.place(buyer.getUsername(),
                new OrderRequest(List.of(new OrderRequest.Item(id, 1)))).id();
        productService.delete(id);

        assertTrue(products.findById(id).isPresent(), "Historical FK target must remain");
        assertTrue(Boolean.TRUE.equals(products.findById(id).orElseThrow().getArchived()));
        assertFalse(products.findById(id).orElseThrow().isProductAvailable());
        assertTrue(productService.all().stream().noneMatch(p -> p.id() == id));
        assertTrue(productService.search("Purchased test item").isEmpty());
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ApiException.class, () -> productService.one(id)).getStatus());
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ApiException.class, () -> orderService.place(buyer.getUsername(),
                        new OrderRequest(List.of(new OrderRequest.Item(id, 1))))).getStatus());
        assertEquals(orderId, orderService.mine(buyer.getUsername()).get(0).id());
        assertEquals(new BigDecimal("100.00"), orderService.mine(buyer.getUsername()).get(0).items().get(0).unitPrice());

        Product legacy = new Product();
        legacy.setName("Older visible item");
        legacy.setBrand("Northshop Test");
        legacy.setDescription("Pre-upgrade row");
        legacy.setCategory("Testing");
        legacy.setPrice(new BigDecimal("50.00"));
        legacy.setStockQuantity(1);
        legacy.setProductAvailable(true);
        legacy.setArchived(null);
        int legacyId = products.save(legacy).getId();
        assertTrue(productService.all().stream().anyMatch(p -> p.id() == legacyId));
    }
}
