import { useEffect, useMemo, useState } from 'react';
import {
  Button, DatePicker, Form, Image, Input, InputNumber, Modal, Select, Spin, Switch, message,
} from 'antd';
import { useMutation, useQuery } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import * as trainApi from '../../api/training';
import type { DemandPublicData, TrainSignUpPerson } from '../../types/training';
import {
  EDUCATION_OPTIONS,
  HOUSE_TYPE_OPTIONS,
  PAY_OPTIONS,
  POLITICAL_OPTIONS,
  STATUS_OPTIONS,
  TITLE_TYPE_OPTIONS,
  WORK_LEVEL_OPTIONS,
  filterDeclarationConditions,
} from './signUpPeopleEditConstants';
import './SignUpPeopleEditModal.css';

const DATE_FORMAT = 'YYYY-MM-DD';

function toProxiedCdnUrl(url?: string): string {
  if (!url) return '';
  const marker = '/exam/files/';
  const idx = url.indexOf(marker);
  if (idx >= 0) return `/api${url.substring(idx)}`;
  if (url.startsWith('/api/')) return url;
  return url;
}

function pickImageUrl(...candidates: (string | undefined)[]): string {
  for (const url of candidates) {
    if (url) return toProxiedCdnUrl(url);
  }
  return '';
}

function parseDateValue(value?: string): Dayjs | undefined {
  if (!value?.trim()) return undefined;
  const d = dayjs(value, DATE_FORMAT);
  return d.isValid() ? d : undefined;
}

function formatDateValue(value?: Dayjs | null): string | undefined {
  if (!value || !value.isValid()) return undefined;
  return value.format(DATE_FORMAT);
}

interface PeopleEditFormValues {
  platformUserId?: number;
  name?: string;
  nation?: string;
  political?: string;
  houseType?: string;
  idCardNumber?: string;
  address?: string;
  phone?: string;
  school?: string;
  inSchoolDate?: Dayjs;
  graduateDate?: Dayjs;
  education?: string;
  major?: string;
  schoolPaperHas?: boolean;
  schoolNo?: string;
  workTime?: Dayjs;
  titleType?: string;
  companyId?: number;
  workDateStart?: Dayjs;
  workDateEnd?: Dayjs;
  workTypePaperHas?: boolean;
  workTypeId?: number;
  level?: number;
  paperNo?: string;
  reqWorkTypeId?: number;
  reqLevel?: number;
  declarationCondition?: string;
  status?: number;
  companyStatus?: number;
  hasPay?: number;
}

interface ConditionOption {
  id: number;
  title: string;
}

function normalizePersonDetail(raw: Record<string, unknown>): TrainSignUpPerson {
  const pick = (...keys: string[]) => {
    for (const key of keys) {
      const v = raw[key];
      if (v !== undefined && v !== null && v !== '') return v;
    }
    return undefined;
  };
  const num = (...keys: string[]) => {
    const v = pick(...keys);
    return v != null ? Number(v) : undefined;
  };
  const str = (...keys: string[]) => {
    const v = pick(...keys);
    return v != null ? String(v) : undefined;
  };
  return {
    id: Number(pick('id')),
    signUpId: num('signUpId', 'sign_up_id'),
    platformUserId: num('platformUserId', 'platform_user_id'),
    name: str('name'),
    phone: str('phone'),
    idCardNumber: str('idCardNumber', 'id_card_number'),
    nation: str('nation'),
    political: str('political'),
    houseType: str('houseType', 'house_type'),
    address: str('address'),
    education: str('education'),
    idcardFront: str('idcardFront', 'idcard_front'),
    idcardBack: str('idcardBack', 'idcard_back'),
    idcardHeader: str('idcardHeader', 'idcard_header'),
    idCardHeader: str('idCardHeader', 'id_card_header'),
    idCardHeader2: str('idCardHeader2', 'id_card_header2', 'id_card_header'),
    school: str('school'),
    major: str('major'),
    schoolPaperHas: num('schoolPaperHas', 'school_paper_has'),
    schoolNo: str('schoolNo', 'school_no'),
    inSchoolDate: str('inSchoolDate', 'in_school_date'),
    graduateDate: str('graduateDate', 'graduate_date'),
    schoolPaper: str('schoolPaper', 'school_paper'),
    workTime: str('workTime', 'work_time'),
    titleType: str('titleType', 'title_type'),
    titlePaper: str('titlePaper', 'title_paper'),
    companyId: num('companyId', 'company_id'),
    workDateStart: str('workDateStart', 'work_date_start'),
    workDateEnd: str('workDateEnd', 'work_date_end'),
    workTypePaperHas: num('workTypePaperHas', 'work_type_paper_has'),
    workTypePaper: str('workTypePaper', 'work_type_paper'),
    workTypeId: num('workTypeId', 'work_type_id'),
    level: num('level'),
    paperNo: str('paperNo', 'paper_no'),
    reqWorkTypeId: num('reqWorkTypeId', 'req_work_type_id'),
    reqLevel: num('reqLevel', 'req_level'),
    declarationCondition: str('declarationCondition', 'condition'),
    workExpList: Array.isArray(raw.workExpList) ? raw.workExpList as Record<string, unknown>[] : undefined,
    contractFirst: str('contractFirst', 'contract_first'),
    contractExpire: str('contractExpire', 'contract_expire'),
    contractSign: str('contractSign', 'contract_sign'),
    status: num('status'),
    companyStatus: num('companyStatus', 'company_status'),
    hasPay: num('hasPay', 'has_pay'),
  };
}

