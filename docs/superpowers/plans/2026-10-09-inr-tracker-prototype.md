# INR 记录 App 可点击原型 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用纯静态 HTML/CSS/JS 做出一个可在浏览器点击体验的「凝稳」App 高保真原型，覆盖首页、记录、趋势、设置与导出。

**Architecture:** 单个静态站点，无构建步骤、无第三方依赖。`index.html` 作为外壳（手机边框 + 3 个 Tab 页 + 底部导航 + FAB + 记录弹层 + 导出弹窗）；`styles.css` 提供设计令牌与组件样式；`mock-data.js` 提供假数据与默认设置；`app.js` 负责状态、渲染与交互（趋势折线图用内联 SVG 手绘）。

**Tech Stack:** HTML5 + CSS3（Flexbox/Grid、CSS 变量）+ 原生 JavaScript（ES modules，无框架）。图表用 SVG `<polyline>` / `<rect>`。

**说明（与流程的适配）:**
- 本计划遵循 brainstorming 产出的设计方案：`docs/superpowers/specs/2026-10-09-inr-tracker-app-design.md`。
- 原型为纯 UI，验证方式为「浏览器目视 + 交互检查」，不使用单元测试框架（YAGNI）。
- 计划中包含 git 提交步骤；若你不想用 git，可跳过所有 `git` 步骤，不影响原型运行。
- 默认值：App 名「凝稳」，目标范围 2.0–3.0，下次测量间隔 7 天（一周）（如与你确认不同，全局替换即可）。

---

## 文件结构

```
inr-tracker/
├── prototype/
│   ├── index.html      # App 外壳：手机框、3 个 Tab 页、底部导航、FAB、记录弹层、导出弹窗
│   ├── styles.css      # 设计令牌 + 全部组件样式
│   ├── mock-data.js    # 假数据（记录列表）与默认设置
│   └── app.js          # 状态、渲染、交互、SVG 折线图
└── docs/superpowers/plans/2026-10-09-inr-tracker-prototype.md
```

| 文件 | 单一职责 |
|---|---|
| `index.html` | 静态结构骨架，不写业务逻辑 |
| `styles.css` | 视觉呈现（颜色/字体/间距/状态色/手机框） |
| `mock-data.js` | 只提供数据常量，无逻辑 |
| `app.js` | 所有动态行为：渲染、事件、校验、图表 |

---

## Task 1: 项目脚手架与 Tab 导航

**Files:**
- Create: `prototype/index.html`
- Create: `prototype/styles.css`
- Create: `prototype/app.js`

- [ ] **Step 1: 初始化 git 仓库（可选）**

Run:
```bash
cd /Users/sunjian/Downloads/inr-tracker && git init && printf "node_modules/\n.DS_Store\n" > .gitignore
```
Expected: `Initialized empty Git repository ...`

- [ ] **Step 2: 写 `prototype/index.html` 骨架**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>凝稳 · INR 记录</title>
  <link rel="stylesheet" href="styles.css" />
</head>
<body>
  <div class="phone">
    <header class="app-bar">
      <span class="app-bar__title">凝稳</span>
    </header>

    <main id="view-home" class="view is-active"></main>
    <main id="view-trend" class="view"></main>
    <main id="view-settings" class="view"></main>

    <nav class="tabbar">
      <button class="tab is-active" data-tab="home">首页</button>
      <button class="tab" data-tab="trend">趋势</button>
      <button class="tab" data-tab="settings">我的</button>
    </nav>

    <button id="fab" class="fab" aria-label="记录">+</button>

    <div id="sheet" class="sheet" hidden></div>
    <div id="modal" class="modal" hidden></div>
    <div id="toast" class="toast" hidden></div>
  </div>

  <script type="module" src="app.js"></script>
</body>
</html>
```

- [ ] **Step 3: 写 `prototype/styles.css` 基础外壳样式**

```css
:root {
  --bg: #f4f6f8;
  --card: #ffffff;
  --text: #1f2933;
  --text-sub: #6b7280;
  --line: #e5e7eb;
  --brand: #2f6fed;
  --ok: #16a34a;
  --ok-bg: #e7f6ec;
  --high: #dc2626;
  --high-bg: #fdeaea;
  --low: #2563eb;
  --low-bg: #e8f0fe;
  --radius: 16px;
  --shadow: 0 6px 20px rgba(15, 23, 42, .08);
}

* { box-sizing: border-box; }
html, body { margin: 0; height: 100%; }
body {
  background: #dfe3e8;
  font-family: -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif;
  color: var(--text);
  display: flex; justify-content: center; align-items: center;
  min-height: 100vh;
}

