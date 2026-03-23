import axios, { type AxiosInstance, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

interface ApiResult<T> {
  code: number
  message?: string
  data: T
}

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

service.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  if (config.data instanceof FormData) {
    delete config.headers['Content-Type']
  }
  return config
})

service.interceptors.response.use(
  (response: AxiosResponse<ApiResult<any>>) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || 'Request failed'))
    }
    return res.data
  },
  (error) => {
    if (error?.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      router.push('/login')
    }
    ElMessage.error(error?.message || '网络错误')
    return Promise.reject(error)
  }
)

export default {
  get: <T = unknown>(url: string, config?: object) => service.get<any, T>(url, config),
  post: <T = unknown>(url: string, data?: unknown, config?: object) => service.post<any, T>(url, data, config),
  put: <T = unknown>(url: string, data?: unknown, config?: object) => service.put<any, T>(url, data, config),
  delete: <T = unknown>(url: string, config?: object) => service.delete<any, T>(url, config)
}
