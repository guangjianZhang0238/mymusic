<template>
  <el-dialog
    v-model="dialogVisible"
    :title="song.title"
    width="800px"
    :close-on-click-modal="false"
  >
    <div class="song-player-container">
      <!-- 专辑封面和歌曲信息 -->
      <div class="player-header">
        <div class="album-cover">
          <img 
            :src="albumCoverUrl" 
            alt="专辑封面"
            class="cover-image"
          />
        </div>
        <div class="song-info">
          <h3 class="song-title">{{ song.title }}</h3>
          <p class="song-artist">{{ song.artistName }}</p>
          <p class="song-album">{{ song.albumName }}</p>
          <div class="song-actions">
            <el-button type="primary" @click="handleLike" :icon="isLiked ? 'Star' : 'StarFilled'">
              {{ isLiked ? '已点赞' : '点赞' }}
            </el-button>
            <el-button @click="handleComment">
              <el-icon><ChatDotRound /></el-icon> 评论
            </el-button>
          </div>
        </div>
      </div>
      
      <!-- 播放控制和进度条 -->
      <div class="player-controls">
        <el-button @click="handlePrevious" :icon="isFirstSong ? 'VideoPlay' : 'VideoPlay'"></el-button>
        <el-button 
          type="primary" 
          :icon="isPlaying ? 'VideoPause' : 'VideoPlay'"
          @click="togglePlay"
        >
          {{ isPlaying ? '暂停' : '播放' }}
        </el-button>
        <el-button @click="handleNext" :icon="isLastSong ? 'VideoPlay' : 'VideoPlay'"></el-button>
      </div>
      
      <div class="progress-container">
        <el-slider 
          v-model="currentTime" 
          :min="0" 
          :max="duration" 
          @input="handleProgressInput"
          @change="handleProgressChange"
          :disabled="duration === 0"
          :format-tooltip="formatTooltip"
        />
        <div class="time-display">
          <span>{{ formatTime(currentTime) }}</span>
          <span>{{ formatTime(duration) }}</span>
        </div>
      </div>
      
      <!-- 歌词滚动 -->
      <div class="lyrics-container">
        <div class="lyrics-title">
          <el-icon><Reading /></el-icon> 歌词
        </div>
        <!-- 歌词校准 -->
        <div class="lyrics-calibration">
          <el-button size="small" @click="adjustLyricsOffset(-0.1)">前移 0.1s</el-button>
          <el-input-number 
            v-model="lyricsOffset" 
            :step="0.1" 
            :precision="1"
            style="width: 100px; margin: 0 10px"
            @change="handleOffsetChange"
          />
          <el-button size="small" @click="adjustLyricsOffset(0.1)">后移 0.1s</el-button>
        </div>
        <div class="lyrics-content" ref="lyricsContent">
          <p 
            v-for="(line, index) in parsedLyrics" 
            :key="index"
            :class="{ 'current-line': currentLyricIndex === index }"
          >
            {{ line.text }}
          </p>
        </div>
      </div>
      
      <!-- 歌曲评论 -->
      <div class="comments-container">
        <div class="comments-title">
          <el-icon><ChatLineRound /></el-icon> 评论 ({{ comments.length }})
        </div>
        
        <!-- 评论输入 -->
        <div class="comment-input">
          <el-input
            v-model="newComment"
            type="textarea"
            rows="3"
            placeholder="写下你的评论..."
          />
          <el-button 
            type="primary" 
            @click="submitComment"
            :disabled="!newComment.trim()"
          >
            发表评论
          </el-button>
        </div>
        
        <!-- 评论列表 -->
        <div class="comments-list">
          <div 
            v-for="(comment, index) in comments" 
            :key="index"
            class="comment-item"
          >
            <div class="comment-avatar">
              <el-avatar :size="40">
                {{ comment.userName.charAt(0) }}
              </el-avatar>
            </div>
            <div class="comment-content">
              <div class="comment-header">
                <span class="comment-user">{{ comment.userName }}</span>
                <span class="comment-time">{{ comment.time }}</span>
              </div>
              <p class="comment-text">{{ comment.content }}</p>
              <div class="comment-actions">
                <el-button 
                  size="small" 
                  @click="handleCommentLike(comment)"
                  :icon="comment.isLiked ? 'Star' : 'StarFilled'"
                >
                  {{ comment.likes }}
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Star, StarFilled, ChatDotRound, Reading, ChatLineRound, VideoPlay, VideoPause } from '@element-plus/icons-vue'
import request from '@/api/request'

