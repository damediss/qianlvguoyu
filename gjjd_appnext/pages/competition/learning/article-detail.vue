<template>
	<view class="content bg-f8">
		<view class="header bg-ff border-b-ee" :style="'padding-top:' + $statusBarHeight + 'px;'">
			<view class="header-box dis-flex-vc dis-flex-hsb">
				<view class="header-back dis-flex-vc" @click="goBack">
					<i class="gjjd-iconfont icon-xiangzuo font-s20 color-33"></i>
				</view>
				<view class="header-title font-s18 font-wb color-33 text-C">文章详情</view>
				<view class="header-placeholder w-60"></view>
			</view>
		</view>

		<view class="timer-bar bg-99 dis-flex-hv" :style="'top:' + (50 + $statusBarHeight) + 'px;'">
			<text class="timer-text font-s14 color-ff">已用时:{{ formattedTime }}</text>
		</view>

		<view class="body" :style="'padding-top:' + (50 + $statusBarHeight + 35) + 'px;'">
			<rich-text class="article-content" :nodes="content"></rich-text>
		</view>
	</view>
</template>

<script>
import { getCoursewareDetail, recordCoursewareStudy } from "@/api/competition";
import { useUserStore } from '@/store/user';
import { useCompetitionStore } from '@/store/competition';
import { isSuccess, showError } from '@/utils/response';

export default {
	setup() {
		const userStore = useUserStore()
		const competitionStore = useCompetitionStore()
		return { userStore, competitionStore }
	},
	data() {
		return {
			matchId: null,
			articleId: '',
			articleTitle: '',
			content: '',
			startTime: 0,
			elapsedTime: 0,
			timer: null
		}
	},
	computed: {
		formattedTime() {
			const hours = Math.floor(this.elapsedTime / 3600);
			const minutes = Math.floor((this.elapsedTime % 3600) / 60);
			const seconds = this.elapsedTime % 60;
			
			return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
		}
	},
	onLoad(options) {
		this.articleId = options.id || '';
		this.articleTitle = decodeURIComponent(options.title || '');

		if (options.matchId) {
			this.matchId = parseInt(options.matchId);
		} else {
			this.matchId = uni.getStorageSync('currentMatchId') || null;
		}

		this.loadContent();

		this.startTimer();
	},
	onUnload() {
		// 页面卸载时停止计时并记录学习时长
		this.stopTimer();
	},
	methods: {
		goBack() {
			this.stopTimer();
			uni.navigateBack();
		},

		/**
		 * 加载文章内容
		 */
		async loadContent() {
			try {
				uni.showLoading({ title: '加载中...' });

				const res = await getCoursewareDetail({
					articleId: parseInt(this.articleId)
				});

				uni.hideLoading();

				if (isSuccess(res) && res.data) {
					this.content = res.data.content || '';
					this.articleTitle = res.data.title || this.articleTitle;
				} else {
					showError(res, '加载失败');
				}
			} catch (error) {
				console.error('【阅读文章】加载文章详情失败：', error);
				uni.hideLoading();
				uni.showToast({ title: '加载失败，请重试', icon: 'none' });
			}
		},

		startTimer() {
			this.startTime = Date.now();
			this.timer = setInterval(() => {
				this.elapsedTime = Math.floor((Date.now() - this.startTime) / 1000);
			}, 1000);
		},

		stopTimer() {
			if (this.timer) {
				clearInterval(this.timer);
				this.timer = null;

				// 记录学习时长
				this.recordStudyTime();
			}
		},

		/**
		 * 记录学习时长（提交到后端计算积分）
		 */
		async recordStudyTime() {
			// 只有学习时长大于5秒才记录
			if (this.elapsedTime < 5) {
				return;
			}
			
			if (!this.userStore.checkLogin()) {
				return;
			}

			try {
				const res = await recordCoursewareStudy({
					articleId: parseInt(this.articleId),
					userId: this.userStore.userId,
					matchId: this.matchId,
					readTime: this.elapsedTime
				});

				if (isSuccess(res) && res.data) {
					// 记录成功
				}
			} catch (error) {
				console.error('【阅读文章】记录学习时长失败：', error);
			}
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
	box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
}

.header-box {
	height: 50px;
	padding: 0 30rpx;
}

.header-back {
	width: 60rpx;
	height: 60rpx;
}

.header-title {
	flex: 1;
}

.timer-bar {
	position: fixed;
	left: 0;
	right: 0;
	height: 35px;
	z-index: 9998;
}

.body {
	padding: 20rpx;
}

.article-content {
	width: 100%;
	line-height: 1.8;
	font-size: 30rpx;
	color: #333;
}

.article-content :deep(p) {
	margin: 0;
	padding: 10rpx 0;
}

.article-content :deep(img) {
	max-width: 100%;
	height: auto;
	display: block;
	margin: 20rpx 0;
}
</style>