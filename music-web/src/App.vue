<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const menus = [
  { path: '/home', label: '首页' },
  { path: '/discover', label: '发现' },
  { path: '/library', label: '歌单' },
  { path: '/player', label: '播放器' },
  { path: '/profile', label: '我的' },
  { path: '/settings', label: '设置' },
  { path: '/feedback', label: '反馈' },
  { path: '/lyrics-share', label: '歌词分享' }
]

const go = (path: string) => router.push(path)
const goSearch = () => router.push('/search')
const logout = () => {
  user.logout()
  router.push('/login')
}
</script>

<template>
  <div v-if="route.path !== '/login'" class="layout">
    <header class="top glow-card">
      <div class="brand">Mymusic</div>
      <nav class="nav-wrap">
        <button
          v-for="item in menus"
          :key="item.path"
          class="nav-btn"
          :class="{ active: route.path.startsWith(item.path) }"
          @click="go(item.path)"
        >
          {{ item.label }}
        </button>
      </nav>
      <div class="user-bar">
        <input class="quick-search" placeholder="搜索" readonly @click="goSearch" />
        <span class="user-name">{{ user.userInfo?.nickname || user.userInfo?.username }}</span>
        <button class="nav-btn logout" @click="logout">退出</button>
      </div>
    </header>
    <main class="container"><router-view /></main>
  </div>
  <router-view v-else />
</template>

<style scoped>
.layout {
  min-height: 100vh;
}

.top {
  display: flex;
  align-items: center;
  gap: 14px;
  justify-content: space-between;
  padding: 12px 20px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.28);
  position: sticky;
  top: 0;
  z-index: 10;
  background: rgba(255, 255, 255, 0.84);
  backdrop-filter: blur(10px);
}

.brand {
  font-weight: 700;
  font-size: 18px;
  color: #1f2937;
}

.nav-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.nav-btn {
  border: 1px solid rgba(244, 114, 182, 0.45);
  background: #ffffff;
  color: #ff5a8a;
  padding: 6px 14px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s;
}

.nav-btn:hover,
.nav-btn.active {
  background: linear-gradient(135deg, #ff8fb8, #7dd3fc);
  color: #ffffff;
  border-color: transparent;
  box-shadow: 0 8px 18px rgba(125, 211, 252, 0.3);
}

.user-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-name {
  color: #5f6b7a;
  font-size: 14px;
}

.quick-search {
  width: 220px;
  padding: 8px 12px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  background: rgba(255, 255, 255, 0.9);
  color: #1f2937;
  outline: none;
  cursor: pointer;
}

.quick-search:hover {
  border-color: rgba(125, 211, 252, 0.7);
  box-shadow: 0 10px 22px rgba(125, 211, 252, 0.2);
}

.logout {
  padding-left: 12px;
  padding-right: 12px;
}
</style>
