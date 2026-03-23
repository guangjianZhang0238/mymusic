<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getArtistPageApi, getHotSongsApi } from '@/api/music'
import { usePlayerStore } from '@/stores/player'
import StateBlock from '@/components/StateBlock.vue'
import { ElMessage } from 'element-plus'
import { normalizeImageUrl } from '@/utils/image'

const router = useRouter()
const player = usePlayerStore()
const artists = ref<any[]>([])
const hotSongs = ref<any[]>([])
const loading = ref(false)
const error = ref('')

onMounted(async () => {
  loading.value = true
  error.value = ''
  try {
    const [artistPage, hot] = await Promise.all([getArtistPageApi(1, 10), getHotSongsApi()])
    artists.value = artistPage.records || []
    hotSongs.value = hot || []
  } catch (e: any) {
    error.value = e?.message || '首页数据加载失败'
  } finally {
    loading.value = false
  }
})

const getSongTitle = (song: any) => song?.title || song?.name || '未知歌曲'
const getSongArtist = (song: any) => song?.artistName || song?.artistNames || '未知歌手'
const getArtistAvatar = (artist: any) => normalizeImageUrl(artist?.avatar)

const play = async (songId: number) => {
  try {
    const ids = hotSongs.value.map((item) => item.id)
    await player.setQueue(ids, Math.max(ids.indexOf(songId), 0))
    router.push(`/player/${songId}`)
  } catch (e: any) {
    ElMessage.error(e?.message || '播放失败')
  }
}
</script>

<template>
  <h2 class="page-title">首页</h2>
  <StateBlock :loading="loading" :error="error">
    <el-card class="glow-card">
      <template #header>热门歌曲</template>
      <StateBlock :empty="!hotSongs.length" empty-text="暂无热门歌曲">
        <el-table :data="hotSongs">
          <el-table-column label="歌曲">
            <template #default="{ row }">{{ getSongTitle(row) }}</template>
          </el-table-column>
          <el-table-column label="歌手">
            <template #default="{ row }">{{ getSongArtist(row) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }"><el-button type="primary" text @click="play(row.id)">播放</el-button></template>
          </el-table-column>
        </el-table>
      </StateBlock>
    </el-card>
    <el-card class="glow-card" style="margin-top: 16px">
      <template #header>热门歌手</template>
      <StateBlock :empty="!artists.length" empty-text="暂无歌手">
        <div class="grid">
          <el-card v-for="artist in artists" :key="artist.id" class="card-soft artist-card">
            <el-avatar :src="getArtistAvatar(artist)" :size="56" class="artist-avatar">
              {{ (artist.name || '?').slice(0, 1) }}
            </el-avatar>
            <div class="artist-main">
              <h4 class="section-title">{{ artist.name }}</h4>
              <el-button text @click="router.push(`/artist/${artist.id}`)">查看详情</el-button>
            </div>
          </el-card>
        </div>
      </StateBlock>
    </el-card>
  </StateBlock>
</template>

<style scoped>
.artist-card {
  display: flex;
  align-items: center;
  gap: 12px;
}

.artist-avatar {
  flex-shrink: 0;
}

.artist-main {
  min-width: 0;
}
</style>
