const { getArtistPage, getHotSongs } = require("../../utils/music")
const { savePlaybackList } = require("../../utils/player")

Page({
  data: {
    nickname: "",
    topArtists: [],
    hot: [],
    defaultSongCover: "https://images.unsplash.com/photo-1511379938547-c1f69419868d?q=80&w=400&auto=format&fit=crop"
  },
  onShow() {
    const app = getApp()
    if (!app.globalData.token) {
      wx.redirectTo({ url: "/pages/login/login" })
      return
    }
    this.setData({ nickname: app.globalData.userInfo?.nickname || "音乐听友" })
    this.loadData()
  },
  async loadData() {
    try {
      const [artistPage, hot] = await Promise.all([
        getArtistPage(1, 10, ""),
        getHotSongs()
      ])
      this.setData({ topArtists: artistPage.records || [], hot })
    } catch (err) {
      wx.showToast({ title: err.message || "加载失败", icon: "none" })
    }
  },
  openArtist(e) {
    const artistId = e.currentTarget.dataset.id
    const artistName = e.currentTarget.dataset.name || "歌手"
    wx.navigateTo({ url: `/pages/artist-songs/artist-songs?artistId=${artistId}&name=${encodeURIComponent(artistName)}` })
  },
  async playSong(e) {
    const songId = e.currentTarget.dataset.id
    const list = [...this.data.hot]
    const ids = list.map(item => item.id)
    const currentIndex = ids.indexOf(songId)
    const app = getApp()
    app.globalData.playbackList = ids
    app.globalData.playbackIndex = currentIndex
    await savePlaybackList(ids, currentIndex)
    wx.navigateTo({ url: `/pages/player/player?songId=${songId}` })
  }
})
