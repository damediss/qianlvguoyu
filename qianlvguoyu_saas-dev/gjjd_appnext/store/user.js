import { defineStore } from 'pinia'
import { isValidToken } from '@/utils/auth.js'

/**
 * 用户状态管理
 */
export const useUserStore = defineStore('user', {
	// 状态
	state: () => ({
		userId: uni.getStorageSync('userId') || null,
		userInfo: null,
		token: uni.getStorageSync('itoken') || null,
		clientId: uni.getStorageSync('clientId') || null
	}),
	
	// 操作
	actions: {
		/**
		 * 设置用户ID
		 */
		setUserId(userId) {
			this.userId = userId
			uni.setStorageSync('userId', userId)
		},
		
		/**
		 * 设置用户信息
		 */
		setUserInfo(userInfo) {
			this.userInfo = userInfo
			uni.setStorageSync('userInfo', JSON.stringify(userInfo))
		},
		
		/**
		 * 设置 token（控制点：非空校验）
		 */
		setToken(token) {
			// ★ 控制点 ③：空/非法 token 不写入，避免"假 token"状态
			if (!isValidToken(token)) {
				console.error('setToken: 拒绝写入空/非法 token', token);
				return false;
			}
			this.token = token
			uni.setStorageSync('itoken', token)
			return true
		},
		
		/**
		 * 设置 websocket clientid
		 */
		setClientId(clientid) {
			this.clientId = clientid
			uni.setStorageSync('clientId', clientid)
		},
		
		/**
		 * 登录（保存用户信息）
		 */
		login(userData) {
			console.log('📝 userStore.login 接收到的数据：', userData);
			
			if (userData.token) {
				console.log('✅ 保存 token:', userData.token);
				const ok = this.setToken(userData.token)
				if (!ok) {
					console.error('❌ token 写入失败，登录中断');
					return false;
				}
			} else {
				console.error('❌ userData.token 不存在！');
			}
			
			if (userData.user?.id) {
				console.log('✅ 保存 userId:', userData.user.id);
				this.setUserId(userData.user.id)
			} else {
				console.error('❌ userData.user.id 不存在！', userData);
			}
			
			if (userData.user) {
				this.setUserInfo(userData.user)
			}
			return true
		},
		
		/**
		 * 退出登录
		 */
		logout() {
			this.userId = null
			this.userInfo = null
			this.token = null
			uni.removeStorageSync('userId')
			uni.removeStorageSync('userInfo')
			uni.removeStorageSync('itoken')
			uni.removeStorageSync('currentMatchId') // 清除竞赛ID
		},
		
		/**
		 * 检查登录状态
		 * @returns {Boolean} 是否已登录
		 */
		checkLogin() {
			if (!this.userId) {
				uni.showToast({
					title: '请先登录',
					icon: 'none',
					duration: 1500
				})
				setTimeout(() => {
					uni.navigateTo({
						url: '/pages/auth/login'
					})
				}, 1500)
				return false
			}
			return true
		}
	},
	
	// 计算属性
	getters: {
		// 是否已登录
		isLoggedIn: (state) => state.userId !== null,
		
		// 用户昵称
		userName: (state) => state.userInfo?.name || '未登录',
		
		// 用户头像
		userAvatar: (state) => state.userInfo?.avatar || '/static/default-avatar.png'
	}
})
