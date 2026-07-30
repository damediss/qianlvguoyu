/**
 * 用户信息管理工具
 * 统一管理用户ID获取逻辑
 */

/**
 * 获取当前用户ID
 * @returns {Number|null} 用户ID，未登录返回 null
 */
export const getUserId = () => {
	// 从 storage 获取（登录后会存储）
	const storageUserId = uni.getStorageSync('userId');
	if (storageUserId) {
		return parseInt(storageUserId);
	}
	
	// 未登录，提示并跳转登录页
	console.warn('用户未登录，请先登录');
	uni.showToast({
		title: '请先登录',
		icon: 'none',
		duration: 1500
	});
	
	setTimeout(() => {
		uni.navigateTo({
			url: '/pages/auth/login'
		});
	}, 1500);
	
	return null;
};

/**
 * 设置用户ID（登录成功后调用）
 * @param {Number} userId - 用户ID
 */
export const setUserId = (userId) => {
	uni.setStorageSync('userId', userId);
};

/**
 * 清除用户ID（退出登录时调用）
 */
export const clearUserId = () => {
	uni.removeStorageSync('userId');
};

/**
 * 获取当前用户信息（预留接口）
 * TODO: 登录功能实现后，返回完整的用户信息对象
 * @returns {Object} 用户信息
 */
export const getUserInfo = () => {
	return {
		userId: getUserId(),
		// 以后可以添加更多字段：username, avatar, role等
	};
};

