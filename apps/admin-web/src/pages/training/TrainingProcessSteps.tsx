import { Card, Steps } from 'antd';
import { DatabaseOutlined, EditOutlined, FileTextOutlined, PictureOutlined, UploadOutlined } from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import './TrainingProcessSteps.css';

/** 流程步骤对应的菜单 code，与 main.tsx pageMap 一致 */
export const TRAINING_PROCESS_STEP_ROUTES = [
  'train_needs',
  'train_solutions',
  'train_info-list',
  'train_people-list',
  'train_declares',
  'train_sign-up',
] as const;

export const TRAINING_PROCESS_STEPS = [
  { title: '需求管理', icon: <EditOutlined /> },
  { title: '培训方案', icon: <FileTextOutlined /> },
  { title: '培训信息', icon: <UploadOutlined /> },
  { title: '班级人员', icon: <PictureOutlined /> },
  { title: '开班申报', icon: <EditOutlined /> },
  { title: '信息收集', icon: <DatabaseOutlined /> },
];

type TrainingProcessStepsProps = {
  /** 当前流程步骤索引，0=需求管理 … 5=信息收集 */
  current: number;
  /** 自定义各步骤跳转路由，默认使用 TRAINING_PROCESS_STEP_ROUTES */
  stepRoutes?: readonly string[];
};

export function TrainingProcessSteps({ current, stepRoutes = TRAINING_PROCESS_STEP_ROUTES }: TrainingProcessStepsProps) {
  const navigateTo = useNavStore((s) => s.setSelectedKey);

  const handleChange = (step: number) => {
    if (step === current) return;
    const route = stepRoutes[step];
    if (route) navigateTo(route);
  };

  return (
    <Card size="small" style={{ marginBottom: 16 }}>
      <Steps
        className="training-process-steps"
        current={current}
        size="small"
        items={TRAINING_PROCESS_STEPS}
        onChange={handleChange}
      />
    </Card>
  );
}
