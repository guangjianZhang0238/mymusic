<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getHotArtistsApi, getHotSongsApi } from '@/api/music'
import { usePlayerStore } from '@/stores/player'
import { playerAudio } from '@/utils/playerAudio'
import StateBlock from '@/components/StateBlock.vue'
import { ElMessage } from 'element-plus'
import { normalizeImageUrl } from '@/utils/image'
import { getDisplaySongTitle } from '@/utils/songTitle'

const router = useRouter()
const player = usePlayerStore()
const artistPool = ref<any[]>([])
const hotSongPool = ref<any[]>([])
const displayArtists = ref<any[]>([])
const displayHotSongs = ref<any[]>([])
const lastHotSongBatchIds = ref<number[]>([])
const loading = ref(false)
const hotSongsRefreshing = ref(false)
const error = ref('')

const HOT_ARTIST_POOL_SIZE = 50
const HOT_ARTIST_DISPLAY_SIZE = 9
const HOT_SONG_POOL_SIZE = 100
const HOT_SONG_DISPLAY_SIZE = 15

const toNumber = (value: unknown) => {
  const n = Number(value)
  return Number.isFinite(n) ? n : 0
}

const sortSongsByPlayCount = <T>(list: T[]) =>
  [...(list || [])].sort((a, b) => toNumber((b as any)?.playCount) - toNumber((a as any)?.playCount))

const getSongId = (song: any) => toNumber(song?.id)

const pickRandomItems = <T>(list: T[], count: number) => {
  if (!Array.isArray(list) || !list.length || count <= 0) {
    return []
  }
  const copy = [...list]
  for (let i = copy.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[copy[i], copy[j]] = [copy[j], copy[i]]
  }
  return copy.slice(0, Math.min(count, copy.length))
}

const resetDisplayHotSongsFromPool = () => {
  const sortedPool = sortSongsByPlayCount(hotSongPool.value)
  const previousBatchSet = new Set(lastHotSongBatchIds.value)
  const nonDuplicateBatch = sortedPool.filter((song) => !previousBatchSet.has(getSongId(song)))

  const nextBatch: any[] = [...nonDuplicateBatch.slice(0, HOT_SONG_DISPLAY_SIZE)]
  if (nextBatch.length < HOT_SONG_DISPLAY_SIZE) {
    const nextBatchIdSet = new Set(nextBatch.map((song) => getSongId(song)))
    const fallbackBatch = sortedPool
      .filter((song) => !nextBatchIdSet.has(getSongId(song)))
      .slice(0, HOT_SONG_DISPLAY_SIZE - nextBatch.length)
    nextBatch.push(...fallbackBatch)
  }

  displayHotSongs.value = nextBatch
  lastHotSongBatchIds.value = nextBatch.map((song) => getSongId(song)).filter((id) => id > 0)
}

const fetchHotSongs = async () => {
  const hotSongs = await getHotSongsApi(HOT_SONG_POOL_SIZE)
  hotSongPool.value = Array.isArray(hotSongs) ? sortSongsByPlayCount(hotSongs) : []
}

const refreshHotSongs = async () => {
  hotSongsRefreshing.value = true
  try {
    await fetchHotSongs()
    resetDisplayHotSongsFromPool()
  } catch (e: any) {
    // Keep interaction responsive even when request fails.
    resetDisplayHotSongsFromPool()
    ElMessage.error(e?.message || '刷新热门歌曲失败')
  } finally {
    hotSongsRefreshing.value = false
  }
}

const refreshHotArtists = () => {
  displayArtists.value = pickRandomItems(artistPool.value, HOT_ARTIST_DISPLAY_SIZE)
}

const buildHotArtistPool = async () => {
  const artists = await getHotArtistsApi(HOT_ARTIST_POOL_SIZE)
  artistPool.value = Array.isArray(artists) ? [...artists] : []
}

onMounted(async () => {
  loading.value = true
  error.value = ''
  try {
    await fetchHotSongs()
    await buildHotArtistPool()
    resetDisplayHotSongsFromPool()
    refreshHotArtists()
  } catch (e: any) {
    error.value = e?.message || '首页数据加载失败'
  } finally {
    loading.value = false
  }
})

const getSongTitle = (song: any) => getDisplaySongTitle(song)
const getSongArtist = (song: any) => song?.artistName || song?.artistNames || '未知歌手'
const getSongPlayCount = (song: any) => toNumber(song?.playCount).toLocaleString('zh-CN')
const getArtistAvatar = (artist: any) => normalizeImageUrl(artist?.avatar)

const startPlayNow = async (songId: number) => {
  const audio = playerAudio
  const targetSrc = `/api/app/music/song/${songId}/stream`
  const sameSource = !!audio.src && audio.src.includes(targetSrc)
  if (!sameSource) {
    audio.src = targetSrc
    audio.load()
  }
  await audio.play()
}

