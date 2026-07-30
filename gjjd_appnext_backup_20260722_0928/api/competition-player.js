import {
  getCompetitionPlayerTenantId,
  getCompetitionPlayerToken,
  handleCompetitionPlayerSessionExpired,
} from '@/utils/competition-auth'

const API_BASE_URL = '/api'

function requestPlayerApi({ method, path, data, publicRequest = false }) {
  const token = getCompetitionPlayerToken()
  const headers = { 'Content-Type': 'application/json' }
  const tenantId = getCompetitionPlayerTenantId()
  if (!publicRequest && token) headers.Authorization = `Bearer ${token}`
  if (tenantId) headers['X-Tenant-Id'] = String(tenantId)

  return new Promise((resolve, reject) => {
    uni.request({
      url: `${API_BASE_URL}${path}`,
      method,
      data,
      header: headers,
      success: (response) => {
        if (response.statusCode === 401 && !publicRequest) {
          handleCompetitionPlayerSessionExpired()
        }
        if (response.statusCode < 200 || response.statusCode >= 300) {
          reject(new Error(response.data?.message || `Competition player request failed (${response.statusCode})`))
          return
        }
        const body = response.data || {}
        if (String(body.code) !== '200') {
          reject(new Error(body.message || '选手端请求失败'))
          return
        }
        resolve(body.data)
      },
      fail: () => reject(new Error('Competition player network request failed')),
    })
  })
}

export const resolvePlayerEntry = (entry) => requestPlayerApi({
  method: 'GET', path: `/competition/player/entry/resolve?entry=${encodeURIComponent(entry)}`, publicRequest: true,
})
export const sendActivationCode = (data) => requestPlayerApi({
  method: 'POST', path: '/competition/player/activation/send-code', data, publicRequest: true,
})
export const verifyActivation = (data) => requestPlayerApi({
  method: 'POST', path: '/competition/player/activation/verify', data, publicRequest: true,
})
export const playerLogin = (data) => requestPlayerApi({
  method: 'POST', path: '/competition/player/auth/login', data, publicRequest: true,
})
export const playerLogout = () => requestPlayerApi({ method: 'POST', path: '/competition/player/auth/logout' })
export const getCurrentPlayer = () => requestPlayerApi({ method: 'GET', path: '/competition/player/auth/me' })
export const getPlayerMessages = (page = 1, pageSize = 20) => requestPlayerApi({
  method: 'GET', path: `/competition/player/messages?page=${page}&pageSize=${pageSize}`,
})
export const getPlayerUnreadCount = () => requestPlayerApi({
  method: 'GET', path: '/competition/player/messages/unread-count',
})
export const markPlayerMessageRead = (messageId) => requestPlayerApi({
  method: 'POST', path: `/competition/player/messages/${messageId}/read`, data: {},
})
