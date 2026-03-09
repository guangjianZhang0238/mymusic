<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">歌手管理</h2>
      <div class="header-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索歌手"
          style="width: 300px; margin-right: 10px"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button @click="handleSearch"><el-icon><Search /></el-icon></el-button>
          </template>
        </el-input>
        <el-button type="success" @click="handleScanArtists">
          <el-icon><Refresh /></el-icon>
          更新歌手
        </el-button>
        <el-button type="primary" @click="handleAddArtist">
          <el-icon><Plus /></el-icon>
          添加歌手
        </el-button>
      </div>
    </div>
    
    <el-table
      v-loading="loading"
      :data="artistList"
      style="width: 100%"
      border
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="头像" width="100">
        <template #default="scope">
          <el-avatar :size="40" :src="scope.row.avatar ? '/static/' + scope.row.avatar : ''" />
        </template>
      </el-table-column>
      <el-table-column label="歌手名称">
        <template #default="scope">
          {{ formatName(scope.row.name, scope.row.nameEn) }}
        </template>
      </el-table-column>
      <el-table-column prop="region" label="地区" width="120" />
      <el-table-column prop="type" label="类型" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.type === 0 ? 'primary' : 'success'">
            {{ scope.row.type === 0 ? '个人' : '组合' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="albumCount" label="专辑数" width="80" />
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
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="scope">
          <el-button size="small" @click="handleEditArtist(scope.row)">
            编辑
          </el-button>
          <el-button
            size="small"
            type="danger"
            @click="handleDeleteArtist(scope.row.id)"
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
    
    <!-- 歌手编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
    >
      <el-form
        ref="artistFormRef"
        :model="artistForm"
        :rules="artistRules"
      >
        <el-form-item label="歌手名称" prop="name">
          <el-input v-model="artistForm.name" placeholder="请输入歌手名称" />
        </el-form-item>
        <el-form-item label="英文名称" prop="nameEn">
          <el-input v-model="artistForm.nameEn" placeholder="请输入英文名称" />
        </el-form-item>
        <el-form-item label="头像">
          <el-upload
            class="avatar-uploader"
            action="/api/upload/artist-avatar"
            :headers="{ Authorization: `Bearer ${token}` }"
            :data="{ artistId: artistForm.id, artistName: artistForm.name }"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            :on-error="handleAvatarError"
            :before-upload="beforeAvatarUpload"
            :on-progress="handleAvatarProgress"
            :disabled="avatarUploading"
          >
            <el-avatar
              v-if="artistForm.avatar && !avatarUploading"
              :size="100"
              :src="'/static/' + artistForm.avatar"
            />
            <el-icon v-else-if="!avatarUploading" class="avatar-uploader-icon"><Plus /></el-icon>
            <el-loading v-else class="avatar-uploader-loading" :fullscreen="false" text="上传中..." />
          </el-upload>
        </el-form-item>
        <el-form-item label="地区">
          <el-input v-model="artistForm.region" placeholder="请输入地区" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="artistForm.type" placeholder="请选择类型">
            <el-option label="个人" :value="0" />
            <el-option label="组合" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="简介">
          <el-input
            v-model="artistForm.description"
            type="textarea"
            rows="4"
            placeholder="请输入歌手简介"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveArtist">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getArtistList, createArtist, updateArtist, deleteArtist } from '@/api/artist'
import type { ArtistVO } from '@/api/types'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Refresh } from '@element-plus/icons-vue'

const userStore = useUserStore()
const token = computed(() => userStore.token)

const loading = ref(false)
const artistList = ref<ArtistVO[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('添加歌手')
const artistFormRef = ref()
const avatarUploading = ref(false)
const artistForm = reactive({
  id: 0,
  name: '',
  nameEn: '',
  avatar: '',
  region: '',
  type: 0,
  description: ''
})

const artistRules = {
  name: [
    { required: true, message: '请输入歌手名称', trigger: 'blur' }
  ]
}

const formatName = (name?: string, nameEn?: string) => {
  if (!nameEn) return name || ''
  return `${name || ''} (${nameEn})`
}

const loadArtists = async () => {
  loading.value = true
  try {
    const response = await getArtistList({
      keyword: searchKeyword.value,
      current: currentPage.value,
      size: pageSize.value
    })
    artistList.value = response.records
    total.value = response.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadArtists()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  loadArtists()
}

const handleCurrentChange = (current: number) => {
  currentPage.value = current
  loadArtists()
}

const handleAddArtist = () => {
  dialogTitle.value = '添加歌手'
  Object.assign(artistForm, {
    id: 0,
    name: '',
    nameEn: '',
    avatar: '',
    region: '',
    type: 0,
    description: ''
  })
  dialogVisible.value = true
}

const handleEditArtist = (artist: ArtistVO) => {
  dialogTitle.value = '编辑歌手'
  Object.assign(artistForm, artist)
  dialogVisible.value = true
}

const handleSaveArtist = async () => {
  if (!artistFormRef.value) return
  
  const valid = await artistFormRef.value.validate()
  if (!valid) return
  
  try {
    if (artistForm.id) {
      await updateArtist(artistForm)
      ElMessage.success('歌手信息更新成功')
    } else {
      await createArtist(artistForm)
      ElMessage.success('歌手创建成功')
    }
    dialogVisible.value = false
    loadArtists()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleDeleteArtist = async (id: number) => {
  await ElMessageBox.confirm('确定要删除这个歌手吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  
  try {
    await deleteArtist(id)
    ElMessage.success('歌手删除成功')
    loadArtists()
  } catch (error: any) {
    ElMessage.error(error.message || '删除失败')
  }
}

const handleStatusChange = async (artist: ArtistVO) => {
  try {
    await updateArtist(artist)
  } catch (error) {
    artist.status = artist.status === 1 ? 0 : 1
    ElMessage.error('状态更新失败')
  }
}

const handleAvatarSuccess = (response: any) => {
  avatarUploading.value = false
  console.log('头像上传响应：', response)
  // 直接访问 response.data.path
  if (response && response.data && response.data.path) {
    artistForm.avatar = response.data.path
    console.log('设置的头像路径：', artistForm.avatar)
    ElMessage.success('头像上传成功')
  } else {
    ElMessage.error('头像上传失败：返回数据格式错误')
  }
}

const handleAvatarError = (error: any) => {
  avatarUploading.value = false
  ElMessage.error('头像上传失败：' + (error.message || '未知错误'))
}

const handleAvatarProgress = (event: any) => {
  // 可以在这里添加进度条逻辑
}

const beforeAvatarUpload = (file: any) => {
  if (!artistForm.name) {
    ElMessage.error('请先输入歌手名称');
    return false;
  }
  
  // 检查文件类型
  const isImage = file.type.startsWith('image/');
  if (!isImage) {
    ElMessage.error('请上传图片文件');
    return false;
  }
  
  // 检查文件大小
  const isLt2M = file.size / 1024 / 1024 < 2;
  if (!isLt2M) {
    ElMessage.error('上传图片大小不能超过 2MB');
    return false;
  }
  
  avatarUploading.value = true
  return true;
}

const handleScanArtists = async () => {
  try {
    const module = await import('@/api/artist')
    const response = await module.scanArtists()
    ElMessage.success(`扫描完成：新增 ${response.addedCount} 个歌手，跳过 ${response.skippedCount} 个歌手`)
    loadArtists()
  } catch (error: any) {
    ElMessage.error(error.message || '扫描失败')
  }
}

onMounted(() => {
  loadArtists()
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
</style>
