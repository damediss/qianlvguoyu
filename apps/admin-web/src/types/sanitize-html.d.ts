declare module 'sanitize-html' {
  interface Options {
    allowedTags?: string[];
    allowedAttributes?: Record<string, string[]>;
    allowedSchemes?: string[];
    allowProtocolRelative?: boolean;
    transformTags?: Record<string, (tagName: string, attribs: Record<string, string>) => {
      tagName: string;
      attribs: Record<string, string>;
    }>;
  }

  export default function sanitizeHtml(html: string, options?: Options): string;
}
