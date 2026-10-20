import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useCallback, useEffect, useRef, useState } from 'react';
import { Spin } from 'antd';
import { TeamOutlined } from '@ant-design/icons';
import { useAuthStore } from '../../stores/authStore';
/**
 * 知识库 → 人才识别
 * 通过 iframe 嵌入 rencaishibie Vue3 应用，通过 postMessage 传递登录 token。
 *
 * 服务器 nginx 反代：/rencai/ → http://127.0.0.1:8001/
 * 本机开发：vite 代理 /rencai/ → http://localhost:8001/
 * iframe src 用相对路径 /rencai/，postMessage targetOrigin 动态拼接当前页面 origin。
 */
export function RencaishibieEmbedPage() {
    const [loading, setLoading] = useState(true);
    const iframeRef = useRef(null);
    const token = useAuthStore((s) => s.token);
    const tenantId = useAuthStore((s) => s.tenantId);
    // 动态拼接目标 origin（支持 localhost:5173 和域名访问）
    const targetOrigin = `${window.location.protocol}//${window.location.host}`;
    // iframe 加载完成后，通过 postMessage 把 token 传过去
    const sendToken = useCallback(() => {
        if (!iframeRef.current?.contentWindow || !token)
            return;
        iframeRef.current.contentWindow.postMessage({ type: 'saas-auth', token, tenantId }, targetOrigin // 动态 origin，适配本机和服务器
        );
    }, [token, tenantId, targetOrigin]);
    // token/tenantId 变化时发送；iframe 刷新后也会触发 onLoad 重新发送
    useEffect(() => {
        sendToken();
    }, [sendToken]);
    return (_jsxs("div", { style: { position: 'relative', width: '100%', height: 'calc(100vh - 104px)' }, children: [loading && (_jsx("div", { style: {
                    position: 'absolute',
                    inset: 0,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    zIndex: 10,
                    background: 'rgba(255,255,255,0.6)',
                }, children: _jsx(Spin, { size: "large", tip: "\u4EBA\u624D\u8BC6\u522B\u52A0\u8F7D\u4E2D\u2026", indicator: _jsx(TeamOutlined, { spin: true, style: { fontSize: 24, color: '#1677ff' } }) }) })), _jsx("iframe", { ref: iframeRef, src: "/rencai/", title: "\u4EBA\u624D\u8BC6\u522B", style: {
                    width: '100%',
                    height: '100%',
                    border: 0,
                    display: 'block',
                }, onLoad: () => {
                    setLoading(false);
                    // iframe 每次加载完成（包括刷新）都重新下发 token
                    sendToken();
                } })] }));
}
