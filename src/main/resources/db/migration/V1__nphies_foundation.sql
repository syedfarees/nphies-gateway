-- V1__nphies_foundation.sql (MySQL 8.0.16+)
-- Place in: src/main/resources/db/migration/

CREATE TABLE tenant_nphies_config (
      tenant_id                VARCHAR(100)  PRIMARY KEY,
      client_id                VARCHAR(255)  NOT NULL,
      client_secret_encrypted  VARCHAR(1024) NOT NULL,  -- AES-256-GCM ciphertext
      provider_license_no      VARCHAR(100)  NOT NULL,
      token_endpoint           VARCHAR(500)  NOT NULL,
      api_base_url             VARCHAR(500)  NOT NULL,
      environment              VARCHAR(20)   NOT NULL,
      polling_interval_sec     INT           NOT NULL DEFAULT 60,
      active                   BOOLEAN       NOT NULL DEFAULT TRUE,
      external_secret_ref      VARCHAR(1000),           -- GCP Secret Manager ref or Vault path
      created_at               DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
      updated_at               DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

      CONSTRAINT uq_tenant_provider_license UNIQUE (tenant_id, provider_license_no),
      CONSTRAINT chk_nphies_environment CHECK (environment IN ('UAT', 'PRODUCTION')),
      CONSTRAINT chk_polling_interval CHECK (polling_interval_sec >= 30)
);

CREATE INDEX idx_nphies_config_active     ON tenant_nphies_config (active);
CREATE INDEX idx_nphies_config_env        ON tenant_nphies_config (environment);
