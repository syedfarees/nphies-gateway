-- System schema: tenant registry only.
-- All tenant data lives in per-tenant schemas (nphies_{tenantId}).

CREATE TABLE IF NOT EXISTS tenant_registry (
    tenant_id    VARCHAR(100)  NOT NULL PRIMARY KEY,
    schema_name  VARCHAR(120)  NOT NULL UNIQUE,
    display_name VARCHAR(255)  NOT NULL,
    status       VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_tenant_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DEPROVISIONED'))
);
