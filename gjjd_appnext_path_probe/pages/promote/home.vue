<template>
	<view class="content">
		<view class="header">
			<view class="header-box">
				<view :style="'height: '+$statusBarHeight+'px;'"><!--状态栏占位--></view>
				<view class="header-user" :style="'height: '+(160-$statusBarHeight)+'px;'">
					<view class="dis-flex-hv mar-L40 mar-R40">
						<view class="user-icon"><image src="/static/images/logo.jpg"></image></view>
						<view class="mar-L20 color-ff">{{user.realname}}</view>
					</view>
				</view>
			</view>
		</view>
		<view class="body" :style="'height:'+($winHeight-160-50)+'px;'">
			<view class="body-info">
				<view class="body-info-box">
					<view class="mar-B20">
						<text class="mar-R20">积分:{{user.point}}</text>
						<text class="mar-R20">财富值:{{user.wealthValue}}</text>
						<text>价值估值:{{user.valueValuation}}</text>
					</view>
					<view class="body-info-line"></view>
					<view class="mar-T20 dis-flex-hsb">
						<view class="body-info-item">
							<view class="text-C">
								<view><i class="gjjd-iconfont font-s20 icon-a-kecheng2 color-73a9fa"></i></view>
								<view class="mar-T5">课程</view>
							</view>
						</view>
						<view class="body-info-item">
							<view class="text-C">
								<view><i class="gjjd-iconfont font-s20 icon-record color-ff9f40"></i></view>
								<view class="mar-T5">学习记录</view>
							</view>
						</view>
						<view class="body-info-item">
							<view class="text-C">
								<view><i class="gjjd-iconfont font-s20 icon-kecheng- color-ff6b6b"></i></view>
								<view class="mar-T5">我的课程</view>
							</view>
						</view>
						<view class="body-info-item">
							<view class="text-C">
								<view><i class="gjjd-iconfont font-s20 icon-tijianbaogao color-ff69b4"></i></view>
								<view class="mar-T5">学习报告</view>
							</view>
						</view>
					</view>
				</view>
			</view>
			<view class="body-all-box" style="padding-top: 100px;">
				<view>全景图</view>
				<view class="dis-flex-hsb font-s14 hide">
					<view class="color-92">全部</view>
					<view><i class="gjjd-iconfont icon-xiangyou color-92"></i></view>
				</view>
			</view>
			<view class="all-box dis-flex-hsb">
				<view class="body-info-item">
					<view class="text-C" @click="toRace('path')">
						<view><i class="gjjd-iconfont font-s20 icon-lujing color-ff69b4"></i></view>
						<view class="mar-T5">学习路径</view>
					</view>
				</view>
				<view class="body-info-item">
					<view class="text-C" @click="toRace('map')">
						<view><i class="gjjd-iconfont font-s20 icon-dingwei color-57d801"></i></view>
						<view class="mar-T5">学习地图</view>
					</view>
				</view>
				<view class="body-info-item">
					<view class="text-C" @click="toRace('rate')">
						<view><i class="gjjd-iconfont font-s20 icon-rec-2 color-00abf4"></i></view>
						<view class="mar-T5">能力距阵</view>
					</view>
				</view>
				<view class="body-info-item" @click="toRace('task')">
					<view class="text-C">
						<view><i class="gjjd-iconfont font-s20 icon-jindutiao color-ff9f40"></i></view>
						<view class="mar-T5">任务进度</view>
					</view>
				</view>
			</view>
			<view class="body-list" :style="'height:'+($winHeight-160-100-30-60-50)+'px;'">
				<view class="body-list-row mar-T20">
					<view>培训任务</view>
					<view class="dis-flex-hsb font-s14" @click="toAll('train')">
						<view class="color-92">全部</view>
						<view><i class="gjjd-iconfont icon-xiangyou color-92"></i></view>
					</view>
				</view>
				<view class="task-item" v-for="item in traininglist" @click="goToTrainingDetail(item)">
					<view class="task-item-cover"><image src="/static/images/balance-bg.png"></image></view>
					<view class="task-item-content">{{item.classInfo.title}}</view>
				</view>
				<view class="body-list-row mar-T20">
					<view>复习任务</view>
					<view class="dis-flex-hsb font-s14" @click="toAll('review')">
						<view class="color-92">全部</view>
						<view><i class="gjjd-iconfont icon-xiangyou color-92"></i></view>
					</view>
				</view>
				<view class="task-item" v-for="item in reviewlist" @click="resultLink(item,1)">
					<view class="task-item-icon border-73a9fa">
						<i class="gjjd-iconfont icon-bianji1 font-s30 color-73a9fa"></i>
					</view>
					<view class="task-item-content">{{item.title}}</view>
				</view>
				<view class="body-list-row mar-T20">
					<view>考试任务</view>
					<view class="dis-flex-hsb font-s14" @click="toAll('exam')">
						<view class="color-92">全部</view>
						<view><i class="gjjd-iconfont icon-xiangyou color-92"></i></view>
					</view>
				</view>
				<view class="task-item" v-for="item in examlist" @click="resultLink(item,0)">
					<view class="task-item-icon border-73a9fa">
						<i class="gjjd-iconfont icon-bianji1 font-s30 color-73a9fa"></i>
					</view>
					<view class="task-item-content">{{item.title}}</view>
				</view>
				<view class="body-list-row mar-T20 hide">
					<view>竞赛任务</view>
					<view class="dis-flex-hsb font-s14" @click="toAll('match')">
						<view class="color-92">全部</view>
						<view><i class="gjjd-iconfont icon-xiangyou color-92"></i></view>
					</view>
				</view>
				<view class="task-item" v-for="item in matchlist" @click="toMatch(item)">
					<view class="task-item-icon border-73a9fa">
						<i class="gjjd-iconfont icon-bianji1 font-s30 color-73a9fa"></i>
					</view>
					<view class="task-item-content">{{item.title}}</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import {request, navTo, showMsg} from "@/utils";
	export default {
		data() {
			return {
				user:{id:0,realname:'',point:0,wealthValue:0,valueValuation:0},
				reviewlist:[],
				examlist:[],
				traininglist:[],
				trainingplanlist:[],
				matchlist:[],
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
					url:"/apppromote/home"
				});
				console.log("promote home",res)
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
				if(res?.data?.user){
					this.user = res.data.user;
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
				if(res?.data?.matchlist){
					this.matchlist = res.data.matchlist;
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
				navTo('/pages/promote/train', {id: item.id});
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
			},
			toAll(type){
				navTo('/pages/promote/taskList', {type});
			},
			toMatch(item){
				navTo('/pages/competition/home/detail',{id:item.id});
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
		height: 160px;
	}
	.header-box{
		position: relative;
		width: 750rpx;
		height: 160px;
	}
	.header-user{
		display: flex;
		align-items: center;
	}
	.user-icon{
		width: 100rpx;
		height: 100rpx;
	}
	.user-icon image{
		width: 100rpx;
		height: 100rpx;
		border-radius: 50rpx;
	}
	.body{
		width: 750rpx;
		position: relative;
	}
	.body-info{
		position: absolute;
		top: -20px;
		left: 0;
		right: 0;
		z-index: 999;
		height: 100px;
	}
	.body-info-box{
		margin-left: 20rpx;
		margin-right: 20rpx;
		padding: 10px;
		background-color: #fff;
		border-radius: 12rpx;
	}
	.body-info-line{
		height: 1px;
		border-bottom: 1rpx solid #ccc;
	}
	.body-info-item{
		width: 200rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 14px;
	}
	.body-all-box{
		width: 710rpx;
		height: 30px;
		padding: 0 20rpx;
		display: flex;
		justify-content: space-between;
	}
	.all-box{
		background-color: #fff;
		border-radius: 12rpx;
		height: 60px;
	}
	.body-list{
		overflow-x: hidden;
		overflow-y: auto;
		padding-left: 20rpx;
		padding-right: 20rpx;
	}
	.body-list::-webkit-scrollbar {
	  display: none;
	}
	.body-list-row{
		width: 710rpx;
		height: 30px;
		line-height: 30px;
		display: flex;
		justify-content: space-between;
	}
	.task-item{
		display: flex;
		margin-top: 10px;
	}
	.task-item-cover{
		width: 260rpx;
		height: 80px;
	}
	.task-item-cover image{
		width: 260rpx;
		height: 80px;
		background-color: #ccc;
		border-top-left-radius: 10rpx;
		border-bottom-left-radius: 10rpx;
	}
	.task-item-icon{
		width: 120rpx;
		padding: 10px 0;
		text-align: center;
		background-color: #fff;
		border-top-left-radius: 10rpx;
		border-bottom-left-radius: 10rpx;
	}
	.task-item-content{
		flex: 1;
		padding: 5px 10rpx;
		background-color: #fff;
		border-top-right-radius: 10rpx;
		border-bottom-right-radius: 10rpx;
	}
</style>