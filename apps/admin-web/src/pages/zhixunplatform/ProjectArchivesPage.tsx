import { Typography } from 'antd';
import { FolderOpenOutlined } from '@ant-design/icons';

export function ProjectArchivesPage() {
  return (
    <div className="zhixun-page">
      <section className="page-heading">
        <FolderOpenOutlined style={{ fontSize: 28, color: '#2A6DFF', marginBottom: 12 }} />
        <Typography.Title level={4}>项目档案</Typography.Title>
        <Typography.Text type="secondary">人机料法环业务档案与项目管控十大管理表</Typography.Text>
      </section>
    </div>
  );
}
