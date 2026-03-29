import request from './request'
import type { Artist, PageResult, Playlist, Song } from './types'

export const getHotSongsApi = (limit?: number) => request.get<Song[]>('/app/music/song/hot', { params: { limit } })
export const getPersonalRecommendApi = (limit = 10) => request.get<Song[]>('/web/recommend/personal', { params: { limit } })
export const getDailyRecommendApi = (limit = 10) => request.get<Song[]>('/web/recommend/daily', { params: { limit } })
export const getSceneRecommendApi = (limit = 10) => request.get<Song[]>('/web/recommend/scene', { params: { limit } })

export const getArtistPageApi = (current = 1, size = 20, keyword = '') =>
  request.get<PageResult<Artist>>('/app/music/artist/page', { params: { current, size, keyword } })

export const getHotArtistsApi = (limit = 20) => request.get<Artist[]>('/app/music/artist/hot', { params: { limit } })

export const getArtistDetailApi = (artistId: number) => request.get<Artist>(`/app/music/artist/${artistId}`)
export const getArtistTopSongsApi = (artistId: number, limit = 20) =>
  request.get<Song[]>(`/app/music/artist/${artistId}/top-songs`, { params: { limit } })

export const getSongPageApi = (current = 1, size = 20, keyword = '', albumId?: number, artistId?: number) =>
  request.get<PageResult<Song>>('/app/music/song/page', { params: { current, size, keyword, albumId, artistId } })

export const getSongsByIdsApi = (ids: number[]) => request.get<Song[]>('/app/music/song/by-ids', { params: { ids: ids.join(',') } })

export const getAlbumPageApi = (current = 1, size = 20, keyword = '', artistId?: number) =>
  request.get<PageResult<any>>('/app/music/album/page', { params: { current, size, keyword, artistId } })
export const getAlbumDetailApi = (albumId: number) => request.get<any>(`/app/music/album/${albumId}`)

export const getLyricsApi = (songId: number) => request.get<any>(`/app/music/lyrics/song/${songId}`)
export const getSearchSuggestionsApi = (keyword: string, limit = 10) =>
  request.get<any[]>('/app/music/search/suggestions', { params: { keyword, limit } })

export const getUserPlaylistsApi = () => request.get<Playlist[]>('/app/music/playlist/user')
export const createPlaylistApi = (name: string, description = '') => request.post<number>('/app/music/playlist', { name, description })
export const updatePlaylistApi = (id: number, name: string, description = '') =>
  request.put<void>('/app/music/playlist', { id, name, description })
export const deletePlaylistApi = (id: number) => request.delete<void>(`/app/music/playlist/${id}`)
export const getPlaylistDetailApi = (id: number) => request.get<Playlist>(`/app/music/playlist/${id}`)
export const getPlaylistSongsApi = (id: number) => request.get<number[]>(`/app/music/playlist/${id}/songs`)
export const addSongToPlaylistApi = (playlistId: number, songId: number) =>
  request.post<void>(`/app/music/playlist/${playlistId}/song/${songId}`)
export const removeSongFromPlaylistApi = (playlistId: number, songId: number) =>
  request.delete<void>(`/app/music/playlist/${playlistId}/song/${songId}`)
export const clearPlaylistSongsApi = (playlistId: number) => request.delete<void>(`/app/music/playlist/${playlistId}/songs`)

export const uploadAvatarApi = (file: File) => {
  const data = new FormData()
  data.append('file', file)
  return request.post<{ path: string; url: string }>('/mp/asset/avatar', data)
}
export const uploadPlaylistCoverApi = (playlistId: number, file: File) => {
  const data = new FormData()
  data.append('file', file)
  data.append('playlistId', String(playlistId))
  return request.post<{ path: string; url: string }>('/mp/asset/playlist-cover', data)
}
