<template>
	<view class="content">
		<view style="position:fixed;top:40px;left:20rpx;right:20rpx;z-index:9999;background-color:rgba(0, 0, 0, 0.6);padding: 10rpx;color:#fff;border-radius:8rpx;" v-if="showpermissionTip">
			<view style="font-weight: bold;">拨打电话授权提示</view>
			<view style="margin-top: 10px;">为了实现电话客服、联系顾客、联系商家等功能，需要访问您的拨打电话权限，您如果拒绝开启，将无法使用上述功能。</view>
		</view>
		<view class="header">
			<view class="header-box">
				<view class="header-bg dis-flex-hv">
					<image style="height: 150px;" mode="heightFix" src="/static/images/user.png"></image>
				</view>
				<view class="header-body" :style="'top:'+($statusBarHeight)+'px;'">
					<view class="mar-T10 mar-L20 font-s20">我的</view>
					<view class="dis-flex-vc mar-T10 mar-L20">
						<view class="user-icon"><image src="/static/images/logo.jpg"></image></view>
						<view class="mar-L20" v-if="isLoggedIn">
							<view class="font-s14">ID:{{user.id}}</view>
							<view class="font-s14">姓名:{{user.realname}}</view>
							<view class="font-s14">
								<text>积分:{{user.point}}</text>
								<text>财富值:{{user.wealthValue}}</text>
								<text>价值估值:{{user.valueValuation}}</text>
							</view>
						</view>
						<view class="mar-L20" v-else>
							<text @click="goPage('/pages/auth/login')">请登录</text>
						</view>
					</view>
				</view>
			</view>
		</view>
		<view class="body" :style="'height:'+($winHeight-170-50-50)+'px;'">
			<view class="navs-box">
				<view class="navs-item dis-flex-hv" v-for="item in navs">
					<view class="text-C" @click="goPage(item.page)">
						<view><i class="gjjd-iconfont font-s20" :class="item.icon"></i></view>
						<view class="mar-T10">{{item.title}}</view>
					</view>
				</view>
			</view>
			<view class="border-line"></view>
			<view class="user-tools">
				<template v-for="item in menus">
					<view class="user-tools-item" @click="goPage(item.page)">
						<view class="user-tools-item-pic"><i class="gjjd-iconfont font-s20" :class="item.icon"></i></view>
						<view class="user-tools-item-text">{{item.title}}</view>
						<view class="user-tools-item-icon"><i class="gjjd-iconfont icon-xiangyou color-92"></i></view>
					</view>
				</template>
				<view v-if="isLoggedIn && user.expert_id>0" class="user-tools-item" @click="goTask()">
					<view class="user-tools-item-pic"><i class="gjjd-iconfont font-s20 icon-renwu"></i></view>
					<view class="user-tools-item-text">审核任务</view>
					<view class="user-tools-item-icon"><i class="gjjd-iconfont icon-xiangyou color-92"></i></view>
				</view>
			</view>
		</view>
		<view class="footer">
			<view v-if="isLoggedIn" class="bg-73a9fa color-ff" @click="onLogout">退出</view>
			<view v-else class="bg-73a9fa color-ff" @click="goPage('/pages/auth/login')">登陆</view>
		</view>
	</view>
</template>

<script>
import {request,uploads,makeCall,rectTo,navTo,showMsg} from "@/utils";
import { useUserStore } from '@/store/user';

const emptyUser = () => ({
	id: 0,
	realname: '',
	expert_id: 0,
	point: 0,
	wealthValue: 0,
	valueValuation: 0
});

