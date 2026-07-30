<template>
	<view class="content">
		<view class="cover">
			<image :src="info.cover" model="widthFix"></image>
		</view>
		<view class="tabs">
			<view class="tabs-item dis-flex-hv" v-for="tab in tabs">
				<view @click="onTabs(tab)">
					<view>{{tab.title}}</view>
					<view class="dis-flex-hv" v-if="tab.tag == activeTag">
						<view class="tabs-active"></view>
					</view>
				</view>
			</view>
		</view>
		<view class="tabs-content scrollbar-none::-webkit-scrollbar" v-if="activeTag == 'detail'" :style="'height:'+($winHeight-$statusBarHeight-44-200-44-5)+'px;'">
			<view>标题:{{info.class.title}}</view>
			<view>工种:{{info.class.work_type}}</view>
			<view>人数:{{info.class.nums}}</view>
			<view>开始日期:{{info.date}}</view>
		</view>
		<view class="tabs-content scrollbar-none::-webkit-scrollbar" v-if="activeTag == 'study'" :style="'height:'+($winHeight-$statusBarHeight-44-200-44-5)+'px;'">
			<view class="pad-L10 pad-R10" :class="i%2>0?'bg-73a9fa':'bg-a8c8ff'" v-for="(item,i) in info.kejian">
				<view class="dis-flex-hsb">
					<view>{{item.title}}(课时:{{item.keshi}}时)</view>
					<view class="kejian-model mar-T5 font-s12" @click="toExamLog(item)">练习次数({{item.examcount}})</view>
				</view>
				<view class="dis-flex mar-T10">
					<view class="kejian-model mar-L10" @click="toKejianTuwen(item)">图文模式</view>
					<view class="kejian-model mar-L10" @click="toKejian(item)">视频模式</view>
				</view>
			</view>
		</view>
		<uni-popup ref="kejian" type="bottom" :mask-click="false">
			<view class="popup-kejian" :style="'height:'+($winHeight*2/3)+'px;'">
				<template v-if="videoUrl">
					<view class="video mar-B20">
						<video style="width: 99%;height: 280px;"
						autoplay
						:src="videoUrl"
						:controls="true"
						:show-fullscreen-btn="false"
						@ended="playEnd"></video>
						<view class="mar-T20 text-C" style="position: absolute;bottom: -50px;">
							<text class="video-close" @click="onKejianClose()">关闭</text>
						</view>
					</view>
				</template>
				<template v-if="tuwenContent">
					<view class="kejian-content">
						<view class="kejian-content-box scrollbar-none::-webkit-scrollbar" :style="'height:'+(($winHeight*2/3) - 50)+'px;'">
							<uv-parse :content="tuwenContent"></uv-parse>
						</view>
					</view>
					<view class="mar-T20 text-C">
						<text class="do-xiti mar-R20" @click="onKejianXiti()">做习题</text>
						<text class="video-close" @click="onKejianClose()">关闭</text>
					</view>
				</template>
				<template v-if="isExam">
					<view class="exam-box">
						<view class="exam-title">{{ currentIndex + 1 }}/{{ examList.length }}</view>
						<view class="exam-item scrollbar-none" v-if="examList.length > 0">
							<view class="color-73a9fa">
								<text v-if="examList[currentIndex].type == 'multi'">多选题</text>
								<text v-if="examList[currentIndex].type == 'single'">单选题</text>
								<text v-if="examList[currentIndex].type == 'judge'">判断题</text>
							</view>
							<view class="pad-T10 pad-B10 font-s16">
								<uv-parse :content="examList[currentIndex].content"></uv-parse>
							</view>
							<view class="exam-answer">
								<template v-if="examList[currentIndex].type == 'multi'">
									<template v-for="(item, index) in examList[currentIndex].answer">
										<view class="exam-answer-item border-92 mar-B10"
											:class="{ 'bg-73a9fa color-ff': examList[currentIndex]['user_answer'].includes(item.tag) }"
											@click="multiAnswer(index)">
											<view class="mar-L20">{{ item.tag }}、</view>
											<view class="mar-L10"><uv-parse :content="item.content + ''"></uv-parse></view>
										</view>
									</template>
								</template>
								<template v-else>
									<template v-for="(item, index) in examList[currentIndex].answer">
										<view class="exam-answer-item border-92 mar-B10"
											:class="{ 'bg-73a9fa color-ff': examList[currentIndex]['user_answer'] == item.tag }"
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
								<text class="bg-73a9fa color-ff" @click="doNext">{{ currentIndex == examList.length - 1 ? '查看结果' : '下一题'}}</text>
							</view>
						</view>
					</view>
				</template>
			</view>
		</uni-popup>
		<uni-popup ref="kejianlog" type="bottom" :mask-click="false">
			<view class="popup-kejian">
				<view style="height: 30px;line-height:30px;text-align: right;">
					<text class="mar-R10" @click="kejianlogClose">关闭</text>
				</view>
				<view class="pad-L10 pad-R10">
					<view class="dis-flex-hsb">
						<view class="w-full">做题日期</view>
						<view class="w-80">题量</view>
						<view class="w-80 text-R">得分</view>
					</view>
				</view>
				<view :style="'height:'+($winHeight*2/3)+'px;'" style="overflow-y: auto;scrollbar-none::-webkit-scrollbar">
				<view class="pad-L10 pad-R10" :class="i%2>0?'bg-73a9fa':'bg-a8c8ff'" v-for="(item,i) in examlogs">
					<view class="dis-flex-hsb">
						<view class="w-full">{{item.date}}</view>
						<view class="w-80">{{item.nums}}</view>
						<view class="w-80 text-R">{{item.score}}</view>
					</view>
				</view>
				</view>
			</view>
		</uni-popup>
	</view>
