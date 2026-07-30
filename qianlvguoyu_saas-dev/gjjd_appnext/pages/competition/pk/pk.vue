<template>
	<view class="content bg-blue">
		<view class="header bg-ff" :style="'padding-top:' + $statusBarHeight + 'px;'">
			<view class="header-box dis-flex-hsb dis-flex-vc">
				<view class="header-back dis-flex-hv w-60" @click="goBack">
					<i class="gjjd-iconfont icon-xiangzuo font-s20 color-33"></i>
				</view>
				<view class="header-title font-s18 font-wb color-33 text-C">个人竞技</view>
				<view class="header-placeholder w-60"></view>
			</view>
		</view>

		<view class="body" :style="'margin-top:' + (50 + $statusBarHeight) + 'px;'">
			<view v-if="!showResultPage && questions.length > 0" class="question-card bg-ff radius-16 shadow-md">
				<view class="question-number font-s16 color-33">
					{{ currentQuestionIndex + 1 }} / {{ questions.length }}
				</view>

				<view class="question-type font-s14 color-00b6ff">{{ questionTypeText }}</view>

				<view class="question-text font-s16 color-33">
					<uv-parse :content="currentQuestion.question"></uv-parse>
				</view>

				<view class="options-list">
					<template v-for="(option, index) in currentQuestion.options" :key="index">
						<view v-if="currentQuestion.type=='multi'"
							class="option-item bg-f8 radius-36"
							:class="{ 'selected bg-505ef2': selectedAnswerMulti.includes(index), 'correct bg-e8f5e9': showAnswer && index === currentQuestion.answer, 'wrong bg-ffebee': showAnswer && selectedAnswerMulti.includes(index) && index !== currentQuestion.answer }"
							@click="selectAnswer(index)">
							<text class="option-text font-s15"
								:class="selectedAnswerMulti.includes(index) ? 'color-ff' : 'color-33'">{{ option }}</text>
						</view>
						<view v-else
							class="option-item bg-f8 radius-36"
							:class="{ 'selected bg-505ef2': selectedAnswer === index, 'correct bg-e8f5e9': showAnswer && index === currentQuestion.answer, 'wrong bg-ffebee': showAnswer && selectedAnswer === index && index !== currentQuestion.answer }"
							@click="selectAnswer(index)">
							<text class="option-text font-s15"
								:class="selectedAnswer === index ? 'color-ff' : 'color-33'">{{ option }}</text>
						</view>
					</template>
				</view>

				<!-- 下一题按钮 -->
				<view class="next-btn-container">
					<view class="next-btn dis-flex-hv w-full bg-00b6ff radius-32" @click="nextQuestion">
						<text class="font-s16 font-wb color-ff">{{ isLastQuestion ? '提交' : '下一题' }}</text>
					</view>
				</view>
			</view>

			<!-- 结果页面 -->
			<view v-if="showResultPage" class="result-page">
				<view class="result-icon">
					<view class="icon-circle dis-flex-hv bg-f5 radius-full shadow-md">
						<i class="gjjd-iconfont icon-info font-s48 color-99"></i>
					</view>
				</view>

				<view class="result-text">
					<text class="font-s20 color-33">本次答对</text>
					<text class="font-s24 font-wb color-red"> {{ correctCount }} </text>
					<text class="font-s20 color-33">题</text>
				</view>

				<!-- 积分奖励 -->
				<view class="result-points" v-if="pointReward > 0">
					<text class="font-s16 color-66">🎉 获得 </text>
					<text class="font-s18 font-wb color-00b6ff">{{ pointReward }}</text>
					<text class="font-s16 color-66"> 积分</text>
				</view>

				<view class="finish-btn-container">
					<view class="finish-btn dis-flex-hv w-full bg-4c54d3 radius-32" @click="finishGame">
						<text class="font-s16 font-wb color-ff">结束本局</text>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import { getPKQuestions, submitPKResult } from "@/api/competition";
