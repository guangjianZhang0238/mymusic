<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getDailyRecommendApi, getPersonalRecommendApi, getSceneRecommendApi } from '@/api/music'
import { usePlayerStore } from '@/stores/player'
import StateBlock from '@/components/StateBlock.vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const player = usePlayerStore()
const daily = ref<any[]>([])
const personal = ref<any[]>([])
const scene = ref<any[]>([])
const mix = ref<any[]>([])
const loading = ref(false)
const error = ref('')
const mode = ref<'daily' | 'personal' | 'scene' | 'mix'>('daily')
const mood = ref<'calm' | 'focus' | 'energy' | 'night'>('calm')

const buildMixList = (sources: any[][], limit = 12) => {
  const map = new Map<number, any>()
  sources.forEach((list) => {
    list.forEach((item) => {
      const id = Number(item?.id)
      if (!Number.isFinite(id) || id <= 0 || map.has(id)) return
      map.set(id, item)
    })
  })
  return Array.from(map.values()).slice(0, limit)
}

const moodMap = computed(() => {
  const base = {
    calm: { title: '轻松漫游', tip: '慢节奏，适合阅读或放松', seed: daily.value },
    focus: { title: '专注不打扰', tip: '节奏克制，适合工作', seed: personal.value },
    energy: { title: '动感补给', tip: '节奏强烈，适合运动', seed: mix.value },
    night: { title: '夜行电台', tip: '氛围感更强', seed: scene.value }
  }
  return base[mood.value]
})

const modeMap = computed(() => ({
  daily: { title: '今日惊喜', list: daily.value, hint: '每天更新 10 首专属歌单' },
  personal: { title: '懂你推荐', list: personal.value, hint: '根据你的播放习惯生成' },
  scene: { title: '场景声场', list: scene.value, hint: '旅行、咖啡馆、夜跑等场景' },
  mix: { title: '灵感混合', list: mix.value, hint: '从你的推荐里选出更特别的组合' }
}))

const nowList = computed(() => modeMap.value[mode.value].list)

onMounted(async () => {
  loading.value = true
  error.value = ''
  try {
    const [d, p, s] = await Promise.all([
      getDailyRecommendApi(10),
      getPersonalRecommendApi(10),
      getSceneRecommendApi(10)
    ])
    daily.value = d
    personal.value = p.length ? p : d
    scene.value = s
    mix.value = buildMixList([personal.value, daily.value, scene.value])
  } catch (e: any) {
    error.value = e?.message || '发现页加载失败'
  } finally {
    loading.value = false
  }
})

const playIn = async (list: any[], songId: number) => {
  try {
    const ids = list.map((item) => item.id)
    await player.setQueue(ids, Math.max(ids.indexOf(songId), 0))
    router.push(`/player/${songId}`)
  } catch (e: any) {
    ElMessage.error(e?.message || '播放失败')
  }
}

const playMood = async () => {
  const list = moodMap.value.seed || []
  if (!list.length) return
  await playIn(list, list[0].id)
}
</script>

<template>
  <div class="discover">
    <section class="hero">
      <div>
        <p class="hero-tag">今日发现</p>
        <h2 class="hero-title">一键进入你的音乐情绪实验室</h2>
        <p class="hero-sub">通过「情绪电台 + 灵感卡片」快速找到想听的歌</p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" size="large" @click="playMood">马上开启电台</el-button>
        <el-button size="large" @click="mode = 'mix'">换一组灵感</el-button>
      </div>
    </section>

    <StateBlock :loading="loading" :error="error">
      <section class="mood-section">
        <div class="section-title">
          <div>
            <h3>情绪电台</h3>
            <p>选择心情，智能拼接适合的歌曲</p>
          </div>
          <el-radio-group v-model="mood" size="small">
            <el-radio-button label="calm">轻松</el-radio-button>
            <el-radio-button label="focus">专注</el-radio-button>
            <el-radio-button label="energy">动感</el-radio-button>
            <el-radio-button label="night">夜晚</el-radio-button>
          </el-radio-group>
        </div>
        <div class="mood-card">
          <div>
            <h4>{{ moodMap.title }}</h4>
            <p>{{ moodMap.tip }}</p>
          </div>
          <div class="mood-actions">
            <el-button type="primary" @click="playMood">播放这个心情</el-button>
            <el-button text @click="mode = 'scene'">换成场景模式</el-button>
          </div>
        </div>
        <div class="song-grid">
          <StateBlock :empty="!moodMap.seed?.length">
            <button v-for="s in moodMap.seed" :key="s.id" class="song-chip" @click="playIn(moodMap.seed, s.id)">
              <span class="song-name">{{ s.name }}</span>
              <span class="song-meta">立即播放</span>
            </button>
          </StateBlock>
        </div>
      </section>

      <section class="mode-section">
        <div class="section-title">
          <div>
            <h3>灵感卡片</h3>
            <p>{{ modeMap[mode].hint }}</p>
          </div>
          <el-segmented v-model="mode" :options="[
            { label: '今日惊喜', value: 'daily' },
            { label: '懂你推荐', value: 'personal' },
            { label: '场景声场', value: 'scene' },
            { label: '灵感混合', value: 'mix' }
          ]" />
        </div>
        <div class="card-list">
          <StateBlock :empty="!nowList.length">
            <div v-for="s in nowList" :key="s.id" class="song-card">
              <div>
                <p class="card-title">{{ s.name }}</p>
                <p class="card-sub">点击加入队列继续听</p>
              </div>
              <el-button text @click="playIn(nowList, s.id)">播放</el-button>
            </div>
          </StateBlock>
        </div>
      </section>
    </StateBlock>
  </div>
