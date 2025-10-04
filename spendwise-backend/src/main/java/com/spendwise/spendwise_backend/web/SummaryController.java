package com.spendwise.spendwise_backend.web;

import com.spendwise.spendwise_backend.model.CategoryGroup;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
public class SummaryController {

    private final EntityManager em;

    @GetMapping("/monthly/by-group")
    public Map<String,Object> byGroup(@RequestParam String month) {
        LocalDate start = LocalDate.parse(month + "-01");
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        BigDecimal income = (BigDecimal) em.createQuery("""
        select coalesce(sum(t.amount), 0) from Transaction t
        where t.postedAt between :s and :e and t.amount > 0
      """).setParameter("s", start).setParameter("e", end).getSingleResult();

        BigDecimal expenses = ((BigDecimal) em.createQuery("""
        select coalesce(sum(t.amount), 0) from Transaction t
        where t.postedAt between :s and :e and t.amount < 0
      """).setParameter("s", start).setParameter("e", end).getSingleResult()).abs();

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery("""
        select c.group, abs(coalesce(sum(t.amount), 0))
        from Transaction t left join t.category c
        where t.postedAt between :s and :e and t.amount < 0 and c.group is not null
        group by c.group
      """).setParameter("s", start).setParameter("e", end).getResultList();

        Map<CategoryGroup, BigDecimal> byGroup = new EnumMap<>(CategoryGroup.class);
        for (CategoryGroup g : CategoryGroup.values()) byGroup.put(g, BigDecimal.ZERO);
        for (Object[] r : rows) byGroup.put((CategoryGroup) r[0], (BigDecimal) r[1]);

        return Map.of(
                "month", month,
                "income", income,
                "expenses", expenses,
                "byGroup", Map.of(
                        "ESSENTIAL", byGroup.get(CategoryGroup.ESSENTIAL),
                        "SURPLUS",   byGroup.get(CategoryGroup.SURPLUS),
                        "DEBT",      byGroup.get(CategoryGroup.DEBT)
                ),
                "net", income.subtract(expenses)
        );
    }
}
