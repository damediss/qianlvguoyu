<template>
	<view class="content bg-blue">
		<view class="header bg-ff border-b-ee" :style="'padding-top:' + $statusBarHeight + 'px;'">
			<view class="header-box dis-flex-hsb dis-flex-vc">
				<view class="header-back dis-flex-hv w-60" @click="goBack">
					<i class="gjjd-iconfont icon-xiangzuo font-s20 color-33"></i>
				</view>
				<view class="header-title font-s18 font-wb color-33 text-C">答题闯关-{{ levelName }}</view>
				<view class="header-placeholder w-60"></view>
			</view>
		</view>

		<view class="body" :style="'padding-top:' + (50 + $statusBarHeight + 50) + 'px;'">
			<view v-if="questions.length > 0" class="question-card bg-ff radius-16 shadow-md">
				<view class="question-number font-s16 color-33">
					{{ currentQuestionIndex + 1 }} / {{ questions.length }}
				</view>

				<view class="question-type font-s14 color-00b6ff">{{ currentQuestion.type === 'judge' ? '判断题' :
					'单选题' }}(分)答对获得1分，答错扣1分</view>

				<view class="question-text font-s16 color-33">
					{{ currentQuestion.question }}
				</view>

				<view class="options-list">
					<view v-for="(option, index) in currentQuestion.options" :key="index"
						class="option-item bg-f8 radius-12"
						:class="{ 'selected bg-505ef2 border-505ef2': selectedAnswer === index }"
						@click="selectAnswer(index)">
						<text class="option-text font-s15"
							:class="selectedAnswer === index ? 'color-ff' : 'color-33'">{{ option }}</text>
					</view>
				</view>

				<view class="next-btn-container">
					<view class="next-btn dis-flex-hv w-full bg-00b6ff radius-32 shadow-blue" @click="nextQuestion">
						<text class="font-s16 font-wb color-ff">{{ isLastQuestion ? '提交' : '下一题' }}</text>
					</view>
				</view>
			</view>

			<view class="pass-modal-mask dis-flex-hv mask-dark" v-if="showPassModal">
				<view class="pass-modal">
					<view class="pass-content bg-ff radius-20 shadow-xl">
						<view class="pass-icon">
							<view class="success-circle dis-flex-hv bg-00b6ff radius-full shadow-blue-strong">
								<text class="success-mark color-ff">✓</text>
							</view>
						</view>
						<text class="pass-title font-s20 font-wb color-33">恭喜过关！</text>
						<text class="pass-text font-s15 color-66">即将进入下一关</text>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import { getAdventureQuestions, submitAdventureAnswers } from "@/api/competition";
import { useUserStore } from '@/store/user';
import { useCompetitionStore } from '@/store/competition';

