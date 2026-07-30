/**
 * 千铝国昱 SaaS 响应工具（code === '0'）
 * 与 utils/response.js（PHP code===1）隔离，仅供采集等 SaaS 接口使用
 */

export function isSaasSuccess(res) {
	if (!res) return false;
	const c = res.code;
	return c === '0' || c === 0 || c === '200';
}

export function getSaasMsg(res) {
	if (!res) return '';
	return res.message || res.msg || '';
}

export function getSaasData(res, defaultValue = null) {
	return isSaasSuccess(res) ? res.data : defaultValue;
}

export function showSaasError(res, defaultMsg = '操作失败') {
	uni.showToast({
		title: getSaasMsg(res) || defaultMsg,
		icon: 'none',
		duration: 2000
	});
}

export function showSaasSuccess(res, defaultMsg = '操作成功') {
	uni.showToast({
		title: getSaasMsg(res) || defaultMsg,
		icon: 'success',
		duration: 1500
	});
}
