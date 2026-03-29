<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { usePlayerStore } from '@/stores/player'
import { playerAudio } from '@/utils/playerAudio'
import { getDisplaySongTitle } from '@/utils/songTitle'
import { getSearchSuggestionsApi } from '@/api/music'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const player = usePlayerStore()

const menus = [
  { path: '/home', label: '首页' },
  { path: '/discover', label: '发现' },
  { path: '/library', label: '歌单' },
  { path: '/player', label: '播放器' },
  { path: '/profile', label: '我的' },
  { path: '/settings', label: '设置' },
  { path: '/feedback', label: '反馈' },
  { path: '/lyrics-share', label: '歌词分享' }
]

const go = (path: string) => {
  closeMoreMenu()
  router.push(path)
}
const headerKeyword = ref('')
const headerSuggesting = ref(false)
const isMoreMenuOpen = ref(false)

const viewportWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1440)

const getPrimaryMenuCount = () => {
  const w = viewportWidth.value
  if (w >= 1480) return menus.length
  if (w >= 1320) return 7
  if (w >= 1200) return 6
  if (w >= 1024) return 5
  return 4
}

const primaryMenus = computed(() => menus.slice(0, getPrimaryMenuCount()))
const overflowMenus = computed(() => menus.slice(getPrimaryMenuCount()))
const overflowMenuActive = computed(() => overflowMenus.value.some((item) => route.path.startsWith(item.path)))

const openMoreMenu = () => {
  if (!overflowMenus.value.length) return
  if (moreMenuCloseTimer) {
    window.clearTimeout(moreMenuCloseTimer)
    moreMenuCloseTimer = null
  }
  isMoreMenuOpen.value = true
}

const toggleMoreMenu = () => {
  if (!overflowMenus.value.length) return
  isMoreMenuOpen.value = !isMoreMenuOpen.value
}

let moreMenuCloseTimer: number | null = null

const closeMoreMenu = () => {
  if (moreMenuCloseTimer) {
    window.clearTimeout(moreMenuCloseTimer)
    moreMenuCloseTimer = null
  }
  isMoreMenuOpen.value = false
}

const scheduleCloseMoreMenu = () => {
  if (moreMenuCloseTimer) window.clearTimeout(moreMenuCloseTimer)
  moreMenuCloseTimer = window.setTimeout(() => {
    isMoreMenuOpen.value = false
    moreMenuCloseTimer = null
  }, 120)
}

const goSearch = (keyword?: string) => {
  const kw = String(keyword ?? headerKeyword.value ?? '').trim()
  router.push({ path: '/search', query: kw ? { keyword: kw } : {} })
}

const queryHeaderSuggestions = async (queryString: string, cb: (items: any[]) => void) => {
  const q = String(queryString || '').trim()
  if (!q) {
    cb([])
    return
  }
  headerSuggesting.value = true
  try {
    const list = await getSearchSuggestionsApi(q, 10)
    cb(Array.isArray(list) ? list : [])
  } catch {
    cb([])
  } finally {
    headerSuggesting.value = false
  }
}

const getHeaderSuggestionTitle = (item: any) => getDisplaySongTitle(item)

const getHeaderSuggestionArtist = (item: any) => {
  const artist = String(item?.artistName || item?.artistNames || '').trim()
  return artist || '未知歌手'
}

const onHeaderSuggestionSelect = (item: any) => {
  const name = getHeaderSuggestionTitle(item)
  if (!name) return
  headerKeyword.value = name
  goSearch(name)
}

const onHeaderSearchEnter = () => {
  const kw = headerKeyword.value.trim()
  if (!kw) return
  goSearch(kw)
}

const logout = () => {
  user.logout()
  router.push('/login')
}

const hasSong = computed(() => Number(player.currentSongId || 0) > 0)
const isPlaying = computed(() => !!player.playing)
const currentSongTitle = computed(() => {
  if (!hasSong.value) return '未选择歌曲'
  const s = player.currentSong
  return s ? getDisplaySongTitle(s) : '未选择歌曲'
})

