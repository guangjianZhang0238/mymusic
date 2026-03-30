<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getArtistPageApi } from '@/api/music'
import StateBlock from '@/components/StateBlock.vue'
import { normalizeImageUrl } from '@/utils/image'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const artists = ref<any[]>([])

onMounted(async () => {
  loading.value = true
  error.value = ''
  try {
    const page = await getArtistPageApi(1, 100)
    artists.value = page?.records || []
  } catch (e: any) {
    error.value = e?.message || '歌手排行榜加载失败'
  } finally {
    loading.value = false
  }
})

const getAvatar = (artist: any) => normalizeImageUrl(artist?.avatar)
</script>

<template>
  <h2 class="page-title">歌手排行榜</h2>
  <StateBlock :loading="loading" :error="error">
    <el-card class="glow-card">
      <StateBlock :empty="!artists.length" empty-text="暂无歌手数据">
        <el-table :data="artists">
          <el-table-column label="排名" width="90">
            <template #default="{ $index }">#{{ $index + 1 }}</template>
          </el-table-column>
          <el-table-column label="头像" width="100">
            <template #default="{ row }">
              <el-avatar :src="getAvatar(row)" :size="40">{{ (row?.name || '?').slice(0, 1) }}</el-avatar>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="歌手" min-width="220" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button text type="primary" @click="router.push(`/artist/${row.id}`)">查看详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </StateBlock>
    </el-card>
  </StateBlock>
</template>

<style scoped>
@media (max-width: 960px) {
  .el-card :deep(.el-button) {
    width: 100%;
  }
}
</style>
