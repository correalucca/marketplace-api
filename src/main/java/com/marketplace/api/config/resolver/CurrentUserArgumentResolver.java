package com.marketplace.api.config.resolver;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.marketplace.api.entity.User;
import com.marketplace.api.service.security.SecurityService;

public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final SecurityService securityService;

    public CurrentUserArgumentResolver(SecurityService securityService) {
        this.securityService = securityService;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) && parameter.getParameterType().equals(User.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        if (securityService == null) return null;
        return securityService.getAuthenticatedUser();
    }
}
