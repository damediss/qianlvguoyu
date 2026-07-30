<template>
	<view class="content">
		<view style="height: 40px;"></view>
		<view class="exam-box">
			<view class="exam-time color-73a9fa">{{timeText}}</view>
			<view class="exam-title">{{currentIndex+1}}/{{examList.length}}</view>
			<view class="exam-item scrollbar-none" v-if="examList.length>0">
				<view class="color-73a9fa">
					<text v-if="examList[currentIndex].type=='multi'">多选题</text>
					<text v-if="examList[currentIndex].type=='single'">单选题</text>
					<text v-if="examList[currentIndex].type=='judge'">判断题</text>
				</view>
				<view class="pad-T10 pad-B10 font-s16">
					<uv-parse :content="examList[currentIndex].content"></uv-parse>
				</view>
				<view class="exam-answer">
					<template v-if="examList[currentIndex].type=='multi'">
						<template v-for="(item,index) in examList[currentIndex].answer">
							<view class="exam-answer-item border-92 mar-B10" :class="{'bg-73a9fa color-ff':examList[currentIndex]['user_answer'].includes(item.content)}" @click="multiAnswer(index)">
								<view class="mar-L20">{{item.tag}}、</view>
								<view class="mar-L10"><uv-parse :content="item.content+''"></uv-parse></view>
							</view>
						</template>
					</template>
					<template v-else>
						<template v-for="(item,index) in examList[currentIndex].answer">
							<view class="exam-answer-item border-92 mar-B10" :class="{'bg-73a9fa color-ff':examList[currentIndex]['user_answer']==item.content}" @click="selectedAnswer(index)">
								<view class="mar-L20">{{item.tag}}、</view>
								<view class="mar-L10"><uv-parse :content="item.content+''"></uv-parse></view>
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
					<text v-if="currentIndex==examList.length-1" class="btn-none color-92">下一题</text>
					<text v-else class="bg-73a9fa color-ff" @click="doNext">下一题</text>
				</view>
			</view>
			<view class="exam-submit" v-if="currentIndex==examList.length-1" @click="doSave">交卷</view>
		</view>
	</view>
</template>

