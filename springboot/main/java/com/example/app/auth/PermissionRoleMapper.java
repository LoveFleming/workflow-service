package com.example.app.auth;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class PermissionRoleMapper {
    private static final Map<String, String> PERMISSION_ROLE_MAP = Map.of(
        "PERM_VIEW", "USER",
        "PERM_ADMIN", "ADMIN"
    );
    public Set<String> mapPermissionIdsToRoles(List<String> permissionIds) {
        Set<String> roles = new HashSet<>();
        for (String pid : permissionIds) {
            String role = PERMISSION_ROLE_MAP.get(pid);
            if (role != null) roles.add(role);
        }
        return roles;
    }
}