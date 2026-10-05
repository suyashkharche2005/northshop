package com.cart.ecom_proj.model;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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
 @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR) @Column(nullable=false,length=32) private OrderStatus status=OrderStatus.PLACED;
 @Column(length=80,unique=true) private String gatewayOrderId;
 @Column(length=80) private String gatewayPaymentId;
 private Instant updatedAt;
 protected CustomerOrder() {}
 public CustomerOrder(User customer, BigDecimal total) {this.customer=customer;this.total=total;this.createdAt=Instant.now();}
 public OrderStatus getStatus(){return status;}
 public void setStatus(OrderStatus status){this.status=status;this.updatedAt=Instant.now();}
 public String getGatewayOrderId(){return gatewayOrderId;}
 public void setGatewayOrderId(String id){this.gatewayOrderId=id;}
 public String getGatewayPaymentId(){return gatewayPaymentId;}
 public void setGatewayPaymentId(String id){this.gatewayPaymentId=id;}
 public Instant getUpdatedAt(){return updatedAt;}
 public void addItem(OrderItem item){items.add(item);item.setOrder(this);}
 public int getId(){return id;} public User getCustomer(){return customer;} public Instant getCreatedAt(){return createdAt;}
 public BigDecimal getTotal(){return total;} public List<OrderItem> getItems(){return items;}
}
