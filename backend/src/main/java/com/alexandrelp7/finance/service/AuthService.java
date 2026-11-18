package com.alexandrelp7.finance.service;

import com.alexandrelp7.finance.domain.AppUser;
import com.alexandrelp7.finance.domain.RefreshToken;
import com.alexandrelp7.finance.repository.AppUserRepository;
import com.alexandrelp7.finance.repository.RefreshTokenRepository;
import com.alexandrelp7.finance.security.JwtService;
import com.alexandrelp7.finance.security.TotpService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final AppUserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final TotpService totpService;
  private final SecureRandom random = new SecureRandom();

  public AuthService(
      AppUserRepository userRepository,
      RefreshTokenRepository refreshTokenRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      TotpService totpService) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.totpService = totpService;
  }

  @Transactional
  public LoginResult login(String username, String password) {
    AppUser user = findUser(username);
    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      throw new AuthException("Identifiants invalides");
    }
    if (user.isTotpEnabled()) {
      return new LoginResult(null, true, jwtService.generatePre2faToken(username), username);
    }
    return new LoginResult(issueTokens(user), false, null, username);
  }

  @Transactional
  public LoginResult verify2fa(String pre2faToken, String code) {
    JwtService.TokenInfo info = jwtService.parse(pre2faToken);
    if (info == null || !JwtService.SCOPE_PRE_2FA.equals(info.scope())) {
      throw new AuthException("Jeton pre-2FA invalide ou expire");
    }
    AppUser user = findUser(info.subject());
    if (!user.isTotpEnabled()
        || !totpService.verify(user.getTotpSecret(), code, Instant.now().toEpochMilli())) {
      throw new AuthException("Code TOTP invalide");
    }
    return new LoginResult(issueTokens(user), false, null, user.getUsername());
  }

  @Transactional
  public TokenPair refresh(String refreshTokenValue) {
    RefreshToken stored =
        refreshTokenRepository
            .findByTokenHash(sha256Hex(refreshTokenValue))
            .orElseThrow(() -> new AuthException("Refresh token inconnu"));
    if (stored.isRevoked() || stored.isExpired()) {
      throw new AuthException("Refresh token revoque ou expire");
    }
    stored.revoke();
    refreshTokenRepository.save(stored);
    return issueTokens(stored.getUser());
  }

  @Transactional
  public void logout(String username) {
    AppUser user = findUser(username);
    refreshTokenRepository.findByUserId(user.getId()).forEach(RefreshToken::revoke);
  }

  @Transactional
  public TotpSetup setup2fa(String username) {
    AppUser user = findUser(username);
    String secret = totpService.generateSecret();
    user.setTotpPendingSecret(secret);
    userRepository.save(user);
    return new TotpSetup(secret, totpService.otpauthUri(secret, username));
  }

  @Transactional
  public void enable2fa(String username, String code) {
    AppUser user = findUser(username);
    String pending = user.getTotpPendingSecret();
    if (pending == null
        || !totpService.verify(pending, code, Instant.now().toEpochMilli())) {
      throw new AuthException("Code TOTP invalide pour l'activation");
    }
    user.setTotpSecret(pending);
    user.setTotpEnabled(true);
    user.setTotpPendingSecret(null);
    userRepository.save(user);
  }

  @Transactional
  public void disable2fa(String username, String code) {
    AppUser user = findUser(username);
    if (!user.isTotpEnabled()
        || !totpService.verify(user.getTotpSecret(), code, Instant.now().toEpochMilli())) {
      throw new AuthException("Code TOTP invalide pour la desactivation");
    }
    user.setTotpSecret(null);
    user.setTotpEnabled(false);
    user.setTotpPendingSecret(null);
    userRepository.save(user);
  }

  @Transactional
  public void changePassword(String username, String oldPassword, String newPassword) {
    AppUser user = findUser(username);
    if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
      throw new AuthException("Ancien mot de passe invalide");
    }
    user.setPasswordHash(passwordEncoder.encode(newPassword));
    userRepository.save(user);
    refreshTokenRepository.findByUserId(user.getId()).forEach(RefreshToken::revoke);
  }

  public boolean isTotpEnabled(String username) {
    return findUser(username).isTotpEnabled();
  }

  private AppUser findUser(String username) {
    return userRepository
        .findByUsername(username)
        .orElseThrow(() -> new AuthException("Identifiants invalides"));
  }

  private TokenPair issueTokens(AppUser user) {
    byte[] raw = new byte[32];
    random.nextBytes(raw);
    String refreshTokenValue =
        Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    RefreshToken stored =
        new RefreshToken(
            user,
            sha256Hex(refreshTokenValue),
            Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()));
    refreshTokenRepository.save(stored);
    return new TokenPair(jwtService.generateAccessToken(user.getUsername()), refreshTokenValue);
  }

  static String sha256Hex(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        hex.append(Character.forDigit((b >> 4) & 0xf, 16));
        hex.append(Character.forDigit(b & 0xf, 16));
      }
      return hex.toString();
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 indisponible", e);
    }
  }

  public record TokenPair(String accessToken, String refreshToken) {}

  public record LoginResult(
      TokenPair tokens, boolean totpRequired, String pre2faToken, String username) {}

  public record TotpSetup(String secret, String otpauthUri) {}
}
