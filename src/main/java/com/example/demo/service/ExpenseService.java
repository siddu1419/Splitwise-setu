package com.example.demo.service;

import com.example.demo.model.Expense;
import com.example.demo.model.ExpenseShare;
import com.example.demo.model.Group;
import com.example.demo.model.User;
import com.example.demo.model.SplitType;
import com.example.demo.repository.ExpenseRepository;
import com.example.demo.service.split.ExpenseSplitStrategy;
import com.example.demo.service.split.ExpenseSplitStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service class for managing expenses and their shares in the application.
 * Handles creation, retrieval, and management of expenses and their associated shares.
 */
@Service
public class ExpenseService {
    private static final Logger logger = LoggerFactory.getLogger(ExpenseService.class);

    private final ExpenseRepository expenseRepository;
    private final GroupService groupService;
    private final UserService userService;
    private final ExpenseSplitStrategyFactory splitStrategyFactory;

    /**
     * Constructs a new ExpenseService with required dependencies.
     *
     * @param expenseRepository Repository for expense operations
     * @param groupService Service for group operations
     * @param userService Service for user operations
     * @param splitStrategyFactory Factory for creating split strategies
     */
    public ExpenseService(ExpenseRepository expenseRepository,
                         GroupService groupService,
                         UserService userService,
                         ExpenseSplitStrategyFactory splitStrategyFactory) {
        this.expenseRepository = expenseRepository;
        this.groupService = groupService;
        this.userService = userService;
        this.splitStrategyFactory = splitStrategyFactory;
    }

    /**
     * Creates a new expense in a group with specified shares.
     *
     * @param expense The expense to create
     * @param groupId The ID of the group to create the expense in
     * @param userId The ID of the user who paid for the expense
     * @return The created expense with calculated shares
     */
    @Transactional
    public Expense createExpense(Expense expense, Long groupId, Long userId) {
        logger.debug("Creating expense in group: {} by user: {}", groupId, userId);
        
        Group group = groupService.getGroupById(groupId);
        User paidBy = userService.getUserById(userId);

        if (paidBy == null) {
            throw new RuntimeException("Paid by user not found");
        }
        
        // Set the group and paidBy fields
        expense.setGroup(group);
        expense.setPaidBy(paidBy);
        
        // Get the appropriate split strategy
        ExpenseSplitStrategy strategy = splitStrategyFactory.getStrategy(expense.getSplitType());
        
        // Get list of users who need to pay
        Set<User> usersToPay = expense.getShares().stream()
                .map(share -> userService.getUserById(share.getUser().getId()))
                .collect(Collectors.toSet());
        
        // Calculate shares based on split type
        if (expense.getSplitType() == SplitType.EQUAL) {
            // For equal split, divide amount equally among all users
            BigDecimal shareAmount = expense.getAmount()
                    .divide(BigDecimal.valueOf(usersToPay.size()), 2, BigDecimal.ROUND_HALF_UP);
            BigDecimal sharePercentage = BigDecimal.ONE
                    .divide(BigDecimal.valueOf(usersToPay.size()), 2, BigDecimal.ROUND_HALF_UP);
            
            expense.getShares().clear();
            for (User user : usersToPay) {
                ExpenseShare share = new ExpenseShare();
                share.setUser(user);
                share.setShareAmount(shareAmount);
                share.setPercentage(sharePercentage);
                share.setExpense(expense);
                expense.getShares().add(share);
            }
        } else if (expense.getSplitType() == SplitType.UNEQUAL) {
            // For unequal split, use the provided share amounts
            for (ExpenseShare share : expense.getShares()) {
                share.setExpense(expense);
                // Calculate percentage based on share amount
                share.setPercentage(share.getShareAmount()
                        .divide(expense.getAmount(), 2, BigDecimal.ROUND_HALF_UP));
            }
        } else if (expense.getSplitType() == SplitType.PERCENTAGE) {
            // For percentage split, calculate share amounts from percentages
            for (ExpenseShare share : expense.getShares()) {
                share.setExpense(expense);
                share.setShareAmount(expense.getAmount()
                        .multiply(share.getPercentage())
                        .setScale(2, BigDecimal.ROUND_HALF_UP));
            }
        }
        
        // Validate shares
        strategy.validateShares(expense, expense.getShares().stream().toList());
        
        // Set the date fields
        expense.setDate(LocalDateTime.now());
        expense.setCreatedAt(LocalDateTime.now());
        
        // Ensure paidBy is set and not null
        if (expense.getPaidBy() == null) {
            expense.setPaidBy(paidBy);
        }
        
        // Save the expense first to get the ID
        Expense savedExpense = expenseRepository.save(expense);
        
        // Update the shares with the saved expense
        for (ExpenseShare share : savedExpense.getShares()) {
            share.setExpense(savedExpense);
        }
        
        // Save again to ensure all relationships are properly set
        savedExpense = expenseRepository.save(savedExpense);
        
        logger.info("Created expense: {} in group: {} by user: {}", 
            savedExpense.getId(), 
            groupId,
            savedExpense.getPaidBy() != null ? savedExpense.getPaidBy().getId() : "null");
        return savedExpense;
    }