function personToFormValues(person: TrainSignUpPerson): PeopleEditFormValues {
  return {
    platformUserId: person.platformUserId,
    name: person.name,
    nation: person.nation,
    political: person.political,
    houseType: person.houseType,
    idCardNumber: person.idCardNumber,
    address: person.address,
    phone: person.phone,
    school: person.school,
    inSchoolDate: parseDateValue(person.inSchoolDate),
    graduateDate: parseDateValue(person.graduateDate),
    education: person.education,
    major: person.major,
    schoolPaperHas: (person.schoolPaperHas ?? 0) > 0,
    schoolNo: person.schoolNo,
    workTime: parseDateValue(person.workTime),
    titleType: person.titleType,
    companyId: person.companyId,
    workDateStart: parseDateValue(person.workDateStart),
    workDateEnd: parseDateValue(person.workDateEnd),
    workTypePaperHas: (person.workTypePaperHas ?? 0) > 0,
    workTypeId: person.workTypeId,
    level: person.level,
    paperNo: person.paperNo,
    reqWorkTypeId: person.reqWorkTypeId,
    reqLevel: person.reqLevel,
    declarationCondition: person.declarationCondition,
    status: person.status ?? 0,
    companyStatus: person.companyStatus ?? 0,
    hasPay: person.hasPay ?? 0,
  };
}

function formValuesToPayload(values: PeopleEditFormValues): Partial<TrainSignUpPerson> {
  return {
    platformUserId: values.platformUserId,
    name: values.name,
    nation: values.nation,
    political: values.political,
    houseType: values.houseType,
    idCardNumber: values.idCardNumber,
    address: values.address,
    phone: values.phone,
    school: values.school,
    inSchoolDate: formatDateValue(values.inSchoolDate),
    graduateDate: formatDateValue(values.graduateDate),
    education: values.education,
    major: values.major,
    schoolPaperHas: values.schoolPaperHas ? 1 : 0,
    schoolNo: values.schoolNo,
    workTime: formatDateValue(values.workTime),
    titleType: values.titleType,
    companyId: values.companyId,
    workDateStart: formatDateValue(values.workDateStart),
    workDateEnd: formatDateValue(values.workDateEnd),
    workTypePaperHas: values.workTypePaperHas ? 1 : 0,
    workTypeId: values.workTypeId,
    level: values.level,
    paperNo: values.paperNo,
    reqWorkTypeId: values.reqWorkTypeId,
    reqLevel: values.reqLevel,
    declarationCondition: values.declarationCondition,
    status: values.status,
    companyStatus: values.companyStatus,
    hasPay: values.hasPay,
  };
}

