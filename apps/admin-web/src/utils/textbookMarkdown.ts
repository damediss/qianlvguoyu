/**
 * 教材 Markdown 安全与本地备份工具 —— 《二期编写协同实施设计》§8.1/§8.2/§10.5
 *
 * 预览第二道防线（第一道是后端保存校验）：
 * - 原始 HTML / script / iframe / 事件属性全部剥离（sanitize-html 白名单）
 * - 图片仅允许本节 textbook-asset://{assetId} 经 A2 换取的 blob: URL，外链/data:/javascript:/blob 伪造一律清空
 * - 链接仅允许 http/https/mailto
 *
 * 抽公共 util，勿在各页面私有复制（CompetitionNoticeEditorDrawer 的实现为竞赛公告私有，不复用其业务逻辑）。
 */
import sanitizeHtml from 'sanitize-html';

/** Markdown 正文里的内部图片 URI 前缀（A1 上传返回 sourceUri） */
export const TEXTBOOK_ASSET_SCHEME = 'textbook-asset://';

/** 预览 HTML 白名单 sanitize：图片只放行 blob:（内部资产经 A2 替换后的形态） */
export function sanitizeTextbookHtml(html: string): string {
  return sanitizeHtml(html, {
    allowedTags: [
      'p', 'br', 'strong', 'em', 's', 'del', 'ins', 'mark', 'blockquote', 'ul', 'ol', 'li',
      'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'hr', 'pre', 'code', 'table', 'thead', 'tbody',
      'tr', 'th', 'td', 'a', 'img', 'input', 'figure', 'figcaption', 'span',
    ],
    allowedAttributes: {
      a: ['href', 'title', 'target', 'rel'],
      img: ['src', 'alt', 'title'],
      code: ['class'],
      input: ['type', 'checked', 'disabled'],
      span: ['class'],
    },
    allowedSchemes: ['http', 'https', 'mailto', 'blob'],
    allowProtocolRelative: false,
    transformTags: {
      img: (tagName, attribs) => ({
        tagName,
        attribs: {
          ...attribs,
          // 只放行 A2 换来的 blob:，其余（外链/data:/javascript:/textbook-asset: 未替换形态）一律清空
          src: attribs.src?.startsWith('blob:') ? attribs.src : '',
        },
      }),
      a: (tagName, attribs) => ({
        tagName,
        attribs: {
          ...attribs,
          href: /^(https?:|mailto:)/i.test(attribs.href || '') ? attribs.href : '',
          target: '_blank',
          rel: 'noopener noreferrer',
        },
      }),
    },
  });
}

/** 收集正文中出现的全部 textbook-asset:// 资产 ID（去重） */
export function collectTextbookAssetIds(contentMd: string): string[] {
  const ids = new Set<string>();
  const re = new RegExp(`${TEXTBOOK_ASSET_SCHEME.replace(':', '\\:')}([A-Za-z0-9_-]+)`, 'g');
  let m: RegExpExecArray | null;
  while ((m = re.exec(contentMd || '')) !== null) {
    ids.add(m[1]);
  }
  return [...ids];
}

/**
 * 把正文中的内部资产 URI 替换为经 A2 取回的 blob URL（§8.2：在线预览先替换再渲染）。
 * 单个资产取回失败时保留原样（sanitize 会将其清空，不裂死页面）。
 */
export async function resolveTextbookAssetUris(
  contentMd: string,
  fetchAsset: (assetId: string) => Promise<Blob>,
  objectUrls: Map<string, string>,
): Promise<string> {
  const ids = collectTextbookAssetIds(contentMd);
  await Promise.all(
    ids.map(async (id) => {
      if (objectUrls.has(id)) return;
      try {
        const blob = await fetchAsset(id);
        objectUrls.set(id, URL.createObjectURL(blob));
      } catch {
        // 越权/已删除：保留原 URI，由 sanitize 兜底
      }
    }),
  );
  let resolved = contentMd || '';
  objectUrls.forEach((url, id) => {
    resolved = resolved.split(`${TEXTBOOK_ASSET_SCHEME}${id}`).join(url);
  });
  return resolved;
}

/** 释放全部 blob URL（关闭工作台/切换节时统一 revoke，§8.2） */
export function revokeObjectUrls(objectUrls: Map<string, string>): void {
  objectUrls.forEach((url) => URL.revokeObjectURL(url));
  objectUrls.clear();
}

// ------------------------------------------------------------------
// 本地备份（§10.5：键含 username+sectionId+baseVersion，保存成功清除）
// ------------------------------------------------------------------

const BACKUP_KEY_PREFIX = 'textbook_section_draft';

export function textbookBackupKey(username: string, sectionId: string, baseVersion: number): string {
  return `${BACKUP_KEY_PREFIX}:${username}:${sectionId}:${baseVersion}`;
}

export function saveTextbookBackup(key: string, contentMd: string): void {
  try {
    localStorage.setItem(key, contentMd);
  } catch {
    // 配额满/隐私模式：静默降级，仅失去本地备份能力
  }
}

export function loadTextbookBackup(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

export function clearTextbookBackup(key: string): void {
  try {
    localStorage.removeItem(key);
  } catch {
    // ignore
  }
}
