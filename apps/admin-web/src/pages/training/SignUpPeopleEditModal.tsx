import { useEffect, useMemo, useRef, useState, type ChangeEvent } from 'react';
import {
  AutoComplete, Button, DatePicker, Form, Input, InputNumber, Modal, Select, Spin, Switch, message,
} from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useMutation, useQuery } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import * as trainApi from '../../api/training';
import { uploadFile } from '../../api/knowledge';
import type { DemandPublicData, TrainSignUpPerson } from '../../types/training';
import { AuthImage } from '../../components/ApiAuthImage';
import { toBrowserPublicFileSrc } from '../../api/files';
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

/** 上传后用于预览/落库的地址：统一走鉴权下载，避免 COS 私有桶直链空白 */
function resolveUploadedImageUrl(result: { id: number | string; url?: string | null }): string {
  return `/api/files/${result.id}/download?inline=true`;
}

type EditableImageField =
  | 'idCardFront'
  | 'idCardBack'
  | 'idCardHeader'
  | 'idCardHeader2'
  | 'schoolPaper'
  | 'contactFirstPage'
  | 'contactExpirePage'
  | 'contactSignPage'
  | 'workTypePaper'
  | 'titlePaper';

function pickImageUrl(...candidates: (string | undefined)[]): string {
  for (const raw of candidates) {
    if (!raw?.trim()) continue;
    const url = raw.trim();
    if (url.includes('/exam/files/') || url.startsWith('/api/exam/files/')) {
      return toBrowserPublicFileSrc(url);
    }
    if (url.includes('/files/') && url.includes('/download')) {
      // 统一成可被 ApiAuthImage 识别的路径
      const idx = url.indexOf('/files/');
      return idx >= 0 ? `/api${url.substring(idx)}` : url;
    }
    if (url.startsWith('/api/') || url.startsWith('blob:') || url.startsWith('data:')
      || url.startsWith('http://') || url.startsWith('https://')) {
      return url;
    }
    // 旧相对路径 idCard/... → 走 exam/files
    if (/^(idCard|contract|upload|down)\//i.test(url.replace(/^\//, ''))) {
      return toBrowserPublicFileSrc(`/api/exam/files/${url.replace(/^\//, '')}`);
    }
    return url;
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
  id: number | string;
  title: string;
  value?: string;
  reqLevel?: number;
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
    idcardFront: str('idcardFront', 'idCardFront', 'id_card_front', 'idcard_front'),
    idcardBack: str('idcardBack', 'idCardBack', 'id_card_back', 'idcard_back'),
    idcardHeader: str('idcardHeader', 'idCardHeader', 'id_card_header', 'idcard_header'),
    idCardHeader: str('idCardHeader', 'id_card_header', 'idcardHeader'),
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
    contractFirst: str('contractFirst', 'contract_first', 'contactFirstPage', 'contact_first_page'),
    contractExpire: str('contractExpire', 'contract_expire', 'contactExpirePage', 'contact_expire_page'),
    contractSign: str('contractSign', 'contract_sign', 'contactSignPage', 'contact_sign_page'),
    contactFirstPage: str('contactFirstPage', 'contact_first_page', 'contractFirst'),
    contactExpirePage: str('contactExpirePage', 'contact_expire_page', 'contractExpire'),
    contactSignPage: str('contactSignPage', 'contact_sign_page', 'contractSign'),
    status: num('status'),
    companyStatus: num('companyStatus', 'company_status'),
    hasPay: num('hasPay', 'has_pay'),
  };
}

/** 0 在选项里是「五级/初级工」，也是库字段默认值；无扫码/证书痕迹时按未填写处理 */
function optionalCurrentWorkLevel(person: TrainSignUpPerson): number | undefined {
  const level = person.level;
  if (level == null || Number.isNaN(level) || level < 0) return undefined;
  if (level > 0) return level;
  const scannedOrFilled = Boolean(person.workTypeId)
    || Boolean(person.paperNo?.trim())
    || (person.workTypePaperHas ?? 0) > 0
    || Boolean(person.workTypePaper?.trim());
  return scannedOrFilled ? 0 : undefined;
}

