<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAlbumDetailApi, getSongPageApi } from '@/api/music'
import { usePlayerStore } from '@/stores/player'
import { playerAudio } from '@/utils/playerAudio'
import StateBlock from '@/components/StateBlock.vue'
import { ElMessage } from 'element-plus'
import { getDisplaySongTitle } from '@/utils/songTitle'

const route = useRoute()
const router = useRouter()
const player = usePlayerStore()
const id = Number(route.params.id)
const album = ref<any>({})
const songs = ref<any[]>([])
const loading = ref(false)
const error = ref('')

onMounted(async () => {
  loading.value = true
  error.value = ''
  try {
    album.value = await getAlbumDetailApi(id)
    const page = await getSongPageApi(1, 50, '', id)
    songs.value = page.records || []
  } catch (e: any) {
    error.value = e?.message || '专辑详情加载失败'
  } finally {
    loading.value = false
  }
})

const getSongTitle = (song: any) => getDisplaySongTitle(song)

const startPlayNow = async (songId: number) => {
  const targetSrc = `/api/app/music/song/${songId}/stream`
  const sameSource = !!playerAudio.src && playerAudio.src.includes(targetSrc)
  if (!sameSource) {
    playerAudio.src = targetSrc
    playerAudio.load()
  }
  try {
    await playerAudio.play()
    player.playing = true
  } catch {
    player.playing = false
  }
}

const playSong = async (songId: number) => {
  try {
    await player.playBySongId(songId)
    router.push(`/player/${songId}`)
  } catch (e: any) {
    ElMessage.error(e?.message || '播放失败')
  }
}

const playAll = async () => {
  const ids = songs.value.map((item) => Number(item.id)).filter((sid) => Number.isFinite(sid) && sid > 0)
  if (!ids.length) return ElMessage.warning('暂无可播放歌曲')
  try {
    const firstId = ids[0]
    await player.replaceQueue(ids, 0)
    await player.playBySongId(firstId)
    await startPlayNow(firstId)
    router.push(`/player/${firstId}`)
  } catch (e: any) {
    ElMessage.error(e?.message || '播放全部失败')
  }
}

const appendAll = async () => {
  const ids = songs.value.map((item) => Number(item.id)).filter((sid) => Number.isFinite(sid) && sid > 0)
  if (!ids.length) return ElMessage.warning('暂无可添加歌曲')
  try {
    await player.appendSongs(ids)
    ElMessage.success(`已追加 ${ids.length} 首歌曲到播放列表`)
  } catch (e: any) {
    ElMessage.error(e?.message || '追加失败')
  }
}

const playNextAll = async () => {
  const ids = songs.value.map((item) => Number(item.id)).filter((sid) => Number.isFinite(sid) && sid > 0)
  if (!ids.length) return ElMessage.warning('暂无可操作歌曲')
  try {
    await player.playNextBatch(ids)
    ElMessage.success(`已将 ${ids.length} 首歌曲加入下一曲队列`)
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

const appendSong = async (songId: number) => {
  try {
    await player.addToQueueTail(songId)
    ElMessage.success('已添加到播放列表')
  } catch (e: any) {
    ElMessage.error(e?.message || '添加失败')
  }
}

const playNextSong = async (songId: number) => {
  try {
    await player.playNext(songId)
    ElMessage.success('已加入下一曲播放')
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}
</script>

<template>
  <h2 class="page-title">专辑：{{ album.name || '未知专辑' }}</h2>
  <StateBlock :loading="loading" :error="error" :empty="!songs.length" empty-text="专辑暂无歌曲">
    <el-card class="glow-card">
      <template #header>
        <div class="header-actions">
          <span>专辑歌曲（{{ songs.length }}）</span>
          <el-space>
            <el-button text type="primary" :disabled="!songs.length" @click="playAll">播放全部</el-button>
            <el-button text :disabled="!songs.length" @click="appendAll">全部加列表</el-button>
            <el-button text :disabled="!songs.length" @click="playNextAll">全部下一曲播放</el-button>
          </el-space>
        </div>
      </template>
      <div class="song-list-scroll">
        <div class="song-grid">
          <div v-for="row in songs" :key="row.id" class="song-item">
            <div class="song-title" :title="getSongTitle(row)">{{ getSongTitle(row) }}</div>
            <el-space class="song-actions">
              <el-button class="mini-action-btn" size="small" plain @click="appendSong(row.id)">加列表</el-button>
              <el-button class="mini-action-btn" size="small" plain @click="playNextSong(row.id)">下一曲</el-button>
              <el-button text type="primary" @click="playSong(row.id)">播放</el-button>
            </el-space>
          </div>
        </div>
      </div>
    </el-card>
  </StateBlock>
</template>

<style scoped>
.header-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.song-list-scroll {
  /* 给列表区域加滚动条，避免页面被长列表撑爆 */
  max-height: 62vh;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 4px; /* 给滚动条留点空间 */
}

.song-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
  padding: 6px 4px;
}

.song-item {
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
  border-radius: 10px;
  padding: 10px 12px;
  background: var(--el-fill-color-light, #fafafa);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.song-title {
  font-size: 14px;
  line-height: 1.4;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.song-actions {
  justify-content: flex-start;
  flex-wrap: wrap;
}

.mini-action-btn {
  font-size: 12px;
  padding-left: 8px;
  padding-right: 8px;
}
</style>