// Props
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  song: {
    type: Object,
    default: () => ({})
  }
})

// Emits
const emit = defineEmits(['update:visible'])

// Reactive data
const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

const isPlaying = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const lyrics = ref<string[]>([])
const parsedLyrics = ref<Array<{time: number, text: string}>>([])
const currentLyricIndex = ref(0)
const lyricsOffset = ref(0) // 歌词偏移量，单位：秒
const comments = ref<any[]>([])
const newComment = ref('')
const isLiked = ref(false)
const isFirstSong = ref(true)
const isLastSong = ref(true)

// Refs
const lyricsContent = ref<HTMLElement>()

// Audio对象
const audio = ref<HTMLAudioElement | null>(null)
const playbackInterval = ref<number | null>(null)

// 拖动标志
const isDragging = ref(false)

// Computed
const albumCoverUrl = computed(() => {
  if (props.song.albumCover) {
    return `/static/${props.song.albumCover}`
  }
  return 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=music%20album%20cover%20placeholder%20default&image_size=square'
})

// Methods
const togglePlay = () => {
  isPlaying.value = !isPlaying.value
  if (isPlaying.value) {
    playSong()
  } else {
    pauseSong()
  }
}

const playSong = () => {
  // 创建Audio对象并播放歌曲
  if (!audio.value) {
    // 构建正确的歌曲URL
    let songUrl = ''
    if (props.song.filePath) {
      // 如果filePath已经是完整URL，则直接使用
      if (props.song.filePath.startsWith('http')) {
        songUrl = props.song.filePath
      } else {
        // 否则构建相对路径：
        // 1. 将 Windows 路径中的反斜杠统一替换为 URL 斜杠
        // 2. 对中文和空格等特殊字符进行编码
        const normalizedPath = String(props.song.filePath).replace(/\\/g, '/')
        songUrl = `/static/${encodeURI(normalizedPath)}`
      }
    }
    
    if (!songUrl) {
      ElMessage.error('歌曲文件路径不存在，无法播放')
      isPlaying.value = false
      return
    }
    
    console.log('播放歌曲URL:', songUrl)
    
    audio.value = new Audio(songUrl)
    
    // 为DSF等特殊格式设置额外的属性
    if (props.song.format && props.song.format.toLowerCase() === 'dsf') {
      // 设置MIME类型提示（虽然浏览器可能不支持）
      console.log('检测到DSF格式文件')
    }
    
    // 设置音频事件监听
    audio.value.addEventListener('loadedmetadata', () => {
      duration.value = audio.value?.duration || 0
      console.log('音频元数据加载完成，时长:', duration.value, '秒')
    })
    
    audio.value.addEventListener('canplay', () => {
      console.log('音频可以播放')
    })
    
    audio.value.addEventListener('timeupdate', () => {
      if (audio.value && !isDragging.value) {
        currentTime.value = audio.value.currentTime
        // 根据播放时间更新歌词索引
        updateLyricIndex(currentTime.value)
      }
    })
    
    audio.value.addEventListener('ended', () => {
      isPlaying.value = false
      currentTime.value = 0
      currentLyricIndex.value = 0
      ElMessage.info('歌曲播放完成')
    })
    
    audio.value.addEventListener('error', (e) => {
      const error = e as ErrorEvent
      console.error('音频播放错误:', error)
      let errorMessage = '歌曲播放失败'
      
      // 根据错误类型提供更具体的错误信息
      if (audio.value?.error) {
        switch (audio.value.error.code) {
          case MediaError.MEDIA_ERR_ABORTED:
            errorMessage = '播放被中止'
            break
          case MediaError.MEDIA_ERR_NETWORK:
            errorMessage = '网络错误，请检查文件路径'
            break
          case MediaError.MEDIA_ERR_DECODE:
            errorMessage = '音频解码失败，可能格式不支持'
            break
          case MediaError.MEDIA_ERR_SRC_NOT_SUPPORTED:
            errorMessage = '浏览器不支持此音频格式'
            break
          default:
            errorMessage = `播放错误: ${audio.value.error.message || '未知错误'}`
        }
      }
      
      ElMessage.error(errorMessage)
      isPlaying.value = false
    })
  }
  
  // 开始播放
  if (audio.value) {
    audio.value.play().then(() => {
      ElMessage.success('开始播放: ' + props.song.title)
    }).catch((error) => {
      console.error('播放启动失败:', error)
      ElMessage.error('播放失败: ' + error.message)
      isPlaying.value = false
    })
  }
}

