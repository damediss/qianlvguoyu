<template>
	<view class="content">
		<!-- 任务状态 -->
		<view class="task-status bg-ff">
			<view class="task-status-header dis-flex-hsb dis-flex-vc font-s16 font-wb" @click="goToTaskStatus">
				<view>任务状态</view>
				<view class="hide"><i class="gjjd-iconfont icon-xiangyou color-92"></i></view>
			</view>
			<view class="task-item bg-ff border-62 radius-8">
				<view class="dis-flex-hsb">
					<view class="task-label dis-flex-vc font-s14 color-00 font-wb">当前培训作业学时统计(小时)</view>
					<view class="task-value dis-flex-vc font-s16 font-wb study-hours color-1fc92e">{{taskStatus.studyHours}}</view>
				</view>
			</view>
			<view class="task-item bg-ff border-62 radius-8">
				<view class="dis-flex-hsb">
					<view class="task-label dis-flex-vc font-s14 color-00 font-wb">当前培训作业完成比例</view>
					<view class="task-value dis-flex-vc font-s16 font-wb completion-rate dis-flex-vc">
						<view class="progress-container dis-flex-hv">
							<l-circle 
							  :percent="parseFloat(taskStatus.completionRate)" 
							  size="85rpx" 
							  :strokeWidth="3" 
							  :trailWidth="2"
							  strokeColor="#4CAF50"
							  trailColor="#E0E0E0"
							  lineCap="round">
								<!-- 总是显示百分比文字 -->
								<view class="progress-text font-s12 color-4ad356 font-wb">{{taskStatus.completionRate}}%</view>
							</l-circle>
						</view>
					</view>
				</view>
			</view>
		</view>
		<!-- 全景图 -->
		<view class="row-box bg-ff">
		<view class="mar-B10 section-title font-wb">全景图</view>
			<view class="dis-flex-hsb mar-B10">
				<view class="all-path-btn green-bg-0 dis-flex-hv radius-8" @click="toRace('path')">
					<image src="/static/images/race-1.png" class="path-icon" mode="aspectFit"></image>
					<view>学习路径</view>
				</view>
				<view class="all-path-btn blue-bg-0 dis-flex-hv radius-8" @click="toRace('map')">
					<image src="/static/images/race-4.png" class="path-icon" mode="aspectFit"></image>
					<view>学习地图</view>
				</view>
			</view>
			<view class="dis-flex-hsb">
				<view class="all-path-btn blue-bg-0 dis-flex-hv radius-8" @click="toRace('rate')">
					<image src="/static/images/race-2.png" class="path-icon" mode="aspectFit"></image>
					<view>能力距阵</view>
				</view>
				<view class="all-path-btn green-bg-0 dis-flex-hv radius-8" @click="toRace('task')">
					<image src="/static/images/race-3.png" class="path-icon" mode="aspectFit"></image>
					<view>任务进度条</view>
				</view>
			</view>
		</view>
		<!-- 通知 -->
		<view class="row-box bg-ff">
			<view class="mar-B10 section-title font-wb">通知</view>
		</view>
		<!-- 复习任务 -->
		<view class="row-box bg-ff">
			<view class="mar-B10 section-title font-wb">复习任务({{reviewlist.length}})</view>
			<template v-if="reviewlist.length>0">
				<view class="task-fuxi-item border-62 radius-12" v-for="item in reviewlist" :key="item.id" @click="resultLink(item,1)">
					<view class="font-wb">{{item.title}}</view>
					<view class="font-s12 color-92 mar-T5">
						复习截止日期:
						<text class="mar-L10">{{item.exam_date}}</text>
					</view>
					<view class="font-s12 color-92 mar-T5">
						复习次数:
						<text class="mar-L10">{{item.trainExamPeopleInfo.learn_time}}</text>
					</view>
					<view class="font-s12 color-92 mar-T5">
						复习时长:
						<text class="mar-L10">{{Math.floor(item.trainExamPeopleInfo.learn_use_time / 3600)}}小时{{Math.floor(item.trainExamPeopleInfo.learn_use_time % 3600 / 60)}}分钟{{Math.floor(item.trainExamPeopleInfo.learn_use_time % 60)}}秒</text>
					</view>
				</view>
			</template>
		</view>
		<!-- 考试任务 -->
		<view class="row-box bg-ff">
			<view class="mar-B10 section-title font-wb">考试任务({{examlist.length}})</view>
			<template v-if="examlist.length>0">
				<view class="task-exam-item dis-flex-hsb border-62 radius-12" v-for="item in examlist" :key="item.id" @click="resultLink(item,0)">
					<view>
						<view class="font-wb">{{item.title}}</view>
						<view class="font-s12 color-92 mar-T5">
							考试类型:
							<text v-if="item.type>0" class="mar-L10">线下</text>
							<text v-else class="mar-L10">线上</text>
						</view>
						<view class="font-s12 color-92 mar-T5">
							考试截止日期:
							<text class="mar-L10">{{item.exam_date}}</text>
						</view>
						<view class="font-s12 color-92 mar-T5" v-if="item.exam_result>0">
							考试得分:
							<text class="mar-L10">{{item.trainExamPeopleInfo.score}}</text>
						</view>
						<view class="font-s12 color-92 mar-T5" v-if="item.exam_result>0">
							<text class="mar-L10 color-green" v-if="item.trainExamPeopleInfo.score>=60">合格</text>
							<text class="mar-L10 color-red" v-else>不合格</text>
						</view>
					</view>
					<view>
						<l-circle 
						  :percent="item.exam_result>0?100:0" 
						  size="100rpx" 
						  :strokeWidth="4" 
						  :trailWidth="4">
							<text class="font-s10" v-if="item.trainExamPeopleInfo.real_score>0">完成</text>
							<text class="font-s10" v-else>未完成</text>
						</l-circle>
					</view>
				</view>
			</template>
		</view>
		<!-- 培训任务 -->
		<view class="row-box bg-ff">
			<view class="mar-B10 section-title font-wb">培训任务</view>
			<template v-if="traininglist.length>0">
				<view class="training-item" v-for="item in traininglist" :key="item.id" @click="goToTrainingDetail(item)">
					<view class="training-card radius-12">
						<view class="training-title color-ff font-wb">{{item.title}}</view>
						<view class="training-info">
							<view class="training-date color-ff">开始日期:{{item.date}}</view>
							<view class="training-hours color-ff">已学:{{item.learned_hours}}个学时</view>
						</view>
					</view>
				</view>
			</template>
			<template v-else>
				<view class="empty-state dis-flex flex-column dis-flex-hv">
					<i class="gjjd-iconfont icon-wenjian color-92 font-s40"></i>
					<view class="empty-text color-99">暂无培训任务</view>
				</view>
			</template>
		</view>
		<!-- 培训计划 -->
		<view class="row-box bg-ff hide">
			<view class="mar-B10 section-title font-wb">培训计划</view>
			<template v-if="trainingplanlist.length>0">
				<view class="plan-item border-f0" v-for="item in trainingplanlist" :key="item.id" @click="goToPlanDetail(item)">
					<view class="plan-title color-33 font-wb">{{item.title}}</view>
					<view class="plan-info color-66">{{item.description}}</view>
				</view>
			</template>
			<template v-else>
				<view class="empty-state dis-flex flex-column dis-flex-hv">
					<i class="gjjd-iconfont icon-wenjian color-92 font-s40"></i>
					<view class="empty-text color-99">暂无培训计划</view>
				</view>
			</template>
		</view>
		<!-- 竞赛任务 -->
		<view class="row-box bg-ff hide">
			<view class="mar-B10 section-title font-wb">竞赛任务</view>
			<template v-if="competitionlist.length>0">
			<view class="competition-item dis-flex-hsb dis-flex-vc border-f0" v-for="item in competitionlist" :key="item.id" @click="goToCompetitionDetail(item)">
				<view class="competition-title color-33 font-wb">{{item.title}}</view>
				<view class="competition-info color-66">{{item.participants}}人</view>
				<view class="competition-time color-99">竞赛时间:{{item.start_date}} ~ {{item.end_date}}</view>
				<view class="competition-status dis-flex-vc">
					<view class="progress-circle dis-flex-hv bg-4caf50 radius-full">
						<text class="status-text color-ff font-wb">{{item.status}}</text>
					</view>
				</view>
			</view>
			</template>
			<template v-else>
				<view class="empty-state dis-flex flex-column dis-flex-hv">
					<i class="gjjd-iconfont icon-wenjian color-92 font-s40"></i>
					<view class="empty-text color-99">暂无竞赛任务</view>
				</view>
			</template>
		</view>
		<!-- 申报任务 -->
		<view class="row-box bg-ff hide">
			<view class="mar-B10 section-title font-wb">申报任务</view>
			<template v-if="declarationlist.length>0">
			<view class="declaration-item dis-flex-vc border-f0" v-for="item in declarationlist" :key="item.id" @click="goToDeclarationDetail(item)">
				<view class="declaration-title color-33 font-wb">{{item.title}}</view>
				<view class="declaration-info color-66">{{item.participants}}人</view>
				<view class="declaration-status dis-flex-vc">
					<view class="declaration-progress-bar bg-f0">
						<view class="declaration-progress-fill bg-73a9fa" :style="'width:'+item.progress+'%'"></view>
					</view>
					<view class="declaration-status-text color-99" :class="item.status === '进行中' ? 'declaration-status-active color-4caf50' : ''">{{item.status}}</view>
				</view>
			</view>
			</template>
			<template v-else>
				<view class="empty-state dis-flex flex-column dis-flex-hv">
					<i class="gjjd-iconfont icon-wenjian color-92 font-s40"></i>
					<view class="empty-text color-99">暂无申报任务</view>
				</view>
			</template>
		</view>
		<!-- 调研任务 -->
		<view class="row-box bg-ff hide">
			<view class="mar-B10 section-title font-wb">调研任务</view>
			<template v-if="researchlist.length>0">
			<view class="research-item dis-flex-vc border-f0" v-for="item in researchlist" :key="item.id" @click="goToResearchDetail(item)">
				<view class="research-title color-33 font-wb">{{item.title}}</view>
				<view class="research-info color-66">{{item.participants}}人</view>
				<view class="research-status dis-flex-vc">
					<view class="research-progress-bar bg-f0">
						<view class="research-progress-fill bg-73a9fa" :style="'width:'+item.progress+'%'"></view>
					</view>
					<view class="research-status-text color-99" :class="item.status === '进行中' ? 'research-status-active color-4caf50' : ''">{{item.status}}</view>
				</view>
			</view>
			</template>
			<template v-else>
				<view class="empty-state dis-flex flex-column dis-flex-hv">
					<i class="gjjd-iconfont icon-wenjian color-92 font-s40"></i>
					<view class="empty-text color-99">暂无调研任务</view>
				</view>
			</template>
		</view>
	</view>
