<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getAlbumPageApi, getArtistPageApi, getSearchSuggestionsApi, getSongPageApi } from '@/api/music'
import { createFeedbackApi } from '@/api/social'
import { usePlayerStore } from '@/stores/player'
import { playerAudio } from '@/utils/playerAudio'
import StateBlock from '@/components/StateBlock.vue'
import { ElMessage } from 'element-plus'
import { normalizeImageUrl } from '@/utils/image'
import { getDisplaySongTitle } from '@/utils/songTitle'

const router = useRouter()
const player = usePlayerStore()
const keyword = ref('')
const suggestions = ref<any[]>([])
const songs = ref<any[]>([])
const albums = ref<any[]>([])
const artists = ref<any[]>([])
const loading = ref(false)
const suggesting = ref(false)
const error = ref('')

const allResultsCount = computed(() => songs.value.length + albums.value.length + artists.value.length)

const querySearchSuggestions = async (queryString: string, cb: (items: any[]) => void) => {
  if (!queryString.trim()) {
    suggestions.value = []
    cb([])
    return
  }

  suggesting.value = true
  try {
    const list = await getSearchSuggestionsApi(queryString, 10)
    suggestions.value = list || []
    cb(suggestions.value)
  } catch {
    suggestions.value = []
    cb([])
  } finally {
    suggesting.value = false
  }
}

const handleSuggestionSelect = async (item: any) => {
  if (!item?.name) return
  keyword.value = item.name
  await search()
}

const search = async () => {
  const kw = keyword.value.trim()
  if (!kw) return ElMessage.warning('请输入关键词')
  loading.value = true
  error.value = ''
  try {
    const doSearch = async (query: string) => {
      const [songPage, albumPage, artistPage] = await Promise.all([
        getSongPageApi(1, 30, query),
        getAlbumPageApi(1, 20, query),
        getArtistPageApi(1, 20, query)
      ])

      songs.value = songPage.records || []
      albums.value = albumPage.records || []
      artists.value = artistPage.records || []
    }

    await doSearch(kw)

    // 如果用户输入看起来像“拼音首拼”（纯字母），但分页搜索三类结果都为空，
    // 则用联想接口的第一个候选名再搜索一次，提升可用性。
    const looksLikeInitial = /^[a-zA-Z]+$/.test(kw)
    if (
      looksLikeInitial &&
      !songs.value.length &&
      !albums.value.length &&
      !artists.value.length
    ) {
      try {
        const suggestionsForInitial = await getSearchSuggestionsApi(kw, 5)
        const mappedName = suggestionsForInitial?.[0]?.name
        if (mappedName && mappedName !== kw) {
          keyword.value = mappedName
          await doSearch(mappedName)
        }
      } catch {
        // ignore fallback errors
      }
    }

    if (!songs.value.length && !albums.value.length && !artists.value.length) ElMessage.info('暂无匹配结果，可尝试换个关键词')
  } catch (e: any) {
    error.value = e?.message || '搜索失败'
  } finally {
    loading.value = false
  }
}

const report = async () => {
  if (!keyword.value.trim()) return ElMessage.warning('请先输入关键词')
  try {
    await createFeedbackApi({ type: 'SEARCH', content: `搜索不到: ${keyword.value}`, keyword: keyword.value, scene: 'web-search' })
    ElMessage.success('反馈已提交')
  } catch (e: any) {
    ElMessage.error(e?.message || '反馈提交失败')
  }
}

const suggestionTypeText = (type?: number) => {
  if (type === 2) return '歌手'
  if (type === 3) return '专辑'
  return '歌曲'
}

const playSong = async (songId: number) => {
  try {
    await player.playBySongId(songId)
    router.push(`/player/${songId}`)
  } catch (e: any) {
    ElMessage.error(e?.message || '播放失败')
  }
}

const playAllSongs = async () => {
  const ids = songs.value.map((item) => Number(item.id)).filter((id) => Number.isFinite(id) && id > 0)
  if (!ids.length) return ElMessage.warning('暂无可播放歌曲')
  try {
    const firstId = ids[0]
    await player.replaceQueue(ids, 0)
    await player.playBySongId(firstId)
    const targetSrc = `/api/app/music/song/${firstId}/stream`
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
    router.push(`/player/${firstId}`)
  } catch (e: any) {
    ElMessage.error(e?.message || '播放全部失败')
  }
}

