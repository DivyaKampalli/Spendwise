import { useEffect, useMemo, useRef, useState } from "react";
import { listMonths } from "../api";

const MONTHS = [
  "January",
  "February",
  "March",
  "April",
  "May",
  "June",
  "July",
  "August",
  "September",
  "October",
  "November",
  "December",
];

function ymString(y, mIndex) {
  const mm = String(mIndex + 1).padStart(2, "0");
  return `${y}-${mm}`;
}
function labelFrom(ym) {
  const y = Number(ym.slice(0, 4));
  const m = Number(ym.slice(5, 7)) - 1;
  return `${MONTHS[m]} ${y}`;
}

export default function MonthPicker({ value, onChange }) {
  // Determine initial YYYY-MM (current month if parent didn't pass one)
  const initial = useMemo(() => {
    if (value && /^\d{4}-\d{2}$/.test(value)) return value;
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
  }, [value]);

  // 🔑 NEW: push the initial value up to the parent once, so month is never empty
  useEffect(() => {
    if (!value) onChange?.(initial);
    // run once on mount; do not re-run when initial/value change
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const [selected, setSelected] = useState(initial);
  const [year, setYear] = useState(Number(initial.slice(0, 4)));
  const [open, setOpen] = useState(false);
  const [existingMonths, setExistingMonths] = useState([]);
  const popRef = useRef(null);

  useEffect(() => {
    listMonths()
      .then((d) => setExistingMonths(d.months || []))
      .catch(() => setExistingMonths([]));
  }, []);

  useEffect(() => {
    if (value && value !== selected) {
      setSelected(value);
      setYear(Number(value.slice(0, 4)));
    }
  }, [value]);

  useEffect(() => {
    function onDocClick(e) {
      if (!open) return;
      if (popRef.current && !popRef.current.contains(e.target)) setOpen(false);
    }
    document.addEventListener("mousedown", onDocClick);
    return () => document.removeEventListener("mousedown", onDocClick);
  }, [open]);

  const hasData = new Set(existingMonths);

  function chooseYM(y, mIndex) {
    const v = ymString(y, mIndex);
    setSelected(v);
    setYear(y);
    setOpen(false);
    onChange?.(v);
  }

  const selLabel = labelFrom(selected);

  return (
    <div
      className="monthpicker card"
      style={{ position: "relative", overflow: "visible" }}
    >
      <div className="h2">Month</div>

      <button
        type="button"
        className="month-trigger"
        aria-haspopup="dialog"
        aria-expanded={open}
        onClick={() => setOpen((o) => !o)}
        title="Pick month"
      >
        {selLabel}
        <span className="chev">▾</span>
      </button>

      {open && (
        <div
          className="popover"
          ref={popRef}
          role="dialog"
          aria-label="Choose month"
        >
          <div className="pop-header">
            <button
              type="button"
              className="secondary"
              onClick={() => setYear((y) => y - 1)}
            >
              ◀
            </button>
            <div className="year">{year}</div>
            <button
              type="button"
              className="secondary"
              onClick={() => setYear((y) => y + 1)}
            >
              ▶
            </button>
          </div>

          <div className="month-grid">
            {MONTHS.map((label, idx) => {
              const ym = ymString(year, idx);
              const active = ym === selected;
              const hint = hasData.has(ym);
              return (
                <button
                  key={label}
                  type="button"
                  className={`month-btn ${active ? "active" : ""}`}
                  onClick={() => chooseYM(year, idx)}
                  title={ym + (hint ? " (has data)" : "")}
                >
                  <span>{label.slice(0, 3)}</span>
                  {hint && <span className="dot" />}
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