const play = async (songId: number) => {
  try {
    await player.playBySongId(songId)
    await startPlayNow(songId)
    router.push(`/player/${songId}`)
  } catch (e: any) {
    ElMessage.error(e?.message || '播放失败')
  }
}

const playAllHotSongs = async () => {
  const ids = displayHotSongs.value.map((song) => Number(song?.id)).filter((id) => Number.isFinite(id) && id > 0)
  if (!ids.length) return
  try {
    await player.replaceQueue(ids, 0)
    await player.playBySongId(ids[0])
    await startPlayNow(ids[0])
    router.push(`/player/${ids[0]}`)
  } catch (e: any) {
    ElMessage.error(e?.message || '播放失败')
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

const goArtistRank = () => {
  router.push('/artist-rank')
}
</script>

<template>
  <h2 class="page-title">首页</h2>
  <StateBlock :loading="loading" :error="error">
    <div class="home-page">
      <section class="hero-section">
        <div class="hero-main">
          <div class="hero-title">今天想听点什么？</div>
          <div class="hero-subtitle">为你精选当下热门歌曲与歌手，点一下就开播</div>
          <div class="hero-actions">
            <el-button type="primary" round :disabled="!displayHotSongs.length" @click="playAllHotSongs">一键播放热门</el-button>
            <el-button :loading="hotSongsRefreshing" round :disabled="hotSongPool.length <= 1" @click="refreshHotSongs">刷新歌曲推荐</el-button>
          </div>
        </div>
        <div class="hero-stats">
          <div class="stat-card">
            <div class="stat-label">热门歌曲池</div>
            <div class="stat-value">{{ hotSongPool.length }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">当前展示</div>
            <div class="stat-value">{{ displayHotSongs.length }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">热门歌手池</div>
            <div class="stat-value">{{ artistPool.length }}</div>
          </div>
        </div>
      </section>

      <div class="panel-grid">
        <el-card class="glow-card home-panel songs-panel">
          <template #header>
            <div class="panel-header">
              <span class="panel-title">热门歌曲</span>
              <div class="panel-header-actions">
                <el-button text type="primary" :disabled="!displayHotSongs.length" @click="playAllHotSongs">播放全部</el-button>
                <el-button
                  text
                  type="primary"
                  :loading="hotSongsRefreshing"
                  :disabled="hotSongPool.length <= 1"
                  @click="refreshHotSongs"
                >
                  换一批
                </el-button>
              </div>
            </div>
          </template>
          <div class="home-panel-body">
            <StateBlock :empty="!displayHotSongs.length" empty-text="暂无热门歌曲">
              <el-table :data="displayHotSongs" height="100%" class="song-table" stripe>
                <el-table-column label="#" width="54" align="center">
                  <template #default="{ $index }">
                    <span class="song-rank" :class="{ 'song-rank-top': $index < 3 }">{{ $index + 1 }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="歌曲" min-width="60" show-overflow-tooltip>
                  <template #default="{ row }">
                    <span class="song-title">{{ getSongTitle(row) }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="歌手" min-width="60" show-overflow-tooltip>
                  <template #default="{ row }">
                    <span class="song-artist">{{ getSongArtist(row) }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="播放量" width="60" align="center">
                  <template #default="{ row }">
                    <span class="song-play-count">{{ getSongPlayCount(row) }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="" width="208" align="right">
                  <template #default="{ row }">
                    <el-space class="song-actions" wrap>
                      <el-button class="mini-action-btn" size="small" plain @click="appendSong(row.id)">加列表</el-button>
                      <el-button class="mini-action-btn" size="small" plain @click="playNextSong(row.id)">下一曲</el-button>
                      <el-button type="primary" text @click="play(row.id)">播放</el-button>
                    </el-space>
                  </template>
                </el-table-column>
              </el-table>
            </StateBlock>
          </div>
        </el-card>

        <el-card class="glow-card home-panel artists-panel">
          <template #header>
            <div class="panel-header">
              <span class="panel-title">热门歌手</span>
              <el-button text type="primary" :disabled="artistPool.length <= 1" @click="refreshHotArtists">换一批</el-button>
            </div>
          </template>
          <div class="home-panel-body">
            <StateBlock :empty="!displayArtists.length" empty-text="暂无歌手">
              <div class="artist-grid">
                <el-card v-for="artist in displayArtists" :key="artist.id" class="card-soft artist-card" shadow="hover">
                  <el-avatar :src="getArtistAvatar(artist)" :size="36" class="artist-avatar">
                    {{ (artist.name || '?').slice(0, 1) }}
                  </el-avatar>
                  <div class="artist-main">
                    <div class="artist-name" :title="artist.name">{{ artist.name }}</div>
                    <el-button text size="small" class="artist-detail-btn" @click="router.push(`/artist/${artist.id}`)">查看详情</el-button>
                  </div>
                </el-card>
              </div>
              <div class="artist-more-wrap">
                <el-link type="primary" :underline="false" @click="goArtistRank">查看更多热门歌手</el-link>
              </div>
            </StateBlock>
          </div>
        </el-card>
      </div>
    </div>
  </StateBlock>
</template>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: min(96vw, 1200px);
  max-width: 100%;
  margin: 1.6vh auto 2vh;
  padding: 0;
  box-sizing: border-box;
}

.hero-section {
  border-radius: 16px;
  padding: 14px 16px;
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 0.8fr);
  gap: 12px;
  background: linear-gradient(135deg, #1d4ed8 0%, #2563eb 45%, #3b82f6 100%);
  color: #fff;
  box-shadow: 0 8px 20px rgba(37, 99, 235, 0.2);
}

.hero-main {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
}

.hero-title {
  font-size: 24px;
  font-weight: 800;
  line-height: 1.15;
}

.hero-subtitle {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.88);
}

.hero-actions {
  margin-top: 4px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.stat-card {
  border-radius: 12px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(3px);
}

.stat-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.8);
}

.stat-value {
  margin-top: 4px;
  font-size: 20px;
  font-weight: 700;
}

.panel-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 0.8fr);
  gap: 10px;
  height: calc(100vh - 270px);
  min-height: 420px;
  max-height: 620px;
}

.home-panel {
  height: 100%;
  overflow: hidden;
  border-radius: 14px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-title {
  font-size: 15px;
  font-weight: 700;
  color: #111827;
}

.panel-header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.home-panel :deep(.el-card__header) {
  border-bottom: 1px solid #eef2ff;
  padding: 10px 12px;
}

.home-panel :deep(.el-card__body) {
  height: calc(100% - 49px);
  overflow: hidden;
  padding: 10px 12px;
}

.home-panel-body {
  height: 100%;
  min-height: 0;
}

.home-panel-body :deep(.state-block),
.home-panel-body :deep(.state-content) {
  height: 100%;
}

.home-panel-body :deep(.state-content > div) {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.songs-panel :deep(.el-table) {
  height: 100% !important;
}

.songs-panel :deep(.el-table__body-wrapper) {
  overflow-y: auto;
}

.song-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.song-table :deep(.el-table__header th) {
  background: #f8fafc;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
}

.song-table :deep(.el-table__cell) {
  padding-top: 8px;
  padding-bottom: 8px;
}

.song-table :deep(.cell) {
  min-width: 0;
}

.song-table :deep(.el-table__row:hover > td.el-table__cell) {
  background: #f8fbff;
}

.song-actions {
  width: 100%;
  justify-content: flex-end;
}

.song-rank {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
  background: #f1f5f9;
}

.song-rank-top {
  color: #1d4ed8;
  background: #dbeafe;
}

.song-title {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
}

.song-artist {
  font-size: 12px;
  color: #475569;
}

.song-play-count {
  font-size: 14px;
  font-weight: 700;
  color: #0f766e;
}

.mini-action-btn {
  font-size: 12px;
  border-color: #dbeafe;
  color: #2563eb;
  background: #eff6ff;
  padding-left: 8px;
  padding-right: 8px;
}

.mini-action-btn:hover,
.mini-action-btn:focus {
  border-color: #93c5fd;
  background: #dbeafe;
  color: #1d4ed8;
}

.artist-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  min-height: 0;
}

.artist-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-height: 72px;
  text-align: center;
  border-radius: 10px;
  border: 1px solid #eef2ff;
  padding: 6px;
}

.artist-avatar {
  flex-shrink: 0;
  border: 2px solid #dbeafe;
}

.artist-main {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1px;
}

.artist-name {
  font-size: 12px;
  font-weight: 600;
  color: #1f2937;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.artist-detail-btn {
  padding: 0;
  min-height: 16px;
  font-size: 11px;
}

.artist-more-wrap {
  margin-top: 8px;
  display: flex;
  justify-content: center;
}

@media (max-width: 1200px) {
  .home-page {
    width: min(97vw, 1200px);
    margin: 1.4vh auto 1.8vh;
    padding: 0;
  }

  .hero-section {
    grid-template-columns: 1fr;
  }

  .hero-stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .panel-grid {
    min-height: auto;
    height: auto;
    max-height: none;
    grid-template-columns: 1fr;
  }

  .home-panel {
    min-height: 340px;
  }

  .artist-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .hero-section {
    padding: 18px;
  }

  .hero-title {
    font-size: 24px;
  }

  .hero-stats {
    grid-template-columns: 1fr;
  }

  .artist-grid {
    grid-template-columns: 1fr;
  }

  .panel-header {
    flex-wrap: wrap;
    gap: 6px;
  }

  .song-table :deep(.el-table__cell) {
    padding-top: 5px;
    padding-bottom: 5px;
  }
}
</style>
