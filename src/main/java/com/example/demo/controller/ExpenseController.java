package com.example.demo.controller;

import com.example.demo.exception.ExpenseValidationException;
import com.example.demo.model.Expense;
import com.example.demo.model.ExpenseShare;
import com.example.demo.model.Group;
import com.example.demo.model.User;
import com.example.demo.model.SplitType;
import com.example.demo.service.ExpenseService;
import com.example.demo.service.GroupService;
import com.example.demo.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * REST controller for managing expenses and their shares.
 * Handles expense creation, retrieval, and settlement operations.
 * Provides endpoints for:
 * - Creating expenses with different split types (EQUAL, UNEQUAL, PERCENTAGE)
 * - Retrieving expenses for a group
 * - Managing expense shares and settlements
 * - Deleting expenses
 */
@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {
    private static final Logger logger = LoggerFactory.getLogger(ExpenseController.class);

    private final ExpenseService expenseService;
    private final UserService userService;
    private final GroupService groupService;

    /**
     * Constructs a new ExpenseController with required dependencies.
     *
     * @param expenseService Service for expense operations
     * @param userService Service for user operations
     * @param groupService Service for group operations
     */
    public ExpenseController(ExpenseService expenseService, UserService userService, GroupService groupService) {
        this.expenseService = expenseService;
        this.userService = userService;
        this.groupService = groupService;
    }

    /**
     * Creates a new expense in a group with the specified split type and shares.
     * Validates that:
     * - The current user is a member of the group
     * - The expense amount is positive
     * - The shares are valid based on the split type
     * - All users in shares are members of the group
     *
     * @param groupId The ID of the group to create the expense in
     * @param expense The expense details to create
     * @return ResponseEntity containing the created expense with calculated shares
     * @throws ExpenseValidationException if validation fails
     */
    @PostMapping("/groups/{groupId}")
    public ResponseEntity<Map<String, Object>> createExpense(@PathVariable Long groupId, @RequestBody Expense expense) {
        logger.debug("Creating expense in group: {}", groupId);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.getUserByEmail(authentication.getName());
        
        // Check if user is a member of the group
        Group group = groupService.getGroupById(groupId);
        logger.debug("Current user: {}, Group members: {}", currentUser.getEmail(), 
            group.getMembers().stream().map(User::getEmail).toList());
        
        if (!group.getMembers().contains(currentUser)) {
            throw new ExpenseValidationException("User is not a member of this group");
        }
        
        // Set the paidBy user to the current user
        expense.setPaidBy(currentUser);
        
        // Validate expense data
        validateExpense(expense, group);
        
        // Create the expense with the current user as paidBy
        Expense createdExpense = expenseService.createExpense(expense, groupId, currentUser.getId());
        
        // Create a response map with only the necessary fields
        Map<String, Object> response = new HashMap<>();
        response.put("id", createdExpense.getId());
        response.put("description", createdExpense.getDescription());
        response.put("amount", createdExpense.getAmount());
        response.put("splitType", createdExpense.getSplitType());
        response.put("date", createdExpense.getDate());
        response.put("createdAt", createdExpense.getCreatedAt());
        
        // Add group info
        Map<String, Object> groupInfo = new HashMap<>();
        groupInfo.put("id", group.getId());
        groupInfo.put("name", group.getName());
        groupInfo.put("description", group.getDescription());
        response.put("group", groupInfo);
        
        // Add paidBy info
        Map<String, Object> paidByInfo = new HashMap<>();
        paidByInfo.put("id", currentUser.getId());
        paidByInfo.put("email", currentUser.getEmail());
        paidByInfo.put("name", currentUser.getName());
        response.put("paidBy", paidByInfo);
        
        // Add shares info
        List<Map<String, Object>> sharesInfo = createdExpense.getShares().stream()
            .map(share -> {
                Map<String, Object> shareInfo = new HashMap<>();
                shareInfo.put("id", share.getId());
                shareInfo.put("shareAmount", share.getShareAmount());
                shareInfo.put("percentage", share.getPercentage());
                shareInfo.put("settled", share.isSettled());
                
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", share.getUser().getId());
                userInfo.put("email", share.getUser().getEmail());
                userInfo.put("name", share.getUser().getName());
                shareInfo.put("user", userInfo);
                
                return shareInfo;
            })
            .collect(Collectors.toList());
        response.put("shares", sharesInfo);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Validates an expense and its shares based on the split type.
     * For EQUAL split:
     * - Divides amount equally among shares
     * - Handles rounding by giving remainder to last person
     * For UNEQUAL split:
     * - Validates total share amount equals expense amount
     * - Allows 1 cent tolerance for rounding
     * For PERCENTAGE split:
     * - Supports both 0-1 and 0-100 percentage formats
     * - Validates total percentage equals 1.0 or 100
     * - Calculates share amounts based on percentages
     *
     * @param expense The expense to validate
     * @param group The group the expense belongs to
     * @throws ExpenseValidationException if validation fails
     */
    private void validateExpense(Expense expense, Group group) {
        // Validate paidBy user
        if (expense.getPaidBy() == null || expense.getPaidBy().getId() == null) {
            throw new ExpenseValidationException("Paid by user is required");
        }
        User paidByUser = userService.getUserById(expense.getPaidBy().getId());
        if (!group.getMembers().contains(paidByUser)) {
            throw new ExpenseValidationException("Paid by user is not a member of this group");
        }

        // Validate shares
        if (expense.getShares() == null || expense.getShares().isEmpty()) {
            throw new ExpenseValidationException("At least one share is required");
        }

        // Validate users in shares are group members
        for (ExpenseShare share : expense.getShares()) {
            if (share.getUser() == null || share.getUser().getId() == null) {
                throw new ExpenseValidationException("Each share must have a user");
            }
            User shareUser = userService.getUserById(share.getUser().getId());
            if (!group.getMembers().contains(shareUser)) {
                throw new ExpenseValidationException("Share user is not a member of this group");
            }
        }

        // Validate based on split type
        if (expense.getSplitType() == SplitType.UNEQUAL) {
            BigDecimal totalShareAmount = BigDecimal.ZERO;
            for (ExpenseShare share : expense.getShares()) {
                if (share.getShareAmount() == null || share.getShareAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ExpenseValidationException("Share amount must be greater than 0");
                }
                totalShareAmount = totalShareAmount.add(share.getShareAmount());
            }
            
            // Use a tolerance for comparison instead of exact equality
            BigDecimal tolerance = new BigDecimal("0.01"); // 1 cent tolerance
            BigDecimal difference = totalShareAmount.subtract(expense.getAmount()).abs();
            
            if (difference.compareTo(tolerance) > 0) {
                throw new ExpenseValidationException(
                    String.format("Total share amount (%s) must equal expense amount (%s)", 
                        totalShareAmount.setScale(2, RoundingMode.HALF_UP),
                        expense.getAmount().setScale(2, RoundingMode.HALF_UP)));
            }
        } else if (expense.getSplitType() == SplitType.PERCENTAGE) {
            // Determine if percentages are stored as 0-1 or 0-100
            boolean isPercentageFormat = false;
            for (ExpenseShare share : expense.getShares()) {
                if (share.getPercentage() != null && share.getPercentage().compareTo(new BigDecimal("1.5")) > 0) {
                    isPercentageFormat = true;
                    break;
                }
            }
            
            BigDecimal totalPercentage = BigDecimal.ZERO;
            for (ExpenseShare share : expense.getShares()) {
                if (share.getPercentage() == null) {
                    throw new ExpenseValidationException("Share percentage is required");
                }
                
                // Validate percentage values
                if (isPercentageFormat) {
                    // Percentage is in 0-100 format
                    if (share.getPercentage().compareTo(BigDecimal.ZERO) <= 0 || 
                        share.getPercentage().compareTo(new BigDecimal("100")) > 0) {
                        throw new ExpenseValidationException("Share percentage must be between 0 and 100");
                    }
                } else {
                    // Percentage is in 0-1 format
                    if (share.getPercentage().compareTo(BigDecimal.ZERO) <= 0 || 
                        share.getPercentage().compareTo(BigDecimal.ONE) > 0) {
                        throw new ExpenseValidationException("Share percentage must be between 0 and 1");
                    }
                }
                
                totalPercentage = totalPercentage.add(share.getPercentage());
            }
            
            // Compare with tolerance and different formats
            BigDecimal expectedTotal = isPercentageFormat ? new BigDecimal("100") : BigDecimal.ONE;
            BigDecimal tolerance = isPercentageFormat ? new BigDecimal("0.01") : new BigDecimal("0.0001");
            BigDecimal difference = totalPercentage.subtract(expectedTotal).abs();
            
            if (difference.compareTo(tolerance) > 0) {
                throw new ExpenseValidationException(
                    String.format("Share percentages must sum to %s, but sum to %s", 
                        expectedTotal, totalPercentage.setScale(4, RoundingMode.HALF_UP)));
            }
            
            // Calculate share amounts based on percentages
            for (ExpenseShare share : expense.getShares()) {
                BigDecimal multiplier = isPercentageFormat ? 
                    share.getPercentage().divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP) : 
                    share.getPercentage();
                    
                share.setShareAmount(expense.getAmount().multiply(multiplier)
                    .setScale(2, RoundingMode.HALF_UP));
            }
        } else if (expense.getSplitType() == SplitType.EQUAL) {
            // For EQUAL split, set equal share amounts
            BigDecimal equalShare = expense.getAmount()
                .divide(new BigDecimal(expense.getShares().size()), 2, RoundingMode.HALF_UP);
                
            // Handle potential rounding issues
            BigDecimal totalAllocated = BigDecimal.ZERO;
            int index = 0;
            
            for (ExpenseShare share : expense.getShares()) {
                // Give the remainder to the last person to handle rounding errors
                if (index == expense.getShares().size() - 1) {
                    share.setShareAmount(expense.getAmount().subtract(totalAllocated));
                } else {
                    share.setShareAmount(equalShare);
                    totalAllocated = totalAllocated.add(equalShare);
                }
                index++;
            }
        }
    }

    /**
     * Retrieves a paginated list of expenses for a group.
     * Validates that the current user is a member of the group.
     *
     * @param groupId The ID of the group to get expenses for
     * @param pageable Pagination information
     * @return ResponseEntity containing a page of expenses
     * @throws ExpenseValidationException if user is not a group member
     */
    @GetMapping("/groups/{groupId}")
    public ResponseEntity<Page<Expense>> getGroupExpenses(@PathVariable Long groupId, Pageable pageable) {
        logger.debug("Fetching expenses for group: {}", groupId);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.getUserByEmail(authentication.getName());
        
        // Check if user is a member of the group
        Group group = groupService.getGroupById(groupId);
        if (!group.getMembers().contains(currentUser)) {
            throw new ExpenseValidationException("User is not a member of this group");
        }
        
        Page<Expense> expenses = expenseService.getGroupExpenses(groupId, pageable);
        return ResponseEntity.ok(expenses);
    }

    /**
     * Retrieves all expenses where the current user is the payer.
     * This includes expenses where the user has paid for others.
     *
     * @return ResponseEntity containing a list of expenses where the user is the payer
     */
    @GetMapping("/me")
    public ResponseEntity<List<Expense>> getMyExpenses() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.getUserByEmail(authentication.getName());
        List<Expense> expenses = expenseService.getUserExpenses(user);
        return ResponseEntity.ok(expenses);
    }

    /**
     * Retrieves all expense shares for the current user.
     * This includes both settled and unsettled shares across all groups.
     *
     * @return ResponseEntity containing a list of expense shares
     */
    @GetMapping("/me/shares")
    public ResponseEntity<List<ExpenseShare>> getMyExpenseShares() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.getUserByEmail(authentication.getName());
        List<ExpenseShare> shares = expenseService.getUserExpenseShares(user);
        return ResponseEntity.ok(shares);
    }

    /**
     * Retrieves all unsettled expense shares for the current user.
     * These are shares where the user owes money but hasn't settled yet.
     *
     * @return ResponseEntity containing a list of unsettled shares
     */
    @GetMapping("/me/shares/unsettled")
    public ResponseEntity<List<ExpenseShare>> getMyUnsettledShares() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.getUserByEmail(authentication.getName());
        List<ExpenseShare> shares = expenseService.getUserUnsettledShares(user);
        return ResponseEntity.ok(shares);
    }

    /**
     * Retrieves all unsettled expense shares for the current user in a specific group.
     * Validates that the current user is a member of the group.
     *
     * @param groupId The ID of the group to get shares for
     * @return ResponseEntity containing a list of unsettled shares
     * @throws ExpenseValidationException if user is not a group member
     */
    @GetMapping("/groups/{groupId}/me/shares/unsettled")
    public ResponseEntity<List<ExpenseShare>> getMyGroupUnsettledShares(@PathVariable Long groupId) {
        logger.debug("Fetching unsettled shares for group: {}", groupId);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.getUserByEmail(authentication.getName());
        
        // Check if user is a member of the group
        Group group = groupService.getGroupById(groupId);
        if (!group.getMembers().contains(currentUser)) {
            logger.error("User {} is not a member of group {}", currentUser.getEmail(), groupId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        List<ExpenseShare> shares = expenseService.getGroupUserUnsettledShares(groupId, currentUser);
        return ResponseEntity.ok(shares);
    }

    /**
     * Marks an expense share as settled.
     * Validates that the current user is the owner of the share.
     *
     * @param shareId The ID of the share to settle
     * @return ResponseEntity indicating success
     * @throws ExpenseValidationException if user is not the share owner
     */
    @PostMapping("/shares/{shareId}/settle")
    public ResponseEntity<Void> settleExpenseShare(@PathVariable Long shareId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.getUserByEmail(authentication.getName());
        
        // Check if the share belongs to the current user
        ExpenseShare share = expenseService.getExpenseShareById(shareId);
        if (!share.getUser().equals(currentUser)) {
            logger.error("User {} is not the owner of share {}", currentUser.getEmail(), shareId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        expenseService.settleExpenseShare(shareId);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes an expense by its ID.
     * This will also delete all associated shares.
     *
     * @param id The ID of the expense to delete
     * @return ResponseEntity indicating success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.ok().build();
    }
} 