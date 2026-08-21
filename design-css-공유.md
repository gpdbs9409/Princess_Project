# 프론트엔드 CSS 공유



## 디자인 토큰 (컬러/타이포)

| 변수 | Light | Dark | 용도 |
|---|---|---|---|
| `--bg` | `#faf7f3` | `#17131a` | 배경 |
| `--surface` | `#f3e4e4` | `#221c26` | 카드/표면 |
| `--surface-2` | `#faf7f3` | `#2a222e` | 인풋/보조 표면 |
| `--border` | `#a9a29a` | `#392f3d` | 테두리 |
| `--text` | `#1d1a17` | `#f1e9ec` | 본문 텍스트 |
| `--text-muted` | `#a9a29a` | `#b0a3ab` | 보조 텍스트 |
| `--accent` | `#d48a94` | `#e08bab` | 포인트 컬러 |
| `--accent-strong` | `#aa6e76` | `#f0a8c4` | 강조 포인트 |
| `--good` | `#3e7a72` | `#7fc0b4` | 성공/좋음 |
| `--warn` | `#a5732c` | `#d9ad6a` | 경고 |
| `--danger` | `#b3403f` | `#e5817e` | 위험/에러 |
| `--radius` | `12px` | | 기본 border-radius |

- 폰트: `"Pretendard Variable", "Pretendard", -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif`
- 다크모드: `prefers-color-scheme` 또는 `data-theme="dark"` 속성으로 전환
- 커서: `frontend/public/cursor/cursor.png` 커스텀 커서 사용

## 전체 CSS

