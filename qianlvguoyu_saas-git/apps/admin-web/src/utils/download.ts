/**
 * 从 Axios 响应头中解析 Content-Disposition 文件名。
 * 优先使用 filename* (RFC 5987 UTF-8 编码)，否则回退到 filename。
 * 返回 null 表示未找到。
 */
export function parseContentDispositionFilename(resp: { headers?: unknown }): string | null {
  const headers = resp.headers as Record<string, string> | undefined;
  const cd = headers?.['content-disposition'];
  if (!cd) return null;

  // RFC 5987: filename*=UTF-8''<url-encoded>
  const utf8Match = cd.match(/filename\*=(?:UTF-8''|utf-8'')([^;\s]+)/i);
  if (utf8Match) {
    try {
      return decodeURIComponent(utf8Match[1]);
    } catch {
      // fall through
    }
  }

  // Standard: filename="..." or filename=...
  const stdMatch = cd.match(/filename="?([^";]+)"?/);
  if (stdMatch) return stdMatch[1];

  return null;
}
