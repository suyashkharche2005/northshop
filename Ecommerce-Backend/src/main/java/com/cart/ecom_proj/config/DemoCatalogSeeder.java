package com.cart.ecom_proj.config;

import com.cart.ecom_proj.model.Product;
import com.cart.ecom_proj.repo.ProductRepo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;

/** Adds a curated catalog to local demo databases without modifying existing products. */
@Component
@Profile("demo")
public class DemoCatalogSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoCatalogSeeder.class);
    private final ProductRepo products;
    private final ObjectMapper mapper;
    private final boolean enabled;

    public DemoCatalogSeeder(ProductRepo products, ObjectMapper mapper,
                             @Value("${app.demo.seed:true}") boolean enabled) {
        this.products = products;
        this.mapper = mapper;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (!enabled) return;
        List<DemoItem> catalog;
        try (InputStream source = new ClassPathResource("demo/catalog.json").getInputStream()) {
            catalog = mapper.readValue(source, new TypeReference<>() {});
        }
        int created = 0;
        for (DemoItem item : catalog) {
            if (products.existsByName(item.name())) continue;
            Product product = new Product();
            product.setName(item.name());
            product.setBrand(item.brand());
            product.setDescription(item.description());
            product.setPrice(item.price());
            product.setCategory(item.category());
            product.setReleaseDate(Date.valueOf(item.releaseDate()));
            product.setStockQuantity(item.stockQuantity());
            product.setProductAvailable(item.productAvailable() && item.stockQuantity() > 0);
            product.setImageName(item.image());
            product.setImageType("image/png");
            try (InputStream image = new ClassPathResource("demo/images/" + item.image()).getInputStream()) {
                product.setImageDate(image.readAllBytes());
            }
            products.save(product);
            created++;
        }
        log.info("Demo catalog: {} new products added, {} already present", created, catalog.size() - created);
    }

    public record DemoItem(String name, String brand, String description, BigDecimal price,
                            String category, String releaseDate, boolean productAvailable,
                            int stockQuantity, String image) {}
}
