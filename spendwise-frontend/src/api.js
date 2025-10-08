const BASE = import.meta.env.VITE_API_BASE || "";

export async function listTransactions(month) {
  const base = import.meta.env.VITE_API_BASE || "";
  const r = await fetch(
    `${base}/api/transactions?month=${encodeURIComponent(month)}`
  );
  if (!r.ok) throw new Error("Failed to fetch transactions");
  return r.json(); // { month, count, income, expenses, net, transactions:[...] }
}

export async function listMonths() {
  const r = await fetch(`${BASE}/api/months`);
  if (!r.ok) throw new Error("Failed to fetch months");
  return r.json();
}

export async function listCategories() {
  const r = await fetch(`${BASE}/api/categories`);
  if (!r.ok) throw new Error("Failed to fetch categories");
  return r.json(); // { categories: [{id,name,group,isIncome}] }
}

export async function createCategory({ name, group, isIncome }) {
  const r = await fetch(`${BASE}/api/categories`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name, group, isIncome }),
  });
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

export async function previewImport(file, month, statementType) {
  const fd = new FormData();
  fd.append("file", file);
  const url = new URL(`${BASE}/api/import/csv`);
  url.searchParams.set("dryRun", "true");
  if (month) url.searchParams.set("month", month);
  if (statementType) url.searchParams.set("statementType", statementType);
  const r = await fetch(url, { method: "POST", body: fd });
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

export async function commitImport(
  file,
  month,
  overrides,
  statementType,
  excludeHashes,
  descOverrides,
  groupOverrides
) {
  const fd = new FormData();
  fd.append("file", file);
  if (overrides && Object.keys(overrides).length) {
    fd.append("overrides", JSON.stringify(overrides)); // hash -> categoryName
  }
  if (excludeHashes && excludeHashes.length) {
    fd.append("exclude", JSON.stringify(excludeHashes)); // [hash]
  }
  if (descOverrides && Object.keys(descOverrides).length) {
    fd.append("descOverrides", JSON.stringify(descOverrides)); // hash -> description
  }
  if (groupOverrides && Object.keys(groupOverrides).length) {
    fd.append("groupOverrides", JSON.stringify(groupOverrides)); // hash -> ESSENTIAL|SURPLUS|DEBT
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
