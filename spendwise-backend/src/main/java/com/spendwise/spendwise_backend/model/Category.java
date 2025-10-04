package com.spendwise.spendwise_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "category")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    // income is kept separate from expense grouping
    @Column(nullable = false)
    private Boolean isIncome = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_type")    // nullable: income categories have no expense group
    private CategoryGroup group;
}
