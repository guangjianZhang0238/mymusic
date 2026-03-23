<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getAlbumPageApi, getArtistPageApi, getSearchSuggestionsApi, getSongPageApi } from '@/api/music'
import { createFeedbackApi } from '@/api/social'
import StateBlock from '@/components/StateBlock.vue'
import { ElMessage } from 'element-plus'
import { normalizeImageUrl } from '@/utils/image'

const router = useRouter()
const keyword = ref('')
const suggestions = ref<any[]>([])
const songs = ref<any[]>([])
const albums = ref<any[]>([])
const artists = ref<any[]>([])
const loading = ref(false)
const suggesting = ref(false)
const error = ref('')

const allResultsCount = computed(() => songs.value.length + albums.value.length + artists.value.length)
const artistPreview = computed(() => artists.value.slice(0, 2))

const suggest = async () => {
  if (!keyword.value.trim()) {
    suggestions.value = []
    return
  }
  suggesting.value = true
  try {
    suggestions.value = await getSearchSuggestionsApi(keyword.value, 10)
  } catch {
    suggestions.value = []
  } finally {
    suggesting.value = false
  }
}

const applySuggestion = async (value: string) => {
  keyword.value = value
  await search()
}

const search = async () => {
  const kw = keyword.value.trim()
  if (!kw) return ElMessage.warning('请输入关键词')
  loading.value = true
  error.value = ''
  try {
    const doSearch = async (query: string) => {
      const [songPage, albumPage, artistPage] = await Promise.all([
        getSongPageApi(1, 30, query),
        getAlbumPageApi(1, 20, query),
        getArtistPageApi(1, 20, query)
      ])

      songs.value = songPage.records || []
      albums.value = albumPage.records || []
      artists.value = artistPage.records || []
    }

    await doSearch(kw)

    // 如果用户输入看起来像“拼音首拼”（纯字母），但分页搜索三类结果都为空，
    // 则用联想接口的第一个候选名再搜索一次，提升可用性。
    const looksLikeInitial = /^[a-zA-Z]+$/.test(kw)
    if (
      looksLikeInitial &&
      !songs.value.length &&
      !albums.value.length &&
      !artists.value.length
    ) {
      try {
        const suggestionsForInitial = await getSearchSuggestionsApi(kw, 5)
        const mappedName = suggestionsForInitial?.[0]?.name
        if (mappedName && mappedName !== kw) {
          keyword.value = mappedName
          await doSearch(mappedName)
        }
      } catch {
        // ignore fallback errors
      }
    }

    if (!songs.value.length && !albums.value.length && !artists.value.length) ElMessage.info('暂无匹配结果，可尝试换个关键词')
  } catch (e: any) {
    error.value = e?.message || '搜索失败'
  } finally {
    loading.value = false
  }
}

const report = async () => {
  if (!keyword.value.trim()) return ElMessage.warning('请先输入关键词')
  try {
    await createFeedbackApi({ type: 'SEARCH', content: `搜索不到: ${keyword.value}`, keyword: keyword.value, scene: 'web-search' })
    ElMessage.success('反馈已提交')
  } catch (e: any) {
    ElMessage.error(e?.message || '反馈提交失败')
  }
}

const suggestionTypeText = (type?: number) => {
  if (type === 2) return '歌手'
  if (type === 3) return '专辑'
  return '歌曲'
}
</script>

<template>
  <h2 class="page-title">搜索</h2>
  <el-card class="glow-card">
    <el-space wrap>
      <el-input
        v-model="keyword"
        placeholder="输入歌曲/专辑/歌手或拼音首字母"
        style="width: 460px"
        clearable
        :loading="suggesting"
        @input="suggest"
        @keyup.enter="search"
      />
      <el-button type="primary" :loading="loading" @click="search">搜索</el-button>
      <el-button @click="report">反馈缺歌</el-button>
    </el-space>

    <el-divider>搜索建议</el-divider>
    <StateBlock :loading="suggesting" :empty="!suggestions.length" empty-text="输入关键词后显示建议">
      <el-space wrap>
        <el-tag
          v-for="(s, idx) in suggestions"
          :key="idx"
          class="suggest-tag"
          effect="plain"
          @click="applySuggestion(s.name)"
        >
          {{ s.name }}（{{ suggestionTypeText(s.type) }}）
        </el-tag>
      </el-space>
    </StateBlock>
  </el-card>

  <el-card class="glow-card" style="margin-top: 16px">
    <template #header>搜索结果（共 {{ allResultsCount }} 条）</template>
    <StateBlock :loading="loading" :error="error" :empty="!allResultsCount" empty-text="暂无搜索结果">
      <div class="results-section">
        <div v-if="artistPreview.length" class="artist-preview">
          <div class="section-title">歌手（前2）</div>
          <el-space wrap size="large">
            <div
              v-for="a in artistPreview"
              :key="a.id"
              class="artist-item"
              @click="router.push(`/artist/${a.id}`)"
            >
              <el-avatar :src="normalizeImageUrl(a.avatar)" :size="52" />
              <div class="artist-name">{{ a.name }}</div>
            </div>
          </el-space>
        </div>

        <div class="section-divider" />

        <div class="section-title">歌曲</div>
        <StateBlock :empty="!songs.length" empty-text="暂无匹配歌曲">
          <el-table :data="songs" size="small">
            <el-table-column label="歌曲" min-width="200">
              <template #default="{ row }">
                {{ row.title || row.name }}
              </template>
            </el-table-column>
            <el-table-column prop="artistName" label="歌手" min-width="160" />
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button text type="primary" @click="router.push(`/player/${row.id}`)">播放</el-button>
              </template>
            </el-table-column>
          </el-table>
        </StateBlock>

        <div v-if="albums.length" class="section-divider" />
        <div v-if="albums.length" class="section-title">专辑（{{ albums.length }}）</div>
        <StateBlock v-if="albums.length" :empty="!albums.length" empty-text="暂无匹配专辑">
          <el-table :data="albums" size="small">
            <el-table-column prop="name" label="专辑" min-width="220" />
            <el-table-column prop="artistName" label="歌手" min-width="160" />
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button text type="primary" @click="router.push(`/album/${row.id}`)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </StateBlock>
      </div>
    </StateBlock>
  </el-card>
</template>

<style scoped>
.suggest-tag {
  cursor: pointer;
}

.results-section {
  padding-top: 4px;
}

.artist-preview {
  margin-bottom: 6px;
}

.section-title {
  font-weight: 600;
  margin-bottom: 8px;
  color: #334155;
}

.artist-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px 10px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  background: rgba(255, 255, 255, 0.6);
  transition: all 0.15s;
}

.artist-item:hover {
  border-color: rgba(125, 211, 252, 0.8);
  box-shadow: 0 10px 22px rgba(125, 211, 252, 0.15);
}

.artist-name {
  color: #1f2937;
  font-size: 13px;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section-divider {
  height: 1px;
  background: rgba(148, 163, 184, 0.28);
  margin: 12px 0;
}
</style>
