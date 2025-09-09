package com.zoe.server.domain.auth.models;

import com.zoe.server.domain.user.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_token_hash", columnList = "tokenHash"),
        @Index(name = "idx_refresh_token_user", columnList = "userId"),
        @Index(name = "idx_refresh_token_expires", columnList = "expiresAt"),
        @Index(name = "idx_refresh_token_revoked", columnList = "revokedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "user_role", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private UserRole userRole;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "revoked_at", nullable = true)
    private Instant revokedAt;
    @Column(name = "revoked_reason", length = 100)
    private String revokedReason;
    @Column(name = "family_id", nullable = true)
    private String familyId;
    @Column(name = "is_compromised", nullable = false)
    private boolean isCompromised = false;
    @Column(name = "last_used_at")
    private Instant lastUsedAt;
    @Column(name = "use_count", nullable = false)
    private Integer useCount = 0;
    //quando adicionar o Audit, remover este campo
    @CreationTimestamp
    private Instant createdAt;
    public void revoke(String reason) {
        this.revokedAt = Instant.now();
        this.revokedReason = reason;
    }
    public boolean isValid() {
        return !isExpired() && !isRevoked() && !isCompromised();
    }

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }
    public boolean isRevoked() {
        return revokedAt != null;
    }
}
