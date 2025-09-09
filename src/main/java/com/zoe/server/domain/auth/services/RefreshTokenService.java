package com.zoe.server.domain.auth.services;

import com.zoe.server.domain.auth.models.RefreshToken;
import com.zoe.server.domain.auth.repositories.RefreshTokenRepository;
import com.zoe.server.domain.user.enums.UserRole;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenSecurityService tokenSecurityService;

    @Value("${jwt.refresh-token-expiration:604800000}") // 7 days in milliseconds
    private long refreshTokenExpiration;

    @Value("${jwt.max-tokens-per-user:5}") // Maximum tokens per user
    private int maxTokensPerUser;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               TokenSecurityService tokenSecurityService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenSecurityService = tokenSecurityService;
    }


    @Transactional
    public String createRefreshToken(Long userId, UserRole userRole) {

        limitActiveTokensPerUser(userId, userRole);

        String secureToken = tokenSecurityService.generateSecureToken();
        String familyId = tokenSecurityService.generateFamilyId();
        String tokenHash = tokenSecurityService.hashToken(secureToken);

        Instant expiresAt = Instant.now().plusMillis(refreshTokenExpiration);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setUserRole(userRole);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setFamilyId(familyId);
        refreshToken.setExpiresAt(expiresAt);
        refreshTokenRepository.save(refreshToken);

        logger.info("Created refresh token for user {} ({}), family: {}", userId, userRole, familyId);
        return secureToken;
    }

    @Transactional
    public Long validateRefreshToken(String token) {
        if (tokenSecurityService.isValidToken(token)) {
            throw new IllegalArgumentException("Invalid refresh token format");
        }

        String tokenHash = tokenSecurityService.hashToken(token);
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByTokenHash(tokenHash);

        if (tokenOpt.isEmpty()) {
            throw new IllegalArgumentException("Refresh token not found");
        }

        RefreshToken refreshToken = tokenOpt.get();

        if (!refreshToken.isValid()) {
            if (refreshToken.isExpired()) {
                refreshTokenRepository.delete(refreshToken);
                throw new IllegalArgumentException("Refresh token has expired");
            } else if (refreshToken.isRevoked()) {
                throw new IllegalArgumentException("Refresh token has been revoked: " + refreshToken.getRevokedReason());
            } else if (refreshToken.isCompromised()) {
                throw new IllegalArgumentException("Refresh token has been compromised");
            }
        }

        refreshTokenRepository.save(refreshToken);

        logger.debug("Validated refresh token for user {}, use count: {}",
                refreshToken.getUserId(), refreshToken.getUseCount());

        return refreshToken.getUserId();
    }

    @Transactional
    public String rotateRefreshToken(String oldToken, UserRole userRole) {

        Long userId = validateRefreshToken(oldToken);

        String oldTokenHash = tokenSecurityService.hashToken(oldToken);
        Optional<RefreshToken> oldTokenOpt = refreshTokenRepository.findByTokenHash(oldTokenHash);

        if (oldTokenOpt.isPresent()) {
            RefreshToken oldRefreshToken = oldTokenOpt.get();
            oldRefreshToken.revoke("Token rotated");
            refreshTokenRepository.save(oldRefreshToken);

            refreshTokenRepository.revokeAllTokensInFamily(
                    oldRefreshToken.getFamilyId(),
                    Instant.now(),
                    "Family revoked due to rotation"
            );
        }

        String newToken = createRefreshToken(userId, userRole);

        logger.info("Rotated refresh token for user {}, old family revoked", userId);
        return newToken;
    }

    @Transactional
    public void revokeRefreshToken(String token, String reason) {
        if (tokenSecurityService.isValidToken(token)) {
            return;
        }

        String tokenHash = tokenSecurityService.hashToken(token);
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByTokenHash(tokenHash);

        if (tokenOpt.isPresent()) {
            RefreshToken refreshToken = tokenOpt.get();
            refreshToken.revoke(reason);
            refreshTokenRepository.save(refreshToken);

            logger.info("Revoked refresh token for user {}, reason: {}",
                    refreshToken.getUserId(), reason);
        }
    }

    @Transactional
    public void revokeAllUserTokens(Long userId, UserRole userRole, String reason) {
        var userTokens = refreshTokenRepository.findByUserIdAndUserRole(userId, userRole);

        for (RefreshToken token : userTokens) {
            token.revoke(reason);
        }

        refreshTokenRepository.saveAll(userTokens);

        logger.info("Revoked all {} tokens for user {} ({}), reason: {}",
                userTokens.size(), userId, userRole, reason);
    }

    @Transactional
    public void markFamilyAsCompromised(String token) {
        if (tokenSecurityService.isValidToken(token)) {
            return;
        }

        String tokenHash = tokenSecurityService.hashToken(token);
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByTokenHash(tokenHash);

        if (tokenOpt.isPresent()) {
            RefreshToken refreshToken = tokenOpt.get();
            String familyId = refreshToken.getFamilyId();

            refreshTokenRepository.markFamilyAsCompromised(familyId, Instant.now());

            logger.warn("Marked token family {} as compromised for user {}",
                    familyId, refreshToken.getUserId());
        }
    }

    private void limitActiveTokensPerUser(Long userId, UserRole userRole) {
        var activeTokens = refreshTokenRepository.findValidTokensByUser(userId, userRole);

        if (activeTokens.size() >= maxTokensPerUser) {
            var oldestToken = activeTokens.stream()
                    .min((t1, t2) ->
                            t1.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()
                                    .compareTo(t2.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                    )
                    .orElse(null);

            if (oldestToken != null) {
                oldestToken.revoke("Maximum tokens per user limit reached");
                refreshTokenRepository.save(oldestToken);

                logger.info("Revoked oldest token for user {} due to limit", userId);
            }
        }
    }

    @Scheduled(cron = "0 0 2 * * ?") // 2 AM daily
    @Transactional
    public void cleanupExpiredTokens() {
        Instant cutoff = Instant.now().minusSeconds(86400); // 24 hours ago
        int deletedCount = refreshTokenRepository.deleteExpiredTokens(cutoff);

        if (deletedCount > 0) {
            logger.info("Cleaned up {} expired refresh tokens", deletedCount);
        }
    }


    public TokenStats getTokenStats() {
        long totalTokens = refreshTokenRepository.count();
        long expiredTokens = refreshTokenRepository.findExpiredTokens(Instant.now()).size();
        long revokedTokens = refreshTokenRepository.findRevokedOrCompromisedTokens().size();
        long activeTokens = totalTokens - expiredTokens - revokedTokens;

        return new TokenStats(totalTokens, activeTokens, expiredTokens, revokedTokens);
    }

    @Getter
    public static class TokenStats {
        // Getters
        private final long totalTokens;
        private final long activeTokens;
        private final long expiredTokens;
        private final long revokedTokens;

        public TokenStats(long totalTokens, long activeTokens, long expiredTokens, long revokedTokens) {
            this.totalTokens = totalTokens;
            this.activeTokens = activeTokens;
            this.expiredTokens = expiredTokens;
            this.revokedTokens = revokedTokens;
        }

    }

    // Stub: Extract userId from refresh token (implement JWT claim extraction here)
    public Long extractUserIdFromRefreshToken(String refreshToken) {
        // TODO: Implement JWT parsing and claim extraction
        return 1L;
    }

    // Stub: Extract userRole from refresh token (implement JWT claim extraction here)
    public UserRole extractUserRoleFromRefreshToken(String refreshToken) {
        // TODO: Implement JWT parsing and claim extraction
        return UserRole.PSYCHOLOGIST;
    }
}