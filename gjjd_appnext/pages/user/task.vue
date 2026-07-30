<template>
	<view class="content">
		<view class="row-box">
			<view class="mar-B10 dis-flex-hsb" @click="toPage('/pages/promote/index')">
				<view>复习任务({{reviewlist.length}})</view>
				<view><i class="gjjd-iconfont icon-xiangyou"></i></view>
			</view>
			<template v-if="reviewlist.length>0">
				<view class="task-box" v-for="item in reviewlist" @click="toPage('/pages/promote/index')">
					<view class="font-wb">{{item.title}}</view>
					<view class="color-62 font-s14">
						复习截止日期:
						<text class="mar-L10">{{item.exam_date}}</text>
					</view>
					<view class="color-62 font-s14">
						复习次数:
						<text class="mar-L10">{{item.trainExamPeopleInfo.learn_time}}</text>
					</view>
				<view class="color-62 font-s14">
					复习时长:
					<text class="mar-L10">{{Math.floor(item.trainExamPeopleInfo.learn_use_time / 3600)}}小时{{Math.floor(item.trainExamPeopleInfo.learn_use_time % 3600 / 60)}}分钟{{Math.floor(item.trainExamPeopleInfo.learn_use_time % 60)}}秒</text>
				</view>
				</view>
			</template>
			<template v-else>
				<view class="text-C color-62">您当前还没有复习任务</view>
			</template>
		</view>
		<view class="row-box mar-T10">
			<view class="mar-B10 dis-flex-hsb" @click="toPage('/pages/promote/index')">
				<view>考试任务({{examlist.length}})</view>
				<view><i class="gjjd-iconfont icon-xiangyou"></i></view>
			</view>
			<template v-if="examlist.length>0">
				<view class="task-box" v-for="item in examlist" @click="toPage('/pages/promote/index')">
					<view class="font-wb">{{item.title}}</view>
					<view class="color-62 font-s14">
						考试类型:
						<text v-if="item.type>0" class="mar-L10">线下</text>
						<text v-else class="mar-L10">线上</text>
					</view>
					<view class="color-62 font-s14">
						考试截止日期:
						<text class="mar-L10">{{item.exam_date}}</text>
					</view>
				</view>
			</template>
			<template v-else>
				<view class="text-C color-62">您当前还没有考试任务</view>
			</template>
		</view>
		<view class="row-box mar-T10">
			<view class="mar-B10 dis-flex-hsb" @click="toPage('/pages/promote/index')">
				<view>培训任务({{traininglist.length}})</view>
				<view><i class="gjjd-iconfont icon-xiangyou"></i></view>
			</view>
			<template v-if="traininglist.length>0">
				<view class="task-box" v-for="item in traininglist" @click="toPage('/pages/promote/index')">
					<view class="font-wb">{{item.classInfo.title}}</view>
					<view class="color-62 font-s14">
						培训类型:
						<text v-if="item.type>0" class="mar-L10">线下</text>
						<text v-else class="mar-L10">线上</text>
					</view>
					<view class="color-62 font-s14">
						培训开始日期:
						<text class="mar-L10">{{item.date}}</text>
					</view>
				</view>
			</template>
			<template v-else>
				<view class="text-C color-62">您当前还没有培训任务</view>
			</template>
		</view>
	</view>
</template>

<script>
	import {request,uploads,makeCall,rectTo,navTo,showMsg} from "@/utils";
	export default {
		data() {
			return {
				reviewlist:[],
				examlist:[],
				traininglist:[]
			}
		},
		onLoad() {
			// this.$util.checkLogin();
			this.getData();
		},
		onShow() {
			//this.getData();
		},
		methods: {
			toPage(path){
				navTo(path);
			},
			async getData(){
				uni.showLoading({
					title:'数据加载中...'
				});
				let res = await request({
					method:'POST',
					data:{},
					url:"/apppromote/index"
				});
				uni.hideLoading();
				console.log('promote index getData',res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg && res.msg){
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
			},
		}
	}
</script>

<style>
	page{
		background-color: #f8f8f8;
	}
	.row-box{
		margin-left: 20rpx;
		margin-right: 20rpx;
		padding: 20rpx;
		background-color: #fff;
		border-radius: 12rpx;
	}
	.task-box{
		border: 1rpx solid #000;
		border-radius: 12rpx;
		padding: 10rpx;
		border-radius: 8rpx;
		margin-bottom: 10px;
	}
</style>
