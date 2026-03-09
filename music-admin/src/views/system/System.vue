<template>
  <div class="system">
    <div class="page-header">
      <h2 class="page-title">系统管理</h2>
    </div>
    
    <div class="system-content">
      <el-card class="system-card">
        <template #header>
          <div class="card-header">
            <span>系统信息</span>
          </div>
        </template>
        <div class="system-info">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="系统名称">
              <span>音乐管理系统</span>
            </el-descriptions-item>
            <el-descriptions-item label="版本">
              <span>1.0.0</span>
            </el-descriptions-item>
            <el-descriptions-item label="后端地址">
              <span>http://localhost:8081</span>
            </el-descriptions-item>
            <el-descriptions-item label="前端地址">
              <span>http://localhost:5555</span>
            </el-descriptions-item>
            <el-descriptions-item label="运行环境">
              <span>开发环境</span>
            </el-descriptions-item>
            <el-descriptions-item label="当前时间">
              <span>{{ currentTime }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </el-card>
      
      <el-card class="system-card">
        <template #header>
          <div class="card-header">
            <span>系统配置</span>
          </div>
        </template>
        <div class="system-config">
          <el-form :model="systemConfig" label-width="120px">
            <el-form-item label="存储路径">
              <el-input v-model="systemConfig.storagePath" disabled />
            </el-form-item>
            <el-form-item label="最大文件大小">
              <el-input v-model="systemConfig.maxFileSize" disabled />
            </el-form-item>
            <el-form-item label="上传线程数">
              <el-input v-model="systemConfig.uploadThreadCount" disabled />
            </el-form-item>
            <el-form-item label="临时路径">
              <el-input v-model="systemConfig.tempPath" disabled />
            </el-form-item>
          </el-form>
        </div>
      </el-card>
      
      <el-card class="system-card">
        <template #header>
          <div class="card-header">
            <span>数据库信息</span>
          </div>
        </template>
        <div class="database-info">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="数据库类型">
              <span>MySQL</span>
            </el-descriptions-item>
            <el-descriptions-item label="连接状态">
              <el-tag type="success">已连接</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="数据库名称">
              <span>music</span>
            </el-descriptions-item>
            <el-descriptions-item label="用户名">
              <span>root</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </el-card>
      
      <el-card class="system-card">
        <template #header>
          <div class="card-header">
            <span>用户反馈</span>
          </div>
        </template>
        <div class="feedback-nav">
          <el-button type="primary" @click="$router.push('/feedback')">
            查看用户反馈
          </el-button>
          <p style="margin-top: 10px; color: #606266; font-size: 14px;">
            管理用户的反馈信息，包括歌词问题、歌曲缺失等问题的处理
          </p>
        </div>
      </el-card>
      
      <el-card class="system-card">
        <template #header>
          <div class="card-header">
            <span>数据清理</span>
          </div>
        </template>
        <div class="cleanup-section">
          <p class="cleanup-description">
            对数据库中的垃圾数据进行清理，按顺序执行：
            <br>① 扫描歌曲表，删除文件已不存在的歌曲记录
            <br>② 扫描歌词表，删除对应歌曲已被删除的孤立歌词记录
            <br>③ 扫描专辑表，删除清理后其下没有任何歌曲的空专辑
          </p>

          <!-- 清理进度弹窗 -->
          <el-dialog
            v-model="cleanup.dialogVisible"
            title="数据清理进度"
            width="520px"
            :close-on-click-modal="false"
            :close-on-press-escape="false"
          >
            <div class="cleanup-dialog-content">
              <div class="cleanup-status-header">
                <el-icon v-if="cleanup.status === 'RUNNING'" class="is-loading"><Loading /></el-icon>
                <el-icon v-else-if="cleanup.status === 'COMPLETED'" style="color: #67C23A"><CircleCheck /></el-icon>
                <el-icon v-else-if="cleanup.status === 'FAILED'" style="color: #F56C6C"><CircleClose /></el-icon>
                <span class="status-label">{{ cleanup.statusLabel }}</span>
              </div>

              <div class="cleanup-progress-area">
                <div class="progress-info-row">
                  <span class="progress-msg">{{ cleanup.message }}</span>
                  <span class="progress-pct">{{ cleanup.progress }}%</span>
                </div>
                <el-progress
                  :percentage="cleanup.progress"
                  :status="cleanup.status === 'FAILED' ? 'exception' : (cleanup.status === 'COMPLETED' ? 'success' : undefined)"
                  striped
                  striped-flow
                />
              </div>

              <!-- 清理结果 -->
              <div v-if="cleanup.status === 'COMPLETED' && cleanup.result" class="cleanup-result">
                <el-divider>清理结果</el-divider>
                <el-descriptions :column="3" border size="small">
                  <el-descriptions-item label="删除歌曲">
                    <el-tag type="danger" size="small">{{ cleanup.result.deletedSongs }} 条</el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="删除歌词">
                    <el-tag type="warning" size="small">{{ cleanup.result.deletedLyrics }} 条</el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="删除专辑">
                    <el-tag type="info" size="small">{{ cleanup.result.deletedAlbums }} 个</el-tag>
                  </el-descriptions-item>
                </el-descriptions>
                <div v-if="cleanup.result.details && cleanup.result.details.length" class="cleanup-details">
                  <p v-for="(detail, i) in cleanup.result.details" :key="i" class="detail-item">✔ {{ detail }}</p>
                </div>
              </div>
            </div>

            <template #footer>
              <div class="dialog-footer">
                <el-button
                  v-if="cleanup.status === 'RUNNING'"
                  type="danger"
                  size="small"
                  @click="handleCancelCleanup"
                >取消清理</el-button>
                <el-button
                  v-if="cleanup.status !== 'RUNNING'"
                  @click="cleanup.dialogVisible = false"
                >关闭</el-button>
              </div>
            </template>
          </el-dialog>

          <el-button type="danger" @click="handleStartCleanup" :loading="cleanup.status === 'RUNNING'">
            <el-icon><Delete /></el-icon>
            执行数据清理
          </el-button>
        </div>
      </el-card>
      
      <el-card class="system-card">
        <template #header>
          <div class="card-header">
            <span>操作日志</span>
          </div>
        </template>
        <div class="operation-logs">
          <el-table :data="logs" style="width: 100%" border>
            <el-table-column prop="time" label="操作时间" width="180" />
            <el-table-column prop="user" label="操作用户" width="120" />
            <el-table-column prop="action" label="操作内容" />
            <el-table-column prop="ip" label="IP地址" width="150" />
          </el-table>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Loading, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import request from '@/api/request'

const currentTime = ref('')
const timer = ref<number | null>(null)

const systemConfig = reactive({
  storagePath: 'D:\\music_source',
  maxFileSize: '500MB',
  uploadThreadCount: '5',
  tempPath: 'D:\\music_temp'
})

const logs = ref([
  {
    time: '2026-02-24 16:00:00',
    user: 'admin',
    action: '登录系统',
    ip: '127.0.0.1'
  },
  {
    time: '2026-02-24 15:30:00',
    user: 'admin',
    action: '添加歌手',
    ip: '127.0.0.1'
  },
  {
    time: '2026-02-24 15:00:00',
    user: 'admin',
    action: '上传歌曲',
    ip: '127.0.0.1'
  }
])

// 数据清理状态
const cleanup = reactive({
  dialogVisible: false,
  taskId: null as number | null,
  status: '',     // RUNNING / COMPLETED / FAILED
  progress: 0,
  message: '',
  result: null as any,
  statusLabel: ''
})

let cleanupTimer: number | null = null

const handleStartCleanup = async () => {
  try {
    await ElMessageBox.confirm(
      '数据清理会永久删除不合规数据，且不可恢复！确认要执行吗？',
      '警告',
      { confirmButtonText: '确认清理', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  try {
    const taskId = await request.post('/data-cleanup/start')
    if (taskId === undefined || taskId === null) {
      ElMessage.error('启动失败：未能获取任务ID')
      return
    }

    cleanup.taskId = taskId
    cleanup.status = 'RUNNING'
    cleanup.progress = 0
    cleanup.message = '清理任务已启动...'
    cleanup.result = null
    cleanup.statusLabel = '正在清理...'
    cleanup.dialogVisible = true

    startCleanupPolling()
  } catch (e: any) {
    ElMessage.error('启动清理失败：' + (e.message || e))
  }
}

const startCleanupPolling = () => {
  if (cleanupTimer) clearInterval(cleanupTimer)
  if (!cleanup.taskId) return

  cleanupTimer = window.setInterval(async () => {
    try {
      const data = await request.get(`/data-cleanup/progress/${cleanup.taskId}`)
      const progressData = data?.data ?? data

      cleanup.progress = progressData.progress ?? 0
      cleanup.status = progressData.status ?? ''
      cleanup.message = progressData.message ?? ''

      if (progressData.status === 'COMPLETED') {
        cleanup.statusLabel = '清理完成'
        cleanup.result = progressData.cleanupResult ?? null
        stopCleanupPolling()
        ElMessage.success(progressData.message || '数据清理完成')
      } else if (progressData.status === 'FAILED') {
        cleanup.statusLabel = '清理失败'
        stopCleanupPolling()
        ElMessage.error('清理失败: ' + (progressData.errorMessage || progressData.message))
      } else {
        cleanup.statusLabel = '正在清理...'
      }
    } catch (e: any) {
      console.error('获取清理进度失败:', e)
    }
  }, 2000)
}

const stopCleanupPolling = () => {
  if (cleanupTimer) {
    clearInterval(cleanupTimer)
    cleanupTimer = null
  }
}

const handleCancelCleanup = async () => {
  if (!cleanup.taskId) return
  try {
    await request.post(`/data-cleanup/cancel/${cleanup.taskId}`)
    stopCleanupPolling()
    cleanup.status = 'FAILED'
    cleanup.statusLabel = '已取消'
    ElMessage.success('清理任务已取消')
  } catch (e) {
    ElMessage.error('取消失败')
  }
}

const updateCurrentTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

onMounted(() => {
  updateCurrentTime()
  timer.value = window.setInterval(updateCurrentTime, 1000)
})

onUnmounted(() => {
  if (timer.value) {
    clearInterval(timer.value)
  }
  stopCleanupPolling()
})
</script>

<style scoped>
.system {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
  margin: 0;
}

.system-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.system-card {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.system-info {
  margin-top: 10px;
}

.system-config {
  margin-top: 10px;
}

.database-info {
  margin-top: 10px;
}

.operation-logs {
  margin-top: 10px;
}

.feedback-nav {
  margin-top: 10px;
}

/* 数据清理样式 */
.cleanup-section {
  margin-top: 10px;
}

.cleanup-description {
  color: #606266;
  font-size: 14px;
  line-height: 2;
  margin-bottom: 20px;
  padding: 12px 16px;
  background: #fdf6ec;
  border-left: 4px solid #E6A23C;
  border-radius: 4px;
}

.cleanup-dialog-content {
  padding: 4px 0;
}

.cleanup-status-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.cleanup-status-header .el-icon {
  font-size: 22px;
}

.status-label {
  font-size: 15px;
}

.cleanup-progress-area {
  margin-bottom: 16px;
}

.progress-info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.progress-msg {
  font-size: 13px;
  color: #606266;
  flex: 1;
  margin-right: 10px;
}

.progress-pct {
  font-size: 14px;
  font-weight: bold;
  color: #409eff;
  min-width: 40px;
  text-align: right;
}

.cleanup-result {
  margin-top: 10px;
}

.cleanup-details {
  margin-top: 12px;
  padding: 10px 12px;
  background: #f0f9eb;
  border-radius: 4px;
}

.detail-item {
  margin: 4px 0;
  font-size: 13px;
  color: #67C23A;
}

@media (max-width: 768px) {
  .system-content {
    flex-direction: column;
  }
  
  .system-card {
    width: 100%;
  }
}
</style>