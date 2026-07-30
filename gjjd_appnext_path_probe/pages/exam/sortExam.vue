<template>
	<view class="content">
		<view style="height: 40px;"></view>
		<view class="exam-box">
			<view class="exam-title">{{currentIndex+1}}/{{examList.length}}</view>
			<view class="exam-item scrollbar-none" v-if="examList.length>0">
				<view class="color-73a9fa">
					<text v-if="examList[currentIndex].type=='multi'">多选题</text>
					<text v-if="examList[currentIndex].type=='single'">单选题</text>
					<text v-if="examList[currentIndex].type=='judge'">判断题</text>
					<text v-if="isSubjectExam(examList[currentIndex])">简答题</text>
				</view>
				<view class="pad-T10 pad-B10 font-s16">
					<text v-if="isPlainText(getExamContent(examList[currentIndex]))">{{getExamContent(examList[currentIndex])}}</text>
					<uv-parse v-else :content="getExamContent(examList[currentIndex])"></uv-parse>
				</view>
				<view class="exam-answer">
					<template v-if="isSubjectExam(examList[currentIndex])">
						<view class="subject-answer">
							<view class="font-wb mar-B10">参考答案：</view>
							<template v-for="(item,index) in getSubjectAnswers(examList[currentIndex])">
								<view class="mar-B10">
									<text v-if="isPlainText(item)">{{item}}</text>
									<uv-parse v-else :content="item"></uv-parse>
								</view>
							</template>
						</view>
					</template>
					<template v-else-if="examList[currentIndex].type=='multi'">
						<template v-for="(item,index) in examList[currentIndex].answer">
							<view class="exam-answer-item border-92 mar-B10" :class="{'bg-73a9fa color-ff':examList[currentIndex]['user_answer'].includes(item.content)}" @click="multiAnswer(index)">
								<view class="mar-L20">{{item.tag}}、</view>
								<view class="mar-L10">
									<text v-if="isPlainText(item.content)">{{item.content}}</text>
									<uv-parse v-else :content="item.content+''"></uv-parse>
								</view>
							</view>
						</template>
					</template>
					<template v-else>
						<template v-for="(item,index) in examList[currentIndex].answer">
							<view class="exam-answer-item border-92 mar-B10" :class="{'bg-73a9fa color-ff':examList[currentIndex]['user_answer']==item.content}" @click="selectedAnswer(index)">
								<view class="mar-L20">{{item.tag}}、</view>
								<view class="mar-L10">
									<text v-if="isPlainText(item.content)">{{item.content}}</text>
									<uv-parse v-else :content="item.content+''"></uv-parse>
								</view>
							</view>
						</template>
					</template>
				</view>
			</view>
			<view class="exam-btns">
				<view class="mar-L20">
					<text v-if="currentIndex==0" class="btn-none color-92">上一题</text>
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
							<view>{{item.tag}}、</view>
							<view class="w-full">
								<text v-if="isPlainText(item.content)">{{item.content}}</text>
								<uv-parse v-else :content="item.content+''"></uv-parse>
							</view>
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
	import {request,uploads,rectTo,navTo,showMsg} from "@/utils";
	export default {
		data() {
			return {
				paper_id:0,
				exam_id:0,
				detail_id:0,
				detail_exam_id:0,
				exam_time:60,
				paper:{id:0,title:'',time:60},
				examList:[],
				currentIndex:0,
				start_time:0,
				resultList:[],
				wrongList:[],
				isAnswer:0,
				allrowBack:true
			}
		},
		onLoad(options) {
			if(options.item){
				let item = JSON.parse(decodeURIComponent(options.item));
				console.log(item);
				if(item.paper_id){
					this.paper_id = item.paper_id;
				}
				if(item.exam_id){
					this.exam_id = item.exam_id;
				}
				if(item.detail_id){
					this.detail_id = item.detail_id;
				}
				if(item.detail_exam_id){
					this.detail_exam_id = item.detail_exam_id;
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
			async getexamList(){
				uni.showLoading({
					title:'数据加载中...'
				});
				let res = await request({
					method:'POST',
					data:{paper_id:this.paper_id,exam_id:this.exam_id,num:this.num},
					url:"/appreview/sortexam"
				});
				uni.hideLoading();
				console.log('exam getData',res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('back',{title:msg});
				}
				if(res?.data?.list){
					this.examList = res.data.list.map(item => this.normalizeExam(item));
					this.logPracticeStats('sortexam practice stats',this.examList);
					// 清空旧答案，支持重复练习
					for(let i in this.examList){
						if(this.examList[i].type == 'multi'){
							this.examList[i].user_answer = [];
						}else{
							this.examList[i].user_answer = '';
						}
						if(this.examList[i].result){
							this.examList[i].result.user_answer = null;
						}
						if(this.isCurrentExam(this.examList[i])){
							this.currentIndex = Number(i);
						}
					}
				}
				if(res?.data?.startTime){
					this.start_time = res.data.startTime;
				}
			},
			multiAnswer(index){
				this.isAnswer++;
				if(this.examList[this.currentIndex]['user_answer'].includes(this.examList[this.currentIndex]['answer'][index]['content'])){
					for(let i in this.examList[this.currentIndex]['user_answer']){
						if(this.examList[this.currentIndex]['user_answer'][i] == this.examList[this.currentIndex]['answer'][index]['content']){
							this.examList[this.currentIndex]['user_answer'].splice(i,1);
							break;
						}
					}
				}else{
					this.examList[this.currentIndex]['user_answer'].push(this.examList[this.currentIndex]['answer'][index]['content']);
				}
			},
			selectedAnswer(index){
				this.isAnswer++;
				this.examList[this.currentIndex]['user_answer'] = this.examList[this.currentIndex]['answer'][index]['content'];
			},
			isPlainText(content){
				if(content === undefined || content === null) return true;
				return !/<[a-z][\s\S]*>/i.test(content + '');
			},
			getFirstValue(obj,keys){
				for(let i in keys){
					let value = obj[keys[i]];
					if(value !== undefined && value !== null && value !== ''){
						return value;
					}
				}
				return '';
			},
			getExamContent(exam){
				if(!exam) return '';
				return this.getFirstValue(exam,['content','exam_content','question','title','stem','subject']);
			},
			getCorrectTags(exam){
				let answer = this.getFirstValue(exam,['exam_answer','right_answer','rightAnswer','correct_answer','correctAnswer']);
				if(Array.isArray(answer)) return answer.map(item => item + '');
				return (answer + '').replace(/，/g,',').split(',').map(item => item.trim()).filter(item => item);
			},
			normalizeAnswerItem(item,index,correctTags){
				let tags = ['A','B','C','D','E','F','G','H'];
				if(typeof item != 'object'){
					let tag = tags[index] || (index + 1) + '';
					return {
						tag,
						content: item + '',
						isCorrect: correctTags.includes(tag)
					};
				}
				let tag = this.getFirstValue(item,['tag','key','label','option','name']) || tags[index] || (index + 1) + '';
				let content = this.getFirstValue(item,['content','exam_content','value','text','title','answer','option_content']);
				let isCorrect = item.isCorrect || item.is_correct || item.correct || correctTags.includes(tag + '');
				return {
					...item,
					tag: tag + '',
					content: content + '',
					isCorrect
				};
			},
			buildAnswersFromExam(exam){
				let keys = ['a','b','c','d','e','f','g','h'];
				let answers = [];
				let correctTags = this.getCorrectTags(exam);
				for(let i in keys){
					let tag = keys[i].toUpperCase();
					let content = this.getFirstValue(exam,['exam_' + keys[i],keys[i],tag,'option_' + keys[i],tag.toLowerCase() + '_content']);
					if(content !== ''){
						answers.push({
							tag,
							content: content + '',
							isCorrect: correctTags.includes(tag)
						});
					}
				}
				return answers;
			},
			normalizeExam(exam){
				if(!exam) return exam;
				let item = {...exam};
				item.content = this.getExamContent(item);
				if(Array.isArray(item.answer)){
					let correctTags = this.getCorrectTags(item);
					item.answer = item.answer.map((answer,index) => this.normalizeAnswerItem(answer,index,correctTags));
				}else if(this.isSubjectExam(item)){
					item.answer = item.answer || this.getFirstValue(item,['exam_answer','right_answer','rightAnswer','correct_answer','correctAnswer','reference_answer']);
				}else{
					item.answer = this.buildAnswersFromExam(item);
				}
				return item;
			},
			isSubjectExam(exam){
				if(!exam) return false;
				if(!exam.type){
					return typeof exam.answer == 'string' || (typeof exam.answer == 'object' && !Array.isArray(exam.answer));
				}
				return !['multi','single','judge'].includes(exam.type + '');
			},
			isCurrentExam(exam){
				if(!exam) return false;
				let detailId = this.detail_id + '';
				let detailExamId = this.detail_exam_id + '';
				return (detailId && (exam.id + '' == detailId || exam.exam_id + '' == detailId)) ||
					(detailExamId && exam.exam_id + '' == detailExamId);
			},
			getSubjectAnswers(exam){
				if(!exam) return [];
				if(typeof exam.answer == 'string'){
					return [exam.answer];
				}
				if(Array.isArray(exam.answer)){
					return exam.answer.map(item => {
						if(typeof item == 'object'){
							return item.content ? item.content + '' : '';
						}
						return item + '';
					}).filter(item => item);
				}
				if(typeof exam.answer == 'object'){
					return exam.answer.content ? [exam.answer.content + ''] : [];
				}
				return exam.answer ? [exam.answer + ''] : [];
			},
			logPracticeStats(label,list){
				let stats = {};
				if(Array.isArray(list)){
					for(let i in list){
						let type = list[i] && list[i].type ? list[i].type : 'unknown';
						stats[type] = (stats[type] || 0) + 1;
					}
				}
				console.log(label,{
					total: Array.isArray(list) ? list.length : 0,
					stats
				});
			},
			doBack(){
				if(this.currentIndex>0){
					this.currentIndex--;
				}
			},
			doNext(){
				let exam = this.examList[this.currentIndex];
				if(this.isSubjectExam(exam)){
					this.next();
					return;
				}
				if(exam.type == 'multi' && exam['user_answer'].length == 0){
					showMsg('this',{title:'请选择答案'});
					return;
				}
				if(exam.type != 'multi' && exam['user_answer']==''){
					showMsg('this',{title:'请选择答案'});
					return;
				}
				this.$refs['answer-result'].open();
			},
			next(){
				if(this.isSubjectExam(this.examList[this.currentIndex])){
					this.isAnswer++;
					if(this.currentIndex<this.examList.length-1){
						this.currentIndex++;
					}else{
						this.doFinish();
					}
					return;
				}
				this.doSave((e)=>{
					this.$refs['answer-result'].close();
					if(this.currentIndex<this.examList.length-1){
						this.currentIndex++;
					}
				});
			},
			doFinish(){
				this.doLearn((e)=>{
					uni.navigateBack();
				});
			},
			async doSave(fun){
				let exam = this.examList[this.currentIndex];
				let user_answer = exam['user_answer'];
				let wrong = {};
				let isWrong = 0;
				if(exam.type == 'multi'){
					user_answer = JSON.stringify(exam['user_answer']);
					isWrong = 0;
					for(let j in exam['answer']){
						if(!exam['answer'][j].isCorrect && exam['user_answer'].includes(exam['answer'][j].content)){
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
				}else{
					isWrong = 0;
					for(let j in exam['answer']){
						if(exam['answer'][j].isCorrect && exam['answer'][j].content!=exam['user_answer']){
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
					paper_id: 0,
					detail_id: exam.id,
					get_score: isWrong>0?0:exam.score,
					exam_type: 3,//0正式 1模拟考 2能力测评 3顺序练习 4随机练习
					exam_id: exam.exam_id,//3.2.3版本开始由$this.exam_id改为this.examList[i].exam_id
					type: exam.type,
					user_answer: user_answer,
					start_time:this.start_time
				}
				let paper_id = this.paper_id;
				let exam_id = this.exam_id;
				let param = {paper_id,exam_id,result,wrong};
				uni.showLoading({
					title:'数据加载中...'
				});
				let res = await request({
					method:'POST',
					data:param,
					url:"/appreview/trainsave"
				});
				uni.hideLoading();
				console.log('exam getData',res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg});
					if(fun) fun(0);
				}
				if(fun) fun(1);
			},
			async doLearn(fun){
				this.allrowBack = false;
				if(this.isAnswer>0){
					uni.showLoading({
						title:'数据加载中...'
					});
					let res = await request({
						method:'POST',
						data:{paper_id:this.paper_id,exam_id:this.exam_id,start_time:this.start_time},
						url:"/appreview/learnsave"
					});
					uni.hideLoading();
					console.log('exam getData',res);
				}
				if(fun) fun(1);
			}
		},
		computed: {
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
	page{
		background-color: #73a9fa;
	}
	.content{
	}
	.exam-box{
		margin-left: 40rpx;
		margin-right: 40rpx;
		padding-bottom: 15px;
		background-color: #fff;
		border-radius: 12rpx;
	}
	.exam-title{
		height: 50px;
		line-height: 50px;
		padding: 0 20rpx;
	}
	.exam-item{
		margin-left: 20rpx;
		margin-right: 20rpx;
		padding-top: 5px;
		max-height: 500px;
		overflow-y: auto;
		font-size: 14px;
	}
	.exam-answer-item{
		display: flex;
		align-items: center;
		padding: 4px 0;
		min-height: 40px;
		border-radius: 22px;
	}
	.subject-answer{
		padding: 10px 20rpx;
		line-height: 1.6;
		background-color: #f8f8f8;
		border-radius: 12rpx;
	}
	.exam-btns{
		margin-top: 10px;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}
	.exam-btns text{
		padding: 5px 10px;
		border-radius: 8rpx;
	}
	.btn-none{
		background-color: #ccc;
	}
	.exam-done{
		margin-top: 20px;
		background-color: #ccc;
		border: 8rpx;
	}
	.exam-submit{
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
	.popup{
		width: 750rpx;
		border-top-left-radius: 20rpx;
		border-top-right-radius: 20rpx;
		background-color: #fff;
	}
	.popup-header{
		padding-top: 10px;
	}
	.popup-body{
		padding: 10px 20rpx;
	}
	.popup-footer{
		padding-top: 10px;
		padding-bottom: 10px;
	}
	.popup-footer view{
		margin-left: 20rpx;
		margin-right: 20rpx;
		height: 40px;
		line-height: 40px;
		text-align: center;
		border-radius: 20px;
	}
</style>