const appendAllSongs = async () => {
  const ids = songs.value.map((item) => Number(item.id)).filter((id) => Number.isFinite(id) && id > 0)
  if (!ids.length) return ElMessage.warning('暂无可添加歌曲')
  try {
    await player.appendSongs(ids)
    ElMessage.success(`已追加 ${ids.length} 首歌曲到播放列表`)
  } catch (e: any) {
    ElMessage.error(e?.message || '添加失败')
  }
}

const playNextAllSongs = async () => {
  const ids = songs.value.map((item) => Number(item.id)).filter((id) => Number.isFinite(id) && id > 0)
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
  <!-- <h2 class="page-title">搜索</h2> -->
  <el-card class="glow-card">
    <el-space wrap class="search-toolbar">
      <el-autocomplete
        v-model="keyword"
        class="search-autocomplete"
        clearable
        :fetch-suggestions="querySearchSuggestions"
        :trigger-on-focus="false"
        :debounce="220"
        :loading="suggesting"
        placeholder="输入歌曲/专辑/歌手或拼音首字母"
        @select="handleSuggestionSelect"
        @keyup.enter="search"
      >
        <template #default="{ item }">
          <div class="suggestion-option">
            <span class="suggestion-name">{{ item.name }}</span>
            <span class="suggestion-type">{{ suggestionTypeText(item.type) }}</span>
          </div>
        </template>
      </el-autocomplete>
      <el-button type="primary" :loading="loading" @click="search">搜索</el-button>
      <el-button @click="report">反馈缺歌</el-button>
    </el-space>
  </el-card>

  <el-card class="glow-card search-results-card" style="margin-top: 16px">
    <template #header>搜索结果（共 {{ allResultsCount }} 条）</template>
    <StateBlock :loading="loading" :error="error" :empty="!allResultsCount" empty-text="暂无搜索结果">
      <div class="results-section">
        <div class="results-grid">
          <div
            v-if="artists.length"
            class="result-panel artist-panel two-col-panel"
            :class="{ 'artist-panel-single': artists.length === 1 }"
          >
            <div class="section-title">歌手（{{ artists.length }}）</div>
            <div class="artist-list-scroll" :class="{ 'artist-list-single': artists.length === 1 }">
              <div
                v-for="a in artists"
                :key="a.id"
                class="artist-item"
                @click="router.push(`/artist/${a.id}`)"
              >
                <el-avatar :src="normalizeImageUrl(a.avatar)" :size="56" />
                <div class="artist-name">{{ a.name }}</div>
              </div>
            </div>
          </div>

          <div v-if="albums.length" class="result-panel album-panel">
            <div class="section-title">专辑（{{ albums.length }}）</div>
            <div class="album-list-scroll">
              <div
                v-for="album in albums"
                :key="album.id"
                class="album-item"
                @click="router.push(`/album/${album.id}`)"
              >
                <el-image
                  class="album-cover"
                  fit="cover"
                  :src="normalizeImageUrl(album.coverImage || album.cover || album.coverUrl || album.picUrl)"
                />
                <div class="album-name">{{ album.name }}</div>
                <div class="album-artist">{{ album.artistName || '未知歌手' }}</div>
              </div>
            </div>
          </div>

          <div v-if="songs.length" class="result-panel song-panel">
            <div class="section-title section-title-with-actions">
              <span>歌曲（{{ songs.length }}）</span>
              <el-space wrap>
                <el-button text type="primary" :disabled="!songs.length" @click="playAllSongs">全部播放</el-button>
                <el-button text :disabled="!songs.length" @click="appendAllSongs">全部加列表</el-button>
                <el-button text :disabled="!songs.length" @click="playNextAllSongs">全部下一曲播放</el-button>
              </el-space>
            </div>
            <div class="song-list-scroll">
              <div v-for="row in songs" :key="row.id" class="song-row">
                <div class="song-main">
                  <div class="song-title" :title="getDisplaySongTitle(row)">{{ getDisplaySongTitle(row) }}</div>
                  <div class="song-artist" :title="row.artistName || '未知歌手'">{{ row.artistName || '未知歌手' }}</div>
                </div>
                <div class="song-actions">
                  <el-button class="mini-action-btn" text @click="appendSong(row.id)">加列表</el-button>
                  <el-button class="mini-action-btn" text @click="playNextSong(row.id)">下一曲</el-button>
                  <el-button class="mini-action-btn" text type="primary" @click="playSong(row.id)">播放</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </StateBlock>
  </el-card>
</template>

<style scoped>
.search-toolbar {
  width: 100%;
  justify-content: space-between;
}

.search-autocomplete {
  width: min(620px, 100%);
}

.suggestion-option {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.suggestion-name {
  color: #111827;
}

.suggestion-type {
  color: #64748b;
  font-size: 12px;
}

.search-results-card {
  max-height: calc(100vh - 180px);
  display: flex;
  flex-direction: column;
}

.search-results-card :deep(.el-card__body) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.results-section {
  padding-top: 4px;
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 6px;
  padding-bottom: 10px;
}

.results-grid {
  display: flex;
  align-items: flex-start;
  gap: 24px;
  min-width: 0;
}

.result-panel {
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.92), rgba(248, 250, 252, 0.68));
  padding: 12px;
  min-width: 0;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.04);
  flex: 0 0 auto;
}

