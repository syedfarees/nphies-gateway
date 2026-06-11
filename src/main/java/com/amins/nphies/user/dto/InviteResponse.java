package com.amins.nphies.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
public class InviteResponse {
    private String email;
    private String otp;          // null when the OTP was emailed; otherwise returned once for out-of-band delivery
    private OffsetDateTime expiresAt;
    private boolean emailSent;
}
