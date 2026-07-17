import { useEffect, useRef, useState } from 'react';
import { Spin, message } from 'antd';
import { TeamOutlined } from '@ant-design/icons';
import { useAuthStore } from '../../stores/authStore';

/**
 * 知识库 → 人才识别
 * 通过 iframe 嵌入 rencaishie Vue3 应用，通过 postMessage 传递登录 token。
 * Token 不经过 URL，安全可靠。
 */
export function RencaishibieEmbedPage() {
  const [loading, setLoading] = useState(true);
  const [tokenReady, setTokenReady] = useState(false);
  const iframeRef = useRef<HTMLIFrameElement>(null);
  const token = useAuthStore((s) => s.token);
  const tenantId = useAuthStore((s) => s.tenantId);

  // iframe 加载完成后，通过 postMessage 把 token 传过去
  useEffect(() => {
    if (!iframeRef.current || !token || tokenReady) return;

    const sendToken = () => {
      iframeRef.current?.contentWindow?.postMessage(
        {
          type: 'saas-auth',
          token: token,
          tenantId: tenantId,
        },
        'http://127.0.0.1:8001'  // 生产环境换成实际域名 'http://43.138.163.7:8001'
      );
      setTokenReady(true);
    };

    // 等 iframe 加载后再发，确保对方监听器已就绪
    const timer = setTimeout(sendToken, 1000);
    return () => clearTimeout(timer);
  }, [token, tenantId, tokenReady]);

  return (
    <div style={{ position: 'relative', width: '100%', height: 'calc(100vh - 104px)' }}>
      {loading && (
        <div
          style={{
            position: 'absolute',
            inset: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 10,
            background: 'rgba(255,255,255,0.6)',
          }}
        >
          <Spin size="large" tip="人才识别加载中…" indicator={<TeamOutlined spin style={{ fontSize: 24, color: '#1677ff' }} />} />
        </div>
      )}
      <iframe
        ref={iframeRef}
        src="http://127.0.0.1:8001/tasks"
        title="人才识别"
        style={{
          width: '100%',
          height: '100%',
          border: 0,
          display: 'block',
        }}
        onLoad={() => setLoading(false)}
      />
    </div>
  );
}
