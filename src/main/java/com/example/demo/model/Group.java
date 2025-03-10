package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "groups")
@Data
@NoArgsConstructor
public class Group {
    private static final Logger logger = LoggerFactory.getLogger(Group.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "group_members",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> members = new HashSet<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Expense> expenses = new HashSet<>();

    @PrePersist
    public void prePersist() {
        logger.info("Creating new group: {}", this.name);
        if (members == null) {
            members = new HashSet<>();
        }
        if (expenses == null) {
            expenses = new HashSet<>();
        }
    }

    @PostLoad
    public void postLoad() {
        logger.debug("Loaded group: {} with {} members", this.name, this.members.size());
    }
} 