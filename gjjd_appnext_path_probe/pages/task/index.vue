<template>
	<view>
		<view class="row-box">
			<view class="mar-B10">细目表-审核任务</view>
			<view class="dis-flex">
				<view class="task-item" @click="toXimu(0)">
					<view class="task-item-box">
						<view class="font-wb mar-B10">初审</view>
						<view class="font-s12">{{ximu.start}}个工种</view>
					</view>
				</view>
				<view class="task-item" @click="toXimu(1)">
					<view class="task-item-box">
						<view class="font-wb mar-B10">终审</view>
						<view class="font-s12">{{ximu.end}}个工种</view>
					</view>
				</view>
			</view>
		</view>
		<view class="row-box">
			<view class="mar-B10">题库-理论-审核任务</view>
			<view class="dis-flex">
				<view class="task-item" @click="toExam(0)">
					<view class="task-item-box">
						<view class="font-wb mar-B10">初审</view>
						<view class="font-s12">{{exam.start}}个工种</view>
					</view>
				</view>
				<view class="task-item" @click="toExam(1)">
					<view class="task-item-box">
						<view class="font-wb mar-B10">终审</view>
						<view class="font-s12">{{exam.end}}个工种</view>
					</view>
				</view>
			</view>
		</view>
		<view class="row-box">
			<view class="mar-B10">题库-实操-审核任务</view>
			<view class="dis-flex">
				<view class="task-item" @click="toExamshicao(0)">
					<view class="task-item-box">
						<view class="font-wb mar-B10">初审</view>
						<view class="font-s12">{{examshicao.start}}个工种</view>
					</view>
				</view>
				<view class="task-item" @click="toExamshicao(1)">
					<view class="task-item-box">
						<view class="font-wb mar-B10">终审</view>
						<view class="font-s12">{{examshicao.end}}个工种</view>
					</view>
				</view>
			</view>
		</view>
		<view class="row-box">
			<view class="mar-B10">题库-主观-审核任务</view>
			<view class="dis-flex">
				<view class="task-item" @click="toExamsub(0)">
					<view class="task-item-box">
						<view class="font-wb mar-B10">初审</view>
						<view class="font-s12">{{examsub.start}}个工种</view>
					</view>
				</view>
				<view class="task-item" @click="toExamsub(1)">
					<view class="task-item-box">
						<view class="font-wb mar-B10">终审</view>
						<view class="font-s12">{{examsub.end}}个工种</view>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import {request,uploads,makeCall,rectTo,navTo,showMsg} from "@/utils";
	export default {
		data() {
			return {
				ximu:{
					start:0,
					end:0
				},
				exam:{
					start:0,
					end:0
				},
				examshicao:{
					start:0,
					end:0
				},
				examsub:{
					start:0,
					end:0
				}
			}
		},
		onShow() {
			this.getData();
		},
		methods: {
			async getData(){
				uni.showLoading({
					title:'数据加载中'
				});
				let res = await request({
					method:'POST',
					data:{},
					url:"/apptask/index"
				});
				uni.hideLoading();
				console.log(res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg})
				}
				if(res?.data?.ximu){
					this.ximu = res.data.ximu;
				}
				if(res?.data?.exam){
					this.exam = res.data.exam;
				}
				if(res?.data?.examshicao){
					this.examshicao = res.data.examshicao;
				}
				if(res?.data?.examsub){
					this.examsub = res.data.examsub;
				}
			},
			toXimu(role){
				navTo("/pages/task/ximu",{role}); 
			},
			toExam(role){
				navTo("/pages/task/exam",{role});
			},
			toExamshicao(role){
				navTo("/pages/task/examshicao",{role});
			},
			toExamsub(role){
				navTo("/pages/task/examsub",{role});
			}
			
		}
	}
</script>

<style>
	page{
		background-color: #f8f8f8;
	}
	.row-box{
		width: 690rpx;
		margin-left: 20rpx;
		margin-right: 20rpx;
		padding: 10rpx;
		background-color: #fff;
		border-radius: 12rpx;
	}
	.task-item{
		width: 230rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}
	.task-item-box{
		width: 210rpx;
		padding-top: 10px;
		padding-bottom: 10px;
		border: 1rpx solid #dedede;
		border-radius: 8rpx;
		text-align: center;
	}
</style>