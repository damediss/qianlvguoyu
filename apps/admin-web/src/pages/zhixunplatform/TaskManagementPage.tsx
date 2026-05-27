import { useState } from 'react';
import { Typography } from 'antd';
import {
  UnorderedListOutlined,
  PlusCircleOutlined,
  ArrowRightOutlined,
  FolderOpenOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import './TaskManagementPage.css';

// ============================================================
// 样式常量
// ============================================================
const S = {
  page: { padding: '24px 20px 48px', maxWidth: 1200, margin: '0 auto' } as React.CSSProperties,
  heading: { marginBottom: 24 } as React.CSSProperties,
  tabsWrap: { display: 'flex', gap: 8, marginBottom: 24 } as React.CSSProperties,
  entryCard: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 24,
    padding: '28px 32px',
    background: 'linear-gradient(135deg, #E8F0FF 0%, #fff 60%)',
    border: '2px dashed rgba(42, 109, 255, 0.25)',
    borderRadius: 20,
    textDecoration: 'none',
    color: 'inherit',
    cursor: 'pointer',
    transition: 'all 0.25s ease',
    marginBottom: 16,
  } as React.CSSProperties,
  entryTextH3: { fontSize: 18, fontWeight: 700, color: '#2A6DFF', margin: '0 0 6px' } as React.CSSProperties,
  entryTextP: { fontSize: 13, color: '#64748B', maxWidth: 520, margin: 0 } as React.CSSProperties,
  entryBtn: {
    flexShrink: 0,
    padding: '14px 28px',
    background: '#2A6DFF',
    color: '#fff',
    borderRadius: 12,
    fontWeight: 600,
    fontSize: 14,
    display: 'inline-flex',
    alignItems: 'center',
    gap: 10,
    border: 'none',
    cursor: 'pointer',
    whiteSpace: 'nowrap',
  } as React.CSSProperties,
  infoText: { marginTop: 14, fontSize: 13, color: '#64748B' } as React.CSSProperties,
};

// ============================================================
// Tab 按钮
// ============================================================
function TabBtn({ active, children, onClick }: { active: boolean; children: React.ReactNode; onClick: () => void }) {
  return (
    <button className={`tp-tab-btn${active ? ' active' : ''}`} onClick={onClick}>
      {children}
    </button>
  );
}

// ============================================================
// 入口卡片
// ============================================================
function EntryCard({
  icon,
  title,
  desc,
  btnText,
  onClick,
}: {
  icon: React.ReactNode;
  title: string;
  desc: string;
  btnText: string;
  onClick?: () => void;
}) {
  return (
    <div className="tp-entry-card" style={S.entryCard} onClick={onClick}>
      <div>
        <h3 style={S.entryTextH3}>
          {icon} {title}
        </h3>
        <p style={S.entryTextP}>{desc}</p>
      </div>
      <span style={S.entryBtn}>
        {btnText} <ArrowRightOutlined />
      </span>
    </div>
  );
}

// ============================================================
// 主页面
// ============================================================
export function TaskManagementPage() {
  const [activeTab, setActiveTab] = useState<'create' | 'archive'>('create');
  const navigateTo = useNavStore((s) => s.setSelectedKey);

  return (
    <div style={S.page}>
      {/* 页面标题 */}
      <section style={S.heading}>
        <h2 style={{ fontSize: 26, fontWeight: 700, margin: '0 0 6px' }}>任务管理</h2>
        <Typography.Text type="secondary">创建培训任务并管理任务档案</Typography.Text>
      </section>

      {/* Tab 切换 */}
      <div style={S.tabsWrap}>
        <TabBtn active={activeTab === 'create'} onClick={() => setActiveTab('create')}>
          创建任务
        </TabBtn>
        <TabBtn active={activeTab === 'archive'} onClick={() => setActiveTab('archive')}>
          任务档案
        </TabBtn>
      </div>

      {/* 创建任务 Tab */}
      {activeTab === 'create' && (
        <div>
          <EntryCard
            icon={<PlusCircleOutlined />}
            title="创建培训任务"
            desc="完成基本信息、项目关联与任务配置，提交后同步至任务档案。"
            btnText="进入创建页"
            onClick={() => navigateTo('zhixun-create-task')}
          />
          <p style={S.infoText}>
            <InfoCircleOutlined style={{ color: '#2A6DFF', marginRight: 6 }} />
            学员名单、师资安排等配置项在创建页中按需填写，提交后可继续完善。
          </p>
        </div>
      )}

      {/* 任务档案 Tab */}
      {activeTab === 'archive' && (
        <div>
          <EntryCard
            icon={<FolderOpenOutlined />}
            title="任务档案台账"
            desc="查询、筛选全部培训任务，查看详情与配置进度，支持编辑、删除与导出演示。"
            btnText="进入档案页"
            onClick={() => navigateTo('zhixun-task-archives')}
          />
        </div>
      )}
    </div>
  );
}
