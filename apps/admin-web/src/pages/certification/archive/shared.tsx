import { Tag } from 'antd';

export const taskMaterialOptions = [
  { label: '职业技能等级认定评价结果确认表', value: 'RESULT_CONFIRM_FORM' },
  { label: '广西壮族自治区职业技能鉴定业务申请表', value: 'BUSINESS_APPLICATION' },
  { label: '四类人员安排表', value: 'STAFF_ARRANGEMENT' },
  { label: '职业技能等级认定成绩公示文件', value: 'SCORE_PUBLICITY' },
  { label: '广西技能人才评价质量督导表', value: 'QUALITY_SUPERVISION' },
  { label: '职业技能等级认定成绩花名册', value: 'SCORE_ROSTER' },
  { label: '职业技能等级获证人员花名册', value: 'CERTIFIED_ROSTER' },
  { label: '职业技能等级认定理论考务材料', value: 'THEORY_EXAM_MATERIAL' },
  { label: '职业技能等级认定技能考务材料', value: 'SKILL_EXAM_MATERIAL' },
];

export function archiveStatusTag(value?: string) {
  const map: Record<string, { text: string; color: string }> = {
    NOT_ARCHIVED: { text: '未归档', color: 'default' },
    BASIC_ARCHIVED: { text: '基础信息已归档', color: 'processing' },
    MATERIAL_ARCHIVED: { text: '资料已归档', color: 'warning' },
    ARCHIVED: { text: '认定归档已完成', color: 'success' },
  };
  const item = value ? map[value] : undefined;
  return <Tag color={item?.color || 'default'}>{item?.text || value || '-'}</Tag>;
}

export function batchStatusTag(value?: string) {
  const map: Record<string, { text: string; color: string }> = {
    UPLOADED: { text: '已上传', color: 'default' },
    PARSING: { text: '解析中', color: 'processing' },
    PARSE_SUCCESS: { text: '解析成功', color: 'success' },
    PARSE_FAILED: { text: '解析失败', color: 'error' },
    CONFIRMED: { text: '已确认入库', color: 'success' },
    CANCELLED: { text: '已取消', color: 'default' },
  };
  const item = value ? map[value] : undefined;
  return <Tag color={item?.color || 'default'}>{item?.text || value || '-'}</Tag>;
}

export function dateTimeText(value?: string) {
  return value ? value.replace('T', ' ') : '-';
}

export function fileSizeText(value?: number) {
  if (!value) {
    return '-';
  }
  if (value >= 1024 * 1024) {
    return `${(value / 1024 / 1024).toFixed(2)} MB`;
  }
  if (value >= 1024) {
    return `${(value / 1024).toFixed(1)} KB`;
  }
  return `${value} B`;
}

type SnapshotRecord = Record<string, unknown>;

function parseSnapshotJson(raw?: string): SnapshotRecord[] {
  if (!raw) {
    return [];
  }
  try {
    const parsed = JSON.parse(raw) as unknown;
    return Array.isArray(parsed) ? parsed as SnapshotRecord[] : [];
  } catch {
    return [];
  }
}

function uniqueTexts(values: string[]) {
  return [...new Set(values.filter(Boolean))];
}

export function formatScopeSnapshot(raw?: string) {
  const levels = uniqueTexts(parseSnapshotJson(raw).map((item) => String(item.levelName || item.levelCode || '').trim()));
  return levels.length ? levels.join('、') : '-';
}

export function formatExamStaffSnapshot(raw?: string) {
  const lines = parseSnapshotJson(raw)
    .map((item) => {
      const name = String(item.staffName || '').trim();
      const role = String(item.roleName || item.taskRole || '').trim();
      if (!name) {
        return '';
      }
      return role ? `${name}（${role}）` : name;
    })
    .filter(Boolean);
  return lines.length ? lines.join('\n') : '-';
}

export function formatDeviceSnapshot(raw?: string) {
  const names = uniqueTexts(parseSnapshotJson(raw).map((item) => String(item.equipmentName || '').trim()));
  return names.length ? names.join('、') : '-';
}

export function formatRoomSnapshot(raw?: string) {
  const names = uniqueTexts(parseSnapshotJson(raw).map((item) => String(item.siteName || '').trim()));
  return names.length ? names.join('、') : '-';
}

export function formatEvaluatorSnapshot(raw?: string) {
  const names = uniqueTexts(parseSnapshotJson(raw).map((item) => String(item.assessorName || '').trim()));
  return names.length ? names.join('、') : '-';
}
