// spendwise-frontend/src/App.jsx
import { useState } from "react";
import MonthPicker from "./components/MonthPicker.jsx";
import UploadArea from "./components/UploadArea.jsx";
import SummaryCard from "./components/SummaryCard.jsx";
import TransactionsForMonth from "./components/TransactionsForMonth";

export default function App() {
  const [month, setMonth] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);

  return (
    <div className="container">
      <h1 className="h1">Spendwise</h1>

      <div className="card" style={{ marginBottom: 16 }}>
        <div className="row">
          <MonthPicker value={month} onChange={setMonth} />
        </div>
      </div>
      <SummaryCard month={month} />

      <UploadArea
        month={month}
        onCommitted={() => setRefreshKey((k) => k + 1)}
      />
      <TransactionsForMonth month={month} />

      <div style={{ height: 16 }} />
      <SummaryCard month={month} refreshKey={refreshKey} />
    </div>
  );
}
