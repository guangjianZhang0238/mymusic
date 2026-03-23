const { login, register } = require("../../utils/auth")

const LOGIN_EXPIRE_DAYS = 15
const LOGIN_EXPIRE_MS = LOGIN_EXPIRE_DAYS * 24 * 60 * 60 * 1000

Page({
  data: {
    mode: "login",
    loading: false,
    errorMessage: "",
    form: {
      username: "",
      password: "",
      confirmPassword: ""
    }
  },

  switchMode(e) {
    const mode = e.currentTarget.dataset.mode
    if (!mode || mode === this.data.mode) return
    this.setData({
      mode,
      errorMessage: "",
      form: {
        username: this.data.form.username,
        password: "",
        confirmPassword: ""
      }
    })
  },

  handleInput(e) {
    const field = e.currentTarget.dataset.field
    const value = e.detail.value
    if (!field) return
    this.setData({
      [`form.${field}`]: value,
      errorMessage: ""
    })
  },

  validateForm() {
    const { mode, form } = this.data
    const username = (form.username || "").trim()
    const password = form.password || ""
    const confirmPassword = form.confirmPassword || ""

    if (!username) return "请输入用户名"
    if (!password) return "请输入密码"

    if (mode === "register") {
      if (password.length < 6) return "密码长度不能少于6位"
      if (password !== confirmPassword) return "两次输入的密码不一致"
    }

    return ""
  },

  async handleSubmit() {
    if (this.data.loading) return

    const validationError = this.validateForm()
    if (validationError) {
      this.setData({ errorMessage: validationError })
      return
    }

    const { mode, form } = this.data
    const username = form.username.trim()
    const password = form.password

    this.setData({ loading: true, errorMessage: "" })

    try {
      if (mode === "register") {
        await register({ username, password })
      }

      const loginData = await login({ username, password })
      const app = getApp()
      app.globalData.token = loginData.token
      app.globalData.userInfo = loginData.userInfo

      const tokenExpireAt = Date.now() + LOGIN_EXPIRE_MS

      wx.setStorageSync("token", loginData.token)
      wx.setStorageSync("userInfo", loginData.userInfo)
      wx.setStorageSync("tokenExpireAt", tokenExpireAt)

      wx.switchTab({ url: "/pages/home/home" })
    } catch (err) {
      const message = err.message || (mode === "login" ? "登录失败" : "注册失败")
      this.setData({ errorMessage: message })
    } finally {
      this.setData({ loading: false })
    }
  }
})
