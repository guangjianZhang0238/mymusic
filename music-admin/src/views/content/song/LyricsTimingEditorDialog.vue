<template>
  <el-dialog
    v-model="dialogVisible"
    title="歌词编辑器"
    width="860px"
    :close-on-click-modal="false"
  >
    <div class="timing-editor">
      <div class="left-panel">
        <div class="song-meta">
          <div><b>歌曲：</b>{{ song?.title || '-' }}</div>
          <div><b>歌手：</b>{{ song?.artistName || '-' }}</div>
        </div>

        <el-input
          v-model="lyricsText"
          type="textarea"
          :rows="16"
          placeholder="请输入或粘贴歌词（每行一句）"
        />

        <div class="hint">提示：先整理好每行歌词，再点击“开始录制”逐行打点。</div>
      </div>

      <div class="right-panel">
        <div class="current-time">当前时间：{{ formatTime(playTime) }}</div>

        <div class="progress-container">
          <el-slider
            v-model="playTime"
            :min="0"
            :max="duration"
            :disabled="duration === 0"
            :format-tooltip="formatTooltip"
            @input="handleProgressInput"
            @change="handleProgressChange"
          />
          <div class="time-display">
            <span>{{ formatTime(playTime) }}</span>
            <span>{{ formatTime(duration) }}</span>
          </div>
        </div>

        <div class="current-line-box">
          <div class="label">当前行歌词</div>
          <div class="line">{{ currentLineText || '（暂无歌词）' }}</div>
          <div class="index">{{ currentLineIndex + 1 }} / {{ lyricLines.length }}</div>
        </div>

        <div class="control-buttons">
          <el-button type="primary" @click="startRecording" :disabled="!canStartRecording">
            {{ isRecording ? '录制中...' : '开始录制' }}
          </el-button>
          <el-button @click="pauseRecording" :disabled="!audio">暂停</el-button>
          <el-button type="success" @click="markNextLine" :disabled="!isRecording || !hasNextLine">
            向下换行
          </el-button>
          <el-button type="warning" plain @click="undoLastMark" :disabled="!canUndo">
            回退上一行打点
          </el-button>
          <el-button @click="resetRecording">重置</el-button>
        </div>
        <div class="shortcut-tip">快捷键：录制中按 <b>↓</b> 或 <b>Space</b> 可向下换行</div>

        <el-table :data="previewRows" size="small" border height="300">
          <el-table-column prop="index" label="#" width="50" />
          <el-table-column prop="time" label="时间" width="90" />
          <el-table-column prop="text" label="歌词" show-overflow-tooltip />
        </el-table>
      </div>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="saveTimedLyrics">保存歌词时间轴</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const props = defineProps<{
  visible: boolean
  song: any
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'saved', songId: number): void
}>()

const dialogVisible = computed({
  get: () => props.visible,
  set: (v: boolean) => emit('update:visible', v)
})

const lyricsText = ref('')
const playTime = ref(0)
const duration = ref(0)
const currentLineIndex = ref(0)
const lineTimes = ref<number[]>([])
const isRecording = ref(false)
const isDragging = ref(false)
const audio = ref<HTMLAudioElement | null>(null)

const lyricLines = computed(() => lyricsText.value.split('\n').map(l => l.trim()).filter(Boolean))
const currentLineText = computed(() => lyricLines.value[currentLineIndex.value] || '')
const canStartRecording = computed(() => !!props.song?.filePath && lyricLines.value.length > 0)
const hasNextLine = computed(() => currentLineIndex.value < lyricLines.value.length - 1)
const canUndo = computed(() => lineTimes.value.length > 1 || currentLineIndex.value > 0)

const previewRows = computed(() => lyricLines.value.map((text, i) => ({
  index: i + 1,
  time: lineTimes.value[i] != null ? formatTime(lineTimes.value[i]) : '--:--.--',
  text
})))

