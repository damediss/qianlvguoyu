<template>
	<view class="content bg-f8">
		<view class="header bg-ff shadow-sm" :style="'padding-top:' + $statusBarHeight + 'px;'">
			<view class="header-box dis-flex-hsb dis-flex-vc">
				<view class="header-back dis-flex-hv" @click="goBack">
					<i class="gjjd-iconfont icon-xiangzuo font-s20 color-33"></i>
				</view>
				<view class="header-title font-s18 font-wb color-33 text-C">积分明细</view>
				<view class="header-placeholder w-60"></view>
			</view>
		</view>

		<view class="body" :style="'padding-top:' + (50 + $statusBarHeight + 20) + 'px;'">
			<view class="score-summary bg-ff radius-16 shadow-sm">
				<view class="summary-title font-s16 font-wb color-33">当前积分</view>
				<view class="summary-value font-s32 font-wb color-57d801">{{ totalPoints }}</view>
				<view class="summary-desc font-s12 color-99">累计获得积分</view>
			</view>

			<view class="score-list">
				<view v-for="(item, index) in scoreList" :key="item.id" class="score-item bg-ff radius-16 shadow-sm">
					<view class="score-icon dis-flex-hv">
						<i :class="item.icon + ' font-s20'" :style="{ color: item.color }"></i>
					</view>
					<view class="score-info">
						<text class="score-title font-s15 color-33">{{ item.title }}</text>
						<text class="score-desc font-s12 color-99">{{ item.desc }}</text>
						<text class="score-time font-s12 color-99">{{ item.time }}</text>
					</view>
					<view class="score-points">
						<text class="font-s16 font-wb" :class="item.points > 0 ? 'color-57d801' : 'color-ff6b6b'">
							{{ item.points > 0 ? '+' : '' }}{{ item.points }}
						</text>
					</view>
				</view>
			</view>

			<!-- 加载完成但无数据时显示空状态 -->
			<view v-if="scoreList.length === 0 && loaded" class="empty-state dis-flex-hv flex-column">
				<view class="empty-icon mar-B20">
					<i class="gjjd-iconfont icon-kong font-s48 color-cc"></i>
				</view>
				<text class="empty-text font-s15 color-99">暂无积分记录</text>
			</view>
		</view>
	</view>
</template>

<script>
import { getPointDetail } from "@/api/competition";
import { useUserStore } from '@/store/user';
import { useCompetitionStore } from '@/store/competition';
import { isSuccess } from '@/utils/response';

