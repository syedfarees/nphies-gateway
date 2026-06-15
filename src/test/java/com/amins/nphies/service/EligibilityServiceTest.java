package com.amins.nphies.service;

import com.amins.nphies.config.TenantNphiesConfig;
import com.amins.nphies.exception.NphiesException;
import com.amins.nphies.fhir.bundle.CoverageEligibilityRequestBundleBuilder;
import com.amins.nphies.fhir.bundle.EligibilityRequestInput;
import com.amins.nphies.fhir.response.CoverageEligibilityResponseMapper;
import com.amins.nphies.fhir.response.EligibilityResponse;
import com.amins.nphies.gateway.NphiesGatewayClient;
import com.amins.nphies.model.TenantContext;
import com.amins.nphies.repository.TenantNphiesConfigRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EligibilityServiceTest {

    @Mock TenantNphiesConfigRepository        configRepository;
    @Mock CoverageEligibilityRequestBundleBuilder bundleBuilder;
    @Mock NphiesGatewayClient                 gatewayClient;
    @Mock CoverageEligibilityResponseMapper   responseMapper;

    @InjectMocks EligibilityService eligibilityService;

    private static final String TENANT_ID        = "hospital-abc";
    private static final String API_BASE_URL     = "https://HSB.nphies.sa/r4";
    private static final String PROVIDER_LICENSE = "N-F-0000001";
    private static final String BUNDLE_JSON      = "{\"resourceType\":\"Bundle\"}";
    private static final String RESPONSE_JSON    = "{\"resourceType\":\"Bundle\",\"type\":\"message\"}";

    @BeforeEach
    void setTenantContext() {
        TenantContext.set(TENANT_ID);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void checkEligibility_happyPath_returnsResponse() {
        stubConfig();
        when(bundleBuilder.build(any())).thenReturn(BUNDLE_JSON);
        when(gatewayClient.submitBundle(any(), any(), any())).thenReturn(RESPONSE_JSON);
        EligibilityResponse expected = completeResponse();
        when(responseMapper.map(any(), any())).thenReturn(expected);

        EligibilityResponse result = eligibilityService.checkEligibility(minimalRequest().build());

        assertThat(result).isSameAs(expected);
    }

    @Test
    void checkEligibility_happyPath_returnsExpectedOutcome() {
        stubConfig();
        when(bundleBuilder.build(any())).thenReturn(BUNDLE_JSON);
        when(gatewayClient.submitBundle(any(), any(), any())).thenReturn(RESPONSE_JSON);
        when(responseMapper.map(any(), any())).thenReturn(EligibilityResponse.builder()
                .requestId("req-1")
                .outcome(EligibilityResponse.EligibilityOutcome.COMPLETE)
                .benefits(List.of())
                .build());

        EligibilityResponse result = eligibilityService.checkEligibility(minimalRequest().build());

        assertThat(result.getOutcome()).isEqualTo(EligibilityResponse.EligibilityOutcome.COMPLETE);
    }

    // ── Config loading ────────────────────────────────────────────────────────

    @Test
    void checkEligibility_noActiveConfig_throwsNphiesException() {
        when(configRepository.findByTenantIdAndActiveTrue(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                eligibilityService.checkEligibility(minimalRequest().build()))
                .isInstanceOf(NphiesException.class)
                .hasMessageContaining(TENANT_ID);
    }

    @Test
    void checkEligibility_noActiveConfig_gatewayNeverCalled() {
        when(configRepository.findByTenantIdAndActiveTrue(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                eligibilityService.checkEligibility(minimalRequest().build()));
        verifyNoInteractions(gatewayClient);
    }

    // ── Provider mapping ──────────────────────────────────────────────────────

    @Test
    void checkEligibility_providerLicenseFromConfig() {
        stubConfig();
        when(bundleBuilder.build(any())).thenReturn(BUNDLE_JSON);
        when(gatewayClient.submitBundle(any(), any(), any())).thenReturn(RESPONSE_JSON);
        when(responseMapper.map(any(), any())).thenReturn(completeResponse());

        ArgumentCaptor<EligibilityRequestInput> captor =
                ArgumentCaptor.forClass(EligibilityRequestInput.class);
        eligibilityService.checkEligibility(minimalRequest().build());

        verify(bundleBuilder).build(captor.capture());
        assertThat(captor.getValue().getProviderLicenseNumber()).isEqualTo(PROVIDER_LICENSE);
    }

    @Test
    void checkEligibility_providerNameIsTenantId() {
        stubConfig();
        when(bundleBuilder.build(any())).thenReturn(BUNDLE_JSON);
        when(gatewayClient.submitBundle(any(), any(), any())).thenReturn(RESPONSE_JSON);
        when(responseMapper.map(any(), any())).thenReturn(completeResponse());

        ArgumentCaptor<EligibilityRequestInput> captor =
                ArgumentCaptor.forClass(EligibilityRequestInput.class);
        eligibilityService.checkEligibility(minimalRequest().build());

        verify(bundleBuilder).build(captor.capture());
        assertThat(captor.getValue().getProviderName()).isEqualTo(TENANT_ID);
    }

    // ── Caller fields ─────────────────────────────────────────────────────────

    @Test
    void checkEligibility_payerFieldsPassedThrough() {
        stubConfig();
        when(bundleBuilder.build(any())).thenReturn(BUNDLE_JSON);
        when(gatewayClient.submitBundle(any(), any(), any())).thenReturn(RESPONSE_JSON);
        when(responseMapper.map(any(), any())).thenReturn(completeResponse());

        EligibilityCheckRequest req = minimalRequest()
                .payerLicenseNumber("INS-9999")
                .payerName("Test Insurer")
                .build();

        ArgumentCaptor<EligibilityRequestInput> captor =
                ArgumentCaptor.forClass(EligibilityRequestInput.class);
        eligibilityService.checkEligibility(req);

        verify(bundleBuilder).build(captor.capture());
        assertThat(captor.getValue().getPayerLicenseNumber()).isEqualTo("INS-9999");
        assertThat(captor.getValue().getPayerName()).isEqualTo("Test Insurer");
    }

    @Test
    void checkEligibility_patientFieldsPassedThrough() {
        stubConfig();
        when(bundleBuilder.build(any())).thenReturn(BUNDLE_JSON);
        when(gatewayClient.submitBundle(any(), any(), any())).thenReturn(RESPONSE_JSON);
        when(responseMapper.map(any(), any())).thenReturn(completeResponse());

        EligibilityCheckRequest req = minimalRequest()
                .patientNationalId("1987654321")
                .patientFirstName("Fatima")
                .patientFamilyName("Al-Rashid")
                .patientDateOfBirth(LocalDate.of(1985, 3, 20))
                .patientGender("female")
                .build();

        ArgumentCaptor<EligibilityRequestInput> captor =
                ArgumentCaptor.forClass(EligibilityRequestInput.class);
        eligibilityService.checkEligibility(req);

        verify(bundleBuilder).build(captor.capture());
        assertThat(captor.getValue().getPatientNationalId()).isEqualTo("1987654321");
        assertThat(captor.getValue().getPatientFirstName()).isEqualTo("Fatima");
        assertThat(captor.getValue().getPatientGender()).isEqualTo("female");
    }

    // ── Gateway interaction ───────────────────────────────────────────────────

    @Test
    void checkEligibility_calledWithApiBaseUrl() {
        stubConfig();
        when(bundleBuilder.build(any())).thenReturn(BUNDLE_JSON);
        when(gatewayClient.submitBundle(any(), any(), any())).thenReturn(RESPONSE_JSON);
        when(responseMapper.map(any(), any())).thenReturn(completeResponse());

        eligibilityService.checkEligibility(minimalRequest().build());

        verify(gatewayClient).submitBundle(eq(TENANT_ID), eq(API_BASE_URL), any());
    }

    @Test
    void checkEligibility_responsePassedToMapper() {
        stubConfig();
        when(bundleBuilder.build(any())).thenReturn(BUNDLE_JSON);
        when(gatewayClient.submitBundle(any(), any(), any())).thenReturn(RESPONSE_JSON);
        when(responseMapper.map(any(), any())).thenReturn(completeResponse());

        eligibilityService.checkEligibility(minimalRequest().build());

        verify(responseMapper).map(any(), eq(RESPONSE_JSON));
    }

    @Test
    void checkEligibility_requestIdPassedToMapper() {
        stubConfig();
        when(bundleBuilder.build(any())).thenReturn(BUNDLE_JSON);
        when(gatewayClient.submitBundle(any(), any(), any())).thenReturn(RESPONSE_JSON);
        when(responseMapper.map(any(), any())).thenReturn(completeResponse());

        eligibilityService.checkEligibility(minimalRequest().requestId("specific-req-id").build());

        verify(responseMapper).map(eq("specific-req-id"), any());
    }

    // ── Error propagation ─────────────────────────────────────────────────────

    @Test
    void checkEligibility_nonRetryableException_propagates() {
        stubConfig();
        when(bundleBuilder.build(any())).thenReturn(BUNDLE_JSON);
        when(gatewayClient.submitBundle(any(), any(), any()))
                .thenThrow(new NphiesException.NonRetryable("bad request"));

        assertThatThrownBy(() ->
                eligibilityService.checkEligibility(minimalRequest().build()))
                .isInstanceOf(NphiesException.NonRetryable.class);
    }

    @Test
    void checkEligibility_retryableException_propagates() {
        stubConfig();
        when(bundleBuilder.build(any())).thenReturn(BUNDLE_JSON);
        when(gatewayClient.submitBundle(any(), any(), any()))
                .thenThrow(new NphiesException.Retryable("server error"));

        assertThatThrownBy(() ->
                eligibilityService.checkEligibility(minimalRequest().build()))
                .isInstanceOf(NphiesException.Retryable.class);
    }

    @Test
    void checkEligibility_illegalArgFromBuilder_propagates() {
        stubConfig();
        when(bundleBuilder.build(any())).thenThrow(new IllegalArgumentException("bad gender"));

        assertThatThrownBy(() ->
                eligibilityService.checkEligibility(minimalRequest().build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── PENDING passthrough ───────────────────────────────────────────────────

    @Test
    void checkEligibility_mapperReturnsPending_serviceReturnsPending() {
        stubConfig();
        when(bundleBuilder.build(any())).thenReturn(BUNDLE_JSON);
        when(gatewayClient.submitBundle(any(), any(), any())).thenReturn("");
        when(responseMapper.map(any(), any())).thenReturn(EligibilityResponse.builder()
                .requestId("req-1")
                .outcome(EligibilityResponse.EligibilityOutcome.PENDING)
                .benefits(List.of())
                .build());

        EligibilityResponse result = eligibilityService.checkEligibility(minimalRequest().build());

        assertThat(result.getOutcome()).isEqualTo(EligibilityResponse.EligibilityOutcome.PENDING);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void stubConfig() {
        TenantNphiesConfig config = TenantNphiesConfig.builder()
                .tenantId(TENANT_ID)
                .clientId("client-001")
                .clientSecretEncrypted("encrypted-secret")
                .providerLicenseNo(PROVIDER_LICENSE)
                .tokenEndpoint("https://HSB.nphies.sa/auth/token")
                .apiBaseUrl(API_BASE_URL)
                .environment(TenantNphiesConfig.NphiesEnvironment.UAT)
                .build();
        when(configRepository.findByTenantIdAndActiveTrue(TENANT_ID)).thenReturn(Optional.of(config));
    }

    private EligibilityCheckRequest.EligibilityCheckRequestBuilder minimalRequest() {
        return EligibilityCheckRequest.builder()
                .requestId("req-1")
                .patientNationalId("1234567890")
                .patientFirstName("Ahmed")
                .patientFamilyName("Al-Test")
                .patientDateOfBirth(LocalDate.of(1990, 6, 15))
                .patientGender("male")
                .memberId("MEM-001")
                .payerLicenseNumber("INS-0000001")
                .payerName("Test Insurance Co");
    }

    private EligibilityResponse completeResponse() {
        return EligibilityResponse.builder()
                .requestId("req-1")
                .outcome(EligibilityResponse.EligibilityOutcome.COMPLETE)
                .benefits(List.of())
                .build();
    }
}
