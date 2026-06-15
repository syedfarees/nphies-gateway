package com.amins.nphies.claim;

import com.amins.nphies.beneficiary.Beneficiary;
import com.amins.nphies.beneficiary.BeneficiaryRepository;
import com.amins.nphies.claim.dto.ClaimRequest;
import com.amins.nphies.claim.dto.ClaimResponseDto;
import com.amins.nphies.claim.dto.ClaimSummaryResponse;
import com.amins.nphies.config.TenantNphiesConfig;
import com.amins.nphies.coverage.Coverage;
import com.amins.nphies.coverage.CoverageRepository;
import com.amins.nphies.encounter.EncounterRepository;
import com.amins.nphies.exception.NphiesException;
import com.amins.nphies.fhir.bundle.ClaimBundleBuilder;
import com.amins.nphies.fhir.response.ClaimResponseMapper;
import com.amins.nphies.gateway.NphiesGatewayClient;
import com.amins.nphies.model.TenantContext;
import com.amins.nphies.organization.Organization;
import com.amins.nphies.organization.OrganizationRepository;
import com.amins.nphies.practitioner.PractitionerRepository;
import com.amins.nphies.repository.TenantNphiesConfigRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock TenantNphiesConfigRepository configRepository;
    @Mock BeneficiaryRepository        beneficiaryRepository;
    @Mock CoverageRepository           coverageRepository;
    @Mock EncounterRepository          encounterRepository;
    @Mock OrganizationRepository       organizationRepository;
    @Mock PractitionerRepository       practitionerRepository;
    @Mock ClaimRepository              claimRepository;
    @Mock ClaimItemRepository          claimItemRepository;
    @Mock ClaimCareTeamRepository      claimCareTeamRepository;
    @Mock ClaimDiagnosisRepository     claimDiagnosisRepository;
    @Mock ClaimResponseRepository      claimResponseRepository;
    @Mock ClaimBundleBuilder           claimBundleBuilder;
    @Mock NphiesGatewayClient          gatewayClient;
    @Mock ClaimResponseMapper          claimResponseMapper;

    @InjectMocks ClaimService claimService;

    private static final String TENANT_ID    = "hospital-abc";
    private static final String API_BASE_URL = "https://HSB.nphies.sa/r4";
    private static final String BUNDLE_JSON  = "{\"resourceType\":\"Bundle\"}";
    private static final String RESPONSE_JSON = "{\"resourceType\":\"Bundle\",\"id\":\"resp-bundle-1\"}";

    @BeforeEach
    void setTenantContext() {
        TenantContext.set(TENANT_ID);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    // ── Happy path ─────────────────────────────────────────────────────────────

    @Test
    void submitClaim_happyPath_returnsSubmittedStatus() {
        stubConfig();
        stubBeneficiary();
        stubCoverage();
        stubInsurer();
        stubClaimSave();
        when(claimBundleBuilder.build(any())).thenReturn(BUNDLE_JSON);
        when(gatewayClient.submitBundle(any(), any(), any())).thenReturn(RESPONSE_JSON);
        ClaimResponseDto responseDto = ClaimResponseDto.builder()
                .bundleId("resp-bundle-1")
                .outcome("complete")
                .receivedAt(OffsetDateTime.now())
                .currency("SAR")
                .build();
        when(claimResponseMapper.map(any(), any())).thenReturn(responseDto);
        when(claimResponseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ClaimSummaryResponse result = claimService.submitClaim(minimalRequest());

        assertThat(result.getSubmissionStatus()).isEqualTo(Claim.SubmissionStatus.SUBMITTED);
        verify(claimRepository, atLeast(2)).save(any());
    }

    @Test
    void submitClaim_happyPath_nphiesBundleIdIsSet() {
        stubConfig();
        stubBeneficiary();
        stubCoverage();
        stubInsurer();
        stubClaimSave();
        when(claimBundleBuilder.build(any())).thenReturn(BUNDLE_JSON);
        when(gatewayClient.submitBundle(any(), any(), any())).thenReturn(RESPONSE_JSON);
        ClaimResponseDto responseDto = ClaimResponseDto.builder()
                .bundleId("resp-bundle-1")
                .outcome("complete")
                .receivedAt(OffsetDateTime.now())
                .currency("SAR")
                .build();
        when(claimResponseMapper.map(any(), any())).thenReturn(responseDto);
        when(claimResponseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        claimService.submitClaim(minimalRequest());

        // Verify that a claim was saved with nphiesBundleId set
        verify(claimRepository, atLeast(2)).save(argThat(c -> "resp-bundle-1".equals(c.getNphiesBundleId())));
    }

    // ── Gateway failure ────────────────────────────────────────────────────────

    @Test
    void submitClaim_gatewayThrows_statusIsError() {
        stubConfig();
        stubBeneficiary();
        stubCoverage();
        stubInsurer();
        stubClaimSave();
        when(claimBundleBuilder.build(any())).thenReturn(BUNDLE_JSON);
        when(gatewayClient.submitBundle(any(), any(), any()))
                .thenThrow(new NphiesException.Retryable("gateway down"));
        when(claimResponseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ClaimSummaryResponse result = claimService.submitClaim(minimalRequest());

        assertThat(result.getSubmissionStatus()).isEqualTo(Claim.SubmissionStatus.ERROR);
    }

    @Test
    void submitClaim_gatewayThrows_errorResponseIsPersisted() {
        stubConfig();
        stubBeneficiary();
        stubCoverage();
        stubInsurer();
        stubClaimSave();
        when(claimBundleBuilder.build(any())).thenReturn(BUNDLE_JSON);
        when(gatewayClient.submitBundle(any(), any(), any()))
                .thenThrow(new RuntimeException("timeout"));
        when(claimResponseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        claimService.submitClaim(minimalRequest());

        verify(claimResponseRepository).save(argThat(r -> "error".equals(r.getOutcome())));
    }

    // ── Config / entity not found ──────────────────────────────────────────────

    @Test
    void submitClaim_tenantNotFound_throwsNphiesException() {
        when(configRepository.findByTenantIdAndActiveTrue(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> claimService.submitClaim(minimalRequest()))
                .isInstanceOf(NphiesException.class)
                .hasMessageContaining(TENANT_ID);
    }

    @Test
    void submitClaim_beneficiaryNotFound_throws404() {
        stubConfig();
        when(beneficiaryRepository.findByIdAndActiveTrue(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> claimService.submitClaim(minimalRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Beneficiary not found");
    }

    // ── getClaimDetail ─────────────────────────────────────────────────────────

    @Test
    void getClaimDetail_notFound_throws404() {
        when(claimRepository.findByClaimId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> claimService.getClaimDetail("unknown-claim"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Claim not found");
    }

    // ── pollClaimResponse ──────────────────────────────────────────────────────

    @Test
    void pollClaimResponse_noBundleId_throws400() {
        Claim claim = new Claim();
        claim.setClaimId("claim-1");
        claim.setNphiesBundleId(null);
        when(claimRepository.findByClaimId("claim-1"))
                .thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> claimService.pollClaimResponse("claim-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no NPHIES bundle ID");
    }

    @Test
    void pollClaimResponse_happyPath_returnsDto() {
        Claim claim = new Claim();
        claim.setId(1L);
        claim.setClaimId("claim-1");
        claim.setNphiesBundleId("bundle-xyz");
        when(claimRepository.findByClaimId("claim-1"))
                .thenReturn(Optional.of(claim));
        stubConfig();
        when(gatewayClient.pollBundleResponse(any(), any(), eq("bundle-xyz")))
                .thenReturn(RESPONSE_JSON);
        ClaimResponseDto expected = ClaimResponseDto.builder()
                .bundleId("bundle-xyz")
                .outcome("complete")
                .receivedAt(OffsetDateTime.now())
                .currency("SAR")
                .build();
        when(claimResponseMapper.map(eq("claim-1"), any())).thenReturn(expected);
        when(claimResponseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ClaimResponseDto result = claimService.pollClaimResponse("claim-1");

        assertThat(result).isSameAs(expected);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void stubConfig() {
        TenantNphiesConfig config = TenantNphiesConfig.builder()
                .tenantId(TENANT_ID)
                .clientId("client-001")
                .clientSecretEncrypted("encrypted-secret")
                .providerLicenseNo("N-F-0000001")
                .tokenEndpoint("https://HSB.nphies.sa/auth/token")
                .apiBaseUrl(API_BASE_URL)
                .environment(TenantNphiesConfig.NphiesEnvironment.UAT)
                .build();
        when(configRepository.findByTenantIdAndActiveTrue(TENANT_ID)).thenReturn(Optional.of(config));
    }

    private void stubBeneficiary() {
        Beneficiary b = new Beneficiary();
        b.setId(10L);
        b.setNationalId("1234567890");
        b.setFirstName("Ahmed");
        b.setFamilyName("Al-Test");
        b.setDateOfBirth(LocalDate.of(1990, 6, 15));
        b.setGender("male");
        b.setMemberId("MEM-001");
        b.setPayerLicenseNo("INS-0000001");
        b.setPayerName("Test Insurance Co");
        when(beneficiaryRepository.findByIdAndActiveTrue(any()))
                .thenReturn(Optional.of(b));
    }

    private void stubCoverage() {
        Coverage c = new Coverage();
        c.setId(20L);
        c.setMemberId("MEM-001");
        c.setPayerLicenseNo("INS-0000001");
        c.setPayerName("Test Insurance Co");
        c.setCoverageRelationship("self");
        when(coverageRepository.findByIdAndActiveTrue(any()))
                .thenReturn(Optional.of(c));
    }

    private void stubInsurer() {
        Organization org = new Organization();
        org.setId(30L);
        org.setLicenseNo("INS-0000001");
        org.setName("Test Insurance Co");
        when(organizationRepository.findByIdAndActiveTrue(any()))
                .thenReturn(Optional.of(org));
    }

    private void stubClaimSave() {
        when(claimRepository.save(any())).thenAnswer(inv -> {
            Claim c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(1L);
            }
            return c;
        });
    }

    private ClaimRequest minimalRequest() {
        ClaimRequest req = new ClaimRequest();
        req.setUseType(Claim.UseType.CLAIM);
        req.setClaimType(Claim.ClaimType.INSTITUTIONAL);
        req.setPriority("normal");
        req.setBeneficiaryId(10L);
        req.setCoverageId(20L);
        req.setInsurerOrgId(30L);
        req.setBillablePeriodStart(LocalDate.of(2025, 1, 1));
        req.setBillablePeriodEnd(LocalDate.of(2025, 1, 31));
        req.setTotalNet(new BigDecimal("1000.00"));
        req.setCurrency("SAR");
        req.setCareTeam(List.of());
        req.setDiagnoses(List.of());
        req.setItems(List.of());
        return req;
    }
}