    /**
     * Retrieves a paginated list of expenses for a specific group.
     *
     * @param groupId The ID of the group to get expenses for
     * @param pageable Pagination information
     * @return A page of expenses for the specified group
     */
    public Page<Expense> getGroupExpenses(Long groupId, Pageable pageable) {
        Group group = groupService.getGroupById(groupId);
        return expenseRepository.findByGroup(group, pageable);
    }

    /**
     * Retrieves all expenses where the specified user is the payer.
     *
     * @param user The user to get expenses for
     * @return List of expenses paid by the specified user
     */
    public List<Expense> getUserExpenses(User user) {
        return expenseRepository.findByPaidBy(user);
    }

    /**
     * Retrieves all expense shares associated with a specific user.
     *
     * @param user The user to get shares for
     * @return List of expense shares for the specified user
     */
    public List<ExpenseShare> getUserExpenseShares(User user) {
        return expenseRepository.findSharesByUser(user);
    }

    /**
     * Retrieves all unsettled expense shares for a specific user.
     *
     * @param user The user to get unsettled shares for
     * @return List of unsettled expense shares for the specified user
     */
    public List<ExpenseShare> getUserUnsettledShares(User user) {
        return expenseRepository.findUnsettledSharesByUser(user);
    }

    /**
     * Retrieves all unsettled expense shares for a user in a specific group.
     *
     * @param groupId The ID of the group to get shares for
     * @param user The user to get unsettled shares for
     * @return List of unsettled expense shares for the user in the specified group
     */
    public List<ExpenseShare> getGroupUserUnsettledShares(Long groupId, User user) {
        return expenseRepository.findUnsettledSharesByGroupAndUser(groupId, user);
    }

    /**
     * Retrieves an expense share by its ID.
     *
     * @param shareId The ID of the share to retrieve
     * @return The expense share with the specified ID
     * @throws RuntimeException if the share is not found
     */
    public ExpenseShare getExpenseShareById(Long shareId) {
        return expenseRepository.findShareById(shareId)
                .orElseThrow(() -> new RuntimeException("Share not found with id: " + shareId));
    }

    /**
     * Marks an expense share as settled.
     *
     * @param shareId The ID of the share to settle
     */
    @Transactional
    public void settleExpenseShare(Long shareId) {
        ExpenseShare share = getExpenseShareById(shareId);
        share.setSettled(true);
        expenseRepository.save(share.getExpense());
        logger.info("Settled share: {} for expense: {}", shareId, share.getExpense().getId());
    }

    /**
     * Deletes an expense by its ID.
     *
     * @param id The ID of the expense to delete
     */
    @Transactional
    public void deleteExpense(Long id) {
        expenseRepository.deleteById(id);
        logger.info("Deleted expense: {}", id);
    }
} 