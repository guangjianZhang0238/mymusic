import request from './request'

export const getCommentsApi = (songId: number, page = 1, size = 20) =>
  request.get<any>(`/app/music/comment/song/${songId}`, { params: { page, size } })
export const addCommentApi = (songId: number, content: string) => request.post<number>('/app/music/comment', { songId, content })
export const deleteCommentApi = (commentId: number) => request.delete<void>(`/app/music/comment/${commentId}`)
export const likeCommentApi = (commentId: number) => request.post<void>(`/app/music/comment/${commentId}/like`)
export const unlikeCommentApi = (commentId: number) => request.delete<void>(`/app/music/comment/${commentId}/like`)

export const createLyricsShareApi = (payload: any) => request.post<number>('/app/music/lyrics/share', payload)
export const getMyLyricsSharesApi = (page = 1, size = 20) => request.get<any>('/app/music/lyrics/share/user', { params: { page, size } })

export const createFeedbackApi = (payload: any) => request.post<number>('/app/music/feedback', payload)
export const getMyFeedbackApi = (page = 1, size = 20) => request.get<any>('/app/music/feedback/mine', { params: { page, size } })
