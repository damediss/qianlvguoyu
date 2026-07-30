const IMAGE_REFERENCE = /!\[([^\]]*)\]\(notice-asset:\/\/(\d+)\)/g

const inlineStyle = (style) => ({ style })

function textNode(text) {
  return { type: 'text', text: String(text || '') }
}

function block(text, style) {
  return { name: 'div', attrs: inlineStyle(style), children: [textNode(text)] }
}

function imageNode(url, alt) {
  return {
    name: 'img',
    attrs: {
      src: url,
      alt: alt || '公告图片',
      style: 'display:block;max-width:100%;height:auto;margin:18px 0;border-radius:10px;background:#f3f6fb;',
    },
  }
}

function plainInline(text) {
  return String(text || '')
    .replace(/\[([^\]]+)\]\([^)]*\)/g, '$1')
    .replace(/(\*\*|__)(.*?)\1/g, '$2')
    .replace(/[*_`]/g, '')
}

/**
 * Converts the supported Markdown subset into rich-text nodes.  It deliberately does not accept
 * raw HTML or arbitrary URLs; images can only come from already-authorized notice assets.
 */
export function buildCompetitionNoticeMarkdownNodes(markdown, imageUrls = {}) {
  const nodes = []
  const lines = String(markdown || '').replace(/\r\n?/g, '\n').split('\n')

  lines.forEach((rawLine) => {
    const line = rawLine.trim()
    if (!line) return

    if (/^(-{3,}|\*{3,}|_{3,})$/.test(line)) {
      nodes.push({ name: 'hr', attrs: inlineStyle('border:0;border-top:1px solid #e7ecf3;margin:20px 0;') })
      return
    }

    const heading = line.match(/^(#{1,6})\s+(.+)$/)
    if (heading) {
      const sizes = ['24px', '22px', '20px', '18px', '17px', '16px']
      nodes.push(block(plainInline(heading[2]), `margin:18px 0 10px;color:#152b52;font-size:${sizes[heading[1].length - 1]};font-weight:700;line-height:1.35;`))
      return
    }

    const quote = line.match(/^>\s?(.*)$/)
    if (quote) {
      nodes.push(block(plainInline(quote[1]), 'margin:14px 0;padding:9px 12px;border-left:3px solid #4b84e8;background:#f4f8ff;color:#526884;font-size:15px;line-height:1.7;'))
      return
    }

    const list = line.match(/^(?:[-*+]\s+|\d+[.)]\s+)(.*)$/)
    if (list) {
      nodes.push(block(`• ${plainInline(list[1])}`, 'margin:6px 0;color:#344a6b;font-size:16px;line-height:1.7;'))
      return
    }

    let cursor = 0
    let hasImage = false
    IMAGE_REFERENCE.lastIndex = 0
    let match
    while ((match = IMAGE_REFERENCE.exec(line)) !== null) {
      const before = plainInline(line.slice(cursor, match.index)).trim()
      if (before) nodes.push(block(before, 'margin:0 0 12px;color:#344a6b;font-size:16px;line-height:1.75;white-space:pre-wrap;'))
      const imageUrl = imageUrls[String(match[2])]
      if (imageUrl) nodes.push(imageNode(imageUrl, match[1]))
      else nodes.push(block(match[1] || '公告图片加载失败', 'margin:8px 0;color:#8a98aa;font-size:14px;'))
      cursor = match.index + match[0].length
      hasImage = true
    }
    const after = plainInline(line.slice(cursor)).trim()
    if (after || !hasImage) {
      nodes.push(block(after || plainInline(line), 'margin:0 0 12px;color:#344a6b;font-size:16px;line-height:1.75;white-space:pre-wrap;'))
    }
  })

  return nodes.length ? nodes : [block('暂无公告内容', 'color:#8a98aa;font-size:16px;')]
}
