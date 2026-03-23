const LOGIN_EXPIRE_DAYS = 15
const LOGIN_EXPIRE_MS = LOGIN_EXPIRE_DAYS * 24 * 60 * 60 * 1000

App({
  globalData: {
    // 统一使用 HTTPS，避免真机环境下 image 组件拦截 HTTP 资源
    apiBaseUrl: "https://ypamnmll.fun",
    token: "",
    userInfo: null,
    playbackList: [],
    playbackIndex: 0
  },

  clearLoginState() {
    wx.removeStorageSync("token")
    wx.removeStorageSync("userInfo")
    wx.removeStorageSync("tokenExpireAt")
    this.globalData.token = ""
    this.globalData.userInfo = null
  },

  loadLoginState() {
    const token = wx.getStorageSync("token")
    const userInfo = wx.getStorageSync("userInfo")
    const tokenExpireAt = Number(wx.getStorageSync("tokenExpireAt") || 0)

    if (!token || !tokenExpireAt || tokenExpireAt <= Date.now()) {
      this.clearLoginState()
      return false
    }

    this.globalData.token = token
    this.globalData.userInfo = userInfo || null
    return true
  },

  onLaunch() {
    this.loadLoginState()
  },

  onShow() {
    const hasValidLogin = this.loadLoginState()
    if (hasValidLogin) return

    const pages = getCurrentPages()
    const currentRoute = pages.length ? pages[pages.length - 1].route : ""
    if (currentRoute !== "pages/login/login") {
      wx.reLaunch({ url: "/pages/login/login" })
    }
  }
})
