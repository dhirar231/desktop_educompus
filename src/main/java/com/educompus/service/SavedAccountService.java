package com.educompus.service;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

public final class SavedAccountService {
    public static final int MAX_SAVED_ACCOUNTS = 2;

    private static final String PREF_KEY_ACCOUNTS = "quickLogin.savedAccounts";
    private static final byte[] KDF_SALT = "educompus.quick-login.v1".getBytes(StandardCharsets.UTF_8);
    private static final int KDF_ITERATIONS = 120_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;

    private final Preferences prefs;
    private final SecureRandom secureRandom = new SecureRandom();
    private volatile SecretKeySpec key;

    public SavedAccountService() {
        Preferences node;
        try {
            node = Preferences.userNodeForPackage(SavedAccountService.class);
        } catch (Exception ignored) {
            node = null;
        }
        this.prefs = node;
    }

    public List<SavedAccountEntry> listAccounts() {
        return new ArrayList<>(readAccounts().values());
    }

    public boolean containsEmail(String email) {
        String normalized = normalizeEmail(email);
        return !normalized.isBlank() && readAccounts().containsKey(normalized);
    }

    public String loadPassword(String email) {
        String normalized = normalizeEmail(email);
        if (normalized.isBlank()) {
            return "";
        }
        SavedAccountEntry entry = readAccounts().get(normalized);
        if (entry == null) {
            return "";
        }
        return entry.password();
    }

    public SaveResult save(String email, String plainPassword, String replaceEmail) {
        String normalized = normalizeEmail(email);
        String password = plainPassword == null ? "" : plainPassword;
        if (normalized.isBlank() || password.isBlank()) {
            return SaveResult.ofInvalidInput();
        }

        LinkedHashMap<String, SavedAccountEntry> accounts = readAccounts();
        String replaceNormalized = normalizeEmail(replaceEmail);

        if (!replaceNormalized.isBlank() && !replaceNormalized.equals(normalized)) {
            accounts.remove(replaceNormalized);
        }

        if (!accounts.containsKey(normalized) && accounts.size() >= MAX_SAVED_ACCOUNTS) {
            return SaveResult.limitReached(new ArrayList<>(accounts.values()));
        }

        accounts.put(normalized, new SavedAccountEntry(normalized, password));
        writeAccounts(accounts);
        return SaveResult.saved(new ArrayList<>(accounts.values()));
    }

    public void remove(String email) {
        String normalized = normalizeEmail(email);
        if (normalized.isBlank()) {
            return;
        }
        LinkedHashMap<String, SavedAccountEntry> accounts = readAccounts();
        if (accounts.remove(normalized) != null) {
            writeAccounts(accounts);
        }
    }

    private LinkedHashMap<String, SavedAccountEntry> readAccounts() {
        LinkedHashMap<String, SavedAccountEntry> out = new LinkedHashMap<>();
        if (prefs == null) {
            return out;
        }

        String raw;
        try {
            raw = prefs.get(PREF_KEY_ACCOUNTS, "");
        } catch (Exception e) {
            return out;
        }
        if (raw == null || raw.isBlank()) {
            return out;
        }

        String[] lines = raw.split("\\R");
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            int sep = line.indexOf('|');
            if (sep <= 0 || sep >= line.length() - 1) {
                continue;
            }

            String emailB64 = line.substring(0, sep).trim();
            String encryptedPassword = line.substring(sep + 1).trim();
            String email = decodeBase64(emailB64);
            String normalized = normalizeEmail(email);
            if (normalized.isBlank()) {
                continue;
            }

            String password;
            try {
                password = decrypt(encryptedPassword);
            } catch (Exception ignored) {
                continue;
            }
            if (password.isBlank()) {
                continue;
            }

            out.put(normalized, new SavedAccountEntry(normalized, password));
            if (out.size() >= MAX_SAVED_ACCOUNTS) {
                break;
            }
        }
        return out;
    }

    private void writeAccounts(LinkedHashMap<String, SavedAccountEntry> accounts) {
        if (prefs == null) {
            return;
        }

        StringBuilder serialized = new StringBuilder();
        int count = 0;
        for (Map.Entry<String, SavedAccountEntry> kv : accounts.entrySet()) {
            if (count++ >= MAX_SAVED_ACCOUNTS) {
                break;
            }
            SavedAccountEntry entry = kv.getValue();
            String email = normalizeEmail(entry.email());
            String password = entry.password() == null ? "" : entry.password();
            if (email.isBlank() || password.isBlank()) {
                continue;
            }
            try {
                String encrypted = encrypt(password);
                if (serialized.length() > 0) {
                    serialized.append('\n');
                }
                serialized.append(encodeBase64(email)).append('|').append(encrypted);
            } catch (Exception ignored) {
            }
        }

        try {
            if (serialized.length() == 0) {
                prefs.remove(PREF_KEY_ACCOUNTS);
            } else {
                prefs.put(PREF_KEY_ACCOUNTS, serialized.toString());
            }
        } catch (Exception ignored) {
        }
    }

    private String encrypt(String plainPassword) throws Exception {
        byte[] iv = new byte[GCM_IV_BYTES];
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, resolveKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] ciphertext = cipher.doFinal(plainPassword.getBytes(StandardCharsets.UTF_8));

        byte[] payload = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, payload, 0, iv.length);
        System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
        return Base64.getEncoder().encodeToString(payload);
    }

    private String decrypt(String encryptedPayload) throws Exception {
        byte[] payload = Base64.getDecoder().decode(encryptedPayload);
        if (payload.length <= GCM_IV_BYTES) {
            return "";
        }

        byte[] iv = new byte[GCM_IV_BYTES];
        byte[] ciphertext = new byte[payload.length - GCM_IV_BYTES];
        System.arraycopy(payload, 0, iv, 0, iv.length);
        System.arraycopy(payload, iv.length, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, resolveKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] decoded = cipher.doFinal(ciphertext);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    private SecretKeySpec resolveKey() throws Exception {
        SecretKeySpec local = key;
        if (local != null) {
            return local;
        }

        synchronized (this) {
            if (key != null) {
                return key;
            }
            String material = String.join("|",
                    normalizeValue(System.getProperty("user.name")),
                    normalizeValue(System.getProperty("user.home")),
                    normalizeValue(System.getProperty("os.name")),
                    normalizeValue(System.getProperty("os.arch"))
            );

            PBEKeySpec spec = new PBEKeySpec(material.toCharArray(), KDF_SALT, KDF_ITERATIONS, KEY_LENGTH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] derived = factory.generateSecret(spec).getEncoded();
            spec.clearPassword();
            key = new SecretKeySpec(derived, "AES");
            return key;
        }
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static String normalizeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private static String encodeBase64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeBase64(String base64) {
        try {
            return new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
        }
    }

    public record SavedAccountEntry(String email, String password) {
    }

    public record SaveResult(boolean saved, boolean limitReached, boolean invalidInput, List<SavedAccountEntry> accounts) {
        private static SaveResult saved(List<SavedAccountEntry> accounts) {
            return new SaveResult(true, false, false, accounts == null ? List.of() : List.copyOf(accounts));
        }

        private static SaveResult limitReached(List<SavedAccountEntry> accounts) {
            return new SaveResult(false, true, false, accounts == null ? List.of() : List.copyOf(accounts));
        }

        private static SaveResult ofInvalidInput() {
            return new SaveResult(false, false, true, List.of());
        }
    }
}
