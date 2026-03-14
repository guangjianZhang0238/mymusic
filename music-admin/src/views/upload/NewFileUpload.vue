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
          <el-form-item label="主要歌手">
            <el-autocomplete
              v-model="selectedArtist.name"
              :fetch-suggestions="searchArtists"
              placeholder="请输入或选择主要歌手"
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
          <el-form-item label="其他歌手">
            <div class="chorus-selector">
              <el-autocomplete
                v-model="additionalArtistKeyword"
                :fetch-suggestions="searchChorusArtists"
                placeholder="输入歌手名搜索并从下拉中选择（可不填）"
                @select="handleAdditionalArtistSelect"
                @keyup.enter="handleAdditionalArtistEnter"
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
                v-if="additionalArtists.length"
                class="chorus-tags"
              >
                <span class="chorus-tags-label">已选其他歌手：</span>
                <el-tag
                  v-for="artist in additionalArtists"
                  :key="artist.id ?? artist.name"
                  size="small"
                  closable
                  @close="removeAdditionalArtist(artist)"
                  class="chorus-tag-item"
                >
                  {{ artist.name }}
                </el-tag>
              </div>
            </div>
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
          <el-table-column prop="name" label="文件名" />
          <el-table-column prop="size" label="大小" width="120">
            <template #default="scope">
              {{ formatFileSize(scope.row.size) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="scope">
              <el-button 
                type="danger" 
                size="small" 
                @click="removeFile(scope.$index)"
              >
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
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
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
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
// 其他歌手（顶部范围）的 chips
const additionalArtists = ref<ChorusArtist[]>([])
const additionalArtistKeyword = ref('')

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

const handleAdditionalArtistSelect = (item: Artist) => {
  const exists = additionalArtists.value.some(a => a.id === item.id || a.name === item.name)
  if (!exists) {
    additionalArtists.value.push({ id: item.id, name: item.name })
  }
  additionalArtistKeyword.value = ''
}

const handleAdditionalArtistEnter = () => {
  const keyword = additionalArtistKeyword.value.trim()
  if (!keyword) return

  const exists = additionalArtists.value.some(a => a.name === keyword)
  if (!exists) {
    additionalArtists.value.push({ name: keyword })
  }

  additionalArtistKeyword.value = ''
}

const removeAdditionalArtist = (artist: ChorusArtist) => {
  additionalArtists.value = additionalArtists.value.filter(
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

    // 自动触发更新歌曲库（异步任务），不在本页面轮询进度
    try {
      await request.post('/library/update-async')
      ElMessage.success('已自动触发更新歌曲库任务')
    } catch (e) {
      console.error('自动触发更新歌曲库失败:', e)
      ElMessage.warning('上传完成，但自动更新歌曲库任务触发失败，可稍后手动点击“更新歌曲库”')
    }
    
  } catch (error) {
    console.error('上传失败:', error)
    ElMessage.error('上传过程中发生错误')
  } finally {
    uploading.value = false
  }
}

// 上传单个文件
const uploadSingleFile = async (pendingFile: PendingFile, albumId: number, index: number) => {
  const formData = new FormData()
  formData.append('file', pendingFile.file)
  formData.append('albumId', albumId.toString())
  // 合并顶部“其他歌手”和当前文件行的合唱歌手，去重
  const fileChorusArtists = pendingFile.chorusArtists || []
  const merged: ChorusArtist[] = []
  const pushUnique = (artist?: ChorusArtist) => {
    if (!artist || !artist.name?.trim()) return
    const exists = merged.some(a => (a.id && artist.id && a.id === artist.id) || a.name === artist.name)
    if (!exists) merged.push(artist)
  }
  additionalArtists.value.forEach(a => pushUnique(a))
  fileChorusArtists.forEach(a => pushUnique(a))

  const chorusArtists = merged
  for (const a of chorusArtists) {
    if (a.id != null) formData.append('chorusArtistIds', String(a.id))
    if (a.name) formData.append('chorusArtistNames', a.name)
  }
  
  try {
    const response = await fetch('/api/upload/single', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token.value}`
      },
      body: formData
    })
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }
    
    const result = await response.json()
    
    uploadList.value.push({
      filename: pendingFile.name,
      size: formatFileSize(pendingFile.size),
      path: result.data?.path || '',
      status: 'success'
    })
    
    ElMessage.success(`[${index}/${pendingFiles.value.length}] ${pendingFile.name} 上传成功`)
    
  } catch (error) {
    console.error('文件上传失败:', error)
    uploadList.value.push({
      filename: pendingFile.name,
      size: formatFileSize(pendingFile.size),
      path: '',
      status: 'error'
    })
    ElMessage.error(`[${index}/${pendingFiles.value.length}] ${pendingFile.name} 上传失败`)
    throw error
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

.file-list {
  margin-top: 20px;
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

.chorus-selector {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.chorus-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  margin-top: 4px;
}

.chorus-tags-label {
  font-size: 12px;
  color: #909399;
  margin-right: 4px;
}

.chorus-tag-item {
  margin-right: 4px;
  margin-top: 2px;
}
</style>hint {
  font-size: 14px;
  color: #909399;
}

.file-list {
  margin-top: 20px;
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
</style>