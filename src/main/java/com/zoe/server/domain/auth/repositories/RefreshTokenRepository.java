package com.zoe.server.domain.auth.repositories;

import com.zoe.server.domain.auth.models.RefreshToken;
import com.zoe.server.domain.user.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUserIdAndUserRole(Long userId, UserRole userRole);

    List<RefreshToken> findByFamilyId(String familyId);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.userId = :userId AND rt.userRole = :userRole AND rt.revokedAt IS NULL AND rt.expiresAt > CURRENT_TIMESTAMP AND rt.isCompromised = false")
    List<RefreshToken> findValidTokensByUser(@Param("userId") Long userId, @Param("userRole") UserRole userRole);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.expiresAt < :now")
    List<RefreshToken> findExpiredTokens(@Param("now") Instant now);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.revokedAt IS NOT NULL OR rt.isCompromised = true")
    List<RefreshToken> findRevokedOrCompromisedTokens();

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :revokedAt, rt.revokedReason = :reason WHERE rt.familyId = :familyId")
    void revokeAllTokensInFamily(@Param("familyId") String familyId, @Param("revokedAt") Instant revokedAt, @Param("reason") String reason);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :cutoff")
    int deleteExpiredTokens(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isCompromised = true, rt.revokedAt = :revokedAt, rt.revokedReason= 'Token compromised' WHERE rt.familyId = :familyId")
    void markFamilyAsCompromised(@Param("familyId") String familyId, @Param("revokedAt") Instant revokedAt);

    boolean existsByTokenHash(String tokenHash);
}