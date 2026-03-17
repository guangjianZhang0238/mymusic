<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">专辑管理</h2>
      <div class="header-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索专辑"
          style="width: 300px; margin-right: 10px"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button @click="handleSearch"><el-icon><Search /></el-icon></el-button>
          </template>
        </el-input>
        <el-button type="primary" @click="handleAddAlbum">
          <el-icon><Plus /></el-icon>
          添加专辑
        </el-button>
      </div>
    </div>
    
    <el-table
      ref="albumTableRef"
      v-loading="loading"
      :data="albumList"
      style="width: 100%"
      border
      row-key="id"
      @row-click="handleAlbumRowClick"
      @expand-change="handleExpandChange"
    >
      <el-table-column type="expand" width="50">
        <template #default="scope">
          <div class="album-song-panel">
            <div class="album-song-panel-title">专辑歌曲列表</div>
            <el-table :data="albumSongsMap[scope.row.id] || []" border size="small" class="album-song-table" empty-text="暂无歌曲数据">
              <el-table-column label="歌曲名称" min-width="180" show-overflow-tooltip>
                <template #default="songScope">
                  {{ formatName(songScope.row.title, songScope.row.titleEn) }}
                </template>
              </el-table-column>
              <el-table-column label="歌手" width="160" show-overflow-tooltip>
                <template #default="songScope">
                  {{ songScope.row.artistNames || formatName(songScope.row.artistName, songScope.row.artistNameEn) }}
                </template>
              </el-table-column>
              <el-table-column prop="durationFormat" label="时长" width="84" />
              <el-table-column prop="hasLyrics" label="歌词" width="84">
                <template #default="songScope">
                  <el-tag size="small" :type="songScope.row.hasLyrics ? 'success' : 'info'">{{ songScope.row.hasLyrics ? '已同步' : '无歌词' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="300">
                <template #default="songScope">
                  <div class="album-song-actions">
                    <el-button size="small" type="success" plain @click="handleAutoMatchSongLyrics(songScope.row)">自动匹配歌词</el-button>
                    <el-button size="small" type="primary" plain @click="openLyricsEditor(songScope.row)">编辑歌词</el-button>
                    <el-upload
                      class="inline-upload"
                      action="/api/lyrics/upload"
                      :headers="{ Authorization: `Bearer ${token}` }"
                      :data="{ songId: songScope.row.id }"
                      :show-file-list="false"
                      :on-success="(response: any) => handleLyricsUploadSuccess(response, songScope.row)"
                      :on-error="handleLyricsError"
                      :before-upload="beforeUploadLyricsFile"
                    >
                      <el-button size="small" type="info" plain>上传歌词</el-button>
                    </el-upload>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="封面" width="100">
        <template #default="scope">
          <el-avatar :size="40" :src="getCoverUrl(scope.row.coverImage)" />
        </template>
      </el-table-column>
      <el-table-column label="专辑名称" min-width="220">
        <template #default="scope">
          {{ formatName(scope.row.name, scope.row.nameEn) }}
        </template>
      </el-table-column>
      <el-table-column label="歌手" min-width="180">
        <template #default="scope">
          {{ formatName(scope.row.artistName, scope.row.artistNameEn) }}
        </template>
      </el-table-column>
      <el-table-column prop="folderPath" label="文件夹路径" width="300" />
      <el-table-column prop="songCount" label="歌曲数" width="80" />
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
      <el-table-column label="操作" width="320" fixed="right">
        <template #default="scope">
          <el-button size="small" @click="handleEditAlbum(scope.row)">
            编辑
          </el-button>
          <el-button
            size="small"
            type="warning"
            plain
            @click.stop="openSwitchArtistDialog(scope.row)"
          >
            切换歌手
          </el-button>
          <el-button
            size="small"
            type="primary"
            plain
            @click.stop="openBindSongsDialog(scope.row)"
          >
            收录歌曲
          </el-button>
          <el-button
            size="small"
            type="danger"
            @click="handleDeleteAlbum(scope.row.id)"
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
    
    <!-- 专辑编辑对话框 -->
    <LyricsEditorDialog
      v-model:visible="lyricsEditorVisible"
      :song="currentSongForLyrics"
      @saved="handleLyricsSaved"
    />

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
    >
      <el-form
        ref="albumFormRef"
        :model="albumForm"
        :rules="albumRules"
      >
        <el-form-item label="专辑名称" prop="name">
          <el-input v-model="albumForm.name" placeholder="请输入专辑名称" />
        </el-form-item>
        <el-form-item label="英文名称" prop="nameEn">
          <el-input v-model="albumForm.nameEn" placeholder="请输入英文名称" />
        </el-form-item>
        <el-form-item label="封面">
          <div class="avatar-uploader" @click="triggerCoverUpload">
            <input
              ref="coverFileInput"
              type="file"
              accept="image/*"
              style="display: none"
              @change="handleCoverFileChange"
            />
            <el-avatar
              v-if="albumForm.coverImage"
              :size="100"
              :src="getCoverUrl(albumForm.coverImage)"
            />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </div>
          <div class="cover-tip">
            提示：上传的封面会保存为专辑文件夹下的 cover.jpg 文件，覆盖已存在的封面
          </div>
        </el-form-item>
        <el-form-item label="歌手" prop="artistId">
          <el-select v-model="albumForm.artistId" placeholder="请选择歌手">
            <el-option
              v-for="artist in artistList"
              :key="artist.id"
              :label="artist.name"
              :value="artist.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="简介">
          <el-input
            v-model="albumForm.description"
            type="textarea"
            rows="4"
            placeholder="请输入专辑简介"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveAlbum">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 专辑收录歌曲对话框 -->
    <el-dialog
      v-model="bindDialogVisible"
      :title="`收录歌曲 - ${currentAlbumForBind ? formatName(currentAlbumForBind.name, currentAlbumForBind.nameEn) : ''}`"
      width="880px"
    >
      <div class="bind-dialog-header">
        <el-input
          v-model="bindSongSearchKeyword"
          placeholder="搜索歌曲（歌曲名 / 拼音）"
          style="width: 320px; margin-right: 12px"
          clearable
          @keyup.enter="loadCandidateSongs"
        >
          <template #append>
            <el-button @click="loadCandidateSongs">
              <el-icon><Search /></el-icon>
            </el-button>
          </template>
        </el-input>
        <span class="bind-dialog-tip">
          已选中 {{ bindSelectedSongIds.length }} 首歌曲，点击“确定收录”将它们加入当前专辑
        </span>
      </div>
      <el-table
        v-loading="bindSongLoading"
        :data="bindSongList"
        border
        height="420px"
        row-key="id"
        @selection-change="handleBindSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column label="歌曲名称" min-width="220" show-overflow-tooltip>
          <template #default="scope">
            {{ formatName(scope.row.title, scope.row.titleEn) }}
          </template>
        </el-table-column>
        <el-table-column label="歌手" min-width="180" show-overflow-tooltip>
          <template #default="scope">
            {{ scope.row.artistNames || formatName(scope.row.artistName, scope.row.artistNameEn) }}
          </template>
        </el-table-column>
        <el-table-column prop="durationFormat" label="时长" width="90" />
        <el-table-column label="歌词" width="90">
          <template #default="scope">
            <el-tag size="small" :type="scope.row.hasLyrics ? 'success' : 'info'">
              {{ scope.row.hasLyrics ? '已同步' : '无歌词' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="bindDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmBindSongs">确定收录</el-button>
        </span>
      </template>
    </el-dialog>

    <SwitchArtistAlbumDialog
      v-model:visible="switchArtistDialogVisible"
      title="切换专辑歌手"
      confirm-text="开始切换"
      :show-album="false"
      @confirm="confirmSwitchAlbumArtist"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import * as albumApi from '@/api/album'
import * as artistApi from '@/api/artist'
import * as songApi from '@/api/song'
import request from '@/api/request'
import LyricsEditorDialog from './LyricsEditorDialog.vue'
import SwitchArtistAlbumDialog from '@/components/SwitchArtistAlbumDialog.vue'

const userStore = useUserStore()
const token = computed(() => userStore.token)

const loading = ref(false)
const albumList = ref<any[]>([])
const artistList = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('添加专辑')
const albumFormRef = ref()
const albumTableRef = ref()
const coverFileInput = ref<HTMLInputElement>()
const albumSongsMap = reactive<Record<number, any[]>>({})
const expandedAlbumRows = ref<any[]>([])
const lyricsEditorVisible = ref(false)
const currentSongForLyrics = ref<any>(null)
const albumForm = reactive({
  id: 0,
  name: '',
  nameEn: '',
  coverImage: '',
  artistId: 0,
  releaseDate: '',
  description: '',
  folderPath: ''
})

// 收录歌曲对话框相关状态
const bindDialogVisible = ref(false)
const currentAlbumForBind = ref<any | null>(null)
const bindSongSearchKeyword = ref('')
const bindSongList = ref<any[]>([])
const bindSelectedSongIds = ref<number[]>([])
const bindSongLoading = ref(false)

// 切换歌手对话框相关
const switchArtistDialogVisible = ref(false)
const currentAlbumForSwitch = ref<any | null>(null)

const albumRules = {
  name: [
    { required: true, message: '请输入专辑名称', trigger: 'blur' }
  ],
  artistId: [
    { required: true, message: '请选择歌手', trigger: 'blur' }
  ]
}

// 获取封面 URL，对路径进行 URL 编码以支持中文和特殊字符
const getCoverUrl = (coverImage: string | undefined | null) => {
  if (!coverImage) {
    return 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=music%20album%20cover%20placeholder%20default&image_size=square'
  }
  // 对路径进行 URL 编码，保留斜杠
  const encodedPath = coverImage.split('/').map(segment => encodeURIComponent(segment)).join('/')
  return `/static/${encodedPath}`
}

const formatName = (name?: string, nameEn?: string) => {
  if (!nameEn) return name || ''
  return `${name || ''} (${nameEn})`
}

const loadingSongMap = reactive<Record<number, boolean>>({})

const loadAlbumSongs = async (albumId: number, forceReload = false) => {
  if (!albumId) return
  if (loadingSongMap[albumId]) return
  if (!forceReload && albumSongsMap[albumId] && albumSongsMap[albumId].length >= 0) return

  loadingSongMap[albumId] = true
  try {
    const response = await songApi.getSongList({ current: 1, size: 500, albumId })
    albumSongsMap[albumId] = response.records || []
  } finally {
    loadingSongMap[albumId] = false
  }
}

const handleExpandChange = async (row: any, expandedRows: any[]) => {
  if (!row?.id) return
  const expanded = expandedRows.some((item: any) => item.id === row.id)

  if (expanded) {
    try {
      await loadAlbumSongs(row.id, true)
    } catch (error: any) {
      ElMessage.error(error.message || '加载专辑歌曲失败')
    }
  }

  expandedAlbumRows.value = expandedRows
}

const handleAlbumRowClick = async (row: any) => {
  if (!row?.id || !albumTableRef.value) return

  const exists = expandedAlbumRows.value.some((item) => item.id === row.id)
  albumTableRef.value.toggleRowExpansion(row, !exists)

  if (!exists) {
    try {
      await loadAlbumSongs(row.id, true)
    } catch (error: any) {
      ElMessage.error(error.message || '加载专辑歌曲失败')
    }
  }
}

const loadArtists = async () => {
  try {
    // 调用API获取歌手列表
    const response = await artistApi.getArtistList({ current: 1, size: 100 })
    artistList.value = response.records || []
  } catch (error: any) {
    ElMessage.error(error.message || '获取歌手列表失败')
  }
}

const loadAlbums = async () => {
  loading.value = true
  try {
    // 调用API获取专辑列表
    const response = await albumApi.getAlbumList({
      current: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value
    })
    
    albumList.value = response.records || []
    total.value = response.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '获取专辑列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadAlbums()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  loadAlbums()
}

const handleCurrentChange = (current: number) => {
  currentPage.value = current
  loadAlbums()
}

const handleAddAlbum = () => {
  dialogTitle.value = '添加专辑'
  Object.assign(albumForm, {
    id: 0,
    name: '',
    nameEn: '',
    coverImage: '',
    artistId: 0,
    releaseDate: '',
    description: '',
    folderPath: ''
  })
  dialogVisible.value = true
}

const handleEditAlbum = (album: any) => {
  dialogTitle.value = '编辑专辑'

  // 若歌手不在当前下拉选项中，补充到选项里，避免编辑时只显示ID不显示名称
  if (album?.artistId && !artistList.value.some((artist: any) => artist.id === album.artistId)) {
    artistList.value.unshift({
      id: album.artistId,
      name: album.artistName || String(album.artistId),
      nameEn: album.artistNameEn || ''
    })
  }

  Object.assign(albumForm, album)
  dialogVisible.value = true
}

const handleSaveAlbum = async () => {
  if (!albumFormRef.value) return
  
  const valid = await albumFormRef.value.validate()
  if (!valid) return
  
  try {
    if (albumForm.id) {
      // 更新专辑
      await albumApi.updateAlbum(albumForm)
      ElMessage.success('专辑更新成功')
    } else {
      // 创建专辑
      await albumApi.createAlbum(albumForm)
      ElMessage.success('专辑添加成功')
    }
    dialogVisible.value = false
    loadAlbums()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleDeleteAlbum = async (id: number) => {
  await ElMessageBox.confirm('确定要删除这个专辑吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  
  try {
    await albumApi.deleteAlbum(id)
    ElMessage.success('专辑删除成功')
    loadAlbums()
  } catch (error: any) {
    ElMessage.error(error.message || '删除失败')
  }
}

const handleStatusChange = async (album: any) => {
  try {
    await albumApi.updateAlbum({
      id: album.id,
      status: album.status
    })
  } catch (error) {
    album.status = album.status === 1 ? 0 : 1
    ElMessage.error('状态更新失败')
  }
}

const openSwitchArtistDialog = (album: any) => {
  if (!album?.id) return
  currentAlbumForSwitch.value = album
  switchArtistDialogVisible.value = true
}

const confirmSwitchAlbumArtist = async (payload: { artistId: number; artistName: string }) => {
  const album = currentAlbumForSwitch.value
  if (!album?.id) {
    ElMessage.error('专辑信息无效')
    return
  }

  await ElMessageBox.confirm(
    `确定要将专辑《${formatName(album.name, album.nameEn)}》切换到歌手「${payload.artistName}」吗？\n该操作会迁移专辑文件夹并同步路径。`,
    '确认操作',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )

  try {
    const res = await albumApi.switchAlbumArtist(album.id, {
      targetArtistId: payload.artistId,
      targetArtistName: payload.artistName
    })
    if (res && res.success === false) {
      ElMessage.error(res.reason || '切换失败')
      return
    }
    ElMessage.success('切换成功')
    switchArtistDialogVisible.value = false
    await loadAlbums()
  } catch (e: any) {
    ElMessage.error(e?.message || '切换歌手失败')
  }
}

// 打开收录歌曲对话框
const openBindSongsDialog = (album: any) => {
  if (!album?.id) return
  currentAlbumForBind.value = album
  bindSongSearchKeyword.value = ''
  bindSelectedSongIds.value = []
  loadCandidateSongs()
  bindDialogVisible.value = true
}

// 加载可选歌曲列表
const loadCandidateSongs = async () => {
  bindSongLoading.value = true
  try {
    const response = await songApi.getSongList({
      current: 1,
      size: 200,
      keyword: bindSongSearchKeyword.value || undefined
    })

    const records = response.records || []

    // 可选：过滤掉已经在当前专辑里的歌曲，避免重复展示
    const albumId = currentAlbumForBind.value?.id
    const existingSongIds = albumId && albumSongsMap[albumId]
      ? new Set((albumSongsMap[albumId] || []).map((s: any) => s.id))
      : new Set<number>()

    bindSongList.value = records.filter((song: any) => {
      if (!song?.id) return false
      return !existingSongIds.has(song.id)
    })
  } catch (error: any) {
    ElMessage.error(error.message || '加载候选歌曲失败')
  } finally {
    bindSongLoading.value = false
  }
}

// 勾选变化
const handleBindSelectionChange = (selection: any[]) => {
  bindSelectedSongIds.value = selection
    .map((item: any) => item.id)
    .filter((id: any) => typeof id === 'number')
}

// 提交收录
const confirmBindSongs = async () => {
  const album = currentAlbumForBind.value
  if (!album?.id) {
    ElMessage.error('专辑信息无效')
    return
  }
  if (!bindSelectedSongIds.value.length) {
    ElMessage.warning('请先选择要收录的歌曲')
    return
  }

  try {
    await albumApi.bindAlbumSongs(album.id, bindSelectedSongIds.value)
    ElMessage.success('收录成功')
    bindDialogVisible.value = false
    // 刷新该专辑下的歌曲列表和专辑统计
    await loadAlbumSongs(album.id, true)
    await loadAlbums()
  } catch (error: any) {
    ElMessage.error(error.message || '收录歌曲失败')
  }
}

// 触发文件选择
const triggerCoverUpload = () => {
  console.log('触发封面上传')
  if (!albumForm.id) {
    ElMessage.warning('请先保存专辑基本信息后再上传封面')
    return
  }
  if (!albumForm.folderPath) {
    ElMessage.warning('专辑文件夹路径为空，请先保存专辑后重试')
    return
  }
  
  console.log('触发文件选择对话框')
  coverFileInput.value?.click()
}

// 处理文件选择变化
const handleCoverFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement
  console.log('文件选择事件触发')
  console.log('input元素:', input)
  console.log('files数组:', input.files)
  
  const fileList = input.files
  if (!fileList || fileList.length === 0) {
    console.warn('没有选择文件')
    ElMessage.warning('未选择文件')
    return
  }
  
  // 立即复制文件引用，避免input被重置导致引用失效
  const file = new File([fileList[0]], fileList[0].name, {
    type: fileList[0].type,
    lastModified: fileList[0].lastModified
  })
  
  console.log('复制的文件:', file)
  console.log('文件有效性检查:')
  console.log('- file instanceof File:', file instanceof File)
  console.log('- file.name:', file.name)
  console.log('- file.size:', file.size)
  console.log('- file.type:', file.type)
  
  // 检查文件是否有效
  if (file.size === 0) {
    ElMessage.error('文件为空，无法上传')
    return
  }
  
  // 立即重置input，避免后续问题
  input.value = ''
  
  // 验证文件
  const isImage = file.type.startsWith('image/') || /\.(jpg|jpeg|png|webp|bmp|gif)$/i.test(file.name)
  if (!isImage) {
    ElMessage.warning('仅支持图片文件作为专辑封面')
    return
  }
  
  const isLt10M = file.size <= 10 * 1024 * 1024
  if (!isLt10M) {
    ElMessage.warning('专辑封面大小不能超过10MB')
    return
  }
  
  // 上传文件
  await uploadCoverFile(file)
}

// 上传封面文件（Base64方式）
const uploadCoverFile = async (file: File) => {
  console.log('开始上传专辑封面:', file.name)
  console.log('文件详情:', {
    name: file.name,
    size: file.size,
    type: file.type,
    lastModified: file.lastModified
  })
  
  try {
    // 使用 Blob API 读取文件
    console.log('使用 Blob API 读取文件...')
    const arrayBuffer = await file.arrayBuffer()
    console.log('ArrayBuffer 读取完成，大小:', arrayBuffer.byteLength)
    
    // 转换为 Base64
    const base64Data = btoa(
      new Uint8Array(arrayBuffer)
        .reduce((data, byte) => data + String.fromCharCode(byte), '')
    )
    
    console.log('Base64编码完成，长度:', base64Data.length)
    console.log('准备发送请求到:', '/upload/album-cover/base64')
    console.log('请求参数:', {
      albumId: albumForm.id,
      folderPath: albumForm.folderPath,
      fileName: file.name,
      contentType: file.type || 'image/jpeg'
    })

    const result = await request.post('/upload/album-cover/base64', {
      albumId: albumForm.id,
      folderPath: albumForm.folderPath,
      fileName: file.name,
      contentType: file.type || 'image/jpeg',
      data: base64Data
    })

    console.log('上传响应:', result)
    albumForm.coverImage = result?.path || albumForm.coverImage
    ElMessage.success('封面上传成功')
  } catch (error: any) {
    console.error('封面上传失败:', error)
    ElMessage.error(error?.message || '封面上传失败，请重试')
  }
}

const handleSongStatusChange = async (song: any) => {
  try {
    await songApi.updateSong({ id: song.id, status: song.status })
  } catch (error) {
    song.status = song.status === 1 ? 0 : 1
    ElMessage.error('歌曲状态更新失败')
  }
}

const handleEditSong = (song: any) => {
  ElMessage.info(`请前往歌曲管理编辑《${song.title}》`)
}

const handleAutoMatchSongLyrics = async (song: any) => {
  if (!song?.id) {
    ElMessage.error('歌曲ID无效')
    return
  }

  try {
    const response = await songApi.autoMatchLyrics(song.id)
    if (response && response.success) {
      song.hasLyrics = 1
      ElMessage.success(response.message || `歌曲《${song.title}》歌词匹配成功`)
      await loadAlbumSongs(song.albumId, true)
    } else {
      ElMessage.warning(response?.message || '未匹配到歌词')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '自动匹配歌词失败')
  }
}

const openLyricsEditor = (song: any) => {
  currentSongForLyrics.value = song
  lyricsEditorVisible.value = true
}

const handleLyricsSaved = async (songId: number) => {
  const albumEntry = Object.entries(albumSongsMap).find(([, songs]) => songs.some((s: any) => s.id === songId))
  if (!albumEntry) return

  const [albumId, songs] = albumEntry
  const target = songs.find((s: any) => s.id === songId)
  if (target) {
    target.hasLyrics = 1
  }

  try {
    await loadAlbumSongs(Number(albumId), true)
  } catch (error) {
    // ignore refresh failure
  }
}

const handleLyricsUploadSuccess = (_response: any, song: any) => {
  song.hasLyrics = 1
  ElMessage.success(`歌曲《${song.title}》歌词上传成功`)
}

const handleLyricsError = () => {
  ElMessage.error('歌词上传失败，请重试')
}

const beforeUploadLyricsFile = (file: File) => {
  const lowerName = file.name.toLowerCase()
  const isLrcOrTxt = lowerName.endsWith('.lrc') || lowerName.endsWith('.txt')
  if (!isLrcOrTxt) {
    ElMessage.error('只支持上传.lrc或.txt格式的歌词文件')
    return false
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    ElMessage.error('上传文件大小不能超过 2MB')
    return false
  }
  return true
}

onMounted(() => {
  loadArtists()
  loadAlbums()
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

.avatar-uploader {
  display: block;
  cursor: pointer;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  line-height: 100px;
  text-align: center;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
}

.cover-tip {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.album-song-panel {
  margin: 4px 10px 10px 10px;
  padding: 12px;
  background: #f7f9fc;
  border: 1px solid #e8edf5;
  border-radius: 8px;
}

.album-song-panel-title {
  font-size: 13px;
  color: #606266;
  font-weight: 600;
  margin-bottom: 10px;
}

.album-song-table {
  width: 88%;
  max-width: 980px;
}

.album-song-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  justify-content: flex-start;
}

.album-song-panel :deep(.el-table .cell) {
  padding-left: 8px;
  padding-right: 8px;
}

.album-song-actions :deep(.el-upload) {
  display: inline-flex;
}
</style>