// spendwise-frontend/src/components/UploadArea.jsx
import { useMemo, useRef, useState } from "react";
import { previewImport, commitImport } from "../api";

export default function UploadArea({ month, onCommitted }) {
  const [file, setFile] = useState(null);
  const [fileKey, setFileKey] = useState(0); // force-remount input
  const fileInputRef = useRef(null); // direct reset

  const [preview, setPreview] = useState(null);
  const [overrides, setOverrides] = useState({});
  const [excluded, setExcluded] = useState({});
  const [statementType, setStatementType] = useState("debit"); // 'debit' | 'credit'
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  const rows = preview?.rows || [];
  const eligibleCount = useMemo(
    () => rows.filter((r) => r.inTargetMonth && !excluded[r.hash]).length,
    [rows, excluded]
  );

  function setOverride(hash, value) {
    setOverrides((p) => ({ ...p, [hash]: value }));
  }
  function toggleExclude(hash) {
    setExcluded((p) => ({ ...p, [hash]: !p[hash] }));
  }

  function resetFileInput() {
    setFile(null);
    // clear the native input’s value and force a remount so selecting the same file re-triggers onChange
    if (fileInputRef.current) fileInputRef.current.value = "";
    setFileKey((k) => k + 1);
  }

  function resetState() {
    setPreview(null);
    setOverrides({});
    setExcluded({});
    setMessage("");
    resetFileInput();
  }

  async function doPreview() {
    if (!file || !month) {
      setMessage("Select a month and choose a CSV.");
      return;
    }
    setMessage("");
    setLoading(true);
    try {
      const data = await previewImport(file, month, statementType);
      setPreview(data);
      setOverrides({});
      // default: auto-exclude out-of-month rows
      const ex = {};
      for (const r of data.rows) if (!r.inTargetMonth) ex[r.hash] = true;
      setExcluded(ex);
    } catch (e) {
      setMessage(e.message || "Preview failed");
    } finally {
      setLoading(false);
    }
  }

  async function doCommit() {
    if (!file || !month) return;
    setLoading(true);
    setMessage("");
    try {
      const excludeHashes = Object.entries(excluded)
        .filter(([, v]) => v)
        .map(([h]) => h);
      const res = await commitImport(
        file,
        month,
        overrides,
        statementType,
        excludeHashes
      );
      setMessage(`Inserted ${res.inserted}, skipped ${res.skipped}.`);
      // fully reset so you can upload the same or another file immediately
      resetState();
      onCommitted?.(month);
    } catch (e) {
      setMessage(e.message || "Commit failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="card">
      <div className="flex-spread">
        <h2>
          {" "}
          <div className="h2">Upload Statement</div>
        </h2>
        <div className="small">CSV only</div>
      </div>

      {/* Statement type */}
      <div className="row" style={{ marginTop: 8 }}>
        <h3>
          <label>Statement:</label>
        </h3>
        <label>
          <h3>
            {" "}
            <input
              type="radio"
              name="st"
              value="debit"
              checked={statementType === "debit"}
              onChange={() => setStatementType("debit")}
            />
            &nbsp;Debit card
          </h3>
        </label>
        <label>
          <h3>
            {" "}
            <input
              type="radio"
              name="st"
              value="credit"
              checked={statementType === "credit"}
              onChange={() => setStatementType("credit")}
            />
            &nbsp;Credit card
          </h3>
        </label>
      </div>

      <div className="row" style={{ marginTop: 8 }}>
        <input
          key={fileKey} // <- remounts after reset/commit
          ref={fileInputRef}
          type="file"
          accept=".csv"
          // ensure picking the same file twice still triggers onChange
          onClick={(e) => {
            e.currentTarget.value = null;
          }}
          onChange={(e) => setFile(e.target.files?.[0] || null)}
        />
        <button onClick={doPreview} disabled={!file || !month || loading}>
          Preview
        </button>
        <button className="secondary" onClick={resetState} disabled={loading}>
          Reset
        </button>
      </div>

      {message && (
        <div style={{ marginTop: 8 }} className="small">
          {message}
        </div>
      )}

      {rows.length > 0 && (
        <>
          <div className="hr" />
          <div className="flex-spread">
            <div className="h2">Preview</div>
            <div className="small">
              {rows.length} rows · Eligible to insert: {eligibleCount}
            </div>
          </div>
          <div style={{ overflowX: "auto" }}>
            <table className="table">
              <thead>
                <tr>
                  <th>Include</th>
                  <th>Date</th>
                  <th>Description</th>
                  <th>Amount</th>
                  <th>Suggested</th>
                  <th>Group</th>
                  <th>In Month</th>
                  <th>Override Category</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((r) => {
                  const inc = !(excluded[r.hash] ?? false);
                  return (
                    <tr key={r.hash}>
                      <td>
                        <input
                          type="checkbox"
                          checked={inc}
                          onChange={() => toggleExclude(r.hash)}
                        />
                      </td>
                      <td>{r.date}</td>
                      <td>{r.description}</td>
                      <td>{r.amount}</td>
                      <td>{r.suggestedCategory}</td>
                      <td>{r.categoryGroup ?? "-"}</td>
                      <td>
                        {r.inTargetMonth ? (
                          <span className="badge ok">yes</span>
                        ) : (
                          <span className="badge warn">no</span>
                        )}
                      </td>
                      <td>
                        <input
                          type="text"
                          placeholder="(optional)"
                          value={overrides[r.hash] ?? ""}
                          onChange={(e) => setOverride(r.hash, e.target.value)}
                          style={{ width: 160 }}
                        />
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          <div className="row" style={{ marginTop: 12 }}>
            <button
              onClick={doCommit}
              disabled={eligibleCount === 0 || loading}
            >
              Commit {eligibleCount} rows
            </button>
          </div>
        </>
      )}
    </div>
  );
}
