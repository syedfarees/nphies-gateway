package com.amins.nphies.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank
    private String tenantId;
    @Email @NotBlank
    private String email;
    @NotBlank
    private String password;
}
