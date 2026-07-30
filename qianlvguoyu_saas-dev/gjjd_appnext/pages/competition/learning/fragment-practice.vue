<template>
	<view class="content bg-f8">
		<view class="header bg-ff border-b-ee" :style="'padding-top:' + $statusBarHeight + 'px;'">
			<view class="header-box dis-flex-vc dis-flex-hsb">
				<view class="header-back dis-flex-vc" @click="goBack">
					<i class="gjjd-iconfont icon-xiangzuo font-s20 color-33"></i>
				</view>
				<view class="header-title font-s18 font-wb color-33 text-C">碎片化学习</view>
				<view class="header-placeholder w-60"></view>
			</view>
		</view>

		<view class="body" :style="'margin-top:' + (50 + $statusBarHeight) + 'px;'">
			<view v-if="!loading && questions.length > 0" class="question-card bg-00b6ff radius-16 pad-20">
				<view class="question-type pad-B15">
					<text class="font-s14 font-wb color-ff">{{ currentQuestion.type === 'judge' ? '判断题' : '单选题'
						}}</text>
				</view>

				<view class="question-content">
					<text class="question-text font-s15 color-ff mar-B30">{{ currentQuestion.question }}</text>

					<view class="options-list mar-B20">
						<view v-for="(option, index) in currentQuestion.options" :key="index"
							class="option-item radius-8 pad-20 transition-fast" :class="[
								selectedAnswer === index ? 'selected-option bg-0a3c88' : 'bg-ff',
								selectedAnswer !== null ? 'disabled' : ''
							]" @click="selectAnswer(index)">
							<text class="option-text font-s15 line-h-15"
								:class="selectedAnswer === index ? 'color-ff' : 'color-33'">{{ option }}</text>
						</view>
					</view>

					<view class="answer-tip mar-T20 pad-T20" v-if="selectedAnswer !== null">
						<text class="font-s14 color-red">正确答案:{{ getOptionLabel(currentQuestion.answer) }}</text>
					</view>
				</view>

				<view class="pagination dis-flex-vc dis-flex-hsb pad-T20 gap-15">
					<view class="page-btn bg-ff radius-8 pad-12-20 text-C transition-fast flex-1"
						:class="{ 'disabled': currentIndex === 0 }" @click="prevQuestion">
						<text class="font-s14 color-33">上一题</text>
					</view>

					<view class="page-info bg-ff radius-8 pad-12-20 text-C flex-1">
						<text class="font-s14 color-33">{{ currentIndex + 1 }} / {{ questions.length }}</text>
					</view>

					<view class="page-btn bg-ff radius-8 pad-12-20 text-C transition-fast flex-1"
						:class="{ 'disabled': currentIndex === questions.length - 1 }" @click="nextQuestion">
						<text class="font-s14 color-33">下一题</text>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import { getFragmentQuestions } from "@/api/competition";
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
			currentIndex: 0,
			selectedAnswer: null,
			answerRecords: {}, // 记录每道题的答案 {questionIndex: answerIndex}
			questions: []  // 从后端API获取
		}
	},
	computed: {
		currentQuestion() {
			return this.questions[this.currentIndex] || {};
		}
	},
	watch: {
		currentIndex(newIndex) {
			// 切换题目时，从记录中恢复答案
			this.selectedAnswer = this.answerRecords[newIndex] !== undefined ? this.answerRecords[newIndex] : null;
		}
	},
	onLoad(options) {
		if (!this.competitionStore.checkMatchId()) return;

		// 加载题目
		this.loadQuestions();
	},
	methods: {
		async loadQuestions() {
			this.loading = true;
			uni.showLoading({ title: '加载题目中...' });

			try {
				const res = await getFragmentQuestions({
					matchId: this.competitionStore.currentMatchId
				});

				if (isSuccess(res) && res.data && res.data.questions) {
					this.questions = res.data.questions;

					// 初始化时恢复第一题的答案
					this.selectedAnswer = this.answerRecords[this.currentIndex] !== undefined ?
						this.answerRecords[this.currentIndex] : null;
				} else {
					showError(res, '加载题目失败');

					// 延迟返回
					setTimeout(() => {
						uni.navigateBack();
					}, 1500);
				}
			} catch (error) {
				console.error('【碎片化学习】加载题目失败：', error);
				uni.showToast({
					title: '加载失败，请重试',
					icon: 'none'
				});

				// 延迟返回
				setTimeout(() => {
					uni.navigateBack();
				}, 1500);
			} finally {
				uni.hideLoading();
				this.loading = false;
			}
		},

		goBack() {
			uni.navigateBack();
		},

		selectAnswer(index) {
			if (this.selectedAnswer !== null) {
				return;
			}
			this.selectedAnswer = index;
			this.answerRecords[this.currentIndex] = index;
		},

		getOptionLabel(index) {
			return String.fromCharCode(65 + index);
		},

		prevQuestion() {
			if (this.currentIndex > 0) {
				this.currentIndex--;
			}
		},

		nextQuestion() {
			if (this.currentIndex < this.questions.length - 1) {
				this.currentIndex++;
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

.body {
	padding: 20rpx;
	padding-top: 40rpx;
}

.question-type {
	display: inline-block;
}

.question-text {
	display: block;
	line-height: 1.8;
}

.options-list {
	display: flex;
	flex-direction: column;
	gap: 15rpx;
}

.option-item:active:not(.disabled) {
	transform: scale(0.98);
	opacity: 0.9;
}

.option-item.disabled {
	cursor: not-allowed;
}

.answer-tip {
	display: flex;
	align-items: center;
	border-top: 1rpx solid rgba(255, 255, 255, 0.3);
	min-height: 56rpx;
}

.pagination {
	padding: 15rpx 0;
	gap: 15rpx;
}

.page-btn {
	display: inline-flex;
	align-items: center;
	justify-content: center;
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
}

.page-info {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	height: 56rpx;
	padding: 0 32rpx;
}
</style>