/** 班级工种等级 5=五级…1=一级 → 报名人员表单 0=五级…4=一级 */
function classLevelToPersonReqLevel(classLevel?: number): number | undefined {
  if (classLevel == null || classLevel < 1 || classLevel > 5) return undefined;
  return 5 - classLevel;
}

function optionalReqLevel(person: TrainSignUpPerson): number | undefined {
  const level = person.reqLevel;
  if (level == null || Number.isNaN(level) || level < 0) return undefined;
  if (level > 0) return level;
  return person.reqWorkTypeId ? 0 : undefined;
}

function resolveWorkTypeOptionValue(
  id: unknown,
  title: string | undefined,
  options: Array<{ value: number; label: string }>,
): number | undefined {
  if (options.length === 0) return undefined;
  const byId = id != null && id !== '' && Number(id) > 0
    ? options.find((o) => String(o.value) === String(id))
    : undefined;
  if (byId) return byId.value;
  const name = title?.trim();
  if (!name) return undefined;
  const byTitle = options.find((o) => o.label === name);
  return byTitle?.value;
}

function personToFormValues(
  person: TrainSignUpPerson,
  classDefaults?: { workTypeId?: number; workTypeTitle?: string; workTypeLevel?: number },
  workTypeOptions: Array<{ value: number; label: string }> = [],
): PeopleEditFormValues {
  const ownReqWorkTypeId = person.reqWorkTypeId && person.reqWorkTypeId > 0
    ? person.reqWorkTypeId
    : undefined;
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
    workTypeId: person.workTypeId || undefined,
    level: optionalCurrentWorkLevel(person),
    paperNo: person.paperNo,
    reqWorkTypeId: ownReqWorkTypeId ?? resolveWorkTypeOptionValue(
      classDefaults?.workTypeId,
      classDefaults?.workTypeTitle,
      workTypeOptions,
    ) ?? (classDefaults?.workTypeId && classDefaults.workTypeId > 0
      ? classDefaults.workTypeId
      : undefined),
    reqLevel: optionalReqLevel(person)
      ?? classLevelToPersonReqLevel(classDefaults?.workTypeLevel),
    declarationCondition: person.declarationCondition,
    status: person.status ?? 0,
    companyStatus: person.companyStatus ?? 0,
    hasPay: person.hasPay ?? 0,
  };
}

function formValuesToPayload(
  values: PeopleEditFormValues,
  images: Partial<Record<EditableImageField, string>>,
): Partial<TrainSignUpPerson> {
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
    declarationCondition: values.declarationCondition ?? '',
    status: values.status,
    companyStatus: values.companyStatus,
    hasPay: values.hasPay,
    idCardFront: images.idCardFront,
    idCardBack: images.idCardBack,
    idCardHeader: images.idCardHeader,
    idCardHeader2: images.idCardHeader2,
    schoolPaper: images.schoolPaper,
    contactFirstPage: images.contactFirstPage,
    contactExpirePage: images.contactExpirePage,
    contactSignPage: images.contactSignPage,
    workTypePaper: images.workTypePaper,
    titlePaper: images.titlePaper,
  };
}

