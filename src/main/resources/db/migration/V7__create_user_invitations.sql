-- Invite-based registration: a tenant ADMIN creates an invitation carrying the
-- tenant and role; the invitee redeems it with a one-time code (OTP) to register.
CREATE TABLE user_invitations (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    tenant_id    VARCHAR(100) NOT NULL,
    email        VARCHAR(255) NOT NULL,
    role         VARCHAR(50)  NOT NULL DEFAULT 'USER',
    otp_hash     VARCHAR(255) NOT NULL,
    invited_by   BIGINT       NOT NULL,
    attempts     INT          NOT NULL DEFAULT 0,
    expires_at   DATETIME(6)  NOT NULL,
    accepted_at  DATETIME(6),
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_invitation_tenant FOREIGN KEY (tenant_id) REFERENCES tenant_nphies_config (tenant_id),
    CONSTRAINT fk_invitation_inviter FOREIGN KEY (invited_by) REFERENCES users (id),
    CONSTRAINT chk_invitation_role CHECK (role IN ('USER', 'ADMIN'))
);

CREATE INDEX idx_invitations_email ON user_invitations (email);
