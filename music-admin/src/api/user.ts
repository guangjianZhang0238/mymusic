import request from '@/api/request'
import type { PageResult, LoginDTO, LoginVO } from '@/api/types'

// 用户登录
export const login = (data: LoginDTO) => {
  return request.post<LoginVO>('/auth/login', data)
}

// 用户登出
export const logout = () => {
  return request.post('/auth/logout')
}

export interface UserManageVO {
  id: number
  username: string
  nickname: string
  avatar: string
  phone: string
  email: string
  role: number
  status: number
  createTime: string
  lastLoginTime: string
  loginCount: number
  totalOnlineTime: number
  currentSessionStart: string
  currentSessionDuration: number
  totalPlayCount: number
  lastPlayTime: string
  isOnline: boolean
}

export interface UserPageQuery {
  current?: number
  size?: number
  username?: string
  status?: number
  role?: number
  onlineStatus?: number
}

// 分页查询用户列表
export const getUserPage = (params: UserPageQuery) => {
  return request.get<PageResult<UserManageVO>>('/admin/users/page', { params })
}

// 获取用户详情
export const getUserDetail = (userId: number) => {
  return request.get<UserManageVO>(`/admin/users/${userId}`)
}

// 修改用户密码
export const updateUserPassword = (userId: number, newPassword: string) => {
  return request.put(`/admin/users/${userId}/password`, { newPassword })
}

// 禁用用户
export const disableUser = (userId: number) => {
  return request.put(`/admin/users/${userId}/disable`)
}

// 启用用户
export const enableUser = (userId: number) => {
  return request.put(`/admin/users/${userId}/enable`)
}

// 强制用户下线
export const forceLogoutUser = (userId: number) => {
  return request.post(`/admin/users/${userId}/force-logout`)
}

// 获取在线用户列表
export const getOnlineUsers = () => {
  return request.get<UserManageVO[]>('/admin/users/online')
}

// 获取用户统计信息
export const getUserStats = (userId: number) => {
  return request.get<UserManageVO>(`/admin/users/${userId}/stats`)
}