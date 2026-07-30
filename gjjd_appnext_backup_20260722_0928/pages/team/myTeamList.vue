<template>
	<view class="content">
		<view class="header">
			<view :style="'height: '+$statusBarHeight+'px;'"><!--状态栏占位--></view>
			<view class="header-nav">
				<view class="nav-back" @click="toBack"><i class="gjjd-iconfont icon-xiangzuo font-s26"></i></view>
				<view class="search" v-if="search>0">
					<view class="search-input">
						<input v-model="keyword" placeholder="搜索您的团队" @confirm="onSearch"/>
						<view class="search-btn" @click="onSearch"><i class="gjjd-iconfont icon-sousuo font-s24"></i></view>
					</view>
				</view>
				<text v-else class="font-wb">我的团队</text>
			</view>
			<view style="height: 10px;"><!--header-body间占位--></view>
		</view>
		<view class="body" :style="'height:'+($winHeight-$statusBarHeight-44-10)+'px;'">
			<view class="list-item" v-for="item in list" @click="toDetail(item.id)">
				<view class="item-cover">
					<image :src="item.cover"></image>
				</view>
				<view class="item-info">
					<view class="mar-B20">{{item.title}}</view>
					<view class="color-92">{{item.desc}}</view>
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
				search:0,
				keyword:'',
				list:[]
			}
		},
		onLoad(options) {
			if(options.item){
				let item = JSON.parse(decodeURIComponent(options.item));
				console.log("item",item);
				this.search = item.search;
			}
			this.getData();
		},
		methods: {
			toBack(){
				uni.navigateBack();
			},
			toDetail(id){
				navTo("/pages/team/detail",{id});
			},
			onSearch(){
				this.getData();
			},
			async getData(){
				let res = await request({
					method:'POST',
					data:{keyword:this.keyword},
					url:"/appteam/mylist"
				});
				console.log('myteam list',res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg})
				}
				if(res?.data?.list){
					this.list = res.data.list;
				}
			}
		}
	}
</script>

<style>
	page{
		background-color: #f8f8f8;
	}
	.header-nav{
		position: relative;
		height: 44px;
		line-height: 44px;
		text-align: center;
	}
	.nav-back{
		position: absolute;
		top: 0;
		left: 20rpx;
		z-index: 9;
		height: 44px;
		line-height: 44px;
	}
	.search{
		height: 44px;
		display: flex;
		justify-content: center;
		align-items: center;
	}
	.search-input{
		width: 70%;
		position: relative;
	}
	.search-input input{
		height: 40px;
		line-height: 40px;
		background-color: #ccc;
		border-radius: 20px;
	}
	.search-btn{
		position: absolute;
		top: 0;
		right: 15rpx;
		z-index: 99;
		height: 40px;
		line-height: 40px;
	}
	.body{
		overflow-y: auto;
	}
	.list-item{
		padding: 10px 0;
		border-bottom: 1rpx solid #e0e0e0;
		display: flex;
	}
	.item-cover{
		width: 180rpx;
		height: 180rpx;
		margin-left: 20rpx;
	}
	.item-cover image{
		width: 180rpx;
		height: 180rpx;
		background-color: #ddd;
		border-radius: 8rpx;
	}
	.item-info{
		flex: 1;
		margin-right: 20rpx;
		padding-left: 20rpx;
	}
</style>