package com.cart.ecom_proj.model;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
@Entity @Table(name="customer_order")
public class CustomerOrder {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private int id;
 @ManyToOne(fetch=FetchType.LAZY, optional=false) private User customer;
 @OneToMany(mappedBy="order",cascade=CascadeType.ALL,orphanRemoval=true) private List<OrderItem> items=new ArrayList<>();
 @Column(nullable=false) private Instant createdAt;
 @Column(nullable=false,precision=14,scale=2) private BigDecimal total;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private OrderStatus status=OrderStatus.PLACED;
 protected CustomerOrder() {}
 public CustomerOrder(User customer, BigDecimal total) {this.customer=customer;this.total=total;this.createdAt=Instant.now();}
 public void addItem(OrderItem item){items.add(item);item.setOrder(this);}
 public int getId(){return id;} public User getCustomer(){return customer;} public Instant getCreatedAt(){return createdAt;}
 public BigDecimal getTotal(){return total;} public List<OrderItem> getItems(){return items;}
}
