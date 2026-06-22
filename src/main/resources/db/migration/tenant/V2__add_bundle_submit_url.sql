-- Optional override for the FHIR Bundle submit URL.
-- When set, NphiesGatewayClient uses this URL directly instead of
-- constructing apiBaseUrl + '/Bundle'. Useful for test servers that
-- expose a single $process-message endpoint rather than /r4/Bundle.
ALTER TABLE tenant_nphies_config
    ADD COLUMN bundle_submit_url VARCHAR(500) NULL AFTER api_base_url;

-- When true, skips OAuth2 token fetch and sends requests without a
-- Bearer token. Used for IP-whitelisted test environments where the
-- server authenticates by source IP rather than credentials.
ALTER TABLE tenant_nphies_config
    ADD COLUMN skip_token_auth TINYINT(1) NOT NULL DEFAULT 0 AFTER bundle_submit_url;
