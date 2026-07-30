<template>
	<view class="content bg-f8">
		<view class="header bg-ff pos-fixed border-b-ee z-9999" :style="'padding-top:'+$statusBarHeight+'px;'">
			<view class="header-box dis-flex-vc dis-flex-hsb">
				<view class="header-back dis-flex-hv w-60" @click="goBack">
					<i class="gjjd-iconfont icon-xiangzuo font-s20"></i>
				</view>
				<view class="header-title font-s18 font-wb w-full text-C">活动详情</view>
				<view class="header-placeholder w-60"></view>
			</view>
		</view>
		
	<view class="body" :style="'margin-top:'+(50+$statusBarHeight)+'px;'">
		<!-- 活动轮播图 -->
		<view class="activity-banner">
			<swiper class="banner-swiper" circular :indicator-dots="true" :autoplay="false">
				<swiper-item v-for="(img, index) in bannerImages" :key="index">
					<view class="banner-item">
						<image :src="img" mode="aspectFill"></image>
					</view>
				</swiper-item>
			</swiper>
		</view>
		
		<!-- 活动标题和日期 -->
		<view class="activity-header bg-ff pad-30 pos-relative z-100 border-b-f0 border-t-f0">
			<view class="activity-title font-s20 font-wb color-33 line-h-14 mar-B20">{{activityInfo.title}}</view>
			<view class="activity-date font-s14 color-66">活动日期: {{activityInfo.dateRange}}</view>
		</view>
		
		<!-- 活动简介 -->
		<view class="activity-section bg-ff pad-30 mar-T20 border-b-f0">
			<view class="section-title font-s16 font-wb color-33 mar-B5">活动简介</view>
			<view class="section-content font-s14 color-66 line-h-16 mar-B20">{{activityInfo.description}}</view>
				
				<!-- 活动图片 -->
				<view class="activity-images-container mar-T20" v-if="activityInfo.images && activityInfo.images.length > 0">
					<image 
						v-for="(img, index) in activityInfo.images" 
						:key="index"
						:src="img" 
						mode="aspectFill"
						class="activity-content-image w-full-percent h-300 radius-8 mar-B10"
						@click="previewImage(img, activityInfo.images)"
					></image>
				</view>
			</view>
			
			<!-- 精彩内容 -->
			<view class="activity-section bg-ff pad-30 mar-T20 border-b-f0">
				<view class="section-title font-s16 font-wb color-33 mar-B5">精彩内容</view>
				<view class="highlights-images dis-flex gap-15">
					<image 
						v-for="(img, index) in activityInfo.highlights" 
						:key="index"
						:src="img" 
						mode="aspectFill"
						class="highlight-image w-200 h-150 radius-8"
						@click="previewImage(img, activityInfo.highlights)"
					></image>
				</view>
			</view>
			
			<!-- 底部间距 -->
			<view class="bottom-spacing h-60"></view>
		</view>
		
		<!-- 底部报名按钮 -->
		<view class="bottom-btn bg-73a9fa pos-fixed dis-flex-hv h-100 z-100 border-t-f0" @click="handleRegister">
			<text class="color-ff font-s16 font-wb">立即报名</text>
		</view>
	</view>
</template>

<script>
import {request,showMsg} from "@/utils";

export default {
	data() {
		return {
			activityInfo: {
				id: 1,
				title: '"漫步自然,畅享健康"户外徒步活动',
				dateRange: '2025-03-03 - 2025-04-04',
				description: '户外徒步',
				image: '/static/images/activity.png',
				images: ['/static/images/company.png'],
				highlights: ['/static/images/activity.png', '/static/images/activity.png'],
				isRegistered: false
			},
			bannerImages: ['/static/images/activity.png']
		}
	},
	onLoad(options) {
		console.log('接收参数:', options);
		
		// 处理传递的参数
		if (options.item) {
			try {
				const item = JSON.parse(decodeURIComponent(options.item));
				this.activityInfo = {
					...this.activityInfo,
					...item,
					// 补充完整信息
					dateRange: '2025-03-03 - 2025-04-04',
					description: '户外徒步',
					image: '/static/images/activity.png',
					images: ['/static/images/company.png'],
					highlights: ['/static/images/activity.png', '/static/images/activity.png'],
					isRegistered: false
				};
			} catch (e) {
				console.error('解析参数失败:', e);
			}
		}
	},
	methods: {
		goBack() {
			uni.navigateBack();
		},
		previewImage(current, urls) {
			uni.previewImage({
				current: current,
				urls: urls
			});
		},
	handleRegister() {
		// 检查登录状态
		if (!this.$util.isLogin()) {
			showMsg('this', {title: '请先登录后再报名'});
			this.$util.checkLogin();
			return;
		}
		
		// 执行报名逻辑
		this.registerActivity();
	},
	async registerActivity() {
		try {
			const res = await request({
				method: 'POST',
				url: '/activity/register',
				data: {
					activityId: this.activityInfo.id
				}
			});
			
			if (res.code === 0) {
				showMsg('this', {title: '报名成功'});
				// 可以刷新页面或返回
				setTimeout(() => {
					uni.navigateBack();
				}, 1500);
			} else {
				showMsg('this', {title: res.msg || '报名失败'});
			}
		} catch (error) {
			console.error('报名失败:', error);
			showMsg('this', {title: '报名失败，请重试'});
		}
	}
	}
}
</script>

<style>
.content {
	min-height: 100vh;
	padding-bottom: 120rpx;
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

.body {
	padding: 0;
}

.activity-banner {
	width: 100%;
	height: 500rpx;
	padding: 20rpx 20rpx 0 20rpx;
	box-sizing: border-box;
	margin-bottom: 25rpx;
}

.banner-swiper {
	width: 100%;
	height: 100%;
	border-radius: 12rpx;
	overflow: hidden;
}

.banner-item {
	width: 100%;
	height: 100%;
}

.banner-item image {
	width: 100%;
	height: 100%;
}

/* 轮播图指示器样式 */
.banner-swiper ::v-deep .uni-swiper-dots {
	bottom: 20rpx;
}

.banner-swiper ::v-deep .uni-swiper-dot {
	width: 16rpx;
	height: 16rpx;
	border-radius: 50%;
	background-color: rgba(255, 255, 255, 0.5);
	margin: 0 8rpx;
}

.banner-swiper ::v-deep .uni-swiper-dot-active {
	background-color: #73a9fa;
}

.activity-section:last-of-type {
	border-bottom: none;
}

.bottom-btn {
	bottom: 20rpx;
	left: 20rpx;
	right: 20rpx;
	border-radius: 50rpx;
}
</style>
