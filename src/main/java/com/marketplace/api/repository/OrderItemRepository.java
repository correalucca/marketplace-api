package com.marketplace.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.marketplace.api.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
