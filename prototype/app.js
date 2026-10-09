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

// Tab 切换
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

const viewHome = document.getElementById('view-home');

function nextTestDays() {
  const last = state.records[0];
  if (!last) return null;
  const due = new Date(last.measuredAt);
  due.setDate(due.getDate() + state.settings.testIntervalDays);
  const diff = Math.ceil((due - new Date()) / 86400000);
  return diff;
}

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
  const list = state.records.slice(0, 5).map(recordRowHtml).join('');
  viewHome.insertAdjacentHTML('beforeend',
    `<div class="section-title">最近记录</div>${list}`);
}

const sheet = document.getElementById('sheet');
const toastEl = document.getElementById('toast');
let toastTimer = null;

function toast(msg) {
  toastEl.textContent = msg;
  toastEl.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { toastEl.hidden = true; }, 2000);
}

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
}

document.getElementById('fab').addEventListener('click', openRecordSheet);

const viewTrend = document.getElementById('view-trend');
let trendRange = 90; // 天

function filteredRecords(days) {
  if (days === Infinity) return [...state.records].reverse();
  const since = Date.now() - days * 86400000;
  return [...state.records].reverse().filter((r) => new Date(r.measuredAt).getTime() >= since);
}

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

function renderAll() { renderHome(); renderTrend(); renderSettings(); }

initTabs();
renderAll();
