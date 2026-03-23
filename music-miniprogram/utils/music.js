const { request } = require("./request")

const MAX_IMAGE_SIZE = 5 * 1024 * 1024
const ALLOWED_IMAGE_EXTENSIONS = ["jpg", "jpeg", "png", "webp"]

function getExt(filePath = "") {
  const match = String(filePath).toLowerCase().match(/\.([a-z0-9]+)(?:\?|#|$)/)
  return match ? match[1] : ""
}

function validateImageFile(file = {}) {
  const filePath = file.tempFilePath || ""
  const size = Number(file.size || 0)
  const ext = getExt(filePath)

  if (!filePath) {
    return { valid: false, message: "未获取到图片文件" }
  }

  if (!ALLOWED_IMAGE_EXTENSIONS.includes(ext)) {
    return {
      valid: false,
      message: "仅支持 jpg/jpeg/png/webp 格式"
    }
  }

  if (!size || size <= 0) {
    return { valid: false, message: "图片文件无效" }
  }

  if (size > MAX_IMAGE_SIZE) {
    return { valid: false, message: "图片不能超过 5MB" }
  }

  return { valid: true, message: "" }
}

function uploadFile({ url, filePath, name = "file", formData = {}, onProgress }) {
  const app = getApp()
  return new Promise((resolve, reject) => {
    const task = wx.uploadFile({
      url: `${app.globalData.apiBaseUrl}${url}`,
      filePath,
      name,
      formData,
      header: {
        Authorization: app.globalData.token ? `Bearer ${app.globalData.token}` : ""
      },
      success: (res) => {
        try {
          const payload = JSON.parse(res.data || "{}")
          if (payload && payload.code === 200) {
            resolve(payload.data)
          } else {
            reject(new Error(payload?.message || "上传失败"))
          }
        } catch (err) {
          reject(new Error("上传响应解析失败"))
        }
      },
      fail: reject
    })

    if (task && typeof task.onProgressUpdate === "function" && typeof onProgress === "function") {
      task.onProgressUpdate((progressEvent) => {
        const progress = Number(progressEvent?.progress || 0)
        onProgress(progress)
      })
    }
  })
}

function finalizeImageUrl(url) {
  if (!url || typeof url !== "string") return ""
  const value = url.trim()
  if (!value) return ""

  // 处理中文/空格等字符，避免小程序 image 组件加载失败
  const [pathPart, queryPart] = value.split("?")
  const encodedPath = encodeURI(pathPart)
  return queryPart ? `${encodedPath}?${queryPart}` : encodedPath
}

function normalizeImageUrl(url) {
  if (!url || typeof url !== "string") return ""
  const value = url.trim()
  if (!value) return ""

  // 已经是完整 URL 或 data URI，直接返回（并做安全编码）
  if (/^(https?:)?\/\//i.test(value) || value.startsWith("data:")) {
    // 兼容数据库里可能保存的旧 HTTP 资源链接，避免真机拦截导致图片空白
    // （后端静态资源已通过域名 + SSL 提供）
    const legacyHttpHostRe = /^http:\/\/103\.215\.83\.202(?::39080)?\//i
    const proxyHttpsBase = "https://ypamnmll.fun/"
    if (value.startsWith("http://") && legacyHttpHostRe.test(value)) {
      const replaced = value.replace(legacyHttpHostRe, proxyHttpsBase)
      return finalizeImageUrl(replaced)
    }
    return finalizeImageUrl(value)
  }

  const app = getApp()
  const baseUrl = (app && app.globalData && app.globalData.apiBaseUrl) ? app.globalData.apiBaseUrl.replace(/\/$/, "") : ""
  if (!baseUrl) return finalizeImageUrl(value)

  // 后端已映射 /static/** 与 /user-static/**
  if (value.startsWith("/static/") || value.startsWith("/user-static/")) {
    return finalizeImageUrl(`${baseUrl}${value}`)
  }

  // 兼容数据库中保存为相对文件路径（如：周杰伦/cover.jpg）
  if (!value.startsWith("/")) {
    return finalizeImageUrl(`${baseUrl}/static/${value}`)
  }

  // 兼容错误前缀（如 /pages/home/陈奕迅/cover.jpg），这类不是小程序本地资源，而是服务端文件路径
  // 统一转发到后端静态资源目录
  if (value.startsWith("/pages/") || value.startsWith("/music/") || value.startsWith("/cover/") || value.startsWith("/artist/") || value.startsWith("/album/")) {
    return finalizeImageUrl(`${baseUrl}/static${value}`)
  }

  // 其他以 / 开头的路径默认也按静态资源处理，避免被当成小程序本地路径
  return finalizeImageUrl(`${baseUrl}/static${value}`)
}

function normalizeSong(song = {}) {
  const albumCover = normalizeImageUrl(song.albumCover)
  const artistCover = normalizeImageUrl(song.artistCover)
  const coverUrl = normalizeImageUrl(song.coverUrl)

  return {
    ...song,
    albumCover,
    artistCover,
    coverUrl
  }
}

function normalizePlaylist(playlist = {}) {
  const coverImage = normalizeImageUrl(playlist.coverImage)
  const coverUrl = normalizeImageUrl(playlist.coverUrl || playlist.coverImage)
  return {
    ...playlist,
    coverImage,
    coverUrl
  }
}

function normalizePlaylistList(list) {
  if (!Array.isArray(list)) return []
  return list.map(normalizePlaylist)
}

function normalizeSongList(list) {
  if (!Array.isArray(list)) return []
  return list.map(normalizeSong)
}

async function getDailyRecommend(limit = 10) {
  const data = await request({ url: "/api/mp/recommend/daily", data: { limit } })
  return normalizeSongList(data)
}

async function getSceneRecommend(limit = 10) {
  const data = await request({ url: "/api/mp/recommend/scene", data: { limit } })
  return normalizeSongList(data)
}

async function getPersonalRecommend(limit = 10) {
  const data = await request({ url: "/api/mp/recommend/personal", data: { limit } })
  return normalizeSongList(data)
}

async function getHotSongs() {
  const data = await request({ url: "/api/app/music/song/hot" })
  return normalizeSongList(data)
}

function normalizeArtist(artist = {}) {
  const avatar = normalizeImageUrl(artist.avatar)
  return {
    ...artist,
    avatar
  }
}

function normalizeArtistPage(page = {}) {
  const records = Array.isArray(page.records) ? page.records.map(normalizeArtist) : []
  return {
    ...page,
    records
  }
}

async function getArtistPage(current = 1, size = 10, keyword = "") {
  const data = await request({ url: "/api/app/music/artist/page", data: { current, size, keyword } })
  return normalizeArtistPage(data)
}

async function getArtistTopSongs(artistId, limit = 20) {
  const data = await request({ url: `/api/app/music/artist/${artistId}/top-songs`, data: { limit } })
  return normalizeSongList(data)
}

async function getSongsByIds(ids = []) {
  const serializedIds = Array.isArray(ids) ? ids.join(",") : ""
  const data = await request({ url: "/api/app/music/song/by-ids", data: { ids: serializedIds } })
  return normalizeSongList(data)
}

async function getSongPage(current = 1, size = 20, keyword = "") {
  return request({ url: "/api/app/music/song/page", data: { current, size, keyword } })
}

async function getLyrics(songId) {
  return request({ url: `/api/app/music/lyrics/song/${songId}` })
}

async function getUserPlaylists() {
  const data = await request({ url: "/api/app/music/playlist/user" })
  return normalizePlaylistList(data)
}

async function createPlaylist(name, description = "") {
  return request({
    url: "/api/app/music/playlist",
    method: "POST",
    data: { name, description }
  })
}

async function updatePlaylist(id, name, description = "") {
  return request({
    url: "/api/app/music/playlist",
    method: "PUT",
    data: { id, name, description }
  })
}

async function deletePlaylist(id) {
  return request({
    url: `/api/app/music/playlist/${id}`,
    method: "DELETE"
  })
}

async function getPlaylistDetail(playlistId) {
  const data = await request({ url: `/api/app/music/playlist/${playlistId}` })
  return normalizePlaylist(data)
}

async function getPlaylistSongs(playlistId) {
  const data = await request({ url: `/api/app/music/playlist/${playlistId}/songs` })
  return normalizeSongList(data)
}

async function addSongToPlaylist(playlistId, songId) {
  return request({
    url: `/api/app/music/playlist/${playlistId}/song/${songId}`,
    method: "POST"
  })
}

async function removeSongFromPlaylist(playlistId, songId) {
  return request({
    url: `/api/app/music/playlist/${playlistId}/song/${songId}`,
    method: "DELETE"
  })
}

async function uploadUserAvatar(filePath, onProgress) {
  return uploadFile({
    url: "/api/mp/asset/avatar",
    filePath,
    name: "file",
    onProgress
  })
}

async function uploadPlaylistCover(playlistId, filePath, onProgress) {
  return uploadFile({
    url: "/api/mp/asset/playlist-cover",
    filePath,
    name: "file",
    formData: { playlistId: String(playlistId) },
    onProgress
  })
}

module.exports = {
  getDailyRecommend,
  getSceneRecommend,
  getPersonalRecommend,
  getHotSongs,
  getArtistPage,
  getArtistTopSongs,
  getSongsByIds,
  getSongPage,
  getLyrics,
  getUserPlaylists,
  createPlaylist,
  updatePlaylist,
  deletePlaylist,
  getPlaylistDetail,
  getPlaylistSongs,
  addSongToPlaylist,
  removeSongFromPlaylist,
  uploadUserAvatar,
  uploadPlaylistCover,
  validateImageFile
}