</template>

<style scoped>
.discover {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-radius: 18px;
  background:
    radial-gradient(circle at 0% -60%, rgba(10, 132, 255, 0.16), transparent 58%),
    radial-gradient(circle at 100% -80%, rgba(250, 45, 72, 0.14), transparent 54%),
    rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.86);
  box-shadow: 0 12px 26px rgba(17, 17, 17, 0.08);
}

.hero-tag {
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #8e8e93;
  margin: 0 0 6px;
}

.hero-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0;
  color: #1d1d1f;
}

.hero-sub {
  margin: 8px 0 0;
  color: #6e6e73;
  font-size: 13px;
}

.hero-actions {
  display: flex;
  gap: 12px;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.section-title h3 {
  margin: 0 0 4px;
  font-size: 18px;
  color: #1d1d1f;
}

.section-title p {
  margin: 0;
  color: #6e6e73;
  font-size: 12px;
}

.mood-section,
.mode-section {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 18px;
  padding: 14px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 10px 22px rgba(17, 17, 17, 0.06);
}

.mood-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 14px;
  border: 1px solid rgba(17, 17, 17, 0.08);
  background: rgba(245, 245, 247, 0.9);
  margin-bottom: 12px;
}

.mood-card h4 {
  margin: 0 0 4px;
  font-size: 16px;
  color: #1d1d1f;
}

.mood-card p {
  margin: 0;
  color: #6e6e73;
  font-size: 12px;
}

.mood-actions {
  display: flex;
  gap: 12px;
}

.song-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.song-chip {
  border: 1px solid rgba(17, 17, 17, 0.08);
  border-radius: 12px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  background: rgba(255, 255, 255, 0.96);
  color: inherit;
  cursor: pointer;
  text-align: left;
}

.song-chip:hover {
  border-color: rgba(10, 132, 255, 0.4);
  box-shadow: 0 8px 16px rgba(10, 132, 255, 0.14);
}

.song-name {
  font-weight: 600;
  color: #1d1d1f;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
}

.song-meta {
  font-size: 12px;
  color: #8e8e93;
}

.card-list {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}

.song-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-radius: 12px;
  background: rgba(245, 245, 247, 0.9);
  border: 1px solid rgba(17, 17, 17, 0.08);
}

.card-title {
  margin: 0 0 2px;
  font-weight: 600;
  color: #1d1d1f;
}

.card-sub {
  margin: 0;
  font-size: 12px;
  color: #8e8e93;
}

@media (max-width: 960px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-actions {
    width: 100%;
  }

  .hero-actions :deep(.el-button) {
    flex: 1;
    min-width: 0;
  }

  .section-title {
    flex-direction: column;
    align-items: flex-start;
  }

  .section-title :deep(.el-radio-group),
  .section-title :deep(.el-segmented) {
    width: 100%;
  }

  .mood-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .mood-actions {
    width: 100%;
  }

  .mood-actions :deep(.el-button) {
    flex: 1;
    min-width: 0;
  }

  .song-grid {
    grid-template-columns: 1fr;
  }
}
</style>
