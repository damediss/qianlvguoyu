<template>
	<view class="content">
		<view style="height: 40px;"></view>
		<view class="exam-box">
			<view class="exam-title">{{ currentIndex + 1 }}/{{ examList.length }}</view>
			<view class="exam-item scrollbar-none" v-if="examList.length > 0">
				<view class="color-73a9fa">
					<text>{{ typeLabel(examList[currentIndex].type) }}</text>
				</view>
				<view class="pad-T10 pad-B10 font-s16">
					<uv-parse :content="examList[currentIndex].content"></uv-parse>
				</view>
				<view class="exam-answer">
					<template v-if="isSubjectExam(examList[currentIndex])">
						<view class="subject-answer">
							<view class="font-wb mar-B10">参考答案：</view>
							<template v-for="(item,index) in getSubjectAnswers(examList[currentIndex])">
								<view class="mar-B10">
									<uv-parse :content="item"></uv-parse>
								</view>
							</template>
						</view>
					</template>
					<template v-else-if="examList[currentIndex].type == 'multi'">
						<template v-for="(item, index) in examList[currentIndex].answer">
							<view class="exam-answer-item border-92 mar-B10"
								:class="{ 'bg-73a9fa color-ff': examList[currentIndex]['user_answer'].includes(item.content) }"
								@click="multiAnswer(index)">
								<view class="mar-L20">{{ item.tag }}、</view>
								<view class="mar-L10"><uv-parse :content="item.content + ''"></uv-parse></view>
							</view>
						</template>
					</template>
					<template v-else>
						<template v-for="(item, index) in examList[currentIndex].answer">
							<view class="exam-answer-item border-92 mar-B10"
								:class="{ 'bg-73a9fa color-ff': examList[currentIndex]['user_answer'] == item.content }"
								@click="selectedAnswer(index)">
								<view class="mar-L20">{{ item.tag }}、</view>
								<view class="mar-L10"><uv-parse :content="item.content + ''"></uv-parse></view>
							</view>
						</template>
					</template>
				</view>
			</view>
			<view class="exam-btns">
				<view class="mar-L20">
					<text v-if="currentIndex == 0" class="btn-none color-92">上一题</text>
					<text v-else class="bg-73a9fa color-ff" @click="doBack">上一题</text>
				</view>
				<view class="mar-R20">
					<text class="bg-73a9fa color-ff" @click="doNext">{{ nextButtonText }}</text>
				</view>
			</view>
			<view class="exam-submit" @click="doFinish">结束</view>
		</view>
		<uni-popup ref="answer-result" type="bottom" :is-mask-click="false">
			<view class="popup">
				<view class="popup-header"></view>
				<view class="popup-body">
					<view>正确答案:</view>
					<template v-for="item in examList[currentIndex].answer">
						<view v-if="item.isCorrect" class="dis-flex">
							<view>{{ item.tag }}、</view>
							<view class="w-full"><uv-parse :content="item.content + ''"></uv-parse></view>
						</view>
					</template>
				</view>
				<view class="popup-footer">
					<view class="bg-73a9fa color-ff" @click="next">{{ currentIndex == examList.length - 1 ? '完成' : '下一题' }}</view>
					<view class="exam-done" @click="doFinish">结束</view>
				</view>
			</view>
		</uni-popup>
	</view>
</template>

