<template>
  <el-dialog
    v-model="dialogVisible"
    title="编辑歌词"
    width="720px"
    :close-on-click-modal="false"
  >
    <el-form label-width="90px">
      <el-form-item label="歌曲名称">
        <el-input :model-value="song?.title || ''" disabled />
      </el-form-item>
      <el-form-item label="歌词内容">
        <el-input
          v-model="lyricsContent"
          type="textarea"
          :rows="14"
          placeholder="请输入或粘贴歌词，支持LRC和纯文本"
        />
      </el-form-item>
      <el-form-item label="上传文件">
        <el-upload
          action="/api/lyrics/upload"
          :headers="{ Authorization: `Bearer ${token}` }"
          :data="{ songId: song?.id }"
          :show-file-list="false"
          :before-upload="beforeUploadLyricsFile"
          :on-success="handleUploadSuccess"
          :on-error="handleUploadError"
        >
          <el-button type="primary" plain>上传歌词文件</el-button>
        </el-upload>
        <span class="upload-tip">支持 .lrc / .txt，上传后会自动填充内容</span>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import request from '@/api/request'

const props = defineProps<{
  visible: boolean
  song: any
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'saved', songId: number): void
}>()

const userStore = useUserStore()
const token = computed(() => userStore.token)

const dialogVisible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value)
})

const lyricsContent = ref('')

const loadLyrics = async () => {
  if (!props.song?.id) return
  try {
    const response = await request.get(`/lyrics/song/${props.song.id}`)
    lyricsContent.value = response?.content || ''
  } catch (error) {
    lyricsContent.value = ''
  }
}

watch(
  () => props.visible,
  async (visible) => {
    if (visible) {
      await loadLyrics()
    }
  }
)

watch(
  () => props.song?.id,
  () => {
    lyricsContent.value = ''
  }
)

const beforeUploadLyricsFile = (file: File) => {
  const lowerName = file.name.toLowerCase()
  const isLrcOrTxt = lowerName.endsWith('.lrc') || lowerName.endsWith('.txt')
  if (!isLrcOrTxt) {
    ElMessage.error('只支持上传 .lrc 或 .txt 文件')
    return false
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    ElMessage.error('上传文件大小不能超过 2MB')
    return false
  }
  if (!props.song?.id) {
    ElMessage.error('歌曲信息无效')
    return false
  }
  return true
}

const handleUploadSuccess = (response: any) => {
  if (response?.content) {
    lyricsContent.value = response.content
  }
  ElMessage.success('歌词文件上传成功')
}

const handleUploadError = () => {
  ElMessage.error('歌词文件上传失败，请重试')
}

const handleSave = async () => {
  if (!props.song?.id) {
    ElMessage.error('歌曲信息无效')
    return
  }
  if (!lyricsContent.value.trim()) {
    ElMessage.warning('请输入歌词内容')
    return
  }

  try {
    await request.post('/lyrics', {
      songId: props.song.id,
      content: lyricsContent.value,
      lyricsType: 1,
      source: '专辑管理-歌词编辑'
    })
    ElMessage.success('歌词保存成功')
    emit('saved', props.song.id)
    dialogVisible.value = false
  } catch (error: any) {
    ElMessage.error(error.message || '歌词保存失败')
  }
}
</script>

<style scoped>
.upload-tip {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}
</style>
