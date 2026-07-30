<template>
	<view class="content">
		<view class="show-permission-tip" :style="'top:'+(50+$statusBarHeight)+'px;'" v-if="showpermissionTip">
			<view style="font-weight: bold;">相册、相机信息授权提示</view>
			<view style="margin-top: 10px;">为了实现扫码功能、相关材料上传，需要访问您的相册、相机权限，您如果拒绝开启，将无法使用上述功能。</view>
		</view>
		<view class="header" :style="'padding-top:'+$statusBarHeight+'px;'">
			<view class="header-box">
				<view class="header-search" @click="toSearch">
					<view class="header-search-icon">
						<i class="gjjd-iconfont icon-sousuobeifen2 font-s28"></i>
					</view>
					<input class="header-search-input" disabled placeholder="请输入搜索内容"/>
					<view class="header-search-btn">搜索</view>
				</view>
				<view>
					<i class="gjjd-iconfont icon-saoma font-s30" @click="scan"></i>
				</view>
			</view>
		</view>
		<view class="body" :style="'margin-top:'+(50+$statusBarHeight)+'px;'">
			<!-- 轮播图 -->
			<view class="lunbo">
				<swiper v-if="lunbo.length>0" class="lunbo-swiper" circular :indicator-dots="true" :autoplay="true" :interval="2000" :duration="500">
					<swiper-item v-for="item in lunbo">
						<view class="lunbo-pic">
							<image :src="item.img"></image>
						</view>
					</swiper-item>
				</swiper>
			</view>
			<!-- first资讯 -->
			<view class="first-new mar-T10" @click="tonewsZX(newsZX.article_id)">
				<view class="first-tag">新闻资讯</view>
				<view class="first-title">{{newsZX.title}}</view>
				<view class="first-icon">
					<i class="gjjd-iconfont icon-xiangyou"></i>
				</view>
			</view>
			<!-- tabs 版块 -->
			<view class="tabs-scroll mar-T10">
				<view class="tabs-scroll-item mar-R10" @click="goToRegistrationCenter">
					<view class="tabs-scroll-item-icon">
						<image src="/static/images/nav-1.png"></image>
					</view>
					<view class="tabs-scroll-item-text">报名中心</view>
				</view>
				<view class="tabs-scroll-item mar-R10 hide" @click="goToCompetitionCenter">
					<view class="tabs-scroll-item-icon">
						<image src="/static/images/match.png"></image>
					</view>
					<view class="tabs-scroll-item-text">竞赛中心</view>
				</view>
				<view class="tabs-scroll-item mar-R10 hide">
					<view class="tabs-scroll-item-icon">
						<image src="/static/images/coop.png"></image>
					</view>
					<view class="tabs-scroll-item-text hide">合作单位</view>
				</view>
				<view class="tabs-scroll-item mar-R10 hide">
					<view class="tabs-scroll-item-icon">
						<image src="/static/images/nav-2.png"></image>
					</view>
					<view class="tabs-scroll-item-text">政策查询</view>
				</view>
				<view class="tabs-scroll-item mar-R10 hide">
					<view class="tabs-scroll-item-icon">
						<image src="/static/images/nav-4.png"></image>
					</view>
					<view class="tabs-scroll-item-text">报名查询</view>
				</view>
			</view>
		</view>
		<view class="news-list mar-T20">
			<template v-for="(item,i) in articleList">
				<view class="news-item mar-B10" @click="tonewsZX(item.article_id)">
					<view class="news-item-title">
						<view class="news-item-title-icon">
							<image src="/static/images/hot.png"></image>
						</view>
						<view class="news-item-title-text">{{item.title}}</view>
					</view>
					<view>
						<text class="news-item-tag">#{{item.category}}#</text>
						<text class="color-cc font-s12 mar-L20">{{item.create_time}}</text>
						<text class="color-cc font-s12 mar-L20">{{item.virtual_views}} 浏览</text>
					</view>
					<view class="news-item-pic">
						<template v-if="item.album.length>3">
							<view class="dis-flex-hsb w-710">
								<view class="w-p50">
									<image mode="widthFix" class="news-item-pic-image" :src="item.album[0]['url']"></image>
								</view>
								<view class="w-p50">
									<image mode="widthFix" class="news-item-pic-image" :src="item.album[1]['url']"></image>
								</view>
							</view>
							<view class="dis-flex-hsb w-710 mar-T10">
								<view class="w-p50">
									<image mode="widthFix" class="news-item-pic-image" :src="item.album[2]['url']"></image>
								</view>
								<view class="w-p50">
									<image mode="widthFix" class="news-item-pic-image" :src="item.album[3]['url']"></image>
								</view>
							</view>
						</template>
						<template v-if="item.album.length==3">
							<view class="dis-flex-hsb w-710">
								<view class="w-p50">
									<image mode="widthFix" class="news-item-pic-image" :src="item.album[0]['url']"></image>
								</view>
								<view class="w-p50">
									<image mode="widthFix" class="news-item-pic-image" :src="item.album[1]['url']"></image>
								</view>
							</view>
							<view class="w-710 mar-T10">
								<image mode="widthFix" class="news-item-pic-image-full" :src="item.album[2]['url']"></image>
							</view>
						</template>
						<template v-if="item.album.length==2">
							<view class="dis-flex-hsb w-710">
								<view class="w-p50">
									<image mode="widthFix" class="news-item-pic-image" :src="item.album[0]['url']"></image>
								</view>
								<view class="w-p50">
									<image mode="widthFix" class="news-item-pic-image" :src="item.album[1]['url']"></image>
								</view>
							</view>
						</template>
						<template v-if="item.album.length==1">
							<view class="w-710">
								<image mode="widthFix" class="news-item-pic-image-full" :src="item.album[0]['url']"></image>
							</view>
						</template>
					</view>
				</view>
			</template>
		</view>
		<!-- 更新下载进度条 -->
		<view v-if="upgrade_progress" class="upgrade-progress" @touchmove.stop.prevent="()=>{}">
			<view style="background-color: #fff;padding: 20rpx;border-radius: 6rpx;">{{upgrade_progress}}</view>
		</view>
	</view>