function ImagePreviewBox({
  label,
  url,
  boxClass,
  editable,
  uploading,
  onUpload,
}: {
  label: string;
  url?: string;
  boxClass: string;
  editable?: boolean;
  uploading?: boolean;
  onUpload?: (file: File) => Promise<void>;
}) {
  const inputRef = useRef<HTMLInputElement>(null);

  if (!editable || !onUpload) {
    return (
      <div className="people-edit-image-block">
        <div className="people-edit-image-label">{label}</div>
        <div className={`people-edit-image-box ${boxClass}`}>
          {url ? (
            <AuthImage
              url={url}
              alt={label}
              style={{ width: '100%', height: '100%', objectFit: 'fill', display: 'block' }}
            />
          ) : null}
        </div>
      </div>
    );
  }

  const openPicker = () => {
    if (uploading) return;
    inputRef.current?.click();
  };

  const onFileChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    // 允许连续选择同一文件
    event.target.value = '';
    if (!file) return;
    const lower = file.name.toLowerCase();
    const okExt = /\.(jpe?g|jfif|png|webp|gif|bmp|heic|heif)$/i.test(lower);
    const okType = !file.type || file.type.startsWith('image/');
    if (!okExt && !okType) {
      message.error('请选择 JPG/PNG/WEBP 等图片文件');
      return;
    }
    try {
      await onUpload(file);
    } catch {
      // 错误已在 handleImageUpload 中提示
    }
  };

  return (
    <div className="people-edit-image-block">
      <div className="people-edit-image-label">{label}</div>
      <input
        ref={inputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp,image/gif,image/bmp,.jpg,.jpeg,.jfif,.png,.webp,.gif,.bmp,.heic,.heif"
        style={{ display: 'none' }}
        onChange={onFileChange}
      />
      <div
        role="button"
        tabIndex={0}
        className={`people-edit-image-box ${boxClass} people-edit-image-box-editable${uploading ? ' is-uploading' : ''}`}
        title="点击更换图片"
        onClick={openPicker}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            openPicker();
          }
        }}
      >
        {url ? (
          <AuthImage
            url={url}
            alt={label}
            style={{ width: '100%', height: '100%', objectFit: 'fill', pointerEvents: 'none', display: 'block' }}
          />
        ) : (
          <div className="people-edit-image-empty">
            <PlusOutlined />
            <span>点击上传</span>
          </div>
        )}
        <div className="people-edit-image-mask">
          {uploading ? '上传中…' : (url ? '点击更换' : '点击上传')}
        </div>
      </div>
    </div>
  );
}

export interface SignUpPeopleEditModalProps {
  open: boolean;
  personId: number | null;
  onClose: () => void;
  onSaved: () => void;
  /** 资料收集所属班级的预约工种/等级（1-5），人员未填时回填，与列表一致 */
  classWorkTypeId?: number;
  classWorkTypeTitle?: string;
  classWorkTypeLevel?: number;
}

