-- Forgot-password flow: a short-lived hashed OTP per user, emailed on request
-- and redeemed together with the new password.
CREATE TABLE password_reset_tokens (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    otp_hash    VARCHAR(255) NOT NULL,
    attempts    INT          NOT NULL DEFAULT 0,
    expires_at  DATETIME(6)  NOT NULL,
    used_at     DATETIME(6),
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_reset_token_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
