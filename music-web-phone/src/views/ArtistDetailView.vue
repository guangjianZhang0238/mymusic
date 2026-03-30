<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getArtistDetailApi, getArtistTopSongsApi, getAlbumPageApi } from '@/api/music'
import { usePlayerStore } from '@/stores/player'
import { playerAudio } from '@/utils/playerAudio'
import StateBlock from '@/components/StateBlock.vue'
import { ElMessage } from 'element-plus'
import { normalizeImageUrl } from '@/utils/image'
import { getDisplaySongTitle } from '@/utils/songTitle'

const route = useRoute()
const router = useRouter()
const player = usePlayerStore()
const id = Number(route.params.id)
const artist = ref<any>({})
const songs = ref<any[]>([])
const albums = ref<any[]>([])
const loading = ref(false)
const error = ref('')

onMounted(async () => {
  loading.value = true
  error.value = ''
  try {
    artist.value = await getArtistDetailApi(id)
    songs.value = await getArtistTopSongsApi(id, 50)
    // 按歌手ID拉取相关专辑，避免用歌手名去模糊专辑名导致返回为空
    const page = await getAlbumPageApi(1, 20, '', id)
    albums.value = page.records || []
  } catch (e: any) {
    error.value = e?.message || '歌手详情加载失败'
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
    // 在跳转播放器页之前直接触发播放，避免浏览器拦截“延迟自动播放”
    await startPlayNow(songId)
    router.push(`/player/${songId}`)
  } catch (e: any) {
    ElMessage.error(e?.message || '播放失败')
  }
}

const playAll = async () => {
  const ids = songs.value.slice(0, 50).map((item) => Number(item.id)).filter((sid) => Number.isFinite(sid) && sid > 0)
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
  const ids = songs.value.slice(0, 50).map((item) => Number(item.id)).filter((sid) => Number.isFinite(sid) && sid > 0)
  if (!ids.length) return ElMessage.warning('暂无可添加歌曲')
  try {
    await player.appendSongs(ids)
    ElMessage.success(`已追加 ${ids.length} 首歌曲到播放列表`)
  } catch (e: any) {
    ElMessage.error(e?.message || '追加失败')
  }
}

const playNextAll = async () => {
  const ids = songs.value.slice(0, 50).map((item) => Number(item.id)).filter((sid) => Number.isFinite(sid) && sid > 0)
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
  <h2 class="page-title">歌手：{{ artist.name || '未知歌手' }}</h2>
  <StateBlock :loading="loading" :error="error">
    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <el-card class="glow-card">
          <template #header>
            <div class="header-actions">
              <span>热门歌曲（{{ songs.length }}）</span>
              <el-space>
                <el-button text type="primary" :disabled="!songs.length" @click="playAll">播放全部</el-button>
                <el-button text :disabled="!songs.length" @click="appendAll">全部加列表</el-button>
                <el-button text :disabled="!songs.length" @click="playNextAll">全部下一曲播放</el-button>
              </el-space>
            </div>
          </template>
          <StateBlock :empty="!songs.length" empty-text="暂无热门歌曲">
            <el-table :data="songs">
              <el-table-column label="歌曲" min-width="180">
                <template #default="{ row }">
                  {{ getSongTitle(row) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="220">
                <template #default="{ row }">
                  <el-space>
                    <el-button class="mini-action-btn" size="small" plain @click="appendSong(row.id)">加列表</el-button>
                    <el-button class="mini-action-btn" size="small" plain @click="playNextSong(row.id)">下一曲</el-button>
                    <el-button text type="primary" @click="playSong(row.id)">播放</el-button>
                  </el-space>
                </template>
              </el-table-column>
            </el-table>
          </StateBlock>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card class="glow-card">
          <template #header>相关专辑（{{ albums.length }}）</template>
          <StateBlock :empty="!albums.length" empty-text="暂无相关专辑">
            <div class="album-grid">
              <div
                v-for="a in albums"
                :key="a.id"
                class="album-item"
                @click="router.push(`/album/${a.id}`)"
              >
                <div class="album-cover-wrap">
                  <img v-if="a.coverImage" class="album-cover" :src="normalizeImageUrl(a.coverImage)" />
                  <div v-else class="album-cover-placeholder">无封面</div>
                </div>
                <div class="album-name">{{ a.name }}</div>
              </div>
            </div>
          </StateBlock>
        </el-card>
      </el-col>
    </el-row>
  </StateBlock>
</template>

<style scoped>
.header-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.album-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  padding-top: 4px;
}

.album-item {
  cursor: pointer;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 14px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.6);
  transition: all 0.15s;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.album-item:hover {
  border-color: rgba(125, 211, 252, 0.8);
  box-shadow: 0 10px 22px rgba(125, 211, 252, 0.15);
}

.album-cover-wrap {
  width: 100%;
  aspect-ratio: 1 / 1;
  background: rgba(148, 163, 184, 0.12);
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.album-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.album-cover-placeholder {
  font-size: 12px;
  color: rgba(51, 65, 85, 0.7);
}

.album-name {
  font-size: 13px;
  color: #1f2937;
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mini-action-btn {
  font-size: 12px;
  padding-left: 8px;
  padding-right: 8px;
}

@media (max-width: 960px) {
  .header-actions {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .header-actions :deep(.el-space) {
    width: 100%;
    justify-content: space-between;
  }

  .album-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
