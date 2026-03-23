import request from './request'

export const getPlaybackListApi = () => request.get<{ songIds: number[]; currentIndex: number }>('/app/music/player/playlist')
export const savePlaybackListApi = (songIds: number[], currentIndex: number) =>
  request.post<void>('/app/music/player/playlist', { songIds, currentIndex })

export const addPlayHistoryApi = (songId: number, playDuration = 0) =>
  request.post<void>('/app/music/player/history', { songId, playDuration })
export const getRecentPlaysApi = (limit = 20) => request.get<number[]>('/app/music/player/history/recent', { params: { limit } })

export const addFavoriteApi = (favoriteType: number, targetId: number) =>
  request.post<void>('/app/music/player/favorite', { favoriteType, targetId })
export const removeFavoriteApi = (favoriteType: number, targetId: number) =>
  request.delete<void>('/app/music/player/favorite', { params: { favoriteType, targetId } })
export const checkFavoriteApi = (favoriteType: number, targetId: number) =>
  request.get<boolean>('/app/music/player/favorite/check', { params: { favoriteType, targetId } })
export const getFavoriteSongsApi = () => request.get<number[]>('/app/music/player/favorite/songs')
