package com.spendwise.spendwise_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate postedAt;

    @Column(nullable = false)
    private String description;

    // Convention: expenses are NEGATIVE, income is POSITIVE
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(columnDefinition = "text")
    private String raw;  // optional JSON/CSV row as text

    @Column
    private String hash; // optional for de-dupe
}
