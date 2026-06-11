package com.amins.nphies.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserInvitationRepository extends JpaRepository<UserInvitation, Long> {
    Optional<UserInvitation> findTopByEmailIgnoreCaseAndAcceptedAtIsNullOrderByCreatedAtDesc(String email);
    void deleteByEmailIgnoreCaseAndAcceptedAtIsNull(String email);
}
