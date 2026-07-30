import { getDeviceId, getDeviceInfo } from '@/utils/device.js';

/** 内存缓存密码：仅本次 App 生命周期有效，不落盘 */
let cachedPassword = '';

/** 静默刷新并发锁，避免多请求同时重登 */
let refreshPromise = null;

/**
 * token 合法性校验（非空字符串且长度足够）
 */
export function isValidToken(token) {
	return typeof token === 'string' && token.length >= 10;
}

/**
 * 写入内存密码（登录成功后调用）
 */
export function setCachedPassword(password) {
	cachedPassword = password || '';
}

export function getCachedPassword() {
	return cachedPassword;
}

/**
 * 判断接口响应是否为登录态失效（自然过期 / 未登录）
 */
export function isLoginExpired(data) {
	if (!data || typeof data !== 'object') return false;
	const code = data.code;
	const msg = String(data.msg || '');
	if (code === 401 || code === -1 || code === 10001) return true;
	return /登录.*过期|登录.*失效|请重新登录|未登录|token.*无效|登录超时|请先登录|登录状态/.test(msg);
}

/**
 * 判断是否被踢下线（与自然过期区分）
 */
export function isKickedOut(msg) {
	return /其他设备|异地登录|被踢|强制下线|已在别处登录|已在其他/.test(String(msg || ''));
}

/**
 * 替换或追加 URL 中的 istate
 */
export function updateTokenInUrl(url, token) {
	if (!url || !token) return url;
	if (/([?&])istate=[^&]*/.test(url)) {
		return url.replace(/([?&])istate=[^&]*/, `$1istate=${token}`);
	}
	const sep = url.includes('?') ? '&' : '?';
	return `${url}${sep}istate=${token}`;
}

/**
 * 清除本地登录凭证（storage + 内存密码）
 */
export function clearLocalCredentials() {
	cachedPassword = '';
	uni.removeStorageSync('itoken');
	uni.removeStorageSync('userId');
	uni.removeStorageSync('userInfo');
	uni.removeStorageSync('currentMatchId');
}

/**
 * 用缓存账号密码静默登录，成功则写入 itoken
 * @param {Function} request utils 中的 request
 * @returns {Promise<string|null>}
 */
async function silentLogin(request) {
	const phone = uni.getStorageSync('iusername');
	if (!phone || !cachedPassword) {
		return null;
	}
	const res = await request({
		method: 'POST',
		data: {
			phone,
			password: cachedPassword,
			smscode: '',
			loginType: 'pwd',
			deviceId: getDeviceId(),
			deviceInfo: getDeviceInfo()
		},
		url: '/appauth/login',
		skipAuth: true
	});
	if (!res || res.code) {
		console.warn('silentLogin failed', res && res.msg);
		return null;
	}
	const token = res.data && res.data.token;
	if (!isValidToken(token)) {
		return null;
	}
	uni.setStorageSync('itoken', token);
	if (res.data.user && res.data.user.id) {
		uni.setStorageSync('userId', res.data.user.id);
		uni.setStorageSync('userInfo', JSON.stringify(res.data.user));
	}
	return token;
}

function getOrCreateRefresh(request) {
	if (!refreshPromise) {
		refreshPromise = silentLogin(request).finally(() => {
			refreshPromise = null;
		});
	}
	return refreshPromise;
}

/**
 * 确保有可用 token：有则直接返回，无则尝试静默登录，失败则清缓存
 * @param {Function} request
 * @returns {Promise<string|null>}
 */
export async function ensureToken(request) {
	const existing = uni.getStorageSync('itoken');
	if (isValidToken(existing)) {
		return existing;
	}
	const token = await getOrCreateRefresh(request);
	if (!token) {
		clearLocalCredentials();
		return null;
	}
	return token;
}

/**
 * token 自然过期后：静默重登并重试原请求
 * @param {Object} options 原始 request options
 * @param {string} url 原始完整 URL
 * @param {Function} request
 * @returns {Promise<*>}
 */
export async function handleTokenExpired(options, url, request) {
	const token = await getOrCreateRefresh(request);
	if (!token) {
		clearLocalCredentials();
		uni.showToast({
			title: '登录已过期，请重新登录',
			icon: 'none',
			duration: 1500
		});
		setTimeout(() => {
			uni.navigateTo({ url: '/pages/auth/login' });
		}, 1500);
		return false;
	}
	const retryUrl = updateTokenInUrl(url, token);
	return request({
		...options,
		_url: retryUrl
	});
}

/**
 * 用当前 clientId 重新绑定 WebSocket（登录/换 token 后可选调用）
 */
export async function rebindWebSocket(request) {
	const clientId = uni.getStorageSync('clientId');
	if (!clientId || !request) return null;
	return request({
		method: 'POST',
		data: { clientid: clientId },
		url: '/appauth/bind',
		skipAuth: true
	});
}
