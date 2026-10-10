import { useState } from 'react';
import { Alert, Button, Card, Space, Typography } from 'antd';
import { SettingOutlined } from '@ant-design/icons';
import { ClassPracticeAnalyticsPanel } from '../ClassPracticeAnalyticsModal';
import { ClassPracticeScopeModal } from '../ClassPracticeScopeModal';

type ApiId = number | string;

type Props = {
  classId: ApiId;
  classTitle?: string;
  /** Tab 激活时才拉学情数据 */
  active: boolean;
};

/**
 * 跟班「刷题」Tab：试题配置（练习范围）+ 学员刷题详情。
 * 与「作业」Tab 分列（刷题=自助学情，作业=任务制）。
 */
export function FollowPracticeTab({ classId, classTitle, active }: Props) {
  const [scopeOpen, setScopeOpen] = useState(false);

  return (
    <div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Space wrap style={{ width: '100%', justifyContent: 'space-between' }}>
          <div>
            <Typography.Text strong>学员刷题详情</Typography.Text>
            <Typography.Paragraph type="secondary" style={{ marginBottom: 0, marginTop: 4 }}>
              按班级查看进度、正确率、练习次数与每日刷题；与作业/随堂练分通道统计。保存试题配置后会自动开通同步学（绑定考试计划、名单、灰度）。
            </Typography.Paragraph>
          </div>
          <Button type="primary" icon={<SettingOutlined />} onClick={() => setScopeOpen(true)}>
            试题配置
          </Button>
        </Space>
      </Card>

      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 12 }}
        message="请先配置本班可刷科目与知识点。保存后会自动开通同步学；学员需从 App 班级入口刷题（带 classId）后，此处才有学情数据。"
        action={
          <Button size="small" onClick={() => setScopeOpen(true)}>
            去配置
          </Button>
        }
      />

      <ClassPracticeAnalyticsPanel classId={classId} enabled={active} />

      <ClassPracticeScopeModal
        open={scopeOpen}
        onClose={() => setScopeOpen(false)}
        classId={classId}
        classTitle={classTitle}
        titlePrefix="试题配置"
      />
    </div>
  );
}
