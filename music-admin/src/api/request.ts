import axios, { type AxiosInstance, type AxiosResponse } from 'axios'

// 定义类型以避免 TypeScript 错误
declare global {
  interface ImportMeta {
    env?: {
      VITE_API_BASE_URL?: string
    }
  }
}

const service: AxiosInstance = axios.create({
  baseURL: (import.meta as any).env?.VITE_API_BASE_URL || '/api',
  timeout: 600000,  // 10分钟超时
  // 大文件上传支持 - 移除默认的内容长度限制
  maxContentLength: Infinity,
  maxBodyLength: Infinity,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
    'Accept': 'application/json;charset=UTF-8'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // FormData 上传时不能强制 application/json，否则会导致后端识别不到 multipart 请求
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type']
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<any>) => {
    const res = response.data
    console.log('原始响应:', res)
    if (res.code !== 200) {
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    // 只返回data字段，这样前端可以直接使用records和total
    return res.data
  },
  (error) => {
    console.error('响应错误:', error)
    return Promise.reject(error)
  }
)

// 重写方法以确保返回正确的类型
export default {
  get: <T = any>(url: string, config?: any): Promise<T> => {
    return service.get(url, config)
  },
  post: <T = any>(url: string, data?: any, config?: any): Promise<T> => {
    return service.post(url, data, config)
  },
  put: <T = any>(url: string, data?: any, config?: any): Promise<T> => {
    return service.put(url, data, config)
  },
  delete: <T = any>(url: string, config?: any): Promise<T> => {
    return service.delete(url, config)
  }
}
