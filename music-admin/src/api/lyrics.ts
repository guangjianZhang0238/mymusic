import request from './request'
import type { LyricsVO, PageResult } from './types'

// 分页查询歌词列表
export const getLyricsList = (params: {
  current: number
  size: number
  keyword?: string
  songId?: number
  status?: number
}) => {
  return request.get<PageResult<LyricsVO>>('/lyrics/page', { params })
}

// 根据歌曲ID获取歌词
export const getLyricsBySongId = (songId: number) => {
  return request.get<LyricsVO>(`/lyrics/song/${songId}`)
}

// 创建歌词
export const createLyrics = (data: any) => {
  return request.post<number>('/lyrics', data)
}

// 更新歌词
export const updateLyrics = (data: any) => {
  return request.put('/lyrics', data)
}

// 删除歌词
export const deleteLyrics = (id: number) => {
  return request.delete(`/lyrics/${id}`)
}

// 上传歌词文件
export const uploadLyricsFile = (file: File, songId: number) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('songId', songId.toString())
  return request.post<any>('/lyrics/upload', formData)
}

// 自动同步歌词
export const autoSyncLyrics = () => {
  return request.post<any>('/lyrics/auto-sync')
}
