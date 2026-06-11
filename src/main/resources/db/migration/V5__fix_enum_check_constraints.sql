-- Fix coverage status CHECK to match Java enum names (uppercase)
ALTER TABLE coverages DROP CHECK chk_coverage_status;
ALTER TABLE coverages ADD CONSTRAINT chk_coverage_status
    CHECK (status IN ('ACTIVE', 'CANCELLED', 'DRAFT', 'ENTERED_IN_ERROR'));

-- Fix claim use_type CHECK to match Java enum names
ALTER TABLE claims DROP CHECK chk_claim_use;
ALTER TABLE claims ADD CONSTRAINT chk_claim_use
    CHECK (use_type IN ('CLAIM', 'PREAUTHORIZATION', 'PREDETERMINATION'));

-- Fix claim claim_type CHECK to match Java enum names
ALTER TABLE claims DROP CHECK chk_claim_type;
ALTER TABLE claims ADD CONSTRAINT chk_claim_type
    CHECK (claim_type IN ('INSTITUTIONAL', 'PROFESSIONAL', 'ORAL', 'PHARMACY', 'VISION'));
