const TOKEN_QUERY_REGEX = /(?:collectionToken|collectToken|token)=([^&\s]+)/i;
/**
 * 从扫码结果中解析采集 token。
 * 支持纯 token、`collectionToken=xxx` 文本，以及带 `?token=`/`?collectToken=` 的采集链接。
 */
export function parseCollectionToken(raw) {
    if (!raw) {
        return '';
    }
    const value = raw.trim();
    if (!value) {
        return '';
    }
    const match = value.match(TOKEN_QUERY_REGEX);
    if (match) {
        return decodeURIComponent(match[1].trim());
    }
    return value;
}
