import request from './request'

export const getUserSettingsApi = () => request.get<any[]>('/app/user-setting')
export const saveUserSettingApi = (settingKey: string, settingValue: string, settingType = 'string', description = '') =>
  request.post<any>('/app/user-setting', null, { params: { settingKey, settingValue, settingType, description } })
