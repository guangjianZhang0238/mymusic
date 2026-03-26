<script setup lang="ts">
import { onBeforeUnmount, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { usePlayerStore } from '@/stores/player'
import { playerAudio } from '@/utils/playerAudio'

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

const go = (path: string) => router.push(path)
const goSearch = () => router.push('/search')
const logout = () => {
  user.logout()
  router.push('/login')
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
const onBeforeUnload = () => {
  if (route.path.startsWith('/player')) return
  if (!shouldRegisterBeforeUnload) return
  // When on /player, PlayerView already persists this snapshot.
  setAutoResumeOnReload(!playerAudio.paused && !playerAudio.ended)
  saveReloadProgressSnapshot()
}

onMounted(async () => {
  if (route.path === '/login') return

  const isPlayerPage = route.path.startsWith('/player')
  shouldRegisterBeforeUnload = !isPlayerPage

  // Only non-`/player` pages need global persistence.
  if (isPlayerPage) return

  window.addEventListener('beforeunload', onBeforeUnload)

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
  if (!shouldRegisterBeforeUnload) return
  window.removeEventListener('beforeunload', onBeforeUnload)
})
</script>

<template>
  <div v-if="route.path !== '/login'" class="layout">
    <header class="top glow-card">
      <div class="brand">Mymusic</div>
      <nav class="nav-wrap">
        <button
          v-for="item in menus"
          :key="item.path"
          class="nav-btn"
          :class="{ active: route.path.startsWith(item.path) }"
          @click="go(item.path)"
        >
          {{ item.label }}
        </button>
      </nav>
      <div class="user-bar">
        <input class="quick-search" placeholder="搜索" readonly @click="goSearch" />
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
  min-height: 100vh;
}

.top {
  display: flex;
  align-items: center;
  gap: 14px;
  justify-content: space-between;
  padding: 12px 20px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.28);
  position: sticky;
  top: 0;
  z-index: 10;
  background: rgba(255, 255, 255, 0.84);
  backdrop-filter: blur(10px);
}

.brand {
  font-weight: 700;
  font-size: 18px;
  color: #1f2937;
}

.nav-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.nav-btn {
  border: 1px solid rgba(244, 114, 182, 0.45);
  background: #ffffff;
  color: #ff5a8a;
  padding: 6px 14px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s;
}

.nav-btn:hover,
.nav-btn.active {
  background: linear-gradient(135deg, #ff8fb8, #7dd3fc);
  color: #ffffff;
  border-color: transparent;
  box-shadow: 0 8px 18px rgba(125, 211, 252, 0.3);
}

.user-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-name {
  color: #5f6b7a;
  font-size: 14px;
}

.quick-search {
  width: 220px;
  padding: 8px 12px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  background: rgba(255, 255, 255, 0.9);
  color: #1f2937;
  outline: none;
  cursor: pointer;
}

.quick-search:hover {
  border-color: rgba(125, 211, 252, 0.7);
  box-shadow: 0 10px 22px rgba(125, 211, 252, 0.2);
}

.logout {
  padding-left: 12px;
  padding-right: 12px;
}
</style>
