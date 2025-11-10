package dev.woori.wooriBank.config.security;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

// TODO: 나중에 salt 적용해서 보안 강화시키기
@Component
public class Sha256Encoder implements Encoder{

    @Override
    public String encode(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to encode with SHA-256", e);
        }
    }

    @Override
    public boolean matches(String rawValue, String encodedValue) {
        return encode(rawValue).equals(encodedValue);
    }
}
