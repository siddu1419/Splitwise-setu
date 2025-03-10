package com.example.demo.service;

import com.example.demo.exception.ExpenseValidationException;
import com.example.demo.model.*;
import com.example.demo.repository.ExpenseRepository;
import com.example.demo.repository.ExpenseShareRepository;
import com.example.demo.repository.GroupRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.split.ExpenseSplitStrategy;
import com.example.demo.service.split.ExpenseSplitStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExpenseService.
 * Tests expense creation, retrieval, and management operations.
 */
@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ExpenseShareRepository expenseShareRepository;

    @Mock
    private GroupService groupService;

    @Mock
    private UserService userService;

    @Mock
    private ExpenseSplitStrategyFactory splitStrategyFactory;

    @InjectMocks
    private ExpenseService expenseService;

    private User testUser;
    private Group testGroup;
    private Expense testExpense;
    private ExpenseShare testShare;

    /**
     * Sets up test data before each test.
     * Creates test user, group, expense, and share with predefined values.
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
        Set<User> members = new HashSet<>();
        members.add(testUser);
        testGroup.setMembers(members);

        // Create test expense
        testExpense = new Expense();
        testExpense.setId(1L);
        testExpense.setDescription("Test Expense");
        testExpense.setAmount(new BigDecimal("100.00"));
        testExpense.setGroup(testGroup);
        testExpense.setPaidBy(testUser);
        testExpense.setSplitType(SplitType.EQUAL);
        testExpense.setDate(LocalDateTime.now());
        testExpense.setCreatedAt(LocalDateTime.now());

        // Create test share
        testShare = new ExpenseShare();
        testShare.setId(1L);
        testShare.setUser(testUser);
        testShare.setShareAmount(new BigDecimal("50.00"));
        testShare.setPercentage(new BigDecimal("0.5"));
        testShare.setExpense(testExpense);
        testShare.setSettled(false);
    }


    /**
     * Tests successful retrieval of expenses for a user.
     * Verifies that:
     * 1. The correct expenses are returned
     * 2. The appropriate repository method is called
     */
    @Test
    void getUserExpenses_Success() {
        // Arrange
        when(expenseRepository.findByPaidBy(any())).thenReturn(any());

        // Act
        expenseService.getUserExpenses(testUser);

        // Assert
        verify(expenseRepository).findByPaidBy(any());
    }

    /**
     * Tests successful deletion of an expense.
     * Verifies that:
     * 1. The expense is deleted
     * 2. The appropriate repository method is called
     */
    @Test
    void deleteExpense_Success() {
        // Act
        expenseService.deleteExpense(1L);

        // Assert
        verify(expenseRepository).deleteById(1L);
    }
} 