const currentSongArtistTitle = computed(() => {
  if (!hasSong.value) return '未选择歌曲'
  const s: any = player.currentSong
  const artist = s?.artistName || s?.artistNames || '未知歌手'
  const title = currentSongTitle.value
  if (!title || title === '未选择歌曲') return '未选择歌曲'
  return `${artist}-${title}`
})

const shouldMarquee = computed(() => currentSongArtistTitle.value.length > 12)

const streamSrcBySongId = (songId: number) => `/api/app/music/song/${songId}/stream`

const togglePlayback = async () => {
  if (!hasSong.value) return
  try {
    if (playerAudio.paused) {
      await playerAudio.play()
      player.playing = true
    } else {
      playerAudio.pause()
      player.playing = false
    }
  } catch {
    player.playing = false
  }
}

const syncAudioSrcForSong = async (songId: number) => {
  if (!songId) return
  const targetSrc = streamSrcBySongId(songId)
  const sameSource = !!playerAudio.src && playerAudio.src.includes(targetSrc)
  if (!sameSource) {
    playerAudio.src = targetSrc
    playerAudio.load()
    return
  }
  playerAudio.load()
}

const playPrev = async () => {
  if (!hasSong.value) return
  await player.prev()

  // If PlayerView isn't mounted, update stream manually.
  // (PlayerView will also react to store changes when mounted.)
  if (!route.path.startsWith('/player')) {
    const id = Number(player.currentSongId || 0)
    if (!id) return
    await syncAudioSrcForSong(id)
    if (player.playing) {
      try {
        await playerAudio.play()
      } catch {
        player.playing = false
      }
    } else {
      playerAudio.pause()
    }
  }
}

const playNext = async () => {
  if (!hasSong.value) return
  await player.next()

  // If PlayerView isn't mounted, update stream manually.
  if (!route.path.startsWith('/player')) {
    const id = Number(player.currentSongId || 0)
    if (!id) return
    await syncAudioSrcForSong(id)
    if (player.playing) {
      try {
        await playerAudio.play()
      } catch {
        player.playing = false
      }
    } else {
      playerAudio.pause()
    }
  }
}

const goCurrentPlayer = () => {
  const id = Number(player.currentSongId || 0)
  if (id > 0) {
    router.push(`/player/${id}`)
    return
  }
  router.push('/player')
}

const AUTO_RESUME_ON_RELOAD_KEY = 'music-web:auto-resume-on-reload'
const RELOAD_PROGRESS_SNAPSHOT_KEY = 'music-web:reload-progress-snapshot'

const readReloadSnapshot = () => {
  try {
    const raw = window.localStorage.getItem(RELOAD_PROGRESS_SNAPSHOT_KEY)
    if (!raw) return { songId: 0, currentTime: 0 }
    const parsed = JSON.parse(raw)
    const songId = Number(parsed?.songId || 0)
    const currentTime = Number(parsed?.currentTime || 0)
    if (songId > 0 && Number.isFinite(currentTime) && currentTime >= 0) {
      return { songId, currentTime }
    }
  } catch {
    // ignore
  }
  return { songId: 0, currentTime: 0 }
}

const readAutoResumeOnReload = () => {
  try {
    return window.localStorage.getItem(AUTO_RESUME_ON_RELOAD_KEY) === '1'
  } catch {
    return false
  }
}

const setAutoResumeOnReload = (playing: boolean) => {
  try {
    window.localStorage.setItem(AUTO_RESUME_ON_RELOAD_KEY, playing ? '1' : '0')
  } catch {
    // ignore
  }
}

const saveReloadProgressSnapshot = () => {
  try {
    const currentSongId = Number(player.currentSongId || player.queue?.[player.currentIndex] || 0)
    if (!currentSongId) return
    const currentTime = Number.isFinite(playerAudio.currentTime) ? playerAudio.currentTime : Number(player.currentTime || 0)
    const payload = { songId: currentSongId, currentTime: Math.max(currentTime, 0) }
    window.localStorage.setItem(RELOAD_PROGRESS_SNAPSHOT_KEY, JSON.stringify(payload))
  } catch {
    // ignore storage errors
  }
}

