package com.spendwise.spendwise_backend.web;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/months")
@RequiredArgsConstructor
public class MonthController {
    private final EntityManager em;

    @GetMapping
    public Map<String, Object> months() {
        @SuppressWarnings("unchecked")
        List<String> months = em.createNativeQuery("""
      select to_char(date_trunc('month', posted_at), 'YYYY-MM') as ym
      from "transaction"
      group by 1
      order by 1 desc
    """).getResultList();
        return Map.of("months", months);
    }
}
