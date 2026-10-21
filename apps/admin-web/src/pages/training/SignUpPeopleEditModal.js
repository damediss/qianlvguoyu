import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect, useMemo, useRef, useState } from 'react';
import { AutoComplete, Button, DatePicker, Form, Input, InputNumber, Modal, Select, Spin, Switch, message, } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useMutation, useQuery } from '@tanstack/react-query';
import dayjs from 'dayjs';
import * as trainApi from '../../api/training';
import { uploadFile } from '../../api/knowledge';
import { AuthImage } from '../../components/ApiAuthImage';
import { toBrowserPublicFileSrc } from '../../api/files';
import { EDUCATION_OPTIONS, HOUSE_TYPE_OPTIONS, PAY_OPTIONS, POLITICAL_OPTIONS, STATUS_OPTIONS, TITLE_TYPE_OPTIONS, WORK_LEVEL_OPTIONS, filterDeclarationConditions, } from './signUpPeopleEditConstants';
import './SignUpPeopleEditModal.css';
const DATE_FORMAT = 'YYYY-MM-DD';
/** 上传后用于预览/落库的地址：统一走鉴权下载，避免 COS 私有桶直链空白 */
function resolveUploadedImageUrl(result) {
    return `/api/files/${result.id}/download?inline=true`;
}
function pickImageUrl(...candidates) {
    for (const raw of candidates) {
        if (!raw?.trim())
            continue;
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
function parseDateValue(value) {
    if (!value?.trim())
        return undefined;
    const d = dayjs(value, DATE_FORMAT);
    return d.isValid() ? d : undefined;
}
function formatDateValue(value) {
    if (!value || !value.isValid())
        return undefined;
    return value.format(DATE_FORMAT);
}
function normalizePersonDetail(raw) {
    const pick = (...keys) => {
        for (const key of keys) {
            const v = raw[key];
            if (v !== undefined && v !== null && v !== '')
                return v;
        }
        return undefined;
    };
    const num = (...keys) => {
        const v = pick(...keys);
        return v != null ? Number(v) : undefined;
    };
    const str = (...keys) => {
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
        workExpList: Array.isArray(raw.workExpList) ? raw.workExpList : undefined,
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
function optionalCurrentWorkLevel(person) {
    const level = person.level;
    if (level == null || Number.isNaN(level) || level < 0)
        return undefined;
    if (level > 0)
        return level;
    const scannedOrFilled = Boolean(person.workTypeId)
        || Boolean(person.paperNo?.trim())
        || (person.workTypePaperHas ?? 0) > 0
        || Boolean(person.workTypePaper?.trim());
    return scannedOrFilled ? 0 : undefined;
}
/** 班级工种等级 5=五级…1=一级 → 报名人员表单 0=五级…4=一级 */
function classLevelToPersonReqLevel(classLevel) {
    if (classLevel == null || classLevel < 1 || classLevel > 5)
        return undefined;
    return 5 - classLevel;
}
function optionalReqLevel(person) {
    const level = person.reqLevel;
    if (level == null || Number.isNaN(level) || level < 0)
        return undefined;
    if (level > 0)
        return level;
    return person.reqWorkTypeId ? 0 : undefined;
}
function resolveWorkTypeOptionValue(id, title, options) {
    if (options.length === 0)
        return undefined;
    const byId = id != null && id !== '' && Number(id) > 0
        ? options.find((o) => String(o.value) === String(id))
        : undefined;
    if (byId)
        return byId.value;
    const name = title?.trim();
    if (!name)
        return undefined;
    const byTitle = options.find((o) => o.label === name);
    return byTitle?.value;
}
function personToFormValues(person, classDefaults, workTypeOptions = []) {
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
        reqWorkTypeId: ownReqWorkTypeId ?? resolveWorkTypeOptionValue(classDefaults?.workTypeId, classDefaults?.workTypeTitle, workTypeOptions) ?? (classDefaults?.workTypeId && classDefaults.workTypeId > 0
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
function formValuesToPayload(values, images) {
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
function ImagePreviewBox({ label, url, boxClass, editable, uploading, onUpload, }) {
    const inputRef = useRef(null);
    if (!editable || !onUpload) {
        return (_jsxs("div", { className: "people-edit-image-block", children: [_jsx("div", { className: "people-edit-image-label", children: label }), _jsx("div", { className: `people-edit-image-box ${boxClass}`, children: url ? (_jsx(AuthImage, { url: url, alt: label, style: { width: '100%', height: '100%', objectFit: 'fill', display: 'block' } })) : null })] }));
    }
    const openPicker = () => {
        if (uploading)
            return;
        inputRef.current?.click();
    };
    const onFileChange = async (event) => {
        const file = event.target.files?.[0];
        // 允许连续选择同一文件
        event.target.value = '';
        if (!file)
            return;
        const lower = file.name.toLowerCase();
        const okExt = /\.(jpe?g|jfif|png|webp|gif|bmp|heic|heif)$/i.test(lower);
        const okType = !file.type || file.type.startsWith('image/');
        if (!okExt && !okType) {
            message.error('请选择 JPG/PNG/WEBP 等图片文件');
            return;
        }
        try {
            await onUpload(file);
        }
        catch {
            // 错误已在 handleImageUpload 中提示
        }
    };
    return (_jsxs("div", { className: "people-edit-image-block", children: [_jsx("div", { className: "people-edit-image-label", children: label }), _jsx("input", { ref: inputRef, type: "file", accept: "image/jpeg,image/png,image/webp,image/gif,image/bmp,.jpg,.jpeg,.jfif,.png,.webp,.gif,.bmp,.heic,.heif", style: { display: 'none' }, onChange: onFileChange }), _jsxs("div", { role: "button", tabIndex: 0, className: `people-edit-image-box ${boxClass} people-edit-image-box-editable${uploading ? ' is-uploading' : ''}`, title: "\u70B9\u51FB\u66F4\u6362\u56FE\u7247", onClick: openPicker, onKeyDown: (e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        openPicker();
                    }
                }, children: [url ? (_jsx(AuthImage, { url: url, alt: label, style: { width: '100%', height: '100%', objectFit: 'fill', pointerEvents: 'none', display: 'block' } })) : (_jsxs("div", { className: "people-edit-image-empty", children: [_jsx(PlusOutlined, {}), _jsx("span", { children: "\u70B9\u51FB\u4E0A\u4F20" })] })), _jsx("div", { className: "people-edit-image-mask", children: uploading ? '上传中…' : (url ? '点击更换' : '点击上传') })] })] }));
}
export function SignUpPeopleEditModal({ open, personId, onClose, onSaved, classWorkTypeId, classWorkTypeTitle, classWorkTypeLevel, }) {
    const [form] = Form.useForm();
    const [detail, setDetail] = useState(null);
    const [imageDrafts, setImageDrafts] = useState({});
    const [uploadingField, setUploadingField] = useState(null);
    const reqLevel = Form.useWatch('reqLevel', form);
    const { data: publicData } = useQuery({
        queryKey: ['training-public-data', 'company', 'worktype', 'condition'],
        queryFn: () => trainApi.getDemandPublicData(['company', 'worktype', 'condition']),
        enabled: open,
        staleTime: 5 * 60 * 1000,
    });
    const { data: personData, isLoading: detailLoading, isError: detailError, } = useQuery({
        queryKey: ['sign-up-person-detail', personId, classWorkTypeId, classWorkTypeLevel],
        queryFn: async () => {
            const raw = await trainApi.getSignUpPerson(personId);
            return normalizePersonDetail(raw);
        },
        enabled: open && !!personId,
        retry: false,
    });
    const saveMu = useMutation({
        mutationFn: (payload) => trainApi.updateSignUpPerson(personId, payload),
        onSuccess: () => {
            message.success('保存成功');
            onSaved();
        },
        onError: (err) => {
            const msg = err instanceof Error && err.message ? err.message : '保存失败';
            message.error(msg);
        },
    });
    useEffect(() => {
        if (!open) {
            form.resetFields();
            setDetail(null);
            setImageDrafts({});
            setUploadingField(null);
        }
    }, [open, form]);
    // 列表可能仍缓存已删除行：关掉弹窗并刷新列表
    useEffect(() => {
        if (!open || !detailError)
            return;
        message.error('人员不存在或已删除');
        onClose();
        onSaved();
    }, [open, detailError, onClose, onSaved]);
    const companyOptions = useMemo(() => (publicData?.company_list?.map((c) => ({
        value: c.id,
        label: c.title,
    })) ?? []), [publicData]);
    const workTypeOptions = useMemo(() => {
        const groups = publicData?.worktype_list ?? [];
        return groups.flatMap((g) => (g.options ?? []).map((o) => ({ value: o.id, label: o.title })));
    }, [publicData]);
    // destroyOnHidden 下 Form 晚于 queryFn 挂载；在 open+data 就绪后再回填，避免 setFieldsValue 丢失
    useEffect(() => {
        if (!open || !personData)
            return;
        setDetail(personData);
        setImageDrafts({
            idCardFront: pickImageUrl(personData.idcardFront, personData.idCardFront) || undefined,
            idCardBack: pickImageUrl(personData.idcardBack, personData.idCardBack) || undefined,
            idCardHeader: pickImageUrl(personData.idcardHeader, personData.idCardHeader) || undefined,
            idCardHeader2: pickImageUrl(personData.idCardHeader2) || undefined,
            schoolPaper: pickImageUrl(personData.schoolPaper) || undefined,
            contactFirstPage: pickImageUrl(personData.contractFirst, personData.contactFirstPage) || undefined,
            contactExpirePage: pickImageUrl(personData.contractExpire, personData.contactExpirePage) || undefined,
            contactSignPage: pickImageUrl(personData.contractSign, personData.contactSignPage) || undefined,
            workTypePaper: pickImageUrl(personData.workTypePaper) || undefined,
            titlePaper: pickImageUrl(personData.titlePaper) || undefined,
        });
        form.setFieldsValue(personToFormValues(personData, {
            workTypeId: classWorkTypeId,
            workTypeTitle: classWorkTypeTitle,
            workTypeLevel: classWorkTypeLevel,
        }, workTypeOptions));
    }, [
        open,
        personData,
        classWorkTypeId,
        classWorkTypeTitle,
        classWorkTypeLevel,
        workTypeOptions,
        form,
    ]);
    const allConditions = useMemo(() => (publicData?.condition_list ?? []), [publicData]);
    const conditionOptions = useMemo(() => {
        const filtered = filterDeclarationConditions(allConditions, reqLevel);
        return filtered.map((item) => ({ value: item.title, label: item.title }));
    }, [allConditions, reqLevel]);
    const handleReqLevelChange = (level) => {
        form.setFieldValue('reqLevel', level);
        form.setFieldValue('declarationCondition', undefined);
    };
    const handleImageUpload = async (field, file) => {
        setUploadingField(field);
        // 先本地预览，上传成功后再换成服务端地址
        const localPreview = URL.createObjectURL(file);
        setImageDrafts((prev) => {
            const old = prev[field];
            if (old?.startsWith('blob:'))
                URL.revokeObjectURL(old);
            return { ...prev, [field]: localPreview };
        });
        try {
            const result = await uploadFile(file, 'public');
            const url = resolveUploadedImageUrl(result);
            setImageDrafts((prev) => {
                const old = prev[field];
                if (old?.startsWith('blob:'))
                    URL.revokeObjectURL(old);
                return { ...prev, [field]: url };
            });
            message.success('图片已更新，请点击保存生效');
        }
        catch (err) {
            setImageDrafts((prev) => {
                const old = prev[field];
                if (old?.startsWith('blob:'))
                    URL.revokeObjectURL(old);
                const next = { ...prev };
                delete next[field];
                return next;
            });
            const msg = err instanceof Error && err.message ? err.message : '图片上传失败';
            message.error(msg);
        }
        finally {
            setUploadingField(null);
        }
    };
    const handleSave = async () => {
        if (!personId)
            return;
        try {
            const values = await form.validateFields();
            saveMu.mutate(formValuesToPayload(values, imageDrafts));
        }
        catch {
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
    return (_jsx(Modal, { className: "people-edit-modal", title: "\u62A5\u540D\u4EBA\u5458\u7F16\u8F91", open: open, width: "80%", onCancel: onClose, footer: null, destroyOnHidden: true, maskClosable: false, keyboard: false, children: _jsxs(Spin, { spinning: detailLoading || saveMu.isPending, children: [_jsx("div", { className: "people-edit-modal-body", children: _jsxs(Form, { form: form, layout: "horizontal", labelCol: { flex: '160px' }, wrapperCol: { flex: 1 }, colon: false, children: [_jsx("div", { className: "people-edit-section-title", children: "\u4E2A\u4EBA\u57FA\u672C\u4FE1\u606F" }), _jsxs("div", { className: "people-edit-row", children: [_jsxs("div", { className: "people-edit-fields", children: [_jsx(Form.Item, { name: "platformUserId", label: "\u5E73\u53F0ID", children: _jsx(InputNumber, { className: "people-input", controls: false, style: { width: 400 } }) }), _jsx(Form.Item, { name: "name", label: "\u59D3\u540D", rules: [{ required: true, message: '请输入姓名' }], children: _jsx(Input, { className: "people-input" }) }), _jsx(Form.Item, { name: "nation", label: "\u6C11\u65CF", children: _jsx(Input, { className: "people-input" }) }), _jsx(Form.Item, { name: "political", label: "\u653F\u6CBB\u9762\u8C8C", children: _jsx(Select, { className: "people-input", allowClear: true, placeholder: "\u8BF7\u9009\u62E9", options: POLITICAL_OPTIONS.map((v) => ({ value: v, label: v })) }) }), _jsx(Form.Item, { name: "houseType", label: "\u6237\u53E3\u6027\u8D28", children: _jsx(Select, { className: "people-input", allowClear: true, placeholder: "\u8BF7\u9009\u62E9", options: HOUSE_TYPE_OPTIONS.map((v) => ({ value: v, label: v })) }) }), _jsx(Form.Item, { name: "idCardNumber", label: "\u8EAB\u4EFD\u8BC1\u53F7", children: _jsx(Input, { className: "people-input" }) }), _jsx(Form.Item, { name: "address", label: "\u5730\u5740", children: _jsx(Input, { className: "people-input" }) }), _jsx(Form.Item, { name: "phone", label: "\u624B\u673A\u53F7", rules: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入11位有效手机号' }], children: _jsx(Input, { className: "people-input", maxLength: 11, inputMode: "numeric" }) })] }), _jsxs("div", { className: "people-edit-images", children: [_jsx(ImagePreviewBox, { label: "\u8EAB\u4EFD\u8BC1\u6B63\u9762", url: idFront, boxClass: "id-card", editable: true, uploading: uploadingField === 'idCardFront', onUpload: (file) => handleImageUpload('idCardFront', file) }), _jsx(ImagePreviewBox, { label: "\u8EAB\u4EFD\u8BC1\u53CD\u9762", url: idBack, boxClass: "id-card", editable: true, uploading: uploadingField === 'idCardBack', onUpload: (file) => handleImageUpload('idCardBack', file) }), _jsx(ImagePreviewBox, { label: "\u4E00\u5BF8\u8BC1\u4EF6\u7167", url: photoHeader, boxClass: "photo", editable: true, uploading: uploadingField === 'idCardHeader', onUpload: (file) => handleImageUpload('idCardHeader', file) }), _jsx(ImagePreviewBox, { label: "\u539F\u8BC1\u4EF6\u7167", url: photoOriginal, boxClass: "photo", editable: true, uploading: uploadingField === 'idCardHeader2', onUpload: (file) => handleImageUpload('idCardHeader2', file) })] })] }), _jsx("div", { className: "people-edit-section-title", children: "\u6559\u80B2\u4FE1\u606F" }), _jsxs("div", { className: "people-edit-row", children: [_jsxs("div", { className: "people-edit-fields", children: [_jsx(Form.Item, { name: "school", label: "\u5B66\u6821\u540D\u79F0", children: _jsx(Input, { className: "people-input" }) }), _jsx(Form.Item, { name: "inSchoolDate", label: "\u5165\u5B66\u65E5\u671F", children: _jsx(DatePicker, { className: "people-input", style: { width: 400 }, format: DATE_FORMAT }) }), _jsx(Form.Item, { name: "graduateDate", label: "\u6BD5\u4E1A\u65E5\u671F", children: _jsx(DatePicker, { className: "people-input", style: { width: 400 }, format: DATE_FORMAT }) }), _jsx(Form.Item, { name: "education", label: "\u5B66\u5386", children: _jsx(Select, { className: "people-input", allowClear: true, placeholder: "\u8BF7\u9009\u62E9", options: EDUCATION_OPTIONS.map((v) => ({ value: v, label: v })) }) }), _jsx(Form.Item, { name: "major", label: "\u4E13\u4E1A", children: _jsx(Input, { className: "people-input" }) }), _jsx(Form.Item, { name: "schoolPaperHas", label: "\u662F\u5426\u6709\u6BD5\u4E1A\u8BC1", valuePropName: "checked", children: _jsx(Switch, {}) }), _jsx(Form.Item, { name: "schoolNo", label: "\u6BD5\u4E1A\u8BC1\u7F16\u53F7", children: _jsx(Input, { className: "people-input" }) })] }), _jsx("div", { className: "people-edit-images", children: _jsx(ImagePreviewBox, { label: "\u6BD5\u4E1A\u8BC1", url: schoolPaper, boxClass: "id-card", editable: true, uploading: uploadingField === 'schoolPaper', onUpload: (file) => handleImageUpload('schoolPaper', file) }) })] }), _jsx("div", { className: "people-edit-section-title", children: "\u5DE5\u4F5C\u4FE1\u606F" }), _jsxs("div", { className: "people-edit-row", children: [_jsxs("div", { className: "people-edit-fields", children: [_jsx(Form.Item, { name: "workTime", label: "\u4ECE\u4E1A\u65F6\u95F4", children: _jsx(DatePicker, { className: "people-input", style: { width: 400 }, format: DATE_FORMAT }) }), _jsx(Form.Item, { name: "titleType", label: "\u804C\u79F0", children: _jsx(Select, { className: "people-input", allowClear: true, placeholder: "\u8BF7\u9009\u62E9", options: TITLE_TYPE_OPTIONS.map((v) => ({ value: v, label: v })) }) }), _jsx(Form.Item, { name: "companyId", label: "\u516C\u53F8\u540D\u79F0", children: _jsx(Select, { className: "people-input", showSearch: true, allowClear: true, placeholder: "\u8BF7\u9009\u62E9", optionFilterProp: "label", options: companyOptions }) }), _jsx(Form.Item, { name: "workDateStart", label: "\u672C\u516C\u53F8\u5165\u804C\u65E5\u671F", children: _jsx(DatePicker, { className: "people-input", style: { width: 400 }, format: DATE_FORMAT }) }), _jsx(Form.Item, { name: "workDateEnd", label: "\u672C\u516C\u53F8\u7ED3\u675F\u65E5\u671F", children: _jsx(DatePicker, { className: "people-input", style: { width: 400 }, format: DATE_FORMAT }) }), _jsx(Form.Item, { name: "workTypePaperHas", label: "\u662F\u5426\u6709\u4E0A\u4E00\u7B49\u7EA7\u8BC1\u4E66", valuePropName: "checked", children: _jsx(Switch, {}) }), _jsx(Form.Item, { name: "workTypeId", label: "\u73B0\u5DE5\u79CD", children: _jsx(Select, { className: "people-input", showSearch: true, allowClear: true, placeholder: "\u8BF7\u9009\u62E9", optionFilterProp: "label", options: workTypeOptions }) }), _jsx(Form.Item, { name: "level", label: "\u73B0\u5DE5\u79CD\u7B49\u7EA7", children: _jsx(Select, { className: "people-input", allowClear: true, placeholder: "\u8BF7\u9009\u62E9", options: WORK_LEVEL_OPTIONS }) }), _jsx(Form.Item, { name: "paperNo", label: "\u73B0\u5DE5\u79CD\u8BC1\u4E66\u7F16\u53F7", children: _jsx(Input, { className: "people-input" }) }), _jsx(Form.Item, { name: "reqWorkTypeId", label: "\u9884\u7EA6\u5DE5\u79CD", children: _jsx(Select, { className: "people-input", showSearch: true, allowClear: true, placeholder: "\u8BF7\u9009\u62E9", optionFilterProp: "label", options: workTypeOptions }) }), _jsx(Form.Item, { name: "reqLevel", label: "\u9884\u7EA6\u5DE5\u79CD\u7B49\u7EA7", children: _jsx(Select, { className: "people-input", allowClear: true, placeholder: "\u8BF7\u9009\u62E9", options: WORK_LEVEL_OPTIONS, onChange: handleReqLevelChange }) }), _jsx(Form.Item, { name: "declarationCondition", label: "\u7533\u62A5\u6761\u4EF6", children: allConditions.length > 0 ? (_jsx(AutoComplete, { className: "people-input", allowClear: true, options: conditionOptions, filterOption: (input, option) => String(option?.value ?? '').toLowerCase().includes(input.trim().toLowerCase()), placeholder: reqLevel == null ? '请选择或输入申报条件（建议先选预约等级）' : '请选择或输入申报条件' })) : (_jsx(Input, { className: "people-input", placeholder: "\u8BF7\u8F93\u5165\u7533\u62A5\u6761\u4EF6", maxLength: 500, showCount: true })) }), workExpList.length > 0 ? (_jsx(Form.Item, { label: "\u5DE5\u4F5C\u7ECF\u5386", children: _jsx("div", { children: workExpList.map((exp, index) => (_jsxs("div", { className: "people-edit-work-exp-item", children: [_jsxs("div", { children: [String(exp.startDate ?? ''), "\u81F3", String(exp.endDate ?? '')] }), _jsxs("div", { children: ["\u5728", String(exp.company ?? ''), "\u516C\u53F8\u4ECE\u4E8B", String(exp.position ?? ''), "\u5DE5\u4F5C"] }), _jsxs("div", { children: ["\u8BC1\u660E\u4EBA:", String(exp.proveName ?? ''), ",\u8054\u7CFB\u7535\u8BDD:", String(exp.provePhone ?? '')] })] }, `work-exp-${index}`))) }) })) : null] }), _jsxs("div", { children: [_jsxs("div", { className: "people-edit-contract-row", children: [_jsx(ImagePreviewBox, { label: "\u5408\u540C\u7B2C\u4E00\u9875", url: contractFirst, boxClass: "contract", editable: true, uploading: uploadingField === 'contactFirstPage', onUpload: (file) => handleImageUpload('contactFirstPage', file) }), _jsx(ImagePreviewBox, { label: "\u5408\u540C\u5230\u671F\u9875", url: contractExpire, boxClass: "contract", editable: true, uploading: uploadingField === 'contactExpirePage', onUpload: (file) => handleImageUpload('contactExpirePage', file) }), _jsx(ImagePreviewBox, { label: "\u5408\u540C\u76D6\u7AE0\u9875", url: contractSign, boxClass: "contract", editable: true, uploading: uploadingField === 'contactSignPage', onUpload: (file) => handleImageUpload('contactSignPage', file) })] }), _jsxs("div", { className: "people-edit-cert-row", children: [_jsx(ImagePreviewBox, { label: "\u4E0A\u4E00\u7B49\u7EA7\u8BC1\u4E66", url: workTypePaper, boxClass: "id-card", editable: true, uploading: uploadingField === 'workTypePaper', onUpload: (file) => handleImageUpload('workTypePaper', file) }), _jsx(ImagePreviewBox, { label: "\u804C\u79F0\u8BC1\u4E66", url: titlePaper, boxClass: "id-card", editable: true, uploading: uploadingField === 'titlePaper', onUpload: (file) => handleImageUpload('titlePaper', file) })] })] })] }), _jsx("div", { className: "people-edit-section-title", children: "\u72B6\u6001\u4FE1\u606F" }), _jsxs("div", { className: "people-edit-status-row", children: [_jsx(Form.Item, { name: "status", label: "\u5BA1\u6838\u72B6\u6001", children: _jsx(Select, { style: { width: 120 }, options: STATUS_OPTIONS }) }), _jsx(Form.Item, { name: "companyStatus", label: "\u4F01\u4E1A\u5BA1\u6838\u72B6\u6001", children: _jsx(Select, { style: { width: 120 }, options: STATUS_OPTIONS }) }), _jsx(Form.Item, { name: "hasPay", label: "\u652F\u4ED8\u72B6\u6001", children: _jsx(Select, { style: { width: 120 }, options: PAY_OPTIONS }) })] })] }) }), _jsxs("div", { className: "people-edit-footer", children: [_jsx(Button, { onClick: onClose, style: { marginRight: 8 }, children: "\u53D6\u6D88" }), _jsx(Button, { type: "primary", loading: saveMu.isPending, onClick: handleSave, children: "\u4FDD\u5B58" })] })] }) }));
}
