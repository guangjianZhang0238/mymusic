import request from './request'
import type { AlbumVO, PageResult } from './types'

// 分页查询专辑列表
export const getAlbumList = (params: {
  current: number
  size: number
  keyword?: string
  artistId?: number
  albumType?: number
  status?: number
}) => {
  return request.get<PageResult<AlbumVO>>('/album/page', { params })
}

// 获取专辑详情
export const getAlbumDetail = (id: number) => {
  return request.get<AlbumVO>(`/album/${id}`)
}

// 创建专辑
export const createAlbum = (data: any) => {
  return request.post<number>('/album', data)
}

// 更新专辑
export const updateAlbum = (data: any) => {
  return request.put('/album', data)
}

// 删除专辑
export const deleteAlbum = (id: number) => {
  return request.delete(`/album/${id}`)
}