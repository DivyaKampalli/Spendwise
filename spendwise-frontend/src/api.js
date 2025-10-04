const BASE = import.meta.env.VITE_API_BASE || "";

export async function listMonths() {
  const r = await fetch(`${BASE}/api/months`);
  if (!r.ok) throw new Error("Failed to fetch months");
  return r.json(); // { months: ["YYYY-MM", ...] }
}

export async function previewImport(file, month, statementType) {
  const fd = new FormData();
  fd.append("file", file);

  const url = new URL(`${BASE}/api/import/csv`);
  url.searchParams.set("dryRun", "true");
  if (month) url.searchParams.set("month", month);
  if (statementType) url.searchParams.set("statementType", statementType); // "debit" | "credit"

  const r = await fetch(url, { method: "POST", body: fd });
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

export async function commitImport(
  file,
  month,
  overrides,
  statementType,
  excludeHashes
) {
  const fd = new FormData();
  fd.append("file", file);
  if (overrides && Object.keys(overrides).length) {
    fd.append("overrides", JSON.stringify(overrides));
  }
  if (excludeHashes && excludeHashes.length) {
    fd.append("exclude", JSON.stringify(excludeHashes));
  }

  const url = new URL(`${BASE}/api/import/csv`);
  url.searchParams.set("dryRun", "false");
  if (month) url.searchParams.set("month", month);
  if (statementType) url.searchParams.set("statementType", statementType);

  const r = await fetch(url, { method: "POST", body: fd });
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

export async function summaryByGroup(month) {
  const r = await fetch(
    `${BASE}/api/summary/monthly/by-group?month=${encodeURIComponent(month)}`
  );
  if (!r.ok) throw new Error("Failed to fetch summary");
  return r.json();
}
