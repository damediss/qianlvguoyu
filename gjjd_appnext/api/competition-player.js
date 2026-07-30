import {
  getCompetitionPlayerTenantId,
  getCompetitionPlayerToken,
  handleCompetitionPlayerSessionExpired,
} from '@/utils/competition-auth'
import util from '@/utils/util'

const API_BASE_URL = (() => {
  // 浏览器 H5 由 Vite 的 /api/competition/player 代理转发到本机 platform-api。
  // #ifdef H5
  return '/api'
  // #endif

  // 原生 App 没有 Vite 代理，必须直接访问可被手机访问的后端地址。
  // #ifdef APP-PLUS
  return util.competitionPlayerApi[util.competitionPlayerApiMode] || ''
  // #endif

  return '/api'
})()

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
        if (String(body.code) !== '0') {
          reject(new Error(body.message || '选手端请求失败'))
          return
        }
        resolve(body.data)
      },
      fail: () => reject(new Error('Competition player network request failed')),
    })
  })
}

function playerAssetHeaders() {
  const headers = {}
  const token = getCompetitionPlayerToken()
  const tenantId = getCompetitionPlayerTenantId()
  if (token) headers.Authorization = `Bearer ${token}`
  if (tenantId) headers['X-Tenant-Id'] = String(tenantId)
  return headers
}

/**
 * Fetch a notice asset through the player-authorized endpoint.  The asset is never exposed as a
 * storage URL: H5 receives a short-lived Blob URL; native clients receive a temporary file path.
 */
export const getPlayerMessageAsset = (messageId, assetId) => {
  const url = `${API_BASE_URL}/competition/player/messages/${messageId}/assets/${assetId}`
  const header = playerAssetHeaders()

  // #ifdef H5
  return new Promise((resolve, reject) => {
    uni.request({
      url,
      method: 'GET',
      header,
      responseType: 'arraybuffer',
      success: (response) => {
        if (response.statusCode === 401) handleCompetitionPlayerSessionExpired()
        if (response.statusCode < 200 || response.statusCode >= 300) {
          reject(new Error(response.data?.message || `公告附件加载失败（${response.statusCode}）`))
          return
        }
        const contentType = response.header?.['content-type'] || response.header?.['Content-Type'] || 'application/octet-stream'
        const blobUrl = window.URL.createObjectURL(new Blob([response.data], { type: contentType }))
        resolve({ url: blobUrl, contentType, revoke: () => window.URL.revokeObjectURL(blobUrl) })
      },
      fail: () => reject(new Error('公告附件加载失败，请检查网络')),
    })
  })
  // #endif

  // #ifndef H5
  return new Promise((resolve, reject) => {
    uni.downloadFile({
      url,
      header,
      success: (response) => {
        if (response.statusCode === 401) handleCompetitionPlayerSessionExpired()
        if (response.statusCode < 200 || response.statusCode >= 300) {
          reject(new Error(`公告附件加载失败（${response.statusCode}）`))
          return
        }
        resolve({ url: response.tempFilePath, contentType: response.header?.['content-type'] || '' })
      },
      fail: () => reject(new Error('公告附件加载失败，请检查网络')),
    })
  })
  // #endif
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
