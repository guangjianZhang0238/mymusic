<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createPlaylistApi, deletePlaylistApi, getUserPlaylistsApi, updatePlaylistApi, uploadPlaylistCoverApi } from '@/api/music'
import StateBlock from '@/components/StateBlock.vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const playlists = ref<any[]>([])
const visible = ref(false)
const form = reactive({ id: 0, name: '', description: '' })
const loading = ref(false)
const error = ref('')

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    playlists.value = await getUserPlaylistsApi()
  } catch (e: any) {
    error.value = e?.message || '歌单加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)

const openCreate = () => {
  form.id = 0
  form.name = ''
  form.description = ''
  visible.value = true
}

const openEdit = (row: any) => {
  form.id = row.id
  form.name = row.name
  form.description = row.description || ''
  visible.value = true
}

const submit = async () => {
  if (!form.name.trim()) return ElMessage.warning('请输入歌单名')
  try {
    if (form.id) await updatePlaylistApi(form.id, form.name, form.description)
    else await createPlaylistApi(form.name, form.description)
    visible.value = false
    await load()
    ElMessage.success('保存成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  }
}

const remove = async (id: number) => {
  try {
    await deletePlaylistApi(id)
    await load()
    ElMessage.success('删除成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

const uploadCover = async (id: number, e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  try {
    await uploadPlaylistCoverApi(id, file)
    await load()
    ElMessage.success('封面更新成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '上传失败')
  }
}
</script>

<template>
  <div class="page-head">
    <h2 class="page-title">歌单</h2>
    <el-button type="primary" @click="openCreate">新建歌单</el-button>
  </div>
  <StateBlock :loading="loading" :error="error" :empty="!playlists.length" empty-text="还没有歌单，先创建一个">
    <el-card class="glow-card">
      <el-table :data="playlists">
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="description" label="描述" />
        <el-table-column label="封面上传">
          <template #default="{ row }"><input class="soft-input" type="file" accept="image/*" @change="uploadCover(row.id, $event)" /></template>
        </el-table-column>
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button text @click="router.push(`/playlist/${row.id}`)">详情</el-button>
            <el-button text @click="openEdit(row)">编辑</el-button>
            <el-button text type="danger" @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </StateBlock>

  <el-dialog v-model="visible" :title="form.id ? '编辑歌单' : '新建歌单'">
    <el-input v-model="form.name" placeholder="歌单名" />
    <el-input v-model="form.description" style="margin-top: 10px" placeholder="描述" />
    <template #footer><el-button @click="visible = false">取消</el-button><el-button type="primary" @click="submit">保存</el-button></template>
  </el-dialog>
</template>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.soft-input {
  padding: 6px 8px;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.4);
  background: rgba(255, 255, 255, 0.9);
}
</style>
