const { getArtistTopSongs } = require("../../utils/music")
const { savePlaybackList } = require("../../utils/player")

Page({
  data: {
    artistId: null,
    artistName: "歌手",
    songs: [],
    defaultSongCover: "https://images.unsplash.com/photo-1511379938547-c1f69419868d?q=80&w=400&auto=format&fit=crop"
  },
  onLoad(options) {
    this.setData({
      artistId: options.artistId,
      artistName: decodeURIComponent(options.name || "歌手")
    })
    wx.setNavigationBarTitle({ title: `${this.data.artistName} 热门歌曲` })
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
      const songs = await getArtistTopSongs(this.data.artistId, 30)
      this.setData({ songs: songs || [] })
    } catch (err) {
      wx.showToast({ title: err.message || "加载失败", icon: "none" })
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
