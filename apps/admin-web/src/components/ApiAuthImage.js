import { jsx as _jsx } from "react/jsx-runtime";
import { Image, Spin } from 'antd';
import { useEffect, useState } from 'react';
import { fetchFileBlob, fetchUrlFileBlob, isPrivateCosUrl, isPublicExamFileUrl, isUsableFileUrl, toBrowserPublicFileSrc, } from '../api/files';
function useAuthImageSrc(url) {
    const [src, setSrc] = useState();
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
            let objectUrl;
            let cancelled = false;
            setLoading(true);
            fetchFileBlob(`/files/${encodeURIComponent(fileId)}/download?inline=true`, { silentError: true })
                .then((blob) => {
                if (cancelled)
                    return;
                objectUrl = URL.createObjectURL(blob);
                setSrc(objectUrl);
            })
                .catch(() => {
                if (!cancelled)
                    setSrc(undefined);
            })
                .finally(() => {
                if (!cancelled)
                    setLoading(false);
            });
            return () => {
                cancelled = true;
                if (objectUrl)
                    URL.revokeObjectURL(objectUrl);
            };
        }
        // COS 地址：优先直链（public 对象可显示）；失败时由 img 自然空白，不再走 XHR（会 CORS）
        if (isPrivateCosUrl(url)) {
            setSrc(url);
            setLoading(false);
            return;
        }
        const isExternalDirect = (url.startsWith('http://') || url.startsWith('https://'))
            && !url.includes('/api/');
        if (isExternalDirect) {
            setSrc(url);
            setLoading(false);
            return;
        }
        let objectUrl;
        let cancelled = false;
        setLoading(true);
        fetchUrlFileBlob(url, { silentError: true })
            .then((blob) => {
            if (cancelled)
                return;
            objectUrl = URL.createObjectURL(blob);
            setSrc(objectUrl);
        })
            .catch(() => {
            if (!cancelled)
                setSrc(undefined);
        })
            .finally(() => {
            if (!cancelled)
                setLoading(false);
        });
        return () => {
            cancelled = true;
            if (objectUrl)
                URL.revokeObjectURL(objectUrl);
        };
    }, [url]);
    return { src, loading };
}
/** 原生 img，适用于 Upload 预览、表格缩略图等（Ant Image 包裹会影响布局） */
export function AuthImage({ url, alt, className, style, ...rest }) {
    const { src, loading } = useAuthImageSrc(url);
    const [failed, setFailed] = useState(false);
    useEffect(() => {
        setFailed(false);
    }, [src, url]);
    if (!url)
        return null;
    if (loading && !src) {
        return _jsx(Spin, { size: "small" });
    }
    if (!src || failed)
        return null;
    return (_jsx("img", { src: src, alt: alt, className: className, style: style, onError: () => setFailed(true), ...rest }));
}
/**
 * 展示需鉴权的文件预览；对 /exam/files（公开）则退化为同源直链，避免 axios+302 跨域。
 */
export function ApiAuthImage({ url, ...imageProps }) {
    const { src, loading } = useAuthImageSrc(url);
    if (!url)
        return null;
    if (loading && !src) {
        return _jsx(Spin, { size: "small" });
    }
    if (!src)
        return null;
    return _jsx(Image, { src: src, ...imageProps });
}
/** 鉴权拉取后的可播放 video（blob URL） */
export function ApiAuthVideo({ url, width = 160, height = 90, style, className, controls = true, onClick, }) {
    const { src, loading } = useAuthImageSrc(url);
    if (!url)
        return null;
    if (loading && !src) {
        return _jsx(Spin, { size: "small" });
    }
    if (!src) {
        return _jsx("span", { style: { color: '#999', fontSize: 12 }, children: "\u89C6\u9891\u4E0D\u53EF\u7528" });
    }
    return (_jsx("video", { src: src, controls: controls, className: className, style: {
            width,
            height,
            objectFit: 'cover',
            borderRadius: 4,
            background: '#111',
            display: 'block',
            ...style,
        }, onClick: onClick, preload: "metadata" }));
}
