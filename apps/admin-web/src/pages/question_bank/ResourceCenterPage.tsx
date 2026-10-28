/**
 * 资源管理落地页（课程管理 → 资源管理，方案 §7.4）
 * 目前承载：知识归属修正（exam_exam_detail 数据治理）；预留后续资源类功能入口。
 */
import React from 'react';
import { Button, Card, Col, Row } from 'antd';
import { SafetyCertificateOutlined, ToolOutlined } from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';

const ResourceCenterPage: React.FC = () => {
  const setSelectedKey = useNavStore((s) => s.setSelectedKey);
  return (
    <Row gutter={16}>
      <Col span={8}>
        <Card
          hoverable
          title={
            <span>
              <ToolOutlined style={{ marginRight: 8 }} />
              知识归属修正
            </span>
          }
          onClick={() => setSelectedKey('knowledge_fix')}
        >
          <p>
            对 2026-05-14 以来导入 exam_exam_detail 的题目：重复导入数据软删除去重；
            LLM 判定知识点归属并按置信分层修正（apply/keep/flag 三类，全程备份可回滚）。
          </p>
          <Button type="primary" size="small">
            进入工作台
          </Button>
        </Card>
      </Col>
      <Col span={8}>
        <Card
          hoverable
          title={
            <span>
              <SafetyCertificateOutlined style={{ marginRight: 8 }} />
              客观题答案AI检查
            </span>
          }
          onClick={() => setSelectedKey('objective_answer_check')}
        >
          <p>
            按工种、等级、来源、题型筛选题库，用决策模型独立作答并与原答案比对，
            快速筛出疑似答案错误；只做筛查与人工复核，不会修改题库答案。
          </p>
          <Button type="primary" size="small">
            进入工作台
          </Button>
        </Card>
      </Col>
    </Row>
  );
};

export default ResourceCenterPage;