const pauseSong = () => {
  // 暂停播放
  if (audio.value) {
    audio.value.pause()
    ElMessage.info('暂停播放: ' + props.song.title)
  }
}

const stopSong = () => {
  // 停止播放并清除资源
  if (audio.value) {
    audio.value.pause()
    audio.value.currentTime = 0
    audio.value = null
  }
  
  // 清除播放进度定时器
  if (playbackInterval.value) {
    clearInterval(playbackInterval.value)
    playbackInterval.value = null
  }
  
  // 重置状态
  isPlaying.value = false
  currentTime.value = 0
  duration.value = 0
  currentLyricIndex.value = 0
}

const handlePrevious = () => {
  ElMessage.info('播放上一首')
}

const handleNext = () => {
  ElMessage.info('播放下一首')
}

const handleProgressInput = (value: number) => {
  // 拖动开始，设置拖动标志
  isDragging.value = true
  // 更新显示的当前时间
  currentTime.value = value
}

const handleProgressChange = (value: number) => {
  // 拖动结束，清除拖动标志
  isDragging.value = false
  // 设置音频播放进度
  if (audio.value && duration.value > 0) {
    // 确保进度值在有效范围内
    const safeTime = Math.max(0, Math.min(value, duration.value))
    currentTime.value = safeTime
    audio.value.currentTime = safeTime
    
    // 更新歌词索引
    updateLyricIndex(safeTime)
    
    // 如果当前是暂停状态，自动开始播放
    if (!isPlaying.value) {
      isPlaying.value = true
      audio.value.play().catch((error) => {
        ElMessage.error('播放失败: ' + error.message)
        isPlaying.value = false
      })
    }
    
    // ElMessage.info('跳转到: ' + formatTime(safeTime))
  }
}

const handleLike = () => {
  isLiked.value = !isLiked.value
  ElMessage.success(isLiked.value ? '点赞成功' : '取消点赞')
}

const handleComment = () => {
  // 滚动到评论区域
  const commentsContainer = document.querySelector('.comments-container')
  if (commentsContainer) {
    commentsContainer.scrollIntoView({ behavior: 'smooth' })
  }
}

