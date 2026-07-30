<template>
	<view class="content">
		<view class="search-box">
			<view class="search-icon">
				<i class="gjjd-iconfont icon-sousuobeifen2 font-s28"></i>
			</view>
			<input class="search-input" placeholder="请输入搜索内容" v-model="keyword"/>
			<view class="search-btn" @click="doSearch">搜索</view>
		</view>
		<view class="news-list mar-T20">
			<template v-for="(item,i) in lists">
				<view class="news-item mar-B20" @click="tonewsZX(item.article_id)">
					<view class="news-item-title">
						<!-- <view class="news-item-title-icon">
							<image src="/static/images/hot.png"></image>
						</view> -->
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
	</view>
</template>

<script>
	import {request,uploads,rectTo,navTo,showMsg,cameraPermission} from "@/utils";
	export default {
		data() {
			return {
				keyword:'',
				page:1,
				lists:[],
				loading:0
			}
		},
		onReachBottom() {
			//触底
			if(this.loading) return;
			this.getList();
		},
		methods: {
			doSearch(){
				if(!this.keyword){
					showMsg('this',{title:'请输入关键字'});
					return;
				}
				if(this.loading) return;
				this.getList(true);
			},
			async getList(flag=false){
				if(flag){
					this.page = 1;
					this.lists = [];
				}
				this.loading = 1;
				let res = await request({
					method:'POST',
					data:{keyword:this.keyword,page:this.page},
					url:"/apphome/search"
				});
				this.loading = 0;
				console.log(res);
				if(res?.data?.lists){
					if(res.data.lists.length>0){
						this.lists = [...this.lists,...res.data.lists];
						this.page++;
					}
				}
			},
			tonewsZX(id){
				console.log("article_id="+id);
				navTo('/pages/public/article',{article_id:id});
			},
		}
	}
</script>

<style>
	page{
		background-color: #f8f8f8;
	}
	.search-box{
		width: 80%;
		margin-top: 10px;
		margin-left:auto;
		margin-right: auto;
		background-color: #ddd;
		padding-left: 90rpx;
		height: 36px;
		line-height: 36px;
		border-radius: 18px;
		position: relative;
	}
	.search-icon{
		position: absolute;
		top: 0;
		left: 0;
		width: 80rpx;
		height: 36px;
	}
	.search-btn{
		position: absolute;
		top: 0;
		right: 0;
		width: 80rpx;
		height: 36px;
		z-index: 99;
	}
	.search-input{
		height: 36px;
		line-height: 36px;
	}
	.news-list{
		width: 710rpx;
		padding-top: 10px;
		padding-left: 20rpx;
		padding-right: 20rpx;
	}
	.news-item{
		padding: 20rpx;
		background-color: #fff;
		border-radius: 12rpx;
	}
	.news-item-title{
		width: 670rpx;
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
		width: 670rpx;
		height: 160px;
	}
</style>
