<template>
	<view class="content bg-505ef2">
		<view class="header bg-ff border-b-ee" :style="'padding-top:' + $statusBarHeight + 'px;'">
			<view class="header-box dis-flex-hsb dis-flex-vc">
				<view class="header-back dis-flex-hv w-60" @click="goBack">
					<i class="gjjd-iconfont icon-xiangzuo font-s20 color-33"></i>
				</view>
				<view class="header-title font-s18 font-wb color-33 text-C">大冒险-{{ levelName }}</view>
				<view class="header-placeholder w-60"></view>
			</view>
		</view>

		<view class="body" :style="'padding-top:' + (50 + $statusBarHeight + 60) + 'px;'">
			<view v-if="questions.length > 0" class="question-card bg-ff radius-16 shadow-sm">
				<view class="question-header">
					<text class="question-number font-s16 color-33">{{ currentQuestionIndex + 1 }} /
						{{ questions.length }}</text>
					<text class="question-type font-s14 color-00b6ff">{{ currentQuestion.questionTypeText }}</text>
				</view>

				<view class="question-content">
					<text class="question-text font-s16 color-33">{{ currentQuestion.question }}</text>
				</view>

				<view class="options-list">
					<view v-for="(option, index) in currentQuestion.options" :key="index"
						class="option-item radius-36 transition-all"
						:class="selectedAnswer === index ? 'bg-505ef2' : 'bg-f5'" @click="selectAnswer(index)">
						<text class="font-s15" :class="selectedAnswer === index ? 'color-ff' : 'color-33'">
							{{ option }}
						</text>
					</view>
				</view>

				<view class="next-btn-container">
					<view class="next-btn bg-00b6ff radius-32 dis-flex-hv"
						:class="selectedAnswer === null ? 'disabled' : ''" @click="nextQuestion">
						<text class="font-s16 font-wb color-ff">{{ isLastQuestion ? '提交答案' : '下一题' }}</text>
					</view>
				</view>
			</view>
		</view>

		<view class="pass-modal-mask dis-flex-hv mask-dark" v-if="showPassModal">
			<view class="pass-modal">
				<view class="pass-content bg-ff radius-20 shadow-xl">
					<view class="pass-icon">
						<view class="w-120 h-120 dis-flex-hv bg-00b6ff radius-full shadow-blue-strong">
							<text class="font-s80 font-wb color-ff">✓</text>
						</view>
					</view>
					<text class="font-s20 font-wb color-33 mb-5">恭喜过关！</text>
					<text class="font-s15 color-66 opacity-80">即将进入下一关</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import { getBigAdventureQuestions, submitBigAdventureAnswers } from "@/api/competition";
import { useUserStore } from '@/store/user';
import { useCompetitionStore } from '@/store/competition';
import { isSuccess, showError } from '@/utils/response';

