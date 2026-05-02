import dayjs from 'dayjs';

export function formatDate(dateStr: string | null | undefined): string {
  if (!dateStr) return '-';
  return dayjs(dateStr).format('YYYY-MM-DD HH:mm:ss');
}

export function formatDateShort(dateStr: string | null | undefined): string {
  if (!dateStr) return '-';
  return dayjs(dateStr).format('MM-DD HH:mm');
}

export function formatAge(months: number | null | undefined): string {
  if (!months && months !== 0) return '-';
  if (months < 12) return `${months}个月`;
  const years = Math.floor(months / 12);
  const remainMonths = months % 12;
  return remainMonths > 0 ? `${years}岁${remainMonths}个月` : `${years}岁`;
}