let shouldRegisterBeforeUnload = false
const onGlobalPointerDown = (event: PointerEvent) => {
  const target = event.target as HTMLElement | null
  if (!target) return
  if (target.closest('.more-menu-wrap')) return
  closeMoreMenu()
}

const onResize = () => {
  viewportWidth.value = window.innerWidth
  if (overflowMenus.value.length === 0) closeMoreMenu()
}

const onBeforeUnload = () => {
  if (route.path.startsWith('/player')) return
  if (!shouldRegisterBeforeUnload) return
  // When on /player, PlayerView already persists this snapshot.
  setAutoResumeOnReload(!playerAudio.paused && !playerAudio.ended)
  saveReloadProgressSnapshot()
}

onMounted(async () => {
  onResize()
  window.addEventListener('resize', onResize)

  if (route.path === '/login') return

  const isPlayerPage = route.path.startsWith('/player')
  shouldRegisterBeforeUnload = !isPlayerPage

  // Only non-`/player` pages need global persistence.
  if (isPlayerPage) return

  window.addEventListener('beforeunload', onBeforeUnload)
  window.addEventListener('pointerdown', onGlobalPointerDown)

  const snapshot = readReloadSnapshot()
  const shouldResumeAfterReload = readAutoResumeOnReload()
  if (!snapshot.songId) return

  // Refresh wipes memory store; best-effort re-hydrate so PlayerView can render.
  try {
    await player.hydrateFromServer()
  } catch {
    // ignore
  }

  // Align store to the saved songId (hydrateFromServer should already do this, but keep it safe).
  if (player.queue?.length) {
    const idx = player.queue.findIndex((id) => Number(id) === Number(snapshot.songId))
    if (idx >= 0) {
      player.currentIndex = idx
      player.currentSongId = player.queue[idx] || 0
      await player.refreshQueueSongs()
    } else {
      try {
        await player.playBySongId(snapshot.songId)
      } catch {
        // ignore
      }
    }
  }

  const targetSrc = `/api/app/music/song/${snapshot.songId}/stream`
  const sameSource = !!playerAudio.src && playerAudio.src.includes(targetSrc)
  if (!sameSource) {
    playerAudio.src = targetSrc
    playerAudio.load()
  } else {
    playerAudio.load()
  }

  // Wait metadata so we can clamp currentTime safely.
  const applyPendingTime = () => {
    const d = playerAudio.duration
    if (!Number.isFinite(d) || d <= 0) return false
    const clamped = Math.min(Math.max(snapshot.currentTime, 0), Math.max(d - 0.2, 0))
    playerAudio.currentTime = clamped
    player.currentTime = clamped
    return true
  }

  let applied = false
  if (applyPendingTime()) applied = true
  if (!applied) {
    await new Promise<void>((resolve) => {
      const timeout = window.setTimeout(() => {
        cleanup()
        resolve()
      }, 4000)
      const onReady = () => {
        if (applyPendingTime()) {
          cleanup()
          resolve()
        }
      }
      const cleanup = () => {
        window.clearTimeout(timeout)
        playerAudio.removeEventListener('loadedmetadata', onReady)
        playerAudio.removeEventListener('durationchange', onReady)
      }
      playerAudio.addEventListener('loadedmetadata', onReady)
      playerAudio.addEventListener('durationchange', onReady)
    })
  }

  if (shouldResumeAfterReload) {
    try {
      await playerAudio.play()
      player.playing = true
    } catch {
      player.playing = false
    }
  } else {
    player.playing = false
    playerAudio.pause()
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  window.removeEventListener('pointerdown', onGlobalPointerDown)
  if (shouldRegisterBeforeUnload) {
    window.removeEventListener('beforeunload', onBeforeUnload)
  }
})

watch(
  () => player.currentSongId,
  async (id: number) => {
    // 播放器页由 PlayerView 接管音频 src 与自动播放，避免双端抢控制权。
    if (route.path.startsWith('/player')) return
    const targetId = Number(id || 0)
    if (!targetId) return
    await syncAudioSrcForSong(targetId)
    if (player.playing) {
      try {
        await playerAudio.play()
      } catch {
        player.playing = false
      }
    } else {
      playerAudio.pause()
    }
  }
)
</script>

<template>
  <div v-if="route.path !== '/login'" class="layout">
    <header class="top glow-card">
      <div class="left-bar">
        <div class="brand-block" @click="go('/home')">
          <div class="brand-title">Mymusic</div>
          <div class="brand-subtitle">沉浸式发现你的下一首心动单曲</div>
        </div>

        <div class="search-shell">
          <el-autocomplete
            v-model="headerKeyword"
            class="quick-search"
            clearable
            :fetch-suggestions="queryHeaderSuggestions"
            :trigger-on-focus="false"
            :debounce="220"
            :loading="headerSuggesting"
            placeholder="搜索歌曲/歌手/专辑"
            @select="onHeaderSuggestionSelect"
            @keyup.enter="onHeaderSearchEnter"
          >
            <template #default="{ item }">
              <div class="header-suggestion-option">
                <span class="header-suggestion-name">{{ getHeaderSuggestionTitle(item) }}</span>
                <span class="header-suggestion-artist">{{ getHeaderSuggestionArtist(item) }}</span>
              </div>
            </template>
          </el-autocomplete>
          <button class="search-btn" @click="onHeaderSearchEnter">搜索</button>
        </div>
      </div>

      <nav class="nav-wrap center-nav">
        <button
          v-for="item in primaryMenus"
          :key="item.path"
          class="nav-btn"
          :class="{ active: route.path.startsWith(item.path) }"
          @click="go(item.path)"
        >
          {{ item.label }}
        </button>

        <div
          class="more-menu-wrap"
          v-if="overflowMenus.length"
          @mouseenter="openMoreMenu"
          @mouseleave="scheduleCloseMoreMenu"
        >
          <button class="nav-btn more-btn" :class="{ active: overflowMenuActive }" @click.stop="toggleMoreMenu">更多</button>
          <div class="more-menu-dropdown" :class="{ open: isMoreMenuOpen }">
            <button
              v-for="item in overflowMenus"
              :key="item.path"
              class="more-menu-item"
              :class="{ active: route.path.startsWith(item.path) }"
              @click="go(item.path)"
            >
              {{ item.label }}
            </button>
          </div>
        </div>
      </nav>

      <div class="mini-player" :class="{ disabled: !hasSong }">
        <button class="mini-icon-btn" :disabled="!hasSong" title="上一首" @click="playPrev">⏮</button>
        <button class="mini-play-btn" :disabled="!hasSong" :title="isPlaying ? '暂停播放' : '播放'" @click="togglePlayback">
          {{ isPlaying ? '⏸' : '▶' }}
        </button>
        <button class="mini-icon-btn" :disabled="!hasSong" title="下一首" @click="playNext">⏭</button>
        <div class="mini-title-wrap" :class="{ clickable: hasSong }" @click="goCurrentPlayer">
          <div v-if="hasSong" class="mini-title-marquee" :class="{ 'marquee-on': shouldMarquee }" :title="currentSongArtistTitle">
            <span class="mini-title-text">{{ currentSongArtistTitle }}</span>
          </div>
          <div v-else class="mini-title-empty">未选择歌曲</div>
        </div>
      </div>

      <div class="user-bar">
        <span class="user-name">{{ user.userInfo?.nickname || user.userInfo?.username }}</span>
        <button class="nav-btn logout" @click="logout">退出</button>
      </div>
    </header>
    <main class="container"><router-view /></main>
  </div>
  <router-view v-else />
</template>

<style scoped>
.layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.container {
  flex: 1 1 auto;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 16px 20px 20px;
  overflow: hidden;
  box-sizing: border-box;
}

.top {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: nowrap;
  overflow: visible;
  padding: 12px 20px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.28);
  position: sticky;
  top: 0;
  z-index: 10;
  background: rgba(255, 255, 255, 0.84);
  backdrop-filter: blur(10px);
}

