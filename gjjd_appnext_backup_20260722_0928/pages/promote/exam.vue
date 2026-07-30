<template>
	<view class="content">
		<template v-if="!result_type">
			<view class="nav-item bg-b68320 color-ff mar-B10" @click="toExam">
				<view class="mar-L10 mar-R10"><i class="gjjd-iconfont icon-renwu"></i></view>
				<view class="w-full">去考试</view>
				<view class="mar-R10"><i class="gjjd-iconfont icon-xiangyou"></i></view>
			</view>
		</template>
		<!-- <view v-if="result_type>0 && hasResult" class="nav-item bg-b68320 color-ff mar-B10" @click="toResult(1)">
			<view class="mar-L10 mar-R10"><i class="gjjd-iconfont icon-renwu"></i></view>
			<view class="w-full">查看结果</view>
			<view class="mar-R10"><i class="gjjd-iconfont icon-xiangyou"></i></view>
		</view> -->
		<view class="nav-item bg-00b6ff color-ff mar-B10" @click="toSort">
			<view class="mar-L10 mar-R10">
				<image style="width: 20px;height: 20px;" src="/static/images/sort.png"></image>
			</view>
			<view class="w-full">顺序练习</view>
			<view class="mar-R10"><i class="gjjd-iconfont icon-xiangyou"></i></view>
		</view>
		<view class="nav-item bg-50e3c2 color-ff mar-B10" @click="toRand">
			<view class="mar-L10 mar-R10">
				<image style="width: 20px;height: 20px;" src="/static/images/rand.png"></image>
			</view>
			<view class="w-full">随机练习</view>
			<view class="mar-R10"><i class="gjjd-iconfont icon-xiangyou"></i></view>
		</view>
		<view class="nav-item bg-e7916c color-ff mar-B10" @click="toSim">
			<view class="mar-L10 mar-R10">
				<image style="width: 20px;height: 20px;" src="/static/images/exam.png"></image>
			</view>
			<view class="w-full">模拟考试</view>
			<view class="mar-R10"><i class="gjjd-iconfont icon-xiangyou"></i></view>
		</view>
		<view class="nav-item bg-e55744 color-ff" @click="toWrong">
			<view class="mar-L10 mar-R10">
				<image style="width: 20px;height: 20px;" src="/static/images/wrong.png"></image>
			</view>
			<view class="w-full">我的错题</view>
			<view class="mar-R10"><i class="gjjd-iconfont icon-xiangyou"></i></view>
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
				hasReview:0,
				hasResult:0,
				review:0,
				examInfo:{id:0,exam_exp:1,exam_time:0},
				result_type:0
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
				if(item.has_review){
					this.hasReview = Number(item.has_review);
				}
				if(item.resultType){
					this.result_type = Number(item.resultType);
				}
				if(this.exam_id){
					this.getData();
				}
			}
		},
		methods: {
			async getData(){
				uni.showLoading({
					title:'数据加载中...'
				});
				let res = await request({
					method:'POST',
					data:{paper_id:this.paper_id,exam_id:this.exam_id,result_type:this.result_type},
					url:"/apppromote/examinfo"
				});
				uni.hideLoading();
				console.log('promote exam getData',res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg})
				}
				if(res?.data?.hasResult){
					this.hasResult = res.data.hasResult;
				}
				if(res?.data?.review){
					this.review = res.data.review;
				}
				if(res?.data?.examInfo){
					this.examInfo = res.data.examInfo;
				}
			},
			toExam(){
				//模拟考试
				let param = {
					paper_id:this.paper_id,
					exam_id:this.exam_id,
					num:this.review.num
				};
				navTo("/pages/exam/list",param);return;
				let that = this;
				if(this.examInfo.exam_exp){
					//showMsg('this',{title:'非考试时间,无法参与考试'});//
					//return;
				}
				uni.showModal({
					title: '提示',
					content: '确认要开始考试吗?',
					success(e) {
						if(e.confirm){
							//let url = `/pages/exam/exam?paperId=${paperIdVal.value}&title=${titleVal.value}&examType=0&examId=${examIdVal.value}`
							//if (review.value && review.value.num) url += `&num=${review.value.num}`
							//if (exam.value.examTime) url += `&timeLimit=${exam.value.examTime}`
							let param = {paper_id:that.paper_id,exam_id:that.exam_id};
							if(that.review?.num){
								param.num = that.review.num;
							}
							if(that.examInfo?.exam_time){
								param.exam_time = that.examInfo.exam_time;
							}
							rectTo('/pages/exam/index',param);
						}
					}
				});
			},
			toResult(result_type){
				let param = {
					paper_id:this.paper_id,
					exam_id:this.exam_id,
					type: result_type//0正考1模拟考
				};
				if(result_type>0){
					navTo("/pages/exam/simList",param);
				}else{
					navTo("/pages/exam/result",param);
				}
			},
			toSort(){
				//顺序练习
				let param = {
					paper_id:this.paper_id,
					exam_id:this.exam_id
				};
				navTo("/pages/exam/sort",param);
			},
			toRand(){
				//随机练习
				let param = {
					paper_id:this.paper_id,
					exam_id:this.exam_id
				};
				navTo("/pages/exam/rand",param);
			},
			toSim(){
				//模拟考试
				let param = {
					paper_id:this.paper_id,
					exam_id:this.exam_id,
					num:this.review.num
				};
				navTo("/pages/exam/simList",param);
			},
			toWrong(){
				navTo("/pages/promote/wrong",{exam_id:this.exam_id});
			}
		}
	}
</script>

<style>
	page{
		background-color: #ffffff;
	}
	.content{
		background-color: #ffffff;
		min-height: 100vh;
	}
	.nav-item{
		margin-left: 20rpx;
		margin-right: 20rpx;
		display: flex;
		align-items: center;
		height: 40px;
		border-radius: 10rpx;
	}
</style>
