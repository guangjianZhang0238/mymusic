<template>
  <div class="system">
    <div class="page-header">
      <h2 class="page-title">系统管理 · 控制中心</h2>
      <div class="page-sub">
        <span class="subtitle">当前时间：{{ currentTime }}</span>
      </div>
    </div>

    <!-- 顶部总览 -->
    <div class="overview-row">
      <el-card class="overview-card">
        <div class="overview-title">系统概览</div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="系统名称">
            <span>{{ systemInfo.name }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="版本">
            <span>{{ systemInfo.version }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="运行环境">
            <el-tag size="small" type="info">{{ systemInfo.env }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="后端地址">
            <span>{{ systemInfo.backendBaseUrl }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="前端地址">
            <span>{{ systemInfo.frontendBaseUrl }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card class="overview-card stats-card">
        <div class="overview-title">核心指标</div>
        <div class="stats-grid">
          <div class="stat-item">
            <div class="stat-label">在线用户</div>
            <div class="stat-value text-green">{{ overview.onlineUserCount ?? 0 }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">总用户数</div>
            <div class="stat-value">{{ overview.totalUserCount ?? 0 }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">歌曲总数</div>
            <div class="stat-value">{{ overview.songCount ?? 0 }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">专辑总数</div>
            <div class="stat-value">{{ overview.albumCount ?? 0 }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">歌手总数</div>
            <div class="stat-value">{{ overview.artistCount ?? 0 }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">总播放次数</div>
            <div class="stat-value">{{ overview.totalPlayCount ?? 0 }}</div>
          </div>
        </div>
      </el-card>

      <el-card class="overview-card ffmpeg-card">
        <div class="overview-title">转码 / FFmpeg 状态</div>
        <div class="ffmpeg-status">
          <div class="status-row">
            <span>FFmpeg 可用</span>
            <el-tag :type="transcoding.ffmpegAvailable ? 'success' : 'danger'" size="small">
              {{ transcoding.ffmpegAvailable ? '正常' : '不可用' }}
            </el-tag>
          </div>
          <div class="status-row">
            <span>自动转码</span>
            <el-tag :type="transcoding.transcodingEnabled ? 'success' : 'info'" size="small">
              {{ transcoding.transcodingEnabled ? '开启' : '关闭' }}
            </el-tag>
          </div>
          <div class="status-row path-row">
            <span class="path-label">FFmpeg 路径</span>
            <span class="path-value">{{ transcoding.ffmpegPath || '-' }}</span>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 中间区域：活跃用户 + 维护工具 -->
    <div class="middle-row">
      <div class="left-column">
        <el-card class="system-card">
          <template #header>
            <div class="card-header">
              <span>最活跃用户 Top 10</span>
            </div>
          </template>
          <el-table :data="topUsers" size="small" height="260" v-loading="loadingTopUsers">
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="userId" label="用户ID" width="90" />
            <el-table-column prop="loginCount" label="登录次数" width="100" />
            <el-table-column prop="totalOnlineTime" label="在线时长(秒)" width="120" />
            <el-table-column prop="totalPlayCount" label="播放次数" />
          </el-table>
        </el-card>
      </div>

      <div class="right-column">
        <el-card class="system-card">
          <template #header>
            <div class="card-header">
              <span>用户反馈</span>
            </div>
          </template>
          <div class="feedback-nav">
            <div class="feedback-stats">
              <div class="feedback-main">
                <div class="feedback-number">
                  <span class="label">待处理</span>
                  <span class="value">{{ feedback.pendingCount }}</span>
                </div>
                <div class="feedback-desc">条用户反馈等待处理</div>
              </div>
              <div class="feedback-extra">
                <div>本周新增：{{ feedback.weekNewCount }} 条</div>
              </div>
            </div>
            <el-button type="primary" @click="$router.push('/feedback')">
              查看并处理反馈
            </el-button>
          </div>
        </el-card>

        <el-card class="system-card">
          <template #header>
            <div class="card-header">
              <span>缓存与临时文件</span>
            </div>
          </template>
          <div class="cache-section">
            <div class="cache-row">
              <span>Redis 缓存</span>
              <el-tag :type="cache.redisOk ? 'success' : 'danger'" size="small">
                {{ cache.redisOk ? '连接正常' : '不可用' }}
              </el-tag>
            </div>
            <div class="cache-actions">
              <el-button size="small" type="warning" @click="handleClearCache" :loading="cache.clearing">
                清空应用缓存
              </el-button>
              <el-button size="small" @click="handleCleanupTemp" :loading="temp.clearing">
                清理临时文件 (已删 {{ temp.deletedFiles }} 个)
              </el-button>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 底部：数据清理任务（保留） -->
    <el-card class="system-card cleanup-card">
      <template #header>
        <div class="card-header">
          <span>数据清理任务</span>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Loading, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import request from '@/api/request'

const currentTime = ref('')
const timer = ref<number | null>(null)

const systemInfo = reactive({
  name: '',
  version: '',
  env: '',
  backendBaseUrl: '',
  frontendBaseUrl: ''
})

const overview = reactive<any>({
  onlineUserCount: 0,
  totalUserCount: 0,
  songCount: 0,
  albumCount: 0,
  artistCount: 0,
  totalPlayCount: 0
})

const transcoding = reactive({
  ffmpegAvailable: false,
  transcodingEnabled: false,
  ffmpegPath: ''
})

const topUsers = ref<any[]>([])
const loadingTopUsers = ref(false)

const feedback = reactive({
  pendingCount: 0,
  weekNewCount: 0
})

const cache = reactive({
  redisOk: false,
  clearing: false
})

const temp = reactive({
  clearing: false,
  deletedFiles: 0
})

// 数据清理状态（沿用原有逻辑）
const cleanup = reactive({
  dialogVisible: false,
  taskId: null as number | null,
  status: '', // RUNNING / COMPLETED / FAILED
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

// 加载系统基础信息
const loadSystemInfo = async () => {
  try {
    const data = await request.get('/system/info')
    Object.assign(systemInfo, data || {})
  } catch {
    // ignore
  }
}

// 加载系统总览
const loadOverview = async () => {
  try {
    const data = await request.get('/system/overview')
    Object.assign(overview, data || {})
  } catch {
    // ignore
  }
}

// 加载转码状态
const loadTranscodingStatus = async () => {
  try {
    const data = await request.get('/system/transcoding-status')
    transcoding.ffmpegAvailable = !!data.ffmpegAvailable
    transcoding.transcodingEnabled = !!data.transcodingEnabled
    transcoding.ffmpegPath = data.ffmpegPath || ''
  } catch {
    // ignore
  }
}

// 加载活跃用户
const loadTopUsers = async () => {
  loadingTopUsers.value = true
  try {
    const data = await request.get('/system/top-active-users', { params: { limit: 10 } })
    topUsers.value = Array.isArray(data) ? data : []
  } catch {
    topUsers.value = []
  } finally {
    loadingTopUsers.value = false
  }
}

// 反馈汇总（只算待处理总数）
const loadFeedbackSummary = async () => {
  try {
    const res: any = await request.get('/app/music/feedback/admin/list', {
      params: { page: 1, size: 1, status: 'PENDING' }
    })
    if (typeof res.total === 'number') {
      feedback.pendingCount = res.total
    } else if (Array.isArray(res.records)) {
      feedback.pendingCount = res.records.length
    }
  } catch {
    feedback.pendingCount = 0
  }
}

// 缓存状态
const loadCacheStatus = async () => {
  try {
    const data = await request.get('/system/cache-status')
    cache.redisOk = !!data.redisOk
  } catch {
    cache.redisOk = false
  }
}

const handleClearCache = async () => {
  try {
    cache.clearing = true
    await ElMessageBox.confirm('确定要清空 Redis 缓存吗？此操作不可恢复。', '警告', {
      confirmButtonText: '确认清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await request.get('/system/clear-cache')
    ElMessage.success('缓存已清空')
    loadCacheStatus()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('清空缓存失败')
    }
  } finally {
    cache.clearing = false
  }
}

const handleCleanupTemp = async () => {
  try {
    temp.clearing = true
    const data = await request.get('/system/cleanup-temp')
    temp.deletedFiles = data?.deletedFiles ?? 0
    ElMessage.success(data?.message || `已清理临时文件 ${temp.deletedFiles} 个`)
  } catch {
    ElMessage.error('清理临时文件失败')
  } finally {
    temp.clearing = false
  }
}

onMounted(() => {
  updateCurrentTime()
  timer.value = window.setInterval(updateCurrentTime, 1000)
  loadSystemInfo()
  loadOverview()
  loadTranscodingStatus()
  loadTopUsers()
  loadFeedbackSummary()
  loadCacheStatus()
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
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.page-title {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
  margin: 0;
}

.page-sub .subtitle {
  font-size: 13px;
  color: #909399;
}

.overview-row {
  display: grid;
  grid-template-columns: 2fr 2fr 1.5fr;
  gap: 16px;
  margin-bottom: 16px;
}

.overview-card {
  width: 100%;
}

.overview-title {
  font-weight: 500;
  margin-bottom: 10px;
}

.stats-card .stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.stat-item {
  padding: 8px 10px;
  border-radius: 6px;
  background: #f5f7fa;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.stat-value {
  margin-top: 4px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.text-green {
  color: #67c23a;
}

.ffmpeg-status {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.status-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}

.path-row {
  flex-direction: column;
  align-items: flex-start;
}

.path-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.path-value {
  font-size: 12px;
  color: #606266;
  word-break: break-all;
}

.middle-row {
  display: grid;
  grid-template-columns: 2fr 1.8fr;
  gap: 16px;
  margin-bottom: 16px;
}

.left-column,
.right-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.system-card {
  width: 100%;
}

.cleanup-card {
  margin-top: 4px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.feedback-nav {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.feedback-stats {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.feedback-main {
  display: flex;
  flex-direction: column;
}

.feedback-number {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.feedback-number .label {
  font-size: 13px;
  color: #909399;
}

.feedback-number .value {
  font-size: 24px;
  font-weight: 600;
  color: #f56c6c;
}

.feedback-desc {
  font-size: 12px;
  color: #909399;
}

.feedback-extra {
  font-size: 13px;
  color: #606266;
}

.cache-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.cache-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.cache-actions {
  display: flex;
  gap: 8px;
}

/* 数据清理样式（保留原有视觉） */
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

@media (max-width: 1024px) {
  .overview-row {
    grid-template-columns: 1fr;
  }
  .middle-row {
    grid-template-columns: 1fr;
  }
}
</style>