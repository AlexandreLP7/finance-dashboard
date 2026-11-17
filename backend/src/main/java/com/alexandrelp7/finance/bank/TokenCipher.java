package com.alexandrelp7.finance.bank;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenCipher {

  private static final String ALGO = "AES/GCM/NoPadding";
  private static final int IV_LENGTH = 12;

  private final SecretKeySpec key;
  private final SecureRandom random = new SecureRandom();

  public TokenCipher(@Value("${app.encryption-key}") String base64Key) {
    byte[] raw = Base64.getDecoder().decode(base64Key);
    if (raw.length != 32) {
      throw new IllegalArgumentException("app.encryption-key doit contenir 32 octets encodes en base64");
    }
    this.key = new SecretKeySpec(raw, "AES");
  }

  public String encrypt(String plain) {
    try {
      byte[] iv = new byte[IV_LENGTH];
      random.nextBytes(iv);
      Cipher cipher = Cipher.getInstance(ALGO);
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
      byte[] ciphertext = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
      ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
      buffer.put(iv).put(ciphertext);
      return Base64.getEncoder().encodeToString(buffer.array());
    } catch (Exception e) {
      throw new IllegalStateException("Chiffrement impossible", e);
    }
  }

  public String decrypt(String encoded) {
    try {
      byte[] all = Base64.getDecoder().decode(encoded);
      byte[] iv = new byte[IV_LENGTH];
      byte[] ciphertext = new byte[all.length - IV_LENGTH];
      System.arraycopy(all, 0, iv, 0, IV_LENGTH);
      System.arraycopy(all, IV_LENGTH, ciphertext, 0, ciphertext.length);
      Cipher cipher = Cipher.getInstance(ALGO);
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
      return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Dechiffrement impossible", e);
    }
  }
}
