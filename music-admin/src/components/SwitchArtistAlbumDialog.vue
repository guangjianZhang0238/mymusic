<template>
  <el-dialog
    v-model="visibleProxy"
    :title="title"
    width="520px"
    destroy-on-close
  >
    <el-form label-width="90px">
      <el-form-item label="歌手" required>
        <el-autocomplete
          v-model="artistName"
          :fetch-suggestions="searchArtists"
          placeholder="请输入或选择歌手（Enter 可创建/选中）"
          clearable
          style="width: 100%"
          @select="handleArtistSelect"
          @keyup.enter="handleArtistEnter"
        >
          <template #default="{ item }">
            <div class="artist-suggestion">
              <span class="artist-name">{{ item.name }}</span>
              <span class="artist-stats">{{ item.albumCount }}专辑 {{ item.songCount }}歌曲</span>
            </div>
          </template>
        </el-autocomplete>
      </el-form-item>

      <el-form-item v-if="showAlbum" label="专辑">
        <el-autocomplete
          v-model="albumName"
          :fetch-suggestions="searchAlbums"
          placeholder="请输入或选择专辑（可不填：默认专辑）"
          clearable
          style="width: 100%"
          @select="handleAlbumSelect"
          @keyup.enter="handleAlbumEnter"
        >
          <template #default="{ item }">
            <div class="album-suggestion">
              <span class="album-name">{{ item.name }}</span>
            </div>
          </template>
        </el-autocomplete>
      </el-form-item>

      <div class="tip">
        <div>说明：</div>
        <div>1) 歌手必填；未匹配时按 Enter 会自动创建并选中。</div>
        <div v-if="showAlbum">2) 专辑可不填：后端会迁移到该歌手的默认专辑。</div>
      </div>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="visibleProxy = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleConfirm">
          {{ confirmText }}
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

type ArtistSuggestion = { id?: number; name: string; albumCount?: number; songCount?: number }
type AlbumSuggestion = { id?: number; name: string; artistId?: number }

const props = withDefaults(defineProps<{
  visible: boolean
  title?: string
  confirmText?: string
  showAlbum?: boolean
}>(), {
  title: '切换歌手',
  confirmText: '确定',
  showAlbum: true
})

const emit = defineEmits<{
  (e: 'update:visible', v: boolean): void
  (e: 'confirm', payload: { artistId: number; artistName: string; albumName?: string | null }): void
}>()

const visibleProxy = computed({
  get: () => props.visible,
  set: (v: boolean) => emit('update:visible', v)
})

const submitting = ref(false)

const artistId = ref<number | null>(null)
const artistName = ref('')
const albumName = ref('')

watch(
  () => props.visible,
  (v) => {
    if (v) return
    // dialog close reset
    submitting.value = false
    artistId.value = null
    artistName.value = ''
    albumName.value = ''
  }
)

const searchArtists = async (keyword: string, callback: (results: ArtistSuggestion[]) => void) => {
  const k = keyword?.trim()
  if (!k) {
    callback([])
    return
  }
  try {
    const res = await request.get('/music-metadata/artists/search', {
      params: { keyword: k, limit: 10 }
    })
    callback(res || [])
  } catch {
    callback([])
  }
}

const searchAlbums = async (keyword: string, callback: (results: AlbumSuggestion[]) => void) => {
  const k = keyword?.trim()
  if (!k || !artistId.value) {
    callback([])
    return
  }
  try {
    const res = await request.get('/music-metadata/albums/search', {
      params: { artistId: artistId.value, keyword: k, limit: 10 }
    })
    callback(res || [])
  } catch {
    callback([])
  }
}

const ensureArtistSelected = async () => {
  const name = artistName.value.trim()
  if (!name) {
    ElMessage.warning('请先选择或输入歌手')
    return null
  }
  if (artistId.value) return { id: artistId.value, name }

  // Enter: auto-match/create
  try {
    const res = await request.post('/music-metadata/artists/auto-match', null, {
      params: { artistName: name }
    })
    if (!res?.id) throw new Error('歌手创建失败')
    artistId.value = res.id
    artistName.value = res.name || name
    return { id: res.id as number, name: (res.name || name) as string }
  } catch (e: any) {
    ElMessage.error(e?.message || '创建/匹配歌手失败')
    return null
  }
}

const handleArtistSelect = (item: ArtistSuggestion) => {
  artistId.value = item.id ?? null
  artistName.value = item.name
  albumName.value = ''
}

const handleArtistEnter = async () => {
  await ensureArtistSelected()
}

const handleAlbumSelect = (item: AlbumSuggestion) => {
  albumName.value = item.name
}

const handleAlbumEnter = async () => {
  // 专辑不强制创建；最终由后端 batch-switch 解析/创建
  albumName.value = albumName.value.trim()
}

const handleConfirm = async () => {
  submitting.value = true
  try {
    const artist = await ensureArtistSelected()
    if (!artist) return

    const album = props.showAlbum ? (albumName.value.trim() || null) : null
    emit('confirm', {
      artistId: artist.id,
      artistName: artist.name,
      albumName: album
    })
    visibleProxy.value = false
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.artist-suggestion,
.album-suggestion {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.artist-stats {
  font-size: 12px;
  color: #909399;
}

.tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}
</style>

