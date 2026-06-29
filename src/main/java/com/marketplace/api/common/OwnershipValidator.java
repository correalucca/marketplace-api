package com.marketplace.api.common;

import com.marketplace.api.entity.User;
import com.marketplace.api.entity.enums.Role;
import com.marketplace.api.exception.BusinessException;

public interface OwnershipValidator {
    void validateOwnership(Long resourceOwnerId, User currentUser, String message);
}
