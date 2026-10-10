import { Typography } from 'antd';
import {
  UserOutlined,
  ToolOutlined,
  InboxOutlined,
  BookOutlined,
  BankOutlined,
  AimOutlined,
  BarChartOutlined,
  UserSwitchOutlined,
  FileTextOutlined,
  FlagOutlined,
  WarningOutlined,
  BugOutlined,
  RetweetOutlined,
  MessageOutlined,
  BulbOutlined,
  ApartmentOutlined,
  SendOutlined,
  ProfileOutlined,
  FolderOpenOutlined,
  ArrowRightOutlined,
} from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import './ProjectArchivesPage.css';

/* ============================================================
   项目档案页面 —— 样式见 ProjectArchivesPage.css
   ============================================================ */

export function ProjectArchivesPage() {
  const navigateTo = useNavStore((s) => s.setSelectedKey);

  return (
    <div className="pa-page">
      <header className="page-header">
        <h2>项目档案</h2>
        <p>人机料法环业务档案与项目管控十大管理表，每项独立页面，风格与系统一致</p>
      </header>

      {/* ---- 业务档案（人机料法环） ---- */}
      <h3 className="archive-section-title">
        <InboxOutlined /> 业务档案（人机料法环）
      </h3>
      <div className="archive-entry-list">
        <EntryCard
          icon={<UserOutlined />}
          title="人员档案"
          desc="师资与教务 · 参训学员（资质、确认、审核、预警）"
          onClick={() => navigateTo('zhixun-archive-personnel')}
        />
        <EntryCard
          icon={<ToolOutlined />}
          title="教学设备"
          desc="设备台账、完好率与保养计划"
          onClick={() => navigateTo('zhixun-archive-equipment')}
        />
        <EntryCard
          icon={<InboxOutlined />}
          title="物料台账"
          desc="库存、安全库存与出入库"
          onClick={() => navigateTo('zhixun-archive-materials')}
        />
        <EntryCard
          icon={<BookOutlined />}
          title="方法规则"
          desc="SOP、考核规范与安全制度"
          onClick={() => navigateTo('zhixun-archive-rules')}
        />
        <EntryCard
          icon={<BankOutlined />}
          title="环境配置"
          desc="实训基地、场地与环境评价"
          onClick={() => navigateTo('zhixun-archive-environment')}
        />
      </div>

      {/* ---- 项目管控 · 十大管理表 ---- */}
      <h3 className="archive-section-title" style={{ marginTop: 8 }}>
        <ProfileOutlined /> 项目管控 · 十大管理表
      </h3>
      <div className="archive-entry-list archive-entry-list-compact">
        <EntryCardSm icon={<AimOutlined />} title="战略解码表" onClick={() => navigateTo('zhixun-table-strategic')} />
        <EntryCardSm icon={<BarChartOutlined />} title="组织绩效表" onClick={() => navigateTo('zhixun-table-org-perf')} />
        <EntryCardSm icon={<UserSwitchOutlined />} title="个人绩效承诺（PBC）" onClick={() => navigateTo('zhixun-table-pbc')} />
        <EntryCardSm icon={<FileTextOutlined />} title="项目立项 Charter" onClick={() => navigateTo('zhixun-table-charter')} />
        <EntryCardSm icon={<FlagOutlined />} title="里程碑管理表" onClick={() => navigateTo('zhixun-table-milestone')} />
        <EntryCardSm icon={<WarningOutlined />} title="风险管理表" onClick={() => navigateTo('zhixun-table-risk')} />
        <EntryCardSm icon={<BugOutlined />} title="问题管理表" onClick={() => navigateTo('zhixun-table-issue')} />
        <EntryCardSm icon={<RetweetOutlined />} title="变更管理表" onClick={() => navigateTo('zhixun-table-change')} />
        <EntryCardSm icon={<MessageOutlined />} title="会议行动项表" onClick={() => navigateTo('zhixun-table-meeting')} />
        <EntryCardSm icon={<BulbOutlined />} title="知识收割表" onClick={() => navigateTo('zhixun-table-knowledge')} />
      </div>

      {/* ---- 扩展管理表 ---- */}
      <h3 className="archive-section-title" style={{ marginTop: 8 }}>
        <FileTextOutlined /> 扩展管理表（对齐 Excel 模板）
      </h3>
      <div className="archive-entry-list archive-entry-list-compact">
        <EntryCardSm icon={<ApartmentOutlined />} title="工作分解结构 WBS" onClick={() => navigateTo('zhixun-table-wbs')} />
        <EntryCardSm icon={<SendOutlined />} title="项目沟通计划" onClick={() => navigateTo('zhixun-table-communication')} />
        <EntryCardSm icon={<ProfileOutlined />} title="项目状态报告" onClick={() => navigateTo('zhixun-table-status')} />
      </div>

      {/* ---- 资料归档 ---- */}
      <h3 className="archive-section-title" style={{ marginTop: 8 }}>
        <FolderOpenOutlined /> 资料归档
      </h3>
      <a className="pa-entry-card" style={{ background: '#fafbfc' }} onClick={() => navigateTo('zhixun-material-archives')}>
        <div className="entry-text">
          <h3>
            <FolderOpenOutlined /> 资料档案中心
          </h3>
          <p>按项目-班级-阶段-版本电子归档与审批入库</p>
        </div>
        <span className="entry-btn">
          进入 <ArrowRightOutlined />
        </span>
      </a>
    </div>
  );
}

/* ============================================================
   子组件
   ============================================================ */

function EntryCard({
  icon,
  title,
  desc,
  onClick,
}: {
  icon: React.ReactNode;
  title: string;
  desc: string;
  onClick: () => void;
}) {
  return (
    <a className="pa-entry-card" onClick={onClick}>
      <div className="entry-text">
        <h3>
          {icon} {title}
        </h3>
        <p>{desc}</p>
      </div>
      <span className="entry-btn">
        进入 <ArrowRightOutlined />
      </span>
    </a>
  );
}

function EntryCardSm({
  icon,
  title,
  onClick,
}: {
  icon: React.ReactNode;
  title: string;
  onClick: () => void;
}) {
  return (
    <a className="pa-entry-card pa-entry-card-sm" onClick={onClick}>
      <div className="entry-text">
        <h3>
          {icon} {title}
        </h3>
      </div>
      <span className="entry-btn">
        <ArrowRightOutlined />
      </span>
    </a>
  );
}
