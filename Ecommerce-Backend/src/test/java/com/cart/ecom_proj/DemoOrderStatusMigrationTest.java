package com.cart.ecom_proj;

import com.cart.ecom_proj.config.DemoOrderStatusMigration;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.*;

class DemoOrderStatusMigrationTest {
 @Test void oldSingleValueEnumAcceptsNewStatusesAfterMigration(){
  JdbcTemplate jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:old_status;MODE=MySQL;DB_CLOSE_DELAY=-1","sa",""));
  jdbc.execute("create table customer_order(id integer primary key, status enum('PLACED') not null)");
  jdbc.execute("insert into customer_order(id,status) values (1,'PLACED')");
  DemoOrderStatusMigration migration=new DemoOrderStatusMigration(jdbc);
  migration.migrate();
  jdbc.execute("update customer_order set status='CANCELLED' where id=1");
  assertEquals("CANCELLED",jdbc.queryForObject("select status from customer_order where id=1",String.class));
  migration.migrate();
  jdbc.execute("update customer_order set status='SHIPPED' where id=1");
  assertEquals("SHIPPED",jdbc.queryForObject("select status from customer_order where id=1",String.class));
 }
}
