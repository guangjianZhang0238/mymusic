import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  { path: '/login', component: () => import('@/views/LoginView.vue'), meta: { title: '登录', public: true } },
  { path: '/', redirect: '/home' },
  { path: '/home', component: () => import('@/views/HomeView.vue'), meta: { title: '首页' } },
  { path: '/discover', component: () => import('@/views/DiscoverView.vue'), meta: { title: '发现' } },
  { path: '/library', component: () => import('@/views/LibraryView.vue'), meta: { title: '歌单' } },
  { path: '/playlist/:id', component: () => import('@/views/PlaylistDetailView.vue'), meta: { title: '歌单详情' } },
  { path: '/player/:songId?', component: () => import('@/views/PlayerView.vue'), meta: { title: '播放器' } },
  { path: '/lyrics/:songId', component: () => import('@/views/LyricsView.vue'), meta: { title: '歌词' } },
  { path: '/profile', component: () => import('@/views/ProfileView.vue'), meta: { title: '个人中心' } },
  { path: '/search', component: () => import('@/views/SearchView.vue'), meta: { title: '搜索' } },
  { path: '/album/:id', component: () => import('@/views/AlbumDetailView.vue'), meta: { title: '专辑详情' } },
  { path: '/artist/:id', component: () => import('@/views/ArtistDetailView.vue'), meta: { title: '歌手详情' } },
  { path: '/settings', component: () => import('@/views/SettingsView.vue'), meta: { title: '设置' } },
  { path: '/feedback', component: () => import('@/views/FeedbackView.vue'), meta: { title: '反馈' } },
  { path: '/lyrics-share', component: () => import('@/views/LyricsShareView.vue'), meta: { title: '歌词分享' } },
  { path: '/comments/:songId', component: () => import('@/views/CommentsView.vue'), meta: { title: '评论' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  document.title = `${String(to.meta.title || 'Music Web')} - 音乐网站`
  const user = useUserStore()
  if (to.meta.public) return true
  if (!user.isLoggedIn) return '/login'
  return true
})

export default router