.top.glow-card::after {
  inset: 0;
}

.left-bar {
  flex: 0 1 auto;
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.brand-block {
  display: flex;
  flex-direction: column;
  gap: 2px;
  cursor: pointer;
  user-select: none;
}

.brand-title {
  font-weight: 800;
  font-size: 20px;
  line-height: 1;
  letter-spacing: 0.2px;
  background: linear-gradient(135deg, #ff7fb2, #60a5fa 55%, #7dd3fc);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.brand-subtitle {
  font-size: 11px;
  color: #7c5f8a;
  white-space: nowrap;
}

.nav-wrap {
  display: flex;
  flex-wrap: nowrap;
  gap: 6px;
  min-width: 0;
  white-space: nowrap;
}

.center-nav {
  flex: 1 1 auto;
  min-width: 0;
  justify-content: center;
  overflow: visible;
}

.nav-btn {
  border: 1px solid rgba(244, 114, 182, 0.45);
  background: #ffffff;
  color: #ff5a8a;
  padding: 5px 10px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  flex: 0 0 auto;
}

.nav-btn:hover,
.nav-btn.active {
  background: linear-gradient(135deg, #ff8fb8, #7dd3fc);
  color: #ffffff;
  border-color: transparent;
  box-shadow: 0 8px 18px rgba(125, 211, 252, 0.3);
}

.more-menu-wrap {
  position: relative;
  display: flex;
  align-items: center;
  padding-bottom: 8px;
  margin-bottom: -8px;
}

.more-menu-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  min-width: 140px;
  padding: 8px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba(148, 163, 184, 0.28);
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.12);
  display: flex;
  flex-direction: column;
  gap: 6px;
  opacity: 0;
  transform: translateY(-6px);
  pointer-events: none;
  transition: all 0.18s ease;
  z-index: 20;
}

.more-menu-wrap:hover .more-menu-dropdown,
.more-menu-dropdown.open {
  opacity: 1;
  transform: translateY(0);
  pointer-events: auto;
}

.more-menu-item {
  border: 1px solid transparent;
  background: rgba(241, 245, 249, 0.7);
  color: #0f172a;
  font-size: 13px;
  padding: 6px 10px;
  border-radius: 10px;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.more-menu-item:hover,
.more-menu-item.active {
  background: linear-gradient(135deg, #ff8fb8, #7dd3fc);
  color: #ffffff;
  border-color: transparent;
}

.user-bar {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: max-content;
}

.mini-player {
  flex: 0 0 292px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  background: rgba(255, 255, 255, 0.68);
  min-width: 0;
}

.mini-player.disabled {
  opacity: 0.6;
}

.mini-icon-btn {
  border: 1px solid rgba(244, 114, 182, 0.35);
  background: #ffffff;
  color: #ff5a8a;
  width: 28px;
  height: 28px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.mini-icon-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #ff8fb8, #7dd3fc);
  color: #ffffff;
  border-color: transparent;
  box-shadow: 0 8px 18px rgba(125, 211, 252, 0.25);
}

.mini-play-btn {
  border: 1px solid transparent;
  background: linear-gradient(135deg, #ff8fb8, #7dd3fc);
  color: #ffffff;
  width: 30px;
  height: 30px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 600;
  font-size: 14px;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.mini-play-btn:hover:not(:disabled) {
  filter: brightness(1.02);
  box-shadow: 0 10px 22px rgba(125, 211, 252, 0.2);
}

.mini-title-wrap {
  display: flex;
  align-items: center;
  min-width: 0;
  flex: 1 1 auto;
  max-width: none;
  justify-content: flex-start;
}

.mini-title-wrap.clickable {
  cursor: pointer;
}

.mini-title-empty {
  color: #5f6b7a;
  font-size: 12px;
  white-space: nowrap;
}

.mini-title-marquee {
  overflow: hidden;
  white-space: nowrap;
  width: 100%;
  min-width: 0;
  font-size: 12px;
  color: #1f2937;
  text-overflow: ellipsis;
}

.mini-title-text {
  display: inline-block;
  padding-left: 0;
}

.mini-title-marquee.marquee-on {
  text-overflow: clip;
}

.mini-title-marquee.marquee-on .mini-title-text {
  padding-left: 100%;
  animation: mini-marquee 10s linear infinite;
}

@keyframes mini-marquee {
  0% {
    transform: translateX(0);
  }
  100% {
    transform: translateX(-100%);
  }
}

.user-name {
  color: #5f6b7a;
  font-size: 14px;
}

.search-shell {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(255, 143, 184, 0.18), rgba(125, 211, 252, 0.2));
  box-shadow: 0 10px 24px rgba(125, 211, 252, 0.16);
}

.quick-search {
  width: 250px !important;
  max-width: 250px !important;
  min-width: 180px !important;
  flex: 1 1 250px;
}

.quick-search :deep(.el-input__wrapper) {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 0 0 1px rgba(244, 114, 182, 0.2) inset;
  transition: all 0.2s;
  padding-left: 12px;
  padding-right: 12px;
}

.quick-search :deep(.el-input__inner) {
  color: #334155;
  font-size: 13px;
}

.quick-search :deep(.el-input__wrapper:hover),
.quick-search :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px rgba(125, 211, 252, 0.75) inset, 0 10px 22px rgba(125, 211, 252, 0.2);
}

.search-btn {
  border: 1px solid rgba(244, 114, 182, 0.38);
  background: #ffffff;
  color: #ff5a8a;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.2px;
  padding: 7px 14px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  word-break: keep-all;
}

.search-btn:hover {
  background: linear-gradient(135deg, #ff8fb8, #7dd3fc);
  color: #ffffff;
  border-color: transparent;
  box-shadow: 0 8px 18px rgba(125, 211, 252, 0.3);
}

.header-suggestion-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.header-suggestion-name {
  color: #111827;
}

.header-suggestion-artist {
  color: #94a3b8;
  font-size: 12px;
}

.logout {
  padding-left: 12px;
  padding-right: 12px;
}

@media (max-width: 1360px) {
  .brand-subtitle {
    max-width: 160px;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .quick-search {
    width: 220px !important;
    max-width: 220px !important;
    min-width: 150px !important;
    flex: 1 1 220px;
  }

  .quick-search :deep(.el-input__wrapper) {
    width: 100%;
    max-width: 100%;
    min-width: 0;
  }
}

@media (max-width: 1180px) {
  .brand-subtitle {
    display: none;
  }

  .left-bar {
    gap: 10px;
    min-width: 0;
  }

  .search-shell {
    min-width: 0;
    max-width: 250px;
  }

  .quick-search {
    width: 190px !important;
    max-width: 190px !important;
    min-width: 120px !important;
    flex: 1 1 190px;
  }

  .quick-search :deep(.el-input__wrapper) {
    width: 100%;
    max-width: 100%;
    min-width: 0;
  }

  .search-btn {
    padding: 7px 12px;
  }

  .mini-player,
  .user-bar {
    display: none;
  }

  .center-nav {
    justify-content: flex-start;
    overflow: visible;
  }

  .more-menu-wrap {
    margin-left: 2px;
  }
}

@media (max-width: 1024px) {
  .brand-title {
    font-size: 18px;
  }

  .search-shell {
    max-width: 210px;
  }
}

@media (max-width: 880px) {
  .top {
    gap: 8px;
    padding: 10px 12px;
  }

  .brand-title {
    font-size: 18px;
  }

  .search-shell {
    max-width: 190px;
  }

  .search-btn {
    padding: 6px 10px;
    font-size: 11px;
  }
}
</style>
