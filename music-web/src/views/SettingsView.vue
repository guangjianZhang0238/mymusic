<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { batchSaveUserSettingsApi, getUserSettingsApi, saveUserSettingApi, type UserSettingItem } from '@/api/settings'
import { emitUserSettingsUpdated } from '@/utils/settingsSync'
import StateBlock from '@/components/StateBlock.vue'
import { ElMessage } from 'element-plus'

type SettingType = 'string' | 'number' | 'boolean'

interface FriendlySettingDef {
  key: string
  label: string
  description: string
  type: SettingType
  defaultValue: string
  category: 'player' | 'lyrics' | 'app'
  control: 'switch' | 'slider' | 'select'
  sliderMin?: number
  sliderMax?: number
  sliderStep?: number
  options?: Array<{ label: string; value: string }>
}

interface ManualDraft {
  key: string
  value: string
  type: SettingType
  description: string
}

const settings = ref<UserSettingItem[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const showAdvanced = ref(false)

const friendlyDefs: FriendlySettingDef[] = [
  {
    key: 'player.volume',
    label: '默认音量',
    description: '控制新打开页面时播放器默认音量大小。',
    type: 'number',
    defaultValue: '70',
    category: 'player',
    control: 'slider',
    sliderMin: 0,
    sliderMax: 100,
    sliderStep: 1
  },
  {
    key: 'player.playMode',
    label: '默认播放模式',
    description: '选择进入播放器时默认使用的播放模式。',
    type: 'string',
    defaultValue: 'loop',
    category: 'player',
    control: 'select',
    options: [
      { label: '列表循环', value: 'loop' },
      { label: '顺序播放', value: 'order' },
      { label: '随机播放', value: 'random' },
      { label: '单曲循环', value: 'single' }
    ]
  },
  {
    key: 'player.autoPlayOnOpen',
    label: '打开播放器自动播放',
    description: '进入播放器页面后，自动开始播放当前歌曲。',
    type: 'boolean',
    defaultValue: 'true',
    category: 'player',
    control: 'switch'
  },
  {
    key: 'lyrics.autoScroll',
    label: '歌词自动滚动',
    description: '播放时歌词自动跟随当前进度滚动。',
    type: 'boolean',
    defaultValue: 'true',
    category: 'lyrics',
    control: 'switch'
  },
  {
    key: 'lyrics.fontSize',
    label: '歌词字号',
    description: '调整歌词显示大小，阅读更舒适。',
    type: 'number',
    defaultValue: '18',
    category: 'lyrics',
    control: 'slider',
    sliderMin: 12,
    sliderMax: 34,
    sliderStep: 1
  },
  {
    key: 'app.recommendRefreshCount',
    label: '首页推荐刷新数量',
    description: '每次刷新首页推荐时，展示的热门歌曲数量。',
    type: 'number',
    defaultValue: '15',
    category: 'app',
    control: 'slider',
    sliderMin: 5,
    sliderMax: 30,
    sliderStep: 1
  }
]

const friendlyDraft = ref<Record<string, string>>({})

const manualDraft = ref<ManualDraft>({
  key: '',
  value: '',
  type: 'string',
  description: ''
})

const settingMap = computed(() => {
  const map = new Map<string, UserSettingItem>()
  settings.value.forEach((item) => {
    map.set(String(item.settingKey || '').trim(), item)
  })
  return map
})

const categoryCards = computed(() => {
  const labelMap: Record<FriendlySettingDef['category'], string> = {
    player: '播放设置',
    lyrics: '歌词设置',
    app: '首页与推荐'
  }

  const grouped: Record<FriendlySettingDef['category'], FriendlySettingDef[]> = {
    player: [],
    lyrics: [],
    app: []
  }

  friendlyDefs.forEach((def) => grouped[def.category].push(def))

  return (Object.keys(grouped) as FriendlySettingDef['category'][]).map((category) => ({
    category,
    title: labelMap[category],
    items: grouped[category]
  }))
})

const getSettingValue = (def: FriendlySettingDef) => {
  const raw = settingMap.value.get(def.key)?.settingValue
  if (raw === undefined || raw === null || String(raw).trim() === '') {
    return def.defaultValue
  }
  return String(raw)
}

const syncFriendlyDraftFromServer = () => {
  const next: Record<string, string> = {}
  friendlyDefs.forEach((def) => {
    next[def.key] = getSettingValue(def)
  })
  friendlyDraft.value = next
}

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    settings.value = await getUserSettingsApi()
    syncFriendlyDraftFromServer()
  } catch (e: any) {
    error.value = e?.message || '设置加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)

const boolFromString = (value: string) => String(value).toLowerCase() === 'true'
const boolToString = (value: boolean) => (value ? 'true' : 'false')

const getFriendlySwitchValue = (key: string) => boolFromString(friendlyDraft.value[key] ?? 'false')
const setFriendlySwitchValue = (key: string, value: boolean) => {
  friendlyDraft.value[key] = boolToString(value)
}

const getFriendlySliderValue = (key: string, fallback = 0) => {
  const n = Number(friendlyDraft.value[key])
  if (!Number.isFinite(n)) return fallback
  return n
}
const setFriendlySliderValue = (key: string, value: number) => {
  friendlyDraft.value[key] = String(value)
}

const hasFriendlyChanged = computed(() =>
  friendlyDefs.some((def) => String(friendlyDraft.value[def.key] ?? def.defaultValue) !== String(getSettingValue(def)))
)

const saveFriendlySettings = async () => {
  if (!hasFriendlyChanged.value) {
    ElMessage.info('未检测到变更')
    return
  }

  const changedItems: UserSettingItem[] = friendlyDefs
    .map((def) => {
      const nextVal = String(friendlyDraft.value[def.key] ?? def.defaultValue)
      const currentVal = String(getSettingValue(def))
      if (nextVal === currentVal) return null
      return {
        settingKey: def.key,
        settingValue: nextVal,
        settingType: def.type,
        description: def.description
      }
    })
    .filter(Boolean) as UserSettingItem[]

  if (!changedItems.length) {
    ElMessage.info('未检测到变更')
    return
  }

  saving.value = true
  try {
    await batchSaveUserSettingsApi(changedItems)
    emitUserSettingsUpdated(changedItems)
    await load()
    ElMessage.success('设置已保存')
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const resetFriendlySettings = () => {
  syncFriendlyDraftFromServer()
  ElMessage.success('已恢复为当前已保存设置')
}

const restoreDefaultFriendlySettings = async () => {
  const defaultItems: UserSettingItem[] = friendlyDefs.map((def) => ({
    settingKey: def.key,
    settingValue: def.defaultValue,
    settingType: def.type,
    description: def.description
  }))

  saving.value = true
  try {
    await batchSaveUserSettingsApi(defaultItems)
    emitUserSettingsUpdated(defaultItems)
    await load()
    ElMessage.success('已恢复推荐默认设置')
  } catch (e: any) {
    ElMessage.error(e?.message || '恢复默认设置失败')
  } finally {
    saving.value = false
  }
}

const saveManualSetting = async () => {
  const key = manualDraft.value.key.trim()
  if (!key) {
    ElMessage.warning('请先填写设置名称')
    return
  }
  saving.value = true
  try {
    const item: UserSettingItem = {
      settingKey: key,
      settingValue: manualDraft.value.value,
      settingType: manualDraft.value.type,
      description: manualDraft.value.description
    }
    await saveUserSettingApi(key, manualDraft.value.value, manualDraft.value.type, manualDraft.value.description)
    emitUserSettingsUpdated([item])
    manualDraft.value = { key: '', value: '', type: 'string', description: '' }
    await load()
    ElMessage.success('高级设置保存成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const readableValue = (item: UserSettingItem) => {
  if (item.settingType === 'boolean') {
    return boolFromString(item.settingValue) ? '开' : '关'
  }
  return item.settingValue
}
</script>

<template>
  <h2 class="page-title">设置</h2>

  <StateBlock :loading="loading" :error="error">
    <div class="settings-page">
      

      <div class="friendly-grid">
        <el-card v-for="card in categoryCards" :key="card.category" class="glow-card settings-card">
          <template #header>
            <div class="card-title">{{ card.title }}</div>
          </template>

          <div class="setting-list">
            <div v-for="def in card.items" :key="def.key" class="setting-item">
              <div class="setting-meta">
                <div class="setting-label">{{ def.label }}</div>
                <div class="setting-desc">{{ def.description }}</div>
              </div>

              <div class="setting-control">
                <el-switch
                  v-if="def.control === 'switch'"
                  :model-value="getFriendlySwitchValue(def.key)"
                  @update:model-value="(val) => setFriendlySwitchValue(def.key, Boolean(val))"
                />

                <div v-else-if="def.control === 'slider'" class="slider-control">
                  <el-slider
                    :min="def.sliderMin || 0"
                    :max="def.sliderMax || 100"
                    :step="def.sliderStep || 1"
                    :model-value="getFriendlySliderValue(def.key, Number(def.defaultValue || 0))"
                    @update:model-value="(val) => setFriendlySliderValue(def.key, Number(val))"
                  />
                  <span class="slider-value">{{ getFriendlySliderValue(def.key, Number(def.defaultValue || 0)) }}</span>
                </div>

                <el-select
                  v-else-if="def.control === 'select'"
                  :model-value="friendlyDraft[def.key]"
                  style="width: 220px"
                  @update:model-value="(val) => (friendlyDraft[def.key] = String(val))"
                >
                  <el-option
                    v-for="option in def.options || []"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <div class="action-bar glow-card">
        <el-button :loading="saving" :disabled="!hasFriendlyChanged" type="primary" @click="saveFriendlySettings">
          保存我的设置
        </el-button>
        <el-button :disabled="!hasFriendlyChanged" @click="resetFriendlySettings">撤销未保存修改</el-button>
        <el-popconfirm title="确认恢复所有常用设置为推荐默认值吗？" @confirm="restoreDefaultFriendlySettings">
          <template #reference>
            <el-button :loading="saving" plain>恢复默认设置</el-button>
          </template>
        </el-popconfirm>
        <span class="action-tip">{{ hasFriendlyChanged ? '你有未保存的修改' : '当前设置已保存' }}</span>
      </div>

      <el-divider content-position="left">高级设置（可选）</el-divider>

      <el-card class="glow-card">
        <div class="advanced-header">
          <div>
            <div class="card-title">自定义高级设置</div>
            <div class="card-subtitle">给高级用户预留的扩展能力，不影响普通使用。</div>
          </div>
          <el-switch v-model="showAdvanced" inline-prompt active-text="展开" inactive-text="收起" />
        </div>

        <div v-show="showAdvanced" class="advanced-content">
          <el-row :gutter="12">
            <el-col :md="12" :sm="24" :xs="24">
              <el-input v-model="manualDraft.key" placeholder="设置名称（例如 player.crossfade）" />
            </el-col>
            <el-col :md="12" :sm="24" :xs="24">
              <el-input v-model="manualDraft.value" placeholder="设置值" />
            </el-col>
          </el-row>

          <el-row :gutter="12" style="margin-top: 12px">
            <el-col :md="12" :sm="24" :xs="24">
              <el-select v-model="manualDraft.type" style="width: 100%">
                <el-option label="文本" value="string" />
                <el-option label="数字" value="number" />
                <el-option label="开关（true/false）" value="boolean" />
              </el-select>
            </el-col>
            <el-col :md="12" :sm="24" :xs="24">
              <el-input v-model="manualDraft.description" placeholder="说明（可选）" />
            </el-col>
          </el-row>

          <el-button :loading="saving" type="primary" style="margin-top: 12px" @click="saveManualSetting">保存高级设置</el-button>

          <el-divider content-position="left">当前全部设置（{{ settings.length }}）</el-divider>

          <el-table :data="settings" border>
            <el-table-column prop="settingKey" label="设置名称" min-width="220" />
            <el-table-column label="设置值" min-width="160">
              <template #default="{ row }">
                {{ readableValue(row) }}
              </template>
            </el-table-column>
            <el-table-column prop="settingType" label="类型" width="120" />
            <el-table-column prop="description" label="说明" min-width="220" />
          </el-table>
        </div>
      </el-card>
    </div>
  </StateBlock>
</template>

<style scoped>
.settings-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.friendly-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.settings-card {
  border-radius: 14px;
}

.card-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.card-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
}

.setting-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.setting-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  padding: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #f8fafc;
}

.setting-meta {
  flex: 1;
  min-width: 0;
}

.setting-label {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.setting-desc {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.4;
}

.setting-control {
  flex: 0 0 auto;
  min-width: 180px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.slider-control {
  width: 220px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.slider-value {
  width: 34px;
  text-align: right;
  font-size: 12px;
  color: #0f172a;
  font-weight: 600;
}

.action-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  border-radius: 12px;
  padding: 12px;
}

.action-tip {
  margin-left: auto;
  font-size: 12px;
  color: #64748b;
}

.advanced-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.advanced-content {
  margin-top: 10px;
}

@media (max-width: 1024px) {
  .friendly-grid {
    grid-template-columns: 1fr;
  }

  .setting-item {
    flex-direction: column;
  }

  .setting-control,
  .slider-control {
    width: 100%;
    justify-content: flex-start;
  }

  .action-bar {
    flex-wrap: wrap;
  }

  .action-tip {
    margin-left: 0;
  }
}
</style>
