const dateFormatter = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit'
})

const fullFormatter = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit'
})

function toDate(value) {
  if (!value) return null
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

export function timeAgo(value) {
  const date = toDate(value)
  if (!date) return ''
  const seconds = Math.floor((Date.now() - date.getTime()) / 1000)
  if (seconds < 30) return '刚刚'
  if (seconds < 60) return `${seconds} 秒前`
  if (seconds < 3600) return `${Math.floor(seconds / 60)} 分钟前`
  if (seconds < 86400) return `${Math.floor(seconds / 3600)} 小时前`
  if (seconds < 86400 * 30) return `${Math.floor(seconds / 86400)} 天前`
  if (seconds < 86400 * 365) return `${Math.floor(seconds / (86400 * 30))} 个月前`
  return dateFormatter.format(date)
}

export function formatDate(value) {
  const date = toDate(value)
  return date ? dateFormatter.format(date) : ''
}

export function formatDateTime(value) {
  const date = toDate(value)
  return date ? fullFormatter.format(date) : ''
}

export function formatSize(bytes) {
  if (bytes === null || bytes === undefined) return ''
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

export function shortSha(sha) {
  return sha ? sha.slice(0, 7) : ''
}
