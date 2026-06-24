export interface CertificationInfoQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
  auditStatus?: string;
  status?: string;
}

export interface AssessorInfo {
  id: string;
  userId?: string;
  accountUsername?: string;
  accountPassword?: string;
  assessorName: string;
  certificateNo: string;
  /** @deprecated 旧字段，多个工种以"、"拼接。优先使用 workTypeScopes。 */
  workTypeScope: string;
  workTypeScopes: string[];
  levelScope?: string;
  validUntil: string;
  maskedPhone?: string;
  auditStatus: string;
  workStatus: string;
  materialsDescription?: string;
  remark?: string;
}

export interface AssessorInfoPayload {
  assessorName: string;
  accountUsername?: string;
  accountPassword?: string;
  certificateNo: string;
  /** @deprecated 旧字段。优先使用 workTypeScopes。 */
  workTypeScope?: string;
  workTypeScopes: string[];
  levelScope?: string;
  validUntil: string;
  phone?: string;
  materialsDescription?: string;
  remark?: string;
}

export interface AssessorAccountPayload {
  username: string;
  password: string;
}

export interface ExamStaffInfo {
  id: string;
  userId?: string;
  accountUsername?: string;
  staffName: string;
  phone?: string;
  maskedPhone?: string;
  auditStatus: string;
  workStatus: string;
  /** 筹备期考务配置绑定的认定任务编号，多个以顿号分隔 */
  assignedTaskNos?: string;
}

export interface ExamStaffInfoPayload {
  staffName: string;
  phone: string;
  accountUsername: string;
  accountPassword: string;
}

export interface ExamSiteInfo {
  id: string;
  siteName: string;
  siteType: string;
  address: string;
  seatCount: number;
  filingNo?: string;
  contactName?: string;
  maskedContactPhone?: string;
  equipmentCondition?: string;
  materialsDescription?: string;
  auditStatus: string;
  status: string;
  remark?: string;
}

export interface ExamSiteInfoPayload {
  siteName: string;
  siteType: string;
  address: string;
  seatCount: number;
  filingNo?: string;
  contactName?: string;
  contactPhone?: string;
  equipmentCondition?: string;
  materialsDescription?: string;
  status?: string;
  remark?: string;
}

export interface ExamEquipmentInfo {
  id: string;
  equipmentType: string;
  assetNo: string;
  equipmentQuantity: number;
  siteId?: string;
  siteName?: string;
  equipmentModel?: string;
  calibrationValidUntil?: string;
  auditStatus: string;
  equipmentStatus?: string;
  equipmentPhotoFileId?: string;
  equipmentPhotoFileName?: string;
  materialsDescription?: string;
  remark?: string;
}

export interface ExamEquipmentInfoPayload {
  equipmentType: string;
  assetNo: string;
  equipmentQuantity: number;
  siteId?: string;
  siteName?: string;
  equipmentModel?: string;
  calibrationValidUntil?: string;
  equipmentStatus?: string;
  equipmentPhotoFileId?: string;
  equipmentPhotoFileName?: string;
  materialsDescription?: string;
  remark?: string;
}

export interface InfoReviewPayload {
  reviewAction: 'pass' | 'reject';
  comment?: string;
}

export interface InfoDisablePayload {
  reason?: string;
}
