const { getLyrics, getSongsByIds } = require("../../utils/music")

Page({
  data: {
    songName: "",
    artistName: "",
    lines: []
  },
  onLoad(options) {
    this.songId = Number(options.songId)
  },
  async onShow() {
    const app = getApp()
    if (!app.globalData.token) {
      wx.redirectTo({ url: "/pages/login/login" })
      return
    }
    await this.loadSong()
    await this.loadLyrics()
  },
  async loadSong() {
    const songs = await getSongsByIds([this.songId])
    const song = songs[0] || {}
    this.setData({
      songName: song.name || "",
      artistName: song.artistName || song.artistNames || ""
    })
  },
  async loadLyrics() {
    try {
      const data = await getLyrics(this.songId)
      const lines = (data?.lines || []).map(item => ({ text: item.text }))
      this.setData({ lines })
    } catch (err) {
      wx.showToast({ title: err.message || "加载失败", icon: "none" })
    }
  }
})
