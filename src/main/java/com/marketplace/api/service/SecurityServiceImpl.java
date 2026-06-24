package com.marketplace.api.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.marketplace.api.entity.User;
import com.marketplace.api.entity.enums.Role;
import com.marketplace.api.exception.BusinessException;
import com.marketplace.api.repository.UserRepository;

@Service
public class SecurityServiceImpl implements SecurityService {

    private final UserRepository userRepository;

    public SecurityServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException("User not authenticated");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Authenticated user not found"));
    }

    @Override
    public void requireRole(Role role) {
        User user = getAuthenticatedUser();
        if (user.getRole() != role && user.getRole() != Role.ADMIN) {
            throw new BusinessException("Access denied: " + role + " role required");
        }
    }
}
