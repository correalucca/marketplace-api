package com.marketplace.api.service.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.marketplace.api.entity.User;
import com.marketplace.api.entity.enums.Role;
import com.marketplace.api.exception.BusinessException;
import com.marketplace.api.repository.UserRepository;

@Slf4j
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
            log.warn("Unauthenticated access attempt");
            throw new BusinessException("User not authenticated");
        }
        String email = auth.getName();
        log.debug("Authenticated user: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("Authenticated user not found in database: {}", email);
                    return new BusinessException("Authenticated user not found");
                });
    }

    @Override
    public void requireRole(Role role) {
        User user = getAuthenticatedUser();
        if (user.getRole() != role && user.getRole() != Role.ADMIN) {
            log.warn("Access denied for user={}, required role={}, actual role={}",
                    user.getEmail(), role, user.getRole());
            throw new BusinessException("Access denied: " + role + " role required");
        }
    }
}
