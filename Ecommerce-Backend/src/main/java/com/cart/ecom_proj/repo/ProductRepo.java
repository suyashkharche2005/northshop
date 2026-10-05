package com.cart.ecom_proj.repo;
import com.cart.ecom_proj.model.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface ProductRepo extends JpaRepository<Product,Integer> {
 boolean existsByName(String name);
 @Query("select p from Product p where p.archived is null or p.archived = false")
 List<Product> findVisible();
 @Query("select p from Product p where (p.archived is null or p.archived = false) and (lower(p.name) like lower(concat('%',:keyword,'%')) or lower(p.brand) like lower(concat('%',:keyword,'%')) or lower(p.category) like lower(concat('%',:keyword,'%')))")
 List<Product> searchProducts(@Param("keyword") String keyword);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from Product p where p.id=:id and (p.archived is null or p.archived = false)") Optional<Product> lockById(@Param("id") Integer id);
}
