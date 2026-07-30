/**
 * App 端 token helpers.
 *
 * The upstream repository imports this module from the request layer, the
 * Pinia user store and the login page, but the file is absent from the
 * current Gitee commit. Keep the fallback conservative: a token is reused
 * only when it is present, and expired credentials are cleared instead of
 * silently replaying a password.
 */

let cachedPassword = ''

export function isValidToken(token) {
  return typeof token === 'string' && token.trim().length >= 10
}

export function setCachedPassword(password) {
  // Memory-only cache; do not persist credentials in uni storage.
  cachedPassword = typeof password === 'string' ? password : ''
}

export function getCachedPassword() {
  return cachedPassword
}

export function clearLocalCredentials() {
  ;['itoken', 'userId', 'userInfo', 'clientId', 'currentMatchId'].forEach((key) => {
    uni.removeStorageSync(key)
  })
}

export function isLoginExpired(response) {
  if (!response || typeof response !== 'object') return false
  const code = String(response.code ?? '').toLowerCase()
  const message = String(response.msg ?? response.message ?? '')
  return ['401', '403', '4010', '1001', '1002'].includes(code)
    || /未登录|登录过期|token.*(过期|失效|无效)|无效.*token/i.test(message)
}

export function isKickedOut(message) {
  return /其他设备|异地登录|被踢|已在.*登录/i.test(String(message || ''))
}

export async function ensureToken() {
  const token = uni.getStorageSync('itoken')
  return isValidToken(token) ? token : null
}

export async function handleTokenExpired() {
  clearLocalCredentials()
  return false
}

export function updateTokenInUrl(url, token) {
  if (!url || !isValidToken(token)) return url
  const separator = url.includes('?') ? '&' : '?'
  return `${url}${separator}istate=${encodeURIComponent(token)}`
}

export function rebindWebSocket() {
  return Promise.resolve(false)
}
