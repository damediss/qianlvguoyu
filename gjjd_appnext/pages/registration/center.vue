<template>
	<view class="content bg-f8">
		<view class="header bg-ff pos-fixed border-b-ee z-9999" :style="'padding-top:'+$statusBarHeight+'px;'">
			<view class="header-box dis-flex-vc dis-flex-hsb">
				<view class="header-back dis-flex-hv w-60" @click="goBack">
					<i class="gjjd-iconfont icon-xiangzuo font-s20"></i>
				</view>
				<view class="header-title font-s18 font-wb w-full text-C">报名中心</view>
				<view class="header-placeholder w-60"></view>
			</view>
		</view>
		
		<view class="body" :style="'margin-top:'+(50+$statusBarHeight)+'px;'">
			<!-- 标签切换 -->
			<view class="tabs-nav dis-flex bg-ff">
				<view 
					class="tabs-nav-item font-s14 w-full text-C pos-relative" 
					:class="{
						'active font-wb': currentTab === 'self',
						'color-66': currentTab !== 'self',
						'color-73a9fa': currentTab === 'self'
					}" 
					@click="switchTab('self')"
				>
					自主预约报名
				</view>
				<view 
					class="tabs-nav-item font-s14 w-full text-C pos-relative" 
					:class="{
						'active font-wb': currentTab === 'activity',
						'color-66': currentTab !== 'activity',
						'color-73a9fa': currentTab === 'activity'
					}" 
					@click="switchTab('activity')"
				>
					活动
				</view>
				<view class="tabs-nav-item font-s14 w-full text-C pos-relative hide" 
					:class="{
						'active font-wb': currentTab === 'competition',
						'color-66': currentTab !== 'competition',
						'color-73a9fa': currentTab === 'competition'
					}" 
					@click="switchTab('competition')"
				>
					竞赛
				</view>
			</view>
				
			<!-- 自主预约报名列表 -->
			<view class="registration-list" v-if="currentTab === 'self'">
				<view
					class="registration-item bg-ff mar-B20" 
					v-for="(item, index) in selfLists" 
					:key="item.id + '-' + index"
					:style="'animation-delay: ' + (index * 0.1) + 's;'"
					@click="goToDetail(item)"
				>
					<view class="registration-item-left">
						<view class="registration-item-logo" v-if="item.cover">
							<image :src="item.cover" mode="aspectFit"></image>
						</view>
						<view class="registration-item-text text-C" v-else>
							<view class="registration-item-title font-s16 font-wb color-33 line-h-14">{{item.company}}</view>
							<view class="registration-item-subtitle font-s12 color-99 mar-T5" v-if="item.subtitle">{{item.subtitle}}</view>
						</view>
					</view>
					<view class="registration-item-right w-full mar-L20">
						<view class="registration-item-desc font-s14 color-66 mar-B10 text-L line-h-14">{{item.title}}</view>
						<view class="dis-flex-he">
							<view class="registration-item-btn bg-00b6ff font-s12 radius-20 dis-flex-hv mar-B10 mar-R10">
								<text class="color-ff">查看</text>
							</view>
						</view>
					</view>
				</view>
			</view>
				
		<!-- 活动列表 -->
		<view class="activity-list" v-if="currentTab === 'activity'">
			<!-- 第一个活动：图片在左侧，文字在右侧 -->
			<view 
				class="activity-card bg-ff dis-flex pad-20 radius-12 mar-B20 shadow-sm transition-fast fade-in-item" 
				v-for="(item, index) in activityList" 
				:key="item.id"
				:style="'animation-delay: ' + (index * 0.1) + 's;'"
				@click="goToActivityDetail(item)"
			>
					<!-- 第一个活动使用图片左、文字右的布局 -->
					<template v-if="index === 0">
						<view class="activity-card-image-left w-160 h-160 radius-8 overflow-hidden flex-shrink-0 mar-R20">
							<image :src="item.image" mode="aspectFill" class="w-full-percent h-full-percent"></image>
						</view>
						<view class="activity-card-text-right w-full dis-flex-vc">
							<view>
								<view class="activity-card-title font-s16 font-wb color-33 line-h-14 mar-B10">{{item.title}}</view>
								<view class="activity-card-date font-s12 color-99 mar-B10">{{item.date}}</view>
								<view class="activity-card-status font-s12 color-73a9fa">{{item.status}}</view>
							</view>
						</view>
					</template>
					<!-- 其他活动使用文字左、图片右的布局 -->
					<template v-else>
						<view class="activity-card-left w-full dis-flex-vc mar-R20">
							<view>
								<view class="activity-card-title font-s16 font-wb color-33 line-h-14 mar-B10">{{item.title}}</view>
								<view class="activity-card-date font-s12 color-99 mar-B10">{{item.date}}</view>
								<view class="activity-card-status font-s12 color-73a9fa">{{item.status}}</view>
							</view>
						</view>
						<view class="activity-card-right w-160 h-160 radius-8 overflow-hidden flex-shrink-0">
							<image :src="item.image" mode="aspectFill" class="w-full-percent h-full-percent"></image>
						</view>
					</template>
				</view>
			</view>
			</view>
		</view>
