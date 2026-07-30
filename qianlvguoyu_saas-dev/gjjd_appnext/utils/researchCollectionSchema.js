export const SCHEMA_VERSION = 2;

export const DEFAULT_EDUCATION_OPTIONS = ['初中', '高中', '中专', '大专', '本科', '研究生'];
export const DEFAULT_LEVEL_OPTIONS = ['初级工', '中级工', '高级工', '技师', '高级技师'];

export const DEFAULT_COLLECTION_GROUPS = [
	{ key: 'basic', label: '基本信息', order: 1 },
	{ key: 'certificate', label: '技能等级证书', order: 2 },
	{ key: 'title', label: '职称信息', order: 3 }
];

export const ALL_COLLECTION_FIELDS = [
	{ key: 'name', label: '姓名', inputType: 'text', group: 'basic', order: 1, locked: true, defaultRequired: true },
	{ key: 'idCardNumber', label: '身份证号码', inputType: 'text', group: 'basic', order: 2, locked: true, defaultRequired: true },
	{ key: 'phone', label: '手机号', inputType: 'text', group: 'basic', order: 3 },
	{ key: 'companyName', label: '所在单位', inputType: 'text', group: 'basic', order: 4, readonly: true },
	{ key: 'major', label: '所学专业（初中、高中学历的填无）', inputType: 'select', group: 'basic', order: 5, defaultAllowCustom: true },
	{ key: 'education', label: '学历层次', inputType: 'select', group: 'basic', order: 6, defaultOptions: DEFAULT_EDUCATION_OPTIONS },
	{ key: 'workTime', label: '参加工作时间', inputType: 'date', group: 'basic', order: 7 },
	{ key: 'workTypeName', label: '原工种', inputType: 'select', group: 'basic', order: 8 },
	{ key: 'department', label: '部门', inputType: 'select', group: 'basic', order: 9 },
	{ key: 'job', label: '实际岗位名称', inputType: 'select', group: 'basic', order: 10 },
	{ key: 'workDateStart', label: '入职时间', inputType: 'date', group: 'basic', order: 11 },
	{ key: 'certLevel', label: '已有证书等级', inputType: 'input', group: 'certificate', order: 12 },
	{ key: 'certNo', label: '证书编号', inputType: 'text', group: 'certificate', order: 13 },
	{ key: 'certIssueDate', label: '发证日期', inputType: 'date', group: 'certificate', order: 14 },
	{ key: 'titleType', label: '职称', inputType: 'select', group: 'title', order: 15, defaultAllowCustom: true },
	{ key: 'titleMajor', label: '职称专业', inputType: 'text', group: 'title', order: 16 },
	{ key: 'titleCertDate', label: '职称获证时间', inputType: 'date', group: 'title', order: 17 }
];

function defaultOptionsForField(def) {
	if (def.defaultOptions && def.defaultOptions.length) return [...def.defaultOptions];
	if (def.key === 'education') return [...DEFAULT_EDUCATION_OPTIONS];
	if (def.key === 'certLevel') return [...DEFAULT_LEVEL_OPTIONS];
	return [];
}

export function mergeFieldConfig(def, incoming) {
	let inputType = (incoming && incoming.inputType) || def.inputType;
	// 已有证书等级固定为自由文本输入
	if (def.key === 'certLevel') {
		inputType = 'input';
	}
	// 部门/岗位在 schema 中固定为 select，忽略历史误存的 text，与岗位字段行为一致
	if ((def.key === 'department' || def.key === 'job') && def.inputType === 'select') {
		inputType = 'select';
	}
	// 已保存 options 时强制 select（防止 inputType=text 导致 options 被丢弃）
	const savedOptions = (incoming && incoming.options && incoming.options.length) ? [...incoming.options] : [];
	if (savedOptions.length > 0 && def.inputType === 'select') {
		inputType = 'select';
	}
	const required = (def.locked && (def.key === 'name' || def.key === 'idCardNumber'))
		|| (incoming && incoming.required != null ? incoming.required : (def.defaultRequired || false));
	// 入职时间固定显示（避免历史快照 visible=false 导致 H5 不渲染）
	const visible = (def.locked || def.key === 'workDateStart') ? true : !(incoming && incoming.visible === false);
	let allowCustom = incoming && incoming.allowCustom != null
		? incoming.allowCustom
		: (def.defaultAllowCustom || false);
	const options = inputType === 'select'
		? (savedOptions.length ? savedOptions : defaultOptionsForField(def))
		: [];
	if ((def.key === 'department' || def.key === 'job') && inputType === 'select' && options.length > 0) {
		allowCustom = false;
	}
	return {
		inputType,
		label: (incoming && incoming.label && incoming.label.trim()) || def.label,
		required,
		visible,
		allowCustom,
		order: def.key === 'workDateStart' ? def.order : ((incoming && incoming.order > 0) ? incoming.order : def.order),
		group: def.key === 'workDateStart' ? def.group : ((incoming && incoming.group) || def.group),
		options
	};
}
export const CUSTOM_SELECT_OPTION = '其他（手动输入）';