import { useUserStore } from '@/store/user';
import { useCompetitionStore } from '@/store/competition';
import { isSuccess } from '@/utils/response';
import {request,uploads,rectTo,navTo,showMsg} from '@/utils/index';
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
			currentQuestionIndex: 0,
			selectedAnswer: null,
			selectedAnswerMulti:[],
			showAnswer: false,
			correctCount: 0,
			showResultPage: false,
			pointReward: 0,         // 获得的积分奖励
			userAnswers: [],        // 用户答案记录
			questions: []           // 题目列表（从API加载）
		}
	},
	computed: {
		currentQuestion() {
			// 如果题目列表为空或索引超出范围，返回默认对象避免错误
			if (!this.questions || this.questions.length === 0 || !this.questions[this.currentQuestionIndex]) {
				return {
					id: 0,
					type: 'judge',
					question: '加载中...',
					options: []
				};
			}
			return this.questions[this.currentQuestionIndex];
		},
		questionTypeText() {
			const typeMap = {
				'single': '单选题',
				'judge': '判断题',
				'multi': '多选题'
			};
			return typeMap[this.currentQuestion.type] || this.currentQuestion.type;
		},
		isLastQuestion() {
			return this.currentQuestionIndex === this.questions.length - 1;
		}
	},
	onLoad(options) {
		if (!this.competitionStore.checkMatchId()) {
			return;
		}
		if (!this.userStore.checkLogin()) {
			return;
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

		selectAnswer(index) {
			let type = this.questions[this.currentQuestionIndex]['type'];
			if(type == 'multi'){
				if(this.selectedAnswerMulti.includes(index)){
					for(let i in this.selectedAnswerMulti){
						if(this.selectedAnswerMulti[i] === index){
							this.selectedAnswerMulti.splice(i, 1);
							break;
						}
					}
				}else{
					this.selectedAnswerMulti.push(index);
				}
			}else{
				this.selectedAnswer = index;
			}
		},

		nextQuestion() {
			let type = this.questions[this.currentQuestionIndex]['type'];
			if ((type=='multi'&&this.selectedAnswerMulti.length==0) || (type!='multi'&&this.selectedAnswer === null)) {
				uni.showToast({
					title: '请先选择答案',
					icon: 'none'
				});
				return;
			}

			// 记录用户答案
			let userAnswer = {
				questionId: this.currentQuestion.id,
				type,
				isright: this.questions[this.currentQuestionIndex]['isright']
			};
			if(type=='multi'){
				userAnswer.answer = this.selectedAnswerMulti;
				let answerContent = [];
				for(let i in this.selectedAnswerMulti){
					answerContent.push(this.questions[this.currentQuestionIndex]['options'][i]);
				}
				userAnswer.answerContent = answerContent;
			}else{
				userAnswer.answer = this.selectedAnswer;
				userAnswer.answerContent = this.questions[this.currentQuestionIndex]['options'][this.selectedAnswer];
			}
			this.userAnswers.push(userAnswer);
			//console.log("nextQuestion",this.userAnswers);

			if (this.isLastQuestion) {
				// 最后一题，提交答案
				this.submitAnswers();
			} else {
				this.currentQuestionIndex++;
				this.selectedAnswer = null;
				this.selectedAnswerMulti = [];
				this.showAnswer = false;
			}
		},

		showResult() {
			this.showResultPage = true;
		},

		finishGame() {
			uni.navigateBack();
		},

		/**
		 * 从API加载题目
		 */
		async loadQuestions() {
			uni.showLoading({
				title: '加载题目中...'
			});

			try {
				/*const res = await getPKQuestions({
					matchId: this.competitionStore.currentMatchId,
					userId: this.userStore.userId
				});*/
				let res = await request({
					method:'POST',
					data:{matchId: this.competitionStore.currentMatchId,userId: this.userStore.userId},
					url:"/match/exam-pk"
				});
				if(res.code){
					showMsg('back',{title:res.msg});
					return;
				}
				res['code'] = 1;
				console.log('pk exams',res);
				/*
				res.data的格式:
				{
					"questions":[
						{"id": 0,"question": "设备紧固不包括的项目是( )。","type": "single","options":["A、机座","B、保护罩","C、齿轮","D、油箱盖"]}
					],
					"totalCount": 100,
					"examNum": 10,
					"getPoint": 10,
					"dailyJoinNum": 3,
					"todayRemaining": 3
				}
				*/

				// 先关闭加载提示
				uni.hideLoading();

				if (isSuccess(res) && res.data && res.data.questions) {
					// 格式化题目数据
					this.questions = res.data.questions.map(q => ({
						id: q.id,
						type: q.type,  // judge 或 choice
						question: q.question,
						options: q.options,
						isright:q.isright
					}));

					// 重置答题状态
					this.currentQuestionIndex = 0;
					this.selectedAnswer = null;
					this.correctCount = 0;
					this.userAnswers = [];
					this.showResultPage = false;
				} else {
					// 显示友好的错误提示弹窗
					const errorMsg = res.msg || res.message || '加载题目失败';

					uni.showModal({
						title: '温馨提示',
						content: errorMsg,
						showCancel: false,
						confirmText: '知道了',
						success: () => {
							uni.navigateBack();
						}
					});
				}
			} catch (error) {
				console.error('加载题目失败:', error);
				uni.hideLoading();

				uni.showModal({
					title: '温馨提示',
					content: error.message || '网络请求失败，请重试',
					showCancel: false,
					confirmText: '知道了',
					success: () => {
						uni.navigateBack();
					}
				});
			}
		},

		/**
		 * 提交答案并显示结果
		 */
		async submitAnswers() {
			let param = {
				userId: this.userStore.userId,
				matchId: this.competitionStore.currentMatchId,
				answers: this.userAnswers
			};
			//console.log('submitAnswers',param);return;
			
			uni.showLoading({
				title: '提交中...'
			});

			try {
				//const res = await submitPKResult(param);
				let res = await request({
					method:'POST',
					data:param,
					url:"/match/exam-pk-submit"
				});
				if(res.code){
					showMsg('back',{title:res.msg});
					return;
				}
				res['code'] = 1;
				console.log('pk exams',res);

				if (isSuccess(res) && res.data) {
					this.correctCount = res.data.correctCount || 0;
					this.pointReward = res.data.pointReward || 0;
					this.showResultPage = true;
				} else {
					throw new Error(res.message || '提交失败');
				}
			} catch (error) {
				console.error('提交答案失败:', error);
				uni.showToast({
					title: error.message || '提交失败，请重试',
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
	min-height: 100vh;
	padding-bottom: 30rpx;
}

/* 蓝色渐变背景（无法用全局类实现） */
.bg-blue {
	background: linear-gradient(135deg, #0033ff 0%, #0066ff 50%, #3d8aff 100%);
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
	padding: 40rpx 20rpx;
}

.question-card {
	padding: 40rpx 30rpx;
}

.question-number {
	margin-bottom: 15rpx;
}

.question-type {
	margin-bottom: 20rpx;
}

.question-text {
	margin-bottom: 30rpx;
	line-height: 1.8;
}

.options-list {
	margin-bottom: 40rpx;
}

.option-item {
	padding: 25rpx 20rpx;
	margin-bottom: 15rpx;
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
	margin-top: 20rpx;
}

.next-btn {
	height: 90rpx;
	cursor: pointer;
	transition: all 0.3s;
	box-shadow: 0 4rpx 12rpx rgba(0, 191, 255, 0.3);
}

.next-btn:active {
	transform: scale(0.98);
	opacity: 0.8;
}

.result-page {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	padding: 100rpx 40rpx;
	min-height: 600rpx;
}

.result-icon {
	margin-bottom: 60rpx;
}

.icon-circle {
	width: 180rpx;
	height: 180rpx;
}

.result-text {
	margin-bottom: 20rpx;
	display: flex;
	align-items: baseline;
	justify-content: center;
}

.result-points {
	text-align: center;
	margin-bottom: 40rpx;
}

.finish-btn-container {
	width: 100%;
	max-width: 350rpx;
}

.finish-btn {
	height: 90rpx;
	cursor: pointer;
	transition: all 0.3s;
	box-shadow: 0 4rpx 12rpx rgba(0, 191, 255, 0.3);
}

.finish-btn:active {
	transform: scale(0.98);
	opacity: 0.8;
}
</style>
