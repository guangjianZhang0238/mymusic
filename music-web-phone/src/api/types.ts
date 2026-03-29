export interface UserInfo {
  id: number
  username: string
  nickname?: string
  avatar?: string
}

export interface LoginResp {
  token: string
  userInfo: UserInfo
}

export interface Song {
  id: number
  name: string
  title?: string
  artistName?: string
  artistNames?: string
  albumCover?: string
  coverUrl?: string
  duration?: number
  filePath?: string
  fileUrl?: string
  lyricSnippet?: string
}

export interface Artist {
  id: number
  name: string
  avatar?: string
}

export interface Playlist {
  id: number
  name: string
  description?: string
  coverImage?: string
  coverUrl?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}
