export function formatCurrency(amount: number, currency = 'EUR', locale = 'fr-FR'): string {
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency
  }).format(amount)
}

export function formatDate(isoDate: string, locale = 'fr-FR'): string {
  const date = new Date(isoDate)
  if (Number.isNaN(date.getTime())) {
    return isoDate
  }
  return new Intl.DateTimeFormat(locale, {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  }).format(date)
}
