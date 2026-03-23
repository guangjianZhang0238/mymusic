const { getPersonalRecommend, getHotSongs } = require("../../utils/music")
const { savePlaybackList } = require("../../utils/player")

function randomPick(list = [], count = 6) {
  const source = [...list]
  for (let i = source.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[source[i], source[j]] = [source[j], source[i]]
  }
  return source.slice(0, count)
}

Page({
  data: {
    daily: [],
    personal: [],
    surprise: [],
    surpriseSeed: "",
    defaultSongCover: "https://images.unsplash.com/photo-1511379938547-c1f69419868d?q=80&w=400&auto=format&fit=crop"
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
      const [daily, hot] = await Promise.all([
        getPersonalRecommend(10),
        getHotSongs()
      ])
      const surprise = randomPick(hot || [], 6)
      const surpriseSeed = new Date().toLocaleString()
      this.setData({ daily, personal: hot || [], surprise, surpriseSeed })
    } catch (err) {
      wx.showToast({ title: err.message || "加载失败", icon: "none" })
    }
  },
  refreshSurprise() {
    const surprise = randomPick(this.data.personal || [], 6)
    const surpriseSeed = new Date().toLocaleString()
    this.setData({ surprise, surpriseSeed })
  },
  async playSong(e) {
    const songId = e.currentTarget.dataset.id
    const section = e.currentTarget.dataset.section || "daily"
    const list = section === "surprise" ? this.data.surprise : this.data.daily
    const ids = (list || []).map(item => item.id)
    const currentIndex = ids.indexOf(songId)
    const app = getApp()
    app.globalData.playbackList = ids
    app.globalData.playbackIndex = currentIndex
    await savePlaybackList(ids, currentIndex)
    wx.navigateTo({ url: `/pages/player/player?songId=${songId}` })
  }
})