</template>

<script>
	import {request, navTo, showMsg} from "@/utils";
	export default {
		data() {
			return {
				reviewlist:[],
				examlist:[],
				traininglist:[],
				trainingplanlist:[],
				competitionlist:[],
				declarationlist:[],
				researchlist:[],
				taskStatus: {
					studyHours: '0.00',
					completionRate: '0.00'
				}
			}
		},
		onLoad() {
			// this.$util.checkLogin();
			//this.getData();
		},
		onShow() {
			this.getData();
		},
		onPullDownRefresh() {
			// 下拉刷新时重新获取数据
			this.getData(true).then(() => {
				// 停止下拉刷新动画
				uni.stopPullDownRefresh();
			});
		},
		methods: {
			async getData(isRefresh = false){
				// 如果不是下拉刷新，则显示loading
				if (!isRefresh) {
					uni.showLoading({
						title:'数据加载中...'
					});
				}
				let res = await request({
					method:'POST',
					data:{},
					url:"/apppromote/index"
				});
				console.log("promote index",res)
				// 如果不是下拉刷新，则隐藏loading
				if (!isRefresh) {
					uni.hideLoading();
				}
				if(res.code){
					let msg = '请求失败';
					if(res?.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg})
				}
				if(res?.data?.reviewlist){
					this.reviewlist = res.data.reviewlist;
				}
				if(res?.data?.examlist){
					this.examlist = res.data.examlist;
				}
				if(res?.data?.traininglist){
					this.traininglist = res.data.traininglist;
				}
				if(res?.data?.trainingplanlist){
					this.trainingplanlist = res.data.trainingplanlist;
				}
				if(res?.data?.competitionlist){
					this.competitionlist = res.data.competitionlist;
				}
				if(res?.data?.declarationlist){
					this.declarationlist = res.data.declarationlist;
				}
				if(res?.data?.researchlist){
					this.researchlist = res.data.researchlist;
				}
				if(res?.data?.taskStatus){
					this.taskStatus = {
						studyHours: res.data.taskStatus.studyHours || '0.00',
						completionRate: res.data.taskStatus.completionRate || '0.00'
					};
				}
			},
			resultLink(item,resultType){
				if(item.type == 1) return;//线下
				navTo('/pages/promote/exam',{paper_id:item.paper_id,exam_id:item.id,has_review:item.hasReview,resultType});
			},
			toRace(path){
				navTo('/pages/promote/'+path);
			},
			goToTaskStatus(){
				return;
				navTo('/pages/promote/learn');
			},
			goToTrainingDetail(item){
				console.log("training detail",item);
				navTo('/pages/promote/trainingDetail', {id: item.id});
			},
			goToPlanDetail(item){
				navTo('/pages/promote/planDetail', {id: item.id});
			},
			goToCompetitionDetail(item){
				navTo('/pages/competition/detail', {id: item.id});
			},
			goToDeclarationDetail(item){
				navTo('/pages/promote/declarationDetail', {id: item.id});
			},
			goToResearchDetail(item){
				navTo('/pages/promote/researchDetail', {id: item.id});
			}
		}
	}
