<template>
  <div class="feedback-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户反馈列表</span>
          <el-button type="primary" @click="loadFeedbacks">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>
      
      <!-- 筛选条件 -->
      <el-form :inline="true" class="filter-form">
        <el-form-item label="处理状态">
          <el-select 
            v-model="filterStatus" 
            placeholder="全部" 
            clearable 
            @change="loadFeedbacks"
            class="filter-select"
          >
            <el-option label="待处理" value="PENDING" />
            <el-option label="已解决" value="RESOLVED" />
            <el-option label="后续版本解决" value="FUTURE" />
            <el-option label="无法解决" value="UNABLE" />
          </el-select>
        </el-form-item>
        <el-form-item label="反馈类型">
          <el-select 
            v-model="filterType" 
            placeholder="全部" 
            clearable 
            @change="loadFeedbacks"
            class="filter-select"
          >
            <el-option label="没有歌词" value="NO_LYRICS" />
            <el-option label="歌词错误" value="LYRICS_ERROR" />
            <el-option label="歌词偏移" value="LYRICS_OFFSET" />
            <el-option label="歌曲缺失" value="SONG_MISSING" />
            <el-option label="其他问题" value="OTHER" />
          </el-select>
        </el-form-item>
      </el-form>
      
      <!-- 反馈列表 -->
      <el-table :data="feedbacks" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)">{{ getTypeName(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="200">
          <template #default="{ row }">
            <div class="content-cell">
              {{ row.content }}
              <div v-if="row.songTitle" class="song-info">
                <el-tag size="small" type="warning">歌曲: {{ row.songTitle }}</el-tag>
                <el-tag v-if="row.artistName" size="small" type="info" style="margin-left:4px">歌手: {{ row.artistName }}</el-tag>
              </div>
              <div v-else-if="row.songId" class="song-info">
                <el-tag size="small" type="warning">歌曲ID: {{ row.songId }}</el-tag>
              </div>
              <div v-if="row.keyword" class="keyword-info">
                <el-tag size="small" type="info">关键词: {{ row.keyword }}</el-tag>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="scene" label="场景" width="120" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">{{ getStatusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button 
              v-if="row.status === 'PENDING'" 
              type="primary" 
              size="small"
              @click="openHandleDialog(row)"
            >
              处理
            </el-button>
            <el-button 
              v-else 
              type="info" 
              size="small"
              @click="openHandleDialog(row)"
            >
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadFeedbacks"
        @current-change="loadFeedbacks"
        class="pagination"
      />
    </el-card>
    
    <!-- 通用处理对话框（非NO_LYRICS类型） -->
    <el-dialog 
      v-model="handleDialogVisible" 
      :title="currentFeedback?.status === 'PENDING' ? '处理反馈' : '反馈详情'"
      width="500px"
    >
      <el-form :model="handleForm" label-width="100px" v-if="currentFeedback">
        <el-form-item label="反馈ID">
          <span>{{ currentFeedback.id }}</span>
        </el-form-item>
        <el-form-item label="用户ID">
          <span>{{ currentFeedback.userId }}</span>
        </el-form-item>
        <el-form-item label="反馈类型">
          <el-tag :type="getTypeTagType(currentFeedback.type)">{{ getTypeName(currentFeedback.type) }}</el-tag>
        </el-form-item>
        <el-form-item label="反馈内容">
          <div class="dialog-content">{{ currentFeedback.content }}</div>
        </el-form-item>
        <el-form-item v-if="currentFeedback.songTitle" label="歌曲">
          <span>{{ currentFeedback.songTitle }}
            <template v-if="currentFeedback.artistName"> - {{ currentFeedback.artistName }}</template>
            <template v-if="currentFeedback.albumName"> 《{{ currentFeedback.albumName }}》</template>
          </span>
        </el-form-item>
        <el-form-item label="联系方式" v-if="currentFeedback.contact">
          <span>{{ currentFeedback.contact }}</span>
        </el-form-item>
        <el-form-item label="提交时间">
          <span>{{ formatTime(currentFeedback.createTime) }}</span>
        </el-form-item>
        <el-divider />
        <el-form-item label="处理状态">
          <el-radio-group v-model="handleForm.status" :disabled="!canEdit">
            <el-radio label="RESOLVED">已解决</el-radio>
            <el-radio label="FUTURE">后续版本解决</el-radio>
            <el-radio label="UNABLE">无法解决</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处理意见">
          <el-input 
            v-model="handleForm.handleNote" 
            type="textarea" 
            :rows="3"
            placeholder="请输入处理意见（选填）"
            :disabled="!canEdit"
          />
        </el-form-item>
        <el-form-item label="处理时间" v-if="currentFeedback.handleTime">
          <span>{{ formatTime(currentFeedback.handleTime) }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleDialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="submitHandle"
            :loading="submitting"
            v-if="canEdit"
          >
            确认处理
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 无歌词反馈处理对话框 -->
    <el-dialog
      v-model="noLyricsDialogVisible"
      title="处理无歌词反馈"
      width="720px"
      :close-on-click-modal="false"
      @closed="resetNoLyricsDialog"
    >
      <div v-if="currentFeedback">
        <el-alert
          title="处理说明"
          type="info"
          description="您可以手动输入歌词或点击'自动匹配歌词'按钮。匹配成功后可选择是否将此反馈标记为已解决。"
          show-icon
          :closable="false"
          class="mb-20"
        />
        
        <el-descriptions :column="2" border size="small" class="mb-20">
          <el-descriptions-item label="歌曲">
            {{ currentFeedback.songTitle || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="歌手">
            {{ currentFeedback.artistName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="专辑">
            {{ currentFeedback.albumName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="用户ID">
            {{ currentFeedback.userId }}
          </el-descriptions-item>
          <el-descriptions-item label="提交时间" :span="2">
            {{ formatTime(currentFeedback.createTime) }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="lyrics-editor-section">
          <div class="lyrics-editor-header">
            <span>歌词内容</span>
            <span class="lyrics-format-tip">支持 LRC 格式（如 [00:01.00]歌词文本）或纯文本</span>
          </div>
          <el-input
            v-model="lyricsContent"
            type="textarea"
            :rows="12"
            placeholder="请输入歌词内容，支持 LRC 格式或纯文本格式..."
            :disabled="!canEditNoLyrics"
          />
        </div>

        <div class="lyrics-actions" v-if="canEditNoLyrics">
          <el-button 
            type="primary" 
            @click="autoMatchLyrics"
            :loading="autoMatching"
            :disabled="!currentFeedback.songId"
            plain
          >
            <el-icon><Search /></el-icon>
            自动匹配歌词
          </el-button>
          <span class="action-tip" v-if="autoMatchResult">{{ autoMatchResult }}</span>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="noLyricsDialogVisible = false">取消</el-button>
          <el-button
            type="success"
            @click="saveLyricsAndFinish"
            :loading="savingLyrics"
            :disabled="!lyricsContent.trim() || !canEditNoLyrics"
          >
            <el-icon><Check /></el-icon>
            已匹配歌词，完成
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 是否标记已处理的确认对话框 -->
    <el-dialog
      v-model="markResolvedDialogVisible"
      title="完成歌词匹配"
      width="420px"
      :close-on-click-modal="false"
    >
      <p>歌词内容已保存到该歌曲。</p>
      <p>是否将此条反馈标记为《已解决》？</p>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="confirmMarkResolved(false)">否，保留待处理</el-button>
          <el-button type="primary" @click="confirmMarkResolved(true)">是，标记已解决</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search, Check } from '@element-plus/icons-vue'
import request from '../../api/request'

interface Feedback {
  id: number
  userId: number
  type: string
  content: string
  songId?: number
  songTitle?: string
  artistName?: string
  albumName?: string
  keyword?: string
  contact?: string
  scene?: string
  status: string
  handleNote?: string
  handleTime?: string
  createTime: string
}

const loading = ref(false)
const feedbacks = ref<Feedback[]>([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const filterStatus = ref('')
const filterType = ref('')

// 通用处理对话框
const handleDialogVisible = ref(false)
const currentFeedback = ref<Feedback | null>(null)
const handleForm = ref({
  status: '',
  handleNote: ''
})
const submitting = ref(false)

const canEdit = computed(() => currentFeedback.value?.status === 'PENDING')

// 无歌词反馈对话框
const noLyricsDialogVisible = ref(false)
const lyricsContent = ref('')
const autoMatching = ref(false)
const autoMatchResult = ref('')
const savingLyrics = ref(false)

const canEditNoLyrics = computed(() => currentFeedback.value?.status === 'PENDING')

// 是否标记已处理确认对话框
const markResolvedDialogVisible = ref(false)

// 重置无歌词对话框状态
const resetNoLyricsDialog = () => {
  lyricsContent.value = ''
  autoMatchResult.value = ''
  currentFeedback.value = null
}

const getTypeName = (type: string) => {
  const map: Record<string, string> = {
    'NO_LYRICS': '没有歌词',
    'LYRICS_ERROR': '歌词错误',
    'LYRICS_OFFSET': '歌词偏移',
    'SONG_MISSING': '歌曲缺失',
    'OTHER': '其他问题',
    'LYRICS_ISSUE': '歌词问题'
  }
  return map[type] || type
}

const getTypeTagType = (type: string) => {
  const map: Record<string, string> = {
    'NO_LYRICS': 'danger',
    'LYRICS_ERROR': 'danger',
    'LYRICS_OFFSET': 'warning',
    'SONG_MISSING': 'info',
    'OTHER': '',
    'LYRICS_ISSUE': 'warning'
  }
  return map[type] || ''
}

const getStatusName = (status: string) => {
  const map: Record<string, string> = {
    'PENDING': '待处理',
    'RESOLVED': '已解决',
    'FUTURE': '后续版本解决',
    'UNABLE': '无法解决'
  }
  return map[status] || status
}

const getStatusTagType = (status: string) => {
  const map: Record<string, string> = {
    'PENDING': 'warning',
    'RESOLVED': 'success',
    'FUTURE': 'info',
    'UNABLE': 'danger'
  }
  return map[status] || ''
}

const formatTime = (time: string) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

const loadFeedbacks = async () => {
  loading.value = true
  try {
    const params: any = {
      current: currentPage.value,
      size: pageSize.value
    }
    if (filterStatus.value) params.status = filterStatus.value
    if (filterType.value) params.type = filterType.value

    const res = await request.get('/app/music/feedback/admin/list', { params })
    const data = (res as any)?.records ? res : (res as any)?.data

    if (data && Array.isArray(data.records)) {
      feedbacks.value = data.records
      total.value = data.total || data.records.length
    } else {
      feedbacks.value = []
      total.value = 0
    }
  } catch (error: any) {
    ElMessage.error('加载反馈列表失败: ' + (error.message || '未知错误'))
    feedbacks.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const openHandleDialog = (feedback: Feedback) => {
  currentFeedback.value = feedback
  // 无歌词 / 歌词错误 类型反馈进入歌词处理对话框
  if (feedback.type === 'NO_LYRICS' || feedback.type === 'LYRICS_ERROR') {
    lyricsContent.value = ''
    autoMatchResult.value = ''
    noLyricsDialogVisible.value = true
    // 歌词错误场景下，优先展示当前已存在的歌词，方便在此基础上修改
    if (feedback.type === 'LYRICS_ERROR') {
      loadCurrentLyricsForFeedback()
    }
  } else {
    handleForm.value = {
      status: feedback.status === 'PENDING' ? 'RESOLVED' : feedback.status,
      handleNote: feedback.handleNote || ''
    }
    handleDialogVisible.value = true
  }
}

// 加载当前歌曲已有歌词，用于“歌词错误”场景下展示和编辑
const loadCurrentLyricsForFeedback = async () => {
  if (!currentFeedback.value?.songId) return
  try {
    const res = await request.get(`/lyrics/song/${currentFeedback.value.songId}`)
    if (res && (res as any).content) {
      const normalizedContent = String((res as any).content)
        .replace(/\\r\\n/g, '\n')
        .replace(/\\n/g, '\n')
      lyricsContent.value = normalizedContent
    } else {
      lyricsContent.value = ''
    }
  } catch (error: any) {
    ElMessage.error('获取当前歌词失败: ' + (error.message || '未知错误'))
  }
}

const submitHandle = async () => {
  if (!currentFeedback.value) return
  if (!handleForm.value.status) {
    ElMessage.warning('请选择处理状态')
    return
  }
  
  submitting.value = true
  try {
    await request.put(`/app/music/feedback/admin/handle/${currentFeedback.value.id}`, handleForm.value)
    ElMessage.success('处理成功')
    handleDialogVisible.value = false
    loadFeedbacks()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '处理失败')
  } finally {
    submitting.value = false
  }
}

// 自动匹配歌词
const autoMatchLyrics = async () => {
  if (!currentFeedback.value) return
  autoMatching.value = true
  autoMatchResult.value = ''
  try {
    const res = await request.post(`/app/music/feedback/admin/auto-match-lyrics/${currentFeedback.value.id}`)
    if (res && res.code === 200 && res.data) {
      lyricsContent.value = res.data.content || ''
      autoMatchResult.value = '自动匹配成功！'
      ElMessage.success('自动匹配歌词成功，已填入框内')
    } else {
      autoMatchResult.value = '匹配失败: ' + (res?.message || '未找到歌词')
      ElMessage.warning('自动匹配失败，可手动输入歌词')
    }
  } catch (error: any) {
    autoMatchResult.value = '匹配异常'
    ElMessage.error('自动匹配失败: ' + (error.message || '未知错误'))
  } finally {
    autoMatching.value = false
  }
}

// 保存歌词并弹出确认对话框
const saveLyricsAndFinish = async () => {
  if (!currentFeedback.value || !lyricsContent.value.trim()) return
  savingLyrics.value = true
  try {
    await request.post(
      `/app/music/feedback/admin/save-lyrics/${currentFeedback.value.id}`,
      { content: lyricsContent.value }
    )
    ElMessage.success('歌词已保存')
    noLyricsDialogVisible.value = false
    markResolvedDialogVisible.value = true
  } catch (error: any) {
    ElMessage.error('保存歌词失败: ' + (error.message || '未知错误'))
  } finally {
    savingLyrics.value = false
  }
}

// 确认是否标记已处理
const confirmMarkResolved = async (mark: boolean) => {
  markResolvedDialogVisible.value = false
  if (mark && currentFeedback.value) {
    try {
      await request.put(
        `/app/music/feedback/admin/handle/${currentFeedback.value.id}`,
        { status: 'RESOLVED', handleNote: '已匹配歌词' }
      )
      ElMessage.success('已标记为已解决')
      loadFeedbacks()
    } catch (error: any) {
      ElMessage.error('标记失败，请手动处理')
    }
  }
}

onMounted(() => {
  loadFeedbacks()
})
</script>

<style scoped lang="scss">
.feedback-list {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .filter-form {
    margin-bottom: 20px;
    
    :deep(.el-form-item__label) {
      width: 80px !important;
    }
    
    .filter-select {
      width: 180px;
    }
  }
  
  .content-cell {
    .keyword-info, .song-info {
      margin-top: 4px;
    }
  }
  
  .pagination {
    margin-top: 20px;
    justify-content: flex-end;
  }
  
  .dialog-content {
    max-height: 200px;
    overflow-y: auto;
    white-space: pre-wrap;
    word-break: break-all;
  }
}

.mb-20 {
  margin-bottom: 20px;
}

.lyrics-editor-section {
  .lyrics-editor-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    font-weight: 500;
    
    .lyrics-format-tip {
      font-size: 12px;
      color: #909399;
      font-weight: normal;
    }
  }
}

.lyrics-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  
  .action-tip {
    font-size: 13px;
    color: #67c23a;
  }
}
</style>
