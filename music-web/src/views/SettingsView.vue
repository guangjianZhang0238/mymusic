<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getUserSettingsApi, saveUserSettingApi } from '@/api/settings'
import StateBlock from '@/components/StateBlock.vue'
import { ElMessage } from 'element-plus'

const settings = ref<any[]>([])
const draft = ref({ key: '', value: '', type: 'string', description: '' })
const loading = ref(false)
const error = ref('')

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    settings.value = await getUserSettingsApi()
  } catch (e: any) {
    error.value = e?.message || '设置加载失败'
  } finally {
    loading.value = false
  }
}
onMounted(load)

const save = async () => {
  if (!draft.value.key.trim()) return ElMessage.warning('settingKey 不能为空')
  try {
    await saveUserSettingApi(draft.value.key, draft.value.value, draft.value.type, draft.value.description)
    draft.value = { key: '', value: '', type: 'string', description: '' }
    await load()
    ElMessage.success('保存成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  }
}
</script>

<template>
  <h2 class="page-title">设置</h2>
  <el-card class="glow-card">
    <template #header>新增/修改设置项</template>
    <el-row :gutter="12">
      <el-col :span="12"><el-input v-model="draft.key" placeholder="设置 Key，例如 player.volume" /></el-col>
      <el-col :span="12"><el-input v-model="draft.value" placeholder="设置 Value" /></el-col>
    </el-row>
    <el-row :gutter="12" style="margin-top: 12px">
      <el-col :span="12"><el-input v-model="draft.type" placeholder="类型（string / number / boolean）" /></el-col>
      <el-col :span="12"><el-input v-model="draft.description" placeholder="说明（选填）" /></el-col>
    </el-row>
    <el-button type="primary" style="margin-top: 12px" @click="save">保存设置</el-button>
  </el-card>

  <el-card class="glow-card" style="margin-top: 16px">
    <template #header>当前设置（{{ settings.length }}）</template>
    <StateBlock :loading="loading" :error="error" :empty="!settings.length" empty-text="暂无用户设置">
      <el-table :data="settings">
        <el-table-column prop="settingKey" label="Key" min-width="220" />
        <el-table-column prop="settingValue" label="Value" min-width="160" />
        <el-table-column prop="settingType" label="Type" width="140" />
        <el-table-column prop="description" label="说明" min-width="220" />
      </el-table>
    </StateBlock>
  </el-card>
</template>
