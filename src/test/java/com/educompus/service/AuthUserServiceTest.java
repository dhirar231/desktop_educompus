package com.educompus.service;

import com.educompus.model.AuthUser;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit and Integration tests for AuthUserService.
 * Requires an active MySQL connection for CRUD tests.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthUserServiceTest {

    private static AuthUserService service;
    private static int testUserId;
    private static final String UNIQUE_EMAIL = "test_junit_" + System.currentTimeMillis() + "@example.com";

    @BeforeAll
    static void setUp() {
        service = new AuthUserService();
    }

    @AfterAll
    static void tearDown() throws SQLException {
        if (testUserId > 0) {
            service.delete(testUserId);
            System.out.println("Cleaned up test user ID: " + testUserId);
        }
    }

    // --- Validation & Logic Tests (Unit) ---

    @Test
    @DisplayName("Should calculate statistics correctly")
    void testBuildStats() {
        List<AuthUser> users = List.of(
            new AuthUser(1, "a@e.c", "Admin User", "img.png", true, false),
            new AuthUser(2, "t@e.c", "Teacher User", null, false, true),
            new AuthUser(3, "m@e.c", "Mixed User", "img.png", true, true),
            new AuthUser(4, "s@e.c", "Standard User", null, false, false)
        );

        AuthUserService.UserStats stats = service.buildStats(users);

        assertEquals(4, stats.total());
        assertEquals(2, stats.admins());
        assertEquals(2, stats.teachers());
        assertEquals(1, stats.mixed());
        assertEquals(1, stats.standard());
        assertEquals(2, stats.withAvatar());
    }

    @Test
    @DisplayName("Should throw exception for invalid email format")
    void testCreateWithInvalidEmail() {
        AuthUser user = new AuthUser(0, "invalid-email", "Test User", null, false, false);
        assertThrows(IllegalArgumentException.class, () -> service.create(user, "password123"));
    }

    @Test
    @DisplayName("Should throw exception for short display name")
    void testCreateWithShortName() {
        AuthUser user = new AuthUser(0, "test@example.com", "A", null, false, false);
        assertThrows(IllegalArgumentException.class, () -> service.create(user, "password123"));
    }

    @Test
    @DisplayName("Should throw exception for short password")
    void testCreateWithShortPassword() {
        AuthUser user = new AuthUser(0, "test@example.com", "Test User", null, false, false);
        assertThrows(IllegalArgumentException.class, () -> service.create(user, "123"));
    }

    @Test
    @DisplayName("Should throw exception for missing password")
    void testCreateWithMissingPassword() {
        AuthUser user = new AuthUser(0, "test@example.com", "Test User", null, false, false);
        assertThrows(IllegalArgumentException.class, () -> service.create(user, ""));
    }

    // --- CRUD Operations (Integration) ---

    @Test
    @Order(1)
    @DisplayName("Should create a user successfully")
    void testCreateUser() throws SQLException {
        AuthUser user = new AuthUser(0, UNIQUE_EMAIL, "JUnit Test User", "avatar.png", true, false);
        testUserId = service.create(user, "securePassword123");

        assertTrue(testUserId > 0, "User ID should be positive after creation");

        AuthUser created = service.findByEmail(UNIQUE_EMAIL);
        assertNotNull(created);
        assertEquals("JUnit Test User", created.displayName());
        assertEquals(UNIQUE_EMAIL.toLowerCase(), created.email());
        assertTrue(created.admin());
        assertFalse(created.teacher());
    }

    @Test
    @Order(2)
    @DisplayName("Should update user details")
    void testUpdateUserDetails() throws SQLException {
        AuthUser existing = service.findByEmail(UNIQUE_EMAIL);
        assertNotNull(existing);

        AuthUser updated = new AuthUser(existing.id(), existing.email(), "Updated Name", "new_img.png", false, true);
        service.update(updated, null); // No password change

        AuthUser result = service.findByEmail(UNIQUE_EMAIL);
        assertEquals("Updated Name", result.displayName());
        assertEquals("new_img.png", result.imageUrl());
        assertFalse(result.admin());
        assertTrue(result.teacher());
    }

    @Test
    @Order(3)
    @DisplayName("Should update password without errors")
    void testUpdatePassword() throws SQLException {
        AuthUser existing = service.findByEmail(UNIQUE_EMAIL);
        assertDoesNotThrow(() -> service.update(existing, "newSecurePassword789"));
    }

    @Test
    @Order(4)
    @DisplayName("Should throw exception for duplicate email")
    void testCreateDuplicateEmail() {
        AuthUser duplicate = new AuthUser(0, UNIQUE_EMAIL, "Another User", null, false, false);
        assertThrows(IllegalArgumentException.class, () -> service.create(duplicate, "somePassword"));
    }

    @Test
    @Order(5)
    @DisplayName("Should delete user successfully")
    void testDeleteUser() throws SQLException {
        assertTrue(testUserId > 0);
        service.delete(testUserId);

        AuthUser result = service.findByEmail(UNIQUE_EMAIL);
        assertNull(result, "User should not exist after deletion");
        
        testUserId = 0; // Prevent tearDown from deleting again
    }
}
