<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { usePlayerStore } from '@/stores/player'
import { getSearchSuggestionsApi } from '@/api/music'
import { playerAudio } from '@/utils/playerAudio'
import { getDisplaySongTitle } from '@/utils/songTitle'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const player = usePlayerStore()

const tabMenus = [
  { path: '/home', label: 'Home' },
  { path: '/discover', label: 'Discover' },
  { path: '/library', label: 'Library' },
  { path: '/player', label: 'Player' },
  { path: '/profile', label: 'Profile' }
]

const moreMenus = [
  { path: '/search', label: 'Search' },
  { path: '/artist-rank', label: 'Artist Rank' },
  { path: '/settings', label: 'Settings' },
  { path: '/feedback', label: 'Feedback' },
  { path: '/lyrics-share', label: 'Lyrics Share' }
]

const headerKeyword = ref('')
const headerSuggesting = ref(false)
const showMoreSheet = ref(false)

const isLoginPage = computed(() => route.path === '/login')
const isPlayerPage = computed(() => route.path.startsWith('/player'))
const showMiniPlayer = computed(() => !isLoginPage.value && !isPlayerPage.value)
const moreMenuActive = computed(() => moreMenus.some((item) => route.path.startsWith(item.path)))

const hasSong = computed(() => Number(player.currentSongId || 0) > 0)
const isPlaying = computed(() => !!player.playing)
const currentSongTitle = computed(() => {
  if (!hasSong.value) return 'No song selected'
  return player.currentSong ? getDisplaySongTitle(player.currentSong) : 'No song selected'
})
const currentSongArtist = computed(() => {
  if (!hasSong.value) return 'Tap a song to start'
  const song = player.currentSong as any
  return String(song?.artistName || song?.artistNames || 'Unknown artist')
})
const currentSongLabel = computed(() => {
  if (!hasSong.value) return 'No song selected'
  return `${currentSongArtist.value} - ${currentSongTitle.value}`
})

const progressPercent = computed(() => {
  const d = Number.isFinite(playerAudio.duration) && playerAudio.duration > 0 ? playerAudio.duration : 0
  const t = Number.isFinite(player.currentTime) ? player.currentTime : 0
  if (!d) return 0
  return Math.max(0, Math.min(100, (t / d) * 100))
})

const isTabActive = (path: string) => route.path.startsWith(path)

const streamSrcBySongId = (songId: number) => `/api/app/music/song/${songId}/stream`

const closeMoreSheet = () => {
  showMoreSheet.value = false
}

const go = (path: string) => {
  closeMoreSheet()
  router.push(path)
}

