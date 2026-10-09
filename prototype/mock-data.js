// 假数据：跨越半年的 INR 记录
// 每条记录包含：id, value, measuredAt, doseTabs(华法林用量，片), note
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

// 默认设置：目标范围 2.0–3.0，下次测量间隔 7 天（一周）
export const defaultSettings = {
  targetMin: 2.0,
  targetMax: 3.0,
  testIntervalDays: 7,
  reminderEnabled: true,
  theme: 'light',
};
