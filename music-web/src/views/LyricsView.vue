<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getLyricsApi, getSongsByIdsApi } from '@/api/music'
import { getUserSettingsApi } from '@/api/settings'
import { USER_SETTINGS_UPDATED_EVENT } from '@/utils/settingsSync'
import { normalizeLyrics } from '@/utils/lyrics'
import StateBlock from '@/components/StateBlock.vue'
import { getDisplaySongTitle } from '@/utils/songTitle'

const route = useRoute()
const songId = Number(route.params.songId)
const title = ref('')
const artist = ref('')
const lines = ref<any[]>([])
const loading = ref(false)
const error = ref('')
const lyricFontSize = ref(18)

const lyricLineStyle = computed(() => ({
  fontSize: `${lyricFontSize.value}px`
}))

const applyLyricFontSizeSetting = (value: unknown) => {
  const raw = Number(value)
  lyricFontSize.value = Math.max(12, Math.min(34, Number.isFinite(raw) ? raw : 18))
}

const loadLyricUserSettings = async () => {
  try {
    const settings = await getUserSettingsApi()
    const row = (settings || []).find((item: any) => item.settingKey === 'lyrics.fontSize')
    applyLyricFontSizeSetting(row?.settingValue ?? 18)
  } catch {
    applyLyricFontSizeSetting(18)
  }
}

const onUserSettingsUpdated = (event: Event) => {
  const customEvent = event as CustomEvent<{ items?: Array<{ settingKey?: string; settingValue?: string }> }>
  const items = customEvent?.detail?.items || []
  const matched = items.find((item) => item?.settingKey === 'lyrics.fontSize')
  if (!matched) return
  applyLyricFontSizeSetting(matched.settingValue)
}

onMounted(async () => {
  window.addEventListener(USER_SETTINGS_UPDATED_EVENT, onUserSettingsUpdated as EventListener)
  loading.value = true
  error.value = ''
  try {
    const [songRows, lyric] = await Promise.all([getSongsByIdsApi([songId]), getLyricsApi(songId), loadLyricUserSettings()])
    const song = songRows[0]
    title.value = song ? getDisplaySongTitle(song) : ''
    artist.value = song?.artistName || song?.artistNames || ''
    lines.value = normalizeLyrics(lyric?.lines || []).map((item) => item.text)
  } catch (e: any) {
    error.value = e?.message || '歌词页加载失败'
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  window.removeEventListener(USER_SETTINGS_UPDATED_EVENT, onUserSettingsUpdated as EventListener)
})
</script>

<template>
  <h2 class="page-title">{{ title || '歌词详情' }}</h2>
  <p class="text-muted">{{ artist || '未知歌手' }}</p>
  <StateBlock :loading="loading" :error="error" :empty="!lines.length" empty-text="暂无歌词">
    <el-card class="glow-card">
      <div class="lyric-wrap">
        <p v-for="(line, idx) in lines" :key="idx" class="lyric-line" :style="lyricLineStyle">{{ line }}</p>
      </div>
    </el-card>
  </StateBlock>
</template>

<style scoped>
.lyric-wrap {
  max-height: 68vh;
  overflow: auto;
  padding-right: 8px;
}

.lyric-line {
  margin: 0;
  padding: 8px 0;
  border-bottom: 1px dashed rgba(148, 163, 184, 0.28);
  line-height: 1.7;
}
</style>
