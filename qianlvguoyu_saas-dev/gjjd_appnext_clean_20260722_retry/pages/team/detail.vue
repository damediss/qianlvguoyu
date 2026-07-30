<template>
	<view class="content">
		<view class="navs" :style="'top:'+$statusBarHeight+'px;'">
			<i @click="toBack" class="gjjd-iconfont icon-xiangzuo font-s20 font-wb color-ff"></i>
			<text class="mar-L20 font-wb color-ff">{{master.title}}</text>
		</view>
		<view class="top">
			<image v-if="albums.length>0" class="top-bg" :src="albums[0]['pic']"></image>
		</view>
		<view class="body" :style="'height:'+bHeight+'px;'">
			<view class="body-top">
				<view class="jianjie">
					<uv-parse :content="detail.intro"></uv-parse>
				</view>
			<view class="member-box dis-flex mar-T10 font-s12">
				<view class="member dis-flex">
					<template v-for="(item,i) in members">
						<image src="/static/images/logo.jpg" :style="i>0?'margin-left:-20rpx;':''"></image>
					</template>
				</view>
				<view class="mar-L20">
					<text>0</text>成员在学习
					<text class="color-73a9fa">能力评分:0分</text>
				</view>
			</view>
			<view class="tabs dis-flex-vc mar-T10">
				<view class="tabs-item dis-flex-hv mar-R10" v-for="(item,i) in tabs">
					<view @click="onTabs(i)">
						<view class="text-C mar-B5">{{item.title}}</view>
						<view class="dis-flex-hv">
							<view class="tabs-active" v-if="tabIndex==i"></view>
						</view>
					</view>
				</view>
			</view>
			</view>
			<view class="body-list">
				<view v-if="tabType == 'study'" style="overflow-y: auto;" :style="'height:'+(bHeight-64-90-30)+'px;'">
					<template v-if="studys.length>0">
					<view v-for="(task) in studys">
						<view class="body-list-title sticky" style="top: 0;">{{task.title}}</view>
						<view class="body-list-item dis-flex-hsb dis-flex-vc mar-T10 mar-B10" v-for="(item,i) in task.list">
							<view>{{item.realname}}</view>
							<view style="width: 180rpx;">
								<view class="mar-B5">
									<progress :percent="item.progress" stroke-width="3" />
								</view>
								<view class="font-s12 color-92">{{item.progress}}%</view>
							</view>
						</view>
					</view>
					</template>
				</view>
				<view v-if="tabType == 'album'" class="album-list dis-flex" :style="'max-height:'+(bHeight-64-90-30)+'px;'">
					<template v-if="albums.length>0">
					<view class="album-item" v-for="(item,i) in albums">
						<view class="album-pic dis-flex-hv">
							<image :src="item.pic" mode="aspectFit" @click="onPreviewImage(i,albums,false)"></image>
						</view>
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
				bHeight:600,
				id:0,
				master:{},
				detail:{},
				members:[],
				studys:[],
				albums:[],
				tabs:[
					{title:'学习任务',type:'study',icon:''},
					{title:'相册',type:'album',icon:''}
				],
				tabIndex:0,
				tabType:'study'
			}
		},
		onLoad(options) {
			console.log("team detail",options)
			this.bHeight = this.$winHeight - 190;
			if(options.id){
				this.id = options.id;
				this.getData();
			}
		},
		methods: {
			toBack(){
				uni.navigateBack();
			},
			onTabs(i){
				if(this.loading) return;
				console.log('i='+i);
				if(i != this.tabIndex){
					this.tabIndex = i;
					this.tabType = this.tabs[i].type;
				}
			},
			async getData(){
				let res = await request({
					method:'POST',
					data:{id:this.id},
					url:"/appteam/detail"
				});
				console.log('team list',res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg})
				}
				if(res?.data?.master){
					this.master = res.data.master;
				}
				if(res?.data?.members){
					this.members = res.data.members;
				}
				if(res?.data?.detail){
					this.detail = res.data.detail;
				}
				if(res?.data?.studys){
					this.studys = res.data.studys;
				}
				if(res?.data?.albums){
					this.albums = res.data.albums;
				}
			},
			onPreviewImage(i,lists=[],isarray=true){
				if(lists.length == 0) return;
				let urls = isarray?lists:[];
				if(!isarray){
					urls = [];
					for(let j in lists){
						urls.push(lists[j].pic);
					}
				}
				uni.previewImage({
					current:i,
					urls:urls
				});
			}
		}
	}
</script>

<style>
	.navs{
		position: fixed;
		left: 20rpx;
		right: 20rpx;
		z-index: 9999;
		height: 40px;
		line-height: 40px;
	}
	.top{
		height: 200px;
		background-color: #ddd;
		overflow: hidden;
		position: relative;
	}
	.top-bg{
		position: absolute;
		z-index: 0;
		width: 750rpx;
		height: 200px;
	}
	.body{
		position: fixed;
		top: 180px;
		left: 0;
		right: 0;
		z-index: 99;
		height: 120px;
		background-color: #f8f8f8;
		border-top-left-radius: 20rpx;
		border-top-right-radius: 20rpx;
		/* overflow-y:auto; */
	}
	.body-top{
		padding: 10px;
		background-color: #fff;
		border-top-left-radius: 20rpx;
		border-top-right-radius: 20rpx;
	}
	.jianjie{
		max-height: 90px;
		overflow: hidden;
	}
	.member{
		max-width: 180rpx;
		height: 30px;
		overflow-x: auto;
	}
	.member image{
		width: 50rpx;
		height: 50rpx;
		border-radius: 25rpx;
		flex-shrink: 0;
	}
	.body-list{
		margin-top: 10px;
	}
	.tabs{
		height: 44px;
		justify-content: start;
		background-color: #fff;
	}
	.tabs-item{
		padding: 5px;
	}
	.tabs-active{
		width: 60%;
		height: 4px;
		background-color: #73a9fa;
		border-radius: 2px;
	}
	.body-list-title{
		background-color: #f8f8f8;
		height: 30px;
		line-height: 30px;
		padding-left: 20rpx;
	}
	.body-list-item{
		width: 690rpx;
		margin-bottom: 10px;
		margin-left: 10rpx;
		margin-right: 10rpx;
		padding: 10px;
		background-color: #fff;
		border-radius: 12rpx;
	}
	.album-list{
		width: 750rpx;
		align-items: start;
		justify-content: start;
		flex-wrap: wrap;
		overflow-y: auto;
	}
	.album-item{
		width: 230rpx;
		height: 230rpx;
		margin-bottom: 10px;
		padding: 0 10rpx;
	}
	.album-pic{
		width: 230rpx;
		height: 230rpx;
		background-color: #fff;
		border-radius: 12rpx;
	}
	.album-pic image{
		width: 200rpx;
		height: 200rpx;
	}
</style>
