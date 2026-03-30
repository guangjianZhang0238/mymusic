<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePlayerStore, type PlayMode } from '@/stores/player'
import { checkFavoriteApi, addFavoriteApi, removeFavoriteApi } from '@/api/player'
import { getLyricsApi } from '@/api/music'
import { getCommentsApi, addCommentApi, likeCommentApi, unlikeCommentApi, deleteCommentApi, createFeedbackApi } from '@/api/social'
import { getUserSettingsApi, saveUserSettingApi } from '@/api/settings'
import { playerAudio } from '@/utils/playerAudio'
import { USER_SETTINGS_UPDATED_EVENT } from '@/utils/settingsSync'
import { normalizeLyrics } from '@/utils/lyrics'
import StateBlock from '@/components/StateBlock.vue'
import { ElMessage } from 'element-plus'
import { getDisplaySongTitle } from '@/utils/songTitle'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const store = usePlayerStore()
const user = useUserStore()
const audio = playerAudio
const favorited = ref(false)
const lyrics = ref<any[]>([])
const activeLyric = ref(-1)
const duration = ref(0)
const isSeeking = ref(false)
const loading = ref(false)
const error = ref('')
let seekReleaseTimer: number | undefined
const EQ_FREQUENCIES = [31, 62, 125, 250, 500, 750, 1000, 2000, 4000, 8000, 12000, 16000]
const eqGains = ref<number[]>(EQ_FREQUENCIES.map(() => 0))
const pitchSemitone = ref(0)
const pitchInput = ref('0')
const eqPreset = ref('flat')
const advancedFxPreset = ref('default')
const isApplyingAdvancedFxPreset = ref(false)
const advancedConfigEnabled = ref(false)
const advancedEqVisible = ref(false)
const helpDialogVisible = ref(false)
const helpDialogTitle = ref('')
const helpDialogContent = ref('')
const effectCardRef = ref<HTMLElement | null>(null)
const spectrumCompareCanvasRef = ref<HTMLCanvasElement | null>(null)
const advancedGroupOpen = ref<Record<'master' | 'eq' | 'compressor' | 'reverb' | 'spatial' | 'channel', boolean>>({
  master: true,
  eq: true,
  compressor: true,
  reverb: true,
  spatial: true,
  channel: true
})
const toggleAdvancedGroup = (key: keyof typeof advancedGroupOpen.value) => {
  advancedGroupOpen.value[key] = !advancedGroupOpen.value[key]
}
const compressorEnabled = ref(false)
const compressorThreshold = ref(-24)
const compressorRatio = ref(4)
const compressorAttack = ref(0.003)
const compressorRelease = ref(0.25)
const reverbEnabled = ref(false)
const reverbType = ref<'room' | 'hall' | 'canyon'>('room')
const reverbRoomSize = ref(40)
const reverbDecay = ref(2.2)
const reverbWet = ref(25)
const reverbTypeLabelMap: Record<'room' | 'hall' | 'canyon', string> = {
  room: '房间',
  hall: '大厅',
  canyon: '峡谷'
}
const reverbRoomSceneText = computed(() => {
  const size = reverbRoomSize.value
  if (size <= 25) return '近场小房间（紧凑、贴耳）'
  if (size <= 45) return '标准房间（均衡自然）'
  if (size <= 65) return '中型厅堂（空间明显）'
  if (size <= 85) return '大型空间（尾音延展）'
  return '超大空间（氛围强烈）'
})
const reverbVisualStyle = computed(() => {
  const scale = 0.7 + (reverbRoomSize.value / 100) * 0.8
  const glow = 0.2 + (reverbRoomSize.value / 100) * 0.6
  return {
    transform: `translate(-50%, -50%) scale(${scale.toFixed(3)})`,
    boxShadow: `0 0 0 1px rgba(14, 165, 233, 0.25), 0 0 ${Math.round(20 + glow * 38)}px rgba(56, 189, 248, ${Math.min(0.72, glow).toFixed(2)})`
  }
})
const reverbHearingProfile = computed(() => {
  const type = reverbType.value
  const size = reverbRoomSize.value
  const decay = reverbDecay.value
  const wet = reverbWet.value

  let spatialTone = ''
  let baseFeeling = ''
  if (type === 'room') {
    spatialTone = size <= 35 ? '近场贴耳' : size <= 65 ? '自然围合' : '宽松房间感'
    baseFeeling = '人声更近、尾音偏短，适合突出主体。'
  } else if (type === 'hall') {
    spatialTone = size <= 35 ? '清晰厅堂' : size <= 65 ? '开阔大厅' : '宏大礼堂'
    baseFeeling = '空间更开阔、层次更舒展，适合营造现场感。'
  } else {
    spatialTone = size <= 35 ? '岩壁反射' : size <= 65 ? '峡谷回荡' : '深谷回声'
    baseFeeling = '回声路径更长，尾音拖尾感更明显。'
  }

  const decayTone = decay <= 1.5 ? '尾音收得快' : decay <= 3.2 ? '尾音自然延展' : '尾音拉长明显'
  const wetTone = wet <= 20 ? '直达声占主导' : wet <= 45 ? '直达声与空间感平衡' : '空间染色较强'

  const intimacy = Math.max(0, Math.min(100, Math.round(100 - size * 0.55 - decay * 9 - wet * 0.33)))
  const spaciousness = Math.max(0, Math.min(100, Math.round(size * 0.55 + decay * 8 + wet * 0.45 + (type === 'hall' ? 10 : type === 'canyon' ? 6 : 0))))
  const tail = Math.max(0, Math.min(100, Math.round(decay * 16 + wet * 0.38 + (type === 'canyon' ? 14 : type === 'hall' ? 6 : 0))))

  const headline = `${reverbTypeLabelMap[type]} · ${spatialTone}`
  const summary = `${baseFeeling} 当前：${decayTone}，${wetTone}。`

  return {
    headline,
    summary,
    chips: [
      { key: 'intimacy', label: '贴近感', value: intimacy },
      { key: 'spaciousness', label: '开阔度', value: spaciousness },
      { key: 'tail', label: '回声尾音', value: tail }
    ]
  }
})
const spatialEnabled = ref(false)
const spatialDepth = ref(30)
const spatialWidth = ref(0)
const spatialStageMapRef = ref<HTMLElement | null>(null)
const isDraggingSpatialPoint = ref(false)
const channelBalance = ref(0)
const leftGainDb = ref(0)
const rightGainDb = ref(0)
const phaseInvertLeft = ref(false)
const phaseInvertRight = ref(false)
const monoMerge = ref(false)
const leftDelayMs = ref(0)
const rightDelayMs = ref(0)
const playModeOptions: Array<{ label: string; value: PlayMode }> = [
  { label: '随机播放', value: 'random' },
  { label: '顺序播放', value: 'order' },
  { label: '顺序循环', value: 'loop' },
  { label: '单曲循环', value: 'single' }
]
const eqPresetOptions = [
  { label: '默认', value: 'flat' },
  { label: '关闭', value: 'off' },
  { label: '自定义', value: 'custom' },
  { label: 'HiFi 发烧友', value: 'hifi_audiophile' },
  { label: '现场演出', value: 'live_performance' },
  { label: '人声特写', value: 'vocal_focus' },
  { label: '完美低音', value: 'perfect_bass' },
  { label: '极致摇滚', value: 'rock_extreme' },
  { label: '最毒人声', value: 'vocal_poison' },
  { label: '低音增强', value: 'bass_boost' },
  { label: '流行', value: 'pop' },
  { label: '电子', value: 'electronic' },
  { label: '舞曲', value: 'dance' },
  { label: '爵士', value: 'jazz' },
  { label: '摇滚', value: 'rock' },
  { label: '古典', value: 'classical' },
  { label: '明亮通透', value: 'bright' },
  { label: '影院环绕感', value: 'cinema' },
  { label: '轻柔夜听', value: 'night' }
]
const eqPresetMap: Record<string, number[]> = {
  off: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
  custom: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
  flat: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
  hifi_audiophile: [1, 1, 1, 0, 0, -1, 0, 1, 2, 2, 3, 3],
  live_performance: [4, 4, 3, 1, -1, 0, 1, 2, 3, 4, 5, 4],
  vocal_focus: [-1, -1, 0, 2, 3, 3, 4, 3, 1, -1, -2, -2],
  perfect_bass: [6, 5, 4, 2, -1, -3, 3, 4, 5, 5, 6, 6],
  rock_extreme: [6, 5, 4, 2, 0, -2, -6, 1, 4, 6, 7, 9],
  vocal_poison: [4, 3, 1, 0, 1, 2, 3, 4, 5, 4, 3, 3],
  bass_boost: [6, 5, 4, 3, 2, 1, 0, -1, -2, -2, -2, -2],
  pop: [-1, 1, 3, 4, 5, 3, 1, 0, -1, 1, 2, 2],
  electronic: [5, 4, 3, 2, 1, 0, -1, 1, 3, 5, 6, 6],
  dance: [4, 4, 3, 2, 1, 0, -1, 0, 2, 4, 5, 5],
  jazz: [2, 2, 1, 0, -1, -1, 0, 1, 2, 3, 3, 2],
  rock: [4, 4, 3, 2, 0, -1, -1, 1, 3, 4, 5, 5],
  classical: [3, 2, 1, 0, -1, -1, 0, 1, 2, 3, 4, 4],
  bright: [-2, -1, 0, 1, 2, 2, 1, 2, 3, 4, 5, 5],
  cinema: [3, 3, 2, 1, 0, 0, 0, 1, 3, 4, 5, 4],
  night: [2, 1, 0, -1, -2, -1, 0, 1, 1, 0, -1, -1]
}

const advancedFxPresetOptions = [
  { label: '默认（通用）', value: 'default' },
  { label: '氛围人声', value: 'vocal_ambient' },
  { label: '现场沉浸', value: 'live_immersive' },
  { label: '影院空间', value: 'cinema_space' },
  { label: '自定义', value: 'custom' }
]

const advancedFxPresetConfigs: Record<string, {
  eqPreset: string
  compressorEnabled: boolean
  compressorThreshold: number
  compressorRatio: number
  compressorAttack: number
  compressorRelease: number
  reverbEnabled: boolean
  reverbType: 'room' | 'hall' | 'canyon'
  reverbRoomSize: number
  reverbDecay: number
  reverbWet: number
  spatialEnabled: boolean
  spatialDepth: number
  spatialWidth: number
  channelBalance: number
  leftGainDb: number
  rightGainDb: number
  phaseInvertLeft: boolean
  phaseInvertRight: boolean
  monoMerge: boolean
  leftDelayMs: number
  rightDelayMs: number
}> = {
  default: {
    eqPreset: 'flat',
    compressorEnabled: false,
    compressorThreshold: -24,
    compressorRatio: 4,
    compressorAttack: 0.003,
    compressorRelease: 0.25,
    reverbEnabled: false,
    reverbType: 'room',
    reverbRoomSize: 40,
    reverbDecay: 2.2,
    reverbWet: 25,
    spatialEnabled: false,
    spatialDepth: 30,
    spatialWidth: 0,
    channelBalance: 0,
    leftGainDb: 0,
    rightGainDb: 0,
    phaseInvertLeft: false,
    phaseInvertRight: false,
    monoMerge: false,
    leftDelayMs: 0,
    rightDelayMs: 0
  },
  vocal_ambient: {
    eqPreset: 'vocal_focus',
    compressorEnabled: true,
    compressorThreshold: -28,
    compressorRatio: 3.2,
    compressorAttack: 0.008,
    compressorRelease: 0.24,
    reverbEnabled: true,
    reverbType: 'room',
    reverbRoomSize: 52,
    reverbDecay: 2.7,
    reverbWet: 24,
    spatialEnabled: true,
    spatialDepth: 34,
    spatialWidth: 8,
    channelBalance: 0,
    leftGainDb: 0,
    rightGainDb: 0,
    phaseInvertLeft: false,
    phaseInvertRight: false,
    monoMerge: false,
    leftDelayMs: 0,
    rightDelayMs: 0
  },
  live_immersive: {
    eqPreset: 'live_performance',
    compressorEnabled: true,
    compressorThreshold: -30,
    compressorRatio: 4.8,
    compressorAttack: 0.006,
    compressorRelease: 0.28,
    reverbEnabled: true,
    reverbType: 'hall',
    reverbRoomSize: 64,
    reverbDecay: 3.4,
    reverbWet: 30,
    spatialEnabled: true,
    spatialDepth: 46,
    spatialWidth: 16,
    channelBalance: 0,
    leftGainDb: 0,
    rightGainDb: 0,
    phaseInvertLeft: false,
    phaseInvertRight: false,
    monoMerge: false,
    leftDelayMs: 0,
    rightDelayMs: 0
  },
  cinema_space: {
    eqPreset: 'cinema',
    compressorEnabled: true,
    compressorThreshold: -26,
    compressorRatio: 3.8,
    compressorAttack: 0.01,
    compressorRelease: 0.32,
    reverbEnabled: true,
    reverbType: 'canyon',
    reverbRoomSize: 72,
    reverbDecay: 4.2,
    reverbWet: 35,
    spatialEnabled: true,
    spatialDepth: 52,
    spatialWidth: 22,
    channelBalance: 0,
    leftGainDb: 0,
    rightGainDb: 0,
    phaseInvertLeft: false,
    phaseInvertRight: false,
    monoMerge: false,
    leftDelayMs: 0,
    rightDelayMs: 0
  }
}
let audioCtx: AudioContext | null = null
let mediaSource: MediaElementAudioSourceNode | null = null
let preGainNode: GainNode | null = null
let eqNodes: BiquadFilterNode[] = []
let inputAnalyserNode: AnalyserNode | null = null
let outputAnalyserNode: AnalyserNode | null = null
let compressorNode: DynamicsCompressorNode | null = null
let convolverNode: ConvolverNode | null = null
let reverbWetGainNode: GainNode | null = null
let reverbDryGainNode: GainNode | null = null
let spatialPannerNode: PannerNode | null = null
let channelSplitterNode: ChannelSplitterNode | null = null
let channelMergerNode: ChannelMergerNode | null = null
let leftChannelGainNode: GainNode | null = null
let rightChannelGainNode: GainNode | null = null
let leftDelayNode: DelayNode | null = null
let rightDelayNode: DelayNode | null = null
let spectrumFrame = 0
let spectrumStartRetryTimer: number | undefined
const spectrumInputPeak = ref(0)
const spectrumOutputPeak = ref(0)
let persistTimer: number | undefined
const AUTO_RESUME_ON_RELOAD_KEY = 'music-web:auto-resume-on-reload'
const RELOAD_PROGRESS_SNAPSHOT_KEY = 'music-web:reload-progress-snapshot'
const setAutoResumeOnReload = (playing: boolean) => {
  try {
    window.localStorage.setItem(AUTO_RESUME_ON_RELOAD_KEY, playing ? '1' : '0')
  } catch {
    // ignore storage errors
  }
}
const consumeAutoResumeOnReload = () => {
  try {
    const shouldResume = window.localStorage.getItem(AUTO_RESUME_ON_RELOAD_KEY) === '1'
    window.localStorage.removeItem(AUTO_RESUME_ON_RELOAD_KEY)
    return shouldResume
  } catch {
    return false
  }
}
const isReloadNavigation = () => {
  if (typeof window === 'undefined' || !window.performance?.getEntriesByType) return false
  const nav = window.performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming | undefined
  return nav?.type === 'reload'
}
let shouldResumeAfterReload = false
let pendingResumeSongId = 0
let pendingResumeTime = 0
// Some browsers may emit a `pause` event right before/around `ended`.
// When the song ends naturally and we auto-advance, we don't want that pause
// to flip UI state to "paused" and prevent the next track from auto-playing.
let ignoreNextPause = false
const userPreferredAutoPlayOnOpen = ref<boolean | null>(null)
const shouldAutoplayOnInit = () => {
  if (isReloadNavigation()) return shouldResumeAfterReload
  if (userPreferredAutoPlayOnOpen.value !== null) return userPreferredAutoPlayOnOpen.value
  // 非刷新场景下，仅在原本就是播放态时继续自动播放，避免“暂停后进入播放器又自动播放”
  return !!store.playing
}
const saveReloadProgressSnapshot = () => {
  try {
    const currentSongId = Number(songId.value || store.currentSongId || 0)
    if (!currentSongId) return
    const currentTime = Number.isFinite(audio.currentTime) ? audio.currentTime : Number(store.currentTime || 0)
    const payload = { songId: currentSongId, currentTime: Math.max(currentTime, 0) }
    window.localStorage.setItem(RELOAD_PROGRESS_SNAPSHOT_KEY, JSON.stringify(payload))
  } catch {
    // ignore storage errors
  }
}
const consumeReloadProgressSnapshot = () => {
  try {
    const raw = window.localStorage.getItem(RELOAD_PROGRESS_SNAPSHOT_KEY)
    window.localStorage.removeItem(RELOAD_PROGRESS_SNAPSHOT_KEY)
    if (!raw) return
    const parsed = JSON.parse(raw)
    const parsedSongId = Number(parsed?.songId || 0)
    const parsedCurrentTime = Number(parsed?.currentTime || 0)
    if (parsedSongId > 0 && Number.isFinite(parsedCurrentTime) && parsedCurrentTime >= 0) {
      pendingResumeSongId = parsedSongId
      pendingResumeTime = parsedCurrentTime
    }
  } catch {
    pendingResumeSongId = 0
    pendingResumeTime = 0
  }
}
const applyPendingResumeProgress = () => {
  if (!pendingResumeSongId || pendingResumeSongId !== songId.value) return
  const d = audio.duration
  if (!Number.isFinite(d) || d <= 0) return
  const clamped = Math.min(Math.max(pendingResumeTime, 0), Math.max(d - 0.2, 0))
  audio.currentTime = clamped
  store.currentTime = clamped
  updateActiveLyric(clamped * 1000)
  pendingResumeSongId = 0
  pendingResumeTime = 0
}
const onBeforeUnload = () => {
  setAutoResumeOnReload(!audio.paused)
  saveReloadProgressSnapshot()
}
const gainDb = ref(0)
const attenuateDb = ref(0)
const detailTab = ref<'lyrics' | 'comments'>('lyrics')
const lyricsRefreshing = ref(false)
const lyricFontSize = ref(18)
const lyricAutoScroll = ref(true)
const comments = ref<any[]>([])
const commentsLoading = ref(false)
const commentsError = ref('')
const commentContent = ref('')
const commentSubmitting = ref(false)
const lyricJumpHint = ref('')
const bootstrapping = ref(false)
let lastLoadedSongDetailsId = 0
const feedbackVisible = ref(false)
const feedbackSubmitting = ref(false)
const lyricScrollRef = ref<HTMLElement | null>(null)
const lyricProgrammaticScrolling = ref(false)
const lyricManualPause = ref(false)
let lyricJumpHintTimer: number | undefined
let lyricManualPauseTimer: number | undefined
let lyricProgrammaticReleaseTimer: number | undefined
const commentQuickActions = ['太好听了', '单曲循环中', '歌词很戳我', '已加入收藏']
const feedbackForm = ref({
  type: 'BUG',
  content: '',
  contact: '',
  scene: 'music-web-player'
})

