package com.example.demo.service;

import com.example.demo.exception.ExpenseValidationException;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 * Tests all major functionality including user creation, authentication,
 * and user retrieval operations.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    /**
     * Sets up test data before each test.
     * Creates a test user with predefined values.
     */
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setPassword("password123");
    }

    /**
     * Tests successful registration of a user.
     * Verifies that:
     * 1. The password is encoded
     * 2. The user is saved correctly
     * 3. The correct repository method is called
     */
    @Test
    void registerUser_Success() {
        // Arrange
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(userRepository.save(any())).thenReturn(testUser);

        // Act
        User result = userService.registerUser(testUser);

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getEmail(), result.getEmail());
        assertEquals(testUser.getName(), result.getName());
        verify(passwordEncoder).encode(any());
        verify(userRepository).save(any());
    }

    /**
     * Tests that registering a user with existing email throws an exception.
     * Verifies that:
     * 1. The appropriate exception is thrown
     * 2. The user is not saved
     */
    @Test
    void registerUser_ExistingEmail_ThrowsException() {
        // Arrange
        when(userRepository.existsByEmail(any())).thenReturn(true);

        // Act & Assert
        assertThrows(ExpenseValidationException.class, () -> 
            userService.registerUser(testUser));
        verify(userRepository, never()).save(any());
    }

    /**
     * Tests successful retrieval of a user by ID.
     * Verifies that:
     * 1. The correct user is returned
     * 2. The correct repository method is called
     */
    @Test
    void getUserById_Success() {
        // Arrange
        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.getUserById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getEmail(), result.getEmail());
    }

    /**
     * Tests that retrieving a non-existent user throws an exception.
     * Verifies that:
     * 1. The appropriate exception is thrown
     * 2. The correct repository method is called
     */
    @Test
    void getUserById_NonExistent_ThrowsException() {
        // Arrange
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ExpenseValidationException.class, () -> 
            userService.getUserById(1L));
    }

    /**
     * Tests successful retrieval of a user by email.
     * Verifies that:
     * 1. The correct user is returned
     * 2. The correct repository method is called
     */
    @Test
    void getUserByEmail_Success() {
        // Arrange
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.getUserByEmail("test@example.com");

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getEmail(), result.getEmail());
        assertEquals(testUser.getName(), result.getName());
    }

    /**
     * Tests that retrieving a non-existent user by email throws an exception.
     * Verifies that:
     * 1. The appropriate exception is thrown
     * 2. The correct repository method is called
     */
    @Test
    void getUserByEmail_NonExistent_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ExpenseValidationException.class, () -> 
            userService.getUserByEmail("nonexistent@example.com"));
    }

    /**
     * Tests successful user login.
     * Verifies that:
     * 1. The password is verified correctly
     * 2. The correct user is returned
     * 3. The correct repository and encoder methods are called
     */
    @Test
    void loginUser_Success() {
        // Arrange
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        // Act
        User result = userService.loginUser("test@example.com", "password123");

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getEmail(), result.getEmail());
        verify(passwordEncoder).matches(any(), any());
    }

    /**
     * Tests that login with incorrect password throws an exception.
     * Verifies that:
     * 1. The appropriate exception is thrown
     * 2. The password is verified
     */
    @Test
    void loginUser_InvalidPassword_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        // Act & Assert
        assertThrows(ExpenseValidationException.class, () -> 
            userService.loginUser("test@example.com", "wrongpassword"));
        verify(passwordEncoder).matches(any(), any());
    }

    /**
     * Tests successful update of user details.
     * Verifies that:
     * 1. The user is updated correctly
     * 2. The correct repository method is called
     */
    @Test
    void updateUserDetails_Success() {
        // Arrange
        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);

        // Act
        User result = userService.updateUserDetails(1L, testUser);

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getEmail(), result.getEmail());
        verify(userRepository).save(any());
    }

    /**
     * Tests successful deletion of a user.
     * Verifies that:
     * 1. The user is deleted
     * 2. The correct repository method is called
     */
    @Test
    void deleteUser_Success() {
        // Arrange
        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));

        // Act
        userService.deleteUser(1L);

        // Assert
        verify(userRepository).delete(testUser);
    }
} 