const submitComment = () => {
  if (!newComment.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  
  const comment = {
    userName: '管理员',
    time: new Date().toLocaleString(),
    content: newComment.value,
    likes: 0,
    isLiked: false
  }
  
  comments.value.unshift(comment)
  newComment.value = ''
  ElMessage.success('评论发表成功')
}

const handleCommentLike = (comment: any) => {
  comment.isLiked = !comment.isLiked
  comment.likes += comment.isLiked ? 1 : -1
  ElMessage.success(comment.isLiked ? '点赞成功' : '取消点赞')
}

const formatTime = (seconds: number) => {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

const formatTooltip = (value: number) => {
  // 格式化鼠标悬停时显示的进度值为xx:xx格式
  return formatTime(value)
}



// 移除防抖函数，确保滚动操作及时执行
const scrollToCurrentLyric = () => {
  if (lyricsContent.value && currentLyricIndex.value >= 0) {
    const lines = lyricsContent.value.querySelectorAll('p')
    if (lines[currentLyricIndex.value]) {
      const lineTop = lines[currentLyricIndex.value].offsetTop
      const containerHeight = lyricsContent.value.clientHeight
      // 将滚动位置调整到容器上方1/4处，以适应歌词自滚动
      const targetScrollTop = lineTop - containerHeight / 4 - 600
      
      // 计算最大滚动高度
      const maxScrollTop = lyricsContent.value.scrollHeight - containerHeight
      
      // 确保滚动位置在有效范围内
      const finalScrollTop = Math.max(0, Math.min(targetScrollTop, maxScrollTop))
      
      // 执行滚动操作
      lyricsContent.value.scrollTop = finalScrollTop
    }
  }
}

const updateLyricIndex = (currentTime: number) => {
  if (parsedLyrics.value.length === 0) return
  
  // 考虑歌词偏移量
  const adjustedTime = currentTime + lyricsOffset.value
  
  // 找到当前时间对应的歌词索引
  let newIndex = 0
  
  // 当时间为负数时，从第一句开始
  if (adjustedTime < 0) {
    newIndex = 0
  } else {
    // 找到最后一个时间小于等于当前时间的歌词
    for (let i = 0; i < parsedLyrics.value.length; i++) {
      if (parsedLyrics.value[i].time <= adjustedTime) {
        newIndex = i
      } else {
        break
      }
    }
  }
  
  // 只有当索引发生变化时才更新和滚动
  if (newIndex !== currentLyricIndex.value) {
    currentLyricIndex.value = newIndex
    scrollToCurrentLyric()
  }
}

const adjustLyricsOffset = (delta: number) => {
  lyricsOffset.value += delta
  // 重新计算当前歌词索引
  updateLyricIndex(currentTime.value)
  // 保存偏移量到数据库
  saveLyricsOffset()
  ElMessage.info(`歌词偏移调整为: ${lyricsOffset.value}s`)
}

const handleOffsetChange = () => {
  // 重新计算当前歌词索引
  updateLyricIndex(currentTime.value)
  // 保存偏移量到数据库
  saveLyricsOffset()
  ElMessage.info(`歌词偏移调整为: ${lyricsOffset.value}s`)
}

const saveLyricsOffset = async () => {
  if (!props.song.id) return
  
  try {
    await request.put('/lyrics/update-offset', {
      songId: props.song.id,
      lyricsOffset: lyricsOffset.value
    })
    console.log('歌词偏移量保存成功')
  } catch (error) {
    console.error('保存歌词偏移量失败:', error)
    ElMessage.error('保存歌词偏移量失败')
  }
}

const loadLyrics = async () => {
  if (!props.song.id) return
  
  try {
    const response = await request.get(`/lyrics/song/${props.song.id}`)
    if (response && response.content) {
      // 解析歌词内容（兼容真实换行和字符串中的 \n）
      const normalizedContent = String(response.content)
        .replace(/\\r\\n/g, '\n')
        .replace(/\\n/g, '\n')
      const lines = normalizedContent.split('\n').filter((line: string) => line.trim())
      lyrics.value = lines
      
      // 从响应中获取歌词偏移量
      if (response.lyricsOffset !== undefined) {
        lyricsOffset.value = response.lyricsOffset
      }
      
      // 解析带时间的歌词
      parsedLyrics.value = []
      const timeRegex = /\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)/
      
      lines.forEach(line => {
        const match = line.match(timeRegex)
        if (match) {
          const minutes = parseInt(match[1], 10)
          const seconds = parseInt(match[2], 10)
          const millisecondsStr = match[3]
          const milliseconds = millisecondsStr.length === 2 
            ? parseInt(millisecondsStr, 10) * 10 
            : parseInt(millisecondsStr, 10)
          const time = minutes * 60 + seconds + milliseconds / 1000
          const text = match[4] ? match[4].trim() : ''
          parsedLyrics.value.push({ time, text })
        }
      })
      
      // 如果没有解析出带时间的歌词，使用默认格式
      if (parsedLyrics.value.length === 0) {
        parsedLyrics.value = lines.map((line, index) => ({
          time: index * 5, // 每5秒一行
          text: line
        }))
      }
    } else {
      lyrics.value = [
        '暂无歌词',
        '演唱：' + props.song.artistName
      ]
      parsedLyrics.value = lyrics.value.map((line, index) => ({
        time: index * 5,
        text: line
      }))
    }
  } catch (error) {
    console.error('获取歌词失败:', error)
    lyrics.value = [
      '暂无歌词',
      '演唱：' + props.song.artistName
    ]
    parsedLyrics.value = lyrics.value.map((line, index) => ({
      time: index * 5,
      text: line
    }))
  }
  
  // 歌词加载完成后，根据当前播放时间计算初始的歌词索引
  updateLyricIndex(currentTime.value)
}

const loadComments = () => {
  // 这里应该从API获取评论数据
  // 模拟评论数据
  comments.value = [
    {
      userName: '用户1',
      time: '2026-02-26 10:00',
      content: '这首歌很好听！',
      likes: 5,
      isLiked: false
    },
    {
      userName: '用户2',
      time: '2026-02-26 09:30',
      content: '歌词写得很棒',
      likes: 3,
      isLiked: true
    }
  ]
}

// Watch
watch(() => props.visible, async (newVal) => {
  if (newVal && props.song.id) {
    // 当弹窗打开时，加载歌曲数据
    await loadLyrics()
    loadComments()
    // 重置状态
    stopSong()
    // 开始播放
    isPlaying.value = true
    playSong()
  } else {
    // 当弹窗关闭时，停止播放并清除资源
    stopSong()
  }
})

// Lifecycle
onMounted(async () => {
  if (props.visible && props.song.id) {
    await loadLyrics()
    loadComments()
  }
})
</script>

<style scoped>
.song-player-container {
  padding: 20px 0;
}

.player-header {
  display: flex;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eaeaea;
}

.album-cover {
  flex-shrink: 0;
  margin-right: 30px;
}

.cover-image {
  width: 200px;
  height: 200px;
  object-fit: cover;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.song-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.song-title {
  font-size: 24px;
  font-weight: bold;
  margin: 0 0 10px 0;
  color: #303133;
}

.song-artist {
  font-size: 16px;
  margin: 0 0 5px 0;
  color: #606266;
}

.song-album {
  font-size: 14px;
  margin: 0 0 20px 0;
  color: #909399;
}

.song-actions {
  margin-top: 20px;
}

.player-controls {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 20px;
}

.player-controls .el-button {
  margin: 0 10px;
}

.progress-container {
  margin-bottom: 30px;
}

.time-display {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.lyrics-container {
  margin-bottom: 30px;
  padding: 20px;
  background-color: #f9f9f9;
  border-radius: 8px;
}

.lyrics-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 15px;
  color: #303133;
}

.lyrics-content {
  height: 200px;
  overflow-y: auto;
  text-align: center;
}

.lyrics-content p {
  margin: 10px 0;
  color: #606266;
  transition: all 0.3s ease;
}

.lyrics-content .current-line {
  color: #409EFF;
  font-weight: bold;
  /* 移除transform: scale，避免容器高度变化导致滚动条跳动 */
}

.comments-container {
  padding: 20px;
  background-color: #f9f9f9;
  border-radius: 8px;
}

.comments-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 15px;
  color: #303133;
}

.comment-input {
  margin-bottom: 20px;
}

.comment-input .el-button {
  margin-top: 10px;
  float: right;
}

.comments-list {
  clear: both;
}

.comment-item {
  display: flex;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eaeaea;
}

.comment-item:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.comment-avatar {
  flex-shrink: 0;
  margin-right: 15px;
}

.comment-content {
  flex: 1;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.comment-user {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
}

.comment-time {
  font-size: 12px;
  color: #909399;
}

.comment-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
  margin-bottom: 10px;
}

.comment-actions {
  display: flex;
  justify-content: flex-start;
}

.comment-actions .el-button {
  padding: 0;
  min-width: auto;
  color: #909399;
}

/* 滚动条样式 */
.lyrics-content::-webkit-scrollbar {
  width: 6px;
}

.lyrics-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.lyrics-content::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.lyrics-content::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>