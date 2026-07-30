<template>
	<view class="content">
		<view class="header">
			<view class="header-box" :style="'height: '+($statusBarHeight+100+40+20)+'px;'">
				<view :style="'height: '+$statusBarHeight+'px;'"><!--状态栏占位--></view>
				<view class="header-user">
					<view class="dis-flex-hv mar-L40 mar-R40">
						<view class="user-icon"><image src="/static/images/logo.jpg"></image></view>
						<view class="mar-L20 color-ff">您好, {{user.realname}}</view>
					</view>
				</view>
				<view class="header-search" @click="tomyList(1)">
					<text class="mar-L10 gjjd-iconfont icon-sousuo font-s24"></text>
					<text class="mar-L10 color-92">搜索您的团队</text>
				</view>
			</view>
		</view>
		<view class="body" :style="'height:'+($winHeight-($statusBarHeight+100+40+20)-55)+'px;'">
			<view class="dis-flex-hsb mar-T20 mar-L20 mar-R20" @click="tomyList(0)">
				<view class="font-wb">我的团队</view>
				<view class="color-92 font-s14">
					<text>全部</text>
					<i class="gjjd-iconfont icon-xiangyou color-92"></i>
				</view>
			</view>
			<view class="team-list mar-T20">
				<view class="team-item" :class="{'mar-R20':i<myTeams.length-1}" v-for="(myteam,i) in myTeams">
					<view @click="toDetail(myteam.id)">
						<image class="team-cover" :src="myteam.cover"></image>
						<view class="team-title ellipsis-row2">{{myteam.title}}</view>
					</view>
				</view>
			</view>
			<view class="dis-flex-hsb mar-T20 mar-B20 mar-L20 mar-R20" @click="tomasterList(0)">
				<view class="font-wb">技能大师工作室</view>
				<view class="color-92 font-s14">
					<text>全部</text>
					<i class="gjjd-iconfont icon-xiangyou color-92"></i>
				</view>
			</view>
			<view class="team-list mar-T20">
				<view class="team-item" :class="{'mar-R20':i<marsters.length-1}" v-for="(item,i) in marsters">
					<view @click="toDetail(item.id)">
						<image class="team-cover" :src="item.cover"></image>
						<view class="team-title ellipsis-row2">{{item.title}}</view>
					</view>
				</view>
			</view>
			<view class="dis-flex-hsb mar-T20 mar-B20 mar-L20 mar-R20" @click="tomasterList(1)">
				<view class="font-wb">热门团队</view>
				<view class="color-92 font-s14">
					<text>全部</text>
					<i class="gjjd-iconfont icon-xiangyou color-92"></i>
				</view>
			</view>
			<view class="team-hot mar-T20">
				<view class="team-hot-item mar-B10" :class="{'mar-R20':i<hotTeams.length-1}" v-for="(item,i) in hotTeams" @click="toDetail(item.id)">
					<image class="team-hot-cover" :src="item.cover"></image>
					<view class="team-hot-info">
						<view class="mar-B10 font-wb">{{item.title}}</view>
						<view class="mar-B20 font-s14 color-92">{{item.desc}}</view>
						<view class="dis-flex-hsb font-s14 color-92">
							<text>{{item.date}}</text>
							<text>{{item.look}}</text>
						</view>
					</view>
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
				user:{id:0,realname:'gann',point:0,wealthValue:0,valueValuation:0},
				myTeams:[],
				marsters:[],
				hotTeams:[]
			}
		},
		onLoad() {
			
		},
		onShow() {
			//this.$util.checkLogin();
			this.getList();
		},
		methods: {
			toEcharts(){
				navTo("/pages/team/echarts");
			},
			tomyList(search=0){
				navTo("/pages/team/myTeamList",{search});
			},
			tomasterList(tag=0){
				navTo("/pages/team/masterList",{tag});
			},
			toDetail(id){
				navTo("/pages/team/detail",{id});
			},
			async getList(){
				let res = await request({
					method:'POST',
					data:{},
					url:"/appteam/index"
				});
				console.log('team list',res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg})
				}
				if(res?.data?.user){
					this.user = res.data.user;
				}
				if(res?.data?.myTeams){
					this.myTeams = res.data.myTeams;
				}
				if(res?.data?.marsters){
					this.marsters = res.data.marsters;
				}
				if(res?.data?.hotTeams){
					this.hotTeams = res.data.hotTeams;
				}
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
	}
	.header-box{
		position: relative;
		width: 750rpx;
	}
	.header-user{
		display: flex;
		align-items: center;
		height: 100px;
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
	.header-search{
		height: 40px;
		margin-left: 30rpx;
		margin-right: 30rpx;
		background-color: #fff;
		border-radius: 20px;
		display: flex;
		align-items: center;
	}
	.body{
		overflow-y: auto;
	}
	.team-list{
		width: 710rpx;
		margin-left: 20rpx;
		margin-right: 20rpx;
		display: flex;
		flex-wrap: nowrap;/*不换行*/
		overflow-x: auto;
	}
	.team-list::-webkit-scrollbar {
	  display: none;
	}
	.team-item{
		background-color: #fff;
		border-radius: 12rpx;
		width: 180rpx;
		/* height: 110px; */
		flex-shrink: 0;/*防止项目缩小*/
		display: flex;
		align-items: center;
		justify-content: center;
	}
	.team-cover{
		width: 140rpx;
		height: 140rpx;
		border-radius: 70rpx;
	}
	.team-title{
		width: 170rpx;
		text-align: center;
		margin-top: 5px;
	}
	.team-hot{
		margin-left: 20rpx;
		margin-right: 20rpx;
	}
	.team-hot-item{
		display: flex;
	}
	.team-hot-cover{
		width: 210rpx;
		height: 210rpx;
		background-color: #ccc;
		border-top-left-radius: 12rpx;
		border-bottom-left-radius: 12rpx;
	}
	.team-hot-info{
		flex: 1;
		padding: 5px;
		background-color: #fff;
		border-top-right-radius: 12rpx;
		border-bottom-right-radius: 12rpx;
	}
</style>
