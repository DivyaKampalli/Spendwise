package com.spendwise.spendwise_backend.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReaderHeaderAwareBuilder;
import com.spendwise.spendwise_backend.model.Category;
import com.spendwise.spendwise_backend.model.CategoryGroup;
import com.spendwise.spendwise_backend.model.Transaction;
import com.spendwise.spendwise_backend.repo.CategoryRepo;
import com.spendwise.spendwise_backend.repo.TransactionRepo;
import com.spendwise.spendwise_backend.service.CategorizationService;
import com.spendwise.spendwise_backend.web.dto.ImportPreviewRow;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final TransactionRepo txRepo;          // kept for saves; no de-dupe now
    private final CategoryRepo categoryRepo;
    private final CategorizationService categorization;
    private final ObjectMapper mapper = new ObjectMapper();

    @PostMapping(value = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> importCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "month", required = false) String month,                // "YYYY-MM"
            @RequestParam(name = "dryRun", defaultValue = "true") boolean dryRun,
            @RequestParam(name = "overrides", required = false) String overrides,
            @RequestParam(name = "statementType", defaultValue = "debit") String statementType, // "debit" | "credit"
            @RequestParam(name = "exclude", required = false) String exclude             // JSON array of hashes to skip on commit
    ) throws Exception {

        int inserted = 0, skipped = 0, line = 1, rowIndex = 0;
        List<String> errors = new ArrayList<>();
        List<ImportPreviewRow> preview = new ArrayList<>();

        Map<String, String> overrideMap = parseOverrides(overrides);
        Set<String> excludeSet = parseExcludeSet(exclude);

        ensureBaseCategories();

        String csv = new String(file.getBytes(), StandardCharsets.UTF_8);
        String headerLine = firstLine(csv).replace("\uFEFF", "");
        char sep = detectSeparator(headerLine);

        CSVParser parser = new CSVParserBuilder().withSeparator(sep).build();
        try (var reader = new CSVReaderHeaderAwareBuilder(new StringReader(csv))
                .withCSVParser(parser).build()) {

            Map<String, String> rawRow;
            while ((rawRow = reader.readMap()) != null) {
                line++;
                try {
                    Map<String, String> row = normalizeKeys(rawRow);

                    String dateStr = pick(row, "date", "posted date", "transaction date");
                    String desc    = pick(row, "description", "details", "memo", "narrative", "name", "payee");
                    String amtStr  = pick(row, "amount", "transaction amount", "amt");
                    String credit  = pick(row, "credit", "cr");
                    String debit   = pick(row, "debit", "dr", "withdrawal");

                    if ((dateStr == null || desc == null) || (isBlank(amtStr) && isBlank(credit) && isBlank(debit))) {
                        skipped++; errors.add("line " + line + ": missing required fields"); continue;
                    }

                    LocalDate date = parseDateFlexible(dateStr);
                    BigDecimal amount = parseAmountFlexible(amtStr, credit, debit);

                    // Flip signs for credit-card statements (your simplified rule)
                    if ("credit".equalsIgnoreCase(statementType)) {
                        amount = amount.negate();
                    }

                    // Category: income if positive, else guess
                    Category cat = (amount.signum() > 0)
                            ? ensureIncome()
                            : categorization.guess(desc);

                    rowIndex++;
                    String descNorm = desc.trim();

                    // NO duplicate logic any more
                    boolean duplicate = false;

                    // Unique-per-row hash (helps overrides/exclude)
                    String hash = sha256(date + "|" + descNorm + "|" + amount.toPlainString() + "|" + rowIndex);

                    boolean inTargetMonth = true;
                    if (month != null && !month.isBlank()) {
                        var start = LocalDate.parse(month + "-01");
                        var end = start.withDayOfMonth(start.lengthOfMonth());
                        inTargetMonth = !date.isBefore(start) && !date.isAfter(end);
                    }

                    String groupName = (cat.getGroup() == null) ? null : cat.getGroup().name();
                    boolean wouldImport = inTargetMonth;

                    if (dryRun) {
                        preview.add(new ImportPreviewRow(
                                date, descNorm, amount, cat.getName(), groupName,
                                false, inTargetMonth, hash, wouldImport
                        ));
                    } else {
                        // apply category override if present for this hash
                        String overrideName = overrideMap.get(hash);
                        if (overrideName != null && !overrideName.isBlank()) {
                            var chosen = categoryRepo.findByNameIgnoreCase(overrideName.trim())
                                    .orElseGet(() -> categoryRepo.save(
                                            Category.builder().name(overrideName.trim()).isIncome(false)
                                                    .group(CategoryGroup.SURPLUS) // default for brand-new
                                                    .build()
                                    ));
                            cat = chosen;
                        }

                        boolean excluded = excludeSet.contains(hash);

                        if (inTargetMonth && !excluded) {
                            var t = Transaction.builder()
                                    .postedAt(date)
                                    .description(descNorm)
                                    .amount(amount)
                                    .category(cat)
                                    .raw(mapper.writeValueAsString(rawRow))
                                    .hash(hash)
                                    .build();
                            txRepo.save(t);
                            inserted++;
                        } else {
                            skipped++;
                        }
                    }

                } catch (Exception ex) {
                    skipped++;
                    errors.add("line " + line + ": " + ex.getMessage());
                }
            }
        }

        if (dryRun) {
            var sample = preview.size() > 200 ? preview.subList(0, 200) : preview;
            return Map.of(
                    "mode","preview",
                    "month", month,
                    "rows", sample,
                    "totalRows", preview.size(),
                    "errorsSample", errors.size() > 10 ? errors.subList(0,10) : errors,
                    "errorsTotal", errors.size()
            );
        } else {
            return Map.of(
                    "mode","commit",
                    "month", month,
                    "inserted", inserted,
                    "skipped", skipped,
                    "errorsSample", errors.size() > 10 ? errors.subList(0,10) : errors,
                    "errorsTotal", errors.size()
            );
        }
    }

    /* ---------- helpers ---------- */

    private static Map<String, String> parseOverrides(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            var mapper = new ObjectMapper();
            return mapper.readValue(json, mapper.getTypeFactory()
                    .constructMapType(Map.class, String.class, String.class));
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static Set<String> parseExcludeSet(String json) {
        if (json == null || json.isBlank()) return Collections.emptySet();
        try {
            var mapper = new ObjectMapper();
            String[] arr = mapper.readValue(json, String[].class);
            return new HashSet<>(Arrays.asList(arr));
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private static Map<String, String> normalizeKeys(Map<String, String> in) {
        Map<String, String> m = new LinkedHashMap<>();
        for (var e : in.entrySet()) {
            if (e.getKey() == null) continue;
            String k = e.getKey().replace("\uFEFF", "").trim().toLowerCase();
            m.put(k, e.getValue());
        }
        return m;
    }

    private static String pick(Map<String, String> row, String... keys) {
        for (String k : keys) {
            String v = row.get(k);
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    private static LocalDate parseDateFlexible(String s) {
        List<DateTimeFormatter> fmts = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("M/d/uuuu"),
                DateTimeFormatter.ofPattern("d/M/uuuu"),
                DateTimeFormatter.ofPattern("MM-dd-uuuu"),
                DateTimeFormatter.ofPattern("dd-MM-uuuu"),
                DateTimeFormatter.ofPattern("MMM d, uuuu")
        );
        for (var f : fmts) {
            try { return LocalDate.parse(s.trim(), f); } catch (Exception ignored) {}
        }
        throw new IllegalArgumentException("Unrecognized date: " + s);
    }

    private static BigDecimal parseAmountFlexible(String amountCol, String creditCol, String debitCol) {
        if (!isBlank(creditCol) && isBlank(debitCol)) return cleanMoney(creditCol, false).abs();
        if (!isBlank(debitCol)  && isBlank(creditCol)) return cleanMoney(debitCol, true).abs().negate();
        return cleanMoney(amountCol, null);
    }

    private static BigDecimal cleanMoney(String raw, Boolean forceNegative) {
        if (raw == null) throw new IllegalArgumentException("Empty amount");
        String s = raw.trim();

        boolean parenNeg = s.startsWith("(") && s.endsWith(")");
        s = s.replace("(", "").replace(")", "");
        s = s.replaceAll("[,$€£₹\\s\\u00A0\\u202F]", "");

        boolean trailingMinus = s.endsWith("-");
        if (trailingMinus) s = s.substring(0, s.length()-1);

        boolean leadingMinus = s.startsWith("-");
        s = s.replace("+", "");
        if (s.startsWith("-")) s = s.substring(1);

        BigDecimal val = new BigDecimal(s);
        boolean neg = (forceNegative != null && forceNegative) || parenNeg || trailingMinus || leadingMinus;
        return neg ? val.negate() : val;
    }

    private static String sha256(String s) throws Exception {
        var md = MessageDigest.getInstance("SHA-256");
        return java.util.HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
    }

    private static String firstLine(String s) {
        int i = s.indexOf('\n');
        if (i < 0) return s;
        String line = s.substring(0, i);
        if (line.endsWith("\r")) line = line.substring(0, line.length()-1);
        return line;
    }

    private static char detectSeparator(String header) {
        if (header == null) return ',';
        int commas = count(header, ','), semis = count(header, ';'), tabs = count(header, '\t'), pipes = count(header, '|');
        int max = 0; char sep = ',';
        if (commas > max) { max = commas; sep = ','; }
        if (semis  > max) { max = semis;  sep = ';'; }
        if (tabs   > max) { max = tabs;   sep = '\t'; }
        if (pipes  > max) { max = pipes;  sep = '|'; }
        return max == 0 ? ',' : sep;
    }

    private static int count(String s, char c) {
        int n = 0; for (int i = 0; i < s.length(); i++) if (s.charAt(i) == c) n++; return n;
    }

    private Category ensureIncome() {
        return categoryRepo.findByNameIgnoreCase("Income")
                .orElseGet(() -> categoryRepo.save(
                        Category.builder().name("Income").isIncome(true).group(null).build()
                ));
    }

    private void ensureBaseCategories() {
        ensureIfMissing("Uncategorized", false, CategoryGroup.SURPLUS);
        ensureIfMissing("Income", true, null);
        ensureIfMissing("Groceries", false, CategoryGroup.ESSENTIAL);
        ensureIfMissing("Transport", false, CategoryGroup.ESSENTIAL);
        ensureIfMissing("Fuel", false, CategoryGroup.ESSENTIAL);
        ensureIfMissing("Coffee", false, CategoryGroup.SURPLUS);
        ensureIfMissing("Eating Out", false, CategoryGroup.SURPLUS);
        ensureIfMissing("Shopping", false, CategoryGroup.SURPLUS);
        ensureIfMissing("Mortgage", false, CategoryGroup.DEBT);
        ensureIfMissing("Car Loan", false, CategoryGroup.DEBT);
        ensureIfMissing("Student Loan", false, CategoryGroup.DEBT);
        ensureIfMissing("Credit Card Payment", false, CategoryGroup.DEBT);
    }

    private void ensureIfMissing(String name, Boolean isIncome, CategoryGroup group) {
        categoryRepo.findByNameIgnoreCase(name).orElseGet(() ->
                categoryRepo.save(Category.builder().name(name).isIncome(isIncome).group(group).build())
        );
    }
}
