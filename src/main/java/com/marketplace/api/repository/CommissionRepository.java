package com.marketplace.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.marketplace.api.entity.Commission;
import com.marketplace.api.entity.User;

public interface CommissionRepository extends JpaRepository<Commission, Long> {
    List<Commission> findBySeller(User seller);
}
