<template>
	<view class="content">
		<view class="article-title">
			<view class="article-title-icon">
				<image src="/static/images/hot.png"></image>
			</view>
			<view class="article-title-text">{{article.title}}</view>
		</view>
		<view class="article-tag font-s12">
			<text class="color-73a9fa">#{{article.category}}#</text>
			<text class="color-cc font-s12 mar-L20">{{article.create_time}}</text>
			<text class="color-cc font-s12 mar-L20">{{article.virtual_views}} 浏览</text>
		</view>
		<view class="border-line mar-T20"></view>
		<view class="article-content">
			<uv-parse :content="article.content"></uv-parse>
		</view>
	</view>
</template>

<script>
	import {request,rectTo,navTo,showMsg} from "@/utils";
	export default {
		data() {
			return {
				article_id:0,
				article:{article_id:0,title:''}
			}
		},
		onLoad(options) {
			if(options.item){
				let item = JSON.parse(decodeURIComponent(options.item));
				console.log(item);
				if(item.article_id){
					this.article_id = item.article_id;
					this.getArticle();
				}
			}
		},
		methods: {
			async getArticle(){
				let res = await request({
					method:'POST',
					data:{article_id:this.article_id},
					url:"/apphome/articleinfo"
				});
				console.log(res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg})
				}
				if(res?.data?.article){
					this.article = res.data.article;
				}
			}
		}
	}
</script>

<style>
	.content{
		
	}
	.article-title{
		margin: 10px 20rpx;
		width: 710rpx;
		display: flex;
		align-items: center;
	}
	.article-title-icon{
		width: 20px;
		height: 20px;
	}
	.article-title-icon image{
		width: 20px;
		height: 20px;
	}
	.article-title-text{
		flex: 1;
		font-weight: bold;
	}
	.article-tag{
		margin: 0 20rpx;
	}
	.article-content{
		margin-top: 10px;
		margin-left: 20rpx;
		margin-right: 20rpx;
	}
</style>
