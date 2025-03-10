package com.example.demo.controller;

import com.example.demo.dto.GroupDTO;
import com.example.demo.dto.UserDTO;
import com.example.demo.model.Group;
import com.example.demo.model.User;
import com.example.demo.service.GroupService;
import com.example.demo.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;
    private final UserService userService;

    public GroupController(GroupService groupService, UserService userService) {
        this.groupService = groupService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<GroupDTO> createGroup(@RequestBody Group group) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User creator = userService.getUserByEmail(authentication.getName());
        Group createdGroup = groupService.createGroup(group, creator);
        return ResponseEntity.ok(convertToDTO(createdGroup));
    }

    @GetMapping
    public ResponseEntity<Set<GroupDTO>> getUserGroups() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.getUserByEmail(authentication.getName());
        Set<Group> groups = groupService.getUserGroups(user);
        return ResponseEntity.ok(groups.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toSet()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupDTO> getGroupById(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.getUserByEmail(authentication.getName());
        Group group = groupService.getGroupById(id);
        
        if (!group.getMembers().contains(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(convertToDTO(group));
    }

    @PostMapping("/{groupId}/members/{userId}")
    public ResponseEntity<GroupDTO> addMember(@PathVariable Long groupId, @PathVariable Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.getUserByEmail(authentication.getName());
        Group group = groupService.getGroupById(groupId);
        
        if (!group.getCreatedBy().equals(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            Group updatedGroup = groupService.addMember(groupId, userId);
            return ResponseEntity.ok(convertToDTO(updatedGroup));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("User not found")) {
                return ResponseEntity.badRequest().build();
            } else if (e.getMessage().contains("Group not found")) {
                return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<GroupDTO> removeMember(@PathVariable Long groupId, @PathVariable Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.getUserByEmail(authentication.getName());
        Group group = groupService.getGroupById(groupId);
        
        if (!group.getCreatedBy().equals(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            Group updatedGroup = groupService.removeMember(groupId, userId);
            return ResponseEntity.ok(convertToDTO(updatedGroup));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("User not found")) {
                return ResponseEntity.badRequest().build();
            } else if (e.getMessage().contains("Group not found")) {
                return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<Set<UserDTO>> getGroupMembers(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.getUserByEmail(authentication.getName());
        Group group = groupService.getGroupById(id);
        
        if (!group.getMembers().contains(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            Set<User> members = groupService.getGroupMembers(id);
            return ResponseEntity.ok(members.stream()
                    .map(this::convertToUserDTO)
                    .collect(Collectors.toSet()));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Group not found")) {
                return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.getUserByEmail(authentication.getName());
        Group group = groupService.getGroupById(id);
        
        if (!group.getCreatedBy().equals(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            groupService.deleteGroup(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Group not found")) {
                return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.internalServerError().build();
        }
    }

    private GroupDTO convertToDTO(Group group) {
        GroupDTO dto = new GroupDTO();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        dto.setCreatedById(group.getCreatedBy().getId());
        dto.setCreatedByName(group.getCreatedBy().getName());
        dto.setMembers(group.getMembers().stream()
                .map(this::convertToUserDTO)
                .collect(Collectors.toSet()));
        return dto;
    }

    private UserDTO convertToUserDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        return dto;
    }
} 