export default {
	setup() {
		const userStore = useUserStore()
		const competitionStore = useCompetitionStore()
		return { userStore, competitionStore }
	},
	data() {
		return {
			matchId: null,
			totalPoints: 0,
			scoreList: [],
			loaded: false, // 标记是否加载完成
			sceneMap: {
				'sign': { title: '签到奖励', desc: '每日签到获得积分', icon: 'gjjd-iconfont icon-qian', color: '#00b6ff' },
				'match': { title: '个人竞技', desc: '参与个人竞技获得/扣除积分', icon: 'gjjd-iconfont icon-bisai', color: '#ff9900' },
				'moniexam': { title: '模拟考试', desc: '参与模拟考试获得积分', icon: 'gjjd-iconfont icon-kaoshi', color: '#00cc66' },
				'wrong': { title: '答错题', desc: '答题错误扣除积分', icon: 'gjjd-iconfont icon-cuowu', color: '#ff6b6b' },
				'wz': { title: '问卷/文章', desc: '完成问卷或阅读文章获得积分', icon: 'gjjd-iconfont icon-wenjuan', color: '#9966ff' },
				'courseware': { title: '阅读课件', desc: '完成课件学习获得积分', icon: 'gjjd-iconfont icon-wenjuan', color: '#00cc66' },
				'article': { title: '阅读文章', desc: '完成文章阅读获得积分', icon: 'gjjd-iconfont icon-wenjuan', color: '#00cc66' },
				'checkpoint': { title: '答题闯关', desc: '参与答题闯关获得/扣除积分', icon: 'gjjd-iconfont icon-bisai', color: '#00b6ff' },
				'adventure': { title: '大冒险', desc: '参与大冒险获得/扣除积分', icon: 'gjjd-iconfont icon-bisai', color: '#505ef2' },
				'single': { title: '单人PK', desc: '参与单人PK获得积分', icon: 'gjjd-iconfont icon-bisai', color: '#ff6b00' }
			}
		}
	},
	onLoad(options) {
		if (options && options.matchId) {
			this.matchId = parseInt(options.matchId);
		}

		if (!this.matchId || isNaN(this.matchId) || this.matchId <= 0) {
			uni.showToast({ title: '竞赛ID无效', icon: 'none', duration: 2000 });
			return;
		}

		this.loadScoreDetail();
	},
	methods: {
		goBack() {
			uni.navigateBack();
		},

		async loadScoreDetail() {
			if (!this.userStore.checkLogin()) {
				return;
			}

			uni.showLoading({
				title: '加载中...'
			});

			try {
				const res = await getPointDetail({
					userId: this.userStore.userId,
					matchId: this.matchId,
					limit: 100
				});

				if (isSuccess(res) && res.data) {
					this.totalPoints = res.data.totalPoints || 0;

					if (res.data.pointLogs && res.data.pointLogs.length > 0) {
						this.scoreList = res.data.pointLogs.map(log => {
							const sceneConfig = this.sceneMap[log.scene] || {
								title: '其他积分',
								desc: '积分变动',
								icon: 'gjjd-iconfont icon-jifen',
								color: '#666666'
							};

							const pointValue = parseFloat(log.point || 0);
							const points = log.type === '+' ? pointValue : -pointValue;

							return {
								id: log.id,
								title: sceneConfig.title,
								desc: sceneConfig.desc,
								time: this.formatTime(log.createTime),
								points: points,
								icon: sceneConfig.icon,
								color: sceneConfig.color
							};
						});
					}
				}

			} catch (error) {
				console.error('加载积分明细失败:', error);
				uni.showToast({ title: '加载失败，请重试', icon: 'none' });
			} finally {
				uni.hideLoading();
				this.loaded = true; // 标记加载完成
			}
		},

		formatTime(dateStr) {
			if (!dateStr) return '';
			const date = new Date(dateStr);
			const year = date.getFullYear();
			const month = String(date.getMonth() + 1).padStart(2, '0');
			const day = String(date.getDate()).padStart(2, '0');
			const hours = String(date.getHours()).padStart(2, '0');
			const minutes = String(date.getMinutes()).padStart(2, '0');
			return `${year}-${month}-${day} ${hours}:${minutes}`;
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
	z-index: 100;
}

.header-box {
	height: 50px;
	padding: 0 30rpx;
}

.header-back {
	width: 60rpx;
	height: 60rpx;
}

.body {
	padding: 20rpx;
	padding-bottom: 40rpx;
}

.score-summary {
	padding: 30rpx;
	text-align: center;
	margin-bottom: 20rpx;
}

.summary-title {
	margin-bottom: 15rpx;
}

.summary-value {
	margin-bottom: 10rpx;
}

.summary-desc {
	opacity: 0.8;
}

.score-list {
	margin-bottom: 20rpx;
}

.score-item {
	display: flex;
	align-items: center;
	padding: 20rpx;
	margin-bottom: 15rpx;
}

.score-icon {
	width: 60rpx;
	height: 60rpx;
	margin-right: 20rpx;
	flex-shrink: 0;
}

.score-info {
	flex: 1;
}

.score-title {
	display: block;
	margin-bottom: 8rpx;
	line-height: 1.4;
}

.score-desc {
	display: block;
	margin-bottom: 5rpx;
	line-height: 1.3;
}

.score-time {
	display: block;
	line-height: 1.3;
}

.score-points {
	flex-shrink: 0;
	text-align: right;
}

.empty-state {
	min-height: 60vh;
}

.empty-text {
	opacity: 0.8;
}
</style>