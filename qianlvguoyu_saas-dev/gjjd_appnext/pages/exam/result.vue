<template>
	<view class="content">
	<view class="header font-s12">
		<view class="mar-B10 color-ff">考试得分:<text class="mar-L10">{{exam.total.score}}</text>分</view>
		<view class="mar-B10 color-ff">答题人:<text class="mar-L10">{{paper.realname}}</text></view>
		<view class="mar-B10 color-ff">交卷时间:<text class="mar-L10">{{paper.jiaojuan_time}}</text></view>
		<view class="mar-B10 color-ff">
			本试卷共有<text class="mar-L10">{{paper.exam_num}}</text>题，总分<text>{{paper.total_point}}</text>分，合格分数：<text>{{paper.pass_point}}</text>分，答题时间：<text>{{paper.time}}</text>分钟
		</view>
	</view>
		<view class="body font-s12">
			<view class="row-item border-bottom-dashed">
				<view class="exam-type">题型(题数)</view>
				<view class="exam-right text-C">正确题数</view>
				<view class="exam-score text-R">得分</view>
			</view>
			<view class="row-item">
				<view class="exam-type">单选题({{exam.single.num}})</view>
				<view class="exam-right text-C">{{exam.single.isright}}</view>
				<view class="exam-score color-73a9fa text-R">{{exam.single.score}}</view>
			</view>
			<view class="row-item">
				<view class="exam-type">多选题({{exam.multi.num}})</view>
				<view class="exam-right text-C">{{exam.multi.isright}}</view>
				<view class="exam-score color-73a9fa text-R">{{exam.multi.score}}</view>
			</view>
			<view class="row-item">
				<view class="exam-type">判断题({{exam.judge.num}})</view>
				<view class="exam-right text-C">{{exam.judge.isright}}</view>
				<view class="exam-score color-73a9fa text-R">{{exam.judge.score}}</view>
			</view>
			<view class="row-item">
				<view class="exam-type">总分</view>
				<view class="exam-score color-73a9fa text-R">{{exam.total.score}}</view>
			</view>
		</view>
		<view class="footer">
			<view @click="toDetail">查看答题详情</view>
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
				result_type:0,//0正式 1模拟考 2能力测评 3顺序练习 4随机练习
				exam:{
					single:{num:0,isright:0,score:0},
					multi:{num:0,isright:0,score:0},
					judge:{num:0,isright:0,score:0},
					total:{score:0}
				},
				paper:{
					id:0,
					time:0,
					total_point:0,
					pass_point:0,
					realname:0,
					jiaojuan_time:'',
					exam_num:0,
				}
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
				this.getexamList();
			}
		},
		methods: {
			async getexamList(){
				uni.showLoading({
					title:'数据加载中...'
				});
				let res = await request({
					method:'POST',
					data:{paper_id:this.paper_id,exam_id:this.exam_id,type:this.result_type},
					url:"/apppromote/examresult"
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
				if(res?.data?.exam){
					this.exam = res.data.exam;
				}
				if(res?.data?.paper){
					this.paper = res.data.paper;
				}
			},
			toDetail(){
				let param = {
					paper_id:this.paper_id,
					exam_id:this.exam_id,
					type: this.result_type//0正式 1模拟考
				};
				navTo("/pages/exam/resultDetail",param);
			}
		}
	}
</script>

<style>
	page{
		background-color: #f8f8f8;
	}
	.header{
		background-color: #73a9fa;
		border-bottom-left-radius: 60rpx;
		border-bottom-right-radius: 60rpx;
		padding-top: 10px;
		padding-bottom: 40px;
		padding-left: 30rpx;
		padding-right: 30rpx;
	}
	.body{
		margin-top: -30px;
		margin-left: 30rpx;
		margin-right: 30rpx;
		padding: 10px 20rpx;
		background-color: #fff;
		border-radius: 12rpx;
	}
	.border-bottom-dashed{
		border-bottom: 2rpx dashed #ddd;
	}
	.row-item{
		display: flex;
		align-items: center;
		justify-content: space-between;
		height: 40px;
	}
	.exam-type{
		width: 50%;
	}
	.exam-right{
		width: 20%;
	}
	.exam-score{
		width: 20%;
	}
	.footer{
		position: fixed;
		bottom: 10px;
		left: 0;
		right: 0;
		z-index: 999;
		text-align: center;
	}
	.footer view{
		margin: 0 20rpx;
		height: 40px;
		line-height: 40px;
		text-align: center;
		background-color: #73a9fa;
		border-radius: 20px;
		color: #fff;
	}
</style>
