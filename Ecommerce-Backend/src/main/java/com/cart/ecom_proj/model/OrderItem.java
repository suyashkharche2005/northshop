package com.cart.ecom_proj.model;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="order_item")
public class OrderItem {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private int id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) private CustomerOrder order;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) private Product product;
 @Column(nullable=false) private String productName;
 @Column(nullable=false,precision=14,scale=2) private BigDecimal unitPrice;
 @Column(nullable=false) private int quantity;
 protected OrderItem() {}
 public OrderItem(Product product,int quantity){this.product=product;this.productName=product.getName();this.unitPrice=product.getPrice();this.quantity=quantity;}
 public void setOrder(CustomerOrder order){this.order=order;} public Product getProduct(){return product;}
 public int getProductId(){return product.getId();}
 public String getProductName(){return productName;} public BigDecimal getUnitPrice(){return unitPrice;} public int getQuantity(){return quantity;}
}