</template>

<script>
	import {request,uploads,makeCall,rectTo,navTo,showMsg} from "@/utils";
	export default {
		data() {
			return {
				id:0,
				activeTag:'detail',
				tabs:[
					{title:'详情',tag:'detail'},
					{title:'常规学习',tag:'study'}
				],
				info:{
					id:0,
					cover:'',
					class:{
						id:0,
						title:'',
						work_type:'',
						nums:0
					},
					kejian:[]
				},
				videoUrl:'',
				tuwenContent:'',
				tuwenTimes:0,
				tuwenTimeid:0,
				isExam:0,
				examList:[],
				currentIndex:0,
				isSubmit:0,
				examlogs:[]
			}
		},
		onLoad(options) {
			if(options.id){
				this.id = options.id;
				this.getData();
			}
		},
		onUnload() {
			if(this.tuwenTimeid>0){
				clearInterval(this.tuwenTimeid);
				this.tuwenTimeid = 0;
				this.tuwenTimes = 0;
			}
		},
		methods: {
			onTabs(tab){
				if(tab.tag == this.activeTag){
					return;
				}
				this.activeTag = tab.tag;
			},
			async getData(){
				let res = await request({
					method:'POST',
					data:{id:this.id},
					url:"/promote/train-detail"
				});
				console.log("promote train detail",res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg});
				}
				if(res?.data?.info){
					this.info = res.data.info;
				}
			},
			async toExamLog(item){
				console.log("ExamLog",item);
				let ck_id = item.id;
				let res = await request({
					method:'POST',
					data:{ck_id},
					url:"/promote/train-detail-exam-log-list"
				});
				console.log("promote train detail logs",res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg});
				}
				if(res?.data?.list){
					this.examlogs = res.data.list;
				}
				this.$refs.kejianlog.open();
			},
			kejianlogClose(){
				this.examlogs = [];
				this.$refs.kejianlog.close();
			},
			toKejian(item){
				console.log("toKejian",item);
				this.videoUrl = item.video;
				this.examList = item.exams;
				this.$refs.kejian.open();
				//navTo("/pages/promote/kejian",{obj:item});
				/*uni.downloadFile({
					url: item.fileUrl,
					success: function (res) {
						var filePath = res.tempFilePath;
						uni.openDocument({
							filePath: filePath,
							success: function (res) {
								console.log('打开文档成功');
							}
						});
					}
				});*/
			},
			toKejianTuwen(item){
				console.log("toKejian",item);
				this.tuwenContent = item.content;
				this.tuwenTimes = 0;
				if(this.tuwenTimeid>0){
					clearInterval(this.tuwenTimeid);
					this.tuwenTimeid = 0;
				}
				this.tuwenTimeid = setInterval(()=>{
					this.tuwenTimes++;
				},1000);
				this.examList = item.exams;
				this.$refs.kejian.open();
			},
			onKejianXiti(){
				if(this.tuwenTimeid>0){
					if(this.tuwenTimes<60*3){
						showMsg('this',{title:'您当前学习时长小于3分钟,还不能做题'});
						return;
					}
					clearInterval(this.tuwenTimeid);
					this.tuwenTimeid = 0;
				}
				this.videoUrl = "";
				this.tuwenContent = "";
				this.isExam = 1;
			},
			onKejianClose(){
				this.isExam = 0;
				this.tuwenTimes = 0;
				this.tuwenContent = "";
				this.videoUrl = "";
				this.examList = [];
				this.currentIndex = 0;
				this.$refs.kejian.close();
				if(this.tuwenTimeid>0){
					clearInterval(this.tuwenTimeid);
					this.tuwenTimeid = 0;
				}
			},
			playEnd(e){
				console.log("play end",e);
				this.videoUrl = "";
				this.tuwenContent = "";
				this.isExam = 1;
			},
			multiAnswer(index) {
				if (this.examList[this.currentIndex]['user_answer'].includes(this.examList[this.currentIndex]['answer'][index]['tag'])) {
					for (let i in this.examList[this.currentIndex]['user_answer']) {
						if (this.examList[this.currentIndex]['user_answer'][i] == this.examList[this.currentIndex]['answer'][index]['tag']) {
							this.examList[this.currentIndex]['user_answer'].splice(i, 1);
							break;
						}
					}
				} else {
					this.examList[this.currentIndex]['user_answer'].push(this.examList[this.currentIndex]['answer'][index]['tag']);
				}
			},
			selectedAnswer(index) {
				console.log("****="+index)
				this.examList[this.currentIndex]['user_answer'] = this.examList[this.currentIndex]['answer'][index]['tag'];
			},
			doBack() {
				if (this.currentIndex > 0) {
					this.currentIndex--;
				}
			},
			async doNext() {
				if (this.examList[this.currentIndex].type == 'multi' && this.examList[this.currentIndex]['user_answer'].length == 0) {
					showMsg('this', { title: '请选择答案' });
					return;
				}
				if (this.examList[this.currentIndex].type != 'multi' && this.examList[this.currentIndex]['user_answer'] == '') {
					showMsg('this', { title: '请选择答案' });
					return;
				}
				if (this.currentIndex < this.examList.length - 1) {
					this.currentIndex++;
				}else{
					if(this.isSubmit) return;
					this.isSubmit = 1;
					console.log("练习结果",this.examList);
					let rightCount = 0;
					for(let i in this.examList){
						let type = this.examList[i]['type'];
						let answer = this.examList[i]['user_answer'];
						let obj = this.examList[i].answer;
						let isright = 1;
						if(type == 'multi'){
							for(let j in obj){
								if(!obj[j].isCorrect && answer.includes(obj[j]['tag'])){
									isright = 0;
								}
							}
						}else{
							for(let j in obj){
								if(obj[j].isCorrect && answer!=obj[j]['tag']){
									isright = 0;
								}
							}
						}
						if(isright) rightCount++;
					}
					//计算本次正确率
					console.log(rightCount+","+this.examList.length)
					let rate = (rightCount/this.examList.length)*100;
					rate = rate.toFixed(0);
					showMsg("this",{title:'本次得分:'+rate});
					uni.showLoading({
						title:'正在提交成绩...'
					});
					let res = await request({
						method:'POST',
						data:{exams:this.examList,score:rate},
						url:"/promote/train-detail-exam-log"
					});
					uni.hideLoading();
					this.isSubmit = 0;
					console.log("promote train detail",res);
					this.onKejianClose();
				}
			}
		}
	}
