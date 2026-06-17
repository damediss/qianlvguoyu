import { useEffect, useState } from 'react';
import {
  DatePicker,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  message,
} from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createExam,
  updateExam,
  getExam,
  pageInfoLists,
  importExamPeopleFromTrain,
} from '../../api/training';
import { pageCourses as pageExamCourses } from '../../api/exam';
import type { TrainExamCreateReq, TrainExamUpdateReq } from '../../types/training';

interface FormValues {
  title: string;
  level?: string;
  type?: number;
  source?: number;
  trainId?: number;
  courseIdList?: number[];
  examDate?: Dayjs | string;
  examTime?: number;
  sortNumber?: number;
  invigilateUsername?: string;
  evaluateUsername?: string;
  watcherUsername?: string;
  staffUsername?: string;
  propositionUsername?: string;
  markingUsername?: string;
  examDateMode?: number;
}

interface Props {
  open: boolean;
  editingId?: number;
  onClose: () => void;
}

export function TrainExamListFormModal({ open, editingId, onClose }: Props) {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<FormValues>();
  const isEdit = editingId != null;

  // 培训下拉
  const { data: trainList } = useQuery({
    queryKey: ['train-exam-form-train-list', open],
    queryFn: () => pageInfoLists({ page: 1, pageSize: 999 }),
    enabled: open,
  });
  // 课程下拉
  const { data: courseList } = useQuery({
    queryKey: ['train-exam-form-course-list', open],
    queryFn: () => pageExamCourses({ page: 1, pageSize: 999 }),
    enabled: open,
  });
  // 编辑时回填
  const { data: detail } = useQuery({
    queryKey: ['train-exam-detail', editingId],
    queryFn: () => getExam(editingId as number),
    enabled: open && isEdit,
  });

  useEffect(() => {
    if (open && isEdit && detail) {
      form.setFieldsValue({
        title: detail.title,
        level: detail.level,
        type: detail.type,
        source: detail.source,
        trainId: detail.trainId,
        courseIdList: detail.courseIdList,
        examDate: detail.examDate ? dayjs(detail.examDate) : undefined,
        examTime: detail.examTime,
        sortNumber: detail.sortNumber,
        invigilateUsername: detail.invigilateUsername,
        evaluateUsername: detail.evaluateUsername,
        watcherUsername: detail.watcherUsername,
        staffUsername: detail.staffUsername,
        propositionUsername: detail.propositionUsername,
        markingUsername: detail.markingUsername,
      });
    } else if (open && !isEdit) {
      form.resetFields();
      form.setFieldsValue({ type: 0, source: 0, sortNumber: 1 });
    }
  }, [open, isEdit, detail, form]);

  const saveMu = useMutation({
    mutationFn: async (values: FormValues) => {
      const payload: TrainExamCreateReq = {
        ...values,
        examDate: values.examDate
          ? (typeof values.examDate === 'string' ? values.examDate : (values.examDate as Dayjs).format('YYYY-MM-DD'))
          : undefined,
      };
      if (isEdit) {
        return updateExam(editingId as number, payload as TrainExamUpdateReq);
      }
      return createExam(payload);
    },
    onSuccess: async (created, values) => {
      if (!isEdit && values.trainId && created?.id) {
        // 选了培训 → 创建后自动导入考生
        try {
          const r = await importExamPeopleFromTrain(created.id, values.trainId);
          message.success(`已导入 ${r.count} 名考生`);
        } catch {
          message.warning('考生自动导入失败，可稍后在「考生」按钮中重试');
        }
      } else {
        message.success(isEdit ? '修改成功' : '新增成功');
      }
      queryClient.invalidateQueries({ queryKey: ['train-exam-list'] });
      onClose();
    },
    onError: () => message.error(isEdit ? '修改失败' : '新增失败'),
  });

  return (
    <Modal
      title={isEdit ? '编辑考试计划' : '新增考试计划'}
      open={open}
      width={720}
      destroyOnClose
      onCancel={onClose}
      onOk={() => form.submit()}
      confirmLoading={saveMu.isPending}
    >
      <Form
        form={form}
        layout="vertical"
        onFinish={v => saveMu.mutate(v as FormValues)}
        initialValues={{ type: 0, source: 0, sortNumber: 1 }}
      >
        <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入标题' }]}>
          <Input placeholder="请输入考试计划标题" />
        </Form.Item>
        <Space.Compact block>
          <Form.Item name="level" label="等级" style={{ width: '25%' }}>
            <Input placeholder="如：初级" />
          </Form.Item>
          <Form.Item name="type" label="类型" style={{ width: '25%', marginLeft: 8 }}>
            <Select options={[{ value: 0, label: '线上' }, { value: 1, label: '线下' }]} />
          </Form.Item>
          <Form.Item name="source" label="来源" style={{ width: '25%', marginLeft: 8 }}>
            <Select options={[{ value: 0, label: '内部' }, { value: 1, label: '外部' }]} />
          </Form.Item>
          <Form.Item name="sortNumber" label="排序号" style={{ width: '20%', marginLeft: 8 }}>
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
        </Space.Compact>
        <Form.Item name="trainId" label="关联培训">
          <Select
            allowClear
            showSearch
            placeholder="选择培训（用于自动导入考生）"
            optionFilterProp="label"
            options={(trainList?.list ?? []).map(t => ({ value: t.id, label: t.title }))}
          />
        </Form.Item>
        <Form.Item name="courseIdList" label="关联课程（多选）">
          <Select
            mode="multiple"
            allowClear
            placeholder="选择课程，将自动回填工种/等级"
            optionFilterProp="label"
            options={(courseList?.items ?? []).map(c => ({ value: c.id, label: c.title }))}
          />
        </Form.Item>
        <Space.Compact block>
          <Form.Item name="examDate" label="考试日期" style={{ width: '50%' }}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="examTime" label="考试时长(分钟)" style={{ width: '50%', marginLeft: 8 }}>
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
        </Space.Compact>
        <details>
          <summary style={{ cursor: 'pointer', color: '#888' }}>考务人员（选填）</summary>
          <Space.Compact block style={{ marginTop: 8 }}>
            <Form.Item name="invigilateUsername" label="监考人" style={{ width: '50%' }}>
              <Input />
            </Form.Item>
            <Form.Item name="evaluateUsername" label="考评人" style={{ width: '50%', marginLeft: 8 }}>
              <Input />
            </Form.Item>
          </Space.Compact>
          <Space.Compact block>
            <Form.Item name="watcherUsername" label="督导员" style={{ width: '50%' }}>
              <Input />
            </Form.Item>
            <Form.Item name="staffUsername" label="考务人员" style={{ width: '50%', marginLeft: 8 }}>
              <Input />
            </Form.Item>
          </Space.Compact>
          <Space.Compact block>
            <Form.Item name="propositionUsername" label="命题人" style={{ width: '50%' }}>
              <Input />
            </Form.Item>
            <Form.Item name="markingUsername" label="阅卷人" style={{ width: '50%', marginLeft: 8 }}>
              <Input />
            </Form.Item>
          </Space.Compact>
        </details>
      </Form>
    </Modal>
  );
}
