export const normalizeImageUrl = (url?: string) => {
  const value = (url || '').trim()
  if (!value) return ''
  if (/^(https?:)?\/\//i.test(value) || value.startsWith('data:')) return value
  if (value.startsWith('/static/') || value.startsWith('/user-static/')) return value
  if (value.startsWith('/')) return `/static${value}`
  return `/static/${value}`
}
