/**
 * 千铝国昱 SaaS 专用请求（企业调研公开采集等）
 * 与 utils/index.js（PHP）隔离，避免影响现有报名/考试链路
 */
import util from '@/utils/util.js';

const g = util;

export const saasRequest = (options = {}) => {
	const base = (g.saas && g.saas[g.runMode]) || '';
	let url = base + (options.url || '');

	if (options.params && Object.keys(options.params).length > 0) {
		for (const key in options.params) {
			if (options.params[key] !== undefined && options.params[key] !== null) {
				url += (url.indexOf('?') >= 0 ? '&' : '?') + key + '=' + encodeURIComponent(options.params[key]);
			}
		}
	}

	let body = options.data;
	if (options.method === 'POST' || options.method === 'PUT' || options.method === 'DELETE') {
		body = JSON.stringify(body || {});
	}

	console.log('[saasRequest]', options.method || 'GET', url);

	return new Promise((resolve) => {
		uni.request({
			header: { 'Content-Type': 'application/json' },
			method: options.method || 'GET',
			data: body,
			url,
			timeout: options.timeout || 15000,
			success: (res) => {
				if (res.statusCode && (res.statusCode < 200 || res.statusCode >= 300)) {
					console.log('[saasRequest] http error', res.statusCode, res.data);
					resolve(res.data || { code: String(res.statusCode), message: '服务异常(' + res.statusCode + ')' });
					return;
				}
				resolve(res.data);
			},
			fail: (err) => {
				console.log('[saasRequest] fail', err);
				uni.showToast({
					title: '网络请求失败，请检查网络',
					icon: 'none',
					duration: 2000
				});
				resolve(false);
			}
		});
	});
};