export default {
	setup() {
		const userStore = useUserStore()
		const competitionStore = useCompetitionStore()
		return { userStore, competitionStore }
	},
	data() {
		return {
			matchId: null,
			checkpointId: null,
			levelName: '一',
			currentQuestionIndex: 0,
			selectedAnswer: null,
			userAnswers: [],
			showPassModal: false,
			showResultModal: false,
			resultData: null,
			questions: []
		}
	},
	computed: {
		currentQuestion() {
			if (!this.questions || this.questions.length === 0) {
				return {
					id: 0,
					type: 'judge',
					question: '加载中...',
					options: ['A、对', 'B、错']
				};
			}
			return this.questions[this.currentQuestionIndex] || {};
		},
		isLastQuestion() {
			return this.currentQuestionIndex === this.questions.length - 1;
		}
	},
	onLoad(options) {
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
			uni.showModal({
				title: '提示',
				content: '确定要退出答题吗？',
				success: (res) => {
					if (res.confirm) {
						uni.navigateBack();
					}
				}
			});
		},

		async loadQuestions() {
			if (!this.userStore.checkLogin()) {
				return;
			}

			try {
				uni.showLoading({ title: '加载题目...' });

				const res = await getAdventureQuestions({
					matchId: this.matchId,
					userId: this.userStore.userId,
					checkpointId: this.checkpointId
				});

				uni.hideLoading();

				if (res.code === 1 && res.data && res.data.questions && res.data.questions.length > 0) {
					this.questions = res.data.questions.map(q => ({
						id: q.id,
						type: q.type === 'judge' ? 'judge' : 'choice',
						question: q.content,
						options: q.options,
						difficulty: q.difficulty
					}));
				} else {
					uni.showToast({ title: res.msg || '暂无题目', icon: 'none' });
					setTimeout(() => uni.navigateBack(), 1500);
				}
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
				uni.showToast({
					title: '请先选择答案',
					icon: 'none'
				});
				return;
			}

			const question = this.currentQuestion;

			this.userAnswers.push({
				questionId: question.id,
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

				const res = await submitAdventureAnswers(
					{
						checkpointId: this.checkpointId,
						answers: this.userAnswers,
						userId: this.userStore.userId,
						matchId: this.matchId
					},
				);

				uni.hideLoading();

				if (res.code === 1 && res.data) {
					this.resultData = res.data;
					this.showResult(res.data);
				} else {
					const errorMsg = res.msg || '提交失败';

					if (errorMsg.includes('次数已用完') || errorMsg.includes('已参加')) {
						uni.showModal({
							title: '温馨提示',
							content: errorMsg,
							showCancel: false,
							success: () => uni.navigateBack()
						});
					} else {
						uni.showToast({ title: errorMsg, icon: 'none' });
					}
				}
			} catch (error) {
				console.error('提交答案失败:', error);
				uni.hideLoading();
				uni.showToast({ title: '提交失败，请重试', icon: 'none' });
			}
		},

		goBack() {
			uni.showModal({
				title: '提示',
				content: '确定要退出答题吗？',
				success: (res) => {
					if (res.confirm) {
						uni.navigateBack({
							delta: 1
						});
					}
				}
			});
		},

		showResult(data) {
			const isPassed = data.isPassed;
			const correctCount = data.correctNum || 0;
			const totalCount = data.totalNum || this.questions.length;
			const point = data.getPoint || 0;

			if (isPassed) {
				this.showPassModal = true;

				setTimeout(() => {
					this.showPassModal = false;
					uni.showModal({
						title: '恭喜通过！',
						content: `答对 ${correctCount}/${totalCount} 题\n获得积分：${point > 0 ? '+' : ''}${point}`,
						showCancel: false,
						success: () => {
							uni.navigateBack();
						}
					});
				}, 2000);
			} else {
				const rate = data.correctRate !== undefined
					? data.correctRate.toFixed(0)
					: (data.accuracy || '0%').replace('%', '');
				const passRate = data.passRate || 60;
				uni.showModal({
					title: '未通过',
					content: `答对 ${correctCount}/${totalCount} 题（正确率${rate}%）\n需要达到${passRate}%才能通过\n${point < 0 ? '扣除积分：' + Math.abs(point) : ''}`,
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
	padding-bottom: 30rpx;
}

.bg-blue {
	background: linear-gradient(180deg, #0066ff 0%, #0099ff 100%);
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
	padding: 20rpx;
}

.question-card {
	padding: 30rpx 25rpx;
}

.question-number {
	margin-bottom: 10rpx;
}

.question-type {
	margin-bottom: 15rpx;
}

.question-text {
	margin-bottom: 25rpx;
	line-height: 1.6;
}

.options-list {
	margin-bottom: 30rpx;
}

.option-item {
	padding: 20rpx 18rpx;
	margin-bottom: 12rpx;
	cursor: pointer;
	transition: all 0.3s;
}

.option-item:active {
	transform: scale(0.98);
}

.option-text {
	line-height: 1.6;
}

.next-btn-container {
	margin-top: 10rpx;
}

.next-btn {
	height: 90rpx;
	cursor: pointer;
	transition: all 0.3s;
}

.next-btn:active {
	transform: scale(0.98);
	opacity: 0.8;
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

.success-circle {
	width: 120rpx;
	height: 120rpx;
}

.success-mark {
	font-size: 80rpx;
	font-weight: bold;
	line-height: 1;
}

.pass-title {
	margin-bottom: 5rpx;
}

.pass-text {
	opacity: 0.8;
}
</style>
