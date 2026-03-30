<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { createFeedbackApi, getMyFeedbackApi } from '@/api/social'
import StateBlock from '@/components/StateBlock.vue'
import { ElMessage } from 'element-plus'

const rows = ref<any[]>([])
const form = reactive({ type: 'BUG', content: '', contact: '', scene: 'music-web' })
const loading = ref(false)
const error = ref('')

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    const page = await getMyFeedbackApi(1, 30)
    rows.value = page.records || []
  } catch (e: any) {
    error.value = e?.message || '反馈列表加载失败'
  } finally {
    loading.value = false
  }
}
onMounted(load)

const submit = async () => {
  if (!form.content.trim()) return ElMessage.warning('请输入反馈内容')
  try {
    await createFeedbackApi(form)
    form.content = ''
    await load()
    ElMessage.success('反馈提交成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '反馈提交失败')
  }
}
</script>

<template>
  <h2 class="page-title">反馈</h2>
  <el-card class="glow-card feedback-form-card">
    <template #header>提交新反馈</template>
    <el-select v-model="form.type" class="feedback-type-select">
      <el-option label="问题" value="BUG" />
      <el-option label="建议" value="SUGGESTION" />
      <el-option label="功能" value="FEATURE" />
    </el-select>
    <el-input v-model="form.content" type="textarea" :rows="4" maxlength="400" show-word-limit placeholder="请描述你遇到的问题或建议" style="margin-top: 8px" />
    <el-input v-model="form.contact" placeholder="联系方式（选填）" style="margin-top: 8px" />
    <el-button class="feedback-submit-btn" type="primary" style="margin-top: 8px" @click="submit">提交反馈</el-button>
  </el-card>

  <el-card class="glow-card" style="margin-top: 16px">
    <template #header>历史反馈（{{ rows.length }}）</template>
    <StateBlock :loading="loading" :error="error" :empty="!rows.length" empty-text="暂无反馈记录">
      <el-table :data="rows">
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="content" label="内容" min-width="260" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="createTime" label="时间" min-width="180" />
      </el-table>
    </StateBlock>
  </el-card>
</template>

<style scoped>
.feedback-type-select {
  width: 180px;
}

@media (max-width: 960px) {
  .feedback-type-select {
    width: 100%;
  }

  .feedback-submit-btn {
    width: 100%;
  }
}
</style>
