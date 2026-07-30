<template>
	<view class="content">
		<!-- 培训任务 -->
		<view class="row-box">
		<view class="section-header dis-flex-hsb dis-flex-vc" @click="goToTraining">
			<view class="section-title">培训任务</view>
			<view class="section-arrow">
				<i class="gjjd-iconfont icon-xiangyou"></i>
			</view>
		</view>
		<template v-if="traininglist.length>0">
		<view class="task-item dis-flex-vc" v-for="item in traininglist" @click="goToTrainingDetail(item)">
			<view class="task-title">{{item.title}}</view>
			<view class="task-info">{{item.participants}}人</view>
			<view class="task-status dis-flex-vc">
				<view class="progress-bar">
					<view class="progress-fill" :style="'width:'+item.progress+'%'"></view>
				</view>
				<view class="status-text" :class="item.status === '进行中' ? 'status-active' : ''">{{item.status}}</view>
			</view>
		</view>
		</template>
		<template v-else>
			<view class="empty-state dis-flex flex-column dis-flex-hv">
				<i class="gjjd-iconfont icon-wenjian color-92 font-s40"></i>
				<view class="empty-text">暂无培训任务</view>
			</view>
		</template>
		</view>

		<!-- 考试任务 -->
		<view class="row-box">
		<view class="section-header dis-flex-hsb dis-flex-vc" @click="goToExam">
			<view class="section-title">考试任务</view>
			<view class="section-arrow">
				<i class="gjjd-iconfont icon-xiangyou"></i>
			</view>
		</view>
		<template v-if="loading">
			<view class="loading-state dis-flex flex-column dis-flex-hv">
				<view class="loading-icon">
					<i class="gjjd-iconfont icon-jiazai color-ff font-s20"></i>
				</view>
				<view class="loading-text">数据加载中</view>
			</view>
		</template>
		<template v-else-if="examlist.length>0">
		<view class="task-item dis-flex-vc" v-for="item in examlist" @click="resultLink(item,0)">
			<view class="task-title">{{item.title}}</view>
			<view class="task-info">{{item.participants}}人</view>
				<view class="task-status dis-flex-vc">
					<view class="progress-bar">
						<view class="progress-fill" :style="'width:'+item.progress+'%'"></view>
					</view>
					<view class="status-text" :class="item.status === '进行中' ? 'status-active' : ''">{{item.status}}</view>
				</view>
			</view>
		</template>
		<template v-else>
			<view class="empty-state dis-flex flex-column dis-flex-hv">
				<i class="gjjd-iconfont icon-wenjian color-92 font-s40"></i>
				<view class="empty-text">暂无考试任务</view>
			</view>
		</template>
		</view>

		<!-- 竞赛任务 -->
		<view class="row-box">
		<view class="section-header dis-flex-hsb dis-flex-vc" @click="goToCompetition">
			<view class="section-title">竞赛任务</view>
			<view class="section-arrow">
				<i class="gjjd-iconfont icon-xiangyou"></i>
			</view>
		</view>
		<template v-if="competitionlist.length>0">
			<view class="task-item dis-flex-vc" v-for="item in competitionlist" @click="goToCompetitionDetail(item)">
			<view class="task-title">{{item.title}}</view>
			<view class="task-info">{{item.participants}}人</view>
			<view class="task-status dis-flex-vc">
					<view class="progress-bar">
						<view class="progress-fill" :style="'width:'+item.progress+'%'"></view>
					</view>
					<view class="status-text" :class="item.status === '进行中' ? 'status-active' : ''">{{item.status}}</view>
				</view>
			</view>
		</template>
		<template v-else>
			<view class="empty-state dis-flex flex-column dis-flex-hv">
				<i class="gjjd-iconfont icon-wenjian color-92 font-s40"></i>
				<view class="empty-text">暂无竞赛任务</view>
			</view>
		</template>
		</view>
	</view>
</template>

