package com.amins.nphies.repository;

import com.amins.nphies.config.TenantNphiesConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantNphiesConfigRepository
        extends JpaRepository<TenantNphiesConfig, String> {

    Optional<TenantNphiesConfig> findByTenantIdAndActiveTrue(String tenantId);

    List<TenantNphiesConfig> findAllByActiveTrue();

    @Query("SELECT t FROM TenantNphiesConfig t WHERE t.active = true " +
            "AND t.environment = :env")
    List<TenantNphiesConfig> findActiveByEnvironment(
            TenantNphiesConfig.NphiesEnvironment env);

    boolean existsByTenantIdAndActiveTrue(String tenantId);
}