<script>
	import {request,uploads,rectTo,navTo,showMsg} from "@/utils";
	export default {
		data() {
			return {
				paper_id:0,
				exam_id:0,
				num:0,
				exam_time:60,
				paper:{id:0,title:'',time:3600},
				examList:[],
				currentIndex:0,
				start_time:0,
				resultList:[],
				wrongList:[],
				isSubmit:0,
				timeid:0,
				timeText:0,
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
				if(item.num){
					this.num = item.num;
				}
				this.getexamList();
			}
		},
		onBackPress() {
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
					url:"/appreview/simexam"
				});
				uni.hideLoading();
				console.log('exam getData',res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					this.allrowBack = false;
					showMsg('back',{title:msg});
				}
				if(res?.data?.list){
					this.examList = res.data.list;
				}
				if(res?.data?.startTime){
					this.start_time = res.data.startTime;
				}
				if(res?.data?.paper){
					this.paper = res.data.paper;
					this.exam_time = Number(this.paper.time)*60;
					this.timeText = this.gettimeText(this.exam_time);
					this.setTime();
				}
			},
			setTime(){
				let that = this;
				if(this.timeid>0){
					clearInterval(this.timeid);
					this.timeid = 0;
				}
				this.timeid = setInterval(()=>{
					if(that.exam_time>0){
						that.exam_time--;
						that.timeText = that.gettimeText(that.exam_time);
					}else{
						clearInterval(that.timeid);
						that.timeid = 0;
						//考试时间到
						that.doSave();
					}
				},1000);
			},
			gettimeText(seconds){
				let text = '';
				// 计算剩余分钟
				let minutes = Math.floor(seconds / 60);
				if(minutes<10) minutes = '0'+minutes;
				// 计算剩余秒数
				let secs = seconds % 60;
				if(secs<10) secs = '0'+secs;
				text = minutes+":"+secs;
				return text;
			},
			multiAnswer(index){
				if(this.exam_time<1){
					showMsg('this',{title:'考试时间到了'});
					return;//时间到了
				}
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
				if(this.exam_time<1){
					showMsg('this',{title:'考试时间到了'});
					return;//时间到了
				}
				this.examList[this.currentIndex]['user_answer'] = this.examList[this.currentIndex]['answer'][index]['content'];
			},
			doBack(){
				if(this.currentIndex>0){
					this.currentIndex--;
				}
			},
			doNext(){
				if(this.examList[this.currentIndex].type == 'multi' && this.examList[this.currentIndex]['user_answer'].length == 0){
					showMsg('this',{title:'请选择答案'});
					return;
				}
				if(this.examList[this.currentIndex].type != 'multi' && this.examList[this.currentIndex]['user_answer']==''){
					showMsg('this',{title:'请选择答案'});
					return;
				}
				if(this.currentIndex<this.examList.length-1){
					this.currentIndex++;
				}
			},
			async doSave(){
				if(this.isSubmit) return;
				this.isSubmit = 1;
				this.resultList = [];
				for(let i in this.examList){
					if(this.examList[i].type == 'multi'){
						//检查是否答对
						let isWrong = 0;
						for(let j in this.examList[i]['answer']){
							if(!this.examList[i]['answer'][j].isCorrect && this.examList[i]['user_answer'].includes(this.examList[i]['answer'][j].content)){
								isWrong = 1;
								this.wrongList.push({
									course_id: this.examList[i].course_id,
									exam_detail_id: this.examList[i].id,
									type: 1,
									userAnswer: JSON.stringify(this.examList[i]['user_answer'])
								});
								break;
							}
						}
						this.resultList.push({
							paper_id: this.examList[i].paper_id,
							detail_id: this.examList[i].id,
							get_score: isWrong>0?0:this.examList[i].score,
							exam_type: 1,//0正式 1模拟考 2能力测评 3顺序练习 4随机练习	
							exam_id: this.examList[i].exam_id,//3.2.3版本开始由$this.exam_id改为this.examList[i].exam_id
							type: this.examList[i].type,
							user_answer: JSON.stringify(this.examList[i]['user_answer']),
							start_time:this.start_time
						});
					}else{
						//检查是否答对
						let isWrong = 1;
						for(let j in this.examList[i]['answer']){
							if(this.examList[i]['answer'][j].isCorrect&&this.examList[i]['answer'][j].content == this.examList[i]['user_answer']){
								isWrong = 0;
								break;
							}
						}
						if(isWrong>0){
							this.wrongList.push({
								course_id: this.examList[i].course_id,
								exam_detail_id: this.examList[i].id,
								type: 1,
								userAnswer: this.examList[i]['user_answer']
							});
						}
						this.resultList.push({
							paper_id: this.examList[i].paper_id,
							detail_id: this.examList[i].id,
							get_score: isWrong>0?0:this.examList[i].score,
							exam_type: 1,//0正式 1模拟考 2能力测评 3顺序练习 4随机练习	
							exam_id: this.examList[i].exam_id,//3.2.3版本开始由$this.exam_id改为this.examList[i].exam_id
							type: this.examList[i].type,
							user_answer: this.examList[i]['user_answer'],
							start_time:this.start_time
						});
					}
				}
				uni.showLoading({
					title:'数据加载中...'
				});
				let res = await request({
					method:'POST',
					data:{paper_id:this.paper_id,exam_id:this.exam_id,resultList:this.resultList,wrongList:this.wrongList,result_type:1},
					url:"/appreview/save"
				});
				uni.hideLoading();
				console.log('exam getData',res);
				if(res.code){
					this.isSubmit = 0;
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg});
				}
				let param = {
					paper_id:this.paper_id,
					exam_id:this.exam_id,
					type:1//0正式 1模拟考 2能力测评 3顺序练习 4随机练习
				};
				rectTo("/pages/exam/result",param);
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
		position: relative;
	}
	.exam-time{
		position: absolute;
		top: -30px;
		left: 285rpx;
		right: 285rpx;
		z-index: 9999;
		width: 80px;
		height: 80px;
		line-height: 80px;
		background-color: #fff;
		border-radius: 50px;
		text-align: center;
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
	.exam-submit{
		margin-top: 15px;
		margin-left: 20rpx;
		margin-right: 20rpx;
		height: 40px;
		line-height: 40px;
		line-height: 40px;
		border-radius: 20px;
		text-align: center;
		background-color: #73a9fa;
		color: #fff;
	}
</style>
