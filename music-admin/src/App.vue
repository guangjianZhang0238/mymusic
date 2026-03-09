<template>
  <el-container class="app-container">
    <el-header v-if="$route.path !== '/login'" height="60px">
      <div class="header-content">
        <div class="logo">音乐管理系统</div>
        <div class="header-right">
          <el-dropdown>
            <span class="user-info">
              <el-avatar size="small">{{ userInfo?.nickname?.[0] || '管' }}</el-avatar>
              <span>{{ userInfo?.nickname || '管理员' }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>
    <el-container>
      <el-aside v-if="$route.path !== '/login'" width="200px" class="sidebar">
        <el-menu
          :default-active="activeMenu"
          class="sidebar-menu"
          router
        >
          <el-menu-item index="/dashboard">
            <template #icon>
              <el-icon><House /></el-icon>
            </template>
            <span>仪表盘</span>
          </el-menu-item>
          <el-sub-menu index="/content">
            <template #title>
              <el-icon><Menu /></el-icon>
              <span>内容管理</span>
            </template>
            <el-menu-item index="/content/artist">歌手管理</el-menu-item>
            <el-menu-item index="/content/album">专辑管理</el-menu-item>
            <el-menu-item index="/content/song">歌曲管理</el-menu-item>
            <el-menu-item index="/content/lyrics">歌词管理</el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/upload">
            <template #icon>
              <el-icon><Upload /></el-icon>
            </template>
            <span>文件上传</span>
          </el-menu-item>
          <el-menu-item index="/system">
            <template #icon>
              <el-icon><Setting /></el-icon>
            </template>
            <span>系统管理</span>
          </el-menu-item>
          <el-menu-item index="/users">
            <template #icon>
              <el-icon><User /></el-icon>
            </template>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/feedback">
            <template #icon>
              <el-icon><ChatDotRound /></el-icon>
            </template>
            <span>用户反馈</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main>
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from './stores/user'
import { House, Menu, Upload, Setting, ChatDotRound, User } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const userInfo = computed(() => userStore.userInfo)

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/content/')) {
    return '/content'
  }
  return path
})

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-container {
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
  padding: 0 20px;
  background: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.logo {
  font-size: 18px;
  font-weight: bold;
  color: #409EFF;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.sidebar {
  background: #f0f2f5;
  border-right: 1px solid #e4e7ed;
}

.sidebar-menu {
  height: 100%;
  border-right: none;
}

.el-container:not(.is-vertical) {
  flex: 1;
  overflow: hidden;
}

.el-main {
  overflow-y: auto;
  padding: 20px;
  height: calc(100vh - 60px);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
