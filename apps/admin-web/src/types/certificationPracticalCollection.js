export const QrIssueStatusText = {
    NOT_ISSUED: '未发放',
    ISSUED: '已发放',
};
export const CollectionOverallStatusText = {
    NOT_ISSUED: '未发放',
    ISSUED: '已发放',
    NOT_ARRIVED: '未到场',
    ARRIVED: '已到场',
    QUEUED: '已入队',
    COLLECTING: '采集中',
    PENDING_RETRY: '待补采',
    INFO_ERROR: '异常',
    EXCEPTION_RELEASED: '已放行',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
};
export const CollectionQueueStatusText = {
    NOT_QUEUED: '未入队',
    QUEUED: '排队中',
    COLLECTING: '采集中',
    COMPLETED: '已完成',
    REMOVED: '已移出',
    CANCELLED: '已取消',
};
export const FingerprintStatusText = {
    NOT_STARTED: '未开始',
    CAPTURING: '采集中',
    DONE: '已采集',
    FAILED: '异常',
    EXCEPTION_RELEASED: '已放行',
};
export const SignatureStatusText = {
    NOT_STARTED: '未开始',
    SIGNING: '签名中',
    DONE: '已签名',
    FAILED: '异常',
    EXCEPTION_RELEASED: '已放行',
};
export const CollectionExceptionStatusText = {
    NONE: '无异常',
    PENDING_RETRY: '待补采',
    INFO_ERROR: '异常',
    RELEASED: '已放行',
    RESOLVED: '已处理',
};
export const CollectionReleaseScopeText = {
    FINGERPRINT: '指纹',
    SIGNATURE: '签名',
    ALL: '全部',
    INFO: '信息',
    OTHER: '其他',
};
export const CollectionMatchMethodText = {
    QR_CODE: '二维码',
    ID_CARD_LAST4: '身份证后四位',
    NAME: '姓名',
    ADMISSION_NO: '准考证号',
    MANUAL: '人工处理',
};
export const FingerprintCollectStatusText = {
    SUCCESS: '成功',
    FAILED: '失败',
    EXCEPTION: '异常',
};
export const SignerTypeText = {
    CANDIDATE: '考生',
    EVALUATOR: '考评员',
    LEADER: '组长',
};
export const CollectionExceptionTypeText = {
    DEVICE_ERROR: '设备异常',
    FINGERPRINT_UNAVAILABLE: '无法采集指纹',
    SIGNATURE_UNAVAILABLE: '无法签名',
    INFO_ERROR: '信息异常',
    OTHER: '其他',
};
