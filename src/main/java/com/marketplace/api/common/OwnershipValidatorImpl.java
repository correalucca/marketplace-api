package com.marketplace.api.common;

import org.springframework.stereotype.Component;

import com.marketplace.api.entity.User;
import com.marketplace.api.entity.enums.Role;
import com.marketplace.api.exception.BusinessException;

@Component
public class OwnershipValidatorImpl implements OwnershipValidator {

    @Override
    public void validateOwnership(Long resourceOwnerId, User currentUser, String message) {
        if (!resourceOwnerId.equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN) {
            throw new BusinessException(message);
        }
    }
}
