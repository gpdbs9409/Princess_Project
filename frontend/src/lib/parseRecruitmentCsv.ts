import type { RecruitmentApplicantRequest, RecruitmentStatus } from "../api/types";

/**
 * Minimal RFC4180 CSV reader, written by hand rather than pulled in as a dependency: the
 * only maintained SheetJS build is behind a CDN we can't reach, and the copy left on npm
 * has open prototype-pollution advisories. Excel's "Save as CSV" round-trips through this
 * fine, which is all the recruitment import needs.
 */
function splitRows(text: string): string[][] {
  const rows: string[][] = [];
  let row: string[] = [];
  let field = "";
  let inQuotes = false;

  for (let i = 0; i < text.length; i++) {
    const char = text[i];

    if (inQuotes) {
      if (char === '"') {
        if (text[i + 1] === '"') {
          field += '"';
          i++;
        } else {
          inQuotes = false;
        }
      } else {
        field += char;
      }
      continue;
    }

    if (char === '"') {
      inQuotes = true;
    } else if (char === ",") {
      row.push(field);
      field = "";
    } else if (char === "\n" || char === "\r") {
      // swallow the \n of a \r\n pair so the row isn't closed twice
      if (char === "\r" && text[i + 1] === "\n") i++;
      row.push(field);
      rows.push(row);
      row = [];
      field = "";
    } else {
      field += char;
    }
  }

  if (field.length > 0 || row.length > 0) {
    row.push(field);
    rows.push(row);
  }
  return rows;
}

const STATUS_BY_LABEL: Record<string, RecruitmentStatus> = {
  검토중: "PENDING",
  대기: "PENDING",
  합격: "ACCEPTED",
  불합격: "REJECTED",
  PENDING: "PENDING",
  ACCEPTED: "ACCEPTED",
  REJECTED: "REJECTED",
};

/** Accepts 2026-08-08, 2026/8/8 and Excel's 2026. 8. 8. */
function parseDate(raw: string): string | undefined {
  const trimmed = raw.trim();
  if (!trimmed) return undefined;
  const parts = trimmed.split(/[.\-/]/).map((p) => p.trim()).filter(Boolean);
  if (parts.length < 3) return undefined;
  const [y, m, d] = parts;
  if (!/^\d{4}$/.test(y)) return undefined;
  return `${y}-${m.padStart(2, "0")}-${d.padStart(2, "0")}`;
}

export interface CsvParseResult {
  rows: RecruitmentApplicantRequest[];
  skipped: number;
}

export function parseRecruitmentCsv(text: string): CsvParseResult {
  // Excel writes a UTF-8 BOM; left in place it becomes part of the first header cell.
  const clean = text.replace(/^﻿/, "");
  const table = splitRows(clean).filter((r) => r.some((cell) => cell.trim() !== ""));
  if (table.length === 0) return { rows: [], skipped: 0 };

  // Skip a header row if the first cell looks like a label rather than a person's name.
  const first = table[0][0]?.trim() ?? "";
  const body = /^(이름|성함|name)$/i.test(first) ? table.slice(1) : table;

  const rows: RecruitmentApplicantRequest[] = [];
  let skipped = 0;

  for (const cells of body) {
    const name = (cells[0] ?? "").trim();
    if (!name) {
      skipped++;
      continue;
    }
    rows.push({
      name,
      contact: (cells[1] ?? "").trim() || undefined,
      note: (cells[2] ?? "").trim() || undefined,
      status: STATUS_BY_LABEL[(cells[3] ?? "").trim()] ?? "PENDING",
      appliedAt: parseDate(cells[4] ?? ""),
    });
  }

  return { rows, skipped };
}
