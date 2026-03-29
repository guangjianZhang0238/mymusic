import { defineStore } from 'pinia'
import { addPlayHistoryApi, getPlaybackListApi, savePlaybackListApi } from '@/api/player'
import { getSongsByIdsApi } from '@/api/music'
import type { Song } from '@/api/types'

export type PlayMode = 'random' | 'order' | 'loop' | 'single'

const normalizeIds = (songIds: number[]) =>
  (songIds || []).map((id) => Number(id)).filter((n) => Number.isFinite(n) && n > 0)

const clamp = (n: number, min: number, max: number) => Math.max(min, Math.min(max, n))

export const usePlayerStore = defineStore('player', {
  state: () => ({
    queue: [] as number[],
    queueSongs: [] as Song[],
    currentIndex: 0,
    currentSongId: 0,
    playing: false,
    currentTime: 0,
    playMode: 'loop' as PlayMode
  }),
  getters: {
    currentSong: (state) => state.queueSongs.find((item) => item.id === state.currentSongId)
  },
  actions: {
    async hydrateFromServer() {
      const data = await getPlaybackListApi()
      const normalizedQueue = normalizeIds(data.songIds || [])
      this.queue = normalizedQueue

      const len = normalizedQueue.length
      if (!len) {
        this.currentIndex = 0
        this.currentSongId = 0
        this.queueSongs = []
        return
      }

      const idx = clamp(Number(data.currentIndex || 0), 0, len - 1)
      this.currentIndex = idx
      this.currentSongId = normalizedQueue[idx] || 0
      await this.refreshQueueSongs()
    },

    async persistQueue() {
      try {
        await savePlaybackListApi(this.queue, this.currentIndex)
      } catch {
        // ignore persistence errors
      }
    },

    async refreshQueueSongs() {
      if (!this.queue.length) {
        this.queueSongs = []
        this.currentIndex = 0
        this.currentSongId = 0
        return
      }

      const uniqueIds = Array.from(new Set(this.queue))
      const songs = await getSongsByIdsApi(uniqueIds)
      const songMap = new Map<number, Song>()
      songs.forEach((song) => {
        if (song?.id) songMap.set(Number(song.id), song)
      })

      const rebuilt = this.queue.map((id) => songMap.get(Number(id))).filter(Boolean) as Song[]
      this.queueSongs = rebuilt

      // 如果当前歌曲不在 rebuilt 里，回退到第一首
      if (!this.queueSongs.length) {
        this.currentIndex = 0
        this.currentSongId = 0
        return
      }
      const idx = this.queueSongs.findIndex((s) => s?.id === this.currentSongId)
      if (idx < 0) {
        this.currentIndex = 0
        this.currentSongId = this.queueSongs[0].id
      } else {
        this.currentIndex = idx
      }
    },

    async setQueue(songIds: number[], currentIndex = 0) {
      await this.replaceQueue(songIds, currentIndex)
      // setQueue 在业务上通常表示“立即开始播放该队列中的选中曲目”
      // PlayerView.vue 的首次自动播放依赖 store.playing 状态。
      this.playing = true
    },

    async replaceQueue(songIds: number[], currentIndex = 0) {
      const normalizedQueue = normalizeIds(songIds)
      this.queue = normalizedQueue

      const len = normalizedQueue.length
      if (!len) {
        this.currentIndex = 0
        this.currentSongId = 0
        this.queueSongs = []
        await this.persistQueue()
        return
      }

      const idx = clamp(Number(currentIndex || 0), 0, len - 1)
      this.currentIndex = idx
      this.currentSongId = normalizedQueue[idx] || 0

      await this.persistQueue()
      await this.refreshQueueSongs()
    },

    async playBySongId(songId: number) {
      const targetId = Number(songId)
      if (!targetId || !Number.isFinite(targetId)) return

      if (!this.queue.length) {
        await this.replaceQueue([targetId], 0)
      } else {
        const idx = this.queue.findIndex((id) => Number(id) === targetId)
        if (idx < 0) {
          this.queue.push(targetId)
          this.currentIndex = this.queue.length - 1
          this.currentSongId = targetId
          await this.persistQueue()
          await this.refreshQueueSongs()
        } else {
          this.currentIndex = idx
          this.currentSongId = targetId
          await this.persistQueue()
          await this.refreshQueueSongs()
        }
      }
      // playBySongId 在业务上表示“播放该歌曲”，
      // PlayerView.vue 的 shouldAutoplayOnInit() 依赖 store.playing。
      this.playing = true
      await addPlayHistoryApi(targetId, 0)
    },

    async appendSongs(songIds: number[]) {
      const ids = normalizeIds(songIds)
      if (!ids.length) return
      const exists = new Set(this.queue.map((id) => Number(id)))
      const toAppend = ids.filter((id) => !exists.has(id))
      if (!toAppend.length) return
      this.queue.push(...toAppend)
      await this.persistQueue()
      await this.refreshQueueSongs()
    },

    async addToQueueTail(songId: number) {
      await this.appendSongs([songId])
    },

    async playNextBatch(songIds: number[]) {
      const ids = normalizeIds(songIds)
      if (!ids.length) return
      if (!this.queue.length) {
        await this.replaceQueue(ids, 0)
        return
      }

      const targetIds = ids.filter((id) => id !== this.currentSongId)
      if (!targetIds.length) return

      // 先从队列中移除可能的重复，避免“下一曲”堆很多同一首
      const set = new Set(targetIds)
      this.queue = this.queue.filter((id) => id === this.currentSongId || !set.has(id))

      // 重新定位 currentIndex/currentSongId（filter 后索引可能变化）
      const idx = this.queue.findIndex((id) => id === this.currentSongId)
      this.currentIndex = idx >= 0 ? idx : 0
      this.currentSongId = this.queue[this.currentIndex] || this.currentSongId

      const insertPos = this.currentIndex + 1
      this.queue.splice(insertPos, 0, ...targetIds)

      await this.persistQueue()
      await this.refreshQueueSongs()
    },

    async playNext(songId: number) {
      await this.playNextBatch([songId])
    },

    async removeFromQueue(songId: number) {
      const id = Number(songId)
      if (!id || !Number.isFinite(id)) return
      if (!this.queue.length) return

      const idx = this.queue.findIndex((x) => Number(x) === id)
      if (idx < 0) return

      const wasCurrent = idx === this.currentIndex

      this.queue.splice(idx, 1)
      if (!this.queue.length) {
        this.currentIndex = 0
        this.currentSongId = 0
        this.queueSongs = []
        this.playing = false
        this.currentTime = 0
        await this.persistQueue()
        return
      }

      if (idx < this.currentIndex) this.currentIndex -= 1

      if (wasCurrent) {
        if (this.currentIndex >= this.queue.length) this.currentIndex = this.queue.length - 1
      }

      this.currentSongId = this.queue[this.currentIndex] || 0

      await this.persistQueue()
      await this.refreshQueueSongs()
    },

    async clearQueue() {
      this.queue = []
      this.queueSongs = []
      this.currentIndex = 0
      this.currentSongId = 0
      this.playing = false
      this.currentTime = 0
      await this.persistQueue()
    },

    setPlayMode(mode: PlayMode) {
      this.playMode = mode
    },

    async nextByMode() {
      if (!this.queue.length) return false
      if (!this.queueSongs.length) await this.refreshQueueSongs()
      const len = this.queue.length
      if (!len) return false

      // 单曲循环：由播放器自己重播当前曲
      if (this.playMode === 'single') return true

      if (this.playMode === 'random') {
        if (len === 1) return true
        let nextIndex = this.currentIndex
        let attempts = 0
        while (nextIndex === this.currentIndex && attempts < 8) {
          nextIndex = Math.floor(Math.random() * len)
          attempts += 1
        }
        this.currentIndex = nextIndex
        this.currentSongId = this.queue[this.currentIndex] || 0
        await this.persistQueue()
        return true
      }

      if (this.playMode === 'order') {
        if (this.currentIndex >= len - 1) return false
        this.currentIndex += 1
        this.currentSongId = this.queue[this.currentIndex] || 0
        await this.persistQueue()
        return true
      }

      // loop
      this.currentIndex = (this.currentIndex + 1) % len
      this.currentSongId = this.queue[this.currentIndex] || 0
      await this.persistQueue()
      return true
    },

    async prev() {
      if (!this.queue.length) return
      if (!this.queueSongs.length) await this.refreshQueueSongs()
      const len = this.queue.length
      if (!len) return

      if (this.playMode === 'single') return

      if (this.playMode === 'random') {
        if (len === 1) return
        let prevIndex = this.currentIndex
        let attempts = 0
        while (prevIndex === this.currentIndex && attempts < 8) {
          prevIndex = Math.floor(Math.random() * len)
          attempts += 1
        }
        this.currentIndex = prevIndex
      } else if (this.playMode === 'order') {
        if (this.currentIndex <= 0) {
          this.playing = false
          this.currentTime = 0
          return
        }
        this.currentIndex -= 1
      } else {
        // loop
        this.currentIndex = (this.currentIndex - 1 + len) % len
      }

      this.currentSongId = this.queue[this.currentIndex] || 0
      await this.persistQueue()
    },

    async next() {
      const shouldContinue = await this.nextByMode()
      if (!shouldContinue) {
        // order 播放到末尾
        this.playing = false
        this.currentTime = 0
      }
    }
  }
})
