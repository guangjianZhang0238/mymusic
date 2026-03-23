<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { addCommentApi, deleteCommentApi, getCommentsApi, likeCommentApi, unlikeCommentApi } from '@/api/social'
import StateBlock from '@/components/StateBlock.vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const songId = Number(route.params.songId)
const rows = ref<any[]>([])
const content = ref('')
const loading = ref(false)
const error = ref('')

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    const page = await getCommentsApi(songId, 1, 50)
    rows.value = page.records || []
  } catch (e: any) {
    error.value = e?.message || '评论加载失败'
  } finally {
    loading.value = false
  }
}
onMounted(load)

const create = async () => {
  if (!content.value.trim()) return ElMessage.warning('评论内容不能为空')
  try {
    await addCommentApi(songId, content.value)
    content.value = ''
    await load()
    ElMessage.success('评论已发布')
  } catch (e: any) {
    ElMessage.error(e?.message || '评论发布失败')
  }
}
const remove = async (id: number) => {
  try {
    await deleteCommentApi(id)
    await load()
    ElMessage.success('删除成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}
const like = async (row: any) => {
  try {
    await likeCommentApi(row.id)
    await load()
  } catch (e: any) {
    ElMessage.error(e?.message || '点赞失败')
  }
}
const unlike = async (row: any) => {
  try {
    await unlikeCommentApi(row.id)
    await load()
  } catch (e: any) {
    ElMessage.error(e?.message || '取消点赞失败')
  }
}
</script>

<template>
  <h2 class="page-title">评论</h2>
  <el-card class="glow-card">
    <template #header>发布评论</template>
    <el-input v-model="content" type="textarea" :rows="3" maxlength="300" show-word-limit placeholder="说点什么..." />
    <el-button style="margin-top: 10px" type="primary" @click="create">发布评论</el-button>
  </el-card>

  <el-card class="glow-card" style="margin-top: 16px">
    <template #header>全部评论（{{ rows.length }}）</template>
    <StateBlock :loading="loading" :error="error" :empty="!rows.length" empty-text="暂无评论">
      <el-table :data="rows">
        <el-table-column prop="content" label="内容" min-width="260" />
        <el-table-column prop="likeCount" label="点赞" width="90" />
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <el-space>
              <el-button text @click="like(row)">点赞</el-button>
              <el-button text @click="unlike(row)">取消赞</el-button>
              <el-popconfirm title="确认删除这条评论吗？" @confirm="remove(row.id)">
                <template #reference><el-button text type="danger">删除</el-button></template>
              </el-popconfirm>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
    </StateBlock>
  </el-card>
</template>
