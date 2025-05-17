package com.example.app.auth;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ExternalPrivilegeService {
    public List<String> getUserPermissionIds(String userId) {
        // TODO: Replace with actual external privilege service call
        return List.of("PERM_VIEW", "PERM_ADMIN");
    }
}