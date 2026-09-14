/**
 * 资源管理落地页（课程管理 → 资源管理，方案 §7.4）
 * 目前承载：知识归属修正（exam_exam_detail 数据治理）；预留后续资源类功能入口。
 */
import React from 'react';
import { Button, Card, Col, Row } from 'antd';
import { ToolOutlined } from '@ant-design/icons';
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
    </Row>
  );
};

export default ResourceCenterPage;
