package com.iqkv.foundation.iamservice.oauth2;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

@Component
public class AesGcmEncryptionService {

  private static final int GCM_IV_LENGTH = 12;
  private static final int GCM_TAG_LENGTH = 16;
  private static final String ALGORITHM = "AES/GCM/NoPadding";

  private final SecretKey encryptionKey;

  public AesGcmEncryptionService(
      final com.iqkv.foundation.iamservice.infrastructure.config.OAuth2ConfigurationProperties oauth2Props) {
    final String keyString = oauth2Props.encryptionKey();
    if (keyString == null || keyString.isBlank()) {
      // Generate a random key for development/testing if none provided
      try {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        this.encryptionKey = keyGenerator.generateKey();
      } catch (final Exception e) {
        throw new IllegalStateException("Failed to generate random encryption key", e);
      }
    } else {
      // Decode the provided Base64 key
      byte[] decodedKey = Base64.getDecoder().decode(keyString);
      this.encryptionKey = new SecretKeySpec(decodedKey, "AES");
    }
  }

  public String encrypt(final String plaintext) {
    try {
      byte[] iv = new byte[GCM_IV_LENGTH];
      new SecureRandom().nextBytes(iv);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv));
      byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

      byte[] encrypted = ByteBuffer.allocate(iv.length + ciphertext.length)
          .put(iv)
          .put(ciphertext)
          .array();

      return Base64.getEncoder().encodeToString(encrypted);
    } catch (final Exception e) {
      throw new IllegalStateException("Encryption failed", e);
    }
  }

  public String decrypt(final String encryptedText) {
    try {
      byte[] decoded = Base64.getDecoder().decode(encryptedText);
      ByteBuffer buffer = ByteBuffer.wrap(decoded);
      byte[] iv = new byte[GCM_IV_LENGTH];
      buffer.get(iv);
      byte[] ciphertext = new byte[buffer.remaining()];
      buffer.get(ciphertext);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv));
      byte[] plaintext = cipher.doFinal(ciphertext);

      return new String(plaintext, StandardCharsets.UTF_8);
    } catch (final Exception e) {
      throw new IllegalStateException("Decryption failed", e);
    }
  }
}
