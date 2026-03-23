const { getSongsByIds, getLyrics } = require("../../utils/music")
const { getPlaybackList, savePlaybackList, addPlayHistory, addFavorite, removeFavorite, checkFavorite } = require("../../utils/player")

Page({
  data: {
    song: {},
    playing: false,
    progress: 0,
    duration: 0,
    currentTimeText: "0:00",
    durationText: "0:00",
    favorited: false,
    lyricsLines: [],
    activeLyricIndex: -1,
    lyricScrollIntoView: ""
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
    this.initAudio()
    await this.loadPlaybackList()
    await this.loadSong()
  },
  onUnload() {
    this.lastUiUpdateTs = 0
    if (this.audio) {
      this.audio.destroy()
      this.audio = null
    }
  },
  async loadPlaybackList() {
    try {
      const res = await getPlaybackList()
      const app = getApp()
      app.globalData.playbackList = res.songIds || []
      app.globalData.playbackIndex = res.currentIndex || 0
    } catch (err) {
      console.warn(err)
    }
  },
  async loadSong() {
    const app = getApp()
    let ids = app.globalData.playbackList || []
    if (!ids.length && this.songId) {
      ids = [this.songId]
      app.globalData.playbackList = ids
      app.globalData.playbackIndex = 0
      await savePlaybackList(ids, 0)
    }
    if (!ids.length) return

    const songs = await getSongsByIds(ids)
    if (!songs || !songs.length) {
      wx.showToast({ title: "歌曲加载失败", icon: "none" })
      return
    }

    const song = songs.find(item => item.id === this.songId) || songs[app.globalData.playbackIndex] || songs[0]
    this.songId = song.id

    const normalizedPath = (song.filePath || "").replace(/\\/g, "/")
    const streamUrl = song.id ? `${app.globalData.apiBaseUrl}/api/app/music/song/${song.id}/stream` : ""
    const fallbackStaticUrl = normalizedPath ? `${app.globalData.apiBaseUrl}/static/${encodeURI(normalizedPath)}` : ""
    const songFileUrl = song.fileUrl || streamUrl || fallbackStaticUrl
    const normalizedSong = {
      ...song,
      name: song.name || song.title || "未知歌曲",
      coverUrl: song.coverUrl || song.albumCover || "",
      fileUrl: songFileUrl
    }

    this.setData({
      song: normalizedSong,
      progress: 0,
      currentTimeText: "0:00",
      activeLyricIndex: -1,
      lyricScrollIntoView: ""
    })

    const duration = normalizedSong.duration || 0
    this.setData({ duration, durationText: this.formatTime(duration) })

    // 先播放，歌词/收藏状态异步加载，减少首播等待
    this.playCurrentSong(normalizedSong)

    this.loadLyrics(normalizedSong.id)
      .catch(err => console.warn(err))

    checkFavorite(1, normalizedSong.id)
      .then(favorited => this.setData({ favorited }))
      .catch(err => console.warn(err))
  },
  async loadLyrics(songId) {
    try {
      const data = await getLyrics(songId)
      const lines = (data?.lines || []).map((item, idx) => ({
        id: `lyric-${idx}`,
        text: item.text || "",
        time: Number(item.time || 0)
      }))
      this.setData({ lyricsLines: lines })
    } catch (err) {
      console.warn(err)
      this.setData({ lyricsLines: [] })
    }
  },
  initAudio() {
    if (!this.audio) {
      this.audio = wx.createInnerAudioContext()
      // 边下边播，减少等待时间
      this.audio.autoplay = true
      this.audio.obeyMuteSwitch = false

      this.audio.onCanplay(() => {
        // 某些机型 canplay 时 duration 仍为 0，延时再取一次
        this.syncDurationWithRetry(0)

        // canplay 后立刻跳转到目标片段
        if (typeof this.pendingSeek === "number" && this.pendingSeek >= 0) {
          if (!this.data.playing) {
            this.audio.play()
          }
          this.audio.seek(this.pendingSeek)
          this.pendingSeek = null
        }
      })

      this.audio.onTimeUpdate(() => {
        const now = Date.now()
        if (this.lastUiUpdateTs && now - this.lastUiUpdateTs < 200) {
          return
        }
        this.lastUiUpdateTs = now

        const rawCurrentTime = Number(this.audio.currentTime || 0)
        const progress = Number(rawCurrentTime.toFixed(1))
        const realDuration = Math.floor(Number(this.audio.duration || 0))
        const currentTimeText = this.formatTime(rawCurrentTime)

        const newData = {}

        if (progress !== this.data.progress) {
          newData.progress = progress
        }

        if (currentTimeText !== this.data.currentTimeText) {
          newData.currentTimeText = currentTimeText
        }

        if (realDuration > 0 && realDuration !== this.data.duration) {
          newData.duration = realDuration
          newData.durationText = this.formatTime(realDuration)
        }

        const nextLyricIndex = this.getCurrentLyricIndex(rawCurrentTime * 1000)
        if (nextLyricIndex !== this.data.activeLyricIndex) {
          newData.activeLyricIndex = nextLyricIndex
          newData.lyricScrollIntoView = nextLyricIndex >= 0 ? `lyric-${nextLyricIndex}` : ""
        }

        if (Object.keys(newData).length) {
          this.setData(newData)
        }
      })

      this.audio.onPlay(() => {
        this.setData({ playing: true })
      })

      this.audio.onPause(() => {
        this.setData({ playing: false })
      })

      this.audio.onStop(() => {
        this.setData({ playing: false })
      })

      this.audio.onEnded(() => {
        this.nextSong()
      })
    }

    if (this.data.song.fileUrl) {
      this.playCurrentSong(this.data.song)
    }
  },
  playCurrentSong(song) {
    if (!this.audio || !song || !song.fileUrl) return

    this.pendingSeek = null
    this.audio.stop()

    // 先清空旧状态，再设置新 src，避免切歌后沿用上首歌 duration
    this.setData({
      playing: false,
      progress: 0,
      currentTimeText: "0:00",
      durationText: this.formatTime(this.data.duration || 0)
    })

    this.audio.src = song.fileUrl
    this.audio.play()

    // 主动重试读取 duration，提升时长展示成功率
    this.syncDurationWithRetry(0)

    addPlayHistory(song.id, 0)
  },
  syncDurationWithRetry(retryCount = 0) {
    if (!this.audio) return

    const duration = Math.floor(Number(this.audio.duration || 0))
    if (duration > 0) {
      if (duration !== this.data.duration) {
        this.setData({
          duration,
          durationText: this.formatTime(duration)
        })
      }
      return
    }

    if (retryCount >= 10) return

    setTimeout(() => {
      this.syncDurationWithRetry(retryCount + 1)
    }, 300)
  },
  getCurrentLyricIndex(currentMs) {
    const lines = this.data.lyricsLines || []
    if (!lines.length) return -1

    let index = -1
    for (let i = 0; i < lines.length; i++) {
      if (currentMs >= lines[i].time) {
        index = i
      } else {
        break
      }
    }
    return index
  },
  togglePlay() {
    if (!this.audio) return
    if (this.data.playing) {
      this.audio.pause()
    } else {
      this.audio.play()
    }
  },
  async prevSong() {
    const app = getApp()
    const list = app.globalData.playbackList || []
    if (!list.length) return
    app.globalData.playbackIndex = (app.globalData.playbackIndex - 1 + list.length) % list.length
    await savePlaybackList(list, app.globalData.playbackIndex)
    this.songId = list[app.globalData.playbackIndex]
    await this.loadSong()
    this.initAudio()
  },
  async nextSong() {
    const app = getApp()
    const list = app.globalData.playbackList || []
    if (!list.length) return
    app.globalData.playbackIndex = (app.globalData.playbackIndex + 1) % list.length
    await savePlaybackList(list, app.globalData.playbackIndex)
    this.songId = list[app.globalData.playbackIndex]
    await this.loadSong()
    this.initAudio()
  },
  seekTo(value) {
    if (!this.audio) return

    const target = Math.max(0, Number(value || 0))

    // 媒体尚未进入可 seek 状态时，先记录目标时间，canplay 后立即跳转
    const canSeekNow = this.audio.duration && this.audio.duration > 0
    if (canSeekNow) {
      if (!this.data.playing) {
        this.audio.play()
      }
      this.audio.seek(target)
    } else {
      this.pendingSeek = target
      // 预设起播时间，提升点击进度条后的立即跳转体验
      if (typeof this.audio.startTime === "number") {
        this.audio.startTime = target
      }
      if (!this.data.playing) {
        this.audio.play()
      }
    }

    this.setData({
      progress: target,
      currentTimeText: this.formatTime(target)
    })
  },
  onSeek(e) {
    this.seekTo(e?.detail?.value)
  },
  onSeeking(e) {
    this.seekTo(e?.detail?.value)
  },
  formatTime(seconds) {
    const total = Number(seconds)
    if (!Number.isFinite(total) || total < 0) return "0:00"
    const min = Math.floor(total / 60)
    const sec = Math.floor(total % 60)
    return `${min}:${sec < 10 ? "0" : ""}${sec}`
  },
  async toggleFavorite() {
    const songId = this.data.song.id
    if (!songId) return
    if (this.data.favorited) {
      await removeFavorite(1, songId)
    } else {
      await addFavorite(1, songId)
    }
    this.setData({ favorited: !this.data.favorited })
  }
})