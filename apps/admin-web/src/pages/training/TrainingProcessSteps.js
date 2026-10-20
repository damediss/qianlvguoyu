import { jsx as _jsx } from "react/jsx-runtime";
import { Card, Steps } from 'antd';
import { AccountBookOutlined, ApartmentOutlined, BulbOutlined, EditOutlined, FormOutlined, PictureOutlined, PlayCircleOutlined, SolutionOutlined, TrophyOutlined, } from '@ant-design/icons';
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
];
export const TRAINING_PROCESS_STEPS = [
    { title: '企业调研', icon: _jsx(BulbOutlined, {}) },
    { title: '方案规划', icon: _jsx(SolutionOutlined, {}) },
    { title: '分班', icon: _jsx(ApartmentOutlined, {}) },
    { title: '开班配置', icon: _jsx(PictureOutlined, {}) },
    { title: '开班申报', icon: _jsx(EditOutlined, {}) },
    { title: '跟班', icon: _jsx(PlayCircleOutlined, {}) },
    { title: '考试', icon: _jsx(FormOutlined, {}) },
    { title: '毕业典礼', icon: _jsx(TrophyOutlined, {}) },
    { title: '申报补贴', icon: _jsx(AccountBookOutlined, {}) },
];
export function TrainingProcessSteps({ current, stepRoutes = TRAINING_PROCESS_STEP_ROUTES }) {
    const navigateTo = useNavStore((s) => s.setSelectedKey);
    const handleChange = (step) => {
        if (step === current)
            return;
        const route = stepRoutes[step];
        if (route)
            navigateTo(route);
    };
    return (_jsx(Card, { size: "small", style: { marginBottom: 16 }, children: _jsx(Steps, { className: "training-process-steps", current: current, size: "small", items: TRAINING_PROCESS_STEPS, onChange: handleChange }) }));
}
