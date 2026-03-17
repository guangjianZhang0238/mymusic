<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">歌曲管理</h2>
      <div class="header-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索歌曲"
          style="width: 260px; margin-right: 10px"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button @click="handleSearch"><el-icon><Search /></el-icon></el-button>
          </template>
        </el-input>
        <el-select
          v-model="lyricsFilter"
          style="width: 180px; margin-right: 10px"
          @change="handleLyricsFilterChange"
        >
          <el-option label="全部歌曲" value="all" />
          <el-option label="无歌词歌曲" value="noLyrics" />
        </el-select>
        <el-button type="primary" @click="handleAddSong">
          <el-icon><Plus /></el-icon>
          添加歌曲
        </el-button>
        <el-button
          style="margin-left: 10px"
          @click="openAddToAlbumDialog"
        >
          将选中歌曲加入专辑
        </el-button>
        <el-button
          style="margin-left: 10px"
          type="warning"
          plain
          @click="openBatchSwitchArtistDialog"
        >
          批量修改歌手
        </el-button>
        <el-button
          style="margin-left: 10px"
          type="danger"
          plain
          @click="handleBatchDelete"
        >
          批量删除
        </el-button>
      </div>
    </div>
    
    <el-table
      v-loading="loading"
      :data="songList"
      style="width: 100%"
      border
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="歌曲名称">
        <template #default="scope">
          <span class="song-title-link" @click="handleSongClick(scope.row)">{{ formatName(scope.row.title, scope.row.titleEn) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="歌手">
        <template #default="scope">
          {{ scope.row.artistNames || formatName(scope.row.artistName, scope.row.artistNameEn) }}
        </template>
      </el-table-column>
      <el-table-column label="专辑">
        <template #default="scope">
          {{ formatName(scope.row.albumName, scope.row.albumNameEn) }}
        </template>
      </el-table-column>
      <el-table-column prop="format" label="格式" width="80" />
      <el-table-column prop="durationFormat" label="时长" width="100" />
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
      <el-table-column label="操作" width="390" fixed="right">
        <template #default="scope">
          <div class="action-buttons-row">
            <el-button size="small" @click="handleEditSong(scope.row)">编辑</el-button>
            <el-button size="small" type="primary" @click="openManualLyricsDialog(scope.row)">填写歌词</el-button>
            <el-button size="small" type="info" @click="openTimingEditor(scope.row)">歌词编辑器</el-button>
            <el-button size="small" type="danger" @click="handleDeleteSong(scope.row.id)">删除</el-button>
          </div>
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
    
    <!-- 歌曲编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
    >
      <el-form
        ref="songFormRef"
        :model="songForm"
        :rules="songRules"
      >
        <el-form-item label="歌曲名称" prop="title">
          <el-input v-model="songForm.title" placeholder="请输入歌曲名称" />
        </el-form-item>
        <el-form-item label="英文名称" prop="titleEn">
          <el-input v-model="songForm.titleEn" placeholder="请输入英文名称" />
        </el-form-item>
        <el-form-item label="歌手">
          <el-input v-model="songForm.artistName" disabled />
        </el-form-item>
        <el-form-item label="专辑">
          <el-input v-model="songForm.albumName" disabled />
        </el-form-item>
        <el-form-item label="歌曲文件">
          <el-upload
            class="file-uploader"
            action="/api/upload/single"
            :headers="{ Authorization: `Bearer ${token}` }"
            :data="{ userId: 1 }"
            :show-file-list="false"
            :on-success="handleFileSuccess"
          >
            <el-button type="primary">
              <el-icon><Upload /></el-icon>
              上传歌曲文件
            </el-button>
          </el-upload>
          <div v-if="songForm.filePath" class="file-path">
            文件已上传: {{ songForm.filePath }}
          </div>
        </el-form-item>
        <el-form-item label="歌词文件">
          <el-upload
            class="file-uploader"
            action="/api/lyrics/upload"
            :headers="{ Authorization: `Bearer ${token}` }"
            :data="{ songId: songForm.id }"
            :show-file-list="false"
            :on-success="handleLyricsSuccess"
            :on-error="handleLyricsError"
            :before-upload="beforeUploadLyricsFile"
          >
            <el-button type="primary">
              <el-icon><Upload /></el-icon>
              上传歌词文件
            </el-button>
          </el-upload>
          <div class="lyrics-status" :class="songForm.hasLyrics ? 'lyrics-exists' : 'lyrics-none'">
            <el-icon>
              <CircleCheck v-if="songForm.hasLyrics" />
              <CircleClose v-else />
            </el-icon>
            {{ songForm.hasLyrics ? '已有歌词文件' : '暂无匹配歌词文件' }}
          </div>
          <div class="upload-tip">
            提示：支持上传.lrc格式的歌词文件，上传后会自动关联到歌曲
          </div>
        </el-form-item>
        <el-form-item label="自动匹配歌词">
          <el-button 
            type="info" 
            @click="handleAutoMatchLyrics"
            :disabled="!songForm.id"
          >
            <el-icon><Refresh /></el-icon>
            自动匹配歌词
          </el-button>
          <div class="upload-tip">
            提示：根据歌曲名称和歌手名称自动从歌词库匹配歌词
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveSong">保存</el-button>
        </span>
      </template>
    </el-dialog>
    
    <el-dialog
      v-model="manualLyricsDialogVisible"
      title="手动填写歌词"
      width="700px"
    >
      <el-form label-width="90px">
        <el-form-item label="歌曲名称">
          <el-input v-model="manualLyricsForm.songTitle" disabled />
        </el-form-item>
        <el-form-item label="歌词内容">
          <el-input
            v-model="manualLyricsForm.content"
            type="textarea"
            :rows="14"
            placeholder="请粘贴歌词内容，支持LRC或纯文本"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="manualLyricsDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitManualLyrics">保存歌词</el-button>
      </template>
    </el-dialog>

    <LyricsTimingEditorDialog
      v-model:visible="timingEditorVisible"
      :song="timingEditorSong"
      @saved="handleTimingSaved"
    />

    <!-- 歌曲播放器弹窗 -->
    <SongPlayer 
      v-model:visible="playerVisible" 
      :song="currentSong"
      :playlist="songList"
    />

    <!-- 批量加入专辑对话框 -->
    <el-dialog
      v-model="addToAlbumDialogVisible"
      title="将选中歌曲加入到专辑"
      width="480px"
    >
      <p style="margin-bottom: 16px;">
        当前已选中 <strong>{{ addToAlbumSongIds.length }}</strong> 首歌曲，请选择要加入的目标专辑。
      </p>
      <el-form label-width="80px">
        <el-form-item label="目标专辑">
          <el-select
            v-model="addToAlbumTargetAlbumId"
            placeholder="请选择专辑"
            filterable
            style="width: 100%;"
          >
            <el-option
              v-for="album in albumList"
              :key="album.id"
              :label="formatName(album.name, album.nameEn)"
              :value="album.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="addToAlbumDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmAddSongsToAlbum">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <SwitchArtistAlbumDialog
      v-model:visible="batchSwitchDialogVisible"
      title="批量修改歌手"
      confirm-text="开始修改"
      :show-album="true"
      @confirm="confirmBatchSwitchArtist"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Upload, Refresh, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import * as songApi from '@/api/song'
import * as artistApi from '@/api/artist'
import * as albumApi from '@/api/album'
import request from '@/api/request'
import SongPlayer from './SongPlayer.vue'
import LyricsTimingEditorDialog from './LyricsTimingEditorDialog.vue'
import SwitchArtistAlbumDialog from '@/components/SwitchArtistAlbumDialog.vue'

const userStore = useUserStore()
const token = computed(() => userStore.token)

const loading = ref(false)
const songList = ref<any[]>([])
const artistList = ref<any[]>([])
const albumList = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')
const lyricsFilter = ref<'all' | 'noLyrics'>('all')

const dialogVisible = ref(false)
const dialogTitle = ref('添加歌曲')
const songFormRef = ref()
const songForm = reactive({
  id: 0,
  title: '',
  titleEn: '',
  artistId: 0,
  albumId: 0,
  // 展示用字段（从详情接口补全）
  artistName: '',
  albumName: '',
  filePath: '',
  lyricsPath: '',
  duration: 0,
  // 是否已有歌词标记：0/1
  hasLyrics: 0
})

const manualLyricsDialogVisible = ref(false)
const manualLyricsForm = reactive({
  songId: 0,
  songTitle: '',
  content: ''
})

const timingEditorVisible = ref(false)
const timingEditorSong = ref<any>(null)

// 批量加入专辑相关状态
const selectedSongs = ref<any[]>([])
const addToAlbumDialogVisible = ref(false)
const addToAlbumTargetAlbumId = ref<number | null>(null)
const addToAlbumSongIds = ref<number[]>([])

// 批量切换歌手
const batchSwitchDialogVisible = ref(false)

// 歌曲播放器相关
const playerVisible = ref(false)
const currentSong = ref({})

const handleSongClick = (song: any) => {
  currentSong.value = song
  playerVisible.value = true
}

// 多选变化
const handleSelectionChange = (selection: any[]) => {
  selectedSongs.value = selection
}

const songRules = {
  title: [
    { required: true, message: '请输入歌曲名称', trigger: 'blur' }
  ]
}

const formatName = (name?: string, nameEn?: string) => {
  if (!nameEn) return name || ''
  return `${name || ''} (${nameEn})`
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
  try {
    // 调用API获取专辑列表
    const response = await albumApi.getAlbumList({ current: 1, size: 100 })
    albumList.value = response.records || []
  } catch (error: any) {
    ElMessage.error(error.message || '获取专辑列表失败')
  }
}

const loadSongs = async () => {
  loading.value = true
  try {
    // 调用API获取歌曲列表
    const response = await songApi.getSongList({
      current: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value,
      hasLyrics: lyricsFilter.value === 'noLyrics' ? 0 : undefined
    })
    
    songList.value = response.records || []
    total.value = response.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '获取歌曲列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadSongs()
}

const handleLyricsFilterChange = () => {
  currentPage.value = 1
  loadSongs()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  loadSongs()
}

const handleCurrentChange = (current: number) => {
  currentPage.value = current
  loadSongs()
}

// 打开“加入到专辑”对话框
const openAddToAlbumDialog = () => {
  if (!selectedSongs.value.length) {
    ElMessage.warning('请先选择要加入的歌曲')
    return
  }
  addToAlbumSongIds.value = selectedSongs.value
    .map((item: any) => item.id)
    .filter((id: any) => typeof id === 'number')
  if (!addToAlbumSongIds.value.length) {
    ElMessage.warning('选中的歌曲数据无效')
    return
  }
  addToAlbumTargetAlbumId.value = null
  addToAlbumDialogVisible.value = true
}

// 确认批量加入到专辑
const confirmAddSongsToAlbum = async () => {
  if (!addToAlbumSongIds.value.length) {
    ElMessage.warning('请先选择要加入的歌曲')
    return
  }
  if (!addToAlbumTargetAlbumId.value) {
    ElMessage.warning('请选择目标专辑')
    return
  }

  try {
    await albumApi.bindAlbumSongs(addToAlbumTargetAlbumId.value, addToAlbumSongIds.value)
    ElMessage.success('已将选中歌曲加入到专辑')
    addToAlbumDialogVisible.value = false
    // 刷新列表以保持数据一致
    await loadSongs()
  } catch (error: any) {
    ElMessage.error(error.message || '加入专辑失败')
  }
}

const openBatchSwitchArtistDialog = () => {
  if (!selectedSongs.value.length) {
    ElMessage.warning('请先选择要修改的歌曲')
    return
  }
  batchSwitchDialogVisible.value = true
}

const confirmBatchSwitchArtist = async (payload: { artistId: number; artistName: string; albumName?: string | null }) => {
  const songIds = selectedSongs.value
    .map((s: any) => s.id)
    .filter((id: any) => typeof id === 'number')

  if (!songIds.length) {
    ElMessage.warning('选中的歌曲数据无效')
    return
  }

  try {
    const res = await songApi.batchSwitchArtist({
      songIds,
      targetArtistId: payload.artistId,
      targetArtistName: payload.artistName,
      targetAlbumName: payload.albumName ?? null
    })

    const successCount = (res?.successList?.length ?? res?.successCount ?? 0) as number
    const skipCount = (res?.skipList?.length ?? res?.skipCount ?? 0) as number

    if (skipCount > 0) {
      const skipPreview = (res?.skipList || [])
        .slice(0, 10)
        .map((it: any) => `${it.id}: ${it.reason}`)
        .join('\n')
      await ElMessageBox.alert(
        `批量修改完成：成功 ${successCount} 条，跳过 ${skipCount} 条。\n\n跳过明细（最多展示10条）：\n${skipPreview || '(无明细)'}\n\n提示：跳过通常由“目标路径已存在/源文件不存在/校验失败”等导致。`,
        '已完成',
        { type: 'warning' }
      )
    } else {
      ElMessage.success(`批量修改完成：成功 ${successCount} 条`)
    }
    await loadSongs()
  } catch (e: any) {
    ElMessage.error(e?.message || '批量修改歌手失败')
  }
}

const handleBatchDelete = async () => {
  const songIds = selectedSongs.value
    .map((s: any) => s.id)
    .filter((id: any) => typeof id === 'number')
  if (!songIds.length) {
    ElMessage.warning('请先选择要删除的歌曲')
    return
  }

  await ElMessageBox.confirm(
    `确定要批量删除选中的 ${songIds.length} 首歌曲吗？此操作会同时清理关联关系。`,
    '警告',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )

  try {
    const res = await songApi.batchDeleteSongs(songIds)
    const successCount = (res?.successList?.length ?? res?.successCount ?? 0) as number
    const skipCount = (res?.skipList?.length ?? res?.skipCount ?? 0) as number
    if (skipCount > 0) {
      const skipPreview = (res?.skipList || [])
        .slice(0, 10)
        .map((it: any) => `${it.id}: ${it.reason}`)
        .join('\n')
      await ElMessageBox.alert(
        `批量删除完成：成功 ${successCount} 条，跳过 ${skipCount} 条。\n\n跳过明细（最多展示10条）：\n${skipPreview || '(无明细)'}`,
        '已完成',
        { type: 'warning' }
      )
    } else {
      ElMessage.success(`批量删除完成：成功 ${successCount} 条`)
    }
    await loadSongs()
  } catch (e: any) {
    ElMessage.error(e?.message || '批量删除失败')
  }
}

const handleAddSong = () => {
  dialogTitle.value = '添加歌曲'
  Object.assign(songForm, {
    id: 0,
    title: '',
    titleEn: '',
    artistId: 0,
    albumId: 0,
    filePath: '',
    lyricsPath: '',
    duration: 0
  })
  dialogVisible.value = true
}

const handleEditSong = async (song: any) => {
  dialogTitle.value = '编辑歌曲'
  
  if (song.id) {
    try {
      const response = await songApi.getSongDetail(song.id)
      Object.assign(songForm, response)
    } catch (error: any) {
      ElMessage.error('获取歌曲详情失败')
      return
    }
  } else {
    Object.assign(songForm, song)
  }
  
  dialogVisible.value = true
}

const handleSaveSong = async () => {
  if (!songFormRef.value) return
  
  const valid = await songFormRef.value.validate()
  if (!valid) return
  
  try {
    if (songForm.id) {
      // 更新歌曲
      await songApi.updateSong(songForm)
      ElMessage.success('歌曲更新成功')
    } else {
      // 创建歌曲
      await songApi.createSong(songForm)
      ElMessage.success('歌曲添加成功')
    }
    dialogVisible.value = false
    loadSongs()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleDeleteSong = async (id: number) => {
  await ElMessageBox.confirm('确定要删除这首歌吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  
  try {
    await songApi.deleteSong(id)
    ElMessage.success('歌曲删除成功')
    loadSongs()
  } catch (error: any) {
    ElMessage.error(error.message || '删除失败')
  }
}

const handleStatusChange = async (song: any) => {
  try {
    await songApi.updateSong({
      id: song.id,
      status: song.status
    })
  } catch (error) {
    song.status = song.status === 1 ? 0 : 1
    ElMessage.error('状态更新失败')
  }
}

const handleFileSuccess = (response: any) => {
  songForm.filePath = response.path
}

const handleLyricsSuccess = (response: any) => {
  ElMessage.success('歌词文件上传成功，已自动关联到歌曲')
  songForm.hasLyrics = 1
  loadSongs()
}

// 歌词上传失败回调
const handleLyricsError = () => {
  ElMessage.error('歌词文件上传失败')
}

// 歌词上传前校验（例如只允许 .lrc）
const beforeUploadLyricsFile = (file: File) => {
  const isLrc = file.name.toLowerCase().endsWith('.lrc')
  if (!isLrc) {
    ElMessage.error('仅支持上传 .lrc 格式的歌词文件')
  }
  return isLrc
}

const openTimingEditor = (song: any) => {
  timingEditorSong.value = song
  timingEditorVisible.value = true
}

const handleTimingSaved = (songId: number) => {
  const target = songList.value.find((item: any) => item.id === songId)
  if (target) {
    target.hasLyrics = 1
  }
  loadSongs()
}

const handleAutoMatchLyrics = async () => {
  if (!songForm.id) {
    ElMessage.error('请先保存歌曲信息，获取歌曲ID后再进行歌词匹配')
    return
  }
  
  try {
    const response = await songApi.autoMatchLyrics(songForm.id)
    if (response && response.success) {
      ElMessage.success(response.message || '歌词匹配成功')
      songForm.hasLyrics = 1
    } else {
      ElMessage.warning(response.message || '未找到匹配的歌词')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '歌词匹配失败')
  }
}


const openManualLyricsDialog = (song: any) => {
  manualLyricsForm.songId = song.id
  manualLyricsForm.songTitle = song.title
  manualLyricsForm.content = ''
  manualLyricsDialogVisible.value = true
}

const submitManualLyrics = async () => {
  if (!manualLyricsForm.songId) {
    ElMessage.error('歌曲ID无效')
    return
  }
  if (!manualLyricsForm.content.trim()) {
    ElMessage.warning('请输入歌词内容')
    return
  }

  try {
    await request.post('/lyrics', {
      songId: manualLyricsForm.songId,
      content: manualLyricsForm.content,
      lyricsType: 1,
      source: '管理端手动录入'
    })
    ElMessage.success('歌词保存成功')
    manualLyricsDialogVisible.value = false
    loadSongs()
  } catch (error: any) {
    ElMessage.error(error.message || '歌词保存失败')
  }
}

onMounted(() => {
  loadArtists()
  loadAlbums()
  loadSongs()
})
</script>

<style scoped>
.song-list {
  padding: 20px;
}

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

.file-uploader {
  margin-bottom: 10px;
}

.file-path {
  margin-top: 5px;
  font-size: 12px;
  color: #606266;
  word-break: break-all;
}

.song-title-link {
  color: #409EFF;
  cursor: pointer;
  text-decoration: underline;
  transition: color 0.3s ease;
}

.song-title-link:hover {
  color: #66B1FF;
}

.lyrics-status {
  display: inline-flex;
  align-items: center;
  margin-left: 10px;
  font-size: 14px;
}

.lyrics-status .el-icon {
  margin-right: 5px;
}

.lyrics-exists {
  color: #67C23A;
}

.lyrics-none {
  color: #F56C6C;
}

.action-buttons-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
}

.action-buttons-row :deep(.el-button) {
  margin-left: 0 !important;
}

.action-buttons-row :deep(.el-upload) {
  display: inline-flex;
}
</style>