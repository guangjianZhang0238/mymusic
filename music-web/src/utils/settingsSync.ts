import type { UserSettingItem } from '@/api/settings'

export const USER_SETTINGS_UPDATED_EVENT = 'music-web:user-settings-updated'

export const emitUserSettingsUpdated = (items: UserSettingItem[]) => {
  if (typeof window === 'undefined') return
  window.dispatchEvent(
    new CustomEvent(USER_SETTINGS_UPDATED_EVENT, {
      detail: {
        items: Array.isArray(items) ? items : []
      }
    })
  )
}
