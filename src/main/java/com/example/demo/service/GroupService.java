package com.example.demo.service;

import com.example.demo.exception.ExpenseValidationException;
import com.example.demo.model.Group;
import com.example.demo.model.User;
import com.example.demo.repository.GroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class GroupService {
    private static final Logger logger = LoggerFactory.getLogger(GroupService.class);

    private final GroupRepository groupRepository;
    private final UserService userService;

    public GroupService(GroupRepository groupRepository, UserService userService) {
        this.groupRepository = groupRepository;
        this.userService = userService;
    }

    @Transactional
    public Group createGroup(Group group, User creator) {
        logger.debug("Creating new group: {} by user: {}", group.getName(), creator.getEmail());
        try {
            group.setCreatedBy(creator);
            group.setMembers(new HashSet<>());
            group.getMembers().add(creator);
            Group savedGroup = groupRepository.save(group);
            logger.info("Successfully created group: {} with ID: {}", savedGroup.getName(), savedGroup.getId());
            return savedGroup;
        } catch (Exception e) {
            logger.error("Error creating group: {}", e.getMessage());
            throw new ExpenseValidationException("Error creating group: " + e.getMessage());
        }
    }

    @Transactional
    public Group addMember(Long groupId, Long userId) {
        logger.debug("Adding user: {} to group: {}", userId, groupId);
        try {
            Group group = getGroupById(groupId);
            User user = userService.getUserById(userId);
            
            if (group.getMembers().contains(user)) {
                logger.warn("User: {} is already a member of group: {}", userId, groupId);
                throw new ExpenseValidationException("User is already a member of this group");
            }
            
            Set<User> members = new HashSet<>(group.getMembers());
            members.add(user);
            group.setMembers(members);
            Group savedGroup = groupRepository.save(group);
            logger.info("Successfully added user: {} to group: {}", userId, groupId);
            return savedGroup;
        } catch (Exception e) {
            logger.error("Error adding member to group: {}", e.getMessage());
            throw new ExpenseValidationException("Error adding member to group: " + e.getMessage());
        }
    }

    @Transactional
    public Group removeMember(Long groupId, Long userId) {
        logger.debug("Removing user: {} from group: {}", userId, groupId);
        try {
            Group group = getGroupById(groupId);
            User user = userService.getUserById(userId);
            
            if (!group.getMembers().contains(user)) {
                logger.warn("User: {} is not a member of group: {}", userId, groupId);
                throw new ExpenseValidationException("User is not a member of this group");
            }
            
            Set<User> members = new HashSet<>(group.getMembers());
            members.remove(user);
            group.setMembers(members);
            Group savedGroup = groupRepository.save(group);
            logger.info("Successfully removed user: {} from group: {}", userId, groupId);
            return savedGroup;
        } catch (Exception e) {
            logger.error("Error removing member from group: {}", e.getMessage());
            throw new ExpenseValidationException("Error removing member from group: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Group getGroupById(Long id) {
        logger.debug("Fetching group by ID: {}", id);
        try {
            return groupRepository.findById(id)
                    .orElseThrow(() -> new ExpenseValidationException("Group not found with id: " + id));
        } catch (Exception e) {
            logger.error("Error fetching group by ID: {}", e.getMessage());
            throw new ExpenseValidationException("Error fetching group: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Set<Group> getUserGroups(User user) {
        logger.debug("Fetching groups for user: {}", user.getEmail());
        try {
            return new HashSet<>(groupRepository.findByMembersContaining(user));
        } catch (Exception e) {
            logger.error("Error fetching user groups: {}", e.getMessage());
            throw new ExpenseValidationException("Error fetching user groups: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Set<Group> getGroupsCreatedBy(User user) {
        logger.debug("Fetching groups created by user: {}", user.getEmail());
        try {
            return new HashSet<>(groupRepository.findByCreatedBy(user));
        } catch (Exception e) {
            logger.error("Error fetching groups created by user: {}", e.getMessage());
            throw new ExpenseValidationException("Error fetching groups created by user: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Set<User> getGroupMembers(Long groupId) {
        logger.debug("Fetching members for group: {}", groupId);
        try {
            Group group = getGroupById(groupId);
            return new HashSet<>(group.getMembers());
        } catch (Exception e) {
            logger.error("Error fetching group members: {}", e.getMessage());
            throw new ExpenseValidationException("Error fetching group members: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteGroup(Long groupId) {
        logger.debug("Deleting group: {}", groupId);
        try {
            Group group = getGroupById(groupId);
            groupRepository.delete(group);
            logger.info("Successfully deleted group: {}", groupId);
        } catch (Exception e) {
            logger.error("Error deleting group: {}", e.getMessage());
            throw new ExpenseValidationException("Error deleting group: " + e.getMessage());
        }
    }
} 