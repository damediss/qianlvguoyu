<template>
	<view class="content bg-f8">
		<view class="header bg-ff border-b-ee" :style="'padding-top:' + $statusBarHeight + 'px;'">
			<view class="header-box dis-flex-vc dis-flex-hsb">
				<view class="header-back dis-flex-vc" @click="goBack">
					<i class="gjjd-iconfont icon-xiangzuo font-s20 color-33"></i>
				</view>
				<!-- 动态标题 -->
				<view class="header-title font-s18 font-wb color-33 text-C">{{ pageTitle }}</view>
				<view class="header-placeholder w-60"></view>
			</view>
		</view>

		<view class="timer-bar bg-99 dis-flex-hv" :style="'top:' + (50 + $statusBarHeight) + 'px;'">
			<text class="timer-text font-s14 color-ff">已用时:{{ formattedTime }}</text>
		</view>

		<view class="body" :style="'padding-top:' + (50 + $statusBarHeight + 35) + 'px;'">
			<rich-text class="content-text" :nodes="content"></rich-text>
		</view>
	</view>
</template>

<script>
import {
	getCoursewareDetail, recordCoursewareStudy,
	getArticleDetail, recordArticleStudy
} from "@/api/competition";
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
			contentId: '',        // 统一使用 contentId
			contentTitle: '',     // 统一使用 contentTitle
			contentType: '',      // 内容类型：article 或 courseware
			content: '',
			startTime: 0,
			elapsedTime: 0,
			timer: null
		}
	},
	computed: {
		/**
		 * 动态页面标题
		 */
		pageTitle() {
			return this.contentType === 'article' ? '文章详情' : '课件详情';
		},

		/**
		 * 动态日志标签
		 */
		logTag() {
			return this.contentType === 'article' ? '【阅读文章】' : '【阅读课件】';
		},

		/**
		 * 格式化时间显示
		 */
		formattedTime() {
			const hours = Math.floor(this.elapsedTime / 3600);
			const minutes = Math.floor((this.elapsedTime % 3600) / 60);
			const seconds = this.elapsedTime % 60;

			return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
		}
	},
	onLoad(options) {
		// 获取参数
		this.contentId = options.id || '';
		this.contentTitle = decodeURIComponent(options.title || '');
		this.contentType = options.type || 'article';  // 默认为 article

		if (options.matchId) {
			this.matchId = parseInt(options.matchId);
		} else {
			this.matchId = uni.getStorageSync('currentMatchId') || null;
		}

		// 加载内容
		this.loadContent();

		// 启动计时器
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
		 * 加载内容
		 */
		async loadContent() {
			try {
				uni.showLoading({ title: '加载中...' });

				let res;
				if (this.contentType === 'article') {
					res = await getArticleDetail({ id: parseInt(this.contentId) });
				} else {
					res = await getCoursewareDetail({ id: parseInt(this.contentId) });
				}

				uni.hideLoading();

				if (isSuccess(res) && res.data) {
					this.content = res.data.content || '';
					this.contentTitle = res.data.title || this.contentTitle;
				} else {
					console.error(`${this.logTag}内容加载失败：`, res.msg);
					showError(res, '加载失败');
				}
			} catch (error) {
				console.error(`${this.logTag}加载内容异常：`, error);
				uni.hideLoading();
				uni.showToast({ title: '加载失败，请重试', icon: 'none' });
			}
		},

		/**
		 * 启动计时器
		 */
		startTimer() {
			this.startTime = Date.now();
			this.timer = setInterval(() => {
				this.elapsedTime = Math.floor((Date.now() - this.startTime) / 1000);
			}, 1000);

		},

		/**
		 * 停止计时器
		 */
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
				let res;
				const params = {
					id: parseInt(this.contentId),
					userId: this.userStore.userId,
					matchId: this.matchId,
					readTime: this.elapsedTime
				};

				if (this.contentType === 'article') {
					res = await recordArticleStudy(params);
				} else {
					res = await recordCoursewareStudy(params);
				}

				if (isSuccess(res) && res.data) {
					// 记录成功
				} else {
					console.error(`${this.logTag}学习时长记录失败：`, res.msg);
				}
			} catch (error) {
				console.error(`${this.logTag}记录学习时长异常：`, error);
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

.content-text {
	width: 100%;
	line-height: 1.8;
	font-size: 30rpx;
	color: #333;
}

.content-text :deep(p) {
	margin: 0;
	padding: 10rpx 0;
}

.content-text :deep(img) {
	max-width: 100%;
	height: auto;
	display: block;
	margin: 20rpx 0;
}
</style>
