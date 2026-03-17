import request from './request'
import type { SongVO, PageResult } from './types'

// 分页查询歌曲列表
export const getSongList = (params: {
  current: number
  size: number
  keyword?: string
  artistId?: number
  albumId?: number
  format?: string
  status?: number
  hasLyrics?: number
}) => {
  return request.get<PageResult<SongVO>>('/song/page', { params })
}

// 获取歌曲详情
export const getSongDetail = (id: number) => {
  return request.get<SongVO>(`/song/${id}`)
}

// 创建歌曲
export const createSong = (data: any) => {
  return request.post<number>('/song', data)
}

// 更新歌曲
export const updateSong = (data: any) => {
  return request.put('/song', data)
}

// 删除歌曲
export const deleteSong = (id: number) => {
  return request.delete(`/song/${id}`)
}

// 批量删除歌曲（含关系表清理，由后端保证）
export const batchDeleteSongs = (songIds: number[]) => {
  return request.post<any>('/song/batch-delete', { songIds })
}

// 批量切换歌手（专辑可不填：默认专辑）
export const batchSwitchArtist = (data: {
  songIds: number[]
  targetArtistId: number
  targetArtistName: string
  targetAlbumName?: string | null
}) => {
  return request.post<any>('/song/batch-switch-artist', data)
}

// 获取热门歌曲
export const getHotSongs = () => {
  return request.get<SongVO[]>('/song/hot')
}

// 自动匹配歌词
export const autoMatchLyrics = (songId: number) => {
  return request.post<any>(`/song/${songId}/auto-match-lyrics`)
}