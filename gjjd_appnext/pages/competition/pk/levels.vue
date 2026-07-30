<template>
	<view class="content bg-397ff3">
		<view class="header bg-ff border-b-ee" :style="'padding-top:' + $statusBarHeight + 'px;'">
			<view class="header-box dis-flex-hsb dis-flex-vc">
				<view class="header-back dis-flex-hv w-60" @click="goBack">
					<i class="gjjd-iconfont icon-xiangzuo font-s20 color-33"></i>
				</view>
				<view class="header-title font-s18 font-wb color-33 text-C">竞赛</view>
				<view class="header-placeholder w-60"></view>
			</view>
		</view>

		<view class="body" :style="'padding-top:' + (50 + $statusBarHeight) + 'px;'">
			<view class="cover-container">
				<image v-if="coverUrl" :src="coverUrl" mode="aspectFill" class="cover-bg-image"></image>
				<view v-else class="cover-bg-placeholder"></view>

				<view class="mode-tag-overlay">
					<text class="font-s14 font-wb color-ff">闯关模式</text>
				</view>

				<view class="score-overlay">
					<text class="font-s16 color-33">个人积分：</text>
					<text class="font-s20 font-wb color-red">{{ totalScore }}</text>
				</view>
			</view>

			<view>

				<view v-if="ruleText" class="rules-box">
					<view class="rules-title font-s16 font-wb color-ff">规则说明:</view>
					<rich-text class="rules-text font-s14 color-ff" :nodes="ruleText"></rich-text>
				</view>

				<view class="levels-list">
					<view v-if="levels.length === 0" class="empty-state dis-flex-hv flex-column bg-ff radius-16">
						<text class="font-s15 color-99">暂无关卡数据</text>
					</view>

					<view v-for="(level, index) in levels" :key="level.id"
						class="level-item dis-flex-hsb bg-ff radius-16 shadow-sm transition-all"
						@click="goToQuestion(level)">
						<view v-if="level.isPassed" class="completed-tag bg-fed912 radius-8">
							<text class="font-s12 font-wb color-ff">已完成</text>
						</view>

						<view class="level-left dis-flex-vc">
							<view class="level-icon bg-0066ff radius-full"></view>
							<text class="level-name font-s16 color-33">第{{ getLevelName(index) }}关</text>
						</view>

						<view class="level-right dis-flex-vc">
							<view class="stars dis-flex-vc">
								<image v-for="star in 3" :key="star" class="star-item"
									:src="star <= level.stars ? '/static/images/xi.png' : '/static/images/xu.png'">
								</image>
							</view>

							<view class="arrow-icon dis-flex-hv">
								<view class="arrow-triangle"></view>
							</view>
						</view>
					</view>
				</view>

				<view class="record-btn" @click="goToRecord">
					<text class="font-s16 font-wb color-ff">答题记录</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import { getAdventureInfo } from "@/api/competition";
import { useUserStore } from '@/store/user';
import { useCompetitionStore } from '@/store/competition';
import { isSuccess, showError } from '@/utils/response';

// 关卡名称映射表
const LEVEL_NAMES = ['一', '二', '三', '四', '五', '六', '七', '八', '九', '十'];