.phone {
  position: relative;
  width: 390px; height: 844px;
  background: var(--bg);
  border-radius: 36px;
  overflow: hidden;
  box-shadow: 0 24px 60px rgba(0,0,0,.25);
  display: flex; flex-direction: column;
}

.app-bar {
  height: 56px; display: flex; align-items: center; justify-content: center;
  background: var(--card); border-bottom: 1px solid var(--line);
  font-weight: 600; font-size: 17px;
  padding-top: 8px;
}

.view { flex: 1; overflow-y: auto; padding: 16px 16px 96px; display: none; }
.view.is-active { display: block; }

.tabbar {
  height: 60px; display: flex; background: var(--card);
  border-top: 1px solid var(--line);
}
.tab {
  flex: 1; border: 0; background: none; font-size: 13px;
  color: var(--text-sub); cursor: pointer;
}
.tab.is-active { color: var(--brand); font-weight: 600; }

.fab {
  position: absolute; right: 20px; bottom: 78px;
  width: 56px; height: 56px; border-radius: 50%;
  border: 0; background: var(--brand); color: #fff;
  font-size: 28px; line-height: 1; cursor: pointer;
  box-shadow: 0 8px 20px rgba(47,111,237,.4);
}

.sheet, .modal {
  position: absolute; inset: 0; background: rgba(15,23,42,.4);
  display: flex; align-items: flex-end; z-index: 20;
}
/* 必须显式声明：类选择器的 display:flex 会覆盖 UA 对 [hidden] 的 display:none，
   否则弹层/弹窗会在页面加载时直接显示 */
.sheet[hidden], .modal[hidden] { display: none; }
.modal { align-items: center; justify-content: center; }
.toast {
  position: absolute; left: 50%; bottom: 140px; transform: translateX(-50%);
  background: rgba(31,41,51,.92); color: #fff; padding: 10px 16px;
  border-radius: 10px; font-size: 14px; z-index: 40;
}
```

- [ ] **Step 4: 写 `prototype/app.js` 占位与 Tab 切换**

```js
function initTabs() {
  const tabs = document.querySelectorAll('.tab');
  const views = {
    home: document.getElementById('view-home'),
    trend: document.getElementById('view-trend'),
    settings: document.getElementById('view-settings'),
  };
  tabs.forEach((btn) => {
    btn.addEventListener('click', () => {
      tabs.forEach((b) => b.classList.toggle('is-active', b === btn));
      Object.entries(views).forEach(([name, el]) =>
        el.classList.toggle('is-active', name === btn.dataset.tab));
    });
  });
}

initTabs();
```

- [ ] **Step 5: 浏览器验证**

Run:
```bash
cd /Users/sunjian/Downloads/inr-tracker/prototype && python3 -m http.server 8080
```
打开 http://localhost:8080 。
Expected: 看到手机框、「凝稳」标题、底部 3 个 Tab、右下角蓝色「+」。点击 Tab 能切换（内容暂空）。

- [ ] **Step 6: 提交**

```bash
cd /Users/sunjian/Downloads/inr-tracker && git add -A && git commit -m "chore(prototype): scaffold shell and tab navigation"
```

---

## Task 2: 假数据与状态模块

**Files:**
- Create: `prototype/mock-data.js`
- Modify: `prototype/app.js`

- [ ] **Step 1: 写 `prototype/mock-data.js`**

```js
// 假数据：约 12 条跨越半年的 INR 记录，含达标/偏高/偏低
export const mockRecords = [
  { id: 'r12', value: 2.4, measuredAt: '2026-10-06T09:10:00', doseTabs: 1.5, note: '饮食正常' },
  { id: 'r11', value: 2.9, measuredAt: '2026-09-29T09:00:00', doseTabs: 1.5, note: '' },
  { id: 'r10', value: 3.4, measuredAt: '2026-09-22T09:20:00', doseTabs: 1.5, note: '感冒，服用了感冒药' },
  { id: 'r09', value: 3.1, measuredAt: '2026-09-15T09:00:00', doseTabs: 1.75, note: '' },
  { id: 'r08', value: 2.6, measuredAt: '2026-09-08T09:00:00', doseTabs: 1.75, note: '' },
  { id: 'r07', value: 2.1, measuredAt: '2026-09-01T09:30:00', doseTabs: 1.75, note: '牙龈轻微出血' },
  { id: 'r06', value: 1.8, measuredAt: '2026-08-25T09:00:00', doseTabs: 1.5, note: '' },
  { id: 'r05', value: 2.3, measuredAt: '2026-08-11T09:00:00', doseTabs: 1.5, note: '' },
  { id: 'r04', value: 2.7, measuredAt: '2026-07-28T09:00:00', doseTabs: 1.25, note: '' },
  { id: 'r03', value: 3.2, measuredAt: '2026-07-14T09:00:00', doseTabs: 1.25, note: '' },
  { id: 'r02', value: 2.5, measuredAt: '2026-06-30T09:00:00', doseTabs: 1.25, note: '' },
  { id: 'r01', value: 2.2, measuredAt: '2026-06-16T09:00:00', doseTabs: 1.5, note: '初诊后第一次复查' },
];

