import { Image, Spin } from 'antd';
import type { ImageProps } from 'antd';
import { useEffect, useState, type ImgHTMLAttributes } from 'react';
import { fetchUrlFileBlob, isPrivateCosUrl, isUsableFileUrl } from '../api/files';

function useAuthImageSrc(url?: string) {
  const [src, setSrc] = useState<string>();
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!url || !isUsableFileUrl(url)) {
      setSrc(undefined);
      setLoading(false);
      return;
    }

    const isExternalDirect =
      (url.startsWith('http://') || url.startsWith('https://'))
      && !url.includes('/api/')
      && !isPrivateCosUrl(url);
    if (isExternalDirect) {
      setSrc(url);
      setLoading(false);
      return;
    }

    let objectUrl: string | undefined;
    let cancelled = false;
    setLoading(true);
    fetchUrlFileBlob(url)
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

/** 通过 apiClient 拉取文件并展示（img 直链无法带 Authorization / X-Tenant-Id） */
export function ApiAuthImage({ url, ...imageProps }: ApiAuthImageProps) {
  const { src, loading } = useAuthImageSrc(url);

  if (!url) return null;
  if (loading && !src) {
    return <Spin size="small" />;
  }
  if (!src) return null;

  return <Image src={src} {...imageProps} />;
}
