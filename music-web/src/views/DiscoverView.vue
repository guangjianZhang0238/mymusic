<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getDailyRecommendApi, getHotSongsApi, getPersonalRecommendApi, getSceneRecommendApi } from '@/api/music'
import { usePlayerStore } from '@/stores/player'
import StateBlock from '@/components/StateBlock.vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const player = usePlayerStore()
const daily = ref<any[]>([])
const personal = ref<any[]>([])
const scene = ref<any[]>([])
const loading = ref(false)
const error = ref('')

onMounted(async () => {
  loading.value = true
  error.value = ''
  try {
    const [d, p, s, hot] = await Promise.all([getDailyRecommendApi(10), getPersonalRecommendApi(10), getSceneRecommendApi(10), getHotSongsApi()])
    daily.value = d
    personal.value = p.length ? p : hot
    scene.value = s
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
</script>

<template>
  <h2 class="page-title">发现</h2>
  <StateBlock :loading="loading" :error="error">
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card class="glow-card">
          <template #header>每日推荐</template>
          <StateBlock :empty="!daily.length">
            <div v-for="s in daily" :key="s.id"><el-button text @click="playIn(daily, s.id)">{{ s.name }}</el-button></div>
          </StateBlock>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="glow-card">
          <template #header>个性推荐</template>
          <StateBlock :empty="!personal.length">
            <div v-for="s in personal" :key="s.id"><el-button text @click="playIn(personal, s.id)">{{ s.name }}</el-button></div>
          </StateBlock>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="glow-card">
          <template #header>场景推荐</template>
          <StateBlock :empty="!scene.length">
            <div v-for="s in scene" :key="s.id"><el-button text @click="playIn(scene, s.id)">{{ s.name }}</el-button></div>
          </StateBlock>
        </el-card>
      </el-col>
    </el-row>
  </StateBlock>
</template>
