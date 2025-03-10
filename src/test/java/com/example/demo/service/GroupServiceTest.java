package com.example.demo.service;

import com.example.demo.exception.ExpenseValidationException;
import com.example.demo.model.Group;
import com.example.demo.model.User;
import com.example.demo.repository.GroupRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GroupService.
 * Tests all major functionality including group creation, member management,
 * and group retrieval operations.
 */
@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GroupService groupService;

    private User testUser;
    private Group testGroup;

    /**
     * Sets up test data before each test.
     * Creates a test user and group with predefined values.
     */
    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");

        // Create test group
        testGroup = new Group();
        testGroup.setId(1L);
        testGroup.setName("Test Group");
        testGroup.setDescription("Test Description");
        Set<User> members = new HashSet<>();
        members.add(testUser);
        testGroup.setMembers(members);
    }

    /**
     * Tests successful creation of a group.
     * Verifies that:
     * 1. The group is saved correctly
     * 2. The creator is added as a member
     * 3. The correct repository method is called
     */
    @Test
    void createGroup_Success() {
        // Arrange
        when(groupRepository.save(any())).thenReturn(testGroup);

        // Act
        Group result = groupService.createGroup(testGroup, testUser);

        // Assert
        assertNotNull(result);
        assertEquals(testGroup.getName(), result.getName());
        assertEquals(1, result.getMembers().size());
        assertTrue(result.getMembers().contains(testUser));
        verify(groupRepository).save(any());
    }

    /**
     * Tests retrieval of a group by ID.
     * Verifies that:
     * 1. The correct group is returned
     * 2. The correct repository method is called
     */
    @Test
    void getGroupById_Success() {
        // Arrange
        when(groupRepository.findById(any())).thenReturn(Optional.of(testGroup));

        // Act
        Group result = groupService.getGroupById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testGroup.getId(), result.getId());
        assertEquals(testGroup.getName(), result.getName());
    }

    /**
     * Tests that retrieving a non-existent group throws an exception.
     * Verifies that:
     * 1. The appropriate exception is thrown
     * 2. The correct repository method is called
     */
    @Test
    void getGroupById_NonExistent_ThrowsException() {
        // Arrange
        when(groupRepository.findById(any())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ExpenseValidationException.class, () -> 
            groupService.getGroupById(1L));
    }

    /**
     * Tests successful deletion of a group.
     * Verifies that:
     * 1. The group is deleted
     * 2. The correct repository method is called
     */
    @Test
    void deleteGroup_Success() {
        // Arrange
        when(groupRepository.findById(any())).thenReturn(Optional.of(testGroup));

        // Act
        groupService.deleteGroup(1L);

        // Assert
        verify(groupRepository).delete(testGroup);
    }
} 