<script>
import { request, uploads, rectTo, navTo, showMsg } from "@/utils";
export default {
	data() {
		return {
			paper_id: 0,
			exam_id: 0,
			detail_id: 0,
			exam_time: 60,
			paper: { id: 0, title: '', time: 60 },
			examList: [],
			currentIndex: 0,
			typelist: {
				single: { num: 0, takeNum: 0 },
				multi: { num: 0, takeNum: 0 },
				judge: { num: 0, takeNum: 0 }
			},
			start_time: 0,
			resultList: [],
			wrongList: [],
			isAnswer: 0,
			allrowBack: true,
			typeLabels:{
				single:'单选题',
				multi:'多选题',
				judge:'判断题',
				answer:'简答题',
				analysis:'分析题',
				cal:'计算题',
				essay:'论述题',
				explain:'解释题',
				fill:'填空题',
				drafting:'制图题'
			}
		}
	},
	onLoad(options) {
		if (options.item) {
			let item = JSON.parse(decodeURIComponent(options.item));
			console.log(item);
			if (item.paper_id) {
				this.paper_id = item.paper_id;
			}
			if (item.exam_id) {
				this.exam_id = item.exam_id;
			}
			if (item.typelist) {
				this.typelist = JSON.parse(item.typelist);
				console.log('this.typelist', this.typelist)
			}
			this.getexamList();
		}
	},
	onUnload() {
		console.log("onUnload");
		this.doLearn();
	},
	onBackPress() {
		console.log("onBackPress");
		showMsg('this',{title:'离开请点结束记录复习时长'});
		return this.allrowBack;
	},
	methods: {
		async getexamList() {
			uni.showLoading({
				title: '数据加载中...'
			});
			let res = await request({
				method: 'POST',
				data: { paper_id: this.paper_id, exam_id: this.exam_id, typelist: this.typelist },
				url: "/appreview/randexam"
			});
			uni.hideLoading();
			console.log('exam getData', res);
			if (res.code) {
				let msg = '请求失败';
				if (res?.msg && res.msg) {
					msg = res.msg;
				}
				showMsg('back', { title: msg });
			}
			if (res?.data?.list) {
				this.examList = res.data.list;
				// 清空旧答案，支持重复练习
				for (let i in this.examList) {
					if (this.examList[i].type == 'multi') {
						this.examList[i].user_answer = [];
					} else {
						this.examList[i].user_answer = '';
					}
				}
			}
			if (res?.data?.startTime) {
				this.start_time = res.data.startTime;
			}
		},
		multiAnswer(index) {
			this.isAnswer++;
			if (this.examList[this.currentIndex]['user_answer'].includes(this.examList[this.currentIndex]['answer'][index]['content'])) {
				for (let i in this.examList[this.currentIndex]['user_answer']) {
					if (this.examList[this.currentIndex]['user_answer'][i] == this.examList[this.currentIndex]['answer'][index]['content']) {
						this.examList[this.currentIndex]['user_answer'].splice(i, 1);
						break;
					}
				}
			} else {
				this.examList[this.currentIndex]['user_answer'].push(this.examList[this.currentIndex]['answer'][index]['content']);
			}
		},
		selectedAnswer(index) {
			this.isAnswer++;
			this.examList[this.currentIndex]['user_answer'] = this.examList[this.currentIndex]['answer'][index]['content'];
		},
		isSubjectExam(exam){
			if(!exam) return false;
			if(!exam.type){
				return typeof exam.answer == 'string' || (typeof exam.answer == 'object' && !Array.isArray(exam.answer));
			}
			return !['multi','single','judge'].includes(exam.type + '');
		},
		typeLabel(type){
			return this.typeLabels[type] || (type ? type : '其他题型');
		},
		getSubjectAnswers(exam){
			if(!exam) return [];
			let answer = exam.answer;
			if(typeof answer == 'string'){
				let s = answer.trim();
				if(s.startsWith('[') || s.startsWith('{')){
					try {
						answer = JSON.parse(s);
					} catch(e) {}
				}
				if(typeof answer == 'string'){
					return answer ? [answer] : [];
				}
			}
			if(Array.isArray(answer)){
				return answer.map(item => {
					if(typeof item == 'object'){
						return item.content ? item.content + '' : '';
					}
					return item + '';
				}).filter(item => item);
			}
			if(typeof answer == 'object' && answer){
				return answer.content ? [answer.content + ''] : [];
			}
			return answer ? [answer + ''] : [];
		},
		doBack() {
			if (this.currentIndex > 0) {
				this.currentIndex--;
			}
		},
		doNext() {
			let exam = this.examList[this.currentIndex];
			if(this.isSubjectExam(exam)){
				this.next();
				return;
			}
			if (exam.type == 'multi' && exam['user_answer'].length == 0) {
				showMsg('this', { title: '请选择答案' });
				return;
			}
			if (exam.type != 'multi' && exam['user_answer'] == '') {
				showMsg('this', { title: '请选择答案' });
				return;
			}
			this.$refs['answer-result'].open();
		},
		next() {
			if(this.isSubjectExam(this.examList[this.currentIndex])){
				this.isAnswer++;
				if(this.currentIndex < this.examList.length - 1){
					this.currentIndex++;
				}else{
					this.doFinish();
				}
				return;
			}
			this.doSave((e) => {
				this.$refs['answer-result'].close();
				if (this.currentIndex < this.examList.length - 1) {
					this.currentIndex++;
				}
			});
		},
		doFinish() {
			this.doLearn((e) => {
				uni.navigateBack();
			});
		},
		async doSave(fun) {
			let exam = this.examList[this.currentIndex];
			if(this.isSubjectExam(exam)){
				if(fun) fun(1);
				return;
			}
			let user_answer = exam['user_answer'];
			let wrong = {};
			let isWrong = 0;
			if (exam.type == 'multi') {
				user_answer = JSON.stringify(exam['user_answer']);
				isWrong = 0;
				for (let j in exam['answer']) {
					if (!exam['answer'][j].isCorrect && exam['user_answer'].includes(exam['answer'][j].content)) {
						isWrong = 1;
						wrong = {
							course_id: exam.course_id,
							exam_detail_id: exam.exam_id,
							type: 1,
							userAnswer: user_answer
						};
						break;
					}
				}
			} else {
				isWrong = 0;
				for (let j in exam['answer']) {
					if (exam['answer'][j].isCorrect && exam['answer'][j].content != exam['user_answer']) {
						isWrong = 1;
						wrong = {
							course_id: exam.course_id,
							exam_detail_id: exam.exam_id,
							type: 1,
							userAnswer: user_answer
						};
						break;
					}
				}
			}
			//答题结果
			let result = {
				paper_id: this.paper_id,
				detail_id: exam.id,
				get_score: isWrong > 0 ? 0 : exam.score,
				exam_type: 4,//0正式 1模拟考 2能力测评 3顺序练习 4随机练习
				exam_id: exam.exam_id,//3.2.3版本开始由$this.exam_id改为this.examList[i].exam_id
				type: exam.type,
				user_answer: user_answer,
				start_time: this.start_time
			}
			let paper_id = this.paper_id;
			let exam_id = this.exam_id;
			let param = { paper_id, exam_id, result, wrong };
			uni.showLoading({
				title: '数据加载中...'
			});
			let res = await request({
				method: 'POST',
				data: param,
				url: "/appreview/trainsave"
			});
			uni.hideLoading();
			console.log('exam getData', res);
			if (res.code) {
				let msg = '请求失败';
				if (res?.msg && res.msg) {
					msg = res.msg;
				}
				showMsg('this', { title: msg });
				if (fun) fun(0);
			}
			if (fun) fun(1);
		},
		async doLearn(fun) {
			this.allrowBack = false;
			if (this.isAnswer > 0) {
				uni.showLoading({
					title: '数据加载中...'
				});
				let res = await request({
					method: 'POST',
					data: { paper_id: this.paper_id, exam_id: this.exam_id, start_time: this.start_time },
					url: "/appreview/learnsave"
				});
				uni.hideLoading();
				console.log('exam getData', res);
			}
			fun(1);
		}
	},
	computed:{
		nextButtonText(){
			if(!this.examList.length) return '下一题';
			if(this.isSubjectExam(this.examList[this.currentIndex])){
				return this.currentIndex == this.examList.length - 1 ? '完成' : '下一题';
			}
			return this.currentIndex == this.examList.length - 1 ? '查看答案' : '下一题';
		}
	}
}
</script>

