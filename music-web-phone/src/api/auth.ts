import request from './request'
import type { LoginResp } from './types'

export const loginApi = (payload: { username: string; password: string }) =>
  request.post<LoginResp>('/app/auth/login', payload)

export const registerApi = (payload: { username: string; password: string }) =>
  request.post<void>('/app/auth/register', payload)

export const currentUserApi = () => request.get<LoginResp | null>('/app/auth/me')
