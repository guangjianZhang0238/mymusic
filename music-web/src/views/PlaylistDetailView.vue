<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { addSongToPlaylistApi, getPlaylistDetailApi, getPlaylistSongsApi, getSongPageApi, getSongsByIdsApi, removeSongFromPlaylistApi } from '@/api/music'
import { usePlayerStore } from '@/stores/player'
import StateBlock from '@/components/StateBlock.vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const player = usePlayerStore()
const id = Number(route.params.id)
const playlist = ref<any>({})
const songs = ref<any[]>([])
const searchKey = ref('')
const searchRows = ref<any[]>([])
const loading = ref(false)
const error = ref('')

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    playlist.value = await getPlaylistDetailApi(id)
    const ids = await getPlaylistSongsApi(id)
    songs.value = ids.length ? await getSongsByIdsApi(ids) : []
  } catch (e: any) {
    error.value = e?.message || '歌单详情加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)

const search = async () => {
  try {
    const page = await getSongPageApi(1, 20, searchKey.value)
    searchRows.value = page.records || []
  } catch (e: any) {
    ElMessage.error(e?.message || '搜索失败')
  }
}

const addSong = async (songId: number) => {
  try {
    await addSongToPlaylistApi(id, songId)
    await load()
    ElMessage.success('添加成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '添加失败')
  }
}
const removeSong = async (songId: number) => {
  try {
    await removeSongFromPlaylistApi(id, songId)
    await load()
    ElMessage.success('移除成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '移除失败')
  }
}

const play = async (songId: number) => {
  const ids = songs.value.map((item) => item.id)
  await player.setQueue(ids, Math.max(ids.indexOf(songId), 0))
  router.push(`/player/${songId}`)
}
</script>

<template>
  <h2 class="page-title">{{ playlist.name || '歌单详情' }}</h2>
  <el-card class="glow-card" v-loading="loading">
    <template #header>添加歌曲到歌单</template>
    <el-space wrap>
      <el-input
        v-model="searchKey"
        placeholder="输入歌曲名/歌手名"
        style="width: 360px"
        clearable
        @keyup.enter="search"
      />
      <el-button type="primary" @click="search">搜索</el-button>
    </el-space>
    <div class="search-result-wrap">
      <StateBlock :empty="!searchRows.length" empty-text="搜索后在此显示可添加歌曲">
        <el-space wrap>
          <el-button v-for="item in searchRows" :key="item.id" text @click="addSong(item.id)">+ {{ item.name }}</el-button>
        </el-space>
      </StateBlock>
    </div>
  </el-card>

  <el-card class="glow-card" style="margin-top: 16px">
    <template #header>歌单歌曲（{{ songs.length }}）</template>
    <StateBlock :error="error" :empty="!songs.length" empty-text="歌单暂无歌曲">
      <el-table :data="songs">
        <el-table-column prop="name" label="歌曲" min-width="200" />
        <el-table-column prop="artistName" label="歌手" min-width="160" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-space>
              <el-button text type="primary" @click="play(row.id)">播放</el-button>
              <el-popconfirm title="确认移除这首歌吗？" @confirm="removeSong(row.id)">
                <template #reference><el-button text type="danger">移除</el-button></template>
              </el-popconfirm>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
    </StateBlock>
  </el-card>
</template>

<style scoped>
.search-result-wrap {
  margin-top: 12px;
}
</style>
