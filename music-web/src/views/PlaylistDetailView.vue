<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { addSongToPlaylistApi, getPlaylistDetailApi, getPlaylistSongsApi, getSongPageApi, getSongsByIdsApi, removeSongFromPlaylistApi } from '@/api/music'
import { usePlayerStore } from '@/stores/player'
import StateBlock from '@/components/StateBlock.vue'
import { ElMessage } from 'element-plus'
import { getDisplaySongTitle } from '@/utils/songTitle'

const route = useRoute()
const router = useRouter()
const player = usePlayerStore()
const id = Number(route.params.id)
const playlist = ref<any>({})
const songs = ref<any[]>([])
const searchKey = ref('')
const searchRows = ref<any[]>([])
const addVisible = ref(false)
const loading = ref(false)
const error = ref('')

const getSongTitle = (song: any) => getDisplaySongTitle(song)

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

const openAdd = () => {
  searchKey.value = ''
  searchRows.value = []
  addVisible.value = true
}

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
  await player.playBySongId(songId)
  router.push(`/player/${songId}`)
}

const appendSongToQueue = async (songId: number) => {
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
  <div class="page-head">
    <h2 class="page-title">{{ playlist.name || '歌单详情' }}</h2>
    <el-space>
      <el-button @click="router.push('/library')">返回歌单</el-button>
      <el-button type="primary" @click="openAdd">+ 添加歌曲</el-button>
    </el-space>
  </div>

  <el-card class="glow-card" v-loading="loading">
    <template #header>歌单歌曲（{{ songs.length }}）</template>
    <StateBlock :error="error" :empty="!songs.length" empty-text="歌单暂无歌曲">
      <el-table :data="songs">
        <el-table-column label="歌曲" min-width="220">
          <template #default="{ row }">
            {{ getSongTitle(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="artistName" label="歌手" min-width="160" />
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-space>
              <el-button class="mini-action-btn" size="small" plain @click="appendSongToQueue(row.id)">加列表</el-button>
              <el-button class="mini-action-btn" size="small" plain @click="playNextSong(row.id)">下一曲</el-button>
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

  <el-dialog v-model="addVisible" title="添加歌曲到歌单" width="640px">
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
          <el-button v-for="item in searchRows" :key="item.id" text @click="addSong(item.id)">+ {{ getSongTitle(item) }}</el-button>
        </el-space>
      </StateBlock>
    </div>
    <template #footer>
      <el-button @click="addVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.search-result-wrap {
  margin-top: 12px;
}

.mini-action-btn {
  font-size: 12px;
  padding-left: 8px;
  padding-right: 8px;
}
</style>
