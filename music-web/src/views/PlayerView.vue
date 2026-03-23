<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePlayerStore } from '@/stores/player'
import { checkFavoriteApi, addFavoriteApi, removeFavoriteApi } from '@/api/player'
import { getLyricsApi } from '@/api/music'
import StateBlock from '@/components/StateBlock.vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const store = usePlayerStore()
const audio = new Audio()
const favorited = ref(false)
const lyrics = ref<any[]>([])
const activeLyric = ref(-1)
const duration = ref(0)
const isSeeking = ref(false)
const loading = ref(false)
const error = ref('')

const songId = computed(() => Number(route.params.songId || store.currentSongId || 0))
const song = computed(() => store.currentSong)
const src = computed(() => (songId.value ? `/api/app/music/song/${songId.value}/stream` : ''))

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

const loadLyrics = async () => {
  if (!songId.value) return
  try {
    const data = await getLyricsApi(songId.value)
    lyrics.value = (data?.lines || []).map((item: any) => ({ text: item.text, time: Number(item.time || 0) }))
  } catch (e: any) {
    ElMessage.warning(e?.message || '歌词加载失败')
    lyrics.value = []
  }
}

onMounted(async () => {
  loading.value = true
  error.value = ''
  try {
    await store.hydrateFromServer()
    if (songId.value) await store.playBySongId(songId.value)
    await loadLyrics()
    if (songId.value) favorited.value = await checkFavoriteApi(1, songId.value)
  } catch (e: any) {
    error.value = e?.message || '播放器初始化失败'
  } finally {
    loading.value = false
  }

  audio.addEventListener('loadedmetadata', () => {
    const d = audio.duration
    duration.value = Number.isFinite(d) && d > 0 ? d : 0
  })
  audio.addEventListener('durationchange', () => {
    const d = audio.duration
    duration.value = Number.isFinite(d) && d > 0 ? d : 0
  })

  audio.addEventListener('timeupdate', () => {
    if (isSeeking.value) return
    store.currentTime = audio.currentTime
    updateActiveLyric(audio.currentTime * 1000)
  })
  audio.addEventListener('ended', async () => {
    await store.next()
  })
})

watch(
  src,
  (val) => {
    if (!val) return
    // 进入页面时如果 src 初始就有值，watch 默认不会执行首次回调，
    // 导致后端不请求 stream、也拿不到时长；这里显式 immediate + load。
    audio.src = val
    audio.load()
    audio.play()
    store.playing = true
  },
  { immediate: true }
)

watch(
  () => store.currentSongId,
  async (id) => {
    if (!id) return
    router.replace(`/player/${id}`)
    favorited.value = await checkFavoriteApi(1, id)
    await loadLyrics()
  }
)

onBeforeUnmount(() => {
  // 退出播放器页时停止播放，避免后台继续响导致“找不到停止按钮”
  try {
    audio.pause()
  } catch {
    // ignore
  }
  store.playing = false
})

const toggle = () => {
  if (audio.paused) {
    audio.play()
    store.playing = true
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
  window.setTimeout(() => {
    isSeeking.value = false
  }, 150)
}

const onSeekChange = (e: Event) => {
  const target = e.target as HTMLInputElement | null
  if (!target) return
  seekTo(Number(target.value))
}

const playFromQueue = async (id: number) => {
  if (!id) return
  await store.playBySongId(id)
}

const onQueueRowClick = (row: any) => {
  if (!row?.id) return
  playFromQueue(row.id)
}

const queueRowClassName = ({ row }: any) => {
  return row?.id === store.currentSongId ? 'queue-current-row' : ''
}
</script>

<template>
  <h2 class="page-title">播放器</h2>
  <StateBlock :loading="loading" :error="error">
    <div class="player-layout">
      <div class="player-left">
        <el-card class="glow-card">
          <h3 class="section-title">{{ song?.name || song?.title || '未选择歌曲' }}</h3>
          <p class="text-muted">{{ song?.artistName || song?.artistNames }}</p>

          <div class="progress-wrap">
            <div class="progress-time">
              {{ formatTime(store.currentTime) }} / {{ formatTime(duration) }}
            </div>
            <input
              class="progress-range"
              type="range"
              min="0"
              :max="duration || 0"
              step="0.1"
              :disabled="!duration"
              :value="store.currentTime"
              @change="onSeekChange"
            />
          </div>

          <div class="player-actions">
            <el-button @click="prev">上一首</el-button>
            <el-button type="primary" @click="toggle">{{ store.playing ? '暂停' : '播放' }}</el-button>
            <el-button @click="next">下一首</el-button>
            <el-button @click="toggleFavorite">{{ favorited ? '取消收藏' : '收藏' }}</el-button>
            <el-button @click="router.push(`/lyrics/${songId}`)">歌词页</el-button>
            <el-button @click="router.push(`/comments/${songId}`)">评论</el-button>
          </div>
        </el-card>

        <el-card class="glow-card" style="margin-top: 16px">
          <StateBlock :empty="!lyrics.length" empty-text="暂无歌词">
            <div
              v-for="(line, index) in lyrics"
              :key="index"
              :style="{ color: index === activeLyric ? '#38bdf8' : '#7a8496', lineHeight: '1.9' }"
            >
              {{ line.text }}
            </div>
          </StateBlock>
        </el-card>
      </div>

      <div class="player-right">
        <el-card class="glow-card">
          <template #header>播放队列（{{ store.queueSongs.length }}）</template>
          <StateBlock :empty="!store.queueSongs.length" empty-text="暂无播放队列">
            <el-table :data="store.queueSongs" :row-class-name="queueRowClassName" size="small" @row-click="onQueueRowClick">
              <el-table-column label="歌曲" min-width="200">
                <template #default="{ row }">
                  {{ row.title || row.name }}
                </template>
              </el-table-column>
              <el-table-column prop="artistName" label="歌手" min-width="120" />
              <el-table-column label="操作" width="100">
                <template #default="{ row }">
                  <el-button text type="primary" @click.stop="playFromQueue(row.id)">播放</el-button>
                </template>
              </el-table-column>
            </el-table>
          </StateBlock>
        </el-card>
      </div>
    </div>
  </StateBlock>
</template>

<style scoped>
.player-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.player-left {
  flex: 1;
  min-width: 0;
}

.player-right {
  width: 420px;
}

.progress-wrap {
  margin-top: 10px;
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
  flex-wrap: wrap;
  gap: 10px;
}

.queue-current-row :deep(.el-table__cell) {
  background: rgba(56, 189, 248, 0.12);
}
</style>
