package com.example.demo.dto;

import lombok.Data;
import java.util.Set;

@Data
public class GroupDTO {
    private Long id;
    private String name;
    private String description;
    private Long createdById;
    private String createdByName;
    private Set<UserDTO> members;
} 