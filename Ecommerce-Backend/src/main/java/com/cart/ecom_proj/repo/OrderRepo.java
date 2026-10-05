package com.cart.ecom_proj.repo;
import com.cart.ecom_proj.model.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface OrderRepo extends JpaRepository<CustomerOrder,Integer> {
 List<CustomerOrder> findByCustomerUsernameOrderByCreatedAtDesc(String username);
 List<CustomerOrder> findAllByOrderByCreatedAtDesc();
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select o from CustomerOrder o where o.id = :id") Optional<CustomerOrder> lockById(@Param("id") int id);
}
