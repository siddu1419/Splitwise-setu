package com.example.demo.service.split;

import com.example.demo.model.Expense;
import com.example.demo.model.ExpenseShare;
import java.util.List;

public interface ExpenseSplitStrategy {
    void validateShares(Expense expense, List<ExpenseShare> shares);
} 