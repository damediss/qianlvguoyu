import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { ExportOutlined, RobotOutlined, ReloadOutlined } from '@ant-design/icons';
import { Button, Space, Typography } from 'antd';
import { useRef, useState } from 'react';
import { getWeKnoraEmbedToken } from '../../api/weknora';
const POPUP_NAME = 'weknora-assistant';
const DEFAULT_BASE_URL = 'http://43.138.163.7:8088';
const DEFAULT_CHANNEL_ID = 'aa3a676b-a1cb-4719-8e06-6636932d9bb2';
const WIDGET_PRIMARY_COLOR = '#96bbf8';
const POPUP_WIDTH = 420;
const POPUP_HEIGHT = 700;
export function WeKnoraWidget() {
    const popupRef = useRef(null);
    const [loading, setLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState();
    const openAssistant = async () => {
        if (loading) {
            return;
        }
        setLoading(true);
        setErrorMessage(undefined);
        try {
            if (popupRef.current && !popupRef.current.closed) {
                popupRef.current.focus();
                return;
            }
            const { token } = await getWeKnoraEmbedToken();
            if (!token) {
                throw new Error('WeKnora session token is empty');
            }
            const iframeUrl = embedUrl(token);
            logEmbedDebug(token, iframeUrl);
            const popup = window.open(iframeUrl, POPUP_NAME, popupFeatures());
            if (!popup) {
                throw new Error('浏览器拦截了弹窗，请允许本站弹窗后重试');
            }
            popupRef.current = popup;
            popup.focus();
        }
        catch (error) {
            const message = requestErrorMessage(error);
            setErrorMessage(message);
            console.warn('WeKnora widget open failed:', message);
        }
        finally {
            setLoading(false);
        }
    };
    return (_jsxs("div", { style: launcherWrapStyle, children: [errorMessage ? (_jsxs("div", { style: errorPanelStyle, children: [_jsx(Typography.Text, { strong: true, children: "\u8BA4\u5B9A\u5C0F\u52A9\u624B\u6682\u4E0D\u53EF\u7528" }), _jsx(Typography.Text, { type: "secondary", style: { display: 'block', marginTop: 6 }, children: errorMessage }), _jsx(Button, { size: "small", icon: _jsx(ReloadOutlined, {}), onClick: openAssistant, style: { marginTop: 10 }, children: "\u91CD\u8BD5" })] })) : null, _jsx(Button, { type: "primary", shape: "round", size: "large", icon: _jsx(RobotOutlined, {}), loading: loading, onClick: openAssistant, style: launcherButtonStyle, "aria-label": "\u6253\u5F00\u8BA4\u5B9A\u5C0F\u52A9\u624B", children: _jsxs(Space, { size: 4, children: ["\u8BA4\u5B9A\u5C0F\u52A9\u624B", _jsx(ExportOutlined, {})] }) })] }));
}
function widgetBaseUrl() {
    return import.meta.env.VITE_WEKNORA_BASE_URL?.trim()
        || DEFAULT_BASE_URL;
}
function channelId() {
    return import.meta.env.VITE_WEKNORA_CHANNEL_ID?.trim()
        || DEFAULT_CHANNEL_ID;
}
function embedUrl(token) {
    const base = widgetBaseUrl().replace(/\/$/, '');
    const channel = channelId();
    return `${base}/embed/${encodeURIComponent(channel)}#token=${encodeURIComponent(token)}`;
}
function tokenPrefix(token) {
    if (token.startsWith('ems_')) {
        return 'ems_';
    }
    if (token.startsWith('em_')) {
        return 'em_';
    }
    return 'unknown';
}
function redactEmbedUrl(url) {
    return url.replace(/#token=[^&]+/, '#token=<redacted>');
}
/** 开发环境调试：不打印完整 token，仅记录前缀与脱敏 URL */
function logEmbedDebug(token, iframeUrl) {
    if (!import.meta.env.DEV) {
        return;
    }
    console.info('[WeKnora][debug] pageProtocol=', window.location.protocol);
    console.info('[WeKnora][debug] pageOrigin=', window.location.origin);
    console.info('[WeKnora][debug] tokenPrefix=', tokenPrefix(token));
    console.info('[WeKnora][debug] iframeUrl=', redactEmbedUrl(iframeUrl));
    console.info('[WeKnora][debug] openMode=popup-window (not in-page iframe)');
    console.info('[WeKnora][debug] embedHost=', new URL(iframeUrl).origin, '— if using in-page iframe, check X-Frame-Options / CSP frame-ancestors');
}
function popupFeatures() {
    const left = Math.max(window.screenX + window.outerWidth - POPUP_WIDTH - 24, 0);
    const top = Math.max(window.screenY + window.outerHeight - POPUP_HEIGHT - 24, 0);
    return [
        `width=${POPUP_WIDTH}`,
        `height=${POPUP_HEIGHT}`,
        `left=${left}`,
        `top=${top}`,
        'resizable=yes',
        'scrollbars=yes',
    ].join(',');
}
function requestErrorMessage(error) {
    const maybeAxiosError = error;
    return maybeAxiosError.response?.data?.message
        || (maybeAxiosError.response?.status ? `Request failed with status ${maybeAxiosError.response.status}` : undefined)
        || maybeAxiosError.message
        || 'WeKnora token request failed';
}
const launcherWrapStyle = {
    position: 'fixed',
    right: 24,
    bottom: 24,
    zIndex: 2147483000,
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'flex-end',
    gap: 10,
};
const launcherButtonStyle = {
    background: WIDGET_PRIMARY_COLOR,
    borderColor: WIDGET_PRIMARY_COLOR,
    boxShadow: '0 10px 24px rgba(7, 192, 95, 0.28)',
};
const errorPanelStyle = {
    width: 280,
    border: '1px solid #f0f0f0',
    borderRadius: 8,
    background: '#fff',
    padding: 12,
    boxShadow: '0 10px 28px rgba(15, 23, 42, 0.16)',
};