<script>
	import {request,uploads,rectTo,navTo,showMsg} from "@/utils";
	export default {
		data() {
			return {
				examlist:[],
				traininglist:[],
				competitionlist:[],
				loading: true
			}
		},
		onLoad() {
			this.getData();
		},
		methods: {
			async getData(){
				this.loading = true;
			uni.showLoading({
				title:'数据加载中...'
			});
				let res = await request({
					method:'POST',
					data:{},
					url:"/apppromote/task"
				});
				uni.hideLoading();
				this.loading = false;
				console.log('promote task getData',res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg})
					return;
				}
				if(res?.data?.examlist){
					this.examlist = res.data.examlist;
				}
				if(res?.data?.traininglist){
					this.traininglist = res.data.traininglist;
				}
				if(res?.data?.competitionlist){
					this.competitionlist = res.data.competitionlist;
				}
			},
			resultLink(item,resultType){
				if(item.type == 1) return;//线下
				navTo('/pages/promote/exam',{paper_id:item.paper_id,exam_id:item.id,has_review:item.hasReview,resultType});
			},
		goToTraining(){
			// 跳转到培训任务列表页面
			navTo('/pages/promote/training');
		},
			goToExam(){
				// 跳转到考试任务列表页面
				navTo('/pages/promote/examList');
			},
		goToCompetition(){
			// 跳转到技能矩阵图页面
			navTo('/pages/competition/home/index');
		},
		goToTrainingDetail(item){
			// 跳转到培训任务详情页面
			navTo('/pages/promote/trainingDetail', {id: item.id});
		},
		goToCompetitionDetail(item){
			// 跳转到竞赛任务详情页面
			navTo('/pages/competition/detail', {id: item.id});
		}
		}
	}
</script>

<style>
	page{
		background-color: #f1f1f1;
	}
	.content{
		padding: 20rpx;
	}
	.row-box{
		margin-bottom: 20rpx;
		background-color: #fff;
		border-radius: 12rpx;
		overflow: hidden;
	}
	.section-header{
		padding: 30rpx 40rpx;
		border-bottom: 1rpx solid #f0f0f0;
	}
	.section-title{
		font-size: 32rpx;
		font-weight: bold;
		color: #333;
	}
	.section-arrow{
		color: #999;
		font-size: 24rpx;
	}
	.task-item{
		padding: 30rpx 40rpx;
		border-bottom: 1rpx solid #f0f0f0;
	}
	.task-item:last-child{
		border-bottom: none;
	}
	.task-title{
		flex: 1;
		font-size: 28rpx;
		color: #333;
		font-weight: bold;
		margin-bottom: 10rpx;
	}
	.task-info{
		font-size: 24rpx;
		color: #666;
		margin-bottom: 15rpx;
	}
	.task-status{
		width: 200rpx;
	}
	.progress-bar{
		flex: 1;
		height: 8rpx;
		background-color: #f0f0f0;
		border-radius: 4rpx;
		margin-right: 20rpx;
		overflow: hidden;
	}
	.progress-fill{
		height: 100%;
		background-color: #73a9fa;
		border-radius: 4rpx;
		transition: width 0.3s ease;
	}
	.status-text{
		font-size: 20rpx;
		color: #999;
		white-space: nowrap;
	}
	.status-active{
		color: #4CAF50;
		font-weight: bold;
	}
	.empty-state{
		padding: 80rpx 40rpx;
	}
	.empty-text{
		font-size: 24rpx;
		color: #999;
		margin-top: 20rpx;
	}
	.loading-state{
		padding: 80rpx 40rpx;
		background-color: #666;
		margin: 20rpx;
		border-radius: 12rpx;
	}
	.loading-icon{
		animation: rotate 1s linear infinite;
	}
	.loading-text{
		font-size: 24rpx;
		color: #fff;
		margin-top: 20rpx;
	}
	@keyframes rotate {
		from { transform: rotate(0deg); }
		to { transform: rotate(360deg); }
	}
</style>