export function SignUpPeopleEditModal({
  open,
  personId,
  onClose,
  onSaved,
  classWorkTypeId,
  classWorkTypeTitle,
  classWorkTypeLevel,
}: SignUpPeopleEditModalProps) {
  const [form] = Form.useForm<PeopleEditFormValues>();
  const [detail, setDetail] = useState<TrainSignUpPerson | null>(null);
  const [imageDrafts, setImageDrafts] = useState<Partial<Record<EditableImageField, string>>>({});
  const [uploadingField, setUploadingField] = useState<EditableImageField | null>(null);
  const reqLevel = Form.useWatch('reqLevel', form);

  const { data: publicData } = useQuery({
    queryKey: ['training-public-data', 'company', 'worktype', 'condition'],
    queryFn: () => trainApi.getDemandPublicData(['company', 'worktype', 'condition']),
    enabled: open,
    staleTime: 5 * 60 * 1000,
  });

  const { isLoading: detailLoading } = useQuery({
    queryKey: ['sign-up-person-detail', personId, classWorkTypeId, classWorkTypeLevel],
    queryFn: async () => {
      try {
        const raw = await trainApi.getSignUpPerson(personId!);
        const person = normalizePersonDetail(raw as unknown as Record<string, unknown>);
        setDetail(person);
        setImageDrafts({
          idCardFront: pickImageUrl(person.idcardFront, person.idCardFront) || undefined,
          idCardBack: pickImageUrl(person.idcardBack, person.idCardBack) || undefined,
          idCardHeader: pickImageUrl(person.idcardHeader, person.idCardHeader) || undefined,
          idCardHeader2: pickImageUrl(person.idCardHeader2) || undefined,
          schoolPaper: pickImageUrl(person.schoolPaper) || undefined,
          contactFirstPage: pickImageUrl(person.contractFirst, person.contactFirstPage) || undefined,
          contactExpirePage: pickImageUrl(person.contractExpire, person.contactExpirePage) || undefined,
          contactSignPage: pickImageUrl(person.contractSign, person.contactSignPage) || undefined,
          workTypePaper: pickImageUrl(person.workTypePaper) || undefined,
          titlePaper: pickImageUrl(person.titlePaper) || undefined,
        });
        form.setFieldsValue(personToFormValues(person, {
          workTypeId: classWorkTypeId,
          workTypeTitle: classWorkTypeTitle,
          workTypeLevel: classWorkTypeLevel,
        }));
        return person;
      } catch (err) {
        // 列表可能仍缓存已删除行：关掉弹窗并刷新列表
        onClose();
        onSaved();
        throw err;
      }
    },
    enabled: open && !!personId,
    retry: false,
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
      setImageDrafts({});
      setUploadingField(null);
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

  useEffect(() => {
    if (!open || !detail || workTypeOptions.length === 0) return;
    const values = personToFormValues(detail, {
      workTypeId: classWorkTypeId,
      workTypeTitle: classWorkTypeTitle,
      workTypeLevel: classWorkTypeLevel,
    }, workTypeOptions);
    const current = form.getFieldValue('reqWorkTypeId');
    if ((current == null || current === '') && values.reqWorkTypeId != null) {
      form.setFieldValue('reqWorkTypeId', values.reqWorkTypeId);
    }
  }, [
    open,
    detail,
    workTypeOptions,
    classWorkTypeId,
    classWorkTypeTitle,
    classWorkTypeLevel,
    form,
  ]);

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

  const handleImageUpload = async (field: EditableImageField, file: File) => {
    setUploadingField(field);
    // 先本地预览，上传成功后再换成服务端地址
    const localPreview = URL.createObjectURL(file);
    setImageDrafts((prev) => {
      const old = prev[field];
      if (old?.startsWith('blob:')) URL.revokeObjectURL(old);
      return { ...prev, [field]: localPreview };
    });
    try {
      const result = await uploadFile(file, 'public');
      const url = resolveUploadedImageUrl(result);
      setImageDrafts((prev) => {
        const old = prev[field];
        if (old?.startsWith('blob:')) URL.revokeObjectURL(old);
        return { ...prev, [field]: url };
      });
      message.success('图片已更新，请点击保存生效');
    } catch (err) {
      setImageDrafts((prev) => {
        const old = prev[field];
        if (old?.startsWith('blob:')) URL.revokeObjectURL(old);
        const next = { ...prev };
        delete next[field];
        return next;
      });
      const msg = err instanceof Error && err.message ? err.message : '图片上传失败';
      message.error(msg);
    } finally {
      setUploadingField(null);
    }
  };

  const handleSave = async () => {
    if (!personId) return;
    try {
      const values = await form.validateFields();
      saveMu.mutate(formValuesToPayload(values, imageDrafts));
    } catch {
      // 表单校验
    }
  };

  const idFront = pickImageUrl(imageDrafts.idCardFront, detail?.idcardFront, detail?.idCardFront);
  const idBack = pickImageUrl(imageDrafts.idCardBack, detail?.idcardBack, detail?.idCardBack);
  const photoHeader = pickImageUrl(imageDrafts.idCardHeader, detail?.idcardHeader, detail?.idCardHeader);
  const photoOriginal = pickImageUrl(imageDrafts.idCardHeader2, detail?.idCardHeader2);
  const schoolPaper = pickImageUrl(imageDrafts.schoolPaper, detail?.schoolPaper);
  const contractFirst = pickImageUrl(imageDrafts.contactFirstPage, detail?.contractFirst, detail?.contactFirstPage);
  const contractExpire = pickImageUrl(imageDrafts.contactExpirePage, detail?.contractExpire, detail?.contactExpirePage);
  const contractSign = pickImageUrl(imageDrafts.contactSignPage, detail?.contractSign, detail?.contactSignPage);
  const workTypePaper = pickImageUrl(imageDrafts.workTypePaper, detail?.workTypePaper);
  const titlePaper = pickImageUrl(imageDrafts.titlePaper, detail?.titlePaper);
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
                <ImagePreviewBox
                  label="身份证正面"
                  url={idFront}
                  boxClass="id-card"
                  editable
                  uploading={uploadingField === 'idCardFront'}
                  onUpload={(file) => handleImageUpload('idCardFront', file)}
                />
                <ImagePreviewBox
                  label="身份证反面"
                  url={idBack}
                  boxClass="id-card"
                  editable
                  uploading={uploadingField === 'idCardBack'}
                  onUpload={(file) => handleImageUpload('idCardBack', file)}
                />
                <ImagePreviewBox
                  label="一寸证件照"
                  url={photoHeader}
                  boxClass="photo"
                  editable
                  uploading={uploadingField === 'idCardHeader'}
                  onUpload={(file) => handleImageUpload('idCardHeader', file)}
                />
                <ImagePreviewBox
                  label="原证件照"
                  url={photoOriginal}
                  boxClass="photo"
                  editable
                  uploading={uploadingField === 'idCardHeader2'}
                  onUpload={(file) => handleImageUpload('idCardHeader2', file)}
                />
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
                <ImagePreviewBox
                  label="毕业证"
                  url={schoolPaper}
                  boxClass="id-card"
                  editable
                  uploading={uploadingField === 'schoolPaper'}
                  onUpload={(file) => handleImageUpload('schoolPaper', file)}
                />
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
                  {allConditions.length > 0 ? (
                    <AutoComplete
                      className="people-input"
                      allowClear
                      options={conditionOptions}
                      filterOption={(input, option) =>
                        String(option?.value ?? '').toLowerCase().includes(input.trim().toLowerCase())
                      }
                      placeholder={reqLevel == null ? '请选择或输入申报条件（建议先选预约等级）' : '请选择或输入申报条件'}
                    />
                  ) : (
                    <Input className="people-input" placeholder="请输入申报条件" maxLength={500} showCount />
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
                  <ImagePreviewBox
                    label="合同第一页"
                    url={contractFirst}
                    boxClass="contract"
                    editable
                    uploading={uploadingField === 'contactFirstPage'}
                    onUpload={(file) => handleImageUpload('contactFirstPage', file)}
                  />
                  <ImagePreviewBox
                    label="合同到期页"
                    url={contractExpire}
                    boxClass="contract"
                    editable
                    uploading={uploadingField === 'contactExpirePage'}
                    onUpload={(file) => handleImageUpload('contactExpirePage', file)}
                  />
                  <ImagePreviewBox
                    label="合同盖章页"
                    url={contractSign}
                    boxClass="contract"
                    editable
                    uploading={uploadingField === 'contactSignPage'}
                    onUpload={(file) => handleImageUpload('contactSignPage', file)}
                  />
                </div>
                <div className="people-edit-cert-row">
                  <ImagePreviewBox
                    label="上一等级证书"
                    url={workTypePaper}
                    boxClass="id-card"
                    editable
                    uploading={uploadingField === 'workTypePaper'}
                    onUpload={(file) => handleImageUpload('workTypePaper', file)}
                  />
                  <ImagePreviewBox
                    label="职称证书"
                    url={titlePaper}
                    boxClass="id-card"
                    editable
                    uploading={uploadingField === 'titlePaper'}
                    onUpload={(file) => handleImageUpload('titlePaper', file)}
                  />
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
