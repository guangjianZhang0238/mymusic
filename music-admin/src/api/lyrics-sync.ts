import request from './request'

// 启动异步歌词同步
export const startLyricsSync = () => {
  return request.post<number>('/lyrics-sync/start')
}

// 获取同步进度
export const getLyricsSyncProgress = (taskId: number) => {
  return request.get<any>(`/lyrics-sync/progress/${taskId}`)
}

// 取消同步任务
export const cancelLyricsSync = (taskId: number) => {
  return request.post<void>(`/lyrics-sync/cancel/${taskId}`)
}