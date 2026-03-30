<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { addCommentApi, deleteCommentApi, getCommentsApi, likeCommentApi, unlikeCommentApi } from '@/api/social'
import StateBlock from '@/components/StateBlock.vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const songId = computed(() => Number(route.params.songId || 0))
const rows = ref<any[]>([])
const content = ref('')
const loading = ref(false)
const submitting = ref(false)
const error = ref('')

const quickPresets = ['太好听了', '单曲循环中', '这句歌词太戳我', '今天心情就靠这首了']

const canSubmit = computed(() => !!content.value.trim() && !submitting.value)

const load = async () => {
  if (!songId.value) {
    rows.value = []
    return
  }
  loading.value = true
  error.value = ''
  try {
    const page = await getCommentsApi(songId.value, 1, 80)
    rows.value = page.records || []
  } catch (e: any) {
    error.value = e?.message || '评论加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)

const appendPreset = (text: string) => {
  const phrase = String(text || '').trim()
  if (!phrase) return
  const current = content.value.trim()
  const next = current ? `${current} ${phrase}` : phrase
  content.value = next.slice(0, 300)
}

const create = async () => {
  if (!songId.value) return
  const payload = content.value.trim()
  if (!payload) return ElMessage.warning('评论内容不能为空')
  if (submitting.value) return
  submitting.value = true
  try {
    await addCommentApi(songId.value, payload)
    content.value = ''
    await load()
    ElMessage.success('评论已发布')
  } catch (e: any) {
    ElMessage.error(e?.message || '评论发布失败')
  } finally {
    submitting.value = false
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
  <section class="comments-page">
    <div class="comments-header">
      <h2 class="page-title">歌曲评论</h2>
      <p class="comments-subtitle">手机单手也能顺手评论</p>
    </div>

    <el-card class="glow-card comments-card">
      <div class="quick-preset-row">
        <button
          v-for="item in quickPresets"
          :key="item"
          class="quick-preset-btn"
          type="button"
          @click="appendPreset(item)"
        >
          {{ item }}
        </button>
      </div>

      <StateBlock :loading="loading" :error="error" :empty="!rows.length" empty-text="暂无评论">
        <div class="comment-list">
          <article v-for="row in rows" :key="row.id" class="comment-item">
            <p class="comment-content">{{ row.content }}</p>
            <div class="comment-meta">
              <span class="comment-like">👍 {{ Number(row.likeCount || 0) }}</span>
              <div class="comment-actions">
                <button class="comment-action-btn" type="button" @click="like(row)">赞</button>
                <button class="comment-action-btn" type="button" @click="unlike(row)">取消</button>
                <el-popconfirm title="确认删除这条评论吗？" @confirm="remove(row.id)">
                  <template #reference>
                    <button class="comment-action-btn danger" type="button">删</button>
                  </template>
                </el-popconfirm>
              </div>
            </div>
          </article>
        </div>
      </StateBlock>

      <div class="comment-composer">
        <el-input
          v-model="content"
          class="comment-input"
          type="textarea"
          :rows="2"
          maxlength="300"
          show-word-limit
          resize="none"
          placeholder="写下你的感受…"
        />
        <el-button class="comment-submit-btn" type="primary" :loading="submitting" :disabled="!canSubmit" @click="create">
          发布
        </el-button>
      </div>
    </el-card>
  </section>
</template>

<style scoped>
.comments-page {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.comments-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 8px;
}

.comments-subtitle {
  margin: 0;
  color: #8e8e93;
  font-size: 12px;
}

.comments-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.quick-preset-row {
  display: flex;
  align-items: center;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.quick-preset-row::-webkit-scrollbar {
  display: none;
}

.quick-preset-btn {
  border: 1px solid rgba(10, 132, 255, 0.2);
  background: rgba(10, 132, 255, 0.08);
  color: #0a84ff;
  border-radius: 999px;
  min-height: 30px;
  padding: 0 12px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-bottom: 110px;
}

.comment-item {
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.9);
  padding: 10px 12px;
}

.comment-content {
  margin: 0;
  font-size: 14px;
  line-height: 1.55;
  color: #1f2937;
}

.comment-meta {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.comment-like {
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
}

.comment-actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.comment-action-btn {
  border: 1px solid rgba(148, 163, 184, 0.34);
  background: #ffffff;
  color: #334155;
  border-radius: 999px;
  min-height: 30px;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 600;
}

.comment-action-btn.danger {
  border-color: rgba(239, 68, 68, 0.28);
  color: #be123c;
  background: #fff1f2;
}

.comment-composer {
  position: sticky;
  bottom: calc(58px + env(safe-area-inset-bottom));
  z-index: 3;
  margin-top: auto;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  padding: 10px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(8px);
}

.comment-input {
  min-width: 0;
}

.comment-input :deep(.el-textarea__inner) {
  min-height: 72px;
}

.comment-submit-btn {
  min-height: 40px;
  padding-left: 16px;
  padding-right: 16px;
}

@media (max-width: 960px) {
  .comments-subtitle {
    display: none;
  }

  .comment-list {
    padding-bottom: 132px;
  }
}
</style>