export default {
	setup() {
		const userStore = useUserStore()
		const competitionStore = useCompetitionStore()
		return { userStore, competitionStore }
	},
	data() {
		return {
			matchId: null, // 竞赛ID（从 URL 参数获取）
			totalScore: 0, // 总积分
			coverUrl: '', // 封面图片
			ruleText: '', // 规则说明（HTML格式，来自后台配置）
			dailyNum: 0, // 每日可参与次数
			todayCount: 0, // 今日已参与次数
			levels: [] // 关卡列表
		}
	},
	onLoad(options) {
		// 从 URL 参数获取 matchId
		if (options.matchId) {
			this.matchId = parseInt(options.matchId);
		} else {
			uni.showToast({
				title: '缺少竞赛ID参数',
				icon: 'none'
			});
			setTimeout(() => {
				uni.navigateBack();
			}, 1500);
			return;
		}
		// 页面加载时获取关卡数据
		this.loadAdventureInfo();
	},
	onShow() {
		// 每次显示页面时刷新关卡数据
		this.loadAdventureInfo();
	},
	methods: {
		goBack() {
			uni.navigateBack();
		},

		getLevelName(index) {
			return LEVEL_NAMES[index] || (index + 1);
		},

		async loadAdventureInfo() {
			if (!this.userStore.checkLogin()) {
				return;
			}

			try {
				uni.showLoading({ title: '加载中...' });

				const res = await getAdventureInfo({
					matchId: this.matchId,
					userId: this.userStore.userId
				});

				uni.hideLoading();


				// 后端 Result 类定义：1-成功，0-失败
				if (isSuccess(res) && res.data) {

					// 更新封面图
					this.coverUrl = res.data.cover || '';

					// 更新规则说明（支持HTML）
					this.ruleText = res.data.rule || '';

					// 更新每日次数
					this.dailyNum = res.data.dailyNum || 0;
					this.todayCount = res.data.todayCount || 0;

					// 更新总积分
					this.totalScore = res.data.totalPoint || 0;

					// 更新关卡列表
					const checkpoints = res.data.checkpoints || [];
					this.levels = checkpoints.map((cp, index) => {
						// 星级由后端计算返回（0-3星）
						const stars = cp.stars || 0;

						// 判断是否锁定：第一关默认解锁，其他关卡需前一关通过
						let locked = false;
						if (index > 0) {
							locked = !checkpoints[index - 1].isPassed;
						}

						return {
							id: cp.id,
							title: cp.title,
							stars: stars,
							locked: locked,
							isPassed: cp.isPassed,
							attemptCount: cp.attemptCount,
							examNum: cp.examNum
						};
					});

				} else {

					showError(res, '加载失败');
				}
			} catch (error) {
				uni.hideLoading();
				uni.showToast({
					title: '加载失败，请重试',
					icon: 'none'
				});
			}
		},

		/**
		 * 进入答题页面
		 */
		goToQuestion(level) {
			// 检查是否锁定
			if (level.locked) {
				uni.showToast({
					title: '请先通过前一关',
					icon: 'none'
				});
				return;
			}

			// 检查今日次数
			if (this.dailyNum > 0 && this.todayCount >= this.dailyNum) {
				uni.showModal({
					title: '温馨提示',
					content: `今日参与次数已用完（${this.todayCount}/${this.dailyNum}），请明天再来！`,
					showCancel: false
				});
				return;
			}

			// 跳转到答题页面
			uni.navigateTo({
				url: `/pages/competition/pk/question?matchId=${this.matchId}&checkpointId=${level.id}&title=${level.title}`,
				animationType: 'slide-in-right',
				animationDuration: 300
			});
		},

		/**
		 * 返回上一页
		 */
		goBack() {
			uni.navigateBack({
				delta: 1
			});
		},

		/**
		 * 查看答题记录
		 */
		goToRecord() {
			uni.navigateTo({
				url: '/pages/competition/pk/record',
				animationType: 'slide-in-right',
				animationDuration: 300
			});
		}
	}
}
</script>

<style scoped>
.content {
	min-height: 100vh;
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
	padding: 0;
}

.cover-container {
	position: relative;
	width: 100%;
	height: 280rpx;
	margin-bottom: 0;
	overflow: hidden;
}

.cover-bg-image {
	position: absolute;
	top: 0;
	left: 0;
	width: 100%;
	height: 100%;
	z-index: 1;
}

.cover-bg-placeholder {
	position: absolute;
	top: 0;
	left: 0;
	width: 100%;
	height: 100%;
	background: #397ff3;
	z-index: 1;
}

.mode-tag-overlay {
	position: absolute;
	top: 20rpx;
	left: 20rpx;
	padding: 0 20rpx;
	height: 64rpx;
	display: inline-flex;
	align-items: center;
	background: rgba(51, 51, 51, 0.8);
	border-radius: 8rpx;
	z-index: 2;
}

.score-overlay {
	position: absolute;
	top: 140rpx;
	right: 20rpx;
	padding: 20rpx 30rpx;
	background: rgba(255, 255, 255, 0.9);
	border-radius: 16rpx;
	box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);
	display: flex;
	align-items: center;
	z-index: 2;
}

.rules-box {
	padding: 25rpx 30rpx 30rpx 30rpx;
	background: transparent;
}

.rules-title {
	font-weight: bold;
	margin-bottom: 10rpx;
}

.rules-text {
	line-height: 1.6;
	word-wrap: break-word;
}

.rules-text :deep(p) {
	margin: 0;
	padding: 0;
	line-height: 1.6;
}

.levels-list {
	display: flex;
	flex-direction: column;
	gap: 25rpx;
	margin-bottom: 20rpx;
	padding: 0 20rpx;
}

.level-item {
	position: relative;
	padding: 25rpx 30rpx;
}

.level-item:active {
	transform: scale(0.98);
}

.completed-tag {
	position: absolute;
	top: 0;
	right: 0;
	padding: 0 16rpx;
	height: 48rpx;
	display: inline-flex;
	align-items: center;
	z-index: 2;
}

.level-left {
	gap: 20rpx;
}

.level-icon {
	width: 50rpx;
	height: 50rpx;
}

.level-right {
	gap: 4rpx;
	margin-right: 100rpx;
}

.stars {
	gap: 16rpx;
	margin-right: 24rpx;
}

.star-item {
	width: 40rpx;
	height: 40rpx;
}

.arrow-icon {
	margin: 0 6rpx;
}

/* 箭头三角形（border实现，无法用类替代） */
.arrow-triangle {
	width: 0;
	height: 0;
	border-top: 14rpx solid transparent;
	border-bottom: 14rpx solid transparent;
	border-left: 20rpx solid #333;
}

.record-btn {
	text-align: center;
	padding: 100rpx 0 40rpx 0;
}

.empty-state {
	min-height: 200rpx;
	padding: 60rpx 30rpx;
}
</style>