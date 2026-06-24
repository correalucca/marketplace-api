package com.marketplace.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.marketplace.api.entity.Order;
import com.marketplace.api.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrder(Order order);
}
