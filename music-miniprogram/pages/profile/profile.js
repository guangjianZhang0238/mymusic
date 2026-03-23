const { getSongsByIds, uploadUserAvatar, validateImageFile } = require("../../utils/music")
const { getFavoriteSongs, savePlaybackList } = require("../../utils/player")

const MAX_UPLOAD_RETRY = 3
const BASE_RETRY_DELAY = 500

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function normalizeUserAvatar(url) {
  if (!url || typeof url !== "string") return ""
  const value = url.trim()
  if (!value) return ""
  if (/^(https?:)?\/\//i.test(value) || value.startsWith("data:")) {
    // 兼容数据库里可能保存的旧 HTTP 头像链接，避免真机拦截导致头像空白
    const legacyHttpHostRe = /^http:\/\/103\.215\.83\.202(?::39080)?\//i
    const proxyHttpsBase = "https://ypamnmll.fun/"
    if (value.startsWith("http://") && legacyHttpHostRe.test(value)) {
      return value.replace(legacyHttpHostRe, proxyHttpsBase)
    }
    return value
  }
  const app = getApp()
  const baseUrl = (app && app.globalData && app.globalData.apiBaseUrl) ? app.globalData.apiBaseUrl.replace(/\/$/, "") : ""
  if (!baseUrl) return value
  if (value.startsWith("/user-static/") || value.startsWith("/static/")) return `${baseUrl}${value}`
  if (value.startsWith("/")) return `${baseUrl}/user-static${value}`
  return `${baseUrl}/user-static/${value}`
}

Page({
  data: {
    userInfo: {},
    favorites: [],
    defaultAvatar: "https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?q=80&w=200&auto=format&fit=crop",
    defaultSongCover: "https://images.unsplash.com/photo-1511379938547-c1f69419868d?q=80&w=400&auto=format&fit=crop"
  },
  onShow() {
    const app = getApp()
    if (!app.globalData.token) {
      wx.redirectTo({ url: "/pages/login/login" })
      return
    }
    const rawUser = app.globalData.userInfo || {}
    this.setData({
      userInfo: {
        ...rawUser,
        avatarUrl: normalizeUserAvatar(rawUser.avatar)
      }
    })
    this.loadFavorites()
  },
  async loadFavorites() {
    try {
      const ids = await getFavoriteSongs()
      const songs = ids.length ? await getSongsByIds(ids) : []
      this.setData({ favorites: songs })
    } catch (err) {
      wx.showToast({ title: err.message || "加载失败", icon: "none" })
    }
  },
  chooseAvatar() {
    wx.chooseMedia({
      count: 1,
      mediaType: ["image"],
      sourceType: ["album", "camera"],
      success: async (res) => {
        const selectedFile = res.tempFiles?.[0] || {}
        const filePath = selectedFile.tempFilePath
        if (!filePath) return

        const check = validateImageFile(selectedFile)
        if (!check.valid) {
          wx.showToast({ title: check.message, icon: "none" })
          return
        }

        const doUpload = async () => {
          let lastError = null
          for (let attempt = 1; attempt <= MAX_UPLOAD_RETRY; attempt += 1) {
            wx.showLoading({
              title: attempt === 1 ? "上传中 0%" : `重试中(${attempt}/${MAX_UPLOAD_RETRY})`,
              mask: true
            })
            try {
              const data = await uploadUserAvatar(filePath, (progress) => {
                wx.showLoading({
                  title: `上传中 ${Math.min(100, Math.max(0, progress))}%`,
                  mask: true
                })
              })
              const avatar = data?.path || ""
              const avatarUrl = data?.url ? normalizeUserAvatar(data.url) : normalizeUserAvatar(avatar)
              const appUser = getApp().globalData.userInfo || {}
              const nextUser = { ...appUser, avatar, avatarUrl }
              getApp().globalData.userInfo = nextUser
              wx.setStorageSync("userInfo", nextUser)
              this.setData({ userInfo: nextUser })
              wx.showToast({ title: "头像已更新" })
              return
            } catch (err) {
              lastError = err
              if (attempt < MAX_UPLOAD_RETRY) {
                const delay = BASE_RETRY_DELAY * Math.pow(2, attempt - 1)
                await sleep(delay)
              }
            } finally {
              wx.hideLoading()
            }
          }

          wx.showModal({
            title: "上传失败",
            content: `${lastError?.message || "上传失败，请稍后重试"}\n已自动重试 ${MAX_UPLOAD_RETRY} 次`,
            confirmText: "知道了",
            showCancel: false
          })
        }

        doUpload()
      }
    })
  },
  async playSong(e) {
    const songId = e.currentTarget.dataset.id
    const ids = this.data.favorites.map(item => item.id)
    const currentIndex = ids.indexOf(songId)
    const app = getApp()
    app.globalData.playbackList = ids
    app.globalData.playbackIndex = currentIndex
    await savePlaybackList(ids, currentIndex)
    wx.navigateTo({ url: `/pages/player/player?songId=${songId}` })
  }
})
