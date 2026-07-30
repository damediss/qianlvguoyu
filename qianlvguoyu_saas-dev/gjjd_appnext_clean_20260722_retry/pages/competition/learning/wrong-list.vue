<template>
	<view class="content bg-ff">
		<view class="header bg-ff shadow-sm" :style="'padding-top:' + $statusBarHeight + 'px;'">
			<view class="header-box dis-flex-vc dis-flex-hsb">
				<view class="header-back w-60 h-60 dis-flex-vc" @click="goBack">
					<i class="gjjd-iconfont icon-xiangzuo font-s20 color-33"></i>
				</view>
				<view class="header-title font-s18 font-wb color-33 w-full text-C">错题列表</view>
				<view class="header-placeholder w-60"></view>
			</view>
		</view>

		<view class="body" :style="bodyMarginTop">

			<view v-if="!loading && wrongQuestions.length > 0" class="question-card bg-33aafc radius-32">
				<view class="question-content-scroll bg-ff radius-16">
					<view class="question-type bg-33aafc radius-8">
						<text class="font-s14 font-wb color-ff">{{ currentQuestion.type === 'single' ? '单选题' :
							'判断题' }}</text>
					</view>

					<text class="question-text font-s15 color-33">{{ currentQuestion.question }}</text>

					<view class="options-list dis-flex flex-column">
						<view v-for="(option, index) in currentQuestion.options" :key="index"
							class="option-item dis-flex-hsb dis-flex-vc bg-ff radius-8 border-transparent" :class="{
								'correct-answer bg-f0f9ff border-00b6ff': index === currentQuestion.answer,
								'wrong-answer bg-fff5f5 border-ff6b6b': index === currentQuestion.userAnswer && index !== currentQuestion.answer
							}">
							<text class="option-text font-s15 color-33">{{ option }}</text>
							<text v-if="index === currentQuestion.answer"
								class="answer-tag font-s12 color-4caf50">正确答案</text>
						</view>
					</view>
				</view>

				<view class="user-answer-tip radius-16 border-ff6b6b">
					<text class="font-s14 color-ff">你的答案：{{ currentQuestion.options &&
						currentQuestion.options[currentQuestion.userAnswer] || '未知' }}</text>
				</view>

				<view class="pagination dis-flex-vc dis-flex-hsb">
					<view class="page-btn bg-ff radius-8" :class="{ 'disabled': currentIndex === 0 }"
						@click="prevQuestion">
						<text class="font-s14 color-33">上一题</text>
					</view>

					<view class="page-info">
						<text class="font-s14 color-33">{{ currentIndex + 1 }} / {{ wrongQuestions.length }}</text>
					</view>

					<view class="page-btn bg-ff radius-8"
						:class="{ 'disabled': currentIndex === wrongQuestions.length - 1 }" @click="nextQuestion">
						<text class="font-s14 color-33">下一题</text>
					</view>
				</view>
			</view>

			<view v-if="!loading && wrongQuestions.length > 0" class="stats-circle dis-flex flex-column dis-flex-hv">
				<view class="circle dis-flex-hv bg-ff border-33 radius-full">
					<text class="count-text font-s48 font-wb color-33">{{ wrongQuestions.length }}</text>
				</view>
				<text class="stats-label font-s16 color-33">我的错题</text>
			</view>

			<view v-if="!loading && wrongQuestions.length === 0" class="empty-state dis-flex flex-column dis-flex-hv">
				<text class="font-s16 color-99">暂无错题记录</text>
			</view>
		</view>
	</view>
</template>

<script>
import { getWrongQuestions } from "@/api/competition";
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
			loading: true,
			matchId: null,
			currentIndex: 0,
			wrongQuestions: []
		}
	},

	computed: {
		currentQuestion() {
			return this.wrongQuestions[this.currentIndex] || {};
		},
		bodyMarginTop() {
			const headerPx = uni.upx2px(100);
			return `margin-top:${this.$statusBarHeight + headerPx}px;`;
		}
	},

	onLoad(options) {
		if (options && options.matchId) {
			this.matchId = parseInt(options.matchId);
		}

		this.loadWrongQuestions();
	},

	methods: {
		goBack() {
			uni.navigateBack();
		},

		prevQuestion() {
			if (this.currentIndex > 0) {
				this.currentIndex--;
			}
		},

		nextQuestion() {
			if (this.currentIndex < this.wrongQuestions.length - 1) {
				this.currentIndex++;
			}
		},

		async loadWrongQuestions() {
			if (!this.userStore.checkLogin()) {
				this.loading = false;
				return;
			}

			this.loading = true;
			uni.showLoading({ title: '加载中...' });

			try {
				const res = await getWrongQuestions({
					userId: this.userStore.userId,
					type: null,  // null表示查询所有类型的错题
					limit: 100
				});

				if (isSuccess(res) && res.data) {
					this.wrongQuestions = res.data.questions || [];
				} else {
					showError(res, '加载失败');
				}
			} catch (error) {
				console.error('加载错题失败:', error);
				uni.showToast({ title: '加载失败，请重试', icon: 'none' });
			} finally {
				uni.hideLoading();
				this.loading = false;
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
	z-index: 100;
}

.header-box {
	height: 100rpx;
	padding: 0 30rpx;
}

.body {
	padding: 30rpx;
	padding-bottom: 240rpx;
}

.question-card {
	margin-bottom: 40rpx;
	padding: 20rpx;
}

.question-content-scroll {
	padding: 20rpx;
	margin-bottom: 15rpx;
	max-height: 650rpx;
	overflow-y: auto;
	-webkit-overflow-scrolling: touch;
}

.question-content-scroll::-webkit-scrollbar {
	width: 4rpx;
}

.question-content-scroll::-webkit-scrollbar-thumb {
	background-color: rgba(0, 0, 0, 0.2);
	border-radius: 4rpx;
}

.question-content-scroll::-webkit-scrollbar-track {
	background-color: transparent;
}

.question-type {
	display: inline-flex;
	align-items: center;
	padding: 0 20rpx;
	height: 56rpx;
	margin-bottom: 15rpx;
}

.question-text {
	display: block;
	line-height: 1.8;
	margin-bottom: 30rpx;
}

.options-list {
	gap: 15rpx;
}

.option-item {
	padding: 20rpx;
	transition: all 0.3s;
}

.option-text {
	flex: 1;
	line-height: 1.5;
}

.answer-tag {
	margin-left: 10rpx;
	white-space: nowrap;
	font-weight: bold;
}

.user-answer-tip {
	display: flex;
	align-items: center;
	height: 60rpx;
	padding: 0 24rpx;
	margin-bottom: 15rpx;
}

.pagination {
	padding: 15rpx 0;
	gap: 20rpx;
}

.page-btn {
	display: inline-flex;
	align-items: center;
	height: 56rpx;
	padding: 0 32rpx;
	transition: all 0.3s;
}

.page-btn:active:not(.disabled) {
	transform: scale(0.95);
	opacity: 0.8;
}

.page-btn.disabled {
	opacity: 0.5;
	cursor: not-allowed;
}

.page-info {
	flex: 1;
	text-align: center;
	line-height: 56rpx;
}

.stats-circle {
	padding: 40rpx 0;
}

.circle {
	width: 200rpx;
	height: 200rpx;
	margin-bottom: 20rpx;
}

.count-text {
	line-height: 1;
}

.stats-label {
	margin-top: 10rpx;
}

.empty-state {
	min-height: 60vh;
}
</style>