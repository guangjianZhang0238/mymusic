import request from './request'

export interface UserSettingItem {
  settingKey: string
  settingValue: string
  settingType?: string
  description?: string
}

export const getUserSettingsApi = () => request.get<UserSettingItem[]>('/app/user-setting')

export const saveUserSettingApi = (settingKey: string, settingValue: string, settingType = 'string', description = '') =>
  request.post<UserSettingItem>('/app/user-setting', null, { params: { settingKey, settingValue, settingType, description } })

export const batchSaveUserSettingsApi = (settings: UserSettingItem[]) =>
  request.post<void>('/app/user-setting/batch', settings)
