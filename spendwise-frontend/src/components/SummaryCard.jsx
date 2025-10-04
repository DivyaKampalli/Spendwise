// spendwise-frontend/src/components/SummaryCard.jsx
import { useEffect, useState } from "react";
import { summaryByGroup } from "../api";

function money(n) {
  if (n == null) return "-";
  return Number(n).toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

export default function SummaryCard({ month, refreshKey }) {
  const [data, setData] = useState(null);
  const [err, setErr] = useState("");

  useEffect(() => {
    if (!month) return;
    setErr("");
    summaryByGroup(month)
      .then(setData)
      .catch((e) => setErr(e.message || "Failed"));
  }, [month, refreshKey]);

  return (
    <div className="card">
      <div className="flex-spread">
        <div className="h2">Monthly Summary</div>
        <div className="small">{month || "-"}</div>
      </div>
      {err && <div className="small">{err}</div>}
      {data && (
        <>
          <div className="row" style={{ marginTop: 8, flexWrap: "wrap" }}>
            <div className="card" style={{ flex: "1 1 180px" }}>
              <div className="small">Income</div>
              <div className="h2">${money(data.income)}</div>
            </div>
            <div className="card" style={{ flex: "1 1 180px" }}>
              <div className="small">Essentials</div>
              <div className="h2">${money(data.byGroup.ESSENTIAL)}</div>
            </div>
            <div className="card" style={{ flex: "1 1 180px" }}>
              <div className="small">Surplus</div>
              <div className="h2">${money(data.byGroup.SURPLUS)}</div>
            </div>
            <div className="card" style={{ flex: "1 1 180px" }}>
              <div className="small">Debt</div>
              <div className="h2">${money(data.byGroup.DEBT)}</div>
            </div>
            <div className="card" style={{ flex: "1 1 180px" }}>
              <div className="small">Net</div>
              <div className="h2">${money(data.net)}</div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