export function getSelectPickerOptions(config, currentValue) {
	if (!config || config.inputType !== 'select') return [];
	const base = Array.isArray(config.options) ? config.options.filter((item) => String(item || '').trim()) : [];
	if (!base.length) return [];
	const value = String(currentValue || '').trim();
	if (value && !base.includes(value)) {
		base.unshift(value);
	}
	if (config.allowCustom && !base.includes(CUSTOM_SELECT_OPTION)) {
		return [...base, CUSTOM_SELECT_OPTION];
	}
	return base;
}

/** 是否应渲染为下拉选择（有选项即展示，allowCustom 时追加「其他」） */
export function shouldUseSelectPicker(config, currentValue) {
	return getSelectPickerOptions(config, currentValue).length > 0;
}

export function isCustomSelectOption(value) {
	return value === CUSTOM_SELECT_OPTION;
}

export function normalizeCollectionFieldOptions(raw) {
	const incomingFields = (raw && raw.fields) || {};
	const fields = {};
	ALL_COLLECTION_FIELDS.forEach((def) => {
		fields[def.key] = mergeFieldConfig(def, incomingFields[def.key]);
	});
	const incomingGroups = new Map(((raw && raw.groups) || []).map((g) => [g.key, g]));
	const groups = DEFAULT_COLLECTION_GROUPS.map((def) => {
		const saved = incomingGroups.get(def.key);
		return {
			key: def.key,
			label: (saved && saved.label && saved.label.trim()) || def.label,
			order: (saved && saved.order > 0) ? saved.order : def.order
		};
	}).sort((a, b) => a.order - b.order);
	return {
		version: (raw && raw.version > 0) ? raw.version : SCHEMA_VERSION,
		groups,
		fields
	};
}

export function getOrderedGroups(fieldOptions) {
	return normalizeCollectionFieldOptions(fieldOptions).groups;
}

export function isCertificateFieldKey(key) {
	return key === 'certLevel' || key === 'certNo' || key === 'certIssueDate';
}

export function getOrderedVisibleFields(fieldOptions, groupKey) {
	const normalized = normalizeCollectionFieldOptions(fieldOptions);
	const groupOrder = new Map(normalized.groups.map((g) => [g.key, g.order]));
	const result = [];
	ALL_COLLECTION_FIELDS.forEach((def) => {
		const config = normalized.fields[def.key] || mergeFieldConfig(def);
		if (!def.locked && config.visible === false) return;
		// 入职时间固定归到基本信息组（schema 已声明），忽略历史快照里的 group
		const effectiveGroup = def.key === 'workDateStart' ? def.group : (config.group || def.group);
		if (groupKey && effectiveGroup !== groupKey) return;
		result.push({ key: def.key, def, config: { ...config, group: effectiveGroup } });
	});
	result.sort((a, b) => {
		const ga = groupOrder.get(a.config.group || a.def.group) || 99;
		const gb = groupOrder.get(b.config.group || b.def.group) || 99;
		if (ga !== gb) return ga - gb;
		const orderA = a.def.key === 'workDateStart' ? a.def.order : (a.config.order || a.def.order);
		const orderB = b.def.key === 'workDateStart' ? b.def.order : (b.config.order || b.def.order);
		return orderA - orderB;
	});
	return result;
}

export function validatePersonFormValues(values, fieldOptions) {
	const fields = getOrderedVisibleFields(fieldOptions);
	for (let i = 0; i < fields.length; i++) {
		const { key, config } = fields[i];
		if (!config.required) continue;
		if (isCertificateFieldKey(key)) {
			const first = (values.certificates && values.certificates[0]) || {};
			const certKey = key === 'certLevel' ? 'level' : key === 'certNo' ? 'certNo' : 'issueDate';
			if (!String(first[certKey] || '').trim()) return (config.label || '') + '不能为空';
			continue;
		}
		if (!String(values[key] || '').trim()) return (config.label || '') + '不能为空';
	}
	if (!String(values.name || '').trim()) return '姓名不能为空';
	if (!String(values.idCardNumber || '').trim()) return '身份证号码不能为空';
	return '';
}

export function buildEmptyForm(companyName) {
	return {
		name: '',
		idCardNumber: '',
		phone: '',
		companyName: companyName || '',
		major: '',
		education: '',
		workTime: '',
		workTypeName: '',
		department: '',
		job: '',
		workDateStart: '',
		titleType: '',
		titleMajor: '',
		titleCertDate: '',
		certificates: [{ level: '', certNo: '', issueDate: '' }]
	};
}
