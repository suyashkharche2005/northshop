package com.cart.ecom_proj.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Previous demo databases stored status as H2 ENUM('PLACED'). Hibernate's
 * ddl-auto=update adds columns but does not widen that existing enum. Convert
 * it to text so new lifecycle values work without losing historical orders.
 */
@Component
@Profile("demo")
public class DemoOrderStatusMigration implements ApplicationRunner {
 private static final Logger log=LoggerFactory.getLogger(DemoOrderStatusMigration.class);
 private final JdbcTemplate jdbc;
 public DemoOrderStatusMigration(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @Override public void run(ApplicationArguments args){migrate();}
 public void migrate(){
  List<String> types=jdbc.queryForList(
   "select DATA_TYPE from INFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA = 'PUBLIC' and TABLE_NAME = 'CUSTOMER_ORDER' and COLUMN_NAME = 'STATUS'",
   String.class);
  if(types.size()!=1)throw new IllegalStateException("Expected customer_order.status in the demo database");
  if("ENUM".equalsIgnoreCase(types.get(0))){
   jdbc.execute("alter table customer_order alter column status set data type varchar(32)");
   log.info("Demo database: expanded legacy order status column without removing orders");
  }
 }
}
