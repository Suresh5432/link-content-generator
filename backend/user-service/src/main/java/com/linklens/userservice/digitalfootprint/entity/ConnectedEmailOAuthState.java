package com.linklens.userservice.digitalfootprint.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "connected_email_oauth_states")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectedEmailOAuthState {
    @Id
    @Column(name = "state_hash", length = 64)
    private String stateHash;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(name = "verifier_ciphertext", nullable = false)
    private byte[] verifierCiphertext;
    @Column(name = "verifier_iv", nullable = false)
    private byte[] verifierIv;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
