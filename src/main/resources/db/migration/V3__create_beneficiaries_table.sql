CREATE TABLE beneficiaries (
    id                    BIGINT        AUTO_INCREMENT PRIMARY KEY,
    tenant_id             VARCHAR(100)  NOT NULL,
    national_id           VARCHAR(50)   NOT NULL,
    id_type               VARCHAR(20)   NOT NULL DEFAULT 'NATIONAL_ID',
    first_name            VARCHAR(100)  NOT NULL,
    family_name           VARCHAR(100)  NOT NULL,
    date_of_birth         DATE          NOT NULL,
    gender                VARCHAR(10)   NOT NULL,
    member_id             VARCHAR(100),
    payer_license_no      VARCHAR(100),
    payer_name            VARCHAR(255),
    coverage_relationship VARCHAR(50)   NOT NULL DEFAULT 'self',
    phone                 VARCHAR(20),
    email                 VARCHAR(255),
    active                BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at            DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at            DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_beneficiary_tenant FOREIGN KEY (tenant_id) REFERENCES tenant_nphies_config (tenant_id),
    CONSTRAINT uq_beneficiary_tenant_national_id UNIQUE (tenant_id, national_id),
    CONSTRAINT chk_beneficiary_id_type CHECK (id_type IN ('NATIONAL_ID', 'IQAMA')),
    CONSTRAINT chk_beneficiary_gender CHECK (gender IN ('male', 'female', 'unknown'))
);

CREATE INDEX idx_beneficiaries_national_id ON beneficiaries (national_id);
CREATE INDEX idx_beneficiaries_tenant_active ON beneficiaries (tenant_id, active);
