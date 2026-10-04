package com.cart.ecom_proj.repo;
import com.cart.ecom_proj.model.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface OrderRepo extends JpaRepository<CustomerOrder,Integer> { List<CustomerOrder> findByCustomerUsernameOrderByCreatedAtDesc(String username); }
