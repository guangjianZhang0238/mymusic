const { getUserPlaylists, createPlaylist, updatePlaylist, deletePlaylist, uploadPlaylistCover, validateImageFile } = require("../../utils/music")

const MAX_UPLOAD_RETRY = 3
const BASE_RETRY_DELAY = 500

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

Page({
  data: {
    playlists: [],
    defaultCover: "https://images.unsplash.com/photo-1511379938547-c1f69419868d?q=80&w=400&auto=format&fit=crop",
    showModal: false,
    modalTitle: "新建歌单",
    form: {
      id: null,
      name: "",
      description: ""
    }
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
      const playlists = await getUserPlaylists()
      this.setData({ playlists })
    } catch (err) {
      wx.showToast({ title: err.message || "加载失败", icon: "none" })
    }
  },
  openPlaylist(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/playlist/playlist?id=${id}` })
  },
  openCreate() {
    this.setData({
      showModal: true,
      modalTitle: "新建歌单",
      form: { id: null, name: "", description: "" }
    })
  },
  openEdit(e) {
    this.setData({
      showModal: true,
      modalTitle: "编辑歌单",
      form: {
        id: e.currentTarget.dataset.id,
        name: e.currentTarget.dataset.name || "",
        description: e.currentTarget.dataset.desc || ""
      }
    })
  },
  closeModal() {
    this.setData({ showModal: false })
  },
  noop() {},
  onNameInput(e) {
    this.setData({ "form.name": e.detail.value })
  },
  onDescInput(e) {
    this.setData({ "form.description": e.detail.value })
  },
  async submitForm() {
    const { id, name, description } = this.data.form
    if (!name.trim()) {
      wx.showToast({ title: "请输入歌单名称", icon: "none" })
      return
    }
    try {
      if (id) {
        await updatePlaylist(id, name, description)
      } else {
        await createPlaylist(name, description)
      }
      this.closeModal()
      this.loadData()
    } catch (err) {
      wx.showToast({ title: err.message || "保存失败", icon: "none" })
    }
  },
  uploadCover(e) {
    const playlistId = e.currentTarget.dataset.id
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
              await uploadPlaylistCover(playlistId, filePath, (progress) => {
                wx.showLoading({
                  title: `上传中 ${Math.min(100, Math.max(0, progress))}%`,
                  mask: true
                })
              })
              wx.showToast({ title: "封面已更新" })
              this.loadData()
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
  deleteItem(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: "删除歌单",
      content: "确定删除该歌单吗？",
      success: async (res) => {
        if (!res.confirm) return
        try {
          await deletePlaylist(id)
          this.loadData()
        } catch (err) {
          wx.showToast({ title: err.message || "删除失败", icon: "none" })
        }
      }
    })
  }
})
