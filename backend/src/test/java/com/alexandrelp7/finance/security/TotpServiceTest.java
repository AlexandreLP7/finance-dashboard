package com.alexandrelp7.finance.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TotpServiceTest {

  private static final String RFC_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

  private final TotpService service = new TotpService("TestIssuer");

  @Test
  void rfc6238VectorsSha1() {
    assertThat(service.totp(RFC_SECRET, 59L / 30, 8)).isEqualTo("94287082");
    assertThat(service.totp(RFC_SECRET, 59L / 30, 6)).isEqualTo("287082");
    assertThat(service.totp(RFC_SECRET, 1111111109L / 30, 8)).isEqualTo("07081804");
    assertThat(service.totp(RFC_SECRET, 1234567890L / 30, 6)).isEqualTo("005924");
  }

  @Test
  void verifyAcceptsCurrentCodeAndWindow() {
    long now = 59_000L;
    String current = service.totp(RFC_SECRET, now / 1000 / 30, 6);
    String previous = service.totp(RFC_SECRET, now / 1000 / 30 - 1, 6);
    assertThat(service.verify(RFC_SECRET, current, now)).isTrue();
    assertThat(service.verify(RFC_SECRET, previous, now)).isTrue();
    assertThat(service.verify(RFC_SECRET, "000000", now)).isFalse();
    assertThat(service.verify(RFC_SECRET, "abc", now)).isFalse();
    assertThat(service.verify(RFC_SECRET, null, now)).isFalse();
  }

  @Test
  void generatedSecretRoundTripsThroughBase32() {
    String secret = service.generateSecret();
    assertThat(secret).hasSize(32);
    assertThat(TotpService.base32Decode(secret)).hasSize(20);
    assertThat(TotpService.base32Encode(TotpService.base32Decode(secret))).isEqualTo(secret);
  }

  @Test
  void otpauthUriContainsSecretAndIssuer() {
    String uri = service.otpauthUri(RFC_SECRET, "alice");
    assertThat(uri).startsWith("otpauth://totp/");
    assertThat(uri).contains("secret=" + RFC_SECRET);
    assertThat(uri).contains("issuer=TestIssuer");
    assertThat(uri).contains("digits=6").contains("period=30");
  }
}
