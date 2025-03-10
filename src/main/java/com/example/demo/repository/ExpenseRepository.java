package com.example.demo.repository;

import com.example.demo.model.Expense;
import com.example.demo.model.ExpenseShare;
import com.example.demo.model.Group;
import com.example.demo.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    Page<Expense> findByGroup(Group group, Pageable pageable);
    List<Expense> findByPaidBy(User user);
    
    @Query("SELECT es FROM ExpenseShare es WHERE es.user = :user")
    List<ExpenseShare> findSharesByUser(@Param("user") User user);
    
    @Query("SELECT es FROM ExpenseShare es WHERE es.user = :user AND es.settled = false")
    List<ExpenseShare> findUnsettledSharesByUser(@Param("user") User user);
    
    @Query("SELECT es FROM ExpenseShare es WHERE es.expense.group.id = :groupId AND es.user = :user AND es.settled = false")
    List<ExpenseShare> findUnsettledSharesByGroupAndUser(@Param("groupId") Long groupId, @Param("user") User user);
    
    @Query("SELECT es FROM ExpenseShare es WHERE es.id = :shareId")
    Optional<ExpenseShare> findShareById(@Param("shareId") Long shareId);
} 