import { useState } from 'react';
import { Spin } from 'antd';
import { TeamOutlined } from '@ant-design/icons';

/**
 * 知识库 → 人才识别
 * 通过 iframe 嵌入 rencaishibie Vue3 应用（同域部署，无跨域）。
 */
export function RencaishibieEmbedPage() {
  const [loading, setLoading] = useState(true);

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
        src="/rencai/"
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
