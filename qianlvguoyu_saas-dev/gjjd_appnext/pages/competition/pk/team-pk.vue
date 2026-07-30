<template>
	<view class="content">
		<view class="header bg-ff border-b-ee" :style="'padding-top:' + $statusBarHeight + 'px;'">
			<view class="header-box dis-flex-hsb dis-flex-vc">
				<view class="header-back dis-flex-hv w-60" @click="goBack">
					<i class="gjjd-iconfont icon-xiangzuo font-s20 color-33"></i>
				</view>
				<view class="header-title font-s18 font-wb color-33 text-C">组队竞技</view>
				<view class="header-right w-60"></view>
			</view>
		</view>

		<view class="body">
			<view class="bg-decoration"></view>

			<view class="content-wrapper" :style="'padding-top:' + (50 + $statusBarHeight + 40) + 'px;'">
				<view class="rules-container">
					<text
						class="rules-text font-s15 color-ff">组队PK模式会随机匹配该模式下同时在线的两位参赛人员组队，组队成功后，将与另外两位随机组队的参赛人员进行答题PK，如果同时参与PK人数不足以两两组队，将等待直至有另外一个参赛人员参与组队PK，同时参与人数不小于4人。</text>
				</view>

				<view class="pk-time dis-flex-hv">
					<text class="time-text font-s15 font-wb color-ff">PK赛时间：{{ pkStartDate }} ~ {{ pkEndDate }}</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import { getTeamPKInfo } from "@/api/competition.js";
import { useCompetitionStore } from '@/store/competition';

export default {
	setup() {
		const competitionStore = useCompetitionStore()
		return { competitionStore }
	},
	data() {
		return {
			matchId: null, // 竞赛ID
			pkStartDate: '',
			pkEndDate: '',
			peopleNum: 2, // 每队人数
			examNum: 5, // 题目数量
			getPoint: 5 // 获得积分
		}
	},
	onLoad(options) {
		// 从路由参数获取竞赛ID（如果有）
		if (options.matchId) {
			this.matchId = parseInt(options.matchId);
		}

		// 加载组队规则信息
		this.loadPKInfo();
	},
	methods: {
		goBack() {
			uni.navigateBack();
		},

		/**
		 * 加载PK赛信息（时间、规则等）
		 */
		async loadPKInfo() {
			try {
				uni.showLoading({ title: '加载中...' });

				// 优先使用 this.matchId，如果没有再从 Store 获取
				const matchId = this.matchId || this.competitionStore.currentMatchId;

				if (!matchId) {
					uni.showToast({
						title: '竞赛ID不能为空',
						icon: 'none'
					});
					uni.hideLoading();
					return;
				}

				const res = await getTeamPKInfo({
					matchId: matchId  // 使用驼峰风格，与后端 ParamUtils 保持一致
				});

				if (res.code === 1 && res.data) {
					this.pkStartDate = res.data.startDate || '';
					this.pkEndDate = res.data.endDate || '';
					this.peopleNum = res.data.peopleNum || 2;
					this.examNum = res.data.examNum || 5;
					this.getPoint = res.data.getPoint || 5;
				} else {
					uni.showToast({
						title: res.msg || '加载失败',
						icon: 'none'
					});
				}
			} catch (error) {
				console.error('加载PK信息失败:', error);
				uni.showToast({
					title: '加载失败，请重试',
					icon: 'none'
				});
			} finally {
				uni.hideLoading();
			}
		}
	}
}
</script>

<style scoped>
.content {
	height: 100vh;
	position: relative;
	overflow: hidden;
}

.header {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	z-index: 9999;
}

.header-box {
	height: 50px;
	padding: 0 20rpx;
}

.header-title {
	flex: 1;
}

.body {
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	overflow: hidden;
}

.bg-decoration {
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	z-index: 1;
	background-image: url('/static/images/pk.png');
	background-size: cover;
	background-position: center;
	background-repeat: no-repeat;
}

.content-wrapper {
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	z-index: 2;
	padding: 40rpx 40rpx 60rpx;
	display: flex;
	flex-direction: column;
	justify-content: flex-start;
	/* padding-top 通过内联样式动态计算 */
}

.rules-container {
	flex: 1;
	display: flex;
	align-items: flex-start;
	justify-content: center;
	padding: 0 30rpx;
	margin-top: 0;
}

.rules-text {
	line-height: 1.6;
	text-shadow: 0 2rpx 4rpx rgba(0, 0, 0, 0.2);
	text-align: justify;
	text-align-last: left;
}

.pk-time {
	padding-bottom: 600rpx;
	flex-shrink: 0;
	margin-top: auto;
}

.time-text {
	text-shadow: 0 2rpx 4rpx rgba(0, 0, 0, 0.2);
	white-space: nowrap;
}
</style>