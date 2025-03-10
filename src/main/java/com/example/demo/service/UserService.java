package com.example.demo.service;

import com.example.demo.exception.ExpenseValidationException;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Loading user by username: {}", username);
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
    }

    @Transactional
    public User registerUser(User user) {
        logger.info("Attempting to register new user: {}", user.getEmail());
        try {
            if (userRepository.existsByEmail(user.getEmail())) {
                logger.error("Email already registered: {}", user.getEmail());
                throw new ExpenseValidationException("Email already registered");
            }

            user.setPassword(passwordEncoder.encode(user.getPassword()));
            User savedUser = userRepository.save(user);
            logger.info("Successfully registered user: {}", savedUser.getEmail());
            return savedUser;
        } catch (Exception e) {
            logger.error("Error registering user: {}", e.getMessage());
            throw new ExpenseValidationException("Error registering user: " + e.getMessage());
        }
    }

    public User getUserById(Long id) {
        logger.debug("Fetching user by ID: {}", id);
        try {
            return userRepository.findById(id)
                    .orElseThrow(() -> new ExpenseValidationException("User not found with id: " + id));
        } catch (Exception e) {
            logger.error("Error fetching user by ID: {}", e.getMessage());
            throw new ExpenseValidationException("Error fetching user: " + e.getMessage());
        }
    }

    public User getUserByEmail(String email) {
        logger.debug("Fetching user by email: {}", email);
        try {
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new ExpenseValidationException("User not found with email: " + email));
        } catch (Exception e) {
            logger.error("Error fetching user by email: {}", e.getMessage());
            throw new ExpenseValidationException("Error fetching user: " + e.getMessage());
        }
    }

    public User loginUser(String email, String password) {
        logger.debug("Attempting login for user: {}", email);
        User user = getUserByEmail(email);
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            logger.error("Invalid password for user: {}", email);
            throw new ExpenseValidationException("Invalid password");
        }
        
        logger.info("Successfully logged in user: {}", email);
        return user;
    }

    @Transactional
    public User updateUserDetails(Long id, User userDetails) {
        logger.debug("Updating user details for ID: {}", id);
        User user = getUserById(id);
        
        user.setName(userDetails.getName());
        user.setEmail(userDetails.getEmail());
        
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }
        
        User updatedUser = userRepository.save(user);
        logger.info("Successfully updated user: {}", updatedUser.getEmail());
        return updatedUser;
    }

    @Transactional
    public void deleteUser(Long id) {
        logger.debug("Deleting user with ID: {}", id);
        User user = getUserById(id);
        userRepository.delete(user);
        logger.info("Successfully deleted user: {}", user.getEmail());
    }
} 