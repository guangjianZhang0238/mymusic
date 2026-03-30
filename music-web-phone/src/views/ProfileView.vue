<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { uploadAvatarApi, getSongsByIdsApi } from '@/api/music'
import { getFavoriteSongsApi } from '@/api/player'
import { useUserStore } from '@/stores/user'
import { usePlayerStore } from '@/stores/player'
import StateBlock from '@/components/StateBlock.vue'
import { ElMessage } from 'element-plus'
import { getDisplaySongTitle } from '@/utils/songTitle'

const router = useRouter()
const user = useUserStore()
const player = usePlayerStore()
const favorites = ref<any[]>([])
const loading = ref(false)
const error = ref('')

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    const ids = await getFavoriteSongsApi()
    favorites.value = ids.length ? await getSongsByIdsApi(ids) : []
  } catch (e: any) {
    error.value = e?.message || '收藏加载失败'
  } finally {
    loading.value = false
  }
}
onMounted(load)

const uploadAvatar = async (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  try {
    const data = await uploadAvatarApi(file)
    const updated = { ...(user.userInfo || {}), avatar: data.path }
    user.userInfo = updated as any
    localStorage.setItem('userInfo', JSON.stringify(updated))
    ElMessage.success('头像更新成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '头像上传失败')
  }
}

const play = async (songId: number) => {
  const ids = favorites.value.map((item) => item.id)
  await player.setQueue(ids, Math.max(ids.indexOf(songId), 0))
  router.push(`/player/${songId}`)
}
</script>

<template>
  <h2 class="page-title">个人中心</h2>
  <el-row :gutter="16">
    <el-col :xs="24" :md="8">
      <el-card class="glow-card">
        <template #header>账号信息</template>
        <p><strong>用户名：</strong>{{ user.userInfo?.username || '-' }}</p>
        <p><strong>昵称：</strong>{{ user.userInfo?.nickname || '-' }}</p>
        <p class="text-muted">支持上传 jpg/png/webp，建议 1:1 比例头像。</p>
        <input type="file" accept="image/*" @change="uploadAvatar" />
      </el-card>
    </el-col>
    <el-col :xs="24" :md="16">
      <el-card class="glow-card">
        <template #header>我的收藏（{{ favorites.length }}）</template>
        <StateBlock :loading="loading" :error="error" :empty="!favorites.length" empty-text="暂无收藏歌曲">
          <el-table :data="favorites">
            <el-table-column label="歌曲" min-width="180">
              <template #default="{ row }">
                {{ getDisplaySongTitle(row) }}
              </template>
            </el-table-column>
            <el-table-column prop="artistName" label="歌手" min-width="140" />
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button text type="primary" @click="play(row.id)">立即播放</el-button>
              </template>
            </el-table-column>
          </el-table>
        </StateBlock>
      </el-card>
    </el-col>
  </el-row>
</template>

<style scoped>
@media (max-width: 960px) {
  .el-row {
    row-gap: 10px;
  }
}
</style>
