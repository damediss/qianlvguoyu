<template>
	<view class="content bg-f8">
		<view v-if="showResult" class="result-page">
			<view class="header bg-ff border-b-ee shadow-sm" :style="'padding-top:' + $statusBarHeight + 'px;'">
				<view class="header-box dis-flex-vc dis-flex-hsb">
					<view class="header-back dis-flex-vc" @click="goBack">
						<i class="gjjd-iconfont icon-xiangzuo font-s20 color-33"></i>
					</view>
					<view class="header-title font-s18 font-wb color-33 text-C">模拟考试</view>
					<view class="header-placeholder w-60"></view>
				</view>
			</view>

			<view class="result-content dis-flex flex-column dis-flex-hv"
				:style="'padding-top:' + (50 + $statusBarHeight) + 'px;'">
				<view class="result-stats dis-flex dis-flex-ha w-full-percent">
					<text class="font-s16 color-33 text-C">得分:{{ score }}分</text>
					<text class="font-s16 color-33 text-C">答对:{{ correctCount }}题</text>
					<text class="font-s16 color-33 text-C">答错:{{ wrongCount }}题</text>
					<text class="font-s16 color-33 text-C">未答:{{ unansweredCount }}题</text>
					<text v-if="points > 0" class="font-s16 color-00b6ff text-C">积分:+{{ points }}</text>
				</view>

				<view class="success-btn bg-33 radius-8 dis-flex-hv transition-fast" @click="goBack">
					<text class="font-s16 color-ff">成功</text>
				</view>
			</view>
		</view>

		<!-- 空状态 -->
		<view v-else-if="pageStatus === 'empty'" class="empty-page">
			<view class="header bg-ff border-b-ee shadow-sm" :style="'padding-top:' + $statusBarHeight + 'px;'">
				<view class="header-box dis-flex-vc dis-flex-hsb">
					<view class="header-back dis-flex-vc" @click="goBack">
						<i class="gjjd-iconfont icon-xiangzuo font-s20 color-33"></i>
					</view>
					<view class="header-title font-s18 font-wb color-33 text-C">模拟考试</view>
					<view class="header-placeholder w-60"></view>
				</view>
			</view>
			<view class="empty-state dis-flex-hv flex-column" :style="'padding-top:' + (50 + $statusBarHeight) + 'px;'">
				<text class="empty-text font-s15 color-99">{{ emptyMessage }}</text>
			</view>
		</view>

		<!-- 加载中 -->
		<view v-else-if="pageStatus === 'loading'" class="loading-page">
			<view class="header bg-ff border-b-ee shadow-sm" :style="'padding-top:' + $statusBarHeight + 'px;'">
				<view class="header-box dis-flex-vc dis-flex-hsb">
					<view class="header-back dis-flex-vc" @click="goBack">
						<i class="gjjd-iconfont icon-xiangzuo font-s20 color-33"></i>
					</view>
					<view class="header-title font-s18 font-wb color-33 text-C">模拟考试</view>
					<view class="header-placeholder w-60"></view>
				</view>
			</view>
			<view class="loading-content dis-flex flex-column dis-flex-hv"
				:style="'padding-top:' + (50 + $statusBarHeight) + 'px;'">
				<text class="font-s16 color-99">加载中...</text>
			</view>
		</view>

		<!-- 考试界面 -->
		<view v-else-if="pageStatus === 'ready'">
			<view class="header bg-ff border-b-ee shadow-sm" :style="'padding-top:' + $statusBarHeight + 'px;'">
				<view class="header-box dis-flex-vc dis-flex-hsb">
					<view class="header-back dis-flex-vc" @click="goBack">
						<i class="gjjd-iconfont icon-xiangzuo font-s20 color-33"></i>
					</view>
					<view class="header-title font-s18 font-wb color-33 text-C">模拟考试</view>
					<view class="header-placeholder w-60"></view>
				</view>
			</view>

			<view class="timer-bar" :style="timerBarStyle">
				<view class="timer-box dis-flex-vc">
					<text class="timer-text font-s14 color-33">考试剩余时间:{{ formattedTime }}</text>
					<text v-if="autoSubmitWarning" class="timer-warning font-s14 color-red">将在{{ timeRemaining
					}}秒后自动交卷</text>
				</view>
			</view>

			<view class="body" :style="bodyStyle">
				<view class="question-card bg-00b6ff radius-16">
					<view class="question-type">
						<text class="font-s14 font-wb color-ff">{{ currentQuestion.type === 'judge' ? '判断题' :
							'单选题' }}</text>
					</view>

					<view class="question-content">
						<text class="question-text font-s15 color-ff">{{ currentQuestion.question }}</text>

						<view class="options-list dis-flex flex-column">
							<view v-for="(option, index) in currentQuestion.options" :key="index"
								class="option-item dis-flex-vc radius-8 transition-all"
								:class="{ 'selected bg-0a3c88': userAnswers[currentIndex] === index, 'bg-ff': userAnswers[currentIndex] !== index }"
								@click="selectAnswer(index)">
								<text v-if="userAnswers[currentIndex] === index"
									class="check-icon color-ff font-s16">✓</text>
								<text class="option-text font-s15"
									:class="userAnswers[currentIndex] === index ? 'color-ff' : 'color-33'">{{ option
									}}</text>
							</view>
						</view>
					</view>

					<view class="pagination dis-flex-hsb dis-flex-vc">
						<view class="page-btn bg-99 radius-8 transition-all" :class="{ 'disabled': currentIndex === 0 }"
							@click="prevQuestion">
							<text class="font-s14 color-ff">上一题</text>
						</view>

						<view class="page-info bg-ff radius-8">
							<text class="font-s14 color-33">{{ currentIndex + 1 }} / {{ questions.length }}</text>
						</view>

						<view class="page-btn bg-ff radius-8 transition-all"
							:class="{ 'disabled': currentIndex === questions.length - 1 }" @click="nextQuestion">
							<text class="font-s14 color-33">下一题</text>
						</view>
					</view>
				</view>

				<view class="action-buttons dis-flex flex-column">
					<view class="action-btn bg-0a3c88 radius-24 transition-all" @click="showQuestionSelector">
						<text class="font-s16 font-wb color-ff">选题</text>
					</view>
					<view class="action-btn bg-f9d52b radius-24 transition-all" @click="confirmSubmit">
						<text class="font-s16 font-wb color-33">交卷</text>
					</view>
				</view>
			</view>

			<view class="selector-mask dis-flex mask-dark" style="position:fixed;top:0;left:0;right:0;bottom:0;"
				v-if="showSelector" @click="closeSelector">
				<view class="selector-panel bg-ff radius-top-20" @click.stop>
					<view class="dis-flex-hsb dis-flex-vc mar-B20">
						<text class="font-s16 font-wb color-33">单选题</text>
						<view class="close-btn" @click="closeSelector">
							<text class="font-s16 color-red">关闭</text>
						</view>
					</view>

					<view class="question-grid dis-flex-vc">
						<view v-for="(item, index) in singleChoiceQuestions" :key="'single-' + index"
							class="grid-item bg-ff radius-8 border-e0 transition-all" :class="{
								'current bg-00b6ff border-00b6ff': currentIndex === index,
								'answered border-00b6ff': userAnswers[index] !== null
							}" @click="jumpToQuestion(index)">
							<text class="font-s14" :class="currentIndex === index ? 'color-ff' : 'color-33'">第{{ index +
								1 }}题</text>
						</view>
					</view>

					<view class="dis-flex-hsb dis-flex-vc mar-T30 mar-B20">
						<text class="font-s16 font-wb color-33">判断题</text>
					</view>

					<view class="question-grid dis-flex-vc">
						<view v-for="(item, index) in judgeQuestions" :key="'judge-' + index"
							class="grid-item bg-ff radius-8 border-e0 transition-all" :class="{
								'current bg-00b6ff border-00b6ff': currentIndex === singleChoiceCount + index,
								'answered border-00b6ff': userAnswers[singleChoiceCount + index] !== null
							}" @click="jumpToQuestion(singleChoiceCount + index)">
							<text class="font-s14"
								:class="currentIndex === singleChoiceCount + index ? 'color-ff' : 'color-33'">第{{
									singleChoiceCount
									+ index + 1 }}题</text>
						</view>
					</view>
				</view>
			</view>

			<view class="modal-mask dis-flex-hv mask-dark" style="position:fixed;top:0;left:0;right:0;bottom:0;"
				v-if="showSubmitModal" @click="closeSubmitModal">
				<view class="modal-content bg-ff radius-16" @click.stop>
					<view class="text-C font-s18 font-wb color-33 mar-B20">友情提示</view>
					<view class="modal-body">
						<text class="font-s15 color-33">还有{{ unansweredCount }}题没答，确定要交卷吗？</text>
					</view>
					<view class="dis-flex mar-T30" style="justify-content:space-around;">
						<view class="modal-btn" @click="closeSubmitModal">
							<text class="font-s16 color-66">取消</text>
						</view>
						<view class="modal-btn" @click="submitExam">
							<text class="font-s16 color-00b6ff">确定</text>
						</view>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import { getMockExamQuestions, submitMockExam } from '@/api/competition';
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
			paperId: null,
			startTime: null,
			currentIndex: 0,
			userAnswers: [],
			timeRemaining: 1800,
			timer: null,
			showSelector: false,
			showSubmitModal: false,
			showResult: false,
			score: 0,
			correctCount: 0,
			wrongCount: 0,
			unansweredCount: 0,
			points: 0,
			questions: [],
			// 新增：页面状态
			pageStatus: 'loading', // loading | empty | ready
			emptyMessage: '暂未配置模拟考试'
		}
	},
	computed: {
		currentQuestion() {
			return this.questions[this.currentIndex] || {};
		},
		timerBarStyle() {
			const headerPx = uni.upx2px(100);
			return `top:${this.$statusBarHeight + headerPx}px;`;
		},
		bodyStyle() {
			const headerPx = uni.upx2px(100);
			const timerPx = uni.upx2px(60);
			const extraGap = 6;
			return `padding-top:${this.$statusBarHeight + headerPx + timerPx + extraGap}px;`;
		},
		formattedTime() {
			// 格式化时间，显示小时、分钟、秒
			const hours = Math.floor(this.timeRemaining / 3600);
			const minutes = Math.floor((this.timeRemaining % 3600) / 60);
			const seconds = this.timeRemaining % 60;
			return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
		},
		autoSubmitWarning() {
			// 剩余时间 <= 5分钟（300秒）时显示警告
			return this.timeRemaining <= 300;
		},
		singleChoiceQuestions() {
			return this.questions.filter(q => q.type === 'single');
		},
		judgeQuestions() {
			return this.questions.filter(q => q.type === 'judge');
		},
		singleChoiceCount() {
			return this.singleChoiceQuestions.length;
		}
	},
	onLoad(options) {
		if (!this.competitionStore.checkMatchId()) return;

		this.initQuestions();
		this.startTimer();
	},
	onUnload() {
		this.stopTimer();
	},
	methods: {
		goBack() {
			// 空状态或结果页可以直接返回
			if (this.showResult || this.pageStatus === 'empty') {
				uni.navigateBack();
			} else if (this.pageStatus === 'ready') {
				uni.showModal({
					title: '友情提示',
					content: '考试中不能退出',
					showCancel: true,
					confirmText: '确定',
					cancelText: '取消'
				});
			} else {
				uni.navigateBack();
			}
		},

		async initQuestions() {
			try {
				this.pageStatus = 'loading';

				// 只传matchId，后端从配置读取试卷ID
				const res = await getMockExamQuestions(this.competitionStore.currentMatchId);

				if (isSuccess(res) && res.data && res.data.questions && res.data.questions.length > 0) {
					this.questions = res.data.questions;
					this.userAnswers = new Array(this.questions.length).fill(null);
					this.timeRemaining = res.data.duration || 1800;
					this.unansweredCount = res.data.totalCount || this.questions.length;
					this.startTime = res.data.startTime || Date.now();
					this.paperId = res.data.paperId;
					this.pageStatus = 'ready';
				} else {
					// 没有配置或没有题目，显示空状态
					this.emptyMessage = res.msg || '暂未配置模拟考试';
					this.pageStatus = 'empty';
					this.stopTimer();
				}
			} catch (error) {
				console.error('加载题目失败:', error);
				this.emptyMessage = error.message || '加载失败，请稍后重试';
				this.pageStatus = 'empty';
				this.stopTimer();
			}
		},

		startTimer() {
			// 每1秒更新一次倒计时
			this.timer = setInterval(() => {
				this.timeRemaining--;
				if (this.timeRemaining <= 0) {
					this.stopTimer();
					this.autoSubmitExam();
				}
			}, 1000);
		},

		stopTimer() {
			if (this.timer) {
				clearInterval(this.timer);
				this.timer = null;
			}
		},

		selectAnswer(index) {
			this.userAnswers[this.currentIndex] = index;
			this.unansweredCount = this.userAnswers.filter(answer => answer === null).length;
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
		},

		showQuestionSelector() {
			this.showSelector = true;
		},

		closeSelector() {
			this.showSelector = false;
		},

		jumpToQuestion(index) {
			this.currentIndex = index;
			this.closeSelector();
		},

		confirmSubmit() {
			this.unansweredCount = this.userAnswers.filter(answer => answer === null).length;
			this.showSubmitModal = true;
		},

		closeSubmitModal() {
			this.showSubmitModal = false;
		},

		async submitExam() {
			try {
				this.stopTimer();
				this.closeSubmitModal();

				uni.showLoading({ title: '提交中...' });

				const answers = this.questions.map((q, index) => ({
					questionId: q.id,
					userAnswer: this.userAnswers[index]
				}));

				const currentTime = Date.now();
				const elapsedSeconds = Math.floor((currentTime - this.startTime) / 1000);

				const res = await submitMockExam({
					paperId: this.paperId,
					userId: this.userStore.userId,
					matchId: this.competitionStore.currentMatchId,
					startTime: this.startTime,
					elapsedSeconds: elapsedSeconds,
					answers: answers
				});

				uni.hideLoading();

				if (isSuccess(res) && res.data) {
					this.score = res.data.score;
					this.correctCount = res.data.correctCount;
					this.wrongCount = res.data.wrongCount;
					this.unansweredCount = res.data.unansweredCount;
					this.points = res.data.points || 0;
					this.showResult = true;

					if (res.data.points) {
						setTimeout(() => {
							uni.showToast({
								title: `获得${res.data.points}积分`,
								icon: 'success'
							});
						}, 500);
					}
				} else {
					showError(res, '提交失败');
				}

			} catch (error) {
				console.error('提交失败:', error);
				uni.hideLoading();
				uni.showToast({ title: '提交失败', icon: 'none' });
			}
		},

		async autoSubmitExam() {
			await this.submitExam();
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
	height: 100rpx;
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
	padding: 25rpx;
	z-index: 9998;
	background-color: transparent;
	font-weight: bold;
}

.timer-box {
	padding: 5rpx 0;
	gap: 15rpx;
	flex-wrap: wrap;
}

.timer-warning {
	margin-left: 10rpx;
}

.body {
	padding: 20rpx;
	padding-bottom: 30rpx;
}

.question-card {
	padding: 20rpx;
	margin-top: 5rpx;
}

.question-type {
	display: inline-block;
	padding: 0 0 15rpx 0;
}

.question-content {
	padding: 0;
}

.question-text {
	display: block;
	line-height: 1.8;
	margin-bottom: 30rpx;
}

.options-list {
	gap: 15rpx;
	margin-bottom: 20rpx;
}

.option-item {
	padding: 20rpx;
	gap: 10rpx;
	align-items: center;
}

.option-item.transition-all:active {
	transform: scale(0.98);
}

.check-icon {
	font-weight: bold;
	flex-shrink: 0;
	line-height: 1;
	display: flex;
	align-items: center;
}

.option-text {
	line-height: 1.5;
	flex: 1;
}

.pagination {
	padding: 16rpx 0 0;
	gap: 15rpx;
}

.page-btn {
	flex: 1;
	display: inline-flex;
	align-items: center;
	justify-content: center;
	height: 56rpx;
	padding: 0 32rpx;
	text-align: center;
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
	min-width: 140rpx;
	display: inline-flex;
	align-items: center;
	justify-content: center;
	height: 56rpx;
	padding: 0 32rpx;
	text-align: center;
}

.action-buttons {
	margin-top: 30rpx;
	gap: 10rpx;
}

.action-btn {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	height: 64rpx;
	padding: 0 28rpx;
	text-align: center;
}

.action-btn.transition-all:active {
	transform: scale(0.95);
	opacity: 0.9;
}

.selector-mask {
	z-index: 10000;
	align-items: flex-start;
	padding-top: 100rpx;
}

.selector-panel {
	width: 100%;
	max-height: 80vh;
	overflow-y: auto;
	padding: 30rpx;
}

.close-btn {
	padding: 10rpx 20rpx;
}

.question-grid {
	display: grid;
	grid-template-columns: repeat(4, 1fr);
	gap: 15rpx;
}

.grid-item {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	min-height: 88rpx;
	padding: 0 20rpx;
	text-align: center;
}

.grid-item.transition-all:active {
	transform: scale(0.95);
}

.modal-mask {
	z-index: 10001;
}

.modal-content {
	width: 80%;
	padding: 30rpx;
}

.modal-body {
	text-align: center;
	padding: 20rpx 0;
	line-height: 1.8;
}

.modal-btn {
	padding: 15rpx 50rpx;
}

.result-page {
	min-height: 100vh;
}

.result-content {
	padding: 40rpx 20rpx;
	min-height: 60vh;
}

.result-stats {
	margin-bottom: 100rpx;
	flex-wrap: wrap;
	gap: 20rpx;
}

.success-btn {
	padding: 20rpx 80rpx;
}

.success-btn:active {
	transform: scale(0.95);
	opacity: 0.9;
}

/* 空状态样式 */
.empty-page,
.loading-page {
	min-height: 100vh;
}

.empty-state,
.loading-content {
	min-height: 60vh;
}
</style>