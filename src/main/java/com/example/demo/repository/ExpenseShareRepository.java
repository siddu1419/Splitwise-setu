package com.example.demo.repository;

import com.example.demo.model.ExpenseShare;
import com.example.demo.model.Group;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseShareRepository extends JpaRepository<ExpenseShare, Long> {
    List<ExpenseShare> findByUser(User user);
    List<ExpenseShare> findByUserAndSettledFalse(User user);
    List<ExpenseShare> findByExpense_GroupAndUserAndSettledFalse(Group group, User user);
} 