<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const user = useUserStore()
const mode = ref<'login' | 'register'>('login')
const loading = ref(false)
const form = reactive({ username: '', password: '', confirmPassword: '' })

const submit = async () => {
  if (!form.username || !form.password) return ElMessage.warning('请输入账号和密码')
  if (mode.value === 'register' && form.password !== form.confirmPassword) return ElMessage.warning('两次密码不一致')
  loading.value = true
  try {
    if (mode.value === 'register') {
      await user.register(form.username, form.password)
    }
    await user.login(form.username, form.password)
    router.push('/home')
    ElMessage.success(mode.value === 'login' ? '登录成功' : '注册并登录成功')
  } catch (e: any) {
    ElMessage.error(e?.message || (mode.value === 'login' ? '登录失败' : '注册失败'))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-wrap">
    <el-card class="login-card glow-card">
      <h2 class="page-title">{{ mode === 'login' ? '登录' : '注册' }}</h2>
      <el-input v-model="form.username" placeholder="用户名" />
      <el-input v-model="form.password" type="password" placeholder="密码" style="margin-top: 12px" />
      <el-input
        v-if="mode === 'register'"
        v-model="form.confirmPassword"
        type="password"
        placeholder="确认密码"
        style="margin-top: 12px"
      />
      <el-button :loading="loading" type="primary" style="margin-top: 12px; width: 100%" @click="submit">{{ mode === 'login' ? '登录' : '注册并登录' }}</el-button>
      <el-button text style="margin-top: 8px" @click="mode = mode === 'login' ? 'register' : 'login'">
        {{ mode === 'login' ? '没有账号，去注册' : '已有账号，去登录' }}
      </el-button>
    </el-card>
  </div>
</template>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: radial-gradient(circle at top right, rgba(125, 211, 252, 0.25), transparent 50%),
    radial-gradient(circle at bottom left, rgba(244, 114, 182, 0.25), transparent 45%),
    #f6f7ff;
  padding: 24px;
}

.login-card {
  width: 360px;
}

@media (max-width: 960px) {
  .login-wrap {
    min-height: calc(100dvh - env(safe-area-inset-bottom));
    padding: 12px;
  }

  .login-card {
    width: min(96vw, 420px);
  }
}
</style>
