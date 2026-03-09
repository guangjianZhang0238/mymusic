import request from './request'
import type { ArtistVO, PageResult } from './types'

// 分页查询艺术家列表
export const getArtistList = (params: {
  current: number
  size: number
  keyword?: string
  region?: string
  type?: number
  status?: number
}) => {
  return request.get<PageResult<ArtistVO>>('/artist/page', { params })
}

// 获取艺术家详情
export const getArtistDetail = (id: number) => {
  return request.get<ArtistVO>(`/artist/${id}`)
}

// 创建艺术家
export const createArtist = (data: any) => {
  return request.post<number>('/artist', data)
}

// 更新艺术家
export const updateArtist = (data: any) => {
  return request.put('/artist', data)
}

// 删除艺术家
export const deleteArtist = (id: number) => {
  return request.delete(`/artist/${id}`)
}

// 扫描歌手
export const scanArtists = () => {
  return request.post('/artist/scan')
}