import { Card, Steps } from 'antd';
import {
  AccountBookOutlined,
  ApartmentOutlined,
  BulbOutlined,
  EditOutlined,
  FormOutlined,
  PictureOutlined,
  PlayCircleOutlined,
  SolutionOutlined,
  TrophyOutlined,
} from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import './TrainingProcessSteps.css';

/** 流程步骤对应的菜单 code，与 main.tsx pageMap 一致 */
export const TRAINING_PROCESS_STEP_ROUTES = [
  'pxyan_jiu',
  'pxan_fang_gui_hua',
  'fen_ban',
  'train_people-list',
  'train_declares',
  'train_follow',
  'train_follow_exam',
  'train_graduation_ceremony',
  'train_subsidy_declare',
] as const;

export const TRAINING_PROCESS_STEPS = [
  { title: '企业调研', icon: <BulbOutlined /> },
  { title: '方案规划', icon: <SolutionOutlined /> },
  { title: '分班', icon: <ApartmentOutlined /> },
  { title: '开班准备', icon: <PictureOutlined /> },
  { title: '开班申报', icon: <EditOutlined /> },
  { title: '跟班', icon: <PlayCircleOutlined /> },
  { title: '考试', icon: <FormOutlined /> },
  { title: '毕业典礼', icon: <TrophyOutlined /> },
  { title: '申报补贴', icon: <AccountBookOutlined /> },
];

type TrainingProcessStepsProps = {
  /** 当前流程步骤索引，0=企业调研 … 6=考试 … 7=毕业典礼 … 8=申报补贴 */
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
