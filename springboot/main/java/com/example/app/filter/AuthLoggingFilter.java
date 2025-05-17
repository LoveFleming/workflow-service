package com.example.app.filter;

import com.example.app.auth.CustomAuthorization;
import com.example.app.constant.Constants;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class AuthLoggingFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(AuthLoggingFilter.class);
    private final CustomAuthorization customAuthorization;

    public AuthLoggingFilter(CustomAuthorization customAuthorization) {
        this.customAuthorization = customAuthorization;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
        throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        log.info("[{}] {}?{}", request.getMethod(), request.getRequestURI(), request.getQueryString());
        String requiredRole = request.getRequestURI().startsWith("/api/v1/admin") ? Constants.ROLE_ADMIN : Constants.ROLE_USER;
        if (!customAuthorization.authorize(request, requiredRole)) {
            log.warn("Unauthorized request from {}", request.getRemoteAddr());
            response.sendError(401, "Unauthorized");
            return;
        }
        chain.doFilter(req, res);
    }
}