</script>

<style>
	page{
		background-color: #f1f1f1;
	}
	.task-status{
		width: 750rpx;
		padding-top: 20rpx;
		padding-bottom: 20rpx;
		margin-bottom: 20rpx;
	}
	.task-status-header{
		margin: 0 20rpx 20rpx 20rpx;
		padding: 20rpx 0;
	}
	.task-item{
		width: 670rpx;
		margin: 30rpx 20rpx;
		padding: 30rpx 20rpx;
	}
	.task-label{
		flex: 1;
	}
	.progress-container{
		position: relative;
		width: 85rpx;
		height: 85rpx;
	}
	.progress-text{
		text-align: center;
		line-height: 1;
		z-index: 2;
	}
	.row-box{
		margin-top: 20rpx;
		width: 710rpx;
		padding: 20rpx;
	}
	.all-path-btn{
		width: 320rpx;
		height: 110rpx;
	}
	.path-icon{
		width: 50rpx;
		height: 50rpx;
		margin-right: 20rpx;
	}
	.green-bg-0{
		background: url("/static/images/green-bg-0.png");
		background-size: cover;
	}
	.blue-bg-0{
		background: url("/static/images/blue-bg-0.png");
		background-size: cover;
	}
	.task-fuxi-item{
		width: 670rpx;
		margin-top: 20rpx;
		padding: 30rpx 20rpx;
	}
	.task-exam-item{
		width: 670rpx;
		margin-top: 20rpx;
		padding: 30rpx 20rpx;
	}
	.section-title{
		font-weight: bold;
	}
	.training-item{
		margin-bottom: 20rpx;
	}
	.training-card{
		background: linear-gradient(135deg, #ff6b9d, #c44569, #f8b500);
		padding: 30rpx;
		position: relative;
		overflow: hidden;
	}
	.training-card::before{
		content: '';
		position: absolute;
		top: 0;
		left: 0;
		right: 0;
		bottom: 0;
		background-image: radial-gradient(circle, rgba(255,255,255,0.3) 1px, transparent 1px);
		background-size: 40rpx 40rpx;
		pointer-events: none;
	}
	.training-title{
		font-size: 28rpx;
		margin-bottom: 20rpx;
		position: relative;
		z-index: 1;
	}
	.training-info{
		position: relative;
		z-index: 1;
	}
	.training-date{
		font-size: 24rpx;
		margin-bottom: 10rpx;
	}
	.training-hours{
		font-size: 24rpx;
	}
	.plan-item{
		padding: 20rpx;
	}
	.plan-item:not(:last-child){
		border-bottom: 1rpx solid #f0f0f0;
	}
	.plan-title{
		font-size: 28rpx;
		margin-bottom: 10rpx;
	}
	.plan-info{
		font-size: 24rpx;
	}
	.competition-item{
		padding: 20rpx;
	}
	.competition-item:not(:last-child){
		border-bottom: 1rpx solid #f0f0f0;
	}
	.competition-title{
		font-size: 28rpx;
		margin-bottom: 10rpx;
	}
	.competition-info{
		font-size: 24rpx;
		margin-bottom: 5rpx;
	}
	.competition-time{
		font-size: 22rpx;
	}
	.progress-circle{
		width: 60rpx;
		height: 60rpx;
	}
	.status-text{
		font-size: 20rpx;
	}
	.empty-state{
		padding: 80rpx 40rpx;
	}
	.empty-text{
		font-size: 24rpx;
		margin-top: 20rpx;
	}
	.declaration-item{
		padding: 30rpx 40rpx;
	}
	.declaration-item:not(:last-child){
		border-bottom: 1rpx solid #f0f0f0;
	}
	.declaration-title{
		flex: 1;
		font-size: 28rpx;
		margin-bottom: 10rpx;
	}
	.declaration-info{
		font-size: 24rpx;
		margin-bottom: 15rpx;
	}
	.declaration-status{
		width: 200rpx;
	}
	.declaration-progress-bar{
		flex: 1;
		height: 8rpx;
		border-radius: 4rpx;
		margin-right: 20rpx;
		overflow: hidden;
	}
	.declaration-progress-fill{
		height: 100%;
		border-radius: 4rpx;
		transition: width 0.3s ease;
	}
	.declaration-status-text{
		font-size: 20rpx;
		white-space: nowrap;
	}
	.declaration-status-active{
		font-weight: bold;
	}
	.research-item{
		padding: 30rpx 40rpx;
	}
	.research-item:not(:last-child){
		border-bottom: 1rpx solid #f0f0f0;
	}
	.research-title{
		flex: 1;
		font-size: 28rpx;
		margin-bottom: 10rpx;
	}
	.research-info{
		font-size: 24rpx;
		margin-bottom: 15rpx;
	}
	.research-status{
		width: 200rpx;
	}
	.research-progress-bar{
		flex: 1;
		height: 8rpx;
		border-radius: 4rpx;
		margin-right: 20rpx;
		overflow: hidden;
	}
	.research-progress-fill{
		height: 100%;
		border-radius: 4rpx;
		transition: width 0.3s ease;
	}
	.research-status-text{
		font-size: 20rpx;
		white-space: nowrap;
	}
	.research-status-active{
		font-weight: bold;
	}
</style>
