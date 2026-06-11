-- Practitioners (doctors/care team)
CREATE TABLE practitioners (
    id                    BIGINT       AUTO_INCREMENT PRIMARY KEY,
    tenant_id             VARCHAR(100) NOT NULL,
    practitioner_license  VARCHAR(100) NOT NULL,
    first_name            VARCHAR(100) NOT NULL,
    family_name           VARCHAR(100) NOT NULL,
    specialty_code        VARCHAR(50),
    role                  VARCHAR(50)  NOT NULL DEFAULT 'DOCTOR',
    active                BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_practitioner_tenant FOREIGN KEY (tenant_id) REFERENCES tenant_nphies_config (tenant_id),
    CONSTRAINT uq_practitioner_tenant_license UNIQUE (tenant_id, practitioner_license)
);

-- Organizations (payers/insurers)
CREATE TABLE organizations (
    id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
    tenant_id     VARCHAR(100) NOT NULL,
    license_no    VARCHAR(100) NOT NULL,
    org_type      VARCHAR(20)  NOT NULL DEFAULT 'INSURER',
    name          VARCHAR(255) NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_org_tenant FOREIGN KEY (tenant_id) REFERENCES tenant_nphies_config (tenant_id),
    CONSTRAINT uq_org_tenant_license UNIQUE (tenant_id, license_no),
    CONSTRAINT chk_org_type CHECK (org_type IN ('INSURER', 'PROVIDER'))
);

-- Coverage (insurance policies)
CREATE TABLE coverages (
    id                    BIGINT       AUTO_INCREMENT PRIMARY KEY,
    tenant_id             VARCHAR(100) NOT NULL,
    beneficiary_id        BIGINT       NOT NULL,
    member_id             VARCHAR(100) NOT NULL,
    subscriber_id         VARCHAR(100),
    payer_license_no      VARCHAR(100) NOT NULL,
    payer_name            VARCHAR(255) NOT NULL,
    coverage_relationship VARCHAR(50)  NOT NULL DEFAULT 'self',
    period_start          DATE,
    period_end            DATE,
    class_value           VARCHAR(100),
    class_name            VARCHAR(100),
    order_of_benefit      INT          NOT NULL DEFAULT 1,
    status                VARCHAR(20)  NOT NULL DEFAULT 'active',
    active                BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_coverage_tenant FOREIGN KEY (tenant_id) REFERENCES tenant_nphies_config (tenant_id),
    CONSTRAINT fk_coverage_beneficiary FOREIGN KEY (beneficiary_id) REFERENCES beneficiaries (id),
    CONSTRAINT chk_coverage_status CHECK (status IN ('active', 'cancelled', 'draft', 'entered-in-error'))
);

-- Encounters (clinical visit/admission)
CREATE TABLE encounters (
    id                    BIGINT       AUTO_INCREMENT PRIMARY KEY,
    tenant_id             VARCHAR(100) NOT NULL,
    beneficiary_id        BIGINT       NOT NULL,
    practitioner_id       BIGINT,
    encounter_class       VARCHAR(20)  NOT NULL DEFAULT 'AMB',
    service_type          VARCHAR(50),
    priority              VARCHAR(20)  NOT NULL DEFAULT 'normal',
    period_start          DATETIME(6)  NOT NULL,
    period_end            DATETIME(6),
    admission_source      VARCHAR(50),
    discharge_disposition VARCHAR(50),
    service_provider_id   BIGINT,
    status                VARCHAR(30)  NOT NULL DEFAULT 'finished',
    active                BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_encounter_tenant FOREIGN KEY (tenant_id) REFERENCES tenant_nphies_config (tenant_id),
    CONSTRAINT fk_encounter_beneficiary FOREIGN KEY (beneficiary_id) REFERENCES beneficiaries (id),
    CONSTRAINT fk_encounter_practitioner FOREIGN KEY (practitioner_id) REFERENCES practitioners (id),
    CONSTRAINT fk_encounter_service_provider FOREIGN KEY (service_provider_id) REFERENCES organizations (id),
    CONSTRAINT chk_encounter_class CHECK (encounter_class IN ('AMB','EMER','IMP','SS','HH'))
);

