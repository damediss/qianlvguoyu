import { FolderOpenOutlined, UserAddOutlined } from '@ant-design/icons';
import { Empty, Steps, Tabs } from 'antd';
import { useEffect, useState } from 'react';
import { ApplicationMaterials } from './ApplicationMaterials';
import { CandidateImport } from './CandidateImport';

export type CollectionSubTabKey = 'candidates' | 'materials';

export const COLLECTION_SUB_TAB_KEYS: CollectionSubTabKey[] = ['candidates', 'materials'];

export function isCollectionSubTabKey(key: string): key is CollectionSubTabKey {
  return COLLECTION_SUB_TAB_KEYS.includes(key as CollectionSubTabKey);
}

interface InformationCollectionProps {
  taskId?: string;
  activeSubTab?: CollectionSubTabKey;
  onSubTabChange?: (key: CollectionSubTabKey) => void;
  onChanged?: () => void;
}

export function InformationCollection({
  taskId,
  activeSubTab = 'candidates',
  onSubTabChange,
  onChanged,
}: InformationCollectionProps) {
  const [subTab, setSubTab] = useState<CollectionSubTabKey>(activeSubTab);

  useEffect(() => {
    setSubTab(activeSubTab);
  }, [activeSubTab]);

  if (!taskId) {
    return <Empty description="请选择认定任务后进行信息收集" />;
  }

  const handleSubTabChange = (key: string) => {
    const next = key as CollectionSubTabKey;
    setSubTab(next);
    onSubTabChange?.(next);
  };

  const subTabItems = [
    {
      key: 'candidates',
      label: '学员导入',
      icon: <UserAddOutlined />,
      children: <CandidateImport taskId={taskId} onChanged={onChanged} />,
    },
    {
      key: 'materials',
      label: '申报材料',
      icon: <FolderOpenOutlined />,
      children: <ApplicationMaterials taskId={taskId} onChanged={onChanged} />,
    },
  ];

  return (
    <div>
      <Steps
        size="small"
        current={subTabItems.findIndex((item) => item.key === subTab)}
        style={{ marginBottom: 20, maxWidth: 480 }}
        items={[{ title: '学员导入' }, { title: '申报材料' }]}
      />
      <Tabs activeKey={subTab} onChange={handleSubTabChange} items={subTabItems} />
    </div>
  );
}
