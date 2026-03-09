<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">歌词管理</h2>
      <div class="header-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索歌词"
          style="width: 300px; margin-right: 10px"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button @click="handleSearch"><el-icon><Search /></el-icon></el-button>
          </template>
        </el-input>
        <el-button type="primary" @click="handleAddLyrics">
          <el-icon><Plus /></el-icon>
          添加歌词
        </el-button>
        <el-button type="info" @click="handleAutoSyncLyrics" :disabled="syncProgress.isRunning">
          <el-icon><Refresh /></el-icon>
          {{ syncProgress.isRunning ? '同步中...' : '自动同步歌词' }}
        </el-button>
      </div>
    </div>
    
    <!-- 歌词同步进度弹窗 -->
    <el-dialog
      v-model="syncDialogVisible"
      title="歌词同步进度"
      width="500px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
    >
      <div class="sync-progress-content">
        <div class="progress-info">
          <span class="status-text">{{ syncProgress.message }}</span>
          <span class="progress-percent">{{ syncProgress.progress }}%</span>
        </div>
        <el-progress 
          :percentage="syncProgress.progress" 
          :status="syncProgress.status === 'FAILED' ? 'exception' : 'success'"
          striped
          striped-flow
        />
        <div class="progress-details">
          <div class="detail-item">
            <span class="label">总计:</span>
            <span class="value">{{ syncProgress.totalCount }} 首</span>
          </div>
          <div class="detail-item">
            <span class="label">已处理:</span>
            <span class="value">{{ syncProgress.processedCount }} 首</span>
          </div>
          <div class="detail-item">
            <span class="label">成功:</span>
            <span class="value success">{{ syncProgress.successCount }} 首</span>
          </div>
        </div>
        
        <!-- 步骤详情 -->
        <div class="steps-container" v-if="syncSteps.length > 0">
          <div class="steps-title">同步步骤:</div>
          <div class="steps-list">
            <div 
              v-for="(step, index) in syncSteps" 
              :key="index" 
              class="step-item"
              :class="{
                'current': step.status === 'current',
                'completed': step.status === 'completed',
                'failed': step.status === 'failed'
              }"
            >
              <div class="step-icon">
                <el-icon v-if="step.status === 'completed'"><Check /></el-icon>
                <el-icon v-else-if="step.status === 'failed'"><Close /></el-icon>
                <el-icon v-else-if="step.status === 'current'"><Loading /></el-icon>
                <span v-else>{{ index + 1 }}</span>
              </div>
              <div class="step-content">
                <div class="step-title">{{ step.title }}</div>
                <div class="step-desc" v-if="step.description">{{ step.description }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button 
            v-if="syncProgress.status === 'FAILED' || syncProgress.status === 'COMPLETED'"
            type="primary" 
            @click="closeSyncDialog"
          >
            关闭
          </el-button>
          <el-button 
            v-else
            type="danger" 
            @click="cancelSyncTask"
          >
            取消同步
          </el-button>
        </div>
      </template>
    </el-dialog>
    
    <el-table
      v-loading="loading"
      :data="lyricsList"
      style="width: 100%"
      border
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="songTitle" label="歌曲名称" />
      <el-table-column prop="artistName" label="歌手名称" />
      <el-table-column prop="albumName" label="专辑名称" />
      <el-table-column prop="content" label="歌词内容" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-switch
            v-model="scope.row.status"
            @change="handleStatusChange(scope.row)"
            active-value="1"
            inactive-value="0"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="scope">
          <el-button size="small" @click="handleEditLyrics(scope.row)">
            编辑
          </el-button>
          <el-button
            size="small"
            type="danger"
            @click="handleDeleteLyrics(scope.row.id)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <div class="pagination-container">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
    
    <!-- 歌词编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
    >
      <el-form
        ref="lyricsFormRef"
        :model="lyricsForm"
        :rules="lyricsRules"
      >
        <el-form-item label="歌曲" prop="songId">
          <el-select v-model="lyricsForm.songId" placeholder="请选择歌曲">
            <el-option
              v-for="song in songList"
              :key="song.id"
              :label="song.title"
              :value="song.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="歌词内容" prop="content">
          <el-input
            v-model="lyricsForm.content"
            type="textarea"
            rows="10"
            placeholder="请输入歌词内容"
          />
        </el-form-item>
        <el-form-item label="上传歌词文件">
          <el-upload
            class="lyrics-uploader"
            action="/api/lyrics/upload"
            :headers="{ Authorization: `Bearer ${token}` }"
            :data="{ songId: lyricsForm.songId }"
            :show-file-list="false"
            :on-success="handleLyricsFileSuccess"
            :before-upload="beforeUploadLyricsFile"
          >
            <el-button size="small" type="primary">
              <el-icon><Upload /></el-icon>
              上传歌词文件
            </el-button>
          </el-upload>
          <div class="upload-tip">
            提示：支持上传.lrc格式的歌词文件，上传后会自动填充歌词内容
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveLyrics">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Refresh, Check, Close, Loading } from '@element-plus/icons-vue'
import * as lyricsApi from '@/api/lyrics'
import * as songApi from '@/api/song'
import * as lyricsSyncApi from '@/api/lyrics-sync'

const userStore = useUserStore()
const token = computed(() => userStore.token)

const loading = ref(false)
const lyricsList = ref<any[]>([])
const songList = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('添加歌词')
const lyricsFormRef = ref()
const lyricsForm = reactive({
  id: 0,
  songId: 0,
  content: ''
})

// 歌词同步进度相关状态
const syncProgress = ref({
  taskId: null as number | null,
  progress: 0,
  status: '',
  message: '',
  isRunning: false,
  totalCount: 0,
  processedCount: 0,
  successCount: 0
})

// 同步弹窗状态
const syncDialogVisible = ref(false)

// 同步步骤详情
const syncSteps = ref([
  { title: '初始化任务', description: '创建同步任务并准备数据', status: 'pending' },
  { title: '查询歌曲', description: '查找需要同步歌词的歌曲', status: 'pending' },
  { title: '匹配歌词', description: '为每首歌曲匹配歌词', status: 'pending' },
  { title: '保存结果', description: '保存同步成功的歌词', status: 'pending' },
  { title: '完成同步', description: '同步任务完成', status: 'pending' }
])

let progressTimer: number | null = null

const lyricsRules = {
  songId: [
    { required: true, message: '请选择歌曲', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入歌词内容', trigger: 'blur' }
  ]
}

const loadSongs = async () => {
  try {
    // 调用API获取歌曲列表
    const response = await songApi.getSongList({ current: 1, size: 100 })
    songList.value = response.records || []
  } catch (error: any) {
    ElMessage.error(error.message || '获取歌曲列表失败')
  }
}

const loadLyrics = async () => {
  loading.value = true
  try {
    // 调用API获取歌词列表
    const response = await lyricsApi.getLyricsList({
      current: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value
    })
    
    lyricsList.value = response.records || []
    total.value = response.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '获取歌词列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadLyrics()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  loadLyrics()
}

const handleCurrentChange = (current: number) => {
  currentPage.value = current
  loadLyrics()
}

const handleAddLyrics = () => {
  dialogTitle.value = '添加歌词'
  Object.assign(lyricsForm, {
    id: 0,
    songId: 0,
    content: ''
  })
  dialogVisible.value = true
}

const handleEditLyrics = (lyrics: any) => {
  dialogTitle.value = '编辑歌词'
  Object.assign(lyricsForm, lyrics)
  dialogVisible.value = true
}

const handleSaveLyrics = async () => {
  if (!lyricsFormRef.value) return
  
  const valid = await lyricsFormRef.value.validate()
  if (!valid) return
  
  try {
    if (lyricsForm.id) {
      // 更新歌词
      await lyricsApi.updateLyrics(lyricsForm)
      ElMessage.success('歌词更新成功')
    } else {
      // 创建歌词
      await lyricsApi.createLyrics(lyricsForm)
      ElMessage.success('歌词添加成功')
    }
    dialogVisible.value = false
    loadLyrics()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleDeleteLyrics = async (id: number) => {
  await ElMessageBox.confirm('确定要删除这个歌词吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  
  try {
    await lyricsApi.deleteLyrics(id)
    ElMessage.success('歌词删除成功')
    loadLyrics()
  } catch (error: any) {
    ElMessage.error(error.message || '删除失败')
  }
}

const handleStatusChange = async (lyrics: any) => {
  try {
    await lyricsApi.updateLyrics({
      id: lyrics.id,
      status: lyrics.status
    })
  } catch (error) {
    lyrics.status = lyrics.status === 1 ? 0 : 1
    ElMessage.error('状态更新失败')
  }
}

const handleLyricsFileSuccess = (response: any) => {
  if (response.content) {
    lyricsForm.content = response.content
    ElMessage.success('歌词文件上传成功，已自动填充歌词内容')
  }
}

const beforeUploadLyricsFile = (file: File) => {
  const isLrc = file.name.endsWith('.lrc')
  if (!isLrc) {
    ElMessage.error('只支持上传.lrc格式的歌词文件')
    return false
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    ElMessage.error('上传文件大小不能超过 2MB')
    return false
  }
  if (!lyricsForm.songId) {
    ElMessage.error('请先选择歌曲')
    return false
  }
  return true
}

const handleAutoSyncLyrics = async () => {
  try {
    await ElMessageBox.confirm('确定要自动同步歌词吗？这将会为没有歌词的歌曲匹配歌词。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })
    
    // 重置步骤状态
    syncSteps.value.forEach(step => step.status = 'pending')
    
    // 启动异步同步任务
    const response = await lyricsSyncApi.startLyricsSync()
    const taskId = response
    
    if (!taskId) {
      ElMessage.error('启动同步任务失败')
      return
    }
    
    // 设置任务状态
    syncProgress.value.taskId = taskId
    syncProgress.value.isRunning = true
    syncProgress.value.progress = 0
    syncProgress.value.status = 'RUNNING'
    syncProgress.value.message = '任务已启动...'
    
    // 更新步骤状态
    syncSteps.value[0].status = 'completed'
    syncSteps.value[1].status = 'current'
    
    // 显示进度弹窗
    syncDialogVisible.value = true
    
    // 开始轮询进度
    startProgressPolling()
    
    ElMessage.success('歌词同步任务已启动')
    
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '启动同步任务失败')
    }
  }
}

// 关闭同步弹窗
const closeSyncDialog = () => {
  syncDialogVisible.value = false
  // 重置状态
  syncProgress.value.isRunning = false
  syncProgress.value.taskId = null
  loadLyrics() // 重新加载歌词列表
}

// 开始轮询进度
const startProgressPolling = () => {
  // 清除之前的定时器
  if (progressTimer) {
    clearInterval(progressTimer)
  }
  
  // 确保有有效的taskId
  if (!syncProgress.value.taskId) {
    console.error('任务ID不存在')
    return
  }
  
  console.log('开始轮询歌词同步进度，任务ID:', syncProgress.value.taskId)
  
  // 每2秒查询一次进度
  progressTimer = window.setInterval(async () => {
    try {
      console.log('请求歌词同步进度，任务ID:', syncProgress.value.taskId)
      const response = await lyricsSyncApi.getLyricsSyncProgress(syncProgress.value.taskId!)
      console.log('歌词同步原始响应:', response)
      
      // 更新UI状态
      syncProgress.value.progress = response.progress || 0
      syncProgress.value.status = response.status || ''
      syncProgress.value.message = response.message || ''
      syncProgress.value.totalCount = response.totalCount || 0
      syncProgress.value.processedCount = response.processedCount || 0
      syncProgress.value.successCount = response.successCount || 0
      
      // 更新步骤状态
      updateSyncSteps(response.progress || 0, response.status || '')
      
      console.log('更新后歌词同步状态:', {
        progress: syncProgress.value.progress,
        status: syncProgress.value.status,
        message: syncProgress.value.message,
        totalCount: syncProgress.value.totalCount,
        processedCount: syncProgress.value.processedCount,
        successCount: syncProgress.value.successCount
      })
      
      // 检查任务是否完成
      if (response.status === 'COMPLETED') {
        console.log('歌词同步任务已完成，停止轮询')
        stopProgressPolling()
        syncProgress.value.isRunning = false
        syncSteps.value[syncSteps.value.length - 1].status = 'completed'
        ElMessage.success(response.message)
        // 2秒后自动关闭弹窗
        setTimeout(() => {
          closeSyncDialog()
        }, 2000)
      } else if (response.status === 'FAILED') {
        console.log('歌词同步任务失败，停止轮询')
        stopProgressPolling()
        syncProgress.value.isRunning = false
        syncSteps.value.forEach(step => {
          if (step.status === 'current') step.status = 'failed'
        })
        ElMessage.error(`同步失败: ${response.errorMessage || response.message}`)
      } else {
        console.log('歌词同步任务仍在运行中...')
      }
      
    } catch (error: any) {
      console.error('获取歌词同步进度失败:', error)
      // 如果是404错误，可能任务已不存在
      if (error.response?.status === 404) {
        console.log('歌词同步任务不存在，停止轮询')
        stopProgressPolling()
        syncProgress.value.isRunning = false
        syncSteps.value.forEach(step => {
          if (step.status === 'current') step.status = 'failed'
        })
        ElMessage.error('任务不存在或已过期')
      }
    }
  }, 2000)
}

// 更新同步步骤状态
const updateSyncSteps = (progress: number, status: string) => {
  if (progress >= 5 && progress < 10) {
    syncSteps.value[1].status = 'completed'
    syncSteps.value[2].status = 'current'
  } else if (progress >= 10 && progress < 90) {
    syncSteps.value[2].status = 'current'
  } else if (progress >= 90) {
    syncSteps.value[2].status = 'completed'
    syncSteps.value[3].status = 'current'
  }
  
  if (status === 'COMPLETED') {
    syncSteps.value[3].status = 'completed'
    syncSteps.value[4].status = 'completed'
  } else if (status === 'FAILED') {
    syncSteps.value.forEach(step => {
      if (step.status === 'current') step.status = 'failed'
    })
  }
}

// 停止轮询进度
const stopProgressPolling = () => {
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
}

// 取消同步任务
const cancelSyncTask = async () => {
  if (!syncProgress.value.taskId) return
  
  try {
    await lyricsSyncApi.cancelLyricsSync(syncProgress.value.taskId)
    stopProgressPolling()
    syncProgress.value.isRunning = false
    syncSteps.value.forEach(step => {
      if (step.status === 'current') step.status = 'failed'
    })
    syncDialogVisible.value = false
    ElMessage.success('同步任务已取消')
  } catch (error) {
    console.error('取消同步任务失败:', error)
    ElMessage.error('取消任务失败')
  }
}

// 组件卸载时清理定时器
onUnmounted(() => {
  stopProgressPolling()
})

onMounted(() => {
  loadSongs()
  loadLyrics()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 歌词同步进度显示样式 */
.sync-progress-container {
  margin-top: 15px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.status-text {
  font-size: 14px;
  color: #606266;
  flex: 1;
}

.progress-percent {
  font-size: 14px;
  font-weight: bold;
  color: #409eff;
  min-width: 40px;
  text-align: right;
}

.progress-details {
  display: flex;
  justify-content: space-between;
  margin: 10px 0;
  font-size: 12px;
  color: #909399;
}

.progress-actions {
  margin-top: 15px;
  text-align: right;
}

:deep(.el-progress-bar__inner) {
  transition: width 0.3s ease;
}

/* 歌词同步弹窗样式 */
.sync-progress-content {
  padding: 20px 0;
}

.sync-progress-content .progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.sync-progress-content .status-text {
  font-size: 14px;
  color: #606266;
  flex: 1;
}

.sync-progress-content .progress-percent {
  font-size: 16px;
  font-weight: bold;
  color: #409eff;
  min-width: 50px;
  text-align: right;
}

.progress-details {
  display: flex;
  justify-content: space-between;
  margin: 20px 0;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.detail-item {
  text-align: center;
}

.detail-item .label {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 5px;
}

.detail-item .value {
  display: block;
  font-size: 16px;
  font-weight: bold;
  color: #606266;
}

.detail-item .value.success {
  color: #67c23a;
}

/* 步骤容器样式 */
.steps-container {
  margin-top: 25px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.steps-title {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 15px;
}

.steps-list {
  position: relative;
}

.step-item {
  display: flex;
  margin-bottom: 15px;
  opacity: 0.6;
  transition: all 0.3s ease;
}

.step-item.current {
  opacity: 1;
  transform: scale(1.02);
}

.step-item.completed {
  opacity: 1;
  color: #67c23a;
}

.step-item.failed {
  opacity: 1;
  color: #f56c6c;
}

.step-icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background-color: #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  flex-shrink: 0;
  font-size: 12px;
  font-weight: bold;
  color: #909399;
}

.step-item.completed .step-icon {
  background-color: #67c23a;
  color: white;
}

.step-item.failed .step-icon {
  background-color: #f56c6c;
  color: white;
}

.step-item.current .step-icon {
  background-color: #409eff;
  color: white;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.1); }
  100% { transform: scale(1); }
}

.step-content {
  flex: 1;
}

.step-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 3px;
}

.step-desc {
  font-size: 12px;
  color: #909399;
}

.dialog-footer {
  text-align: right;
}

.step-item.completed .step-title,
.step-item.completed .step-desc {
  color: #67c23a;
}

.step-item.failed .step-title,
.step-item.failed .step-desc {
  color: #f56c6c;
}

.step-item.current .step-title,
.step-item.current .step-desc {
  color: #409eff;
  font-weight: 500;
}
</style>