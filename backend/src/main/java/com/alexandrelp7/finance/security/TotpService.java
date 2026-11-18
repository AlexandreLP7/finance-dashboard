package com.alexandrelp7.finance.security;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TotpService {

  private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
  private static final int SECRET_BYTES = 20;
  private static final int PERIOD_SECONDS = 30;
  private static final int DIGITS = 6;
  private static final int WINDOW = 1;

  private final SecureRandom random = new SecureRandom();
  private final String issuer;

  public TotpService(@Value("${app.security.totp-issuer:Finance Dashboard}") String issuer) {
    this.issuer = issuer;
  }

  public String generateSecret() {
    byte[] bytes = new byte[SECRET_BYTES];
    random.nextBytes(bytes);
    return base32Encode(bytes);
  }

  public boolean verify(String base32Secret, String code, long timeMillis) {
    if (code == null || !code.matches("\\d{" + DIGITS + "}")) {
      return false;
    }
    long counter = timeMillis / 1000 / PERIOD_SECONDS;
    for (int offset = -WINDOW; offset <= WINDOW; offset++) {
      if (totp(base32Secret, counter + offset, DIGITS).equals(code)) {
        return true;
      }
    }
    return false;
  }

  public String totp(String base32Secret, long counter, int digits) {
    byte[] key = base32Decode(base32Secret);
    byte[] data = new byte[8];
    long value = counter;
    for (int i = 7; i >= 0; i--) {
      data[i] = (byte) (value & 0xff);
      value >>>= 8;
    }
    try {
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(new SecretKeySpec(key, "HmacSHA1"));
      byte[] hash = mac.doFinal(data);
      int offset = hash[hash.length - 1] & 0xf;
      long binary =
          ((hash[offset] & 0x7fL) << 24)
              | ((hash[offset + 1] & 0xffL) << 16)
              | ((hash[offset + 2] & 0xffL) << 8)
              | (hash[offset + 3] & 0xffL);
      long otp = binary % (long) Math.pow(10, digits);
      return String.format(Locale.ROOT, "%0" + digits + "d", otp);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("HMAC-SHA1 indisponible", e);
    }
  }

  public String otpauthUri(String base32Secret, String account) {
    String label = URLEncoder.encode(issuer + ":" + account, StandardCharsets.UTF_8);
    String issuerParam = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
    return "otpauth://totp/"
        + label
        + "?secret="
        + base32Secret
        + "&issuer="
        + issuerParam
        + "&algorithm=SHA1&digits="
        + DIGITS
        + "&period="
        + PERIOD_SECONDS;
  }

  static String base32Encode(byte[] data) {
    StringBuilder out = new StringBuilder();
    int buffer = 0;
    int bits = 0;
    for (byte b : data) {
      buffer = (buffer << 8) | (b & 0xff);
      bits += 8;
      while (bits >= 5) {
        out.append(BASE32_ALPHABET.charAt((buffer >> (bits - 5)) & 0x1f));
        bits -= 5;
      }
    }
    if (bits > 0) {
      out.append(BASE32_ALPHABET.charAt((buffer << (5 - bits)) & 0x1f));
    }
    return out.toString();
  }

  static byte[] base32Decode(String encoded) {
    String cleaned =
        encoded.toUpperCase(Locale.ROOT).replaceAll("[^A-Z2-7]", "");
    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
    int buffer = 0;
    int bits = 0;
    for (char c : cleaned.toCharArray()) {
      buffer = (buffer << 5) | BASE32_ALPHABET.indexOf(c);
      bits += 5;
      if (bits >= 8) {
        out.write((buffer >> (bits - 8)) & 0xff);
        bits -= 8;
      }
    }
    return out.toByteArray();
  }
}