const songId = computed(() => Number(route.params.songId || store.currentSongId || 0))
const song = computed(() => store.currentSong)
const src = computed(() => (songId.value ? `/api/app/music/song/${songId.value}/stream` : ''))
const getSongFileExtension = () => {
  const rawPath = String(song.value?.filePath || song.value?.fileUrl || '').trim()
  if (!rawPath) return 'mp3'
  const cleanPath = rawPath.split('?')[0].split('#')[0]
  const segments = cleanPath.split('.')
  const ext = segments.length > 1 ? segments.pop() : ''
  const normalized = String(ext || '').toLowerCase()
  return /^[a-z0-9]{1,8}$/.test(normalized) ? normalized : 'mp3'
}
const sanitizeDownloadName = (value: string) => value.replace(/[\\/:*?"<>|]/g, '_').trim()
const downloadSong = () => {
  if (!songId.value) {
    ElMessage.warning('当前没有可下载的歌曲')
    return
  }
  const href = src.value
  if (!href) {
    ElMessage.warning('歌曲下载地址不可用')
    return
  }
  const title = sanitizeDownloadName(song.value ? getDisplaySongTitle(song.value) : `song-${songId.value}`)
  const extension = getSongFileExtension()
  const link = document.createElement('a')
  link.href = href
  link.download = `${title || `song-${songId.value}`}.${extension}`
  link.rel = 'noopener'
  document.body.appendChild(link)
  link.click()
  link.remove()
}
const onLoadedMetadata = () => {
  const d = audio.duration
  duration.value = Number.isFinite(d) && d > 0 ? d : 0
  applyPendingResumeProgress()
}
const onDurationChange = () => {
  const d = audio.duration
  duration.value = Number.isFinite(d) && d > 0 ? d : 0
  applyPendingResumeProgress()
}
const onTimeUpdate = () => {
  if (isSeeking.value) return
  store.currentTime = audio.currentTime
  updateActiveLyric(audio.currentTime * 1000)
}
const onAudioEnded = async () => {
  ignoreNextPause = true
  // Ensure auto-advance keeps "playing" state so src-watch can call audio.play().
  store.playing = true
  const shouldContinue = await store.nextByMode()
  if (!shouldContinue) {
    store.currentTime = 0
    store.playing = false
    ignoreNextPause = false
    return
  }
  try {
    if (store.playMode === 'single') {
      audio.currentTime = 0
      store.currentTime = 0
      await ensureAudioContext()
      await audio.play()
      store.playing = true
      return
    }

    // Non-single modes: wait for the src watcher / route update to swap stream,
    // then explicitly trigger play to avoid "switched but not started".
    await nextTick()
    await new Promise<void>((r) => window.setTimeout(() => r(), 0))
    await ensureAudioContext()
    await audio.play()
    store.playing = true
  } catch {
    // If it fails (e.g. autoplay restrictions), fall back to actual audio state.
    store.playing = !audio.paused
  } finally {
    ignoreNextPause = false
  }
}

const onAudioPlay = async () => {
  store.playing = true
  ignoreNextPause = false
  setAutoResumeOnReload(true)
  try {
    await ensureAudioContext()
  } catch {
    // ignore
  }
  if (advancedEqVisible.value) {
    await nextTick()
    startSpectrumLoop()
  }
}

const onAudioPause = () => {
  if (ignoreNextPause) return
  store.playing = false
  setAutoResumeOnReload(false)
}

const formatTime = (seconds: number) => {
  if (!Number.isFinite(seconds) || seconds < 0) return '00:00'
  const s = Math.floor(seconds)
  const mm = Math.floor(s / 60)
  const ss = s % 60
  return `${String(mm).padStart(2, '0')}:${String(ss).padStart(2, '0')}`
}

const updateActiveLyric = (ms: number) => {
  activeLyric.value = lyrics.value.reduce((acc, item, index) => (ms >= item.time ? index : acc), -1)
}

const dbToGain = (db: number) => Math.pow(10, db / 20)
const clamp = (value: number, min: number, max: number) => Math.min(max, Math.max(min, value))

const helpDocs: Record<string, { title: string; content: string }> = {
  eq: {
    title: '12路均衡器与实时频谱',
    content:
      '12路均衡器用于分别增强或削弱不同频段。高级页会显示处理前与处理后的频谱曲线，帮助你直观看到每个频段调节后的变化。'
  },
  compressor: {
    title: '专业压缩器',
    content:
      '压缩器会压低过高峰值并提升整体可听度。阈值越高越不容易触发，压缩比越大压得越明显；Attack 决定启动速度，Release 决定恢复速度。'
  },
  reverb: {
    title: '混响器',
    content:
      '混响模拟不同空间听感。房间大小越大、衰减时间越长，尾音越明显；干湿比用于平衡原始声音和混响声音。'
  },
  spatial: {
    title: '空间音频与虚拟声场',
    content:
      '空间音频通过 HRTF 方式模拟声源位置。深度用于控制声源前后距离，宽度用于左右声像偏移，让耳机听感更具空间感。'
  },
  channel: {
    title: '声道与平衡控制',
    content:
      '可调节左右声道平衡与独立增益，支持左右相位反转、L+R合并（单声道）与左右独立延时，用于修正听感或做声场微调。'
  }
}

const openHelpDialog = (key: keyof typeof helpDocs) => {
  const doc = helpDocs[key]
  helpDialogTitle.value = doc.title
  helpDialogContent.value = doc.content
  helpDialogVisible.value = true
}

const getAdvancedTransitionState = (shell: HTMLElement) => {
  const host = effectCardRef.value as any
  const fromEl = (host?.$el as HTMLElement | undefined) || (host as HTMLElement | null)
  const from = fromEl?.getBoundingClientRect()
  const to = shell.getBoundingClientRect()
  if (!from || !to.width || !to.height) return null
  return {
    dx: from.left - to.left,
    dy: from.top - to.top,
    sx: clamp(from.width / to.width, 0.08, 1),
    sy: clamp(from.height / to.height, 0.08, 1),
    radius: 14
  }
}

const onAdvancedOverlayBeforeEnter = (el: Element) => {
  const overlay = el as HTMLElement
  const shell = overlay.querySelector('.advanced-eq-shell') as HTMLElement | null
  if (!shell) return
  const state = getAdvancedTransitionState(shell)
  overlay.style.opacity = '0'
  shell.style.transformOrigin = 'top left'
  shell.style.transition = 'none'
  shell.style.willChange = 'transform, border-radius, opacity'
  if (state) {
    shell.style.transform = `translate(${state.dx}px, ${state.dy}px) scale(${state.sx}, ${state.sy})`
    shell.style.borderRadius = `${state.radius}px`
    shell.style.opacity = '0.88'
  }
}

const onAdvancedOverlayEnter = (el: Element, done: () => void) => {
  const overlay = el as HTMLElement
  const shell = overlay.querySelector('.advanced-eq-shell') as HTMLElement | null
  if (!shell) return done()
  shell.offsetHeight
  overlay.style.transition = 'opacity 260ms ease'
  shell.style.transition = 'transform 320ms cubic-bezier(0.2, 0.8, 0.2, 1), border-radius 320ms ease, opacity 240ms ease'
  requestAnimationFrame(() => {
    overlay.style.opacity = '1'
    shell.style.transform = 'translate(0, 0) scale(1, 1)'
    shell.style.borderRadius = '18px'
    shell.style.opacity = '1'
  })
  window.setTimeout(done, 340)
}

const onAdvancedOverlayAfterEnter = (el: Element) => {
  const overlay = el as HTMLElement
  const shell = overlay.querySelector('.advanced-eq-shell') as HTMLElement | null
  overlay.style.transition = ''
  overlay.style.opacity = ''
  if (!shell) return
  shell.style.transition = ''
  shell.style.willChange = ''
  startSpectrumLoop()
}

const onAdvancedOverlayBeforeLeave = (el: Element) => {
  const overlay = el as HTMLElement
  const shell = overlay.querySelector('.advanced-eq-shell') as HTMLElement | null
  if (!shell) return
  overlay.style.opacity = '1'
  shell.style.transformOrigin = 'top left'
  shell.style.transition = 'none'
  shell.style.willChange = 'transform, border-radius, opacity'
}

const onAdvancedOverlayLeave = (el: Element, done: () => void) => {
  const overlay = el as HTMLElement
  const shell = overlay.querySelector('.advanced-eq-shell') as HTMLElement | null
  if (!shell) return done()
  const state = getAdvancedTransitionState(shell)
  overlay.style.transition = 'opacity 240ms ease'
  shell.style.transition = 'transform 280ms cubic-bezier(0.3, 0.1, 0.7, 0.2), border-radius 260ms ease, opacity 220ms ease'
  requestAnimationFrame(() => {
    overlay.style.opacity = '0'
    if (state) {
      shell.style.transform = `translate(${state.dx}px, ${state.dy}px) scale(${state.sx}, ${state.sy})`
      shell.style.borderRadius = `${state.radius}px`
      shell.style.opacity = '0.82'
    } else {
      shell.style.transform = 'scale(0.9)'
      shell.style.opacity = '0'
    }
  })
  window.setTimeout(done, 300)
}

const onAdvancedOverlayAfterLeave = (el: Element) => {
  const overlay = el as HTMLElement
  const shell = overlay.querySelector('.advanced-eq-shell') as HTMLElement | null
  overlay.style.transition = ''
  overlay.style.opacity = ''
  if (!shell) return
  shell.style.transition = ''
  shell.style.transform = ''
  shell.style.borderRadius = ''
  shell.style.opacity = ''
  shell.style.willChange = ''
}

const createImpulseResponse = (seconds: number, roomFactor: number) => {
  if (!audioCtx) return null
  const sampleRate = audioCtx.sampleRate
  const length = Math.max(1, Math.floor(sampleRate * seconds))
  const impulse = audioCtx.createBuffer(2, length, sampleRate)
  for (let channel = 0; channel < 2; channel += 1) {
    const data = impulse.getChannelData(channel)
    for (let i = 0; i < length; i += 1) {
      const t = i / length
      const decay = Math.pow(1 - t, clamp(roomFactor, 0.4, 3))
      data[i] = (Math.random() * 2 - 1) * decay
    }
  }
  return impulse
}

const applyCompressorSettings = () => {
  if (!compressorNode) return
  if (!compressorEnabled.value) {
    compressorNode.threshold.value = 0
    compressorNode.ratio.value = 1
    compressorNode.attack.value = 0.003
    compressorNode.release.value = 0.1
    return
  }
  compressorNode.threshold.value = compressorThreshold.value
  compressorNode.ratio.value = compressorRatio.value
  compressorNode.attack.value = compressorAttack.value
  compressorNode.release.value = compressorRelease.value
}

const applyReverbSettings = () => {
  if (!convolverNode || !reverbWetGainNode || !reverbDryGainNode) return
  const typeFactorMap: Record<'room' | 'hall' | 'canyon', number> = {
    room: 1,
    hall: 1.6,
    canyon: 2.2
  }
  const factor = typeFactorMap[reverbType.value] || 1
  if (reverbEnabled.value) {
    convolverNode.buffer = createImpulseResponse(reverbDecay.value * factor, reverbRoomSize.value / 26)
    const wet = clamp(reverbWet.value / 100, 0, 1)
    reverbWetGainNode.gain.value = wet
    reverbDryGainNode.gain.value = 1 - wet * 0.6
  } else {
    reverbWetGainNode.gain.value = 0
    reverbDryGainNode.gain.value = 1
  }
}

const applySpatialSettings = () => {
  if (!spatialPannerNode) return
  const zDepth = spatialEnabled.value ? -0.6 - spatialDepth.value / 40 : -0.3
  const xWidth = spatialEnabled.value ? clamp(spatialWidth.value / 25, -1.4, 1.4) : 0
  spatialPannerNode.panningModel = 'HRTF'
  spatialPannerNode.distanceModel = 'inverse'
  spatialPannerNode.positionX.value = xWidth
  spatialPannerNode.positionY.value = 0
  spatialPannerNode.positionZ.value = zDepth
}

const getSpatialPointerPos = (clientX: number, clientY: number) => {
  const el = spatialStageMapRef.value
  if (!el) return null
  const rect = el.getBoundingClientRect()
  if (!rect.width || !rect.height) return null
  const px = clamp((clientX - rect.left) / rect.width, 0, 1)
  const py = clamp((clientY - rect.top) / rect.height, 0, 1)
  return { px, py }
}

const updateSpatialByPointer = (clientX: number, clientY: number) => {
  const pos = getSpatialPointerPos(clientX, clientY)
  if (!pos) return
  spatialWidth.value = Math.round(pos.px * 200 - 100)
  spatialDepth.value = Math.round((1 - pos.py) * 100)
}

const onSpatialPointDown = (event: MouseEvent) => {
  if (!advancedConfigEnabled.value || !spatialEnabled.value) return
  isDraggingSpatialPoint.value = true
  updateSpatialByPointer(event.clientX, event.clientY)
}

const onSpatialMapDown = (event: MouseEvent) => {
  if (!advancedConfigEnabled.value || !spatialEnabled.value) return
  isDraggingSpatialPoint.value = true
  updateSpatialByPointer(event.clientX, event.clientY)
}

const onSpatialPointerMove = (event: PointerEvent) => {
  if (!isDraggingSpatialPoint.value) return
  updateSpatialByPointer(event.clientX, event.clientY)
}

const resetSpatialPosition = () => {
  spatialWidth.value = 0
  spatialDepth.value = 30
}

const onSpatialMapDoubleClick = () => {
  if (!advancedConfigEnabled.value || !spatialEnabled.value) return
  resetSpatialPosition()
}

const onSpatialMapContextMenu = (event: MouseEvent) => {
  event.preventDefault()
  if (!advancedConfigEnabled.value || !spatialEnabled.value) return
  resetSpatialPosition()
}

const stopSpatialDragging = () => {
  isDraggingSpatialPoint.value = false
}

const reconnectChannelFlow = () => {
  if (
    !channelSplitterNode ||
    !channelMergerNode ||
    !leftChannelGainNode ||
    !rightChannelGainNode ||
    !leftDelayNode ||
    !rightDelayNode
  ) {
    return
  }
  channelSplitterNode.disconnect()
  leftChannelGainNode.disconnect()
  rightChannelGainNode.disconnect()
  leftDelayNode.disconnect()
  rightDelayNode.disconnect()
  if (monoMerge.value) {
    channelSplitterNode.connect(leftChannelGainNode, 0)
    channelSplitterNode.connect(leftChannelGainNode, 1)
    channelSplitterNode.connect(rightChannelGainNode, 0)
    channelSplitterNode.connect(rightChannelGainNode, 1)
  } else {
    channelSplitterNode.connect(leftChannelGainNode, 0)
    channelSplitterNode.connect(rightChannelGainNode, 1)
  }
  leftChannelGainNode.connect(leftDelayNode)
  rightChannelGainNode.connect(rightDelayNode)
  leftDelayNode.connect(channelMergerNode, 0, 0)
  rightDelayNode.connect(channelMergerNode, 0, 1)
}

const applyChannelSettings = () => {
  if (!leftChannelGainNode || !rightChannelGainNode || !leftDelayNode || !rightDelayNode) return
  reconnectChannelFlow()
  const pan = clamp(channelBalance.value / 100, -1, 1)
  const leftPanGain = pan > 0 ? 1 - pan : 1
  const rightPanGain = pan < 0 ? 1 + pan : 1
  const lGain = dbToGain(leftGainDb.value) * leftPanGain * (phaseInvertLeft.value ? -1 : 1)
  const rGain = dbToGain(rightGainDb.value) * rightPanGain * (phaseInvertRight.value ? -1 : 1)
  leftChannelGainNode.gain.value = monoMerge.value ? (lGain + rGain) * 0.5 : lGain
  rightChannelGainNode.gain.value = monoMerge.value ? (lGain + rGain) * 0.5 : rGain
  leftDelayNode.delayTime.value = clamp(leftDelayMs.value / 1000, 0, 0.2)
  rightDelayNode.delayTime.value = clamp(rightDelayMs.value / 1000, 0, 0.2)
}

const METRICS_PANEL_WIDTH = 220

const getCanvasCssSize = (canvas: HTMLCanvasElement) => ({
  width: Math.max(1, Math.floor(canvas.clientWidth || canvas.width || 1)),
  height: Math.max(1, Math.floor(canvas.clientHeight || canvas.height || 1))
})

const drawSpectrumGuides = (ctx: CanvasRenderingContext2D, analyser: AnalyserNode, drawingWidth?: number) => {
  const { width, height } = getCanvasCssSize(ctx.canvas)
  const renderWidth = Math.max(1, Math.min(drawingWidth ?? width, width))
  const nyquist = analyser.context.sampleRate / 2
  const minFreq = 20
  const usableMin = Math.log(minFreq)
  const usableRange = Math.log(nyquist) - usableMin
  ctx.save()
  for (let i = 0; i < EQ_FREQUENCIES.length; i += 1) {
    const freq = EQ_FREQUENCIES[i]
    const logPos = clamp((Math.log(freq) - usableMin) / usableRange, 0, 1)
    const x = logPos * renderWidth
    ctx.strokeStyle = 'rgba(148, 163, 184, 0.32)'
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.moveTo(x, 0)
    ctx.lineTo(x, height)
    ctx.stroke()
    ctx.fillStyle = 'rgba(203, 213, 225, 0.9)'
    ctx.font = '10px sans-serif'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'bottom'
    const label = freq >= 1000 ? `${freq / 1000}k` : `${freq}`
    ctx.fillText(label, x, height - 2)
  }
  ctx.restore()
}

const drawSpectrum = (
  ctx: CanvasRenderingContext2D,
  analyser: AnalyserNode,
  color: string,
  lineWidth = 2,
  drawingWidth?: number
) => {
  const { width, height } = getCanvasCssSize(ctx.canvas)
  const renderWidth = Math.max(1, Math.min(drawingWidth ?? width, width))
  const data = new Uint8Array(analyser.frequencyBinCount)
  analyser.getByteFrequencyData(data)
  let peak = 0
  for (let i = 0; i < data.length; i += 1) {
    if (data[i] > peak) peak = data[i]
  }
  ctx.lineWidth = lineWidth
  ctx.strokeStyle = color
  ctx.beginPath()
  for (let i = 0; i < data.length; i += 1) {
    const x = (i / (data.length - 1)) * renderWidth
    const y = height - (data[i] / 255) * (height - 6)
    if (i === 0) ctx.moveTo(x, y)
    else ctx.lineTo(x, y)
  }
  ctx.stroke()
  return peak / 255
}

const resizeSpectrumCanvas = (canvas: HTMLCanvasElement) => {
  const dpr = window.devicePixelRatio || 1
  const rect = canvas.getBoundingClientRect()
  const width = Math.max(420, Math.floor(rect.width || canvas.clientWidth || 880))
  const height = Math.max(220, Math.floor(rect.height || canvas.clientHeight || 260))
  const targetWidth = Math.floor(width * dpr)
  const targetHeight = Math.floor(height * dpr)
  if (canvas.width !== targetWidth || canvas.height !== targetHeight) {
    canvas.width = targetWidth
    canvas.height = targetHeight
  }
  const ctx = canvas.getContext('2d')
  if (ctx) ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
}

const startSpectrumLoop = () => {
  if (!inputAnalyserNode || !outputAnalyserNode) return
  const spectrumCanvas = spectrumCompareCanvasRef.value
  if (!spectrumCanvas) return
  resizeSpectrumCanvas(spectrumCanvas)
  const spectrumCtx = spectrumCanvas.getContext('2d')
  if (!spectrumCtx) return
  const canvasWidth = spectrumCanvas.clientWidth || 0
  const canvasHeight = spectrumCanvas.clientHeight || 0
  if (canvasWidth <= 2 || canvasHeight <= 2) {
    if (spectrumStartRetryTimer) window.clearTimeout(spectrumStartRetryTimer)
    spectrumStartRetryTimer = window.setTimeout(() => {
      spectrumStartRetryTimer = undefined
      startSpectrumLoop()
    }, 80)
    return
  }

  const drawSignalMetrics = (ctx: CanvasRenderingContext2D, width: number, inPeak: number, outPeak: number) => {
    const x = Math.max(width - METRICS_PANEL_WIDTH - 10, 8)
    const y = 10
    const w = Math.min(METRICS_PANEL_WIDTH, Math.max(width - 16, 160))
    const h = 74
    ctx.fillStyle = 'rgba(255, 255, 255, 0.9)'
    ctx.fillRect(x, y, w, h)
    ctx.strokeStyle = 'rgba(148, 163, 184, 0.4)'
    ctx.strokeRect(x, y, w, h)

    const mkBar = (label: string, value: number, color: string, row: number) => {
      const by = y + 14 + row * 27
      const barX = x + 10
      const barW = Math.max(w - 78, 72)
      ctx.fillStyle = 'rgba(51, 65, 85, 0.92)'
      ctx.font = '11px sans-serif'
      ctx.fillText(label, barX, by)
      ctx.fillStyle = 'rgba(226, 232, 240, 0.95)'
      ctx.fillRect(barX, by + 5, barW, 8)
      ctx.fillStyle = color
      ctx.fillRect(barX, by + 5, Math.round(barW * clamp(value, 0, 1)), 8)
      ctx.fillStyle = 'rgba(30, 41, 59, 0.9)'
      ctx.fillText(`${Math.round(clamp(value, 0, 1) * 100)}%`, x + w - 52, by + 11)
    }

    mkBar('Pre', inPeak, 'rgba(148, 163, 184, 0.95)', 0)
    mkBar('Post', outPeak, 'rgba(56, 189, 248, 1)', 1)
  }

  const render = () => {
    const sw = spectrumCanvas.clientWidth || spectrumCanvas.width
    const sh = spectrumCanvas.clientHeight || spectrumCanvas.height
    const spectrumPlotWidth = Math.max(sw - 8, 1)
    spectrumCtx.clearRect(0, 0, sw, sh)
    spectrumCtx.fillStyle = 'rgba(248, 250, 252, 0.96)'
    spectrumCtx.fillRect(0, 0, sw, sh)
    drawSpectrumGuides(spectrumCtx, inputAnalyserNode!, spectrumPlotWidth)
    spectrumInputPeak.value = drawSpectrum(spectrumCtx, inputAnalyserNode!, 'rgba(148, 163, 184, 0.95)', 1.8, spectrumPlotWidth)
    spectrumOutputPeak.value = drawSpectrum(spectrumCtx, outputAnalyserNode!, 'rgba(56, 189, 248, 1)', 2.4, spectrumPlotWidth)
    drawSignalMetrics(spectrumCtx, Math.min(sw, METRICS_PANEL_WIDTH + 20), spectrumInputPeak.value, spectrumOutputPeak.value)

    if (spectrumNoSignalReason.value) {
      const w = spectrumCtx.canvas.clientWidth || spectrumCtx.canvas.width
      const h = spectrumCtx.canvas.clientHeight || spectrumCtx.canvas.height
      spectrumCtx.fillStyle = 'rgba(248, 250, 252, 0.86)'
      spectrumCtx.fillRect(0, 0, w, h)
      spectrumCtx.fillStyle = 'rgba(51, 65, 85, 0.96)'
      spectrumCtx.font = '600 14px sans-serif'
      spectrumCtx.textAlign = 'center'
      spectrumCtx.textBaseline = 'middle'
      spectrumCtx.fillText('暂无有效信号', w / 2, h / 2 - 12)
      spectrumCtx.fillStyle = 'rgba(100, 116, 139, 0.96)'
      spectrumCtx.font = '12px sans-serif'
      spectrumCtx.fillText(spectrumNoSignalReason.value, w / 2, h / 2 + 12)
      spectrumCtx.textAlign = 'start'
      spectrumCtx.textBaseline = 'alphabetic'
    }

    spectrumFrame = window.requestAnimationFrame(render)
  }
  if (spectrumFrame) cancelAnimationFrame(spectrumFrame)
  render()
}

const ensureAudioContext = async () => {
  if (!window.AudioContext && !(window as any).webkitAudioContext) return
  if (!audioCtx) {
    const Ctx = window.AudioContext || (window as any).webkitAudioContext
    audioCtx = new Ctx()
    mediaSource = audioCtx.createMediaElementSource(audio)
    inputAnalyserNode = audioCtx.createAnalyser()
    inputAnalyserNode.fftSize = 2048
    inputAnalyserNode.smoothingTimeConstant = 0.78
    outputAnalyserNode = audioCtx.createAnalyser()
    outputAnalyserNode.fftSize = 2048
    outputAnalyserNode.smoothingTimeConstant = 0.78
    preGainNode = audioCtx.createGain()
    preGainNode.gain.value = 1
    compressorNode = audioCtx.createDynamicsCompressor()
    convolverNode = audioCtx.createConvolver()
    reverbWetGainNode = audioCtx.createGain()
    reverbDryGainNode = audioCtx.createGain()
    spatialPannerNode = audioCtx.createPanner()
    channelSplitterNode = audioCtx.createChannelSplitter(2)
    channelMergerNode = audioCtx.createChannelMerger(2)
    leftChannelGainNode = audioCtx.createGain()
    rightChannelGainNode = audioCtx.createGain()
    leftDelayNode = audioCtx.createDelay(0.2)
    rightDelayNode = audioCtx.createDelay(0.2)
    eqNodes = EQ_FREQUENCIES.map((freq, index) => {
      const node = audioCtx!.createBiquadFilter()
      node.type = 'peaking'
      node.frequency.value = freq
      node.Q.value = 1.2
      node.gain.value = eqGains.value[index]
      return node
    })
    mediaSource.connect(inputAnalyserNode)
    inputAnalyserNode.connect(preGainNode)
    preGainNode.connect(eqNodes[0])
    for (let i = 0; i < eqNodes.length - 1; i += 1) {
      eqNodes[i].connect(eqNodes[i + 1])
    }
    eqNodes[eqNodes.length - 1].connect(compressorNode)
    compressorNode.connect(reverbDryGainNode)
    compressorNode.connect(convolverNode)
    convolverNode.connect(reverbWetGainNode)
    reverbDryGainNode.connect(spatialPannerNode)
    reverbWetGainNode.connect(spatialPannerNode)
    spatialPannerNode.connect(channelSplitterNode)
    reconnectChannelFlow()
    channelMergerNode.connect(outputAnalyserNode)
    outputAnalyserNode.connect(audioCtx.destination)
    applyCompressorSettings()
    applyReverbSettings()
    applySpatialSettings()
    applyChannelSettings()
    startSpectrumLoop()
  }
  if (audioCtx.state === 'suspended') {
    await audioCtx.resume()
  }
}

const applyPitch = () => {
  ;(audio as any).preservesPitch = false
  ;(audio as any).mozPreservesPitch = false
  ;(audio as any).webkitPreservesPitch = false
  audio.playbackRate = Math.pow(2, pitchSemitone.value / 12)
}

const applyMasterGain = () => {
  const mixedDb = gainDb.value - attenuateDb.value
  if (preGainNode) {
    preGainNode.gain.value = dbToGain(mixedDb)
  }
}

const applyEqGains = () => {
  eqNodes.forEach((node, index) => {
    node.gain.value = eqGains.value[index]
  })
}

const applyEqPreset = (preset: string) => {
  const values = eqPresetMap[preset] || eqPresetMap.flat
  eqGains.value = [...values]
  applyEqGains()
}

const loadComments = async (targetSongId = songId.value) => {
  if (!targetSongId) {
    comments.value = []
    commentsError.value = ''
    commentsLoading.value = false
    return
  }
  commentsLoading.value = true
  commentsError.value = ''
  try {
    const page = await getCommentsApi(targetSongId, 1, 50)
    comments.value = page?.records || []
  } catch (e: any) {
    commentsError.value = e?.message || '评论加载失败'
  } finally {
    commentsLoading.value = false
  }
}

const EFFECT_SETTINGS_KEY = 'player.equalizer.config'
const EFFECT_SETTINGS_STORAGE_PREFIX = `music-web:${EFFECT_SETTINGS_KEY}:`

type EffectSettingsPayload = ReturnType<typeof buildEffectSettingsPayload>
type EffectSettingsRecord = {
  version: number
  updatedAt: number
  payload: EffectSettingsPayload
}

const getEffectSettingsStorageKey = () => {
  const info = (user.userInfo || {}) as any
  const uid = info?.id || info?.userId || info?.username || info?.email || 'guest'
  return `${EFFECT_SETTINGS_STORAGE_PREFIX}${String(uid)}`
}

const buildEffectSettingsPayload = () => ({
  eqPreset: eqPreset.value,
  advancedFxPreset: advancedFxPreset.value,
  advancedConfigEnabled: advancedConfigEnabled.value,
  eqGains: [...eqGains.value],
  pitchSemitone: pitchSemitone.value,
  gainDb: gainDb.value,
  attenuateDb: attenuateDb.value,
  compressorEnabled: compressorEnabled.value,
  compressorThreshold: compressorThreshold.value,
  compressorRatio: compressorRatio.value,
  compressorAttack: compressorAttack.value,
  compressorRelease: compressorRelease.value,
  reverbEnabled: reverbEnabled.value,
  reverbType: reverbType.value,
  reverbRoomSize: reverbRoomSize.value,
  reverbDecay: reverbDecay.value,
  reverbWet: reverbWet.value,
  spatialEnabled: spatialEnabled.value,
  spatialDepth: spatialDepth.value,
  spatialWidth: spatialWidth.value,
  channelBalance: channelBalance.value,
  leftGainDb: leftGainDb.value,
  rightGainDb: rightGainDb.value,
  phaseInvertLeft: phaseInvertLeft.value,
  phaseInvertRight: phaseInvertRight.value,
  monoMerge: monoMerge.value,
  leftDelayMs: leftDelayMs.value,
  rightDelayMs: rightDelayMs.value
})

const normalizeEffectSettingsRecord = (input: unknown): EffectSettingsRecord | null => {
  if (!input || typeof input !== 'object') return null
  const raw = input as any
  const payload = raw?.payload && typeof raw.payload === 'object' ? raw.payload : raw
  if (!payload || typeof payload !== 'object') return null

  const v = Number(raw?.version)
  const t = Number(raw?.updatedAt)
  return {
    version: Number.isFinite(v) && v > 0 ? Math.floor(v) : 1,
    updatedAt: Number.isFinite(t) && t > 0 ? Math.floor(t) : Date.now(),
    payload: payload as EffectSettingsPayload
  }
}

const readLocalEffectSettingsRecord = (): EffectSettingsRecord | null => {
  try {
    const raw = window.localStorage.getItem(getEffectSettingsStorageKey())
    if (!raw) return null
    return normalizeEffectSettingsRecord(JSON.parse(raw))
  } catch {
    return null
  }
}

const writeLocalEffectSettingsRecord = (record: EffectSettingsRecord) => {
  try {
    window.localStorage.setItem(getEffectSettingsStorageKey(), JSON.stringify(record))
  } catch {
    // ignore local persistence errors
  }
}

const pickNewerEffectSettingsRecord = (
  a: EffectSettingsRecord | null,
  b: EffectSettingsRecord | null
): EffectSettingsRecord | null => {
  if (!a) return b
  if (!b) return a
  if (a.version !== b.version) return a.version > b.version ? a : b
  if (a.updatedAt !== b.updatedAt) return a.updatedAt > b.updatedAt ? a : b
  return a
}

const applyEffectSettings = (parsed: any) => {
  isApplyingAdvancedFxPreset.value = true
  try {
    if (Array.isArray(parsed?.eqGains) && parsed.eqGains.length === EQ_FREQUENCIES.length) {
      eqGains.value = parsed.eqGains.map((n: unknown) => Number(n) || 0)
      applyEqGains()
    }
    if (typeof parsed?.eqPreset === 'string') eqPreset.value = parsed.eqPreset
    if (typeof parsed?.advancedFxPreset === 'string') advancedFxPreset.value = parsed.advancedFxPreset
    if (typeof parsed?.advancedConfigEnabled === 'boolean') advancedConfigEnabled.value = parsed.advancedConfigEnabled
    if (Number.isFinite(Number(parsed?.pitchSemitone))) pitchSemitone.value = Number(parsed.pitchSemitone)
    if (Number.isFinite(Number(parsed?.gainDb))) gainDb.value = Number(parsed.gainDb)
    if (Number.isFinite(Number(parsed?.attenuateDb))) attenuateDb.value = Number(parsed.attenuateDb)
    if (typeof parsed?.compressorEnabled === 'boolean') compressorEnabled.value = parsed.compressorEnabled
    if (Number.isFinite(Number(parsed?.compressorThreshold))) compressorThreshold.value = Number(parsed.compressorThreshold)
    if (Number.isFinite(Number(parsed?.compressorRatio))) compressorRatio.value = Number(parsed.compressorRatio)
    if (Number.isFinite(Number(parsed?.compressorAttack))) compressorAttack.value = Number(parsed.compressorAttack)
    if (Number.isFinite(Number(parsed?.compressorRelease))) compressorRelease.value = Number(parsed.compressorRelease)
    if (typeof parsed?.reverbEnabled === 'boolean') reverbEnabled.value = parsed.reverbEnabled
    if (['room', 'hall', 'canyon'].includes(parsed?.reverbType)) reverbType.value = parsed.reverbType
    if (Number.isFinite(Number(parsed?.reverbRoomSize))) reverbRoomSize.value = Number(parsed.reverbRoomSize)
    if (Number.isFinite(Number(parsed?.reverbDecay))) reverbDecay.value = Number(parsed.reverbDecay)
    if (Number.isFinite(Number(parsed?.reverbWet))) reverbWet.value = Number(parsed.reverbWet)
    if (typeof parsed?.spatialEnabled === 'boolean') spatialEnabled.value = parsed.spatialEnabled
    if (Number.isFinite(Number(parsed?.spatialDepth))) spatialDepth.value = Number(parsed.spatialDepth)
    if (Number.isFinite(Number(parsed?.spatialWidth))) spatialWidth.value = Number(parsed.spatialWidth)
    if (Number.isFinite(Number(parsed?.channelBalance))) channelBalance.value = Number(parsed.channelBalance)
    if (Number.isFinite(Number(parsed?.leftGainDb))) leftGainDb.value = Number(parsed.leftGainDb)
    if (Number.isFinite(Number(parsed?.rightGainDb))) rightGainDb.value = Number(parsed.rightGainDb)
    if (typeof parsed?.phaseInvertLeft === 'boolean') phaseInvertLeft.value = parsed.phaseInvertLeft
    if (typeof parsed?.phaseInvertRight === 'boolean') phaseInvertRight.value = parsed.phaseInvertRight
    if (typeof parsed?.monoMerge === 'boolean') monoMerge.value = parsed.monoMerge
    if (Number.isFinite(Number(parsed?.leftDelayMs))) leftDelayMs.value = Number(parsed.leftDelayMs)
    if (Number.isFinite(Number(parsed?.rightDelayMs))) rightDelayMs.value = Number(parsed.rightDelayMs)
    applyPitch()
    applyMasterGain()
    applyCompressorSettings()
    applyReverbSettings()
    applySpatialSettings()
    applyChannelSettings()
  } finally {
    isApplyingAdvancedFxPreset.value = false
  }
}

const persistEffectSettings = () => {
  if (persistTimer) window.clearTimeout(persistTimer)
  persistTimer = window.setTimeout(async () => {
    const now = Date.now()
    const localCurrent = readLocalEffectSettingsRecord()
    const nextRecord: EffectSettingsRecord = {
      version: Math.max(1, (localCurrent?.version || 0) + 1),
      updatedAt: Math.max(now, (localCurrent?.updatedAt || 0) + 1),
      payload: buildEffectSettingsPayload()
    }

    writeLocalEffectSettingsRecord(nextRecord)

    if (!user.isLoggedIn) return

    try {
      await saveUserSettingApi(EFFECT_SETTINGS_KEY, JSON.stringify(nextRecord), 'json', '播放器效果器设置')
    } catch {
      // ignore remote persistence errors for smoother UX
    }
  }, 400)
}

const loadEffectSettings = async () => {
  const localRecord = readLocalEffectSettingsRecord()
  let remoteRecord: EffectSettingsRecord | null = null

  if (user.isLoggedIn) {
    try {
      const settings = await getUserSettingsApi()
      const row = (settings || []).find((item: any) => item.settingKey === EFFECT_SETTINGS_KEY)
      if (row?.settingValue) {
        remoteRecord = normalizeEffectSettingsRecord(JSON.parse(row.settingValue))
      }
    } catch {
      // ignore network/parse errors, fallback to local record
    }
  }

  const winner = pickNewerEffectSettingsRecord(remoteRecord, localRecord)
  if (!winner) return

  applyEffectSettings(winner.payload)
  writeLocalEffectSettingsRecord(winner)

  if (user.isLoggedIn && winner === localRecord) {
    try {
      await saveUserSettingApi(EFFECT_SETTINGS_KEY, JSON.stringify(winner), 'json', '播放器效果器设置')
    } catch {
      // ignore sync-back errors
    }
  }
}

const applySimpleUserSettings = (rows: Array<{ settingKey?: string; settingValue?: string }>) => {
  const map = new Map<string, string>()
  ;(rows || []).forEach((item) => {
    const key = String(item?.settingKey || '').trim()
    if (!key) return
    map.set(key, String(item?.settingValue ?? '').trim())
  })

  if (map.has('player.volume')) {
    const volumeRaw = Number(map.get('player.volume') || '70')
    const volumePercent = clamp(Number.isFinite(volumeRaw) ? volumeRaw : 70, 0, 100)
    audio.volume = volumePercent / 100
  }

  if (map.has('player.playMode')) {
    const playModeRaw = String(map.get('player.playMode') || 'loop')
    const validMode = ['random', 'order', 'loop', 'single'].includes(playModeRaw) ? (playModeRaw as PlayMode) : 'loop'
    store.setPlayMode(validMode)
  }

  if (map.has('player.autoPlayOnOpen')) {
    const autoPlayRaw = String(map.get('player.autoPlayOnOpen') || 'true').toLowerCase()
    userPreferredAutoPlayOnOpen.value = autoPlayRaw === 'true'
  }

  if (map.has('lyrics.autoScroll')) {
    const lyricAutoRaw = String(map.get('lyrics.autoScroll') || 'true').toLowerCase()
    lyricAutoScroll.value = lyricAutoRaw === 'true'
  }

  if (map.has('lyrics.fontSize')) {
    const lyricFontRaw = Number(map.get('lyrics.fontSize') || '18')
    lyricFontSize.value = clamp(Number.isFinite(lyricFontRaw) ? lyricFontRaw : 18, 12, 34)
  }
}

const getDefaultSimpleUserSettingsRows = () => [
  { settingKey: 'player.volume', settingValue: '70' },
  { settingKey: 'player.playMode', settingValue: 'loop' },
  { settingKey: 'player.autoPlayOnOpen', settingValue: 'true' },
  { settingKey: 'lyrics.autoScroll', settingValue: 'true' },
  { settingKey: 'lyrics.fontSize', settingValue: '18' }
]

const loadSimpleUserSettings = async () => {
  try {
    const rows = await getUserSettingsApi()
    const mergedRows = [...getDefaultSimpleUserSettingsRows(), ...(rows as any[])]
    applySimpleUserSettings(mergedRows)
  } catch {
    // 使用默认配置兜底，避免影响播放体验
    applySimpleUserSettings(getDefaultSimpleUserSettingsRows())
  }
}

const loadLyrics = async (targetSongId = songId.value) => {
  if (!targetSongId) {
    lyrics.value = []
    activeLyric.value = -1
    return
  }
  try {
    const data = await getLyricsApi(targetSongId)
    lyrics.value = normalizeLyrics(data?.lines || [])
    updateActiveLyric((store.currentTime || 0) * 1000)
  } catch (e: any) {
    ElMessage.warning(e?.message || '歌词加载失败')
    lyrics.value = []
    activeLyric.value = -1
  }
}

const refreshLyrics = async () => {
  if (!songId.value || lyricsRefreshing.value) return
  lyricsRefreshing.value = true
  try {
    await loadLyrics(songId.value)
    ElMessage.success('歌词已刷新')
  } catch {
    // loadLyrics 已处理提示
  } finally {
    lyricsRefreshing.value = false
  }
}

const loadFavoriteStatus = async (targetSongId = songId.value) => {
  if (!targetSongId) {
    favorited.value = false
    return
  }
  favorited.value = await checkFavoriteApi(1, targetSongId)
}

const loadSongDetails = async (targetSongId: number) => {
  if (!targetSongId) {
    lyrics.value = []
    comments.value = []
    commentsError.value = ''
    commentsLoading.value = false
    favorited.value = false
    lastLoadedSongDetailsId = 0
    return
  }

  if (targetSongId === lastLoadedSongDetailsId) return

  await Promise.allSettled([loadFavoriteStatus(targetSongId), loadLyrics(targetSongId), loadComments(targetSongId)])
  lastLoadedSongDetailsId = targetSongId
}

onMounted(async () => {
  bootstrapping.value = true
  shouldResumeAfterReload = consumeAutoResumeOnReload()
  if (isReloadNavigation()) consumeReloadProgressSnapshot()
  audio.addEventListener('loadedmetadata', onLoadedMetadata)
  audio.addEventListener('durationchange', onDurationChange)
  audio.addEventListener('timeupdate', onTimeUpdate)
  audio.addEventListener('ended', onAudioEnded)
  audio.addEventListener('play', onAudioPlay)
  audio.addEventListener('pause', onAudioPause)
  window.addEventListener('beforeunload', onBeforeUnload)
  window.addEventListener('resize', onWindowResize)
  window.addEventListener('pointermove', onSpatialPointerMove)
  window.addEventListener('pointerup', stopSpatialDragging)
  window.addEventListener(USER_SETTINGS_UPDATED_EVENT, onUserSettingsUpdated as EventListener)

  loading.value = true
  error.value = ''
  try {
    const hasLocalState = !!store.currentSongId && store.queue.length > 0 && store.queueSongs.length > 0
    if (!hasLocalState) {
      await store.hydrateFromServer()
    }

    if (songId.value && songId.value !== store.currentSongId) {
      await store.playBySongId(songId.value)
    }

    await loadSongDetails(songId.value || store.currentSongId || 0)
    await loadSimpleUserSettings()
    void loadEffectSettings()
  } catch (e: any) {
    error.value = e?.message || '播放器初始化失败'
  } finally {
    loading.value = false
    bootstrapping.value = false
  }

})

watch(
  src,
  async (val) => {
    if (!val) return
    const sameSource = !!audio.src && audio.src.includes(val)
    if (sameSource) {
      const d = audio.duration
      duration.value = Number.isFinite(d) && d > 0 ? d : duration.value
      store.currentTime = Number.isFinite(audio.currentTime) ? audio.currentTime : store.currentTime
      updateActiveLyric((store.currentTime || 0) * 1000)
      // 即便已在播放同一首，也要确保音频上下文和频谱链路可用
      try {
        await ensureAudioContext()
      } catch {
        // ignore
      }
      // 已在播放同一首时，不重新 load，避免跳到播放器页后从头播放。
      // 若当前是暂停状态，仅恢复界面状态，不自动播放。
      if (advancedEqVisible.value) {
        await nextTick()
        startSpectrumLoop()
      }
      return
    }
    // 进入页面时如果 src 初始就有值，watch 默认不会执行首次回调，
    // 导致后端不请求 stream、也拿不到时长；这里显式 immediate + load。
    duration.value = 0
    if (!(pendingResumeSongId && pendingResumeSongId === songId.value)) {
      store.currentTime = 0
    }
    audio.src = val
    audio.load()
    applyPitch()
    if (shouldAutoplayOnInit()) {
      try {
        await ensureAudioContext()
        await audio.play()
        store.playing = true
      } catch {
        store.playing = false
      }
    } else {
      store.playing = false
    }
  },
  { immediate: true }
)

watch(
  () => store.currentSongId,
  async (id) => {
    if (!id) return
    router.replace(`/player/${id}`)
    if (bootstrapping.value) return
    await loadSongDetails(id)
  }
)

onBeforeUnmount(() => {
  audio.removeEventListener('loadedmetadata', onLoadedMetadata)
  audio.removeEventListener('durationchange', onDurationChange)
  audio.removeEventListener('timeupdate', onTimeUpdate)
  audio.removeEventListener('ended', onAudioEnded)
  audio.removeEventListener('play', onAudioPlay)
  audio.removeEventListener('pause', onAudioPause)
  window.removeEventListener('beforeunload', onBeforeUnload)
  window.removeEventListener('resize', onWindowResize)
  window.removeEventListener('pointermove', onSpatialPointerMove)
  window.removeEventListener('pointerup', stopSpatialDragging)
  window.removeEventListener(USER_SETTINGS_UPDATED_EVENT, onUserSettingsUpdated as EventListener)
  if (seekReleaseTimer) window.clearTimeout(seekReleaseTimer)
  if (lyricJumpHintTimer) window.clearTimeout(lyricJumpHintTimer)
  if (lyricManualPauseTimer) window.clearTimeout(lyricManualPauseTimer)
  if (lyricProgrammaticReleaseTimer) window.clearTimeout(lyricProgrammaticReleaseTimer)
  if (persistTimer) window.clearTimeout(persistTimer)
  if (spectrumStartRetryTimer) window.clearTimeout(spectrumStartRetryTimer)
  if (spectrumFrame) {
    cancelAnimationFrame(spectrumFrame)
    spectrumFrame = 0
  }
})

const toggle = () => {
  if (audio.paused) {
    ensureAudioContext()
      .then(() => audio.play())
      .then(() => {
        store.playing = true
      })
      .catch(() => {
        store.playing = false
      })
  } else {
    audio.pause()
    store.playing = false
  }
}
const prev = async () => store.prev()
const next = async () => store.next()
const toggleFavorite = async () => {
  if (!songId.value) return
  if (favorited.value) await removeFavoriteApi(1, songId.value)
  else await addFavoriteApi(1, songId.value)
  favorited.value = !favorited.value
}

const seekTo = (sec: number) => {
  if (!duration.value) return
  const next = Math.min(Math.max(sec, 0), duration.value)
  isSeeking.value = true
  audio.currentTime = next
  store.currentTime = next
  updateActiveLyric(next * 1000)
  // 等浏览器触发 timeupdate 后再恢复
  if (seekReleaseTimer) window.clearTimeout(seekReleaseTimer)
  seekReleaseTimer = window.setTimeout(() => {
    isSeeking.value = false
  }, 150)
}

const onSeekChange = (e: Event) => {
  const target = e.target as HTMLInputElement | null
  if (!target) return
  seekTo(Number(target.value))
}

const showLyricHint = (text: string, duration = 1200) => {
  if (lyricJumpHintTimer) window.clearTimeout(lyricJumpHintTimer)
  lyricJumpHint.value = text
  if (duration <= 0) {
    lyricJumpHintTimer = undefined
    return
  }
  lyricJumpHintTimer = window.setTimeout(() => {
    lyricJumpHint.value = ''
    lyricJumpHintTimer = undefined
  }, duration)
}

const scrollToActiveLyric = (behavior: ScrollBehavior = 'smooth') => {
  if (!lyricScrollRef.value || activeLyric.value < 0) return
  const container = lyricScrollRef.value
  const activeEl = container.querySelector('.lyric-line.is-active') as HTMLElement | null
  if (!activeEl) return
  lyricProgrammaticScrolling.value = true
  if (lyricProgrammaticReleaseTimer) window.clearTimeout(lyricProgrammaticReleaseTimer)
  const targetTop = activeEl.offsetTop - container.clientHeight / 2 + activeEl.clientHeight / 2
  container.scrollTo({
    top: Math.max(targetTop, 0),
    behavior
  })
  lyricProgrammaticReleaseTimer = window.setTimeout(() => {
    lyricProgrammaticScrolling.value = false
    lyricProgrammaticReleaseTimer = undefined
  }, 260)
}

const onLyricManualScroll = () => {
  if (!lyricAutoScroll.value) return
  if (lyricProgrammaticScrolling.value) return
  lyricManualPause.value = true
  if (lyricManualPauseTimer) window.clearTimeout(lyricManualPauseTimer)
  lyricManualPauseTimer = window.setTimeout(() => {
    lyricManualPause.value = false
    lyricManualPauseTimer = undefined
  }, 2400)
  showLyricHint('已暂停自动跟随，点“回到当前”继续', 1500)
}

const jumpToLyricLine = (line: any) => {
  const targetMs = Number(line?.time ?? NaN)
  if (!Number.isFinite(targetMs)) return
  const targetSec = targetMs / 1000
  seekTo(targetSec)
  showLyricHint(`已跳转到 ${formatTime(targetSec)}`)
}

const toggleLyricAutoScroll = () => {
  lyricAutoScroll.value = !lyricAutoScroll.value
  lyricManualPause.value = false
  if (lyricAutoScroll.value) {
    scrollToActiveLyric()
    showLyricHint('自动跟随已开启', 1000)
    return
  }
  showLyricHint('自动跟随已关闭', 1000)
}

const nudgeLyricFontSize = (delta: number) => {
  lyricFontSize.value = clamp(lyricFontSize.value + delta, 12, 34)
}

const resumeLyricAutoFollow = () => {
  lyricManualPause.value = false
  if (lyricManualPauseTimer) {
    window.clearTimeout(lyricManualPauseTimer)
    lyricManualPauseTimer = undefined
  }
  scrollToActiveLyric()
  showLyricHint('已回到当前歌词', 1000)
}

const appendQuickComment = (text: string) => {
  const phrase = String(text || '').trim()
  if (!phrase) return
  const current = commentContent.value.trim()
  const nextContent = current ? `${current} ${phrase}` : phrase
  commentContent.value = nextContent.slice(0, 300)
}

const onPlayModeChange = (mode: PlayMode) => {
  store.setPlayMode(mode)
}

const onEqPresetChange = (preset: string) => {
  eqPreset.value = preset
  applyEqPreset(preset)
  if (!isApplyingAdvancedFxPreset.value) {
    advancedFxPreset.value = 'custom'
  }
  persistEffectSettings()
}

const applyAdvancedFxPreset = (preset: string) => {
  const config = advancedFxPresetConfigs[preset]
  if (!config) return
  isApplyingAdvancedFxPreset.value = true
  try {
    eqPreset.value = config.eqPreset
    applyEqPreset(config.eqPreset)
    compressorEnabled.value = config.compressorEnabled
    compressorThreshold.value = config.compressorThreshold
    compressorRatio.value = config.compressorRatio
    compressorAttack.value = config.compressorAttack
    compressorRelease.value = config.compressorRelease
    reverbEnabled.value = config.reverbEnabled
    reverbType.value = config.reverbType
    reverbRoomSize.value = config.reverbRoomSize
    reverbDecay.value = config.reverbDecay
    reverbWet.value = config.reverbWet
    spatialEnabled.value = config.spatialEnabled
    spatialDepth.value = config.spatialDepth
    spatialWidth.value = config.spatialWidth
    channelBalance.value = config.channelBalance
    leftGainDb.value = config.leftGainDb
    rightGainDb.value = config.rightGainDb
    phaseInvertLeft.value = config.phaseInvertLeft
    phaseInvertRight.value = config.phaseInvertRight
    monoMerge.value = config.monoMerge
    leftDelayMs.value = config.leftDelayMs
    rightDelayMs.value = config.rightDelayMs
    applyCompressorSettings()
    applyReverbSettings()
    applySpatialSettings()
    applyChannelSettings()
    advancedFxPreset.value = preset
  } finally {
    isApplyingAdvancedFxPreset.value = false
  }
  persistEffectSettings()
}

const onAdvancedFxPresetChange = (preset: string) => {
  if (!advancedConfigEnabled.value) return
  if (preset === 'custom') {
    advancedFxPreset.value = 'custom'
    return
  }
  applyAdvancedFxPreset(preset)
}

const onAdvancedConfigToggle = (value: boolean | string | number) => {
  advancedConfigEnabled.value = !!value
  persistEffectSettings()
}

const onPitchChange = (value: number) => {
  pitchSemitone.value = value
  applyPitch()
  persistEffectSettings()
}

const normalizePitchInput = (value: unknown) => {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) return pitchSemitone.value
  const rounded = Math.round(parsed)
  return Math.min(12, Math.max(-12, rounded))
}

const onPitchInputEnter = () => {
  onPitchChange(normalizePitchInput(pitchInput.value))
}

const onEqGainChange = (index: number, value: number) => {
  eqGains.value[index] = value
  applyEqGains()
  eqPreset.value = 'custom'
  if (!isApplyingAdvancedFxPreset.value) {
    advancedFxPreset.value = 'custom'
  }
  persistEffectSettings()
}

const onEqGainInput = (index: number, value: unknown) => {
  onEqGainChange(index, Number(value))
}

const resetEffects = () => {
  pitchSemitone.value = 0
  gainDb.value = 0
  attenuateDb.value = 0
  advancedFxPreset.value = 'default'
  compressorEnabled.value = false
  compressorThreshold.value = -24
  compressorRatio.value = 4
  compressorAttack.value = 0.003
  compressorRelease.value = 0.25
  reverbEnabled.value = false
  reverbType.value = 'room'
  reverbRoomSize.value = 40
  reverbDecay.value = 2.2
  reverbWet.value = 25
  spatialEnabled.value = false
  spatialDepth.value = 30
  spatialWidth.value = 0
  channelBalance.value = 0
  leftGainDb.value = 0
  rightGainDb.value = 0
  phaseInvertLeft.value = false
  phaseInvertRight.value = false
  monoMerge.value = false
  leftDelayMs.value = 0
  rightDelayMs.value = 0
  eqPreset.value = 'flat'
  applyPitch()
  applyEqPreset('flat')
  applyMasterGain()
  applyCompressorSettings()
  applyReverbSettings()
  applySpatialSettings()
  applyChannelSettings()
  persistEffectSettings()
}

const decreasePitch = () => {
  if (pitchSemitone.value <= -12) return
  onPitchChange(pitchSemitone.value - 1)
}

const increasePitch = () => {
  if (pitchSemitone.value >= 12) return
  onPitchChange(pitchSemitone.value + 1)
}

watch(
  () => pitchSemitone.value,
  (value) => {
    pitchInput.value = String(value)
  },
  { immediate: true }
)

watch(
  [compressorEnabled, compressorThreshold, compressorRatio, compressorAttack, compressorRelease],
  () => {
    applyCompressorSettings()
    if (!isApplyingAdvancedFxPreset.value) {
      advancedFxPreset.value = 'custom'
    }
    persistEffectSettings()
  }
)

watch([reverbEnabled, reverbType, reverbRoomSize, reverbDecay, reverbWet], () => {
  applyReverbSettings()
  if (!isApplyingAdvancedFxPreset.value) {
    advancedFxPreset.value = 'custom'
  }
  persistEffectSettings()
})

watch([spatialEnabled, spatialDepth, spatialWidth], () => {
  applySpatialSettings()
  if (!isApplyingAdvancedFxPreset.value) {
    advancedFxPreset.value = 'custom'
  }
  persistEffectSettings()
})

watch(
  [channelBalance, leftGainDb, rightGainDb, phaseInvertLeft, phaseInvertRight, monoMerge, leftDelayMs, rightDelayMs],
  () => {
    applyChannelSettings()
    if (!isApplyingAdvancedFxPreset.value) {
      advancedFxPreset.value = 'custom'
    }
    persistEffectSettings()
  }
)

watch(advancedEqVisible, async (visible) => {
  if (!visible) {
    if (spectrumStartRetryTimer) {
      window.clearTimeout(spectrumStartRetryTimer)
      spectrumStartRetryTimer = undefined
    }
    if (spectrumFrame) {
      cancelAnimationFrame(spectrumFrame)
      spectrumFrame = 0
    }
    return
  }
  try {
    await ensureAudioContext()
  } catch {
    // ignore
  }
  await nextTick()
  window.setTimeout(() => startSpectrumLoop(), 0)
})

watch(
  () => store.playing,
  async (playing) => {
    if (!advancedEqVisible.value) return
    if (!playing) return
    try {
      await ensureAudioContext()
    } catch {
      // ignore
    }
    await nextTick()
    startSpectrumLoop()
  }
)

watch(
  () => advancedGroupOpen.value.eq,
  async (open) => {
    if (!open || !advancedEqVisible.value) return
    await nextTick()
    startSpectrumLoop()
  }
)

const onWindowResize = () => {
  if (!advancedEqVisible.value) return
  const canvas = spectrumCompareCanvasRef.value
  if (canvas) resizeSpectrumCanvas(canvas)
}

const onGainInput = (value: unknown) => {
  gainDb.value = Number(value)
  applyMasterGain()
  persistEffectSettings()
}

const onAttenuateInput = (value: unknown) => {
  attenuateDb.value = Number(value)
  applyMasterGain()
  persistEffectSettings()
}

const spectrumNoSignalReason = computed(() => {
  if (!store.playing) return '当前已暂停，播放后可查看实时频谱'
  if (!songId.value) return '当前无播放歌曲，请先选择歌曲'
  if (audio.volume <= 0.001) return '播放器音量为 0，请调高音量'
  if (audio.muted) return '播放器处于静音状态，请取消静音'
  if (!audioCtx || audioCtx.state !== 'running') return '音频上下文未激活，请点击播放键或刷新页面'
  const peak = Math.max(spectrumInputPeak.value, spectrumOutputPeak.value)
  if (peak < 0.006) return '信号较弱：可提高音量、增益或等待音乐进入有效段落'
  return ''
})

const modeSymbol = computed(() => {
  if (store.playMode === 'random') return '🔀'
  if (store.playMode === 'order') return '➡️'
  if (store.playMode === 'single') return '🔂'
  return '🔁'
})

const publishComment = async () => {
  if (!songId.value) return
  if (commentSubmitting.value) return
  const content = commentContent.value.trim()
  if (!content) return ElMessage.warning('评论内容不能为空')
  commentSubmitting.value = true
  try {
    await addCommentApi(songId.value, content)
    commentContent.value = ''
    await loadComments()
    ElMessage.success('评论已发布')
  } catch (e: any) {
    ElMessage.error(e?.message || '评论发布失败')
  } finally {
    commentSubmitting.value = false
  }
}

const likeComment = async (row: any) => {
  try {
    await likeCommentApi(row.id)
    await loadComments()
  } catch (e: any) {
    ElMessage.error(e?.message || '点赞失败')
  }
}

const unlikeComment = async (row: any) => {
  try {
    await unlikeCommentApi(row.id)
    await loadComments()
  } catch (e: any) {
    ElMessage.error(e?.message || '取消点赞失败')
  }
}

const removeComment = async (id: number) => {
  try {
    await deleteCommentApi(id)
    await loadComments()
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

const submitFeedback = async () => {
  if (!feedbackForm.value.content.trim()) return ElMessage.warning('请输入反馈内容')
  feedbackSubmitting.value = true
  try {
    await createFeedbackApi(feedbackForm.value)
    feedbackForm.value.content = ''
    feedbackForm.value.contact = ''
    feedbackVisible.value = false
    ElMessage.success('反馈提交成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '反馈提交失败')
  } finally {
    feedbackSubmitting.value = false
  }
}

const playFromQueue = async (id: number) => {
  if (!id) return
  const isCurrentSong = Number(id) === Number(store.currentSongId)
  await store.playBySongId(id)
  // Clicking the current queue row does not change songId, so src watcher won't run.
  // Explicitly resume the shared audio element to avoid "UI playing but no sound".
  if (isCurrentSong && audio.paused) {
    try {
      await ensureAudioContext()
      await audio.play()
      store.playing = true
    } catch {
      store.playing = false
    }
  }
}

const removeQueueSong = async (id: number) => {
  if (!id) return
  try {
    await store.removeFromQueue(id)
    ElMessage.success('已从队列移除')
  } catch (e: any) {
    ElMessage.error(e?.message || '移除失败')
  }
}

const clearQueueAll = async () => {
  try {
    audio.pause()
    audio.removeAttribute('src')
    audio.load()
    await store.clearQueue()
    duration.value = 0
    activeLyric.value = -1
    ElMessage.success('播放列表已清空')
  } catch (e: any) {
    ElMessage.error(e?.message || '清空失败')
  }
}

const onQueueRowClick = (row: any) => {
  if (!row?.id) return
  playFromQueue(row.id)
}

const queueRowClassName = ({ row }: any) => {
  if (row?.id !== store.currentSongId) return ''
  return store.playing ? 'queue-current-row queue-current-playing' : 'queue-current-row'
}

const onUserSettingsUpdated = (event: Event) => {
  const customEvent = event as CustomEvent<{ items?: Array<{ settingKey?: string; settingValue?: string }> }>
  const items = customEvent?.detail?.items || []
  if (!items.length) return
  applySimpleUserSettings(items)
}

watch(
  () => activeLyric.value,
  () => {
    if (!lyricAutoScroll.value) return
    if (lyricManualPause.value) return
    if (detailTab.value !== 'lyrics') return
    scrollToActiveLyric('smooth')
  }
)

watch(
  () => detailTab.value,
  (tab) => {
    if (tab !== 'lyrics') return
    if (!lyricAutoScroll.value) return
    lyricManualPause.value = false
    window.setTimeout(() => scrollToActiveLyric('auto'), 0)
  }
)
</script>

<template>
  <!-- <h2 class="page-title">播放器</h2> -->
  <StateBlock :loading="loading" :error="error">
    <div class="player-layout">
      <div class="player-left">
        <el-card class="glow-card player-info-card">
          <div class="song-title-row">
            <h3 class="section-title song-title-text">{{ song ? getDisplaySongTitle(song) : '未选择歌曲' }}</h3>
            <el-button class="download-cute-btn" :disabled="!songId" title="下载歌曲到本地" @click="downloadSong">
              <span class="download-cute-btn__icon" aria-hidden="true">🎵</span>
              <span class="download-cute-btn__text">拿走吧</span>
            </el-button>
          </div>
          <div class="song-meta-row">
            <span class="text-muted">{{ song?.artistName || song?.artistNames || '未知歌手' }}</span>
            <span class="song-meta-time">{{ formatTime(store.currentTime) }} / {{ formatTime(duration) }}</span>
          </div>

          <div class="progress-wrap">
            <div class="progress-time" />
            <input
              class="progress-range"
              type="range"
              min="0"
              :max="duration || 0"
              step="0.1"
              :disabled="!duration"
              :value="store.currentTime"
              @input="onSeekChange"
              @change="onSeekChange"
            />
          </div>

          <div class="player-actions">
            <div class="transport-main">
              <el-button class="transport-btn secondary" circle title="上一首" @click="prev">⏮</el-button>
              <el-button class="transport-btn primary" circle type="primary" :title="store.playing ? '暂停' : '播放'" @click="toggle">
                {{ store.playing ? '❚❚' : '▶' }}
              </el-button>
              <el-button class="transport-btn secondary" circle title="下一首" @click="next">⏭</el-button>
            </div>

            <div class="transport-sub">
              <el-button class="chip-btn" round :title="favorited ? '取消收藏' : '收藏'" @click="toggleFavorite">
                {{ favorited ? '已收藏' : '收藏' }}
              </el-button>
              <el-dropdown @command="onPlayModeChange">
                <el-button class="chip-btn" round :title="playModeOptions.find((item) => item.value === store.playMode)?.label || '播放模式'">
                  <span class="mode-symbol">{{ modeSymbol }}</span>
                  <span>{{ playModeOptions.find((item) => item.value === store.playMode)?.label || '播放模式' }}</span>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-for="item in playModeOptions" :key="item.value" :command="item.value">
                      {{ item.label }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button class="chip-btn" round @click="feedbackVisible = true">反馈</el-button>
            </div>

            <div class="detail-switch" role="tablist" aria-label="详情区切换">
              <button class="detail-switch-btn" :class="{ active: detailTab === 'lyrics' }" @click="detailTab = 'lyrics'">歌词</button>
              <button class="detail-switch-btn" :class="{ active: detailTab === 'comments' }" @click="detailTab = 'comments'">
                评论 {{ comments.length }}
              </button>
            </div>
          </div>
        </el-card>

        <el-card class="glow-card lyric-detail-card">
          <template #header>
            <div class="lyric-detail-header">
              <span>{{ detailTab === 'lyrics' ? '歌词详情' : `歌曲评论（${comments.length}）` }}</span>
              <el-tooltip v-if="detailTab === 'lyrics'" content="刷新歌词" placement="top">
                <el-button
                  class="lyric-refresh-btn"
                  circle
                  text
                  :loading="lyricsRefreshing"
                  :disabled="!songId"
                  aria-label="刷新歌词"
                  @click="refreshLyrics"
                >
                  ⟳
                </el-button>
              </el-tooltip>
            </div>
          </template>
          <template v-if="detailTab === 'lyrics'">
            <div class="lyric-toolbar">
              <button class="lyric-tool-btn" @click="toggleLyricAutoScroll">
                {{ lyricAutoScroll ? '自动跟随：开' : '自动跟随：关' }}
              </button>
              <button class="lyric-tool-btn" @click="resumeLyricAutoFollow">回到当前</button>
              <div class="lyric-font-tools">
                <button class="lyric-font-btn" @click="nudgeLyricFontSize(-1)">A-</button>
                <span class="lyric-font-value">{{ lyricFontSize }}</span>
                <button class="lyric-font-btn" @click="nudgeLyricFontSize(1)">A+</button>
              </div>
              <span v-if="lyricJumpHint" class="lyric-jump-hint">{{ lyricJumpHint }}</span>
            </div>
            <StateBlock :empty="!lyrics.length" empty-text="暂无歌词">
              <div ref="lyricScrollRef" class="lyric-scroll-wrap" @scroll.passive="onLyricManualScroll">
                <div
                  v-for="(line, index) in lyrics"
                  :key="index"
                  class="lyric-line"
                  :style="{ fontSize: `${lyricFontSize}px` }"
                  :class="{ 'is-active': index === activeLyric, 'is-playing': index === activeLyric && store.playing }"
                  @click="jumpToLyricLine(line)"
                >
                  <span class="lyric-text">{{ line.text }}</span>
                  <span class="lyric-side">
                    <span class="lyric-time">{{ formatTime(Number(line?.time || 0) / 1000) }}</span>
                    <span v-if="index === activeLyric && store.playing" class="lyric-wave" aria-hidden="true">
                      <i></i><i></i><i></i>
                    </span>
                  </span>
                </div>
              </div>
            </StateBlock>
          </template>
          <template v-else>
            <div class="comment-panel">
              <div class="comment-quick-actions">
                <button
                  v-for="item in commentQuickActions"
                  :key="item"
                  class="comment-quick-btn"
                  type="button"
                  @click="appendQuickComment(item)"
                >
                  {{ item }}
                </button>
              </div>

              <StateBlock
                class="comment-state"
                :loading="commentsLoading"
                :error="commentsError"
                :empty="!comments.length"
                empty-text="暂无评论"
              >
                <div class="comment-list">
                  <article v-for="row in comments" :key="row.id" class="comment-row">
                    <p class="comment-row-content">{{ row.content }}</p>
                    <div class="comment-row-meta">
                      <span class="comment-row-like">👍 {{ Number(row.likeCount || 0) }}</span>
                      <div class="comment-row-actions">
                        <button class="comment-action-btn" type="button" @click="likeComment(row)">赞</button>
                        <button class="comment-action-btn" type="button" @click="unlikeComment(row)">取消</button>
                        <el-popconfirm title="确认删除这条评论吗？" @confirm="removeComment(row.id)">
                          <template #reference>
                            <button class="comment-action-btn danger" type="button">删</button>
                          </template>
                        </el-popconfirm>
                      </div>
                    </div>
                  </article>
                </div>
              </StateBlock>

              <div class="comment-composer">
                <el-input
                  class="comment-input"
                  v-model="commentContent"
                  type="textarea"
                  :rows="2"
                  maxlength="300"
                  show-word-limit
                  resize="none"
                  placeholder="写下你的感受…"
                />
                <el-button
                  class="comment-submit-btn"
                  type="primary"
                  :loading="commentSubmitting"
                  :disabled="!commentContent.trim()"
                  @click="publishComment"
                >
                  发布
                </el-button>
              </div>
            </div>
          </template>
        </el-card>
      </div>

      <div class="player-right">
        <el-card ref="effectCardRef" class="glow-card effect-card">
          <template #header>
            <div class="effect-card-header">
              <span>效果器</span>
              <el-button size="small" type="primary" plain @click="advancedEqVisible = true">高级</el-button>
            </div>
          </template>
          <div class="effect-row">
            <span class="effect-label">均衡器预设</span>
            <el-select :model-value="eqPreset" size="small" style="width: 180px" @change="onEqPresetChange">
              <el-option v-for="item in eqPresetOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-button text type="primary" @click="resetEffects">重置</el-button>
          </div>

          <div class="effect-row">
            <div class="triple-slider-wrap">
              <div class="dual-slider">
                <span class="mini-label">增益</span>
                <el-slider :model-value="gainDb" :min="0" :max="12" :step="0.5" style="flex: 1" @input="onGainInput($event)" />
              </div>
              <div class="dual-slider">
                <span class="mini-label">衰减</span>
                <el-slider :model-value="attenuateDb" :min="0" :max="12" :step="0.5" style="flex: 1" @input="onAttenuateInput($event)" />
              </div>
              <div class="dual-slider">
                <span class="mini-label">变调</span>
                <div class="pitch-stepper">
                  <el-button size="small" @click="decreasePitch">-</el-button>
                  <el-input
                    v-model="pitchInput"
                    size="small"
                    class="pitch-input"
                    @keyup.enter="onPitchInputEnter"
                  />
                  <el-button size="small" @click="increasePitch">+</el-button>
                </div>
              </div>
            </div>
          </div>

          <div class="eq-grid">
            <div v-for="(freq, index) in EQ_FREQUENCIES" :key="freq" class="eq-band">
              <el-slider
                :model-value="eqGains[index]"
                vertical
                :min="-12"
                :max="12"
                :step="0.5"
                height="82px"
                @input="onEqGainInput(index, $event)"
              />
              <div class="eq-label">{{ freq >= 1000 ? `${freq / 1000}k` : freq }}Hz</div>
            </div>
          </div>

        </el-card>

        <el-card class="glow-card queue-card">
          <template #header>
            <div class="queue-header">
              <span>播放队列（{{ store.queueSongs.length }}）</span>
              <el-popconfirm title="确认清空播放列表吗？" @confirm="clearQueueAll">
                <template #reference>
                  <el-button text type="danger" size="small" :disabled="!store.queueSongs.length">清空</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
          <StateBlock class="queue-state" :empty="!store.queueSongs.length" empty-text="暂无播放队列">
            <el-table
              :data="store.queueSongs"
              :row-class-name="queueRowClassName"
              size="small"
              height="100%"
              @row-click="onQueueRowClick"
            >
              <el-table-column label="歌曲" min-width="200">
                <template #default="{ row }">
                  {{ getDisplaySongTitle(row) }}
                </template>
              </el-table-column>
              <el-table-column prop="artistName" label="歌手" min-width="120" />
              <el-table-column label="操作" width="260">
                <template #default="{ row }">
                  <el-space>
                    <el-button text type="danger" @click.stop="removeQueueSong(row.id)">删除</el-button>
                    <el-button text type="primary" @click.stop="playFromQueue(row.id)">播放</el-button>
                  </el-space>
                </template>
              </el-table-column>
            </el-table>
          </StateBlock>
        </el-card>
      </div>
    </div>
  </StateBlock>

  <transition
    @before-enter="onAdvancedOverlayBeforeEnter"
    @enter="onAdvancedOverlayEnter"
    @after-enter="onAdvancedOverlayAfterEnter"
    @before-leave="onAdvancedOverlayBeforeLeave"
    @leave="onAdvancedOverlayLeave"
    @after-leave="onAdvancedOverlayAfterLeave"
  >
    <div v-if="advancedEqVisible" class="advanced-eq-overlay">
      <div class="advanced-eq-shell">
        <div class="advanced-eq-topbar">
          <div class="advanced-eq-title">均衡器高级功能模块</div>
          <div class="advanced-eq-actions">
            <el-button text @click="resetEffects">重置全部参数</el-button>
            <el-button type="primary" @click="advancedEqVisible = false">关闭</el-button>
          </div>
        </div>
        <div class="advanced-tab-content advanced-scroll-page">
          <div class="advanced-primary-layout">
            <section class="module-section" :class="{ 'is-collapsed': !advancedGroupOpen.master }">
              <div class="module-header-row">
                <span class="module-title">高级预设与基础控制</span>
                <div class="module-header-actions">
                  <button class="module-collapse" @click="toggleAdvancedGroup('master')">
                    {{ advancedGroupOpen.master ? '收起' : '展开' }}
                  </button>
                </div>
              </div>
              <div v-show="advancedGroupOpen.master" class="advanced-form-grid advanced-master-grid">
                <div class="advanced-form-row">
                  <span>均衡器预设</span>
                  <el-select :model-value="eqPreset" :teleported="false" @change="onEqPresetChange">
                    <el-option v-for="item in eqPresetOptions" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </div>

                <div class="advanced-form-row advanced-form-row--dual master-param-row">
                  <div class="dial-card">
                    <span class="dial-label">增益 (dB)</span>
                    <el-input-number
                      :model-value="gainDb"
                      :min="0"
                      :max="12"
                      :step="0.5"
                      controls-position="right"
                      @change="onGainInput($event ?? 0)"
                    />
                    <el-progress :percentage="Math.round((gainDb / 12) * 100)" :stroke-width="8" status="success" />
                  </div>
                  <div class="dial-card">
                    <span class="dial-label">衰减 (dB)</span>
                    <el-input-number
                      :model-value="attenuateDb"
                      :min="0"
                      :max="12"
                      :step="0.5"
                      controls-position="right"
                      @change="onAttenuateInput($event ?? 0)"
                    />
                    <el-progress :percentage="Math.round((attenuateDb / 12) * 100)" :stroke-width="8" color="#94a3b8" />
                  </div>
                </div>

                <div class="advanced-form-row">
                  <span>变调（半音）</span>
                  <div class="pitch-stepper">
                    <el-button size="small" @click="decreasePitch">-</el-button>
                    <el-input
                      v-model="pitchInput"
                      size="small"
                      class="pitch-input"
                      @keyup.enter="onPitchInputEnter"
                    />
                    <el-button size="small" @click="increasePitch">+</el-button>
                  </div>
                </div>

                <div class="master-status-panel">
                  <div class="master-status-title">基础控制状态概览</div>
                  <div class="master-status-grid">
                    <div class="master-status-item">
                      <span>配置状态</span>
                      <el-tag :type="advancedConfigEnabled ? 'success' : 'info'" effect="light">
                        {{ advancedConfigEnabled ? '已启用' : '已关闭' }}
                      </el-tag>
                    </div>
                    <div class="master-status-item">
                      <span>当前预设</span>
                      <el-tag type="primary" effect="light">{{ eqPreset }}</el-tag>
                    </div>
                    <div class="master-status-item">
                      <span>当前音高</span>
                      <el-tag type="warning" effect="light">{{ pitchSemitone > 0 ? `+${pitchSemitone}` : pitchSemitone }} st</el-tag>
                    </div>
                    <div class="master-status-item">
                      <span>高级预设</span>
                      <el-tag type="danger" effect="light">{{ advancedFxPreset }}</el-tag>
                    </div>
                  </div>
                </div>
              </div>
            </section>

            <section class="module-section" :class="{ 'is-collapsed': !advancedGroupOpen.eq }">
              <div class="module-header-row">
                <span class="module-title">12路均衡器与实时频谱分析</span>
                <div class="module-header-actions">
                  <button class="module-help" @click="openHelpDialog('eq')">?</button>
                  <button class="module-collapse" @click="toggleAdvancedGroup('eq')">
                    {{ advancedGroupOpen.eq ? '收起' : '展开' }}
                  </button>
                </div>
              </div>
              <div v-show="advancedGroupOpen.eq" class="advanced-eq-main">
                <div class="visual-panel">
                  <div class="spectrum-title">实时频谱（Pre / Post）</div>
                  <canvas ref="spectrumCompareCanvasRef" class="spectrum-canvas spectrum-canvas-compare"></canvas>
                </div>
                <div class="eq-grid eq-grid-advanced">
                  <div v-for="(freq, index) in EQ_FREQUENCIES" :key="`${freq}-advanced`" class="eq-band eq-band-vivid">
                    <div class="eq-band-head">{{ freq >= 1000 ? `${freq / 1000}k` : freq }}Hz</div>
                    <el-slider
                      :model-value="eqGains[index]"
                      vertical
                      :min="-12"
                      :max="12"
                      :step="0.5"
                      height="170px"
                      @input="onEqGainInput(index, $event)"
                    />
                    <div class="eq-band-value" :class="{ 'is-pos': eqGains[index] > 0, 'is-neg': eqGains[index] < 0 }">
                      {{ eqGains[index] > 0 ? `+${eqGains[index]}` : eqGains[index] }} dB
                    </div>
                  </div>
                </div>
              </div>
            </section>
          </div>

          <section class="module-section advanced-config-section">
            <div class="advanced-config-row">
              <div class="advanced-config-item">
                <span>高级配置开关</span>
                <el-switch
                  :model-value="advancedConfigEnabled"
                  inline-prompt
                  active-text="已开启"
                  inactive-text="已关闭"
                  @change="onAdvancedConfigToggle"
                />
              </div>
              <div class="advanced-config-item">
                <span>高级预设</span>
                <el-select :model-value="advancedFxPreset" :teleported="false" :disabled="!advancedConfigEnabled" @change="onAdvancedFxPresetChange">
                  <el-option v-for="item in advancedFxPresetOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </div>
            </div>
          </section>

          <div class="advanced-fx-grid">
            <section class="module-section" :class="{ 'is-collapsed': !advancedGroupOpen.compressor }">
              <div class="module-header-row">
                <span class="module-title">专业压缩器</span>
                <div class="module-header-actions">
                  <button class="module-help" @click="openHelpDialog('compressor')">?</button>
                  <button class="module-collapse" @click="toggleAdvancedGroup('compressor')">
                    {{ advancedGroupOpen.compressor ? '收起' : '展开' }}
                  </button>
                </div>
              </div>
              <div v-show="advancedGroupOpen.compressor" class="advanced-form-grid advanced-form-grid--compact">
                <div class="advanced-form-row">
                  <span>启用压缩器</span>
                  <el-switch v-model="compressorEnabled" :disabled="!advancedConfigEnabled" />
                </div>
                <div class="advanced-form-row">
                  <span>阈值 (dB)</span>
                  <el-input-number v-model="compressorThreshold" :min="-60" :max="0" :step="1" :disabled="!advancedConfigEnabled" />
                </div>
                <div class="advanced-form-row">
                  <span>压缩比</span>
                  <el-input-number v-model="compressorRatio" :min="1" :max="20" :step="0.1" :precision="1" :disabled="!advancedConfigEnabled" />
                </div>
                <div class="advanced-form-row">
                  <span>启动时间 Attack (s)</span>
                  <el-input-number v-model="compressorAttack" :min="0.001" :max="1" :step="0.001" :precision="3" :disabled="!advancedConfigEnabled" />
                </div>
                <div class="advanced-form-row">
                  <span>释放时间 Release (s)</span>
                  <el-input-number v-model="compressorRelease" :min="0.01" :max="1" :step="0.01" :precision="2" :disabled="!advancedConfigEnabled" />
                </div>
              </div>
            </section>

            <section class="module-section" :class="{ 'is-collapsed': !advancedGroupOpen.reverb }">
              <div class="module-header-row">
                <span class="module-title">混响器</span>
                <div class="module-header-actions">
                  <button class="module-help" @click="openHelpDialog('reverb')">?</button>
                  <button class="module-collapse" @click="toggleAdvancedGroup('reverb')">
                    {{ advancedGroupOpen.reverb ? '收起' : '展开' }}
                  </button>
                </div>
              </div>
              <div v-show="advancedGroupOpen.reverb" class="advanced-form-grid advanced-form-grid--compact">
                <div class="advanced-form-row">
                  <span>启用混响</span>
                  <el-switch v-model="reverbEnabled" :disabled="!advancedConfigEnabled" />
                </div>
                <div class="advanced-form-row">
                  <span>空间类型</span>
                  <el-radio-group class="reverb-type-group" v-model="reverbType" :disabled="!advancedConfigEnabled">
                    <el-radio-button label="room">房间</el-radio-button>
                    <el-radio-button label="hall">大厅</el-radio-button>
                    <el-radio-button label="canyon">峡谷</el-radio-button>
                  </el-radio-group>
                </div>
                <div class="advanced-form-row advanced-form-row--dual reverb-room-row">
                  <div class="dial-card dial-card--room-size">
                    <span class="dial-label">房间大小</span>
                    <el-slider
                      v-model="reverbRoomSize"
                      :min="10"
                      :max="100"
                      :step="1"
                      :disabled="!advancedConfigEnabled"
                      show-input
                      input-size="small"
                    />
                  </div>
                  <div class="dial-card dial-card--room-visual">
                    <span class="dial-label">空间感可视化</span>
                    <div class="reverb-room-visual" :class="[`is-${reverbType}`]">
                      <div class="reverb-room-ripple reverb-room-ripple--1"></div>
                      <div class="reverb-room-ripple reverb-room-ripple--2"></div>
                      <div class="reverb-room-ripple reverb-room-ripple--3"></div>
                      <div class="reverb-room-core" :style="reverbVisualStyle"></div>
                    </div>
                    <div class="reverb-room-tip">
                      <span>{{ reverbHearingProfile.headline }} · {{ reverbRoomSceneText }}</span>
                      <strong>{{ reverbRoomSize }}%</strong>
                    </div>
                  </div>
                </div>
                <div class="advanced-form-row">
                  <span>衰减时间 (s)</span>
                  <el-slider
                    v-model="reverbDecay"
                    :min="0.5"
                    :max="6"
                    :step="0.1"
                    :disabled="!advancedConfigEnabled"
                    show-input
                    input-size="small"
                  />
                </div>
                <div class="advanced-form-row">
                  <span>干湿比（湿）%</span>
                  <el-slider
                    v-model="reverbWet"
                    :min="0"
                    :max="100"
                    :step="1"
                    :disabled="!advancedConfigEnabled"
                    show-input
                    input-size="small"
                  />
                </div>
                <div class="reverb-hearing-panel">
                  <div class="reverb-hearing-summary">{{ reverbHearingProfile.summary }}</div>
                  <div class="reverb-hearing-chips">
                    <div v-for="chip in reverbHearingProfile.chips" :key="chip.key" class="reverb-hearing-chip">
                      <span class="chip-label">{{ chip.label }}</span>
                      <el-progress :percentage="chip.value" :stroke-width="8" :show-text="false" />
                      <strong class="chip-value">{{ chip.value }}%</strong>
                    </div>
                  </div>
                </div>
              </div>
            </section>

            <section class="module-section" :class="{ 'is-collapsed': !advancedGroupOpen.spatial }">
              <div class="module-header-row">
                <span class="module-title">空间音频与虚拟声场</span>
                <div class="module-header-actions">
                  <button class="module-help" @click="openHelpDialog('spatial')">?</button>
                  <button class="module-collapse" @click="toggleAdvancedGroup('spatial')">
                    {{ advancedGroupOpen.spatial ? '收起' : '展开' }}
                  </button>
                </div>
              </div>
              <div v-show="advancedGroupOpen.spatial" class="advanced-form-grid advanced-form-grid--compact spatial-panel">
                <div class="advanced-form-row">
                  <span>启用空间音频（HRTF）</span>
                  <el-switch v-model="spatialEnabled" :disabled="!advancedConfigEnabled" />
                </div>

                <div class="spatial-stage-card">
                  <div class="spatial-stage-title">
                    虚拟声场定位预览（拖动光点控制前后深度与左右宽度）
                  </div>
                  <div
                    ref="spatialStageMapRef"
                    class="spatial-stage-map"
                    :class="{ 'is-disabled': !advancedConfigEnabled || !spatialEnabled }"
                    @mousedown="onSpatialMapDown"
                    @dblclick="onSpatialMapDoubleClick"
                    @contextmenu="onSpatialMapContextMenu"
                  >
                    <div class="stage-axis stage-axis-x"></div>
                    <div class="stage-axis stage-axis-y"></div>
                    <div
                      class="stage-point"
                      :class="{ 'is-dragging': isDraggingSpatialPoint }"
                      :style="{
                        left: `${Math.max(6, Math.min(94, ((spatialWidth + 100) / 200) * 100))}%`,
                        top: `${Math.max(8, Math.min(92, 100 - spatialDepth))}%`
                      }"
                      @mousedown.stop="onSpatialPointDown"
                    ></div>
                  </div>
                  <div class="spatial-value-row">
                    <span>左右宽度：{{ spatialWidth }}</span>
                    <span>前后深度：{{ spatialDepth }}</span>
                  </div>
                </div>
              </div>
            </section>

            <section class="module-section" :class="{ 'is-collapsed': !advancedGroupOpen.channel }">
              <div class="module-header-row">
                <span class="module-title">声道与平衡控制</span>
                <div class="module-header-actions">
                  <button class="module-help" @click="openHelpDialog('channel')">?</button>
                  <button class="module-collapse" @click="toggleAdvancedGroup('channel')">
                    {{ advancedGroupOpen.channel ? '收起' : '展开' }}
                  </button>
                </div>
              </div>
              <div v-show="advancedGroupOpen.channel" class="advanced-form-grid advanced-form-grid--compact">
                <div class="advanced-form-row">
                  <span>平衡（左-右）</span>
                  <el-slider v-model="channelBalance" :min="-100" :max="100" :step="1" :disabled="!advancedConfigEnabled" />
                </div>
                <div class="advanced-form-row">
                  <span>左声道增益 (dB)</span>
                  <el-slider v-model="leftGainDb" :min="-12" :max="12" :step="0.5" :disabled="!advancedConfigEnabled" />
                </div>
                <div class="advanced-form-row">
                  <span>右声道增益 (dB)</span>
                  <el-slider v-model="rightGainDb" :min="-12" :max="12" :step="0.5" :disabled="!advancedConfigEnabled" />
                </div>
                <div class="advanced-form-row">
                  <span>左相位反转</span>
                  <el-switch v-model="phaseInvertLeft" :disabled="!advancedConfigEnabled" />
                </div>
                <div class="advanced-form-row">
                  <span>右相位反转</span>
                  <el-switch v-model="phaseInvertRight" :disabled="!advancedConfigEnabled" />
                </div>
                <div class="advanced-form-row">
                  <span>L+R 合并输出</span>
                  <el-switch v-model="monoMerge" :disabled="!advancedConfigEnabled" />
                </div>
                <div class="advanced-form-row">
                  <span>左声道延时 (ms)</span>
                  <el-slider v-model="leftDelayMs" :min="0" :max="200" :step="1" :disabled="!advancedConfigEnabled" />
                </div>
                <div class="advanced-form-row">
                  <span>右声道延时 (ms)</span>
                  <el-slider v-model="rightDelayMs" :min="0" :max="200" :step="1" :disabled="!advancedConfigEnabled" />
                </div>
              </div>
            </section>
          </div>
        </div>
      </div>
    </div>
  </transition>

  <el-dialog v-model="feedbackVisible" title="问题反馈" width="520px">
    <el-select v-model="feedbackForm.type" style="width: 180px">
      <el-option label="问题" value="BUG" />
      <el-option label="建议" value="SUGGESTION" />
      <el-option label="功能" value="FEATURE" />
    </el-select>
    <el-input
      v-model="feedbackForm.content"
      type="textarea"
      :rows="4"
      maxlength="400"
      show-word-limit
      placeholder="请描述你遇到的问题或建议"
      style="margin-top: 10px"
    />
    <el-input v-model="feedbackForm.contact" placeholder="联系方式（选填）" style="margin-top: 10px" />
    <template #footer>
      <el-button @click="feedbackVisible = false">取消</el-button>
      <el-button type="primary" :loading="feedbackSubmitting" @click="submitFeedback">提交反馈</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="helpDialogVisible" :title="helpDialogTitle" width="560px">
    <div class="help-dialog-content">{{ helpDialogContent }}</div>
    <template #footer>
      <el-button type="primary" @click="helpDialogVisible = false">我知道了</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.player-layout {
  display: flex;
  gap: 16px;
  align-items: stretch;
  height: calc(100vh - 132px);
  min-height: 560px;
  max-height: calc(100vh - 132px);
  overflow: hidden;
}

.player-left {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
}

.player-right {
  width: 560px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
}

.player-info-card {
  flex: 0 0 auto;
}

.song-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.song-title-text {
  margin: 0;
  min-width: 0;
}

.download-cute-btn {
  --btn-bg-1: rgba(34, 197, 94, 0.18);
  --btn-bg-2: rgba(16, 185, 129, 0.12);
  --btn-border: rgba(16, 185, 129, 0.42);
  height: 36px;
  border-radius: 999px;
  padding: 0 14px;
  border: 1px solid var(--btn-border);
  background: linear-gradient(135deg, var(--btn-bg-1), var(--btn-bg-2));
  color: #0f766e;
  font-weight: 700;
  letter-spacing: 0.2px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  box-shadow: 0 6px 18px rgba(16, 185, 129, 0.15);
  transition: transform 0.2s ease, box-shadow 0.2s ease, filter 0.2s ease;
}

.download-cute-btn:hover:not(.is-disabled),
.download-cute-btn:focus-visible:not(.is-disabled) {
  transform: translateY(-1px) scale(1.02);
  filter: saturate(1.05);
  box-shadow: 0 10px 22px rgba(16, 185, 129, 0.22);
}

.download-cute-btn:active:not(.is-disabled) {
  transform: translateY(0) scale(0.98);
}

.download-cute-btn.is-disabled {
  opacity: 0.5;
  box-shadow: none;
}

.download-cute-btn__icon {
  font-size: 16px;
  line-height: 1;
  animation: cuteBounce 1.6s ease-in-out infinite;
}

.download-cute-btn__text {
  font-size: 13px;
  line-height: 1;
}

@keyframes cuteBounce {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-2px);
  }
}

.lyric-detail-card {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.lyric-detail-card :deep(.el-card__body) {
  height: calc(100% - 56px);
  overflow: hidden;
}

.lyric-scroll-wrap {
  height: 100%;
  overflow: auto;
  padding-right: 4px;
}

.lyric-line {
  color: #7a8496;
  line-height: 1.75;
  padding: 8px 10px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  transition: color 0.25s ease, background-color 0.25s ease, transform 0.25s ease, box-shadow 0.25s ease;
  cursor: pointer;
  user-select: none;
}

.lyric-text {
  min-width: 0;
  flex: 1;
}

.lyric-side {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #94a3b8;
  font-size: 11px;
  flex: 0 0 auto;
}

.lyric-time {
  line-height: 1;
}

.lyric-line.is-active {
  color: #38bdf8;
  background: linear-gradient(90deg, rgba(56, 189, 248, 0.2), rgba(56, 189, 248, 0.04));
  transform: scale(1.015);
  box-shadow: 0 8px 20px rgba(56, 189, 248, 0.16);
}

.lyric-line.is-playing {
  animation: lyricPulse 1.6s ease-in-out infinite;
}

.lyric-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
  padding: 8px 10px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.26);
  background: linear-gradient(135deg, rgba(251, 113, 133, 0.08), rgba(56, 189, 248, 0.08));
}

.lyric-tool-btn {
  border: 1px solid rgba(56, 189, 248, 0.4);
  background: #ffffff;
  color: #0369a1;
  border-radius: 999px;
  min-height: 30px;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 600;
}

.lyric-font-tools {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
}

.lyric-font-btn {
  width: 28px;
  height: 28px;
  border: 1px solid rgba(148, 163, 184, 0.42);
  background: #ffffff;
  color: #1e293b;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
}

.lyric-font-value {
  min-width: 26px;
  text-align: center;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
}

.lyric-jump-hint {
  font-size: 12px;
  color: #0f766e;
  font-weight: 600;
}

.lyric-wave {
  width: 18px;
  height: 14px;
  display: inline-flex;
  align-items: flex-end;
  gap: 2px;
  opacity: 0.95;
}

.lyric-wave i {
  width: 3px;
  height: 4px;
  border-radius: 999px;
  background: #38bdf8;
  transform-origin: center bottom;
  animation: lyricWave 1s ease-in-out infinite;
}

.lyric-wave i:nth-child(2) {
  animation-delay: 0.15s;
}

.lyric-wave i:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes lyricWave {
  0%,
  100% {
    height: 4px;
    opacity: 0.5;
  }
  50% {
    height: 14px;
    opacity: 1;
  }
}

@keyframes lyricPulse {
  0%,
  100% {
    box-shadow: 0 8px 20px rgba(56, 189, 248, 0.14);
  }
  50% {
    box-shadow: 0 12px 24px rgba(56, 189, 248, 0.24);
  }
}

.effect-card {
  flex: 0 0 260px;
  overflow: hidden;
}

.effect-card :deep(.el-card__body) {
  height: calc(100% - 56px);
  overflow: hidden;
}

.queue-card {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.queue-card :deep(.el-card__body) {
  height: calc(100% - 56px);
  overflow: hidden;
}

.queue-state {
  height: 100%;
}

.queue-state :deep(.el-table) {
  height: 100%;
}

.queue-state :deep(.el-table__inner-wrapper) {
  height: 100%;
}

.queue-state :deep(.el-table__body-wrapper) {
  height: 100%;
  overflow-y: auto;
  overscroll-behavior: contain;
}

.queue-state :deep(.el-table__body-wrapper::-webkit-scrollbar) {
  width: 8px;
}

.queue-state :deep(.el-table__body-wrapper::-webkit-scrollbar-thumb) {
  background: rgba(148, 163, 184, 0.55);
  border-radius: 999px;
}

.queue-state :deep(.el-table__body-wrapper::-webkit-scrollbar-track) {
  background: rgba(148, 163, 184, 0.14);
}

.lyric-detail-card :deep(.el-card__header),
.queue-card :deep(.el-card__header) {
  min-height: 56px;
  display: flex;
  align-items: center;
}

.lyric-detail-header {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.lyric-refresh-btn {
  font-size: 16px;
  color: #64748b;
}

.lyric-refresh-btn:hover {
  color: #0ea5e9;
}

.queue-header {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.comment-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 0;
  height: 100%;
}

.comment-state {
  min-height: 0;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-bottom: 110px;
}

.comment-row {
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.88);
  padding: 10px 12px;
}

.comment-row-content {
  margin: 0;
  color: #1f2937;
  line-height: 1.55;
  font-size: 14px;
}

.comment-row-meta {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.comment-row-like {
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
}

.comment-row-actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.comment-action-btn {
  border: 1px solid rgba(148, 163, 184, 0.34);
  background: #ffffff;
  color: #334155;
  border-radius: 999px;
  min-height: 30px;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 600;
}

.comment-action-btn.danger {
  border-color: rgba(239, 68, 68, 0.28);
  color: #be123c;
  background: #fff1f2;
}

.comment-quick-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.comment-quick-actions::-webkit-scrollbar {
  display: none;
}

.comment-quick-btn {
  border: 1px solid rgba(10, 132, 255, 0.2);
  background: rgba(10, 132, 255, 0.08);
  color: #0a84ff;
  border-radius: 999px;
  min-height: 30px;
  padding: 0 12px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.comment-composer {
  position: sticky;
  bottom: 0;
  z-index: 3;
  margin-top: auto;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  padding: 10px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(8px);
}

.comment-input {
  min-width: 0;
}

.comment-input :deep(.el-textarea__inner) {
  min-height: 72px;
}

.comment-submit-btn {
  min-height: 40px;
  padding-left: 16px;
  padding-right: 16px;
}

.progress-wrap {
  margin-top: 8px;
}

.progress-time {
  color: #64748b;
  font-size: 12px;
  margin-bottom: 6px;
}

.progress-range {
  width: 100%;
}

.player-actions {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.transport-main {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
}

.transport-btn {
  border-width: 0;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.transport-btn.secondary {
  width: 46px;
  height: 46px;
  font-size: 18px;
  color: #334155;
  background: #eef2ff;
}

.transport-btn.primary {
  width: 66px;
  height: 66px;
  font-size: 24px;
  box-shadow: 0 14px 28px rgba(250, 45, 72, 0.28);
}

.transport-btn:hover {
  transform: translateY(-1px);
}

.transport-sub {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.chip-btn {
  width: 100%;
  min-height: 36px;
  padding-left: 10px;
  padding-right: 10px;
}

.chip-btn span + span {
  margin-left: 4px;
}

.mode-symbol {
  font-size: 14px;
  line-height: 1;
}

.detail-switch {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 999px;
  padding: 4px;
  background: rgba(241, 245, 249, 0.7);
}

.detail-switch-btn {
  border: 0;
  min-height: 34px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  color: #475569;
  background: transparent;
}

.detail-switch-btn.active {
  color: #ffffff;
  background: linear-gradient(120deg, #fa2d48, #ff5f70);
  box-shadow: 0 8px 16px rgba(250, 45, 72, 0.24);
}

.song-meta-row {
  margin-top: 2px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.song-meta-time {
  color: #64748b;
  font-size: 12px;
}

.effect-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.effect-card-header {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.effect-label {
  color: #64748b;
  width: 92px;
  font-size: 13px;
}

.triple-slider-wrap {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.dual-slider {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mini-label {
  color: #94a3b8;
  font-size: 12px;
  width: 28px;
}

.eq-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 4px 3px;
  margin-bottom: 8px;
}

.eq-grid-advanced {
  margin-top: 14px;
  gap: 14px 10px;
  /* 给最后一行留出空间，避免被弹窗底部裁切 */
  padding-bottom: 18px;
}

.eq-band {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.eq-label {
  color: #94a3b8;
  font-size: 11px;
}

.eq-band-vivid {
  background: linear-gradient(180deg, #ffffff, #f8fafc);
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 6px 4px 10px;
  box-sizing: border-box;
  transition: transform 0.22s ease, border-color 0.22s ease, box-shadow 0.22s ease;
}

.eq-band-vivid:hover {
  transform: translateY(-2px);
  border-color: #93c5fd;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.12);
}

.eq-band-head {
  color: #334155;
  font-size: 11px;
  font-weight: 600;
  line-height: 1.2;
}

.eq-band-value {
  font-size: 11px;
  color: #64748b;
  line-height: 1.2;
}

.eq-band-value.is-pos {
  color: #22c55e;
}

.eq-band-value.is-neg {
  color: #fb7185;
}

.advanced-eq-overlay {
  position: fixed;
  inset: 0;
  z-index: 3200;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(6px);
  padding: 20px;
}

.advanced-eq-shell {
  width: 100%;
  height: 100%;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: var(--surface);
  box-shadow: var(--shadow);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.advanced-eq-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.7);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95) 0%, rgba(248, 250, 252, 0.9) 100%);
}

.advanced-eq-title {
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
}

.advanced-eq-actions {
  display: flex;
  gap: 8px;
}

.advanced-tab-content {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 14px 20px 24px;
  background: #f8fafc;
}

.advanced-scroll-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  overscroll-behavior: contain;
}

.advanced-primary-layout {
  display: grid;
  grid-template-columns: minmax(360px, 1fr) minmax(640px, 1.6fr);
  gap: 16px;
}

.advanced-scroll-page::-webkit-scrollbar {
  width: 10px;
}

.advanced-scroll-page::-webkit-scrollbar-thumb {
  background: rgba(100, 116, 139, 0.35);
  border-radius: 999px;
}

.advanced-scroll-page::-webkit-scrollbar-track {
  background: rgba(148, 163, 184, 0.12);
}

.module-section {
  border: 1px solid rgba(255, 255, 255, 0.75);
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow);
  padding: 12px;
  backdrop-filter: blur(8px);
}

.advanced-config-section {
  padding: 14px;
  position: relative;
  z-index: 20;
  overflow: visible;
}

.advanced-config-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.advanced-config-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #f8fafc;
  padding: 12px;
  color: #334155;
  position: relative;
  z-index: 21;
  overflow: visible;
}

.advanced-fx-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  align-items: start;
  position: relative;
  z-index: 1;
}

.advanced-fx-grid .module-section {
  height: 100%;
}

.module-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.module-header-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.module-title {
  color: #0f172a;
  font-size: 16px;
  font-weight: 600;
}

.module-help {
  width: 24px;
  height: 24px;
  border-radius: 999px;
  border: 1px solid #dbeafe;
  background: #eff6ff;
  color: #2563eb;
  font-weight: 700;
  cursor: pointer;
}

.module-collapse {
  height: 26px;
  border-radius: 999px;
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
  padding: 0 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.module-collapse:hover {
  border-color: #93c5fd;
  color: #1d4ed8;
  background: #eff6ff;
}

.module-section.is-collapsed {
  padding-bottom: 8px;
}

.module-section.is-collapsed .module-header-row {
  margin-bottom: 2px;
}

.advanced-eq-main {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92) 0%, rgba(239, 246, 255, 0.85) 100%);
  border: 1px solid rgba(255, 255, 255, 0.75);
  border-radius: var(--radius-lg);
  padding: 12px;
}

.advanced-eq-block {
  margin-bottom: 10px;
}

.spectrum-title {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 6px;
}

.spectrum-canvas {
  width: 100%;
  border-radius: 10px;
  border: 1px solid #dbeafe;
  background: linear-gradient(180deg, #ffffff 0%, #eff6ff 100%);
}

.visual-panel {
  border: 1px solid rgba(226, 232, 240, 0.92);
  border-radius: 12px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95) 0%, rgba(248, 250, 252, 0.92) 100%);
  padding: 10px;
  margin-bottom: 10px;
}

.spectrum-canvas-compare {
  height: 260px;
}

.advanced-form-grid {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94) 0%, rgba(248, 250, 252, 0.88) 100%);
  border: 1px solid rgba(255, 255, 255, 0.75);
  border-radius: var(--radius-lg);
  padding: 16px 14px;
}

.advanced-master-grid {
  min-height: 420px;
  display: grid;
  grid-template-rows: auto auto auto 1fr;
  gap: 12px;
}

.advanced-form-grid--compact {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.advanced-fx-grid .advanced-form-grid--compact {
  grid-template-columns: 1fr;
}

.advanced-form-grid--compact .advanced-form-row {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
  margin-bottom: 0;
  border: 1px solid rgba(255, 255, 255, 0.78);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
  padding: 10px 12px;
}

.advanced-form-grid--compact .advanced-form-row--dual {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.reverb-room-row {
  align-items: stretch;
}

.dial-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 72px;
  border: 1px dashed rgba(148, 163, 184, 0.5);
  border-radius: 10px;
  padding: 8px;
  background: rgba(248, 250, 252, 0.92);
  min-width: 0;
}

.dial-label {
  font-size: 12px;
  color: #64748b;
}

.reverb-type-group {
  max-width: 100%;
  display: inline-flex;
  flex-wrap: wrap;
  row-gap: 6px;
}

.advanced-form-grid--compact .advanced-form-row :deep(.el-radio-group.reverb-type-group) {
  width: 100%;
}

.advanced-form-grid--compact .advanced-form-row :deep(.reverb-type-group .el-radio-button) {
  flex: 1 1 92px;
  min-width: 0;
}

.advanced-form-grid--compact .advanced-form-row :deep(.reverb-type-group .el-radio-button__inner) {
  width: 100%;
  padding: 6px 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dial-card--room-size,
.dial-card--room-visual {
  overflow: hidden;
}

.reverb-room-visual {
  position: relative;
  height: 92px;
  border-radius: 10px;
  border: 1px solid rgba(125, 211, 252, 0.55);
  background: radial-gradient(circle at center, rgba(240, 249, 255, 0.95) 0%, rgba(224, 242, 254, 0.86) 50%, rgba(186, 230, 253, 0.56) 100%);
  overflow: hidden;
}

.reverb-room-core {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 46px;
  height: 46px;
  border-radius: 999px;
  background: radial-gradient(circle at 35% 35%, #ffffff 0%, #38bdf8 55%, #0ea5e9 100%);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.reverb-room-ripple {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 34px;
  height: 34px;
  border-radius: 999px;
  border: 1px solid rgba(56, 189, 248, 0.35);
  transform: translate(-50%, -50%) scale(1);
  opacity: 0.35;
  animation: reverbRipplePulse 2.4s ease-out infinite;
}

.reverb-room-ripple--2 {
  animation-delay: 0.6s;
}

.reverb-room-ripple--3 {
  animation-delay: 1.2s;
}

.reverb-room-visual.is-hall .reverb-room-ripple {
  border-color: rgba(16, 185, 129, 0.35);
}

.reverb-room-visual.is-canyon .reverb-room-ripple {
  border-color: rgba(251, 146, 60, 0.38);
}

.reverb-room-tip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
  color: #475569;
}

.reverb-room-tip span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reverb-room-tip strong {
  color: #0284c7;
  font-size: 13px;
}

.reverb-hearing-panel {
  grid-column: 1 / -1;
  border: 1px solid rgba(191, 219, 254, 0.9);
  border-radius: 12px;
  background: linear-gradient(180deg, rgba(239, 246, 255, 0.82) 0%, rgba(248, 250, 252, 0.94) 100%);
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.reverb-hearing-summary {
  font-size: 12px;
  line-height: 1.6;
  color: #334155;
}

.reverb-hearing-chips {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.reverb-hearing-chip {
  border: 1px solid rgba(203, 213, 225, 0.9);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.88);
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.chip-label {
  font-size: 12px;
  color: #475569;
}

.chip-value {
  font-size: 12px;
  color: #0f766e;
  text-align: right;
}

@keyframes reverbRipplePulse {
  0% {
    transform: translate(-50%, -50%) scale(0.8);
    opacity: 0.38;
  }
  70% {
    opacity: 0.12;
  }
  100% {
    transform: translate(-50%, -50%) scale(3.8);
    opacity: 0;
  }
}

.master-param-row {
  align-items: stretch;
}

.master-status-panel {
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: 12px;
  background: linear-gradient(180deg, rgba(239, 246, 255, 0.58) 0%, rgba(248, 250, 252, 0.7) 100%);
  padding: 12px;
}

.master-status-title {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 10px;
}

.master-status-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.master-status-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.9);
  color: #475569;
  font-size: 12px;
}

.spatial-panel {
  overflow: hidden;
}


.spatial-stage-card {
  border: 1px solid rgba(191, 219, 254, 0.9);
  border-radius: 12px;
  background: linear-gradient(180deg, rgba(239, 246, 255, 0.82) 0%, rgba(248, 250, 252, 0.9) 100%);
  padding: 12px;
}

.spatial-stage-title {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 10px;
}

.spatial-stage-map {
  position: relative;
  width: 100%;
  height: 160px;
  border-radius: 10px;
  border: 1px dashed rgba(148, 163, 184, 0.7);
  background: rgba(255, 255, 255, 0.9);
  overflow: hidden;
  cursor: crosshair;
}

.spatial-stage-map.is-disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.stage-axis {
  position: absolute;
  background: rgba(148, 163, 184, 0.45);
}

.stage-axis-x {
  left: 8px;
  right: 8px;
  top: 50%;
  height: 1px;
}

.stage-axis-y {
  top: 8px;
  bottom: 8px;
  left: 50%;
  width: 1px;
}

.stage-point {
  position: absolute;
  width: 16px;
  height: 16px;
  border-radius: 999px;
  transform: translate(-50%, -50%);
  background: radial-gradient(circle at 35% 35%, #ffffff 0%, #38bdf8 55%, #0ea5e9 100%);
  box-shadow: 0 0 0 5px rgba(56, 189, 248, 0.2), 0 6px 18px rgba(14, 165, 233, 0.35);
  cursor: grab;
  user-select: none;
}

.stage-point.is-dragging {
  cursor: grabbing;
  box-shadow: 0 0 0 8px rgba(56, 189, 248, 0.26), 0 10px 24px rgba(14, 165, 233, 0.42);
}

.spatial-value-row {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #475569;
  font-size: 12px;
}

.advanced-form-grid--compact .advanced-form-row > span {
  font-size: 13px;
  color: #475569;
}

.advanced-form-grid--compact .advanced-form-row :deep(.el-slider) {
  width: 100%;
}

.advanced-form-row {
  display: grid;
  grid-template-columns: 190px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  color: #334155;
}

.advanced-form-row:last-child {
  margin-bottom: 0;
}

.help-dialog-content {
  line-height: 1.8;
  color: #475569;
}

.eq-zoom-enter-active,
.eq-zoom-leave-active {
  transition: all 0.3s ease;
}

.eq-zoom-enter-from,
.eq-zoom-leave-to {
  opacity: 0;
  transform: scale(0.88);
}

.pitch-value {
  width: 30px;
  text-align: right;
  color: #38bdf8;
}

.pitch-stepper {
  flex: 1;
  display: flex;
  gap: 6px;
}

.pitch-input {
  width: 54px;
}

.pitch-stepper :deep(.el-input__wrapper) {
  padding: 0 8px;
}

.pitch-stepper :deep(.el-input__inner) {
  text-align: center;
}

:deep(.queue-current-row .el-table__cell) {
  background: linear-gradient(90deg, rgba(56, 189, 248, 0.18), rgba(56, 189, 248, 0.04));
  transition: background-color 0.25s ease, box-shadow 0.25s ease, transform 0.25s ease;
}

:deep(.queue-current-row) {
  position: relative;
}

:deep(.queue-current-row .el-table__cell:first-child) {
  position: relative;
  overflow: hidden;
}

:deep(.queue-current-row .el-table__cell:first-child)::before {
  content: '';
  position: absolute;
  left: 6px;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 60%;
  border-radius: 999px;
  background: rgba(56, 189, 248, 0.9);
  box-shadow: 0 0 10px rgba(56, 189, 248, 0.5);
}

:deep(.queue-current-playing .el-table__cell) {
  animation: queueRowPulse 1.6s ease-in-out infinite;
}

:deep(.queue-current-playing .el-table__cell:first-child)::before {
  animation: queuePlayingBar 1s ease-in-out infinite;
}

@keyframes queueRowPulse {
  0%,
  100% {
    box-shadow: inset 0 0 0 rgba(56, 189, 248, 0);
  }
  50% {
    box-shadow: inset 0 0 26px rgba(56, 189, 248, 0.14);
  }
}

@keyframes queuePlayingBar {
  0%,
  100% {
    height: 50%;
    opacity: 0.7;
  }
  50% {
    height: 78%;
    opacity: 1;
  }
}

@media (max-width: 960px) {
  .player-layout {
    flex-direction: column;
    height: auto;
    min-height: 0;
    max-height: none;
    overflow: visible;
    gap: 12px;
  }

  .player-right {
    width: 100%;
    gap: 12px;
  }

  .player-left {
    gap: 12px;
  }

  .song-title-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .download-cute-btn {
    width: 100%;
    justify-content: center;
  }

  .player-actions {
    position: sticky;
    bottom: calc(58px + env(safe-area-inset-bottom));
    z-index: 12;
    padding: 10px;
    border-radius: 14px;
    border: 1px solid rgba(148, 163, 184, 0.26);
    background: rgba(255, 255, 255, 0.94);
    backdrop-filter: blur(10px);
    box-shadow: 0 14px 28px rgba(15, 23, 42, 0.12);
  }

  .transport-sub {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .chip-btn {
    min-height: 38px;
    font-size: 12px;
  }

  .detail-switch-btn {
    min-height: 36px;
  }

  .effect-row {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }

  .effect-label {
    width: auto;
  }

  .triple-slider-wrap {
    grid-template-columns: 1fr;
  }

  .eq-grid {
    grid-template-columns: repeat(6, minmax(0, 1fr));
    gap: 8px 8px;
  }

  .lyric-toolbar {
    align-items: flex-start;
  }

  .lyric-font-tools {
    margin-left: 0;
  }

  .lyric-scroll-wrap {
    max-height: min(52vh, 420px);
    padding-bottom: 6px;
  }

  .comment-composer {
    bottom: calc(58px + env(safe-area-inset-bottom));
  }

  .comment-list {
    padding-bottom: 132px;
  }

  .lyric-detail-card :deep(.el-card__body),
  .effect-card :deep(.el-card__body),
  .queue-card :deep(.el-card__body) {
    height: auto;
  }

  .queue-state :deep(.el-table__body-wrapper) {
    max-height: 260px;
  }

  .advanced-eq-overlay {
    padding: 8px;
  }

  .advanced-eq-topbar {
    padding: 10px 12px;
  }

  .advanced-eq-title {
    font-size: 16px;
  }

  .advanced-tab-content {
    padding: 10px 12px 18px;
  }
}

@media (max-width: 1200px) {
  .advanced-primary-layout {
    grid-template-columns: 1fr;
  }

  .advanced-fx-grid {
    grid-template-columns: 1fr;
  }

  .advanced-config-row {
    grid-template-columns: 1fr;
  }

  .advanced-form-grid--compact {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .advanced-form-row {
    grid-template-columns: 1fr;
  }

  .master-status-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 860px) {
  .advanced-form-grid--compact {
    grid-template-columns: 1fr;
  }

  .advanced-form-grid--compact .advanced-form-row--dual {
    grid-template-columns: 1fr;
  }

  .reverb-hearing-chips {
    grid-template-columns: 1fr;
  }

  .spatial-control-grid {
    grid-template-columns: 1fr;
  }

  .spatial-control-head {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