</template>

<script>
	import { getPKTest } from "@/api/competition";
	import {request,uploads,rectTo,navTo,showMsg,cameraPermission,checkUpgrade} from "@/utils";
	export default {
		data() {
			return {
				showpermissionTip:false,
				lunbo:[],
				newsZX:{article_id:0,title:''},
				articleList:[],
				upgrade_progress:''
			}
		},
		onLoad() {
			checkUpgrade({userId:0},this,(e)=>{
				console.log('***',e);
			});
		},
		onShow() {
			this.getPageData();
			if(this.upgrade_progress){
				uni.addInterceptor('switchTab',{
					invoke(e){
						//拦截前触发
						console.log('invoke',e)
						return false;
					},
					success(e){
						//成功回调拦截
						console.log('success',e)
						return false;
					}
				});
			}else{
				var tu=0;
				var timeId = setInterval(()=>{
					if(tu>10){
						clearInterval(timeId);
						tu=0;
					}else{
						tu++;
						if(this.upgrade_progress){
							clearInterval(timeId);
							tu=0;
							uni.addInterceptor('switchTab',{
								invoke(e){
									//拦截前触发
									console.log('invoke',e)
									return false;
								},
								success(e){
									//成功回调拦截
									console.log('success',e)
									return false;
								}
							});
						}
					}
				},1000);
			}
		},
		onPullDownRefresh() {
			setTimeout(function () {
				uni.stopPullDownRefresh();
			}, 1000);
		},
		onNavigationBarButtonTap(e){
			console.log(e);
			if(e.code && e.code == ''){
				//
			}
		},
		methods: {
			async getPageData(){
				let that = this;
				let res = await request({
					method:'POST',
					data:{},
					url:"/apphome/index"
				});
				console.log(res);
				if(res?.data?.lunbo){
					this.lunbo = res.data.lunbo;
				}
				if(res?.data?.newsZX){
					this.newsZX = res.data.newsZX;
				}
				if(res?.data?.articleList){
					this.articleList = res.data.articleList;
				}
			},
			async toSearch(){
				//navTo('/pages/index/search');
				const res = await getPKTest({
					matchId: 71,
					userId: 608
				});
				console.log('***',res);
			},
			tonewsZX(id){
				console.log("article_id="+id);
				navTo('/pages/public/article',{article_id:id});
			},
			scan(){
				cameraPermission((e)=>{
					if(e>0){
						uni.scanCode({
							onlyFromCamera: true,
								success: (res)=>{
									console.log('条码类型：' + res.scanType);
									console.log('条码内容：' + res.result);
									if(res.result){
									if(res.result.includes('/pages/promote/survey')){
										const formId = res.result.split('=')[1]
										uni.navigateTo({
											url: `/pages/promote/survey?formId=${formId}`
										});
									}else if(res.result.includes('/pages/user/kjexam')){
										uni.navigateTo({
											url:'/pages/user/kjexam'
										});
									}else if(res.result.includes('/#/')){
										let array = res.result.split('#');
										let path1 = array[1] && array[1].includes('pages/registration/detail?id=');
										let path2 = array[1] && array[1].includes('pages/signUp/self?id=');
										if(path1 || path2){
											let param = array[1].split('=');
											uni.navigateTo({
												url:'/pages/registration/detail?id='+param[1]
											});
										}
									}
								}
							}
						});
					}
				},this);
			},
			goToRegistrationCenter(){
				// 直接跳转到报名中心页面，不检查登录状态
				// 用户可以先浏览报名项目，在需要填写表单时再登录
				this.$util.navTo('/pages/registration/center');
			},
			goToCompetitionCenter(){
				// 跳转到竞赛中心页面
				this.$util.navTo('/pages/competition/home/index');
			}
		}
	}
