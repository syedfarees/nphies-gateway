package com.amins.nphies.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String name;
    private String email;
    private String token;
    private String role;
    private String tenantId;
}