```css
:root {
  --bg: #faf7f3;
  --surface: #f3e4e4;
  --surface-2: #faf7f3;
  --border: #a9a29a;
  --text: #1d1a17;
  --text-muted: #a9a29a;
  --accent: #d48a94;
  --accent-strong: #aa6e76;
  --accent-soft: rgba(212, 138, 148, 0.16);
  --good: #3e7a72;
  --good-soft: rgba(62, 122, 114, 0.14);
  --warn: #a5732c;
  --warn-soft: rgba(165, 115, 44, 0.14);
  --danger: #b3403f;
  --danger-soft: rgba(179, 64, 63, 0.12);
  --radius: 12px;
  --shadow: 0 1px 2px rgba(30, 20, 25, 0.06), 0 8px 24px rgba(30, 20, 25, 0.04);
  color-scheme: light dark;
}

@media (prefers-color-scheme: dark) {
  :root {
    --bg: #17131a;
    --surface: #221c26;
    --surface-2: #2a222e;
    --border: #392f3d;
    --text: #f1e9ec;
    --text-muted: #b0a3ab;
    --accent: #e08bab;
    --accent-strong: #f0a8c4;
    --accent-soft: rgba(224, 139, 171, 0.16);
    --good: #7fc0b4;
    --good-soft: rgba(127, 192, 180, 0.16);
    --warn: #d9ad6a;
    --warn-soft: rgba(217, 173, 106, 0.16);
    --danger: #e5817e;
    --danger-soft: rgba(229, 129, 126, 0.16);
    --shadow: 0 1px 2px rgba(0, 0, 0, 0.3), 0 8px 24px rgba(0, 0, 0, 0.24);
  }
}

:root[data-theme="dark"] {
  --bg: #17131a;
  --surface: #221c26;
  --surface-2: #2a222e;
  --border: #392f3d;
  --text: #f1e9ec;
  --text-muted: #b0a3ab;
  --accent: #e08bab;
  --accent-strong: #f0a8c4;
  --accent-soft: rgba(224, 139, 171, 0.16);
  --good: #7fc0b4;
  --good-soft: rgba(127, 192, 180, 0.16);
  --warn: #d9ad6a;
  --warn-soft: rgba(217, 173, 106, 0.16);
  --danger: #e5817e;
  --danger-soft: rgba(229, 129, 126, 0.16);
}
:root[data-theme="light"] {
  --bg: #faf7f3;
  --surface: #f3e4e4;
  --surface-2: #faf7f3;
  --border: #a9a29a;
  --text: #1d1a17;
  --text-muted: #a9a29a;
  --accent: #d48a94;
  --accent-strong: #aa6e76;
  --accent-soft: rgba(212, 138, 148, 0.16);
  --good: #3e7a72;
  --good-soft: rgba(62, 122, 114, 0.14);
  --warn: #a5732c;
  --warn-soft: rgba(165, 115, 44, 0.14);
  --danger: #b3403f;
  --danger-soft: rgba(179, 64, 63, 0.12);
}

* {
  box-sizing: border-box;
}

html,
body,
#root {
  height: 100%;
}

body {
  margin: 0;
  /* the side bands flanking the centered .app-shell column */
  background: var(--surface);
  color: var(--text);
  font-family: "Pretendard Variable", "Pretendard", -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  -webkit-font-smoothing: antialiased;
  /* frontend/public/cursor/cursor.png - falls back to the normal arrow if missing */
  cursor: url("/cursor/cursor.png") 4 4, auto;
}

h1,
h2,
h3 {
  font-weight: 600;
  letter-spacing: -0.01em;
  text-wrap: balance;
  margin: 0;
}

.tabular {
  font-variant-numeric: tabular-nums;
}

.app-shell {
  position: relative;
  z-index: 1;
  min-height: 100%;
  display: flex;
  flex-direction: column;
  width: min(60%, 1100px);
  margin: 0 auto;
  background: var(--bg);
  box-shadow: var(--shadow);
}

@media (max-width: 860px) {
  .app-shell {
    width: 100%;
    box-shadow: none;
  }
}

.container {
  max-width: 720px;
  margin: 0 auto;
  width: 100%;
  padding: 24px 20px 64px;
}

.card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  padding: 20px;
}

.card + .card {
  margin-top: 16px;
}

.stack {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.row-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

label {
  font-size: 13px;
  color: var(--text-muted);
  font-weight: 600;
}

input[type="text"],
input[type="password"],
input[type="number"],
select {
  font: inherit;
  color: var(--text);
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 9px 11px;
  width: 100%;
}

input:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 1px;
}

button {
  font: inherit;
  font-weight: 600;
  border-radius: 8px;
  border: 1px solid transparent;
  padding: 9px 16px;
  cursor: pointer;
  transition: opacity 0.15s ease;
}

button:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

button.primary {
  background: var(--accent);
  color: white;
}
button.primary:hover {
  opacity: 0.9;
}
button.primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

button.ghost {
  background: transparent;
  border-color: var(--border);
  color: var(--text);
}
button.ghost:hover {
  background: var(--surface-2);
}

a.link {
  color: var(--accent);
  text-decoration: none;
  font-weight: 600;
  font-size: 13.5px;
}
a.link:hover {
  text-decoration: underline;
}

.eyebrow {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--accent);
}

.muted {
  color: var(--text-muted);
  font-size: 13.5px;
}

.badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11.5px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 999px;
}
.badge.good {
  background: var(--good-soft);
  color: var(--good);
}
.badge.warn {
  background: var(--warn-soft);
  color: var(--warn);
}

.error-banner {
  background: var(--danger-soft);
  color: var(--danger);
  border: 1px solid var(--danger);
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 13.5px;
}

.topnav {
  border-bottom: 1px solid var(--border);
  background: var(--surface);
}
.topnav-inner {
  max-width: 720px;
  margin: 0 auto;
  padding: 14px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.topnav-brand {
  color: inherit;
  text-decoration: none;
}
.topnav-links {
  display: flex;
  gap: 18px;
  align-items: center;
  flex-wrap: wrap;
  row-gap: 6px;
}
.topnav-links .active {
  color: var(--text);
  text-decoration: underline;
  text-decoration-color: var(--accent);
  text-underline-offset: 4px;
}

/* Enough nav items (오늘 기록/대시보드/나의 아비투스/마이페이지/관리자/로그아웃) that the
   18px desktop gap needs to shrink on narrow screens, or the row overflows/crowds. */
@media (max-width: 640px) {
  .topnav-inner {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
    padding: 12px 16px;
  }
  .topnav-links {
    gap: 12px;
    font-size: 13.5px;
    width: 100%;
  }
}

/* stat meters */
.stat-meter-head {
  display: flex;
  justify-content: space-between;
  font-size: 13.5px;
  margin-bottom: 5px;
}
.stat-meter-track {
  height: 10px;
  border-radius: 5px;
  background: var(--surface-2);
  border: 1px solid var(--border);
  overflow: hidden;
}
.stat-meter-fill {
  height: 100%;
  border-radius: 5px;
  background: var(--accent);
  transition: width 0.3s ease;
}
.stat-meter + .stat-meter {
  margin-top: 12px;
}

/* weekly bar chart */
.week-chart {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  height: 140px;
  padding-top: 8px;
  border-bottom: 1px solid var(--border);
}
.week-bar-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  height: 100%;
  position: relative;
}
.week-bar {
  width: 100%;
  max-width: 32px;
  border-radius: 4px 4px 0 0;
  background: var(--surface-2);
  border: 1px solid var(--border);
  position: relative;
  cursor: pointer;
  transition: background 0.15s ease;
}
.week-bar.has-value {
  background: var(--accent);
  border-color: var(--accent);
}
.week-bar.is-today {
  background: var(--accent-strong);
  border-color: var(--accent-strong);
}
.week-bar-tooltip {
  position: absolute;
  bottom: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%);
  background: var(--text);
  color: var(--bg);
  font-size: 11.5px;
  padding: 4px 8px;
  border-radius: 6px;
  white-space: nowrap;
  pointer-events: none;
  z-index: 5;
}
.week-bar-labels {
  display: flex;
  gap: 10px;
  margin-top: 8px;
}
.week-bar-label {
  flex: 1;
  text-align: center;
  font-size: 11px;
  color: var(--text-muted);
}
.week-bar-label.is-today {
  color: var(--accent);
  font-weight: 700;
}

/* file uploads: the native input is visually hidden and triggered via a labeled button */
.visually-hidden-input {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.file-picker-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--text);
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 9px 16px;
  cursor: pointer;
  transition: background 0.15s ease;
}
.file-picker-button:hover {
  background: var(--accent-soft);
  border-color: var(--accent);
}

.photo-preview {
  display: block;
  width: 100%;
  max-width: 220px;
  max-height: 220px;
  object-fit: cover;
  border-radius: var(--radius);
  border: 1px solid var(--border);
}

/* footer */
.site-footer {
  border-top: 1px solid var(--border);
  background: var(--surface);
  margin-top: auto;
}
.site-footer-inner {
  padding: 20px;
}
.site-footer-copyright {
  font-size: 11.5px;
  color: var(--text-muted);
}

/* floating side widget, docked in the margin band beside the centered .app-shell column */
.side-widget {
  position: fixed;
  top: 110px;
  right: calc((100vw - min(60vw, 1100px)) / 4 - 90px);
  width: 180px;
  display: none;
  flex-direction: column;
  gap: 14px;
  z-index: 3;
}
@media (min-width: 1240px) {
  .side-widget {
    display: flex;
  }
}

.side-widget-photo {
  width: 100%;
  height: 130px;
  object-fit: cover;
  border-radius: var(--radius);
  border: 1px solid var(--border);
  box-shadow: var(--shadow);
}

.side-widget-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.side-widget-goal {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
}

.side-widget-priority-list {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.side-widget-clock {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  padding: 12px 10px;
  font-weight: 700;
  color: var(--text);
}
.side-widget-clock-segs {
  display: flex;
  align-items: center;
  gap: 4px;
}
.side-widget-clock-seg {
  background: var(--surface-2);
  border-radius: 6px;
  padding: 6px 7px;
  font-variant-numeric: tabular-nums;
  font-size: 20px;
}
.side-widget-meridiem {
  font-size: 10.5px;
  letter-spacing: 0.05em;
  color: var(--text-muted);
}

/* Notion-planner mood: full-width bar used to label a page section */
.section-band {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px 16px;
  font-weight: 700;
  font-size: 15px;
  margin-bottom: 10px;
}
.section {
  margin-top: 20px;
}
.section:first-child {
  margin-top: 0;
}

.hub-header-band {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 16px 20px;
  font-style: italic;
  font-weight: 600;
  color: var(--accent-strong);
}

.capital-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}

.capital-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-start;
  justify-content: flex-end;
  aspect-ratio: 3 / 4;
  border-radius: var(--radius);
  padding: 16px;
  text-align: left;
  cursor: pointer;
  font: inherit;
  background-size: cover;
  background-position: center top;
  transition: transform 0.15s ease, opacity 0.15s ease;
}
.capital-card:hover {
  transform: translateY(-2px);
}
.capital-card-label {
  font-weight: 700;
  font-size: 14.5px;
}
.capital-card-sub {
  font-size: 11.5px;
}

.capital-card.is-selected {
  background-color: var(--accent);
  border: 1px solid var(--accent-strong);
  color: #fff;
  box-shadow: var(--shadow);
}
.capital-card.is-selected .capital-card-sub {
  color: rgba(255, 255, 255, 0.85);
}

.capital-card.is-unselected {
  background-color: var(--surface-2);
  border: 1px dashed var(--border);
  color: var(--text-muted);
}
.capital-card.is-unselected .capital-card-label {
  color: var(--text);
}

/* record page: each mission is its own card, clearly split between
   "awaiting input" (default .card look) and "already recorded" (below) */
.recorded-mission-card {
  background: var(--surface-2);
  border: 1px solid var(--good);
}
.recorded-field {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 13.5px;
}
.recorded-field .muted {
  font-size: 11.5px;
}

/* corner mascot that trails the cursor across every screen */
.mascot-follower {
  position: fixed;
  top: 0;
  left: 0;
  width: 64px;
  height: 64px;
  object-fit: contain;
  pointer-events: none;
  z-index: 50;
  filter: drop-shadow(0 4px 10px rgba(0, 0, 0, 0.25));
}
@media (max-width: 720px) {
  .mascot-follower {
    display: none;
  }
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(29, 26, 23, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  padding: 20px;
}
.modal-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  padding: 28px 24px;
  max-width: 340px;
  width: 100%;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* Instagram-style profile header */
.profile-header {
  margin-bottom: 20px;
}
.profile-header-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}
.profile-header-title {
  font-weight: 700;
  font-size: 16px;
}
.role-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
  color: var(--accent);
  background: var(--surface);
  border: 1px solid var(--accent);
}

.profile-header-topbar-spacer {
  width: 26px;
}
.profile-header-icon-btn {
  background: none;
  border: none;
  font-size: 18px;
  line-height: 1;
  color: var(--text);
  cursor: pointer;
  padding: 4px;
}

.profile-header-main {
  display: flex;
  align-items: center;
  gap: 20px;
}

.profile-header-avatar-ring {
  flex-shrink: 0;
  width: 76px;
  height: 76px;
  border-radius: 50%;
  padding: 3px;
  background: conic-gradient(from 0deg, var(--accent), #f6c1cf, var(--accent-strong), var(--accent-soft), var(--accent));
  animation: profile-ring-spin 5s linear infinite;
}
@keyframes profile-ring-spin {
  to {
    transform: rotate(360deg);
  }
}
.profile-header-avatar {
  display: block;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 3px solid var(--bg);
  object-fit: cover;
  background: var(--surface);
}
.profile-header-avatar-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  font-weight: 700;
  color: var(--accent-strong);
}

.profile-header-stats {
  flex: 1;
  display: flex;
  justify-content: space-around;
  text-align: center;
}
.profile-header-stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.profile-header-stat strong {
  font-size: 17px;
}
.profile-header-stat .muted {
  font-size: 12px;
}

.profile-header-nickname {
  margin-top: 12px;
  font-weight: 700;
  font-size: 14px;
}

/* save-confirmation toasts */
.toast-stack {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  gap: 8px;
  z-index: 200;
  align-items: center;
}
.toast {
  background: var(--good);
  color: #fff;
  font-weight: 600;
  font-size: 13.5px;
  padding: 10px 18px;
  border-radius: 999px;
  box-shadow: var(--shadow);
  animation: toast-in 0.2s ease;
}
@keyframes toast-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ambient rising-bubble effect */
.bubble-layer {
  position: fixed;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
}
.bubble {
  position: absolute;
  bottom: -60px;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.65), var(--accent-soft) 60%, transparent 80%);
  border: 1px solid rgba(255, 255, 255, 0.3);
  animation-name: bubble-rise;
  animation-timing-function: linear;
  animation-iteration-count: infinite;
}
@keyframes bubble-rise {
  0% {
    transform: translateY(0) translateX(0);
    opacity: 0;
  }
  10% {
    opacity: 0.7;
  }
  85% {
    opacity: 0.5;
  }
  100% {
    transform: translateY(-115vh) translateX(var(--drift, 20px));
    opacity: 0;
  }
}

/* live camera capture modal (MissionCard) */
.camera-modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  z-index: 100;
}
.camera-modal {
  background: var(--surface);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  padding: 18px;
  max-width: 420px;
  width: 100%;
}
.camera-video {
  width: 100%;
  border-radius: 12px;
  background: #000;
  aspect-ratio: 3 / 4;
  object-fit: cover;
}

/* butler AI feedback - chat speech bubbles */
.butler-feedback {
  margin-top: 12px;
}
.butler-feedback-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.butler-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--surface);
  box-shadow: var(--shadow);
  flex-shrink: 0;
}
.butler-name-block {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}
.butler-name-eyebrow {
  font-size: 11px;
  font-weight: 700;
  color: var(--text-muted);
}
.butler-name {
  font-weight: 700;
  font-size: 15px;
}
.butler-bubbles {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.butler-bubble-row {
  display: flex;
  align-items: flex-end;
  gap: 6px;
}
.butler-bubble {
  background: var(--surface-2);
  border-radius: 16px 16px 16px 4px;
  padding: 10px 14px;
  font-size: 13.5px;
  line-height: 1.5;
  max-width: 82%;
  animation: butler-bubble-in 0.25s ease both;
}
.butler-bubble-read {
  font-size: 10.5px;
  color: var(--text-muted);
  flex-shrink: 0;
  margin-bottom: 2px;
}
@keyframes butler-bubble-in {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* admin member table */
.admin-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13.5px;
}
.admin-table th,
.admin-table td {
  text-align: left;
  padding: 10px 8px;
  border-bottom: 1px solid var(--border);
}
.admin-table th {
  color: var(--text-muted);
  font-weight: 700;
  font-size: 12px;
}
```
