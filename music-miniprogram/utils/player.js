const { request } = require("./request")

async function getPlaybackList() {
  return request({ url: "/api/app/music/player/playlist" })
}

async function savePlaybackList(songIds = [], currentIndex = 0) {
  return request({
    url: "/api/app/music/player/playlist",
    method: "POST",
    data: { songIds, currentIndex }
  })
}

async function addPlayHistory(songId, playDuration = 0) {
  return request({
    url: "/api/app/music/player/history",
    method: "POST",
    data: { songId, playDuration }
  })
}

async function getRecentPlays(limit = 20) {
  return request({
    url: "/api/app/music/player/history/recent",
    data: { limit }
  })
}

async function addFavorite(favoriteType, targetId) {
  return request({
    url: "/api/app/music/player/favorite",
    method: "POST",
    data: { favoriteType, targetId }
  })
}

async function removeFavorite(favoriteType, targetId) {
  return request({
    url: "/api/app/music/player/favorite",
    method: "DELETE",
    data: { favoriteType, targetId }
  })
}

async function checkFavorite(favoriteType, targetId) {
  return request({
    url: "/api/app/music/player/favorite/check",
    data: { favoriteType, targetId }
  })
}

async function getFavoriteSongs() {
  return request({ url: "/api/app/music/player/favorite/songs" })
}

// 兼容旧拼写（避免页面仍引用 getFavouriteSongs 导致函数不存在）
const getFavouriteSongs = getFavoriteSongs

module.exports = {
  getPlaybackList,
  savePlaybackList,
  addPlayHistory,
  getRecentPlays,
  addFavorite,
  removeFavorite,
  checkFavorite,
  getFavoriteSongs,
  getFavouriteSongs
}
