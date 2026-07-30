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
  const popupRef = useRef<Window | null>(null);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string>();

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
    } catch (error) {
      const message = requestErrorMessage(error);
      setErrorMessage(message);
      console.warn('WeKnora widget open failed:', message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={launcherWrapStyle}>
      {errorMessage ? (
        <div style={errorPanelStyle}>
          <Typography.Text strong>认定小助手暂不可用</Typography.Text>
          <Typography.Text type="secondary" style={{ display: 'block', marginTop: 6 }}>
            {errorMessage}
          </Typography.Text>
          <Button
            size="small"
            icon={<ReloadOutlined />}
            onClick={openAssistant}
            style={{ marginTop: 10 }}
          >
            重试
          </Button>
        </div>
      ) : null}
      <Button
        type="primary"
        shape="round"
        size="large"
        icon={<RobotOutlined />}
        loading={loading}
        onClick={openAssistant}
        style={launcherButtonStyle}
        aria-label="打开认定小助手"
      >
        <Space size={4}>
          认定小助手
          <ExportOutlined />
        </Space>
      </Button>
    </div>
  );
}

function widgetBaseUrl() {
  return (import.meta.env.VITE_WEKNORA_BASE_URL as string | undefined)?.trim()
    || DEFAULT_BASE_URL;
}

function channelId() {
  return (import.meta.env.VITE_WEKNORA_CHANNEL_ID as string | undefined)?.trim()
    || DEFAULT_CHANNEL_ID;
}

function embedUrl(token: string) {
  const base = widgetBaseUrl().replace(/\/$/, '');
  const channel = channelId();
  return `${base}/embed/${encodeURIComponent(channel)}#token=${encodeURIComponent(token)}`;
}

function tokenPrefix(token: string) {
  if (token.startsWith('ems_')) {
    return 'ems_';
  }
  if (token.startsWith('em_')) {
    return 'em_';
  }
  return 'unknown';
}

function redactEmbedUrl(url: string) {
  return url.replace(/#token=[^&]+/, '#token=<redacted>');
}

/** 开发环境调试：不打印完整 token，仅记录前缀与脱敏 URL */
function logEmbedDebug(token: string, iframeUrl: string) {
  if (!import.meta.env.DEV) {
    return;
  }
  console.info('[WeKnora][debug] pageProtocol=', window.location.protocol);
  console.info('[WeKnora][debug] pageOrigin=', window.location.origin);
  console.info('[WeKnora][debug] tokenPrefix=', tokenPrefix(token));
  console.info('[WeKnora][debug] iframeUrl=', redactEmbedUrl(iframeUrl));
  console.info('[WeKnora][debug] openMode=popup-window (not in-page iframe)');
  console.info(
    '[WeKnora][debug] embedHost=',
    new URL(iframeUrl).origin,
    '— if using in-page iframe, check X-Frame-Options / CSP frame-ancestors',
  );
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

function requestErrorMessage(error: unknown) {
  const maybeAxiosError = error as {
    response?: {
      status?: number;
      data?: {
        message?: string;
      };
    };
    message?: string;
  };
  return maybeAxiosError.response?.data?.message
    || (maybeAxiosError.response?.status ? `Request failed with status ${maybeAxiosError.response.status}` : undefined)
    || maybeAxiosError.message
    || 'WeKnora token request failed';
}

const launcherWrapStyle: React.CSSProperties = {
  position: 'fixed',
  right: 24,
  bottom: 24,
  zIndex: 2147483000,
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'flex-end',
  gap: 10,
};

const launcherButtonStyle: React.CSSProperties = {
  background: WIDGET_PRIMARY_COLOR,
  borderColor: WIDGET_PRIMARY_COLOR,
  boxShadow: '0 10px 24px rgba(7, 192, 95, 0.28)',
};

const errorPanelStyle: React.CSSProperties = {
  width: 280,
  border: '1px solid #f0f0f0',
  borderRadius: 8,
  background: '#fff',
  padding: 12,
  boxShadow: '0 10px 28px rgba(15, 23, 42, 0.16)',
};