export default {
	setup() {
		const userStore = useUserStore();
		return { userStore };
	},
	data() {
		return {
			isLoggedIn: false,
			showpermissionTip:false,
			navs:[
				//{title:'钱包',icon:'icon-qianbao',page:'/pages/user/money'},
				{title:'签到',icon:'icon-qiandao',page:'/pages/user/sign'},
				//{title:'我的收藏',icon:'icon-shoucang',page:'/pages/user/collect'},
				{title:'消息中心',icon:'icon-xiaoxi1',page:'/pages/user/message'},
				//{title:'我的证书',icon:'icon-zhengshu',page:'/pages/user/cert'}
			],
			menus:[
				{title:'联系客服',icon:'icon-kefu',page:'/pages/user/kefu'},
				{title:'设置',icon:'icon-shezhi',page:'/pages/user/setting'}
			],
			user: emptyUser(),
			phone:'18677167310'
		}
	},
	onLoad() {
		//
	},
	onShow() {
		this.syncLoginState();
		this.getData();
		if (this.isLoggedIn) {
			this.getUser();
		}
	},
	methods: {
		syncLoginState() {
			const token = uni.getStorageSync('itoken');
			this.isLoggedIn = !!token;
			if (!this.isLoggedIn) {
				this.resetUser();
			}
		},
		resetUser() {
			this.user = emptyUser();
		},
		async getUser(){
			if (!this.isLoggedIn) {
				return;
			}
			let res = await request({
				method:'POST',
				data:{},
				url:"/appuser/info"
			});
			console.log(res);
			if (res?.code) {
				this.syncLoginState();
				return;
			}
			if(res?.data?.user){
				this.user = res.data.user;
			}
		},
		async getData(){
			uni.showLoading({
				title:'数据加载中'
			});
			let res = await request({
				method:'POST',
				data:{},
				url:"/appuser/index"
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
			if(res?.data?.navs){
				this.navs = res.data.navs;
			}
			if(res?.data?.menus){
				this.menus = res.data.menus;
			}
			if(res?.data?.phone){
				this.phone = res.data.phone;
			}
		},
		async onLogout(){
			try {
				await request({
					method:'POST',
					data:{},
					url:"/appauth/logout"
				});
			} catch (e) {
				console.warn('logout api failed', e);
			}
			this.userStore.logout();
			this.syncLoginState();
			navTo('/pages/index/index');
		},
		goPage(path){
			if(path == '/pages/user/kefu'){
				if(!this.phone){
					showMsg('this',{title:'手机号为空'});
					return;
				}
				makeCall(this.phone,this);
				return;
			}
			navTo(path,{from:'/pages/user/index'});
		},
		goTask(){
			navTo("/pages/task/index");
		}
	}
}
</script>

<style>
	page{
		/* background-color: #929292; */
	}
	.header{
		height: 170px;
		background-image: linear-gradient(#00a8ef, #38b5ea);
	}
	.header-box{
		position: relative;
		width: 750rpx;
		height: 170px;
	}
	.header-bg{
		position: absolute;
		top: 0;
		left: 0;
		right: 0;
		height: 170px;
	}
	.header-body{
		position: absolute;
		left: 0;
		right: 0;
		height: 170px;
		z-index: 99;
		color: #fff;
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
	.navs-box{
		height: 80px;
		display: flex;
		align-items: center;
	}
	.body{
		overflow-y: auto;
	}
	.body::-webkit-scrollbar {
	  display: none;
	}
	.navs-item{
		width: 150rpx;
		height: 70px;
		font-size: 14px;
	}
	.user-tools{
		margin-top: 5px;
	}
	.user-tools-item{
		display: flex;
		align-items: center;
		height: 50px;
		margin-bottom: 2px;
		border-bottom: 1rpx solid #929292;
	}
	.user-tools-item-pic{
		width: 80px;
		height: 50px;
		line-height: 50px;
		text-align: center;
	}
	.user-tools-item-text{
		flex: 1;
		height: 50px;
		line-height: 50px;
		font-size: 14px;
	}
	.user-tools-item-icon{
		width: 40px;
		height: 50px;
		line-height: 50px;
		font-size: 14px;
		text-align: center;
	}
	.footer{
		height: 50px;
		display: flex;
		align-items: center;
		justify-content: center;
	}
	.footer view{
		width: 710rpx;
		height: 40px;
		line-height: 40px;
		border-radius: 20px;
		text-align: center;
	}
</style>
