package com.alexandrelp7.finance.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private static final String SECRET =
      Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

  private final JwtService service = new JwtService(SECRET, 900, 604800, 300);

  @Test
  void accessTokenParsesWithSubjectScopeAndRole() {
    String token = service.generateAccessToken("alice");
    JwtService.TokenInfo info = service.parse(token);
    assertThat(info).isNotNull();
    assertThat(info.subject()).isEqualTo("alice");
    assertThat(info.scope()).isEqualTo(JwtService.SCOPE_ACCESS);
    assertThat(info.roles()).containsExactly(JwtService.ROLE_USER);
    assertThat(info.expiresAt()).isAfter(Instant.now().plusSeconds(800));
  }

  @Test
  void pre2faTokenHasPre2faScope() {
    JwtService.TokenInfo info = service.parse(service.generatePre2faToken("bob"));
    assertThat(info).isNotNull();
    assertThat(info.scope()).isEqualTo(JwtService.SCOPE_PRE_2FA);
    assertThat(info.roles()).containsExactly(JwtService.ROLE_PRE_2FA);
  }

  @Test
  void tamperedTokenReturnsNull() {
    String token = service.generateAccessToken("alice");
    assertThat(service.parse(token + "x")).isNull();
    assertThat(service.parse("not-a-jwt")).isNull();
  }

  @Test
  void tokenSignedWithAnotherSecretReturnsNull() {
    String otherSecret =
        Base64.getEncoder()
            .encodeToString("abcdefghijklmnopabcdefghijklmnop".getBytes(StandardCharsets.UTF_8));
    JwtService other = new JwtService(otherSecret, 900, 604800, 300);
    assertThat(service.parse(other.generateAccessToken("alice"))).isNull();
  }

  @Test
  void tooShortSecretIsRejected() {
    String shortSecret = Base64.getEncoder().encodeToString("too-short".getBytes(StandardCharsets.UTF_8));
    assertThatThrownBy(() -> new JwtService(shortSecret, 900, 604800, 300))
        .isInstanceOf(IllegalStateException.class);
  }
}
