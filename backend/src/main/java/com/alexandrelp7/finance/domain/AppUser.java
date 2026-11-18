package com.alexandrelp7.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "app_users")
public class AppUser {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 100)
  private String username;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "totp_secret", length = 64)
  private String totpSecret;

  @Column(name = "totp_enabled", nullable = false)
  private boolean totpEnabled = false;

  @Column(name = "totp_pending_secret", length = 64)
  private String totpPendingSecret;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected AppUser() {}

  public AppUser(String username, String passwordHash) {
    this.username = username;
    this.passwordHash = passwordHash;
  }

  public Long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getTotpSecret() {
    return totpSecret;
  }

  public boolean isTotpEnabled() {
    return totpEnabled;
  }

  public String getTotpPendingSecret() {
    return totpPendingSecret;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public void setTotpSecret(String totpSecret) {
    this.totpSecret = totpSecret;
  }

  public void setTotpEnabled(boolean totpEnabled) {
    this.totpEnabled = totpEnabled;
  }

  public void setTotpPendingSecret(String totpPendingSecret) {
    this.totpPendingSecret = totpPendingSecret;
  }
}