const goSearch = (keyword?: string) => {
  const kw = String(keyword ?? headerKeyword.value ?? '').trim()
  closeMoreSheet()
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
  return artist || 'Unknown artist'
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

const syncAudioSrcForSong = async (songId: number) => {
  if (!songId) return
  const targetSrc = streamSrcBySongId(songId)
  const sameSource = !!playerAudio.src && playerAudio.src.includes(targetSrc)
  if (!sameSource) {
    playerAudio.src = targetSrc
  }
  playerAudio.load()
}

const syncPlaybackFromAudio = () => {
  player.playing = !playerAudio.paused && !playerAudio.ended
}

const tryAutoPlay = async () => {
  try {
    await playerAudio.play()
    player.playing = true
  } catch {
    player.playing = false
  }
}

const togglePlayback = async () => {
  if (!hasSong.value) return
  const id = Number(player.currentSongId || 0)
  if (!id) return

  await syncAudioSrcForSong(id)

  if (playerAudio.paused || playerAudio.ended) {
    await tryAutoPlay()
    return
  }

  playerAudio.pause()
  player.playing = false
}

const playPrev = async () => {
  if (!hasSong.value) return
  await player.prev()

  const id = Number(player.currentSongId || 0)
  if (!id) return
  await syncAudioSrcForSong(id)

  if (player.playing) {
    await tryAutoPlay()
  } else {
    playerAudio.pause()
  }
}

const playNext = async () => {
  if (!hasSong.value) return
  await player.next()

  const id = Number(player.currentSongId || 0)
  if (!id) return
  await syncAudioSrcForSong(id)

  if (player.playing) {
    await tryAutoPlay()
  } else {
    playerAudio.pause()
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

const onAudioTimeUpdate = () => {
  player.currentTime = Number.isFinite(playerAudio.currentTime) ? playerAudio.currentTime : 0
}

const onAudioEnded = async () => {
  if (isPlayerPage.value) return

  const shouldContinue = await player.nextByMode()
  if (!shouldContinue) {
    player.playing = false
    player.currentTime = 0
    return
  }

  if (player.playMode === 'single') {
    playerAudio.currentTime = 0
    player.currentTime = 0
  }

  await tryAutoPlay()
}

const onGlobalKeyDown = (event: KeyboardEvent) => {
  if (event.key === 'Escape') closeMoreSheet()
}

const logout = () => {
  user.logout()
  closeMoreSheet()
  router.push('/login')
}

onMounted(async () => {
  window.addEventListener('keydown', onGlobalKeyDown)

  if (isLoginPage.value) return

  try {
    await player.hydrateFromServer()
  } catch {
    // ignore hydration errors
  }

  const id = Number(player.currentSongId || 0)
  if (id && !isPlayerPage.value) {
    await syncAudioSrcForSong(id)
    if (player.playing) {
      await tryAutoPlay()
    }
  }

  playerAudio.addEventListener('timeupdate', onAudioTimeUpdate)
  playerAudio.addEventListener('play', syncPlaybackFromAudio)
  playerAudio.addEventListener('pause', syncPlaybackFromAudio)
  playerAudio.addEventListener('ended', onAudioEnded)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onGlobalKeyDown)
  playerAudio.removeEventListener('timeupdate', onAudioTimeUpdate)
  playerAudio.removeEventListener('play', syncPlaybackFromAudio)
  playerAudio.removeEventListener('pause', syncPlaybackFromAudio)
  playerAudio.removeEventListener('ended', onAudioEnded)
})

watch(
  () => route.path,
  async () => {
    closeMoreSheet()
    if (!isPlayerPage.value) {
      const id = Number(player.currentSongId || 0)
      if (id) await syncAudioSrcForSong(id)
    }
  }
)

watch(
  () => player.currentSongId,
  async (id) => {
    const targetId = Number(id || 0)
    if (!targetId || isPlayerPage.value) return
    await syncAudioSrcForSong(targetId)

    if (player.playing) {
      await tryAutoPlay()
    } else {
      playerAudio.pause()
    }
  }
)
</script>

<template>
  <div v-if="!isLoginPage" class="mobile-shell">
    <header class="mobile-header glow-card">
      <div class="brand" @click="go('/home')">
        <div class="brand-title">MyMusic</div>
        <div class="brand-subtitle">Phone Player</div>
      </div>

      <div class="header-search">
        <el-autocomplete
          v-model="headerKeyword"
          class="quick-search"
          clearable
          :fetch-suggestions="queryHeaderSuggestions"
          :trigger-on-focus="false"
          :debounce="220"
          :loading="headerSuggesting"
          placeholder="Search songs or artists"
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
        <button class="header-action-btn" @click="onHeaderSearchEnter">Search</button>
      </div>
    </header>

    <main class="mobile-main">
      <router-view />
    </main>

    <div v-if="showMiniPlayer" class="mini-player" :class="{ disabled: !hasSong }">
      <div class="mini-top" :class="{ clickable: hasSong }" @click="goCurrentPlayer">
        <div class="mini-title" :title="currentSongLabel">{{ currentSongLabel }}</div>
        <div class="mini-subtitle">{{ hasSong ? currentSongArtist : 'Open Home or Discover to select music' }}</div>
      </div>
      <div class="mini-controls">
        <button class="mini-icon-btn" :disabled="!hasSong" @click="playPrev">Prev</button>
        <button class="mini-play-btn" :disabled="!hasSong" @click="togglePlayback">{{ isPlaying ? 'Pause' : 'Play' }}</button>
        <button class="mini-icon-btn" :disabled="!hasSong" @click="playNext">Next</button>
      </div>
      <div class="mini-progress">
        <span class="mini-progress-bar" :style="{ width: `${progressPercent}%` }" />
      </div>
    </div>

    <nav class="mobile-tabbar">
      <button
        v-for="item in tabMenus"
        :key="item.path"
        class="tab-btn"
        :class="{ active: isTabActive(item.path) }"
        @click="go(item.path)"
      >
        {{ item.label }}
      </button>
      <button class="tab-btn" :class="{ active: moreMenuActive || showMoreSheet }" @click="showMoreSheet = !showMoreSheet">
        More
      </button>
    </nav>

    <transition name="sheet-fade">
      <div v-if="showMoreSheet" class="more-sheet-mask" @click.self="closeMoreSheet">
        <section class="more-sheet">
          <button
            v-for="item in moreMenus"
            :key="item.path"
            class="more-item"
            :class="{ active: isTabActive(item.path) }"
            @click="go(item.path)"
          >
            {{ item.label }}
          </button>

          <button class="more-item danger" @click="logout">Logout</button>
        </section>
      </div>
    </transition>
  </div>

  <router-view v-else />
</template>

<style scoped>
.mobile-shell {
  min-height: 100vh;
  background:
    radial-gradient(circle at 10% -10%, rgba(96, 165, 250, 0.22), transparent 48%),
    radial-gradient(circle at 90% -10%, rgba(251, 113, 133, 0.2), transparent 42%),
    #f8fafc;
}

.mobile-header {
  position: sticky;
  top: 0;
  z-index: 40;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px 12px 10px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.24);
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
}

.brand {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}

.brand-title {
  font-weight: 800;
  font-size: 18px;
  line-height: 1;
  letter-spacing: 0.2px;
  background: linear-gradient(130deg, #fb7185, #60a5fa 56%, #22d3ee);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.brand-subtitle {
  color: #64748b;
  font-size: 12px;
}

.header-search {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quick-search {
  flex: 1 1 auto;
  min-width: 0;
}

.quick-search :deep(.el-input__wrapper) {
  border-radius: 999px;
  padding-left: 12px;
  padding-right: 12px;
}

.header-action-btn {
  flex: 0 0 auto;
  height: 34px;
  padding: 0 14px;
  border: 0;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: #ffffff;
  background: linear-gradient(135deg, #fb7185, #38bdf8);
  box-shadow: 0 8px 20px rgba(56, 189, 248, 0.24);
}

.mobile-main {
  padding: 10px 10px 178px;
}

.mini-player {
  position: fixed;
  left: 10px;
  right: 10px;
  bottom: calc(64px + env(safe-area-inset-bottom));
  z-index: 35;
  border-radius: 16px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.14);
  backdrop-filter: blur(14px);
  padding: 10px;
}

.mini-player.disabled {
  opacity: 0.72;
}

.mini-top {
  min-width: 0;
}

.mini-top.clickable {
  cursor: pointer;
}

.mini-title {
  color: #0f172a;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mini-subtitle {
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mini-controls {
  margin-top: 8px;
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 8px;
}

.mini-icon-btn,
.mini-play-btn {
  border: 0;
  height: 34px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.mini-icon-btn {
  color: #0f172a;
  background: #e2e8f0;
}

.mini-play-btn {
  color: #ffffff;
  background: linear-gradient(135deg, #fb7185, #38bdf8);
}

.mini-icon-btn:disabled,
.mini-play-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.mini-progress {
  margin-top: 8px;
  height: 4px;
  border-radius: 999px;
  background: rgba(203, 213, 225, 0.65);
  overflow: hidden;
}

.mini-progress-bar {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #fb7185, #38bdf8);
  transition: width 0.25s ease;
}

.mobile-tabbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 36;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 0;
  border-top: 1px solid rgba(148, 163, 184, 0.22);
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(12px);
  padding-bottom: env(safe-area-inset-bottom);
}

.tab-btn {
  border: 0;
  background: transparent;
  color: #64748b;
  font-size: 11px;
  font-weight: 600;
  line-height: 1.2;
  padding: 10px 6px;
}

.tab-btn.active {
  color: #0f172a;
  background: linear-gradient(180deg, rgba(125, 211, 252, 0.22), rgba(248, 250, 252, 0));
}

.more-sheet-mask {
  position: fixed;
  inset: 0;
  z-index: 45;
  background: rgba(2, 6, 23, 0.42);
  display: flex;
  align-items: flex-end;
}

.more-sheet {
  width: 100%;
  border-radius: 18px 18px 0 0;
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-bottom: 0;
  background: #ffffff;
  box-shadow: 0 -20px 40px rgba(15, 23, 42, 0.22);
  padding: 12px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  padding-bottom: calc(12px + env(safe-area-inset-bottom));
}

.more-item {
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  color: #0f172a;
  border-radius: 12px;
  min-height: 42px;
  font-size: 13px;
  font-weight: 600;
}

.more-item.active {
  border-color: #7dd3fc;
  background: linear-gradient(135deg, rgba(251, 113, 133, 0.2), rgba(56, 189, 248, 0.22));
}

.more-item.danger {
  border-color: rgba(239, 68, 68, 0.32);
  color: #b91c1c;
  background: rgba(254, 242, 242, 0.9);
}

.header-suggestion-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.header-suggestion-name {
  color: #0f172a;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-suggestion-artist {
  color: #94a3b8;
  font-size: 12px;
  white-space: nowrap;
}

.sheet-fade-enter-active,
.sheet-fade-leave-active {
  transition: opacity 0.18s ease;
}

.sheet-fade-enter-from,
.sheet-fade-leave-to {
  opacity: 0;
}

@media (min-width: 961px) {
  .mobile-shell {
    max-width: 480px;
    margin: 0 auto;
    min-height: 100vh;
    border-left: 1px solid rgba(148, 163, 184, 0.28);
    border-right: 1px solid rgba(148, 163, 184, 0.28);
  }
}
</style>
