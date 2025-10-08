import { useEffect, useMemo, useState } from "react";
import { listTransactions } from "../api";

function Amount({ value }) {
  const n = typeof value === "number" ? value : Number(value);
  const neg = n < 0;
  return (
    <span className={neg ? "amount-neg" : "amount-pos"}>
      {neg ? "-" : ""}$
      {Math.abs(n).toLocaleString(undefined, {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      })}
    </span>
  );
}

export default function TransactionsForMonth({ month }) {
  const [data, setData] = useState(null); // { transactions, income, expenses, net, ... }
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [q, setQ] = useState(""); // quick search
  const [sort, setSort] = useState({ key: "date", dir: "asc" });

  useEffect(() => {
    if (!month) return;
    setLoading(true);
    setError("");
    listTransactions(month)
      .then(setData)
      .catch((e) => setError(e.message || "Failed to load"))
      .finally(() => setLoading(false));
  }, [month]);

  const rows = data?.transactions || [];

  const filtered = useMemo(() => {
    const term = q.trim().toLowerCase();
    let out = rows;
    if (term) {
      out = rows.filter(
        (r) =>
          (r.description || "").toLowerCase().includes(term) ||
          (r.category || "").toLowerCase().includes(term) ||
          (r.group || "").toLowerCase().includes(term)
      );
    }
    const s = { ...sort };
    out = [...out].sort((a, b) => {
      let va = a[s.key],
        vb = b[s.key];
      if (s.key === "amount") {
        va = Number(va);
        vb = Number(vb);
      }
      if (s.key === "date") {
        va = a.date;
        vb = b.date;
      } // ISO yyyy-MM-dd sorts lexicographically
      if (va == null && vb == null) return 0;
      if (va == null) return 1;
      if (vb == null) return -1;
      const cmp = va < vb ? -1 : va > vb ? 1 : 0;
      return s.dir === "asc" ? cmp : -cmp;
    });
    return out;
  }, [rows, q, sort]);

  function toggleSort(key) {
    setSort((s) =>
      s.key === key
        ? { key, dir: s.dir === "asc" ? "desc" : "asc" }
        : { key, dir: "asc" }
    );
  }

  return (
    <div className="card" style={{ marginTop: 16 }}>
      <div className="flex-spread">
        <div className="h2">Transactions — {month}</div>
        <div className="row" style={{ gap: 8 }}>
          <input
            placeholder="Search description/category/group..."
            value={q}
            onChange={(e) => setQ(e.target.value)}
            style={{ minWidth: 280 }}
          />
        </div>
      </div>

      {loading && (
        <div className="small" style={{ marginTop: 8 }}>
          Loading…
        </div>
      )}
      {error && (
        <div className="small" style={{ marginTop: 8, color: "#f87171" }}>
          {error}
        </div>
      )}

      {!loading && !error && (
        <>
          <div className="small" style={{ margin: "8px 0" }}>
            {filtered.length} of {rows.length} rows · Income:{" "}
            <strong>
              <Amount value={data?.income || 0} />
            </strong>{" "}
            · Expenses:{" "}
            <strong>
              <Amount value={-(data?.expenses || 0) * -1} />
            </strong>{" "}
            · Net:{" "}
            <strong>
              <Amount value={data?.net || 0} />
            </strong>
          </div>

          <div style={{ overflowX: "auto" }}>
            <table className="table">
              <thead>
                <tr>
                  <th
                    onClick={() => toggleSort("date")}
                    style={{ cursor: "pointer" }}
                  >
                    Date{" "}
                    {sort.key === "date"
                      ? sort.dir === "asc"
                        ? "▲"
                        : "▼"
                      : ""}
                  </th>
                  <th
                    onClick={() => toggleSort("description")}
                    style={{ cursor: "pointer" }}
                  >
                    Description{" "}
                    {sort.key === "description"
                      ? sort.dir === "asc"
                        ? "▲"
                        : "▼"
                      : ""}
                  </th>
                  <th
                    onClick={() => toggleSort("amount")}
                    style={{ cursor: "pointer" }}
                  >
                    Amount{" "}
                    {sort.key === "amount"
                      ? sort.dir === "asc"
                        ? "▲"
                        : "▼"
                      : ""}
                  </th>
                  <th
                    onClick={() => toggleSort("category")}
                    style={{ cursor: "pointer" }}
                  >
                    Category{" "}
                    {sort.key === "category"
                      ? sort.dir === "asc"
                        ? "▲"
                        : "▼"
                      : ""}
                  </th>
                  <th
                    onClick={() => toggleSort("group")}
                    style={{ cursor: "pointer" }}
                  >
                    Group{" "}
                    {sort.key === "group"
                      ? sort.dir === "asc"
                        ? "▲"
                        : "▼"
                      : ""}
                  </th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((tx) => (
                  <tr key={tx.id ?? tx.hash}>
                    <td>{tx.date}</td>
                    <td>{tx.description}</td>
                    <td>
                      <Amount value={tx.amount} />
                    </td>
                    <td>{tx.category ?? "-"}</td>
                    <td>{tx.group ?? "-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}
