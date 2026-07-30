const COMPETITION_PLAYER_ACTIVATION_PREFIX = 'qianlvguoyu-player://competition/activate'

/**
 * Extracts the opaque competition entry code from the QR payload issued by
 * platform-api. The code only establishes the event/tenant context; it is
 * never treated as an authenticated player session.
 */
export function parseCompetitionPlayerEntry(rawValue) {
  const raw = String(rawValue || '').trim()
  if (!raw) return ''

  // Android 端部分 WebView/JS 引擎没有完整实现 URL，因此不用 URL 或
  // URLSearchParams 解析自定义 Scheme 的二维码。
  if (!raw.startsWith(COMPETITION_PLAYER_ACTIVATION_PREFIX)) {
    return ''
  }

  const queryIndex = raw.indexOf('?')
  if (queryIndex < 0) return ''
  const query = raw.substring(queryIndex + 1).split('#')[0]
  const entryPart = query.split('&').find((item) => item.startsWith('entry='))
  if (!entryPart) return ''

  try {
    return decodeURIComponent(entryPart.substring('entry='.length)).trim()
  } catch (error) {
    return ''
  }
}
