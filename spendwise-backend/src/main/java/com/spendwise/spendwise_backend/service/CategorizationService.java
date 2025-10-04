package com.spendwise.spendwise_backend.service;

import com.spendwise.spendwise_backend.model.Category;
import com.spendwise.spendwise_backend.model.CategoryGroup;
import com.spendwise.spendwise_backend.repo.CategoryRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategorizationService {
    private final CategoryRepo categoryRepo;

    // ordered map so first match wins
    private static final Map<String, String> KEYWORDS = new LinkedHashMap<>() {{
        put("uber", "Transport");
        put("lyft", "Transport");
        put("shell", "Fuel");
        put("exxon", "Fuel");
        put("starbucks", "Coffee");
        put("mcdonald", "Food");
        put("whole foods", "Groceries");
        put("trader joe", "Groceries");
        put("walmart", "Groceries");
        put("amazon", "Shopping");
    }};

    public Category guess(String description) {
        final String d = description == null ? "" : description.toLowerCase();

        // ----- DEBT -----
        if (d.contains("mortgage"))       return ensure("Mortgage", false, CategoryGroup.DEBT);
        if (d.contains("home loan"))      return ensure("Home Loan", false, CategoryGroup.DEBT);
        if (d.contains("car loan") || d.contains("auto loan")) return ensure("Car Loan", false, CategoryGroup.DEBT);
        if (d.contains("student loan"))   return ensure("Student Loan", false, CategoryGroup.DEBT);
        if (d.contains("personal loan"))  return ensure("Personal Loan", false, CategoryGroup.DEBT);
        if (d.contains("credit card payment") || d.contains("cc payment") || d.contains("card payment"))
            return ensure("Credit Card Payment", false, CategoryGroup.DEBT);
        if (d.contains("emi") || d.contains("installment"))
            return ensure("Debt Repayment", false, CategoryGroup.DEBT);

        // ----- ESSENTIAL -----
        if (d.contains("uber") || d.contains("lyft"))              return ensure("Transport", false, CategoryGroup.ESSENTIAL);
        if (d.contains("shell") || d.contains("exxon"))            return ensure("Fuel", false, CategoryGroup.ESSENTIAL);
        if (d.contains("whole foods") || d.contains("trader joe") || d.contains("walmart"))
            return ensure("Groceries", false, CategoryGroup.ESSENTIAL);
        if (d.contains("comcast") || d.contains("xfinity") || d.contains("att") || d.contains("verizon"))
            return ensure("Internet", false, CategoryGroup.ESSENTIAL);

        // ----- SURPLUS -----
        if (d.contains("starbucks"))                               return ensure("Coffee", false, CategoryGroup.SURPLUS);
        if (d.contains("mcdonald") || d.contains("chipotle") || d.contains("pizza"))
            return ensure("Eating Out", false, CategoryGroup.SURPLUS);
        if (d.contains("amazon") || d.contains("best buy") || d.contains("target"))
            return ensure("Shopping", false, CategoryGroup.SURPLUS);

        return ensure("Uncategorized", false, CategoryGroup.SURPLUS);
    }

    private Category ensure(String name, boolean isIncome, CategoryGroup group) {
        return categoryRepo.findByNameIgnoreCase(name).orElseGet(() ->
                categoryRepo.save(Category.builder().name(name).isIncome(isIncome).group(group).build())
        );
    }

}
