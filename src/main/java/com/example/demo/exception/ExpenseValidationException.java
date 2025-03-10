package com.example.demo.exception;

public class ExpenseValidationException extends RuntimeException {
    public ExpenseValidationException(String message) {
        super(message);
    }
} 