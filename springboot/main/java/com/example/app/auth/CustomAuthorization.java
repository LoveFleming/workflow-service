package com.example.app.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Set;

@Component
public class CustomAuthorization {
    private final ExternalPrivilegeService privilegeService;
    private final PermissionRoleMapper roleMapper;
    public CustomAuthorization(ExternalPrivilegeService privilegeService, PermissionRoleMapper roleMapper) {
        this.privilegeService = privilegeService;
        this.roleMapper = roleMapper;
    }

    public boolean authorize(HttpServletRequest req, String requiredRole) {
        String userId = req.getHeader("X-USER-ID");
        if (userId == null) return false;

        List<String> permissionIds = privilegeService.getUserPermissionIds(userId);
        Set<String> roles = roleMapper.mapPermissionIdsToRoles(permissionIds);
        return roles.contains(requiredRole);
    }
}