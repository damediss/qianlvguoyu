/**
 * 扫码结果 → App 内跳转路径（增量：仅新增 research 识别）
 * 未识别返回空字符串，由调用方继续走原有逻辑
 */

function parseQueryParam(result, pagePath, paramName) {
	if (!result || !pagePath || !paramName || result.indexOf(pagePath) < 0) return '';
	const re = new RegExp('[?&]' + paramName + '=([^&#]+)');
	const match = String(result).match(re);
	if (!match) return '';
	try {
		return decodeURIComponent(match[1]);
	} catch (e) {
		return match[1];
	}
}

/**
 * @param {string} result 扫码原始内容
 * @returns {string} 可 navigateTo 的 url，无法识别时返回 ''
 */
export function resolveResearchCollectPath(result) {
	if (!result) return '';
	const text = String(result);
	const token = parseQueryParam(text, 'pages/research/collect', 'token');
	if (token) {
		return '/pages/research/collect?token=' + encodeURIComponent(token);
	}
	return '';
}
