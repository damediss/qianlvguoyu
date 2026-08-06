import { Card, Empty, Typography } from 'antd';
import { TrainingProcessSteps } from './TrainingProcessSteps';

/** 运营流程 · 申报补贴（与跟班/考试同级） */
export function TrainingSubsidyDeclarePage() {
  return (
    <div className="training-page">
      <section className="page-heading">
        <div>
          <Typography.Title level={4}>申报补贴</Typography.Title>
          <Typography.Text type="secondary">管理培训补贴申报材料与申报进度</Typography.Text>
        </div>
      </section>

      <TrainingProcessSteps current={7} />

      <Card size="small">
        <Empty description="申报补贴功能建设中" />
      </Card>
    </div>
  );
}
