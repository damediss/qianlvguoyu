import sanitizeHtml from 'sanitize-html';
export function sanitizeExamHtml(html) {
    return sanitizeHtml(html, {
        allowedTags: [
            'p', 'br', 'strong', 'em', 's', 'u', 'blockquote', 'ul', 'ol', 'li',
            'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'pre', 'code', 'table', 'thead',
            'tbody', 'tr', 'th', 'td', 'a', 'img', 'span', 'div', 'sup', 'sub',
        ],
        allowedAttributes: {
            a: ['href', 'title', 'target', 'rel'],
            img: ['src', 'alt', 'title', 'width', 'height'],
            td: ['colspan', 'rowspan'],
            th: ['colspan', 'rowspan'],
            '*': ['class'],
        },
        allowedSchemes: ['http', 'https', 'mailto'],
        allowProtocolRelative: false,
        transformTags: {
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
