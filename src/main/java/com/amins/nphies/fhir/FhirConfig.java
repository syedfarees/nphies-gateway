package com.amins.nphies.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers HAPI FHIR R4 beans.
 *
 * FhirContext is expensive to create (scans classpath for model classes) —
 * kept as a singleton bean and shared across all FHIR builders/parsers.
 */
@Configuration
public class FhirConfig {

    @Bean
    public FhirContext fhirContext() {
        FhirContext ctx = FhirContext.forR4();
        // Suppress noisy HAPI startup banner
        ctx.getVersion(); // warm up model scan at startup, not at first request
        return ctx;
    }

    /** Thread-safe JSON serialiser/deserialiser for FHIR R4 resources. */
    @Bean
    public IParser fhirJsonParser(FhirContext fhirContext) {
        return fhirContext.newJsonParser().setPrettyPrint(false);
    }
}
