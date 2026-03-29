export interface NormalizedLyricLine {
  time: number
  text: string
}

const TIME_TAG_REGEXP = /\[(\d{1,3}):(\d{2})(?:\.(\d{1,3}))?\]/g
const META_TAG_REGEXP = /^\[(ti|ar|al|by|offset):.*\]$/i

const toMs = (mm: string, ss: string, ff?: string) => {
  const minutes = Number(mm) || 0
  const seconds = Number(ss) || 0
  const fractionRaw = ff || '0'
  const fraction = Number(fractionRaw.padEnd(3, '0').slice(0, 3)) || 0
  return minutes * 60_000 + seconds * 1000 + fraction
}

const decodeEscapedLineBreaks = (value: string) => {
  return value
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
    .replace(/\\r\\n/g, '\n')
    .replace(/\\n/g, '\n')
    .replace(/\\r/g, '\n')
}

const parseLrcText = (rawText: string): NormalizedLyricLine[] => {
  const result: NormalizedLyricLine[] = []
  const content = decodeEscapedLineBreaks(rawText)
  const rows = content.split('\n')
  for (const row of rows) {
    const line = row.trim()
    if (!line || META_TAG_REGEXP.test(line)) continue
    const matches = [...line.matchAll(TIME_TAG_REGEXP)]
    const text = line.replace(TIME_TAG_REGEXP, '').trim()
    if (!matches.length) continue
    for (const match of matches) {
      result.push({
        time: toMs(match[1], match[2], match[3]),
        text
      })
    }
  }
  return result
}

export const normalizeLyrics = (rawLines: any[]): NormalizedLyricLine[] => {
  const result: NormalizedLyricLine[] = []
  for (const item of rawLines || []) {
    const text = String(item?.text ?? '')
    const baseTime = Number(item?.time || 0)
    const parsed = parseLrcText(text)
    if (parsed.length) {
      result.push(...parsed)
      continue
    }
    const plainText = decodeEscapedLineBreaks(text)
      .split('\n')
      .map((line) => line.trim())
      .filter(Boolean)
    if (!plainText.length) continue
    if (plainText.length === 1) {
      result.push({ text: plainText[0], time: baseTime })
      continue
    }
    for (const line of plainText) {
      result.push({ text: line, time: baseTime })
    }
  }
  return result.sort((a, b) => a.time - b.time)
}
