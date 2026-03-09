export interface UserVO {
  id: number
  username: string
  nickname: string
  avatar: string
  phone: string
  email: string
  role: number
  status: number
  createTime: string
  lastLoginTime: string
}

export interface LoginDTO {
  username: string
  password: string
}

export interface LoginVO {
  token: string
  userInfo: UserVO
}

export interface ArtistVO {
  id: number
  name: string
  nameEn: string
  avatar: string
  description: string
  region: string
  type: number
  albumCount: number
  songCount: number
  sortOrder: number
  status: number
  createTime: string
}

export interface AlbumVO {
  id: number
  artistId: number
  artistName: string
  name: string
  folderPath: string
  coverImage: string
  releaseDate: string
  description: string
  albumType: number
  songCount: number
  sortOrder: number
  status: number
  createTime: string
}

export interface SongVO {
  id: number
  albumId: number
  albumName: string
  artistId: number
  artistName: string
  title: string
  titleEn: string
  filePath: string
  fileName: string
  fileSize: number
  fileSizeFormat: string
  duration: number
  durationFormat: string
  format: string
  sampleRate: number
  bitDepth: number
  bitRate: number
  channels: number
  trackNumber: number
  discNumber: number
  hasLyrics: number
  playCount: number
  sortOrder: number
  status: number
  createTime: string
}

export interface LyricsVO {
  id: number
  songId: number
  songTitle: string
  lyricsType: number
  content: string
  translation: string
  source: string
  lines: Array<{
    time: number
    text: string
  }>
  createTime: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}
