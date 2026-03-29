<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { createLyricsShareApi, getMyLyricsSharesApi } from '@/api/social'
import StateBlock from '@/components/StateBlock.vue'
import { ElMessage } from 'element-plus'

const rows = ref<any[]>([])
const form = reactive({ lyricsId: 0, shareText: '', bgColor: '#000000', textColor: '#ffffff' })
const loading = ref(false)
const error = ref('')

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    const page = await getMyLyricsSharesApi(1, 30)
    rows.value = page.records || []
  } catch (e: any) {
    error.value = e?.message || '歌词分享列表加载失败'
  } finally {
    loading.value = false
  }
}
onMounted(load)

const submit = async () => {
  if (!form.lyricsId) return ElMessage.warning('请填写歌词ID')
  try {
    await createLyricsShareApi(form)
    await load()
    ElMessage.success('创建分享成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '创建分享失败')
  }
}
</script>

<template>
  <h2 class="page-title">歌词分享</h2>
  <el-card class="glow-card">
    <template #header>创建歌词分享</template>
    <el-space wrap>
      <span class="text-muted">歌词 ID：</span>
      <el-input-number v-model="form.lyricsId" :min="1" />
    </el-space>
    <el-input v-model="form.shareText" placeholder="分享文案（选填）" style="margin-top: 8px" />
    <el-row :gutter="10" style="margin-top: 8px">
      <el-col :span="12"><el-input v-model="form.bgColor" placeholder="背景色，例如 #ffffff" /></el-col>
      <el-col :span="12"><el-input v-model="form.textColor" placeholder="文字色，例如 #333333" /></el-col>
    </el-row>
    <el-button type="primary" style="margin-top: 8px" @click="submit">创建分享</el-button>
  </el-card>

  <el-card class="glow-card" style="margin-top: 16px">
    <template #header>我的分享记录（{{ rows.length }}）</template>
    <StateBlock :loading="loading" :error="error" :empty="!rows.length" empty-text="暂无歌词分享记录">
      <el-table :data="rows">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="lyricsId" label="歌词ID" width="120" />
        <el-table-column prop="shareText" label="文案" min-width="260" />
      </el-table>
    </StateBlock>
  </el-card>
</template>
