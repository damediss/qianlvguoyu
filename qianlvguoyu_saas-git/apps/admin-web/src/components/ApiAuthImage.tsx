import { Image, Spin } from 'antd';
import type { ImageProps } from 'antd';
import { useEffect, useState, type ImgHTMLAttributes } from 'react';
import {
  fetchFileBlob,
  fetchUrlFileBlob,
  isPrivateCosUrl,
  isPublicExamFileUrl,
  isUsableFileUrl,
  toBrowserPublicFileSrc,
} from '../api/files';

function useAuthImageSrc(url?: string) {
  const [src, setSrc] = useState<string>();
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!url) {
      setSrc(undefined);
      setLoading(false);
      return;
    }

    // 本地上传瞬间预览
    if (url.startsWith('blob:') || url.startsWith('data:')) {
      setSrc(url);
      setLoading(false);
      return;
    }

    if (!isUsableFileUrl(url)) {
      setSrc(undefined);
      setLoading(false);
      return;
    }

    // /exam/files/** 后端 permitAll，本地缺失时还会 302 到生产 CDN；必须用 img 直链。
    // 若走 axios，XHR 跟随跨域 302 会变成 Network Error，学员列表会刷一排报错。
    if (isPublicExamFileUrl(url)) {
      setSrc(toBrowserPublicFileSrc(url));
      setLoading(false);
      return;
    }

    // 平台文件下载：鉴权拉取，并强制 inline，避免 attachment 导致预览异常
    const filesDownloadMatch = url.match(/\/files\/([^/?#]+)\/download/i);
    if (filesDownloadMatch) {
      const fileId = filesDownloadMatch[1];
      let objectUrl: string | undefined;
      let cancelled = false;
      setLoading(true);
      fetchFileBlob(`/files/${encodeURIComponent(fileId)}/download?inline=true`, { silentError: true })
        .then((blob) => {
          if (cancelled) return;
          objectUrl = URL.createObjectURL(blob);
          setSrc(objectUrl);
        })
        .catch(() => {
          if (!cancelled) setSrc(undefined);
        })
        .finally(() => {
          if (!cancelled) setLoading(false);
        });
      return () => {
        cancelled = true;
        if (objectUrl) URL.revokeObjectURL(objectUrl);
      };
    }

    // COS 地址：优先直链（public 对象可显示）；失败时由 img 自然空白，不再走 XHR（会 CORS）
    if (isPrivateCosUrl(url)) {
      setSrc(url);
      setLoading(false);
      return;
    }

    const isExternalDirect =
      (url.startsWith('http://') || url.startsWith('https://'))
      && !url.includes('/api/');
    if (isExternalDirect) {
      setSrc(url);
      setLoading(false);
      return;
    }

    let objectUrl: string | undefined;
    let cancelled = false;
    setLoading(true);
    fetchUrlFileBlob(url, { silentError: true })
      .then((blob) => {
        if (cancelled) return;
        objectUrl = URL.createObjectURL(blob);
        setSrc(objectUrl);
      })
      .catch(() => {
        if (!cancelled) setSrc(undefined);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [url]);

  return { src, loading };
}

type AuthImageProps = Omit<ImgHTMLAttributes<HTMLImageElement>, 'src'> & {
  url?: string;
};

/** 原生 img，适用于 Upload 预览、表格缩略图等（Ant Image 包裹会影响布局） */
export function AuthImage({ url, alt, className, style, ...rest }: AuthImageProps) {
  const { src, loading } = useAuthImageSrc(url);
  if (!url) return null;
  if (loading && !src) {
    return <Spin size="small" />;
  }
  if (!src) return null;
  return <img src={src} alt={alt} className={className} style={style} {...rest} />;
}

type ApiAuthImageProps = Omit<ImageProps, 'src'> & {
  url?: string;
};

/**
 * 展示需鉴权的文件预览；对 /exam/files（公开）则退化为同源直链，避免 axios+302 跨域。
 */
export function ApiAuthImage({ url, ...imageProps }: ApiAuthImageProps) {
  const { src, loading } = useAuthImageSrc(url);

  if (!url) return null;
  if (loading && !src) {
    return <Spin size="small" />;
  }
  if (!src) return null;

  return <Image src={src} {...imageProps} />;
}
