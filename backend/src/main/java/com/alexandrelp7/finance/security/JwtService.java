package com.alexandrelp7.finance.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  public static final String SCOPE_ACCESS = "access";
  public static final String SCOPE_PRE_2FA = "pre-2fa";
  public static final String ROLE_USER = "ROLE_USER";
  public static final String ROLE_PRE_2FA = "ROLE_PRE_2FA";

  private final SecretKey key;
  private final long accessTtlSeconds;
  private final long refreshTtlSeconds;
  private final long pre2faTtlSeconds;

  public JwtService(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.access-ttl-seconds}") long accessTtlSeconds,
      @Value("${app.jwt.refresh-ttl-seconds}") long refreshTtlSeconds,
      @Value("${app.jwt.pre2fa-ttl-seconds}") long pre2faTtlSeconds) {
    byte[] keyBytes = Decoders.BASE64.decode(secret);
    if (keyBytes.length < 32) {
      throw new IllegalStateException("app.jwt.secret doit faire au moins 32 octets (base64)");
    }
    this.key = Keys.hmacShaKeyFor(keyBytes);
    this.accessTtlSeconds = accessTtlSeconds;
    this.refreshTtlSeconds = refreshTtlSeconds;
    this.pre2faTtlSeconds = pre2faTtlSeconds;
  }

  public String generateAccessToken(String username) {
    return buildToken(username, SCOPE_ACCESS, List.of(ROLE_USER), accessTtlSeconds);
  }

  public String generatePre2faToken(String username) {
    return buildToken(username, SCOPE_PRE_2FA, List.of(ROLE_PRE_2FA), pre2faTtlSeconds);
  }

  public long getRefreshTtlSeconds() {
    return refreshTtlSeconds;
  }

  private String buildToken(String username, String scope, List<String> roles, long ttlSeconds) {
    Instant now = Instant.now();
    return Jwts.builder()
        .id(UUID.randomUUID().toString())
        .subject(username)
        .claim("scope", scope)
        .claim("roles", roles)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(ttlSeconds)))
        .signWith(key)
        .compact();
  }

  public TokenInfo parse(String token) {
    try {
      Claims claims =
          Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
      @SuppressWarnings("unchecked")
      List<String> roles = claims.get("roles", List.class);
      return new TokenInfo(
          claims.getSubject(),
          claims.get("scope", String.class),
          roles == null ? List.of() : roles,
          claims.getExpiration().toInstant());
    } catch (JwtException | IllegalArgumentException e) {
      return null;
    }
  }

  public record TokenInfo(String subject, String scope, List<String> roles, Instant expiresAt) {}
}