.two-col-panel {
  width: 260px;
  min-width: 260px;
}

.artist-panel-single {
  width: 170px;
  min-width: 170px;
}

.song-panel {
  flex: 1 1 0;
  width: auto;
  min-width: 0;
  margin-left: 0;
  min-height: 0;
  max-height: 100%;
  display: flex;
  flex-direction: column;
}

.song-list-scroll {
  flex: 1;
  min-height: 0;
  max-height: 45vh;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-right: 2px;
  padding-bottom: 24px;
  scroll-padding-bottom: 24px;
}

.song-list-scroll::-webkit-scrollbar {
  width: 8px;
}

.song-list-scroll::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.65);
  border-radius: 999px;
}

.song-list-scroll::-webkit-scrollbar-thumb:hover {
  background: rgba(100, 116, 139, 0.75);
}

.song-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 12px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.78);
}

.song-list-scroll .song-row:last-child {
  margin-bottom: 8px;
}

.song-row:hover {
  border-color: rgba(125, 211, 252, 0.8);
  box-shadow: 0 10px 20px rgba(125, 211, 252, 0.12);
}

.song-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.song-title {
  color: #0f172a;
  font-weight: 500;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.song-artist {
  color: #64748b;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.song-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.artist-list-scroll,
.album-list-scroll {
  max-height: 480px;
  overflow-y: auto;
  display: grid;
  gap: 10px;
}

.artist-list-scroll {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.artist-list-single {
  grid-template-columns: minmax(116px, 132px);
  justify-content: start;
}

.album-list-scroll {
  grid-template-columns: repeat(auto-fill, minmax(128px, 1fr));
}

.artist-item,
.album-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  padding: 10px 8px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  background: rgba(255, 255, 255, 0.7);
  transition: all 0.15s;
}

.artist-item {
  aspect-ratio: 1 / 1;
}

.artist-item:hover,
.album-item:hover {
  border-color: rgba(125, 211, 252, 0.8);
  box-shadow: 0 10px 22px rgba(125, 211, 252, 0.15);
}

.artist-name,
.album-name {
  color: #1f2937;
  font-size: 13px;
  width: 100%;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.album-cover {
  width: 72px;
  height: 72px;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.2);
}

.album-artist {
  color: #64748b;
  font-size: 12px;
  width: 100%;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section-title {
  font-weight: 600;
  margin-bottom: 8px;
  color: #334155;
}

.section-title-with-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding-bottom: 4px;
}

.albums-block {
  margin-top: 14px;
}

.section-divider {
  height: 1px;
  background: rgba(148, 163, 184, 0.28);
  margin: 12px 0;
}

.mini-action-btn {
  font-size: 12px;
  padding-left: 8px;
  padding-right: 8px;
}

.results-section::-webkit-scrollbar,
.artist-list-scroll::-webkit-scrollbar,
.songs-table-scroll-wrap :deep(.el-table__body-wrapper::-webkit-scrollbar) {
  width: 8px;
}

.results-section::-webkit-scrollbar-thumb,
.artist-list-scroll::-webkit-scrollbar-thumb,
.songs-table-scroll-wrap :deep(.el-table__body-wrapper::-webkit-scrollbar-thumb) {
  background: rgba(148, 163, 184, 0.65);
  border-radius: 999px;
}

.results-section::-webkit-scrollbar-thumb:hover,
.artist-list-scroll::-webkit-scrollbar-thumb:hover,
.songs-table-scroll-wrap :deep(.el-table__body-wrapper::-webkit-scrollbar-thumb:hover) {
  background: rgba(100, 116, 139, 0.75);
}


</style>
