export const COMPETITION_PLAYER_TOKEN_KEY = 'competition_player_token'
export const COMPETITION_PLAYER_INFO_KEY = 'competition_player_info'
export const COMPETITION_PLAYER_TENANT_KEY = 'competition_player_tenant_id'
export const COMPETITION_PLAYER_ENTRY_KEY = 'competition_player_entry_code'

export function getCompetitionPlayerToken() {
  const token = uni.getStorageSync(COMPETITION_PLAYER_TOKEN_KEY)
  return typeof token === 'string' && token.trim() ? token : ''
}

export function saveCompetitionPlayerSession(token, playerInfo = null, tenantId = null) {
  if (typeof token !== 'string' || !token.trim()) {
    throw new Error('Competition player token is required')
  }
  uni.setStorageSync(COMPETITION_PLAYER_TOKEN_KEY, token)
  if (playerInfo && typeof playerInfo === 'object') {
    uni.setStorageSync(COMPETITION_PLAYER_INFO_KEY, playerInfo)
  }
  if (tenantId) uni.setStorageSync(COMPETITION_PLAYER_TENANT_KEY, tenantId)
}

export function getStoredCompetitionPlayerInfo() {
  const playerInfo = uni.getStorageSync(COMPETITION_PLAYER_INFO_KEY)
  return playerInfo && typeof playerInfo === 'object' ? playerInfo : null
}

export function clearCompetitionPlayerSession() {
  uni.removeStorageSync(COMPETITION_PLAYER_TOKEN_KEY)
  uni.removeStorageSync(COMPETITION_PLAYER_INFO_KEY)
  uni.removeStorageSync(COMPETITION_PLAYER_TENANT_KEY)
}

export function isCompetitionPlayerLoggedIn() {
  return Boolean(getCompetitionPlayerToken())
}

export function getCompetitionPlayerTenantId() {
  const tenantId = uni.getStorageSync(COMPETITION_PLAYER_TENANT_KEY)
  return tenantId ? Number(tenantId) : null
}

export function saveCompetitionPlayerEntry(entryCode, tenantId = null) {
  if (entryCode) uni.setStorageSync(COMPETITION_PLAYER_ENTRY_KEY, entryCode)
  if (tenantId) uni.setStorageSync(COMPETITION_PLAYER_TENANT_KEY, tenantId)
}

export function getCompetitionPlayerEntry() {
  return uni.getStorageSync(COMPETITION_PLAYER_ENTRY_KEY) || ''
}

export function handleCompetitionPlayerSessionExpired() {
  clearCompetitionPlayerSession()
  redirectToCompetitionLogin()
}

export function redirectToCompetitionLogin() {
  uni.reLaunch({ url: '/pages/competition-app/login' })
}
