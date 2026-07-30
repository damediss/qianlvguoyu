import { defineStore } from 'pinia'
import { getCompetitionHome } from '@/api/competition'

/**
 * 竞赛状态管理
 */
export const useCompetitionStore = defineStore('competition', {
	// 状态
	state: () => ({
		// 当前竞赛ID
		currentMatchId: uni.getStorageSync('currentMatchId') || null,
		
		// 竞赛列表
		competitionList: [],
		
		// 当前竞赛信息
		currentCompetitionInfo: null,
		
		// 报名数据
		registrationData: null,
		
		// 参赛情况
		participationData: null,
		
		// 当前赛段
		currentStageData: null,
		
		// 风采展示列表
		styleDisplayList: [],
		
		// 竞赛资讯列表
		competitionInfoList: []
	}),
	
	// 操作
	actions: {
		/**
		 * 设置当前竞赛ID
		 */
		setMatchId(matchId) {
			this.currentMatchId = matchId
			uni.setStorageSync('currentMatchId', matchId)
		},
		
		/**
		 * 清除竞赛ID
		 */
		clearMatchId() {
			this.currentMatchId = null
			uni.removeStorageSync('currentMatchId')
		},
		
		/**
		 * 从后端加载竞赛首页数据
		 * @param {Number} userId - 用户ID（可选）,如果传入则根据报名状态返回当前赛段
		 * @returns {Boolean} 是否加载成功
		 */
		async fetchCompetitionHome(userId = null) {
			try {
				uni.showLoading({ title: '加载中...' })
				
				const res = await getCompetitionHome(userId)
				
				if (res.code === 1 && res.data) {
					// 保存各项数据到 state
					this.registrationData = res.data.registrationData
					this.participationData = res.data.participationData
					this.currentStageData = res.data.currentStageData
					this.styleDisplayList = res.data.styleDisplayList || []
					this.competitionInfoList = res.data.competitionInfoList || []
					
					// 自动设置当前竞赛ID
					if (res.data.registrationData?.cardData?.id) {
						this.setMatchId(res.data.registrationData.cardData.id)
						this.currentCompetitionInfo = res.data.registrationData.cardData
					}
					
					uni.hideLoading()
					return true
				} else {
					uni.hideLoading()
					uni.showToast({
						title: res.msg || '加载失败',
						icon: 'none'
					})
					return false
				}
			} catch (error) {
				console.error('加载竞赛数据失败:', error)
				uni.hideLoading()
				uni.showToast({
					title: '加载失败，请重试',
					icon: 'none'
				})
				return false
			}
		},
		
		/**
		 * 检查是否已选择竞赛
		 * @returns {Boolean} 是否已选择竞赛
		 */
		checkMatchId() {
			if (!this.currentMatchId) {
				uni.showToast({
					title: '请先进入竞赛中心',
					icon: 'none',
					duration: 1500
				})
				setTimeout(() => {
					uni.navigateBack()
				}, 1500)
				return false
			}
			return true
		}
	},
	
	// 计算属性
	getters: {
		// 是否已选择竞赛
		hasMatchId: (state) => state.currentMatchId !== null,
		
		// 竞赛名称
		competitionName: (state) => {
			return state.currentCompetitionInfo?.mainText || 
				   state.participationData?.competitionName || 
				   '未知竞赛'
		},
		
		// 报名人数
		registrationCount: (state) => {
			return state.participationData?.registrationCount || 0
		},
		
		// 参赛人数
		participantCount: (state) => {
			return state.participationData?.participantCount || 0
		}
	}
})
