package com.educompus.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.educompus.model.AuthUser;
import com.educompus.repository.AuthUserRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

public class AuthUserService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private final AuthUserRepository repository = new AuthUserRepository();

    public List<AuthUser> findAll() throws SQLException {
        return repository.findAll();
    }

    public int create(AuthUser user, String plainPassword) throws SQLException {
        validateUser(user, null);
        validatePassword(plainPassword, true);
        String hash = BCrypt.withDefaults().hashToString(12, plainPassword.toCharArray());
        return repository.create(normalized(user), hash);
    }

    public void update(AuthUser user, String newPassword) throws SQLException {
        validateUser(user, user.id());
        AuthUser normalized = normalized(user);
        repository.update(normalized);

        if (newPassword != null && !newPassword.isBlank()) {
            validatePassword(newPassword, false);
            String hash = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray());
            repository.updatePassword(normalized.id(), hash);
        }
    }

    public void delete(int userId) throws SQLException {
        repository.delete(userId);
    }

    public UserStats buildStats(List<AuthUser> users) {
        int total = users == null ? 0 : users.size();
        int admins = 0;
        int teachers = 0;
        int mixed = 0;
        int standard = 0;
        int withAvatar = 0;

        if (users != null) {
            for (AuthUser user : users) {
                boolean admin = user != null && user.admin();
                boolean teacher = user != null && user.teacher();
                if (admin) {
                    admins++;
                }
                if (teacher) {
                    teachers++;
                }
                if (admin && teacher) {
                    mixed++;
                }
                if (!admin && !teacher) {
                    standard++;
                }
                if (user != null && user.imageUrl() != null && !user.imageUrl().isBlank()) {
                    withAvatar++;
                }
            }
        }

        return new UserStats(total, admins, teachers, standard, mixed, withAvatar);
    }

    private void validateUser(AuthUser user, Integer excludeUserId) throws SQLException {
        if (user == null) {
            throw new IllegalArgumentException("Utilisateur invalide.");
        }

        String displayName = safe(user.displayName());
        if (displayName.length() < 2) {
            throw new IllegalArgumentException("Le nom complet doit contenir au moins 2 caractères.");
        }

        String email = safe(user.email()).toLowerCase();
        if (email.isBlank() || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Veuillez saisir un email valide.");
        }
        if (repository.emailExists(email, excludeUserId)) {
            throw new IllegalArgumentException("Cet email existe déjà.");
        }
    }

    private void validatePassword(String plainPassword, boolean required) {
        String password = plainPassword == null ? "" : plainPassword.trim();
        if (required && password.isBlank()) {
            throw new IllegalArgumentException("Le mot de passe est obligatoire.");
        }
        if (!password.isBlank() && password.length() < 6) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 6 caractères.");
        }
    }

    private AuthUser normalized(AuthUser user) {
        return new AuthUser(
                user.id(),
                safe(user.email()).toLowerCase(),
                safe(user.displayName()),
                safe(user.imageUrl()),
                user.admin(),
                user.teacher()
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public record UserStats(
            int total,
            int admins,
            int teachers,
            int standard,
            int mixed,
            int withAvatar
    ) {
    }
}
