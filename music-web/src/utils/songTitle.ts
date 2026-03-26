import * as OpenCC from 'opencc-js'

const normalizeSpaces = (s: string) => s.replace(/\s+/g, ' ').trim()

const stripTrackIndexPrefix = (s: string) => {
  // e.g. "1.不如见一面", "01. xxx", "2、xxx", "3 - xxx"
  // also support: "12 我不願讓你一個人" / "02 擁抱"
  return s.replace(/^\s*[《〈(（]?\s*\d{1,3}\s*(?:[.．、-]|-\s)?\s*/u, '')
}

const splitByDash = (s: string) => {
  // common separators: " - ", "-", " — ", "—"
  if (s.includes(' - ')) return s.split(' - ')
  if (s.includes(' — ')) return s.split(' — ')
  if (s.includes('—')) return s.split('—')
  if (s.includes('-')) return s.split('-')
  return [s]
}

const tokenizeArtists = (artistName?: string, artistNames?: string) => {
  const raw = normalizeSpaces([artistName, artistNames].filter(Boolean).join(' / '))
  if (!raw) return []
  return raw
    .split(/[\/、,，&＆;；|]+/u)
    .map((x) => normalizeSpaces(x))
    .filter(Boolean)
}

const stripAudioExtension = (s: string) => {
  // e.g. "相爱很难.flac" -> "相爱很难"（只在末尾看起来像音频扩展名时去掉）
  return s.replace(/\.(?:flac|mp3|wav|m4a|aac|ogg|opus|wma|aiff|alac)$/iu, '')
}

const toSimplifiedConverter = (() => {
  try {
    // Traditional (TW) -> Simplified (CN)
    return (OpenCC as any).Converter({ from: 'tw', to: 'cn' })
  } catch {
    return null
  }
})()

const simplifiedCache = new Map<string, string>()
const toSimplifiedCached = (s: string) => {
  if (!toSimplifiedConverter) return s
  const cached = simplifiedCache.get(s)
  if (cached) return cached
  try {
    const result = toSimplifiedConverter(s)
    simplifiedCache.set(s, result)
    return result
  } catch {
    return s
  }
}

/**
 * 只用于“展示层”的歌名提取，不改动数据本体。
 * 规则：
 * - 去掉前缀曲序： "1." / "2、" / "03-" 等
 * - 若形如 "歌手 - 歌名" 且左侧匹配歌手，则展示右侧
 * - 否则保持原样（避免误伤本来就带 '-' 的歌名）
 */
export function getDisplaySongTitle(song: any): string {
  const raw = normalizeSpaces(String(song?.title || song?.name || ''))
  if (!raw) return '未知歌曲'

  const noIndex = normalizeSpaces(stripTrackIndexPrefix(raw)).replace(/\s*[》〉】]$/u, '')
  const noIndexNoExt = stripAudioExtension(noIndex)

  const parts = splitByDash(noIndex).map((p) => normalizeSpaces(p)).filter(Boolean)
  if (parts.length >= 2) {
    const left = parts[0]
    const right = stripAudioExtension(parts.slice(1).join(' - '))
    const artists = tokenizeArtists(song?.artistName, song?.artistNames)

    // 只有在“左边确实像歌手名”时才切分
    if (artists.some((a) => a && left.includes(a))) return toSimplifiedCached(right || noIndexNoExt)
  }

  return toSimplifiedCached(noIndexNoExt)
}

