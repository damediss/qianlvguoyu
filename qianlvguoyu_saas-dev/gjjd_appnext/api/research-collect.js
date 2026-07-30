/**
 * 企业调研人员采集 - 公开 API（千铝国昱 SaaS）
 * 后端: /training/public/research-collection/**
 * 使用 saasRequest，不走 PHP request
 */
import { saasRequest } from '@/utils/saasRequest.js';

export const getResearchCollectionDetail = (token) => {
	return saasRequest({
		method: 'GET',
		url: `/training/public/research-collection/${encodeURIComponent(token)}`
	});
};

export const findMyResearchCollection = (token, idCardNumber) => {
	return saasRequest({
		method: 'GET',
		url: `/training/public/research-collection/${encodeURIComponent(token)}/mine`,
		params: { idCardNumber }
	});
};

export const submitResearchCollection = (token, formData) => {
	return saasRequest({
		method: 'POST',
		url: `/training/public/research-collection/${encodeURIComponent(token)}/submit`,
		data: formData
	});
};