<style>
page {
	background-color: #73a9fa;
}

.content {}

.exam-box {
	margin-left: 40rpx;
	margin-right: 40rpx;
	padding-bottom: 15px;
	background-color: #fff;
	border-radius: 12rpx;
}

.exam-title {
	height: 50px;
	line-height: 50px;
	padding: 0 20rpx;
}

.exam-item {
	margin-left: 20rpx;
	margin-right: 20rpx;
	padding-top: 5px;
	max-height: 500px;
	overflow-y: auto;
	font-size: 14px;
}

.exam-answer-item {
	display: flex;
	align-items: center;
	padding: 4px 0;
	min-height: 40px;
	border-radius: 22px;
}

.subject-answer {
	font-size: 15px;
	line-height: 24px;
}

.exam-btns {
	margin-top: 10px;
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.exam-btns text {
	padding: 5px 10px;
	border-radius: 8rpx;
}

.btn-none {
	background-color: #ccc;
}
.exam-done{
	margin-top: 20px;
	background-color: #ccc;
	border: 8rpx;
}
.exam-submit {
	margin-top: 25px;
	margin-left: 200rpx;
	margin-right: 200rpx;
	height: 40px;
	line-height: 40px;
	line-height: 40px;
	border-radius: 20px;
	text-align: center;
	background-color: #73a9fa;
	color: #fff;
}

.popup {
	width: 750rpx;
	border-top-left-radius: 20rpx;
	border-top-right-radius: 20rpx;
	background-color: #fff;
}

.popup-header {
	padding-top: 10px;
}

.popup-body {
	padding: 10px 20rpx;
}

.popup-footer {
	padding-top: 10px;
	padding-bottom: 10px;
}

.popup-footer view {
	margin-left: 20rpx;
	margin-right: 20rpx;
	height: 40px;
	line-height: 40px;
	text-align: center;
	border-radius: 20px;
}
</style>