import request from './request'
import type { SongVO } from './types'

// 获取仪表盘统计数据
export const getDashboardStats = () => {
  return request.get<Record<string, number>>('/dashboard/stats')
}

// 获取播放量排行榜
export const getPlayCountRanking = (limit: number = 10) => {
  return request.get<SongVO[]>('/dashboard/ranking', { params: { limit } })
}
