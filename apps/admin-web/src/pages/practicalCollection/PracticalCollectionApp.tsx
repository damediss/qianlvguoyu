import { Button, Input, Space, Typography } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { CollectionWorkbenchPage } from './CollectionWorkbenchPage';
import './PracticalCollectionApp.css';

function taskIdFromPath(): string | undefined {
  const match = window.location.pathname.match(/^\/practical-collection\/workbench\/([^/]+)/);
  if (!match?.[1]) return undefined;
  return decodeURIComponent(match[1]).trim() || undefined;
}

export function PracticalCollectionApp() {
  const initialTaskId = useMemo(() => taskIdFromPath(), []);
  const [manualTaskId, setManualTaskId] = useState('');

  useEffect(() => {
    const previous = document.body.style.minWidth;
    document.body.style.minWidth = '1024px';
    return () => {
      document.body.style.minWidth = previous;
    };
  }, []);

  if (initialTaskId) {
    return <CollectionWorkbenchPage taskId={initialTaskId} />;
  }

  const openWorkbench = () => {
    const value = manualTaskId.trim();
    if (value) {
      window.location.assign(`/practical-collection/workbench/${encodeURIComponent(value)}`);
    }
  };

  return (
    <div className="practical-collection-shell practical-collection-landing">
      <div className="collection-landing-panel">
        <Typography.Title>实操信息采集工作台</Typography.Title>
        <Typography.Paragraph>
          请从管理端任务首页进入采集工作台，或输入 taskId 进入现场采集页面。
        </Typography.Paragraph>
        <Space.Compact size="large" className="collection-landing-input">
          <Input
            value={manualTaskId}
            placeholder="输入采集任务 taskId"
            onChange={(event) => setManualTaskId(event.target.value)}
            onPressEnter={openWorkbench}
          />
          <Button type="primary" onClick={openWorkbench}>进入</Button>
        </Space.Compact>
      </div>
    </div>
  );
}
