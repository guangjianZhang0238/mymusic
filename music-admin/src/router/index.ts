import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('../views/dashboard/Dashboard.vue'),
    meta: { title: '仪表盘' }
  },
  {
    path: '/content',
    name: 'Content',
    redirect: '/content/artist',
    children: [
      {
        path: 'artist',
        name: 'Artist',
        component: () => import('../views/content/artist/ArtistList.vue'),
        meta: { title: '歌手管理' }
      },
      {
        path: 'album',
        name: 'Album',
        component: () => import('../views/content/album/AlbumList.vue'),
        meta: { title: '专辑管理' }
      },
      {
        path: 'song',
        name: 'Song',
        component: () => import('../views/content/song/SongList.vue'),
        meta: { title: '歌曲管理' }
      },
      {
        path: 'lyrics',
        name: 'Lyrics',
        component: () => import('../views/content/lyrics/LyricsList.vue'),
        meta: { title: '歌词管理' }
      }
    ]
  },
  {
    path: '/upload',
    name: 'Upload',
    component: () => import('../views/upload/FileUpload.vue'),
    meta: { title: '文件上传' }
  },
  {
    path: '/system',
    name: 'System',
    component: () => import('../views/system/System.vue'),
    meta: { title: '系统管理' }
  },
  {
    path: '/users',
    name: 'UserManagement',
    component: () => import('../views/system/UserManagement.vue'),
    meta: { title: '用户管理' }
  },
  {
    path: '/feedback',
    name: 'Feedback',
    component: () => import('../views/system/FeedbackList.vue'),
    meta: { title: '用户反馈' }
  },
  {
    path: '/test-encoding',
    name: 'TestEncoding',
    component: () => import('../views/TestEncoding.vue'),
    meta: { title: '字符编码测试' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = `${to.meta.title || '音乐管理系统'} - 后台管理`
  
  // 登录拦截
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
