<template>
  <div class="dashboard">
    <h2 class="page-title">仪表盘</h2>
    <div class="stats-container">
      <el-card class="stat-card">
        <div class="stat-item">
          <div class="stat-value">{{ stats.songCount || 0 }}</div>
          <div class="stat-label">歌曲总数</div>
        </div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-item">
          <div class="stat-value">{{ stats.albumCount || 0 }}</div>
          <div class="stat-label">专辑总数</div>
        </div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-item">
          <div class="stat-value">{{ stats.artistCount || 0 }}</div>
          <div class="stat-label">歌手总数</div>
        </div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-item">
          <div class="stat-value">{{ stats.totalPlayCount || 0 }}</div>
          <div class="stat-label">播放次数</div>
        </div>
      </el-card>
    </div>
    
    <div class="ranking-container">
      <h3 class="section-title">播放量排行榜</h3>
      <el-card>
        <el-table :data="rankingList" style="width: 100%">
          <el-table-column label="排名" width="80">
            <template #default="{ $index }">
              <span class="rank-number">{{ $index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="歌曲名称">
            <template #default="{ row }">
              <div class="song-info">
                <span class="song-title">{{ row.title }}</span>
                <span class="song-artist">{{ row.artistName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="播放量" width="120">
            <template #default="{ row }">
              <span class="play-count">{{ row.playCount }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as dashboardApi from '@/api/dashboard'

const stats = ref({
  songCount: 0,
  albumCount: 0,
  artistCount: 0,
  totalPlayCount: 0
})

const rankingList = ref<any[]>([])

const loadStats = async () => {
  try {
    const response = await dashboardApi.getDashboardStats()
    stats.value = response
  } catch (error: any) {
    ElMessage.error(error.message || '获取仪表盘统计数据失败')
  }
}

const loadRanking = async () => {
  try {
    const response = await dashboardApi.getPlayCountRanking(10)
    rankingList.value = response
  } catch (error: any) {
    ElMessage.error(error.message || '获取播放量排行榜失败')
  }
}

onMounted(() => {
  loadStats()
  loadRanking()
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 20px;
  color: #303133;
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  margin: 30px 0 15px 0;
  color: #303133;
}

.stats-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
}

.stat-card {
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.stat-item {
  text-align: center;
  padding: 20px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #606266;
}

.ranking-container {
  margin-top: 30px;
}

.rank-number {
  display: inline-block;
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  border-radius: 4px;
  background-color: #f0f9eb;
  color: #67c23a;
  font-weight: bold;
}

.song-info {
  display: flex;
  flex-direction: column;
}

.song-title {
  font-weight: bold;
  margin-bottom: 4px;
}

.song-artist {
  font-size: 12px;
  color: #909399;
}

.play-count {
  font-weight: bold;
  color: #e6a23c;
}
</style>
