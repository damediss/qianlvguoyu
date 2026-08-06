import { TrainExamListPage } from './TrainExamListPage';
import { TrainingProcessSteps } from './TrainingProcessSteps';

/** 运营流程 · 考试（与跟班同级，复用考试计划能力） */
export function TrainingFollowExamPage() {
  return (
    <div className="training-page">
      <TrainingProcessSteps current={6} />
      <TrainExamListPage />
    </div>
  );
}