function ImagePreviewBox({
  label,
  url,
  boxClass,
}: {
  label: string;
  url?: string;
  boxClass: string;
}) {
  const src = url ? toProxiedCdnUrl(url) : '';
  return (
    <div className="people-edit-image-block">
      <div className="people-edit-image-label">{label}</div>
      <div className={`people-edit-image-box ${boxClass}`}>
        {src ? (
          <Image
            src={src}
            width="100%"
            height="100%"
            style={{ objectFit: 'fill' }}
          />
        ) : null}
      </div>
    </div>
  );
}

export interface SignUpPeopleEditModalProps {
  open: boolean;
  personId: number | null;
  onClose: () => void;
  onSaved: () => void;
}

export function SignUpPeopleEditModal({
  open,
  personId,
  onClose,
  onSaved,
}: SignUpPeopleEditModalProps) {
  const [form] = Form.useForm<PeopleEditFormValues>();
  const [detail, setDetail] = useState<TrainSignUpPerson | null>(null);
  const reqLevel = Form.useWatch('reqLevel', form);

  const { data: publicData } = useQuery({
    queryKey: ['training-public-data', 'company', 'worktype', 'condition'],
    queryFn: () => trainApi.getDemandPublicData(['company', 'worktype', 'condition']),
    enabled: open,
    staleTime: 5 * 60 * 1000,
  });

  const { isLoading: detailLoading } = useQuery({
    queryKey: ['sign-up-person-detail', personId],
    queryFn: async () => {
      const raw = await trainApi.getSignUpPerson(personId!);
      const person = normalizePersonDetail(raw as unknown as Record<string, unknown>);
      setDetail(person);
      form.setFieldsValue(personToFormValues(person));
      return person;
    },
    enabled: open && !!personId,
  });

  const saveMu = useMutation({
    mutationFn: (payload: Partial<TrainSignUpPerson>) =>
      trainApi.updateSignUpPerson(personId!, payload),
    onSuccess: () => {
      message.success('保存成功');
      onSaved();
    },
    onError: () => message.error('保存失败'),
  });

  useEffect(() => {
    if (!open) {
      form.resetFields();
      setDetail(null);
    }
  }, [open, form]);

  const companyOptions = useMemo(() => (
    (publicData as DemandPublicData | undefined)?.company_list?.map((c) => ({
      value: c.id,
      label: c.title,
    })) ?? []
  ), [publicData]);

  const workTypeOptions = useMemo(() => {
    const groups = (publicData as DemandPublicData | undefined)?.worktype_list ?? [];
    return groups.flatMap((g) => (g.options ?? []).map((o) => ({ value: o.id, label: o.title })));
  }, [publicData]);

  const allConditions = useMemo((): ConditionOption[] => (
    (publicData as DemandPublicData | undefined)?.condition_list ?? []
  ), [publicData]);

  const conditionOptions = useMemo(() => {
    const filtered = filterDeclarationConditions(allConditions, reqLevel);
    return filtered.map((item) => ({ value: item.title, label: item.title }));
  }, [allConditions, reqLevel]);

  const handleReqLevelChange = (level: number) => {
    form.setFieldValue('reqLevel', level);
    form.setFieldValue('declarationCondition', undefined);
  };

  const handleSave = async () => {
    if (!personId) return;
    try {
      const values = await form.validateFields();
      saveMu.mutate(formValuesToPayload(values));
    } catch {
      // 表单校验
    }
  };

  const idFront = pickImageUrl(detail?.idcardFront, detail?.idCardFront);
  const idBack = pickImageUrl(detail?.idcardBack, detail?.idCardBack);
  const photoHeader = pickImageUrl(detail?.idcardHeader, detail?.idCardHeader);
  const photoOriginal = pickImageUrl(detail?.idCardHeader2, detail?.idCardHeader);
  const schoolPaper = pickImageUrl(detail?.schoolPaper);
  const contractFirst = pickImageUrl(detail?.contractFirst, detail?.contactFirstPage);
  const contractExpire = pickImageUrl(detail?.contractExpire, detail?.contactExpirePage);
  const contractSign = pickImageUrl(detail?.contractSign, detail?.contactSignPage);
  const workTypePaper = pickImageUrl(detail?.workTypePaper);
  const titlePaper = pickImageUrl(detail?.titlePaper);
  const workExpList = detail?.workExpList ?? [];

  return (
    <Modal
      className="people-edit-modal"
      title="报名人员编辑"
      open={open}
      width="80%"
      onCancel={onClose}
      footer={null}
      destroyOnHidden
      maskClosable={false}
      keyboard={false}
    >
      <Spin spinning={detailLoading || saveMu.isPending}>
        <div className="people-edit-modal-body">
          <Form form={form} layout="horizontal" labelCol={{ flex: '160px' }} wrapperCol={{ flex: 1 }} colon={false}>
            <div className="people-edit-section-title">个人基本信息</div>
            <div className="people-edit-row">
              <div className="people-edit-fields">
                <Form.Item name="platformUserId" label="平台ID">
                  <InputNumber className="people-input" controls={false} style={{ width: 400 }} />
                </Form.Item>
                <Form.Item name="name" label="姓名" rules={[{ required: true, message: '请输入姓名' }]}>
                  <Input className="people-input" />
                </Form.Item>
                <Form.Item name="nation" label="民族">
                  <Input className="people-input" />
                </Form.Item>
                <Form.Item name="political" label="政治面貌">
                  <Select className="people-input" allowClear placeholder="请选择" options={POLITICAL_OPTIONS.map((v) => ({ value: v, label: v }))} />
                </Form.Item>
                <Form.Item name="houseType" label="户口性质">
                  <Select className="people-input" allowClear placeholder="请选择" options={HOUSE_TYPE_OPTIONS.map((v) => ({ value: v, label: v }))} />
                </Form.Item>
                <Form.Item name="idCardNumber" label="身份证号">
                  <Input className="people-input" />
                </Form.Item>
                <Form.Item name="address" label="地址">
                  <Input className="people-input" />
                </Form.Item>
                <Form.Item name="phone" label="手机号">
                  <Input className="people-input" />
                </Form.Item>
              </div>
              <div className="people-edit-images">
                <ImagePreviewBox label="身份证正面" url={idFront} boxClass="id-card" />
                <ImagePreviewBox label="身份证反面" url={idBack} boxClass="id-card" />
                <ImagePreviewBox label="一寸证件照" url={photoHeader} boxClass="photo" />
                <ImagePreviewBox label="原证件照" url={photoOriginal} boxClass="photo" />
              </div>
            </div>

            <div className="people-edit-section-title">教育信息</div>
            <div className="people-edit-row">
              <div className="people-edit-fields">
                <Form.Item name="school" label="学校名称">
                  <Input className="people-input" />
                </Form.Item>
                <Form.Item name="inSchoolDate" label="入学日期">
                  <DatePicker className="people-input" style={{ width: 400 }} format={DATE_FORMAT} />
                </Form.Item>
                <Form.Item name="graduateDate" label="毕业日期">
                  <DatePicker className="people-input" style={{ width: 400 }} format={DATE_FORMAT} />
                </Form.Item>
                <Form.Item name="education" label="学历">
                  <Select className="people-input" allowClear placeholder="请选择" options={EDUCATION_OPTIONS.map((v) => ({ value: v, label: v }))} />
                </Form.Item>
                <Form.Item name="major" label="专业">
                  <Input className="people-input" />
                </Form.Item>
                <Form.Item name="schoolPaperHas" label="是否有毕业证" valuePropName="checked">
                  <Switch />
                </Form.Item>
                <Form.Item name="schoolNo" label="毕业证编号">
                  <Input className="people-input" />
                </Form.Item>
              </div>
              <div className="people-edit-images">
                <ImagePreviewBox label="毕业证" url={schoolPaper} boxClass="id-card" />
              </div>
            </div>

            <div className="people-edit-section-title">工作信息</div>
            <div className="people-edit-row">
              <div className="people-edit-fields">
                <Form.Item name="workTime" label="从业时间">
                  <DatePicker className="people-input" style={{ width: 400 }} format={DATE_FORMAT} />
                </Form.Item>
                <Form.Item name="titleType" label="职称">
                  <Select className="people-input" allowClear placeholder="请选择" options={TITLE_TYPE_OPTIONS.map((v) => ({ value: v, label: v }))} />
                </Form.Item>
                <Form.Item name="companyId" label="公司名称">
                  <Select className="people-input" showSearch allowClear placeholder="请选择" optionFilterProp="label" options={companyOptions} />
                </Form.Item>
                <Form.Item name="workDateStart" label="本公司入职日期">
                  <DatePicker className="people-input" style={{ width: 400 }} format={DATE_FORMAT} />
                </Form.Item>
                <Form.Item name="workDateEnd" label="本公司结束日期">
                  <DatePicker className="people-input" style={{ width: 400 }} format={DATE_FORMAT} />
                </Form.Item>
                <Form.Item name="workTypePaperHas" label="是否有上一等级证书" valuePropName="checked">
                  <Switch />
                </Form.Item>
                <Form.Item name="workTypeId" label="现工种">
                  <Select className="people-input" showSearch allowClear placeholder="请选择" optionFilterProp="label" options={workTypeOptions} />
                </Form.Item>
                <Form.Item name="level" label="现工种等级">
                  <Select className="people-input" allowClear placeholder="请选择" options={WORK_LEVEL_OPTIONS} />
                </Form.Item>
                <Form.Item name="paperNo" label="现工种证书编号">
                  <Input className="people-input" />
                </Form.Item>
                <Form.Item name="reqWorkTypeId" label="预约工种">
                  <Select className="people-input" showSearch allowClear placeholder="请选择" optionFilterProp="label" options={workTypeOptions} />
                </Form.Item>
                <Form.Item name="reqLevel" label="预约工种等级">
                  <Select
                    className="people-input"
                    allowClear
                    placeholder="请选择"
                    options={WORK_LEVEL_OPTIONS}
                    onChange={handleReqLevelChange}
                  />
                </Form.Item>
                <Form.Item name="declarationCondition" label="申报条件">
                  {conditionOptions.length > 0 ? (
                    <Select className="people-input" allowClear showSearch placeholder="请选择" options={conditionOptions} />
                  ) : (
                    <Input className="people-input" placeholder="请输入申报条件" />
                  )}
                </Form.Item>
                {workExpList.length > 0 ? (
                  <Form.Item label="工作经历">
                    <div>
                      {workExpList.map((exp, index) => (
                        <div key={`work-exp-${index}`} className="people-edit-work-exp-item">
                          <div>{String(exp.startDate ?? '')}至{String(exp.endDate ?? '')}</div>
                          <div>在{String(exp.company ?? '')}公司从事{String(exp.position ?? '')}工作</div>
                          <div>证明人:{String(exp.proveName ?? '')},联系电话:{String(exp.provePhone ?? '')}</div>
                        </div>
                      ))}
                    </div>
                  </Form.Item>
                ) : null}
              </div>
              <div>
                <div className="people-edit-contract-row">
                  <Image.PreviewGroup>
                    <ImagePreviewBox label="合同第一页" url={contractFirst} boxClass="contract" />
                    <ImagePreviewBox label="合同到期页" url={contractExpire} boxClass="contract" />
                    <ImagePreviewBox label="合同盖章页" url={contractSign} boxClass="contract" />
                  </Image.PreviewGroup>
                </div>
                <div className="people-edit-cert-row">
                  {workTypePaper ? <ImagePreviewBox label="上一等级证书" url={workTypePaper} boxClass="id-card" /> : null}
                  {titlePaper ? <ImagePreviewBox label="职称证书" url={titlePaper} boxClass="id-card" /> : null}
                </div>
              </div>
            </div>

            <div className="people-edit-section-title">状态信息</div>
            <div className="people-edit-status-row">
              <Form.Item name="status" label="审核状态">
                <Select style={{ width: 120 }} options={STATUS_OPTIONS} />
              </Form.Item>
              <Form.Item name="companyStatus" label="企业审核状态">
                <Select style={{ width: 120 }} options={STATUS_OPTIONS} />
              </Form.Item>
              <Form.Item name="hasPay" label="支付状态">
                <Select style={{ width: 120 }} options={PAY_OPTIONS} />
              </Form.Item>
            </div>
          </Form>
        </div>
        <div className="people-edit-footer">
          <Button onClick={onClose} style={{ marginRight: 8 }}>取消</Button>
          <Button type="primary" loading={saveMu.isPending} onClick={handleSave}>保存</Button>
        </div>
      </Spin>
    </Modal>
  );
}
