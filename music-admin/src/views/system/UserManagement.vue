<template>
  <div class="user-management">
    <el-card class="search-card">
      <el-form :model="searchForm" :inline="true" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchForm.role" placeholder="请选择角色" clearable>
            <el-option label="管理员" :value="1" />
            <el-option label="普通用户" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="在线状态">
          <el-select v-model="searchForm.onlineStatus" placeholder="请选择在线状态" clearable>
            <el-option label="在线" :value="1" />
            <el-option label="离线" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleExport">导出</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>用户列表</span>
        </div>
      </template>

      <el-table :data="userList" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 1 ? 'danger' : 'success'">
              {{ row.role === 1 ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="loginCount" label="登录次数" width="100" />
        <el-table-column prop="totalOnlineTime" label="在线时长" width="120">
          <template #default="{ row }">
            {{ formatDuration(row.totalOnlineTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="totalPlayCount" label="播放次数" width="100" />
        <el-table-column prop="isOnline" label="在线状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isOnline ? 'success' : 'info'">
              {{ row.isOnline ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="180">
          <template #default="{ row }">
            {{ row.lastLoginTime ? formatDate(row.lastLoginTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleViewStats(row)">
              统计
            </el-button>
            <el-button type="warning" size="small" @click="handleEditPassword(row)">
              改密
            </el-button>
            <el-button 
              :type="row.status === 1 ? 'danger' : 'success'" 
              size="small" 
              @click="handleChangeStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button type="danger" size="small" @click="handleForceLogout(row)">
              下线
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="pagination.total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="400px">
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="80px">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPasswordChange" :loading="passwordLoading">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 用户统计详情对话框 -->
    <el-dialog v-model="statsDialogVisible" title="用户统计详情" width="600px">
      <div v-if="currentUserStats" class="stats-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户名">{{ currentUserStats.username }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ currentUserStats.nickname }}</el-descriptions-item>
          <el-descriptions-item label="登录次数">{{ currentUserStats.loginCount }}</el-descriptions-item>
          <el-descriptions-item label="在线时长">{{ formatDuration(currentUserStats.totalOnlineTime) }}</el-descriptions-item>
          <el-descriptions-item label="播放次数">{{ currentUserStats.totalPlayCount }}</el-descriptions-item>
          <el-descriptions-item label="当前会话时长">{{ formatDuration(currentUserStats.currentSessionDuration) }}</el-descriptions-item>
          <el-descriptions-item label="最后登录时间" :span="2">
            {{ currentUserStats.lastLoginTime ? formatDate(currentUserStats.lastLoginTime) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="最后播放时间" :span="2">
            {{ currentUserStats.lastPlayTime ? formatDate(currentUserStats.lastPlayTime) : '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, FormInstance } from 'element-plus'
import * as userApi from '@/api/user'

interface UserItem {
  id: number
  username: string
  nickname: string
  phone: string
  email: string
  role: number
  status: number
  loginCount: number
  totalOnlineTime: number
  totalPlayCount: number
  isOnline: boolean
  lastLoginTime: string
  createTime: string
  currentSessionDuration: number
  lastPlayTime: string
}

// 搜索表单
const searchForm = reactive({
  username: '',
  status: undefined as number | undefined,
  role: undefined as number | undefined,
  onlineStatus: undefined as number | undefined
})

// 分页
const pagination = reactive({
  current: 1,
  size: 20,
  total: 0
})

// 数据
const loading = ref(false)
const userList = ref<UserItem[]>([])

// 密码修改对话框
const passwordDialogVisible = ref(false)
const passwordLoading = ref(false)
const passwordForm = reactive({
  userId: 0,
  newPassword: ''
})
const passwordFormRef = ref<FormInstance>()

const passwordRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

// 统计详情对话框
const statsDialogVisible = ref(false)
const currentUserStats = ref<UserItem | null>(null)

// 格式化日期
const formatDate = (date: string) => {
  return new Date(date).toLocaleString('zh-CN')
}

// 格式化时长
const formatDuration = (seconds: number) => {
  if (!seconds) return '0秒'
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60
  
  if (hours > 0) {
    return `${hours}小时${minutes}分钟${secs}秒`
  } else if (minutes > 0) {
    return `${minutes}分钟${secs}秒`
  } else {
    return `${secs}秒`
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  loadUserList()
}

// 重置
const handleReset = () => {
  searchForm.username = ''
  searchForm.status = undefined
  searchForm.role = undefined
  searchForm.onlineStatus = undefined
  pagination.current = 1
  loadUserList()
}

// 导出用户数据
const handleExport = async () => {
  try {
    loading.value = true
    // 这里可以调用导出API
    const exportData = userList.value.map(user => ({
      ID: user.id,
      用户名: user.username,
      昵称: user.nickname,
      手机号: user.phone,
      邮箱: user.email,
      角色: user.role === 1 ? '管理员' : '普通用户',
      状态: user.status === 1 ? '启用' : '禁用',
      登录次数: user.loginCount,
      在线时长: formatDuration(user.totalOnlineTime),
      播放次数: user.totalPlayCount,
      在线状态: user.isOnline ? '在线' : '离线',
      最后登录: user.lastLoginTime ? formatDate(user.lastLoginTime) : '-',
      创建时间: formatDate(user.createTime)
    }))
    
    // 创建CSV内容
    const csvContent = [
      Object.keys(exportData[0]).join(','),
      ...exportData.map(row => Object.values(row).join(','))
    ].join('\n')
    
    // 创建下载链接
    const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    const url = URL.createObjectURL(blob)
    link.setAttribute('href', url)
    link.setAttribute('download', `用户数据_${new Date().toISOString().slice(0, 10)}.csv`)
    link.style.visibility = 'hidden'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    
    ElMessage.success('导出成功')
  } catch (error: any) {
    ElMessage.error(error.message || '导出失败')
  } finally {
    loading.value = false
  }
}

// 分页变化
const handleSizeChange = (val: number) => {
  pagination.size = val
  loadUserList()
}

const handleCurrentChange = (val: number) => {
  pagination.current = val
  loadUserList()
}

// 查看统计
const handleViewStats = async (row: UserItem) => {
  try {
    const stats = await userApi.getUserStats(row.id)
    currentUserStats.value = stats
    statsDialogVisible.value = true
  } catch (error: any) {
    ElMessage.error(error.message || '获取用户统计信息失败')
  }
}

// 修改密码
const handleEditPassword = (row: UserItem) => {
  passwordForm.userId = row.id
  passwordForm.newPassword = ''
  passwordDialogVisible.value = true
}

// 提交密码修改
const submitPasswordChange = async () => {
  if (!passwordFormRef.value) return
  
  try {
    await passwordFormRef.value.validate()
    passwordLoading.value = true
    
    await userApi.updateUserPassword(passwordForm.userId, passwordForm.newPassword)
    
    ElMessage.success('密码修改成功')
    passwordDialogVisible.value = false
  } catch (error: any) {
    ElMessage.error(error.message || '密码修改失败')
  } finally {
    passwordLoading.value = false
  }
}

// 更改用户状态
const handleChangeStatus = async (row: UserItem) => {
  try {
    await ElMessageBox.confirm(
      `确定要${row.status === 1 ? '禁用' : '启用'}用户"${row.username}"吗？`,
      '提示',
      {
        type: 'warning'
      }
    )
    
    if (row.status === 1) {
      await userApi.disableUser(row.id)
    } else {
      await userApi.enableUser(row.id)
    }
    
    ElMessage.success(`${row.status === 1 ? '禁用' : '启用'}成功`)
    loadUserList()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

// 强制下线
const handleForceLogout = async (row: UserItem) => {
  try {
    await ElMessageBox.confirm(
      `确定要强制用户"${row.username}"下线吗？`,
      '警告',
      {
        type: 'warning'
      }
    )
    
    await userApi.forceLogoutUser(row.id)
    
    ElMessage.success('强制下线成功')
    loadUserList()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '强制下线失败')
    }
  }
}

// 加载用户列表
const loadUserList = async () => {
  loading.value = true
  try {
    const response = await userApi.getUserPage({
      current: pagination.current,
      size: pagination.size,
      username: searchForm.username,
      status: searchForm.status,
      role: searchForm.role,
      onlineStatus: searchForm.onlineStatus
    })
    userList.value = response.records
    pagination.total = response.total
  } catch (error: any) {
    ElMessage.error(error.message || '获取用户列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadUserList()
})
</script>

<style scoped>
.user-management {
  padding: 20px;
}

.search-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.stats-content {
  padding: 20px 0;
}
</style>