export default {
	setup() {
		const userStore = useUserStore()
		const competitionStore = useCompetitionStore()
		return {
			userStore,
			competitionStore
		}
	},
	data() {
		return {
			checkpointId: null,
			levelName: '',
			currentQuestionIndex: 0,
			selectedAnswer: null,
			userAnswers: [],
			showPassModal: false,
			questions: []
		}
	},
	computed: {
		currentQuestion() {
			return this.questions[this.currentQuestionIndex] || {};
		},
		isLastQuestion() {
			return this.currentQuestionIndex === this.questions.length - 1;
		}
	},
	onLoad(options) {
		// 检查竞赛ID
		if (!this.competitionStore.checkMatchId()) {
			return;
		}

		// 解析 URL 参数
		if (options.matchId) {
			this.matchId = parseInt(options.matchId);
		}
		if (options.checkpointId) {
			this.checkpointId = parseInt(options.checkpointId);
		}
		if (options.title) {
			this.levelName = options.title;
		}

		this.loadQuestions();
	},
	methods: {
		goBack() {
			if (this.showPassModal) {
				uni.navigateBack();
			} else {
				uni.showModal({
					title: '提示',
					content: '确定要退出答题吗？',
					success: (res) => {
						if (res.confirm) {
							uni.navigateBack();
						}
					}
				});
			}
		},

		async loadQuestions() {
			try {
				uni.showLoading({ title: '加载题目...' });

				if (!this.userStore.checkLogin()) {
					uni.hideLoading();
					return;
				}

				const res = await getBigAdventureQuestions({
					matchId: this.competitionStore.currentMatchId,
					userId: this.userStore.userId,
					checkpointId: this.checkpointId
				});

				uni.hideLoading();

				if (res.code !== 1 || !res.data?.questions?.length) {
					showError(res, '没有符合条件的题目');
					setTimeout(() => uni.navigateBack(), 1500);
					return;
				}

				this.questions = res.data.questions.map(q => ({
					id: q.id,
					type: q.type === 'judge' ? 'judge' : 'choice',
					question: q.content,
					options: q.options,
					difficulty: q.difficulty
				}));

			} catch (error) {
				console.error('加载题目失败:', error);
				uni.hideLoading();
				uni.showToast({ title: '加载失败，请重试', icon: 'none' });
				setTimeout(() => uni.navigateBack(), 1500);
			}
		},

		selectAnswer(index) {
			this.selectedAnswer = index;
		},

		nextQuestion() {
			if (this.selectedAnswer === null) {
				uni.showToast({ title: '请选择答案', icon: 'none' });
				return;
			}

			this.userAnswers.push({
				questionId: this.currentQuestion.id,
				answer: this.selectedAnswer
			});

			if (this.isLastQuestion) {
				this.submitAnswers();
			} else {
				this.currentQuestionIndex++;
				this.selectedAnswer = null;
			}
		},

		async submitAnswers() {
			try {
				uni.showLoading({ title: '提交中...' });

				if (!this.userStore.checkLogin()) {
					uni.hideLoading();
					return;
				}

				const res = await submitBigAdventureAnswers(
					{
						checkpointId: this.checkpointId,
						answers: this.userAnswers,
						userId: this.userStore.userId,
						matchId: this.competitionStore.currentMatchId

					});

				uni.hideLoading();

				if (isSuccess(res) && res.data) {
					this.showResult(res.data);
				} else {
					showError(res, '提交失败');
				}
			} catch (error) {
				console.error('提交失败:', error);
				uni.hideLoading();
				uni.showToast({ title: '提交失败，请重试', icon: 'none' });
			}
		},

		showResult(data) {
			const { isPassed, correctNum = 0, totalNum = this.questions.length, getPoint = 0, correctRate } = data;

			if (isPassed) {
				this.showPassModal = true;

				setTimeout(() => {
					this.showPassModal = false;
					uni.showModal({
						title: '恭喜通过！',
						content: `答对 ${correctNum}/${totalNum} 题\n获得积分：${getPoint > 0 ? '+' : ''}${getPoint}`,
						showCancel: false,
						success: () => uni.navigateBack()
					});
				}, 2000);
			} else {
				const rate = correctRate !== undefined ? correctRate.toFixed(0) : ((correctNum / totalNum * 100) || 0).toFixed(0);
				const passRateValue = data.passRate || 60;
				const pointText = getPoint < 0 ? `\n扣除积分：${Math.abs(getPoint)}` : '';
				uni.showModal({
					title: '未通过',
					content: `答对 ${correctNum}/${totalNum} 题（正确率${rate}%）\n需要达到${passRateValue}%才能通过${pointText}`,
					showCancel: false,
					success: () => uni.navigateBack()
				});
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
}

.header-box {
	height: 50px;
	padding: 0 20rpx;
}

.header-title {
	flex: 1;
}

.body {
	padding: 0 20rpx 20rpx 20rpx;
}

.question-card {
	padding: 40rpx 30rpx;
}

.question-header {
	display: flex;
	flex-direction: column;
	align-items: flex-start;
	gap: 10rpx;
	margin-bottom: 10rpx;
}

.question-content {
	margin-bottom: 30rpx;
}

.question-text {
	line-height: 1.8;
	word-wrap: break-word;
}

.options-list {
	margin-bottom: 40rpx;
}

.option-item {
	padding: 25rpx 30rpx;
	margin-bottom: 20rpx;
	transition: all 0.3s;
	line-height: 1.6;
	word-wrap: break-word;
}

.option-item:active {
	transform: scale(0.98);
}

.next-btn-container {
	display: flex;
	justify-content: center;
}

.next-btn {
	width: 100%;
	padding: 28rpx 0;
	transition: all 0.3s;
}

.next-btn.disabled {
	opacity: 0.5;
}

.next-btn:active {
	transform: scale(0.98);
}

.pass-modal-mask {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	z-index: 10000;
	animation: maskFadeIn 0.3s;
}

@keyframes maskFadeIn {
	from {
		opacity: 0;
	}

	to {
		opacity: 1;
	}
}

.pass-modal {
	animation: modalBounce 0.5s cubic-bezier(0.68, -0.55, 0.265, 1.55);
}

@keyframes modalBounce {
	0% {
		opacity: 0;
		transform: scale(0.3);
	}

	50% {
		transform: scale(1.05);
	}

	100% {
		opacity: 1;
		transform: scale(1);
	}
}

.pass-content {
	width: 500rpx;
	padding: 60rpx 40rpx 50rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 20rpx;
}

.pass-icon {
	margin-bottom: 10rpx;
	animation: iconRotate 0.6s ease-in-out;
}

@keyframes iconRotate {
	0% {
		transform: rotate(0deg) scale(0);
	}

	50% {
		transform: rotate(180deg) scale(1.2);
	}

	100% {
		transform: rotate(360deg) scale(1);
	}
}
</style>