<template>
  <div class="file-upload">
    <h2 class="page-title">文件上传</h2>
    
    <!-- 歌手和专辑选择区域 -->
    <el-card class="metadata-card">
      <template #header>
        <div class="card-header">
          <span>音乐信息</span>
        </div>
      </template>
      
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="歌手">
            <el-autocomplete
              v-model="selectedArtist.name"
              :fetch-suggestions="searchArtists"
              placeholder="请输入或选择歌手"
              @select="handleArtistSelect"
              clearable
              style="width: 100%"
            >
              <template #default="{ item }">
                <div class="artist-suggestion">
                  <span class="artist-name">{{ item.name }}</span>
                  <span class="artist-stats">{{ item.albumCount }}专辑 {{ item.songCount }}歌曲</span>
                </div>
              </template>
            </el-autocomplete>
          </el-form-item>
        </el-col>
        
        <el-col :span="12">
          <el-form-item label="专辑">
            <el-autocomplete
              v-model="selectedAlbum.name"
              :fetch-suggestions="searchAlbums"
              placeholder="请输入或选择专辑（留空则使用默认专辑）"
              @select="handleAlbumSelect"
              clearable
              style="width: 100%"
            >
              <template #default="{ item }">
                <div class="album-suggestion">
                  <span class="album-name">{{ item.name }}</span>
                </div>
              </template>
            </el-autocomplete>
          </el-form-item>
        </el-col>
      </el-row>
      
      <div v-if="selectedArtist.id" class="selection-info">
        <el-tag type="success" size="small">歌手：{{ selectedArtist.name }}</el-tag>
        <el-tag v-if="selectedAlbum.id" type="primary" size="small" style="margin-left: 10px">
          专辑：{{ selectedAlbum.name }}
        </el-tag>
        <el-tag v-else type="info" size="small" style="margin-left: 10px">
          专辑：默认
        </el-tag>
      </div>
    </el-card>
    
    <!-- 文件上传区域 -->
    <el-card class="upload-card">
      <template #header>
        <div class="card-header">
          <span>文件上传</span>
          <el-button 
            type="primary" 
            @click="handleUpload" 
            :disabled="!selectedArtist.name?.trim() || uploading"
            :loading="uploading"
          >
            {{ uploading ? '上传中...' : '开始上传' }}
          </el-button>
        </div>
      </template>
      
      <div 
        class="upload-dropzone" 
        :class="{ 'drag-over': dragOver }"
        @drop.prevent="handleDrop"
        @dragover.prevent="dragOver = true"
        @dragleave.prevent="dragOver = false"
        @click="fileInput?.click()"
      >
        <el-icon class="upload-icon"><UploadFilled /></el-icon>
        <div class="upload-text">
          <p>拖放文件到此处，或 <em>点击选择文件</em></p>
          <p class="upload-hint">支持音频文件（WAV、FLAC、MP3等）和歌词文件（LRC）</p>
          <p class="transcoding-hint">注意：WAV和DSF文件上传后会自动转码为FLAC或AAC格式</p>
          <p class="transcoding-info">注意：WAV和DSF文件上传后将自动转码为FLAC或AAC格式</p>
        </div>
        
        <input 
          ref="fileInput" 
          type="file" 
          multiple 
          accept="audio/*,.lrc" 
          @change="handleFileSelect" 
          style="display: none"
        />
      </div>
      
      <!-- 文件列表 -->
      <div v-if="pendingFiles.length > 0" class="file-list">
        <h4>待上传文件 ({{ pendingFiles.length }}个，合计 {{ formatFileSize(pendingTotalSize) }})</h4>
        <el-table :data="pendingFiles" style="width: 100%" max-height="300">
          <el-table-column label="文件名">
            <template #default="scope">
              <div class="file-name-cell">
                <div
                  class="file-progress-bg"
                  :style="{ width: `${scope.row.progress || 0}%` }"
                ></div>
                <span class="file-name-text">{{ scope.row.name }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="合唱歌手" width="300">
            <template #default="scope">
              <div class="chorus-selector">
                <el-autocomplete
                  v-model="scope.row.chorusKeyword"
                  :fetch-suggestions="searchChorusArtists"
                  placeholder="输入歌手名搜索并从下拉中选择（可不填）"
                  @select="(item) => handleChorusSelect(scope.row, item)"
                  clearable
                  style="width: 100%"
                >
                  <template #default="{ item }">
                    <div class="artist-suggestion">
                      <span class="artist-name">{{ item.name }}</span>
                      <span class="artist-stats">{{ item.albumCount }}专辑 {{ item.songCount }}歌曲</span>
                    </div>
                  </template>
                </el-autocomplete>
                <div
                  v-if="scope.row.chorusArtists && scope.row.chorusArtists.length"
                  class="chorus-tags"
                >
                  <span class="chorus-tags-label">已选合唱歌手：</span>
                  <el-tag
                    v-for="artist in scope.row.chorusArtists"
                    :key="artist.id ?? artist.name"
                    size="small"
                    closable
                    @close="removeChorusArtist(scope.row, artist)"
                    class="chorus-tag-item"
                  >
                    {{ artist.name }}
                  </el-tag>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="size" label="大小" width="120">
            <template #default="scope">
              {{ formatFileSize(scope.row.size) }}
            </template>
          </el-table-column>
          <el-table-column label="进度" width="120">
            <template #default="scope">
              {{ scope.row.progress || 0 }}%
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="scope">
              <el-button
                type="danger"
                size="small"
                :disabled="scope.row.status === 'uploading'"
                @click="removeFile(scope.$index)"
              >
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>
    
    <!-- 歌曲库更新区域 -->
    <el-card class="scan-library-section">
      <template #header>
        <div class="card-header">
          <span>更新歌曲库</span>
        </div>
      </template>
      
      <p class="scan-library-description">
        点击下方按钮，系统将自动扫描base-path文件夹，对于MySQL中没有的歌曲，在MySQL中添加相关数据。
      </p>
      
      <!-- 进度显示区域 -->
      <div v-if="asyncUpdate.isRunning" class="progress-container">
        <div class="progress-info">
          <span class="status-text">{{ asyncUpdate.message }}</span>
          <span class="progress-percent">{{ asyncUpdate.progress }}%</span>
        </div>
        <el-progress 
          :percentage="asyncUpdate.progress" 
          :status="asyncUpdate.status === 'FAILED' ? 'exception' : 'success'"
          striped
          striped-flow
        />
        <div class="progress-actions">
          <el-button 
            type="danger" 
            size="small" 
            @click="cancelUpdate"
          >
            取消任务
          </el-button>
        </div>
      </div>
      
      <!-- 启动按钮 -->
      <el-button 
        v-else
        type="primary" 
        @click="handleScanLibrary"
        class="scan-library-button"
      >
        <el-icon><Refresh /></el-icon>
        更新歌曲库
      </el-button>
    </el-card>
    
    <!-- 上传历史 -->
    <div v-if="uploadList.length > 0" class="upload-list">
      <h3>上传历史</h3>
      <el-table :data="uploadList" style="width: 100%" border>
        <el-table-column prop="filename" label="文件名" />
        <el-table-column prop="size" label="文件大小" />
        <el-table-column prop="path" label="存储路径" />
        <el-table-column prop="status" label="状态">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'success' ? 'success' : 'danger'">
              {{ scope.row.status === 'success' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled, Refresh } from '@element-plus/icons-vue'
import request from '@/api/request'

type Artist = {
  id?: number
  name: string
  albumCount?: number
  songCount?: number
}

type Album = {
  id?: number
  name: string
  artistId?: number
}

type ChorusArtist = { id?: number; name: string }

type PendingFile = {
  file: File
  name: string
  size: number
  progress: number
  status: 'pending' | 'uploading' | 'success' | 'error'
  chorusArtists: ChorusArtist[]
  chorusKeyword?: string
}

const MAX_FILES_PER_BATCH = 100
const MAX_TOTAL_SIZE = 2 * 1024 * 1024 * 1024 // 2GB（本次选择的文件总大小）
const MAX_SINGLE_FILE_SIZE = 2 * 1024 * 1024 * 1024 // 2GB（单文件）

const userStore = useUserStore()
const token = computed(() => userStore.token)

// 歌手和专辑选择状态
const selectedArtist = ref<Artist>({ name: '' })
const selectedAlbum = ref<Album>({ name: '' })

// 文件上传状态
const pendingFiles = ref<PendingFile[]>([])
const uploading = ref(false)
const dragOver = ref(false)
const fileInput = ref<HTMLInputElement>()

const pendingTotalSize = computed(() => pendingFiles.value.reduce((sum, f) => sum + (f.size || 0), 0))

// 上传历史
const uploadList = ref<Array<{
  filename: string
  size: string
  path: string
  status: string
}>>([])

// 异步更新相关状态
const asyncUpdate = ref({
  taskId: null as number | null,
  progress: 0,
  status: '',
  message: '',
  isRunning: false
})

let progressTimer: number | null = null

const startLibraryUpdate = async (skipConfirm: boolean) => {
  try {
    if (!skipConfirm) {
      // 显示确认对话框
      await ElMessageBox.confirm(
        '更新歌曲库可能需要一些时间，确定要继续吗？',
        '确认操作',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
    }
    
    // 启动异步更新
    const response = await request.post('/library/update-async')
    console.log('启动更新响应:', response)
    
    const taskId = response
    console.log('获取到的任务ID:', taskId)
    
    if (taskId === undefined || taskId === null) {
      ElMessage.error('未能获取任务ID: 响应为空')
      return
    }
    
    if (typeof taskId !== 'number') {
      ElMessage.error(`任务ID格式错误: 期望数字，实际得到 ${typeof taskId}`)
      return
    }
    
    // 设置任务状态
    asyncUpdate.value.taskId = taskId
    asyncUpdate.value.isRunning = true
    asyncUpdate.value.progress = 0
    asyncUpdate.value.status = 'RUNNING'
    asyncUpdate.value.message = '任务已启动...'
    
    // 开始轮询进度
    startProgressPolling()
    
    ElMessage.success('歌曲库更新任务已启动')
    
  } catch (error: any) {
    if (error === 'cancel') return // 用户取消确认
    console.error('启动歌曲库更新失败:', error)
    ElMessage.error('启动更新失败，请稍后重试')
  }
}

const handleScanLibrary = async () => {
  return startLibraryUpdate(false)
}

const startProgressPolling = () => {
  // 清除之前的定时器
  if (progressTimer) {
    clearInterval(progressTimer)
  }
  
  // 确保有有效的taskId
  if (!asyncUpdate.value.taskId) {
    console.error('任务ID不存在')
    return
  }
  
  console.log('开始轮询进度，任务ID:', asyncUpdate.value.taskId)
  
  // 每2秒查询一次进度
  progressTimer = window.setInterval(async () => {
    try {
      console.log('请求进度，任务ID:', asyncUpdate.value.taskId)
      const response = await request.get(`/library/progress/${asyncUpdate.value.taskId}`)
      console.log('原始响应:', response)
      
      // 处理可能的嵌套结构
      let progressData = response
      if (response && typeof response === 'object' && response.data) {
        progressData = response.data
      }
      
      console.log('处理后的进度数据:', progressData)
      
      // 更新UI状态
      asyncUpdate.value.progress = progressData.progress || 0
      asyncUpdate.value.status = progressData.status || ''
      asyncUpdate.value.message = progressData.message || ''
      
      console.log('更新后状态:', {
        progress: asyncUpdate.value.progress,
        status: asyncUpdate.value.status,
        message: asyncUpdate.value.message
      })
      
      // 检查任务是否完成
      if (progressData.status === 'COMPLETED') {
        console.log('任务已完成，停止轮询')
        stopProgressPolling()
        asyncUpdate.value.isRunning = false
        ElMessage.success(progressData.message)
      } else if (progressData.status === 'FAILED') {
        console.log('任务失败，停止轮询')
        stopProgressPolling()
        asyncUpdate.value.isRunning = false
        ElMessage.error(`更新失败: ${progressData.errorMessage || progressData.message}`)
      } else {
        console.log('任务仍在运行中...')
      }
      
    } catch (error: any) {
      console.error('获取进度失败:', error)
      // 如果是404错误，可能任务已不存在
      if (error.response?.status === 404) {
        console.log('任务不存在，停止轮询')
        stopProgressPolling()
        asyncUpdate.value.isRunning = false
        ElMessage.error('任务不存在或已过期')
      }
    }
  }, 2000)
}

const stopProgressPolling = () => {
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
}

const cancelUpdate = async () => {
  if (!asyncUpdate.value.taskId) return
  
  try {
    await request.post(`/library/cancel/${asyncUpdate.value.taskId}`)
    stopProgressPolling()
    asyncUpdate.value.isRunning = false
    ElMessage.success('任务已取消')
  } catch (error) {
    console.error('取消任务失败:', error)
    ElMessage.error('取消任务失败')
  }
}

// 组件卸载时清理定时器
onUnmounted(() => {
  stopProgressPolling()
})

// 歌手搜索
const searchArtists = async (keyword: string, callback: (results: any[]) => void) => {
  if (!keyword.trim()) {
    callback([])
    return
  }
  
  try {
    const response = await request.get('/music-metadata/artists/search', {
      params: { keyword: keyword.trim(), limit: 10 }
    })
    callback(response || [])
  } catch (error) {
    console.error('搜索歌手失败:', error)
    callback([])
  }
}

// 专辑搜索
const searchAlbums = async (keyword: string, callback: (results: any[]) => void) => {
  if (!keyword.trim() || !selectedArtist.value.id) {
    callback([])
    return
  }
  
  try {
    const response = await request.get('/music-metadata/albums/search', {
      params: { 
        artistId: selectedArtist.value.id, 
        keyword: keyword.trim(), 
        limit: 10 
      }
    })
    callback(response || [])
  } catch (error) {
    console.error('搜索专辑失败:', error)
    callback([])
  }
}

// 歌手选择处理
const handleArtistSelect = (item: Artist) => {
  selectedArtist.value = { ...item }
  // 清空专辑选择
  selectedAlbum.value = { name: '' }
}

// 注意：不在 blur 时自动创建歌手，避免误创建无用数据。创建/匹配放到“开始上传”时触发。

// 专辑选择处理
const handleAlbumSelect = (item: Album) => {
  selectedAlbum.value = { ...item }
}

// 文件选择处理
const handleFileSelect = (event: Event) => {
  const input = event.target as HTMLInputElement
  if (input.files) {
    addFiles(Array.from(input.files))
  }
}

// 拖拽处理
const handleDrop = (event: DragEvent) => {
  dragOver.value = false
  if (event.dataTransfer?.files) {
    addFiles(Array.from(event.dataTransfer.files))
  }
}

// 添加文件
const addFiles = (files: File[]) => {
  const validFiles = files.filter(file => {
    const isValidType = file.type.startsWith('audio/') || file.name.toLowerCase().endsWith('.lrc')
    const isValidSize = file.size <= MAX_SINGLE_FILE_SIZE
    
    if (!isValidType) {
      ElMessage.warning(`文件 ${file.name} 类型不支持`) 
      return false
    }
    
    if (!isValidSize) {
      ElMessage.warning(`文件 ${file.name} 超过单文件大小限制（2GB）`) 
      return false
    }
    
    return true
  })
  
  for (const file of validFiles) {
    if (pendingFiles.value.length >= MAX_FILES_PER_BATCH) {
      ElMessage.warning(`一次最多选择 ${MAX_FILES_PER_BATCH} 个文件`)
      break
    }

    const newTotal = pendingTotalSize.value + file.size
    if (newTotal > MAX_TOTAL_SIZE) {
      ElMessage.warning('本次选择的文件总大小不能超过 2GB')
      break
    }

    pendingFiles.value.push({
      file,
      name: file.name,
      size: file.size,
      progress: 0,
      status: 'pending',
      chorusArtists: [],
      chorusKeyword: ''
    })
  }
}

const searchChorusArtists = async (keyword: string, callback: (results: Artist[]) => void) => {
  if (!keyword?.trim()) {
    callback([])
    return
  }
  try {
    const res = await request.get('/music-metadata/artists/search', {
      params: { keyword: keyword.trim(), limit: 20 }
    })
    callback(res || [])
  } catch (e) {
    console.error('搜索合唱歌手失败:', e)
    callback([])
  }
}

const handleChorusSelect = (row: PendingFile, item: Artist) => {
  if (!row.chorusArtists) {
    row.chorusArtists = []
  }
  const exists = row.chorusArtists.some(a => a.id === item.id || a.name === item.name)
  if (!exists) {
    row.chorusArtists.push({ id: item.id, name: item.name })
  }
  row.chorusKeyword = ''
}

const removeChorusArtist = (row: PendingFile, artist: ChorusArtist) => {
  row.chorusArtists = row.chorusArtists.filter(
    a => !(a.id === artist.id && a.name === artist.name)
  )
}

// 移除文件
const removeFile = (index: number) => {
  pendingFiles.value.splice(index, 1)
}

// 开始上传
const handleUpload = async () => {
  const artistName = selectedArtist.value.name?.trim()
  if (!artistName) {
    ElMessage.warning('请先输入歌手名称')
    return
  }
  
  if (pendingFiles.value.length === 0) {
    ElMessage.warning('请先选择要上传的文件')
    return
  }

  if (pendingFiles.value.length > MAX_FILES_PER_BATCH) {
    ElMessage.warning(`一次最多上传 ${MAX_FILES_PER_BATCH} 个文件`)
    return
  }

  if (pendingTotalSize.value > MAX_TOTAL_SIZE) {
    ElMessage.warning('本次上传的文件总大小不能超过 2GB')
    return
  }
  
  uploading.value = true
  
  try {
    // 开始上传时才自动匹配/创建歌手，避免 blur 误创建
    if (!selectedArtist.value.id) {
      const artistResponse = await request.post('/music-metadata/artists/auto-match', null, {
        params: { artistName }
      })
      selectedArtist.value = artistResponse
    }

    // 确保专辑存在
    let albumId = selectedAlbum.value.id
    if (!albumId) {
      const albumResponse = await request.post('/music-metadata/albums/auto-match', null, {
        params: { 
          artistId: selectedArtist.value.id, 
          albumName: selectedAlbum.value.name || '默认' 
        }
      })
      albumId = albumResponse.id
    }
    
    // 逐个上传文件
    for (let i = 0; i < pendingFiles.value.length; i++) {
      const pendingFile = pendingFiles.value[i]
      await uploadSingleFile(pendingFile, albumId!, i + 1)
    }
    
    ElMessage.success('所有文件上传完成')
    pendingFiles.value = []

    // 自动触发更新歌曲库（异步任务）
    await startLibraryUpdate(true)
    
  } catch (error) {
    console.error('上传失败:', error)
    ElMessage.error('上传过程中发生错误')
  } finally {
    uploading.value = false
  }
}

// 上传单个文件（带重试机制）
// 对于大文件（>10MB），优先直连可配置后端；未配置时走同源 /api 代理，兼容远程设备访问
const LARGE_FILE_THRESHOLD = 10 * 1024 * 1024 // 10MB
const BACKEND_BASE_URL = ((import.meta as any).env?.VITE_UPLOAD_BASE_URL || '').replace(/\/$/, '') // 例如: http://120.79.224.90:18080

const uploadSingleFile = async (pendingFile: PendingFile, albumId: number, index: number, maxRetries = 5) => {
  pendingFile.status = 'uploading'
  pendingFile.progress = 0
  pendingFile.status = 'uploading'
  pendingFile.progress = 0

  const formData = new FormData()
  formData.append('file', pendingFile.file)
  formData.append('albumId', albumId.toString())
  const chorusArtists = pendingFile.chorusArtists || []
  for (const a of chorusArtists) {
    if (a.id != null) formData.append('chorusArtistIds', String(a.id))
    if (a.name) formData.append('chorusArtistNames', a.name)
  }
  
  // 文件预检查
  if (pendingFile.file.size === 0) {
    console.warn('文件大小为0:', pendingFile.name)
    throw new Error('文件大小为0，无法上传')
  }
  
  if (!pendingFile.file.type && !pendingFile.name.toLowerCase().endsWith('.wav')) {
    console.warn('文件类型未知:', pendingFile.name)
  }
  
  // 网络状态检查
  if (!navigator.onLine) {
    console.error('网络离线状态')
    throw new Error('网络连接不可用')
  }
  
  // 判断是否为大文件。远程设备场景下，默认走同源 /api，避免硬编码 localhost 导致连接被拒绝
  const isLargeFile = pendingFile.file.size > LARGE_FILE_THRESHOLD
  const uploadUrl = isLargeFile
    ? (BACKEND_BASE_URL ? `${BACKEND_BASE_URL}/api/upload/single` : '/api/upload/single')
    : '/api/upload/single'
  
  console.log('网络状态:', {
    onLine: navigator.onLine,
    connection: (navigator as any).connection ? {
      effectiveType: (navigator as any).connection.effectiveType,
      downlink: (navigator as any).connection.downlink,
      rtt: (navigator as any).connection.rtt
    } : 'unsupported'
  })
  
  console.log('开始上传文件:', {
    filename: pendingFile.name,
    size: pendingFile.size,
    type: pendingFile.file.type,
    lastModified: pendingFile.file.lastModified,
    fileSizeMB: (pendingFile.size / 1024 / 1024).toFixed(2) + 'MB',
    isLargeFile: isLargeFile,
    uploadUrl: uploadUrl
  })
  
  // 重试逻辑
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      if (attempt > 1) {
        console.log(`第${attempt}次重试上传: ${pendingFile.name}`)
        // 增加重试间隔时间
        await new Promise(resolve => setTimeout(resolve, 2000 * attempt))
      }
      
      let result: any
      
      if (isLargeFile) {
        // 大文件使用 XMLHttpRequest 直接请求后端，绕过 Vite 代理
        // XMLHttpRequest 对大文件支持更好，不会像 fetch 那样缓冲整个请求体
        console.log('大文件模式: 使用 XMLHttpRequest 直接请求后端', uploadUrl)
        
        result = await new Promise((resolve, reject) => {
          const xhr = new XMLHttpRequest()
          
          // 设置超时 10 分钟
          xhr.timeout = 600000
          
          // 上传进度监控
          xhr.upload.onprogress = (event) => {
            if (event.lengthComputable) {
              const percent = Math.round((event.loaded * 100) / event.total)
              pendingFile.progress = Math.max(0, Math.min(100, percent))
              console.log(`上传进度 [${pendingFile.name}]: ${percent}% (${formatFileSize(event.loaded)}/${formatFileSize(event.total)})`)
            }
          }
          
          xhr.onload = () => {
            console.log('XHR 响应状态:', xhr.status, xhr.statusText)
            if (xhr.status >= 200 && xhr.status < 300) {
              try {
                const jsonResult = JSON.parse(xhr.responseText)
                if (jsonResult.code === 200) {
                  resolve(jsonResult.data)
                } else {
                  reject(new Error(jsonResult.message || '上传失败'))
                }
              } catch (e) {
                reject(new Error('解析响应失败: ' + xhr.responseText.substring(0, 200)))
              }
            } else {
              reject(new Error(`HTTP ${xhr.status}: ${xhr.statusText}`))
            }
          }
          
          xhr.onerror = (event) => {
            console.error('XHR 网络错误:', event)
            reject(new Error('Network Error - 请检查后端服务是否运行'))
          }
          
          xhr.ontimeout = () => {
            console.error('XHR 超时')
            reject(new Error('上传超时'))
          }
          
          xhr.onabort = () => {
            console.error('XHR 被中止')
            reject(new Error('上传被中止'))
          }
          
          xhr.open('POST', uploadUrl, true)
          xhr.setRequestHeader('Authorization', `Bearer ${token.value}`)
          // 不要设置 Content-Type，让浏览器自动设置 multipart/form-data 和 boundary
          xhr.send(formData)
        })
      } else {
        // 小文件使用 axios 走代理
        result = await request.post('/upload/single', formData, {
          timeout: 600000,
          maxContentLength: Infinity,
          maxBodyLength: Infinity,
          onUploadProgress: (progressEvent: { loaded: number; total?: number }) => {
            if (progressEvent.total) {
              const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
              pendingFile.progress = Math.max(0, Math.min(100, percent))
              console.log(`上传进度 [${pendingFile.name}]: ${percent}% (${formatFileSize(progressEvent.loaded)}/${formatFileSize(progressEvent.total)})`)
            }
          }
        })
      }
      
      pendingFile.progress = 100
      pendingFile.status = 'success'

      uploadList.value.push({
        filename: pendingFile.name,
        size: formatFileSize(pendingFile.size),
        path: result?.path || '',
        status: 'success'
      })
      
      ElMessage.success(`[${index}/${pendingFiles.value.length}] ${pendingFile.name} 上传成功${attempt > 1 ? `(第${attempt}次尝试)` : ''}`)
      return // 成功则返回
      
    } catch (error: any) {
      console.error(`第${attempt}次上传失败:`, error)
      console.error('失败文件详情:', {
        filename: pendingFile.name,
        size: pendingFile.size,
        type: pendingFile.file.type,
        attempt: attempt,
        errorMessage: error.message,
        errorCode: error.code,
        response: error.response?.data,
        status: error.response?.status
      })
      
      // 如果是最后一次尝试，则记录失败
      if (attempt === maxRetries) {
        pendingFile.status = 'error'
        uploadList.value.push({
          filename: pendingFile.name,
          size: formatFileSize(pendingFile.size),
          path: '',
          status: 'error'
        })
        
        // 提供更详细的错误信息
        let errorMessage = `${pendingFile.name} 上传失败`
        if (error.message) {
          errorMessage += `: ${error.message}`
        }
        errorMessage += ` (已重试${maxRetries}次)`
        
        ElMessage.error(`[${index}/${pendingFiles.value.length}] ${errorMessage}`)
        throw error
      }
    }
  }
}

const formatFileSize = (size: number) => {
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
  if (size < 1024 * 1024 * 1024) return (size / (1024 * 1024)).toFixed(2) + ' MB'
  return (size / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}
</script>

<style scoped>
.file-upload {
  padding: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 20px;
}

.metadata-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

.artist-suggestion, .album-suggestion {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.artist-name, .album-name {
  font-weight: 500;
}

.artist-stats {
  font-size: 12px;
  color: #909399;
}

.selection-info {
  margin-top: 15px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.upload-card {
  margin-bottom: 20px;
}

.upload-dropzone {
  border: 2px dashed #d9d9d9;
  border-radius: 6px;
  padding: 40px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  background-color: #fafafa;
}

.upload-dropzone:hover,
.upload-dropzone.drag-over {
  border-color: #409eff;
  background-color: #f0f8ff;
}

.upload-icon {
  font-size: 48px;
  color: #c0c4cc;
  margin-bottom: 16px;
}

.upload-text {
  color: #606266;
}

.upload-text p {
  margin: 8px 0;
}

.upload-hint {
  font-size: 14px;
  color: #909399;
}

.transcoding-hint {
  font-size: 13px;
  color: #E6A23C;
  margin-top: 8px;
  font-style: italic;
}

.transcoding-info {
  font-size: 13px;
  color: #E6A23C;
  margin-top: 8px;
  font-style: italic;
}

.file-list {
  margin-top: 20px;
}

.file-name-cell {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 28px;
  padding: 0 8px;
  overflow: hidden;
  border-radius: 4px;
}

.file-progress-bg {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 0;
  background: rgba(103, 194, 58, 0.35);
  transition: width 0.25s ease;
  pointer-events: none;
}

.file-name-text {
  position: relative;
  z-index: 1;
}

.file-list h4 {
  margin-bottom: 15px;
  color: #303133;
}

.upload-list {
  margin-top: 20px;
}

.upload-list h3 {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 10px;
}

/* 歌曲库更新样式 */
.scan-library-section {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.scan-library-section h3 {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 10px;
}

.scan-library-description {
  color: #606266;
  margin-bottom: 20px;
  line-height: 1.5;
}

.scan-library-button {
  margin-top: 10px;
}

/* 进度显示样式 */
.progress-container {
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

.progress-actions {
  margin-top: 15px;
  text-align: right;
}

:deep(.el-progress-bar__inner) {
  transition: width 0.3s ease;
}
</style>