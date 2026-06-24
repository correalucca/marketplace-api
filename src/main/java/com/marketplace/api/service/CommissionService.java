package com.marketplace.api.service;

import java.util.List;

import com.marketplace.api.entity.Commission;
import com.marketplace.api.entity.Order;
import com.marketplace.api.entity.User;

public interface CommissionService {
    void saveCommissions(Order order);
    List<Commission> findBySeller(User seller);
}
