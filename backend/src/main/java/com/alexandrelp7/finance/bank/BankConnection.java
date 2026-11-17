package com.alexandrelp7.finance.bank;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "bank_connections")
public class BankConnection {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 50)
  private String provider;

  @Column(name = "consent_id")
  private String consentId;

  @Column(name = "access_token_enc", length = 2048)
  private String accessTokenEnc;

  @Column(name = "refresh_token_enc", length = 2048)
  private String refreshTokenEnc;

  @Column(name = "token_expires_at")
  private Instant tokenExpiresAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected BankConnection() {}

  public BankConnection(String provider) {
    this.provider = provider;
  }

  public Long getId() {
    return id;
  }

  public String getProvider() {
    return provider;
  }

  public String getConsentId() {
    return consentId;
  }

  public String getAccessTokenEnc() {
    return accessTokenEnc;
  }

  public String getRefreshTokenEnc() {
    return refreshTokenEnc;
  }

  public Instant getTokenExpiresAt() {
    return tokenExpiresAt;
  }

  public void setConsentId(String consentId) {
    this.consentId = consentId;
    this.updatedAt = Instant.now();
  }

  public void setTokens(String accessTokenEnc, String refreshTokenEnc, Instant expiresAt) {
    this.accessTokenEnc = accessTokenEnc;
    this.refreshTokenEnc = refreshTokenEnc;
    this.tokenExpiresAt = expiresAt;
    this.updatedAt = Instant.now();
  }
}
