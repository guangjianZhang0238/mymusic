import { defineStore } from 'pinia'
import { addPlayHistoryApi, getPlaybackListApi, savePlaybackListApi } from '@/api/player'
import { getSongsByIdsApi } from '@/api/music'
import type { Song } from '@/api/types'

export const usePlayerStore = defineStore('player', {
  state: () => ({
    queue: [] as number[],
    queueSongs: [] as Song[],
    currentIndex: 0,
    currentSongId: 0,
    playing: false,
    currentTime: 0
  }),
  getters: {
    currentSong: (state) => state.queueSongs.find((item) => item.id === state.currentSongId)
  },
  actions: {
    async hydrateFromServer() {
      const data = await getPlaybackListApi()
      // 兼容后端返回为字符串/混合类型的情况，确保 index 匹配正确
      const normalizedQueue = (data.songIds || []).map((id) => Number(id)).filter((n) => Number.isFinite(n))
      this.queue = normalizedQueue
      this.currentIndex = Math.max(Number(data.currentIndex || 0), 0)
      this.currentSongId = this.queue[this.currentIndex] || 0
      await this.refreshQueueSongs()
    },
    async refreshQueueSongs() {
      if (!this.queue.length) {
        this.queueSongs = []
        return
      }
      this.queueSongs = await getSongsByIdsApi(this.queue)
    },
    async setQueue(songIds: number[], currentIndex = 0) {
      // 确保队列都是 number，避免 indexOf 因类型不一致返回 -1
      const normalizedQueue = (songIds || []).map((id) => Number(id)).filter((n) => Number.isFinite(n))
      const idx = Math.max(Number(currentIndex || 0), 0)
      this.queue = normalizedQueue
      this.currentIndex = idx
      this.currentSongId = normalizedQueue[idx] || 0
      await savePlaybackListApi(normalizedQueue, idx)
      await this.refreshQueueSongs()
    },
    async playBySongId(songId: number) {
      const targetId = Number(songId)
      if (!targetId || !Number.isFinite(targetId)) return

      if (!this.queue.length) {
        await this.setQueue([targetId], 0)
      } else {
        const idx = this.queue.findIndex((id) => Number(id) === targetId)
        if (idx < 0) {
          // 队列里找不到该歌曲时，直接重建队列，避免播放跳错
          await this.setQueue([targetId], 0)
        } else {
          this.currentIndex = idx
          this.currentSongId = targetId
          await savePlaybackListApi(this.queue, this.currentIndex)
        }
      }
      await addPlayHistoryApi(targetId, 0)
    },
    async prev() {
      if (!this.queue.length) return
      this.currentIndex = (this.currentIndex - 1 + this.queue.length) % this.queue.length
      this.currentSongId = this.queue[this.currentIndex]
      await savePlaybackListApi(this.queue, this.currentIndex)
    },
    async next() {
      if (!this.queue.length) return
      this.currentIndex = (this.currentIndex + 1) % this.queue.length
      this.currentSongId = this.queue[this.currentIndex]
      await savePlaybackListApi(this.queue, this.currentIndex)
    }
  }
})