-- Claims
CREATE TABLE claims (
    id                    BIGINT       AUTO_INCREMENT PRIMARY KEY,
    tenant_id             VARCHAR(100) NOT NULL,
    claim_id              VARCHAR(100) NOT NULL UNIQUE,
    use_type              VARCHAR(30)  NOT NULL DEFAULT 'claim',
    claim_type            VARCHAR(30)  NOT NULL DEFAULT 'institutional',
    priority              VARCHAR(20)  NOT NULL DEFAULT 'normal',
    beneficiary_id        BIGINT       NOT NULL,
    coverage_id           BIGINT       NOT NULL,
    encounter_id          BIGINT,
    insurer_org_id        BIGINT       NOT NULL,
    billable_period_start DATE         NOT NULL,
    billable_period_end   DATE         NOT NULL,
    total_net             DECIMAL(12,2),
    total_gross           DECIMAL(12,2),
    currency              VARCHAR(3)   NOT NULL DEFAULT 'SAR',
    submission_status     VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    nphies_bundle_id      VARCHAR(255),
    submitted_at          DATETIME(6),
    created_at            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_claim_tenant FOREIGN KEY (tenant_id) REFERENCES tenant_nphies_config (tenant_id),
    CONSTRAINT fk_claim_beneficiary FOREIGN KEY (beneficiary_id) REFERENCES beneficiaries (id),
    CONSTRAINT fk_claim_coverage FOREIGN KEY (coverage_id) REFERENCES coverages (id),
    CONSTRAINT fk_claim_encounter FOREIGN KEY (encounter_id) REFERENCES encounters (id),
    CONSTRAINT fk_claim_insurer_org FOREIGN KEY (insurer_org_id) REFERENCES organizations (id),
    CONSTRAINT chk_claim_use CHECK (use_type IN ('claim','preauthorization','predetermination')),
    CONSTRAINT chk_claim_type CHECK (claim_type IN ('institutional','professional','oral','pharmacy','vision')),
    CONSTRAINT chk_claim_submission CHECK (submission_status IN ('PENDING','SUBMITTED','ERROR'))
);

-- Claim Care Team
CREATE TABLE claim_care_team (
    id               BIGINT      AUTO_INCREMENT PRIMARY KEY,
    claim_id         BIGINT      NOT NULL,
    sequence         INT         NOT NULL,
    practitioner_id  BIGINT      NOT NULL,
    role_code        VARCHAR(50) NOT NULL DEFAULT 'primary',
    qualification    VARCHAR(100),
    CONSTRAINT fk_careteam_claim FOREIGN KEY (claim_id) REFERENCES claims (id) ON DELETE CASCADE,
    CONSTRAINT fk_careteam_practitioner FOREIGN KEY (practitioner_id) REFERENCES practitioners (id),
    CONSTRAINT uq_claim_careteam_seq UNIQUE (claim_id, sequence)
);

-- Claim Diagnoses
CREATE TABLE claim_diagnoses (
    id                BIGINT       AUTO_INCREMENT PRIMARY KEY,
    claim_id          BIGINT       NOT NULL,
    sequence          INT          NOT NULL,
    icd10_code        VARCHAR(20)  NOT NULL,
    icd10_display     VARCHAR(255),
    diagnosis_type    VARCHAR(50)  NOT NULL DEFAULT 'principal',
    on_admission      VARCHAR(10),
    CONSTRAINT fk_diagnosis_claim FOREIGN KEY (claim_id) REFERENCES claims (id) ON DELETE CASCADE,
    CONSTRAINT uq_claim_diagnosis_seq UNIQUE (claim_id, sequence)
);

-- Claim Items
-- care_team_sequences / diagnosis_sequences / modifier_codes are JSON arrays
-- (MySQL has no native array type; entities map them via SqlTypes.JSON)
CREATE TABLE claim_items (
    id                     BIGINT        AUTO_INCREMENT PRIMARY KEY,
    claim_id               BIGINT        NOT NULL,
    sequence               INT           NOT NULL,
    care_team_sequences    JSON,
    diagnosis_sequences    JSON,
    product_service_code   VARCHAR(50)   NOT NULL,
    product_service_system VARCHAR(255),
    serviced_date          DATE          NOT NULL,
    quantity               DECIMAL(10,2) NOT NULL DEFAULT 1,
    unit_price             DECIMAL(12,2) NOT NULL,
    net_amount             DECIMAL(12,2) NOT NULL,
    body_site_code         VARCHAR(50),
    modifier_codes         JSON,
    CONSTRAINT fk_item_claim FOREIGN KEY (claim_id) REFERENCES claims (id) ON DELETE CASCADE,
    CONSTRAINT uq_claim_item_seq UNIQUE (claim_id, sequence)
);

-- Claim Responses
CREATE TABLE claim_responses (
    id                    BIGINT       AUTO_INCREMENT PRIMARY KEY,
    tenant_id             VARCHAR(100) NOT NULL,
    claim_id              BIGINT       NOT NULL,
    nphies_response_id    VARCHAR(255),
    outcome               VARCHAR(20)  NOT NULL,
    disposition           TEXT,
    total_benefit         DECIMAL(12,2),
    total_submitted       DECIMAL(12,2),
    currency              VARCHAR(3)   NOT NULL DEFAULT 'SAR',
    payment_amount        DECIMAL(12,2),
    payment_date          DATE,
    raw_response_json     TEXT,
    received_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_response_tenant FOREIGN KEY (tenant_id) REFERENCES tenant_nphies_config (tenant_id),
    CONSTRAINT fk_response_claim FOREIGN KEY (claim_id) REFERENCES claims (id)
);

-- Indexes (FK columns already get indexes from the constraints above)
CREATE INDEX idx_claims_status ON claims (submission_status);