</script>

<style>
	.content{
		
	}
	.cover{
		height: 200px;
		overflow: hidden;
	}
	.cover image{
		width: 750rpx;
	}
	.tabs{
		height: 44px;
		padding: 0 10rpx;
		display: flex;
		white-space: nowrap;
		overflow-x: auto;
	}
	.tabs-item{
		margin-right: 20rpx;
	}
	.tabs-active{
		width: 80%;
		height: 4px;
		background-color: #ff6b6b;
		border-radius: 2px;
	}
	.tabs-content{
		width: 730rpx;
		padding: 0 10rpx;
		overflow-y: auto;
		/* border: 1rpx solid red; */
	}
	.video{
		width: 750rpx;
		height: 300px;
		display: flex;
		align-items: center;
		justify-content: center;
		position: relative;
	}
	.video-close{
		padding: 2px 6rpx;
		background-color: #ff6b6b;
		border-radius: 4rpx;
		color: #fff;
	}
	.popup-kejian{
		width: 750rpx;
		background-color: #fff;
		border-top-left-radius: 12rpx;
		border-top-right-radius: 12rpx;
	}
	.kejian-content{
		margin-left: 20rpx;
		margin-right: 20rpx;
		padding-top: 5px;
	}
	.kejian-content-box{
		overflow-y: auto;
		border: 1rpx solid #0099ff;
		padding: 3px 5px;
	}
	.do-xiti{
		padding: 2px 6rpx;
		background-color: #0099ff;
		border-radius: 4rpx;
		color: #fff;
	}
	.kejian-model{
		background-color: #fff;
		border-radius: 6rpx;
		padding: 2px 6px;
		margin-bottom: 5px;
	}
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
		height: 40px;
		border-radius: 22px;
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
</style>
