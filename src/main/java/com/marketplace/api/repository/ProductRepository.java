package com.marketplace.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.marketplace.api.entity.Product;
import com.marketplace.api.entity.User;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findBySeller(User seller);
    List<Product> findByNameContainingIgnoreCase(String name);
}
