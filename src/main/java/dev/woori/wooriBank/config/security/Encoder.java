package dev.woori.wooriBank.config.security;

public interface Encoder {
    String encode(String value);
    boolean matches(String rawValue, String encodedValue);
}