export const defaultSettings = {
  targetMin: 2.0,
  targetMax: 3.0,
  testIntervalDays: 7,
  reminderEnabled: true,
  theme: 'light',
};
```

- [ ] **Step 2: 在 `prototype/app.js` 顶部增加状态与工具**

```js
import { mockRecords, defaultSettings } from './mock-data.js';

// 单一数据源：所有渲染都读它
const state = {
  records: [...mockRecords].sort((a, b) => new Date(b.measuredAt) - new Date(a.measuredAt)),
  settings: { ...defaultSettings },
};

// 达标判断：<下限 偏低 / 区间内 达标 / >上限 偏高
function judge(value, settings = state.settings) {
  if (value < settings.targetMin) return 'low';
  if (value > settings.targetMax) return 'high';
  return 'ok';
}

const STATUS_TEXT = { ok: '达标', high: '偏高', low: '偏低' };

function fmtDate(iso) {
  const d = new Date(iso);
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`;
}
```

- [ ] **Step 3: 浏览器验证**

刷新页面。Expected: 无控制台报错（数据模块被正确加载，虽暂未渲染）。

- [ ] **Step 4: 提交**

```bash
cd /Users/sunjian/Downloads/inr-tracker && git add -A && git commit -m "feat(prototype): add mock data and state module"
```

---

## Task 3: 首页 - 状态大卡片

**Files:**
- Modify: `prototype/app.js`
- Modify: `prototype/styles.css`

- [ ] **Step 1: 在 `styles.css` 增加卡片与状态样式**

```css
.card {
  background: var(--card); border-radius: var(--radius);
  box-shadow: var(--shadow); padding: 20px; margin-bottom: 16px;
}
.hero { text-align: center; }
.hero__label { color: var(--text-sub); font-size: 14px; }
.hero__value { font-size: 56px; font-weight: 700; line-height: 1.1; margin: 6px 0; }
.hero__badge {
  display: inline-block; padding: 4px 12px; border-radius: 999px;
  font-size: 14px; font-weight: 600;
}
.badge--ok { color: var(--ok); background: var(--ok-bg); }
.badge--high { color: var(--high); background: var(--high-bg); }
.badge--low { color: var(--low); background: var(--low-bg); }
.hero__meta { color: var(--text-sub); font-size: 13px; margin-top: 10px; }
.hero__delta { font-weight: 600; }
```

- [ ] **Step 2: 在 `app.js` 增加首页渲染（先只渲染 hero）**

```js
const viewHome = document.getElementById('view-home');

function nextTestDays() {
  const last = state.records[0];
  if (!last) return null;
  const due = new Date(last.measuredAt);
  due.setDate(due.getDate() + state.settings.testIntervalDays);
  const diff = Math.ceil((due - new Date()) / 86400000);
  return diff;
}

function renderHome() {
  const last = state.records[0];
  if (!last) {
    viewHome.innerHTML = `<div class="card empty">还没有记录，点右下角「+」记第一条</div>`;
    return;
  }
  const status = judge(last.value);
  const prev = state.records[1];
  let delta = '—';
  if (prev) {
    const d = +(last.value - prev.value).toFixed(1);
    delta = d > 0 ? `↑ ${d}` : d < 0 ? `↓ ${Math.abs(d)}` : '持平';
  }
  const days = nextTestDays();
  viewHome.innerHTML = `
    <section class="card hero">
      <div class="hero__label">最近一次 INR</div>
      <div class="hero__value">${last.value.toFixed(1)}</div>
      <span class="hero__badge badge--${status}">${STATUS_TEXT[status]}</span>
      <div class="hero__meta">
        较上次 <span class="hero__delta">${delta}</span> · ${fmtDate(last.measuredAt)}
      </div>
      <div class="hero__meta">
        ${days !== null && days >= 0 ? `距下次测量还有 ${days} 天` : '已超过测量时间'}
      </div>
    </section>`;
}
```

- [ ] **Step 3: 在 `app.js` 末尾调用渲染**

```js
function renderAll() { renderHome(); }
renderAll();
```

- [ ] **Step 4: 浏览器验证**

刷新。Expected: 首页显示大号 `2.4`、绿色「达标」徽标、「较上次 ↑ …」、日期、距下次测量天数。

- [ ] **Step 5: 提交**

```bash
cd /Users/sunjian/Downloads/inr-tracker && git add -A && git commit -m "feat(prototype): render home hero status card"
```

---

## Task 4: 首页 - 最近记录列表

**Files:**
- Modify: `prototype/app.js`
- Modify: `prototype/styles.css`

- [ ] **Step 1: 增加列表样式**

```css
.section-title {
  font-size: 14px; color: var(--text-sub); margin: 8px 4px;
  display: flex; justify-content: space-between; align-items: center;
}
.rec {
  display: flex; align-items: center; gap: 12px;
  background: var(--card); border-radius: 14px; padding: 14px;
  margin-bottom: 10px; box-shadow: var(--shadow);
}
.rec__val { font-size: 22px; font-weight: 700; min-width: 52px; }
.rec__val--high { color: var(--high); }
.rec__val--low { color: var(--low); }
.rec__body { flex: 1; }
.rec__date { font-size: 13px; color: var(--text-sub); }
.rec__note { font-size: 13px; color: var(--text); margin-top: 2px; }
.rec__dose { font-size: 12px; color: var(--text-sub); }
```

- [ ] **Step 2: 在 `renderHome` 内追加列表（hero 之后）**

在 `renderHome` 的 `viewHome.innerHTML = \`...\`;` 之后追加列表拼接。将 `renderHome` 改为：先算 `heroHtml`，再算 `listHtml`，最后一次性写入：

```js
function recordRowHtml(r) {
  const s = judge(r.value);
  const valCls = s === 'ok' ? '' : `rec__val--${s}`;
  return `
    <div class="rec" data-id="${r.id}">
      <div class="rec__val ${valCls}">${r.value.toFixed(1)}</div>
      <div class="rec__body">
        <div class="rec__date">${fmtDate(r.measuredAt)}</div>
        ${r.note ? `<div class="rec__note">${r.note}</div>` : ''}
      </div>
      ${r.doseTabs ? `<div class="rec__dose">${r.doseTabs} 片</div>` : ''}
    </div>`;
}
```

在 `renderHome` 末尾（hero 写完后）追加：

```js
  const list = state.records.slice(0, 5).map(recordRowHtml).join('');
  viewHome.insertAdjacentHTML('beforeend',
    `<div class="section-title">最近记录</div>${list}`);
```

- [ ] **Step 3: 浏览器验证**

刷新。Expected: hero 下方出现「最近记录」标题与 5 条记录卡片；偏高记录数值显示红色、偏低显示蓝色。

- [ ] **Step 4: 提交**

```bash
cd /Users/sunjian/Downloads/inr-tracker && git add -A && git commit -m "feat(prototype): render recent records list"
```

---

## Task 5: 记录表单弹层（打开 / 关闭）

**Files:**
- Modify: `prototype/index.html`（无需改结构，`#sheet` 已存在）
- Modify: `prototype/app.js`
- Modify: `prototype/styles.css`

- [ ] **Step 1: 增加弹层样式**

```css
.sheet__panel {
  width: 100%; background: var(--card);
  border-radius: 20px 20px 0 0; padding: 20px;
  animation: slideUp .22s ease;
}
@keyframes slideUp { from { transform: translateY(100%); } to { transform: translateY(0); } }
.field { margin-bottom: 14px; }
.field label { display: block; font-size: 13px; color: var(--text-sub); margin-bottom: 6px; }
.field input, .field textarea {
  width: 100%; border: 1px solid var(--line); border-radius: 10px;
  padding: 12px; font-size: 16px; font-family: inherit;
}
.field__big input { font-size: 32px; font-weight: 700; text-align: center; }
.chips { display: flex; flex-wrap: wrap; gap: 8px; }
.chip {
  border: 1px solid var(--line); background: #fff; border-radius: 999px;
  padding: 6px 12px; font-size: 13px; cursor: pointer;
}
.chip.is-on { background: var(--brand); color: #fff; border-color: var(--brand); }
.btn-row { display: flex; gap: 12px; margin-top: 8px; }
.btn {
  flex: 1; border: 0; border-radius: 12px; padding: 14px;
  font-size: 16px; cursor: pointer;
}
.btn--ghost { background: #eef1f5; color: var(--text); }
.btn--primary { background: var(--brand); color: #fff; font-weight: 600; }
```

- [ ] **Step 2: 在 `app.js` 增加弹层控制与表单结构**

```js
const sheet = document.getElementById('sheet');

function openRecordSheet() {
  const lastDose = state.records.find((r) => r.doseTabs)?.doseTabs ?? '';
  sheet.innerHTML = `
    <div class="sheet__panel">
      <div class="section-title">记录一次 INR</div>
      <div class="field field__big">
        <label>INR 数值</label>
        <input id="f-value" type="number" inputmode="decimal" step="0.1" placeholder="如 2.4" />
      </div>
      <div class="field">
        <label>测量时间</label>
        <input id="f-time" type="datetime-local" />
      </div>
      <div class="field">
        <label>华法林用量（片，可选）</label>
        <input id="f-dose" type="number" inputmode="decimal" step="0.25" value="${lastDose}" />
      </div>
      <div class="field">
        <label>备注（可选）</label>
        <div class="chips" id="f-chips">
          ${['出血/淤青','饮食变化','漏服','感冒/感染'].map((t) =>
            `<span class="chip" data-note="${t}">${t}</span>`).join('')}
        </div>
        <textarea id="f-note" rows="2" placeholder="补充说明"></textarea>
      </div>
      <div class="btn-row">
        <button class="btn btn--ghost" id="f-cancel">取消</button>
        <button class="btn btn--primary" id="f-save">保存</button>
      </div>
    </div>`;
  // 默认时间为现在
  const now = new Date();
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
  sheet.querySelector('#f-time').value = now.toISOString().slice(0, 16);
  // 快捷标签切换
  sheet.querySelectorAll('.chip').forEach((c) =>
    c.addEventListener('click', () => c.classList.toggle('is-on')));
  sheet.hidden = false;
  sheet.querySelector('#f-cancel').addEventListener('click', () => { sheet.hidden = true; });
}
```

- [ ] **Step 3: 绑定 FAB**

在 `app.js` 增加：

```js
document.getElementById('fab').addEventListener('click', openRecordSheet);
```

- [ ] **Step 4: 浏览器验证**

刷新，点「+」。Expected: 从底部滑出表单，含大号 INR 输入、时间（默认现在）、用量（片，带出上次值 1.5）、快捷标签与备注、取消/保存按钮；点「取消」关闭。

- [ ] **Step 5: 提交**

```bash
cd /Users/sunjian/Downloads/inr-tracker && git add -A && git commit -m "feat(prototype): record bottom sheet with form"
```

---

## Task 6: 保存记录（校验 + 状态更新 + 反馈）

**Files:**
- Modify: `prototype/app.js`
- Modify: `prototype/styles.css`

- [ ] **Step 1: 增加 toast 显示函数**

```css
.toast[hidden] { display: none; }
```

```js
const toastEl = document.getElementById('toast');
let toastTimer = null;
function toast(msg) {
  toastEl.textContent = msg;
  toastEl.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { toastEl.hidden = true; }, 2000);
}
```

- [ ] **Step 2: 增加保存逻辑**

在 `openRecordSheet` 内、绑定取消之后追加保存事件：

```js
  sheet.querySelector('#f-save').addEventListener('click', () => {
    const raw = sheet.querySelector('#f-value').value.trim();
    const value = parseFloat(raw);
    if (!raw || Number.isNaN(value)) { toast('请输入 INR 数值'); return; }
    if (value < 0.5 || value > 10) {
      if (!confirm(`数值 ${value} 超出常见范围（0.5–10），确认保存？`)) return;
    }
    const timeVal = sheet.querySelector('#f-time').value;
    const noteChips = [...sheet.querySelectorAll('.chip.is-on')].map((c) => c.dataset.note);
    const freeNote = sheet.querySelector('#f-note').value.trim();
    const note = [...noteChips, freeNote].filter(Boolean).join('；');
    const doseRaw = sheet.querySelector('#f-dose').value.trim();
    const rec = {
      id: 'r' + Date.now(),
      value: +value.toFixed(1),
      measuredAt: new Date(timeVal).toISOString(),
      doseTabs: doseRaw ? parseFloat(doseRaw) : null,
      note,
    };
    state.records.unshift(rec);
    state.records.sort((a, b) => new Date(b.measuredAt) - new Date(a.measuredAt));
    sheet.hidden = true;
    renderAll();
    toast(`已保存：${rec.value.toFixed(1)} · ${STATUS_TEXT[judge(rec.value)]}`);
  });
```

- [ ] **Step 3: 浏览器验证**

刷新 → 点「+」→ 输入 `2.6` → 保存。
Expected: 弹层关闭；首页 hero 更新为 `2.6` 且状态正确；toast 提示「已保存：2.6 · 达标」；列表顶部出现新记录。再试输入 `abc` 应提示「请输入 INR 数值」；输入 `12` 应二次确认。

- [ ] **Step 4: 提交**

```bash
cd /Users/sunjian/Downloads/inr-tracker && git add -A && git commit -m "feat(prototype): save record with validation and feedback"
```

---

## Task 7: 趋势页 - SVG 折线图 + 目标区间带

**Files:**
- Modify: `prototype/app.js`
- Modify: `prototype/styles.css`

- [ ] **Step 1: 增加图表样式**

```css
.chart { width: 100%; height: 220px; }
.chart .band { fill: var(--ok-bg); }
.chart .axis { stroke: var(--line); stroke-width: 1; }
.chart .line { fill: none; stroke: var(--brand); stroke-width: 2.5; }
.chart .dot { fill: var(--brand); }
.chart .dot--high { fill: var(--high); }
.chart .dot--low { fill: var(--low); }
.chart .lbl { font-size: 10px; fill: var(--text-sub); }
```

- [ ] **Step 2: 在 `app.js` 增加图表渲染**

```js
const viewTrend = document.getElementById('view-trend');

function buildChart(records) {
  const W = 340, H = 220, padL = 28, padR = 10, padT = 12, padB = 24;
  const cfg = state.settings;
  const vals = records.map((r) => r.value);
  // y 轴范围纳入目标上下限，保证色带可见
  const yMin = Math.min(...vals, cfg.targetMin) - 0.5;
  const yMax = Math.max(...vals, cfg.targetMax) + 0.5;
  const x = (i) => padL + (records.length <= 1 ? 0 : (i / (records.length - 1)) * (W - padL - padR));
  const y = (v) => padT + (1 - (v - yMin) / (yMax - yMin)) * (H - padT - padB);
  const pts = records.map((r, i) => `${x(i)},${y(r.value)}`).join(' ');
  const bandTop = y(cfg.targetMax), bandH = y(cfg.targetMin) - bandTop;
  const dots = records.map((r, i) => {
    const s = judge(r.value);
    return `<circle class="dot ${s === 'ok' ? '' : 'dot--' + s}" cx="${x(i)}" cy="${y(r.value)}" r="3.5" />`;
  }).join('');
  return `
    <svg class="chart" viewBox="0 0 ${W} ${H}">
      <rect class="band" x="${padL}" y="${bandTop}" width="${W - padL - padR}" height="${bandH}" />
      <line class="axis" x1="${padL}" y1="${H - padB}" x2="${W - padR}" y2="${H - padB}" />
      <polyline class="line" points="${pts}" />
      ${dots}
      <text class="lbl" x="2" y="${y(cfg.targetMax) + 3}">${cfg.targetMax}</text>
      <text class="lbl" x="2" y="${y(cfg.targetMin) + 3}">${cfg.targetMin}</text>
    </svg>`;
}
```

- [ ] **Step 3: 临时渲染验证**

```js
function renderTrend() {
  viewTrend.innerHTML = `<section class="card">${buildChart([...state.records].reverse())}</section>`;
}
```
把 `renderAll` 改为 `function renderAll() { renderHome(); renderTrend(); }`（设置页留待 Task 9）。

- [ ] **Step 4: 浏览器验证**

刷新，切到「趋势」。
Expected: 显示折线图，中间有绿色目标区间横带（2.0–3.0），偏高/偏低的数据点分别为红/蓝，左侧标有目标上下限。

- [ ] **Step 5: 提交**

```bash
cd /Users/sunjian/Downloads/inr-tracker && git add -A && git commit -m "feat(prototype): trend SVG chart with target band"
```

---

## Task 8: 趋势页 - 时间范围切换与统计

**Files:**
- Modify: `prototype/app.js`
- Modify: `prototype/styles.css`

- [ ] **Step 1: 增加范围按钮与统计样式**

```css
.seg { display: flex; gap: 8px; margin-bottom: 12px; }
.seg button {
  flex: 1; border: 1px solid var(--line); background: #fff;
  border-radius: 10px; padding: 8px; font-size: 13px; cursor: pointer;
}
.seg button.is-on { background: var(--brand); color: #fff; border-color: var(--brand); }
.stats { display: grid; grid-template-columns: repeat(3, 1fr); text-align: center; }
.stats b { display: block; font-size: 20px; }
.stats span { font-size: 12px; color: var(--text-sub); }
```

- [ ] **Step 2: 改写 `renderTrend` 支持范围与统计**

```js
let trendRange = 90; // 天

function filteredRecords(days) {
  if (days === Infinity) return [...state.records].reverse();
  const since = Date.now() - days * 86400000;
  return [...state.records].reverse().filter((r) => new Date(r.measuredAt).getTime() >= since);
}

function statsHtml(records) {
  if (!records.length) return '';
  const vals = records.map((r) => r.value);
  const avg = vals.reduce((a, b) => a + b, 0) / vals.length;
  const rate = Math.round(vals.filter((v) => judge(v) === 'ok').length / vals.length * 100);
  return `<div class="card stats">
    <div><b>${avg.toFixed(1)}</b><span>平均 INR</span></div>
    <div><b>${rate}%</b><span>达标率</span></div>
    <div><b>${Math.max(...vals).toFixed(1)}</b><span>最高</span></div>
  </div>`;
}

function renderTrend() {
  const ranges = [[30, '1月'], [90, '3月'], [180, '半年'], [Infinity, '全部']];
  const recs = filteredRecords(trendRange);
  viewTrend.innerHTML = `
    <div class="seg">
      ${ranges.map(([d, t]) =>
        `<button data-days="${d}" class="${trendRange === d ? 'is-on' : ''}">${t}</button>`).join('')}
    </div>
    <section class="card">
      ${recs.length ? buildChart(recs) : '<div class="empty">该时间范围内暂无记录</div>'}
    </section>
    ${statsHtml(recs)}`;
  viewTrend.querySelectorAll('.seg button').forEach((b) =>
    b.addEventListener('click', () => { trendRange = Number(b.dataset.days); renderTrend(); }));
}
```

- [ ] **Step 3: 浏览器验证**

刷新，切「趋势」。Expected: 顶部有 `1月/3月/半年/全部` 切换；点击切换图表数据与统计；「1月」范围含 2–3 个点；下方显示平均 INR、达标率、最高。

- [ ] **Step 4: 提交**

```bash
cd /Users/sunjian/Downloads/inr-tracker && git add -A && git commit -m "feat(prototype): trend range switcher and stats"
```

---

## Task 9: 我的 / 设置页

**Files:**
- Modify: `prototype/app.js`
- Modify: `prototype/styles.css`

- [ ] **Step 1: 增加设置项样式**

```css
.setting {
  display: flex; justify-content: space-between; align-items: center;
  background: var(--card); border-radius: 14px; padding: 16px;
  margin-bottom: 10px; box-shadow: var(--shadow);
}
.setting label { font-size: 15px; }
.setting input[type=number], .setting input[type=time] {
  width: 96px; border: 1px solid var(--line); border-radius: 8px;
  padding: 8px; font-size: 15px; text-align: right;
}
.setting input[type=checkbox] { width: 22px; height: 22px; }
.row2 { display: flex; gap: 8px; align-items: center; }
.privacy { font-size: 12px; color: var(--text-sub); text-align: center; margin-top: 16px; line-height: 1.7; }
```

- [ ] **Step 2: 渲染设置页**

```js
const viewSettings = document.getElementById('view-settings');

function renderSettings() {
  const s = state.settings;
  viewSettings.innerHTML = `
    <div class="setting">
      <label>目标范围（INR）</label>
      <div class="row2">
        <input id="s-min" type="number" step="0.1" value="${s.targetMin}" />
        <span>–</span>
        <input id="s-max" type="number" step="0.1" value="${s.targetMax}" />
      </div>
    </div>
    <div class="setting">
      <label>下次测量间隔（天）</label>
      <input id="s-interval" type="number" min="1" value="${s.testIntervalDays}" />
    </div>
    <div class="setting">
      <label>测量提醒</label>
      <input id="s-remind" type="checkbox" ${s.reminderEnabled ? 'checked' : ''} />
    </div>
    <div class="setting" id="export-entry">
      <label>导出 / 分享数据</label><span>›</span>
    </div>
    <div class="privacy">数据仅保存在本机，不上传、无账号、无广告。<br/>本工具仅用于记录，不构成医疗建议。</div>`;
  bindSettings();
}
```

- [ ] **Step 3: 绑定设置变更**

```js
function bindSettings() {
  const s = state.settings;
  const apply = () => {
    const min = parseFloat(viewSettings.querySelector('#s-min').value);
    const max = parseFloat(viewSettings.querySelector('#s-max').value);
    if (!Number.isNaN(min) && !Number.isNaN(max) && min < max) {
      s.targetMin = min; s.targetMax = max;
    }
    s.testIntervalDays = Math.max(1, parseInt(viewSettings.querySelector('#s-interval').value, 10) || s.testIntervalDays);
    s.reminderEnabled = viewSettings.querySelector('#s-remind').checked;
    renderHome(); renderTrend();
  };
  viewSettings.querySelectorAll('input').forEach((el) => el.addEventListener('change', apply));
  viewSettings.querySelector('#export-entry').addEventListener('click', openExportModal);
}
```

- [ ] **Step 4: 接入 `renderAll`**

```js
function renderAll() { renderHome(); renderTrend(); renderSettings(); }
```

- [ ] **Step 5: 浏览器验证**

刷新，切「我的」。Expected: 显示目标范围、下次测量间隔（天，可填数字）、提醒开关、导出入口、隐私说明。把间隔从 7 改为 14 后切回首页，倒计时天数应变大；把上限从 3.0 改为 3.5 后切回首页，原「偏高」的 3.4 应变为「达标」。

- [ ] **Step 6: 提交**

```bash
cd /Users/sunjian/Downloads/inr-tracker && git add -A && git commit -m "feat(prototype): settings page wired to state"
```

---

## Task 10: 导出弹窗与收尾

**Files:**
- Modify: `prototype/app.js`
- Modify: `prototype/styles.css`

- [ ] **Step 1: 增加弹窗样式**

```css
.modal__panel {
  width: 300px; background: var(--card); border-radius: 16px; padding: 20px;
  text-align: center;
}
.modal__panel h3 { margin: 0 0 8px; font-size: 16px; }
.modal__panel p { color: var(--text-sub); font-size: 13px; margin: 0 0 16px; }
```

- [ ] **Step 2: 实现导出弹窗**

```js
const modal = document.getElementById('modal');

function openExportModal() {
  modal.innerHTML = `
    <div class="modal__panel">
      <h3>导出数据</h3>
      <p>生成后可分享给医生（原型仅做演示，不真正下载）</p>
      <div class="btn-row">
        <button class="btn btn--ghost" id="ex-img">趋势图</button>
        <button class="btn btn--primary" id="ex-csv">CSV 表格</button>
      </div>
      <div style="margin-top:12px"><button class="btn btn--ghost" id="ex-close">关闭</button></div>
    </div>`;
  modal.hidden = false;
  const close = () => { modal.hidden = true; };
  modal.querySelector('#ex-close').addEventListener('click', close);
  modal.querySelector('#ex-img').addEventListener('click', () => { close(); toast('已生成趋势图（演示）'); });
  modal.querySelector('#ex-csv').addEventListener('click', () => { close(); toast('已生成 CSV（演示）'); });
}
```

- [ ] **Step 3: 收尾 QA 清单（逐项目视验证）**

刷新后逐项确认：
1. 首页 hero 数值/徽标/天数正确；无记录时空态文案（临时把 `state.records = []` 验证后还原）。
2. 「+」→ 输入非法值被拦截、超范围二次确认、正常保存后首页与趋势同步更新。
3. 趋势「1月/3月/半年/全部」切换与统计正确。
4. 设置改目标范围后，首页与趋势的状态色同步变化。
5. 导出弹窗两个按钮与关闭都可用，toast 正常显示后自动消失。
6. 手机框内滚动正常，底部导航与 FAB 不遮挡内容。

- [ ] **Step 4: 提交**

```bash
cd /Users/sunjian/Downloads/inr-tracker && git add -A && git commit -m "feat(prototype): export modal and final QA polish"
```

---

## 自查（Self-Review）

**1. 规格覆盖检查**（对照 spec 第 4/5/6/8 节）：
- 首页状态卡 → Task 3 ✅
- 最近记录列表 → Task 4 ✅
- 记录表单 + 校验 + 保存反馈 → Task 5、6 ✅
- 趋势折线图 + 目标带 + 范围切换 + 统计 → Task 7、8 ✅
- 我的/设置（目标范围、周期、提醒、导出、免责）→ Task 9 ✅
- 导出/分享 → Task 10 ✅
- 数据模型（记录含值/时间/用量(片)/备注；设置项）→ Task 2 ✅
- 达标判断逻辑 → Task 2 `judge()` ✅

**2. 占位符扫描**：无 TBD/TODO；每个改代码的步骤都给出完整代码。

**3. 类型/命名一致性**：核心标识 `state`、`judge()`、`STATUS_TEXT`、`renderAll()`、`renderHome()`、`renderTrend()`、`renderSettings()`、`openRecordSheet()`、`openExportModal()` 全程一致；`trendRange` 单位统一为「天」，`Infinity` 表示全部。

**说明**：Task 8 的 `filteredRecords` 在「全部」时使用 `Infinity`，`trendRange === d` 比较中 `Infinity === Infinity` 成立，按钮高亮正确。
