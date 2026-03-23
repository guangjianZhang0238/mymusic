const { getPlaylistDetail, getPlaylistSongs, getSongsByIds, getSongPage, addSongToPlaylist, removeSongFromPlaylist } = require("../../utils/music")
const { savePlaybackList } = require("../../utils/player")

Page({
  data: {
    playlist: {},
    songs: [],
    searchResults: [],
    keyword: "",
    defaultCover: "https://images.unsplash.com/photo-1511379938547-c1f69419868d?q=80&w=400&auto=format&fit=crop"
  },
  onLoad(options) {
    this.playlistId = options.id
  },
  onShow() {
    const app = getApp()
    if (!app.globalData.token) {
      wx.redirectTo({ url: "/pages/login/login" })
      return
    }
    this.loadData()
  },
  async loadData() {
    try {
      const playlist = await getPlaylistDetail(this.playlistId)
      const songIds = await getPlaylistSongs(this.playlistId)
      const songs = songIds.length ? await getSongsByIds(songIds) : []
      this.setData({ playlist, songs })
    } catch (err) {
      wx.showToast({ title: err.message || "加载失败", icon: "none" })
    }
  },
  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value })
  },
  async searchSongs() {
    if (!this.data.keyword.trim()) {
      wx.showToast({ title: "请输入关键词", icon: "none" })
      return
    }
    try {
      const result = await getSongPage(1, 20, this.data.keyword)
      this.setData({ searchResults: result.records || [] })
    } catch (err) {
      wx.showToast({ title: err.message || "搜索失败", icon: "none" })
    }
  },
  async addSong(e) {
    const songId = e.currentTarget.dataset.id
    try {
      await addSongToPlaylist(this.playlistId, songId)
      wx.showToast({ title: "已加入" })
      this.loadData()
    } catch (err) {
      wx.showToast({ title: err.message || "添加失败", icon: "none" })
    }
  },
  async removeSong(e) {
    const songId = e.currentTarget.dataset.id
    try {
      await removeSongFromPlaylist(this.playlistId, songId)
      this.loadData()
    } catch (err) {
      wx.showToast({ title: err.message || "移除失败", icon: "none" })
    }
  },
  async playSong(e) {
    const songId = e.currentTarget.dataset.id
    const ids = this.data.songs.map(item => item.id)
    const currentIndex = ids.indexOf(songId)
    const app = getApp()
    app.globalData.playbackList = ids
    app.globalData.playbackIndex = currentIndex
    await savePlaybackList(ids, currentIndex)
    wx.navigateTo({ url: `/pages/player/player?songId=${songId}` })
  }
})
