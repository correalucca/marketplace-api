package com.marketplace.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.marketplace.api.entity.OrderItem;
import com.marketplace.api.entity.Product;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByProductIn(List<Product> products);
}
