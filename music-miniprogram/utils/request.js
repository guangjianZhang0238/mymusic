const app = getApp()

function buildHeaders(customHeaders = {}) {
  const headers = {
    "Content-Type": "application/json",
    ...customHeaders
  }
  if (app.globalData.token) {
    headers.Authorization = `Bearer ${app.globalData.token}`
  }
  return headers
}

function request({ url, method = "GET", data = {}, headers = {} }) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${app.globalData.apiBaseUrl}${url}`,
      method,
      data,
      header: buildHeaders(headers),
      success: (res) => {
        if (res.statusCode === 401) {
          if (typeof app.clearLoginState === "function") {
            app.clearLoginState()
          } else {
            wx.removeStorageSync("token")
            wx.removeStorageSync("userInfo")
            wx.removeStorageSync("tokenExpireAt")
            app.globalData.token = ""
            app.globalData.userInfo = null
          }
          return reject(new Error("未登录"))
        }
        const { data: payload } = res
        if (payload && payload.code === 200) {
          resolve(payload.data)
        } else {
          reject(new Error(payload?.message || "请求失败"))
        }
      },
      fail: reject
    })
  })
}

module.exports = {
  request
}