</template>

<script>
import {request,uploads,rectTo,navTo,showMsg} from "@/utils";
export default {
	data() {
		return {
			currentTab: 'self',
			page:1,
			selfLists: [],
			activityList: [
				{
					id: 1,
					title: '"漫步自然,畅享健康"户外徒步活动',
					date: '03月3日，星期一',
					status: '0人报名|报名中',
					image: '/static/images/activity.png',
					type: 'activity'
				},
				{
					id: 2,
					title: '2024年7月10日爬山学习活动',
					date: '07月10日，星期三',
					status: '1人报名|报名中',
					image: '/static/images/activity.png',
					type: 'activity'
				}
			]
		}
	},
	onLoad() {
		if(this.currentTab == 'self'){
			this.selfList(true);
		}
	},
	onShow() {
		// 移除登录检查，允许用户浏览报名项目
		// this.$util.checkLogin();
	},
	onReachBottom() {
		if(this.currentTab == 'self'){
			this.selfList(false);
		}
	},
	methods: {
		goBack() {
			uni.navigateBack();
		},
		switchTab(tab) {
			if (tab === 'competition') {
				// 直接跳转到竞赛页面
				this.goToCompetition();
				return;
			}
			this.currentTab = tab;
			this.page = 1;
			if(this.currentTab == 'self'){
				this.selfList(true);
			}
		},
		async selfList(flag=false) {
			if(flag){
				this.page = 1;
				this.selfLists = [];
			}
			let res = await request({
				method:'POST',
				data:{page:this.page},
				url:"/appregistration/selflist"
			});
			console.log("self list",res);
			if(res?.data?.selflist){
				if(res.data.selflist.length>0){
					this.selfLists = [...this.selfLists,...res.data.selflist];
					this.page++;
				}
			}
		},
		goToDetail(item) {
			// 传递完整的项目信息到详情页
			this.$util.navTo('/pages/registration/detail', {id: item.id});
		},
		goToActivityDetail(item) {
			// 跳转到活动详情页
			this.$util.navTo('/pages/registration/activityDetail', {
				id: item.id,
				item: item
			});
		},
		goToCompetition() {
			// 跳转到竞赛页面
			this.$util.navTo('/pages/competition/home/index');
		},
	}
}
</script>

<style>
.content {
	min-height: 100vh;
}

.body {
	padding: 0 20rpx 20rpx 20rpx;
	padding-bottom: calc(env(safe-area-inset-bottom) + 20rpx);
}

.header {
	top: 0;
	left: 0;
	right: 0;
}

.header-box {
	height: 50px;
	padding: 0 20rpx;
}

.header-back {
	height: 50px;
}

/* header-title 和 header-placeholder 样式已通过全局类实现 */

.tabs-nav {
	margin-bottom: 20rpx;
}

.tabs-nav-item {
	line-height: 80rpx;
}

.tabs-nav-item.active {
	font-weight: bold;
}

.tabs-nav-item.active::after {
	content: '';
	position: absolute;
	bottom: 0;
	left: 50%;
	transform: translateX(-50%);
	width: 60rpx;
	height: 4rpx;
	background-color: #73a9fa;
	border-radius: 2rpx;
}

.registration-item:last-child {
	margin-bottom: 0;
}

.registration-item-btn {
	padding: 8rpx 20rpx;
}

.activity-card:last-child {
	margin-bottom: 0;
}

/* 列表项淡入动画 */
.fade-in-item {
	opacity: 0;
	transform: translateY(20px);
	animation: fadeInUp 0.6s ease-out forwards;
}

@keyframes fadeInUp {
	from {
		opacity: 0;
		transform: translateY(20px);
	}
	to {
		opacity: 1;
		transform: translateY(0);
	}
}

/* 触摸反馈效果 */
.registration-item {
	-webkit-tap-highlight-color: transparent;
}

.registration-item:active {
	transform: scale(0.98);
	background-color: #f5f5f5;
}

.activity-card {
	-webkit-tap-highlight-color: transparent;
}

.activity-card:active {
	transform: scale(0.98);
	background-color: #f5f5f5;
}

/* 标签切换动画 */
.tabs-nav-item {
	transition: all 0.3s ease;
}

.tabs-nav-item.active::after {
	transition: all 0.3s ease;
}

/* 按钮点击效果 */
.registration-item-btn {
	transition: all 0.2s ease;
	-webkit-tap-highlight-color: transparent;
}

.registration-item-btn:active {
	transform: scale(0.95);
	opacity: 0.8;
}
</style>
