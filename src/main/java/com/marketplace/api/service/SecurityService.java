package com.marketplace.api.service;

import com.marketplace.api.entity.User;
import com.marketplace.api.entity.enums.Role;

public interface SecurityService {
    User getAuthenticatedUser();
    void requireRole(Role role);
}
