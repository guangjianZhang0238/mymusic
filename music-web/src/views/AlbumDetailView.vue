<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAlbumDetailApi, getSongPageApi } from '@/api/music'
import StateBlock from '@/components/StateBlock.vue'

const route = useRoute()
const router = useRouter()
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
</script>

<template>
  <h2 class="page-title">专辑：{{ album.name || '未知专辑' }}</h2>
  <el-card class="glow-card" style="margin-bottom: 16px">
    <template #header>专辑信息</template>
    <el-space wrap>
      <el-tag effect="plain">歌曲数：{{ songs.length }}</el-tag>
      <el-tag effect="plain">ID：{{ album.id || '-' }}</el-tag>
    </el-space>
  </el-card>
  <StateBlock :loading="loading" :error="error" :empty="!songs.length" empty-text="专辑暂无歌曲">
    <el-card class="glow-card">
      <template #header>专辑歌曲</template>
      <el-table :data="songs">
        <el-table-column prop="name" label="歌曲" min-width="220" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }"><el-button text type="primary" @click="router.push(`/player/${row.id}`)">播放</el-button></template>
        </el-table-column>
      </el-table>
    </el-card>
  </StateBlock>
</template>