</script>

<style>
	.content{
		background-color: #eee;
	}
	.show-permission-tip{
		position:fixed;
		left:20rpx;
		right:20rpx;
		z-index:9999;
		background-color:rgba(0, 0, 0, 0.6);
		padding: 10rpx;
		color:#fff;
		border-radius:8rpx;
	}
	.header{
		position: fixed;
		top: 0;
		left: 0;
		right: 0;
		z-index: 9999;
		background-color: #fff;
	}
	.header-box{
		height: 50px;
		padding: 0 20rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}
	.header-search{
		width: 60%;
		background-color: #ddd;
		padding-left: 90rpx;
		height: 36px;
		line-height: 36px;
		border-radius: 18px;
		position: relative;
	}
	.header-search-icon{
		position: absolute;
		top: 0;
		left: 0;
		width: 80rpx;
		height: 36px;
	}
	.header-search-btn{
		position: absolute;
		top: 0;
		right: -90rpx;
		width: 80rpx;
		height: 36px;
	}
	.header-search-input{
		height: 36px;
		line-height: 36px;
	}
	.body{
		padding-left: 20rpx;
		padding-right: 20rpx;
		background-color: #fff;
	}
	.lunbo{
		width: 710rpx;
		height: 160px;
	}
	.lunbo-swiper{
		width: 710rpx;
		height: 160px;
		border-radius: 12rpx;
	}
	.lunbo-pic{
		height: 160px;
	}
	.lunbo-pic image{
		width: 710rpx;
		height: 160px;
	}
	.first-new{
		display: flex;
		align-items: center;
		height: 32px;
		background-color: #ffebd1;
		border-radius: 15px;
	}
	.first-tag{
		padding: 0 10rpx;
		margin: 0 20rpx;
		height: 20px;
		line-height: 20px;
		background-color: #efa100;
		border-radius: 6rpx;
		font-size: 12px;
	}
	.first-title{
		flex: 1;
		font-size: 12px;
	}
	.first-icon{
		width: 60rpx;
		text-align: center;
	}
	.tabs-scroll{
		width: 710rpx;
		overflow-x: auto;
		height: 80px;
		display: flex;
		align-items: center;
		flex-wrap: nowrap;/*不换行*/
	}
	.tabs-scroll-item{
		flex-shrink: 0;/*防止项目缩小*/
		width: 140rpx;
		height: 60px;
	}
	.tabs-scroll-item-icon{
		height: 35px;
		display: flex;
		align-items: center;
		justify-content: center;
	}
	.tabs-scroll-item-icon image{
		width: 30px;
		height: 30px;
	}
	.tabs-scroll-item-text{
		height: 25px;
		line-height: 25px;
		text-align: center;
		font-size: 12px;
	}
	.news-list{
		width: 710rpx;
		padding-top: 10px;
		padding-left: 20rpx;
		padding-right: 20rpx;
		background-color: #fff;
	}
	.news-item-title{
		width: 710rpx;
		display: flex;
		align-items: center;
	}
	.news-item-title-icon{
		width: 20px;
		height: 20px;
	}
	.news-item-title-icon image{
		width: 20px;
		height: 20px;
	}
	.news-item-title-text{
		flex: 1;
		font-weight: bold;
	}
	.news-item-tag{
		font-size: 12px;
		color: #73a9fa;
	}
	.news-item-pic{
		width: 710rpx;
	}
	.w-710{
		width: 710rpx;
	}
	.w-p50{
		width: 355rpx;
		max-height: 160px;
		display: flex;
		justify-content: center;
	}
	.news-item-pic-image{
		width: 345rpx;
		max-height: 160px;
	}
	.news-item-pic-image-full{
		width: 710rpx;
		height: 160px;
	}
	.upgrade-progress{
		position: fixed;
		top: 0;
		bottom: 0;
		left: 0;
		right: 0;
		z-index: 9999;
		background-color: rgba(0, 0, 0, 0.6);
		display: flex;
		align-items: center;
		justify-content: center;
	}
</style>
