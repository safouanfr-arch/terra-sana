export function toCsv(rows) {
  return rows
    .map(row => row.map(escapeCsvCell).join(';'))
    .join('\n')
}

export function downloadCsv(fileName, rows) {
  const blob = new Blob(['\ufeff' + toCsv(rows)], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

export function addCsvSection(rows, title) {
  if (rows.length > 0) {
    rows.push([])
  }
  rows.push([title])
}

export function addCsvKeyValues(rows, entries) {
  for (const [label, value] of entries) {
    rows.push([label, value ?? ''])
  }
}

export function formatCsvDate(value) {
  if (!value) return ''
  const normalized = typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value)
    ? `${value}T00:00:00`
    : value
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleDateString('fr-BE')
}

export function formatCsvDateTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('fr-BE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

export function formatCsvMoney(value) {
  return formatCsvNumber(value, 2, 2)
}

export function formatCsvQuantity(value) {
  const number = Number(value || 0)
  const hasDecimals = Math.abs(number % 1) > 0
  return formatCsvNumber(number, hasDecimals ? 2 : 0, 3)
}

export function exportDateStamp() {
  return new Date().toISOString().slice(0, 10)
}

function formatCsvNumber(value, minimumFractionDigits, maximumFractionDigits) {
  return Number(value || 0).toLocaleString('fr-BE', {
    minimumFractionDigits,
    maximumFractionDigits
  })
}

function escapeCsvCell(value) {
  return `"${String(value ?? '').replaceAll('"', '""')}"`
}