const formatTime = (seconds: number) => {
  const safe = Math.max(0, seconds)
  const mins = Math.floor(safe / 60)
  const secs = Math.floor(safe % 60)
  const cent = Math.floor((safe - Math.floor(safe)) * 100)
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}.${String(cent).padStart(2, '0')}`
}

const formatTooltip = (value: number) => formatTime(value)

const handleProgressInput = (value: number) => {
  isDragging.value = true
  playTime.value = value
}

const handleProgressChange = (value: number) => {
  isDragging.value = false
  if (!audio.value || duration.value <= 0) {
    return
  }

  const safeTime = Math.max(0, Math.min(value, duration.value))
  playTime.value = safeTime
  audio.value.currentTime = safeTime
}

const setupAudio = () => {
  if (!props.song?.filePath) return
  const songUrl = `/static/${props.song.filePath}`
  audio.value = new Audio(songUrl)
  audio.value.addEventListener('loadedmetadata', () => {
    duration.value = audio.value?.duration || 0
  })
  audio.value.addEventListener('timeupdate', () => {
    if (!isDragging.value) {
      playTime.value = audio.value?.currentTime || 0
    }
  })
  audio.value.addEventListener('ended', () => {
    isRecording.value = false
  })
}

const startRecording = async () => {
  if (!canStartRecording.value) {
    ElMessage.warning('请先准备歌词并确认歌曲文件可播放')
    return
  }

  if (!audio.value) {
    setupAudio()
  }

  if (!audio.value) return

  if (!isRecording.value) {
    currentLineIndex.value = 0
    lineTimes.value = []
    audio.value.currentTime = 0
  }

  try {
    await audio.value.play()
    isRecording.value = true
    lineTimes.value[0] = audio.value.currentTime || 0
  } catch (error: any) {
    ElMessage.error(error.message || '播放失败')
  }
}

const pauseRecording = () => {
  if (audio.value) {
    audio.value.pause()
  }
  isRecording.value = false
}

const markNextLine = () => {
  if (!audio.value || !isRecording.value) return
  if (!hasNextLine.value) {
    ElMessage.info('已到最后一行')
    return
  }

  const nextIndex = currentLineIndex.value + 1
  lineTimes.value[nextIndex] = audio.value.currentTime || 0
  currentLineIndex.value = nextIndex
}

const undoLastMark = () => {
  if (!canUndo.value) return

  if (currentLineIndex.value > 0) {
    lineTimes.value.splice(currentLineIndex.value, 1)
    currentLineIndex.value = currentLineIndex.value - 1
    return
  }

  if (lineTimes.value.length > 1) {
    lineTimes.value.splice(lineTimes.value.length - 1, 1)
  }
}

const handleKeydown = (event: KeyboardEvent) => {
  if (!props.visible || !isRecording.value) return

  const target = event.target as HTMLElement | null
  if (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA')) {
    return
  }

  if (event.key === 'ArrowDown' || event.key === ' ') {
    event.preventDefault()
    markNextLine()
  }
}

const resetRecording = () => {
  currentLineIndex.value = 0
  lineTimes.value = []
  playTime.value = 0
  duration.value = audio.value?.duration || duration.value
  isRecording.value = false
  if (audio.value) {
    audio.value.pause()
    audio.value.currentTime = 0
  }
}

const buildLrc = () => {
  let lastTime = 0
  return lyricLines.value.map((line, index) => {
    const t = lineTimes.value[index] != null ? lineTimes.value[index] : lastTime + 2
    lastTime = t
    return `[${formatTime(t)}]${line}`
  }).join('\n')
}

const saveTimedLyrics = async () => {
  if (!props.song?.id) {
    ElMessage.error('歌曲ID无效')
    return
  }
  if (lyricLines.value.length === 0) {
    ElMessage.warning('请先输入歌词')
    return
  }

  const lrcContent = buildLrc()
  try {
    await request.post('/lyrics', {
      songId: props.song.id,
      content: lrcContent,
      lyricsType: 1,
      source: '歌词编辑器-打点'
    })
    ElMessage.success('歌词时间轴保存成功')
    emit('saved', props.song.id)
    dialogVisible.value = false
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  }
}

const loadSongLyrics = async () => {
  if (!props.song?.id) return
  try {
    const data = await request.get(`/lyrics/song/${props.song.id}`)
    const content = String(data?.content || '')
      .replace(/\r\n/g, '\n')
      .replace(/\r/g, '\n')

    const lines = content.split('\n').map((line: string) => line.trim()).filter(Boolean)
    const lrcRegex = /^\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)$/
    const plainLines: string[] = []
    const times: number[] = []

    lines.forEach((line: string) => {
      const match = line.match(lrcRegex)
      if (match) {
        const m = Number(match[1])
        const s = Number(match[2])
        const csRaw = match[3]
        const cs = csRaw.length === 3 ? Number(csRaw) / 1000 : Number(csRaw) / 100
        times.push(m * 60 + s + cs)
        plainLines.push((match[4] || '').trim())
      } else {
        plainLines.push(line)
      }
    })

    lyricsText.value = plainLines.join('\n')
    lineTimes.value = times
    currentLineIndex.value = 0
  } catch (e) {
    lyricsText.value = ''
    lineTimes.value = []
  }
}

watch(() => props.visible, async (visible) => {
  if (visible) {
    resetRecording()
    await loadSongLyrics()
  } else {
    pauseRecording()
  }
})

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown)
  if (audio.value) {
    audio.value.pause()
    audio.value = null
  }
})
</script>

<style scoped>
.timing-editor {
  display: flex;
  gap: 14px;
}

.left-panel {
  width: 46%;
}

.right-panel {
  width: 54%;
}

.song-meta {
  margin-bottom: 10px;
  font-size: 13px;
  color: #606266;
}

.hint {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}

.current-time {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
}

.progress-container {
  margin-bottom: 10px;
}

.time-display {
  margin-top: 4px;
  display: flex;
  justify-content: space-between;
  color: #909399;
  font-size: 12px;
}

.current-line-box {
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 10px;
  margin-bottom: 10px;
}

.current-line-box .label {
  font-size: 12px;
  color: #909399;
}

.current-line-box .line {
  font-size: 16px;
  color: #303133;
  margin: 8px 0;
  min-height: 24px;
}

.current-line-box .index {
  font-size: 12px;
  color: #67c23a;
}

.control-buttons {
  margin-bottom: 10px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.shortcut-tip {
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
}
</style>
