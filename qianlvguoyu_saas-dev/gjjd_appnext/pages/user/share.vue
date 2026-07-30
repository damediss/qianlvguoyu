<template>
	<view class="content">
		<!-- 分享列表 -->
		<view v-if="list.length>0" class="share-list">
			<template v-for="(item,i) in list">
				<view class="share-item mar-B20" @click="hideTools(i)">
					<view class="user-box">
						<image class="user-avatar" :src="item.avatar"></image>
						<view class="user-info">
							<view>{{item.realname}}</view>
							<view class="font-s12 color-92">
								<text class="mar-R10">{{item.datetime}}</text>
								<text class="mar-R10">{{item.location}}</text>
								<text>{{item.see_count}}次浏览</text>
							</view>
						</view>
						<view class="user-tool">
							<text @click.stop="onTools(i)">...</text>
							<view class="user-tool-list" v-if="item.show">
								<view class="mar-B10" @click.stop="toShareEdit(item.id)">编辑</view>
								<view @click="toShareDel(item.id,i)">删除</view>
							</view>
						</view>
					</view>
					<view class="share-imgs">
						<image v-for="pic in item.imgs" :src="pic" mode="heightFix" class="share-img"></image>
					</view>
					<view class="share-content mar-T10">{{item.content}}</view>
				</view>
			</template>
		</view>
		<!-- 新增按钮 -->
		<view class="share-add">
			<i class="gjjd-iconfont icon-xinzeng font-s30" @click="toShareEdit(0)"></i>
		</view>
	</view>
</template>

<script>
	import {request,uploads,makeCall,rectTo,navTo,showMsg} from "@/utils";
	export default {
		data() {
			return {
				list:[],
				page:1,
				loading:0,
				finish:0
			}
		},
		onLoad(options) {
			//this.getData(true);
		},
		onShow() {
			this.getData(true);
		},
		onPullDownRefresh() {
			//下拉刷新
			if(this.loading) return;
			this.getData(true);
		},
		onReachBottom() {
			//触底
			if(this.loading || this.finish) return;
			this.getData(false);
		},
		methods: {
			toShareEdit(id){
				navTo("/pages/user/shareEdit",{shareId:id});
			},
			hideTools(i){
				this.list[i].show = 0;
			},
			onTools(i){
				if(this.list[i].show == 0){
					this.list[i].show = 1;
				}else{
					this.list[i].show = 0;
				}
			},
			toShareDel(id=0,index){
				if(id == 0) return;
				uni.showModal({
					title:'友情提示',
					content:'确要删除该分享吗?',
					success:(res)=>{
						if(res.confirm){
							this.onDeleted(id,index);
						}
					}
				})
			},
			async getData(flag=false){
				if(flag){
					this.list = [];
					this.page = 1;
					this.finish = 0;
				}
				if(this.finish) return;
				if(this.loading) return;
				this.loading = 1;
				let res = await request({
					method:'POST',
					data:{page:this.page},
					url:"/appuser/share"
				});
				this.loading = 0;
				console.log("my share",res);
				if(res?.code){
				showMsg("this",{title:res.msg});
					return;
				}
				if(res?.data?.list){
					if(res.data.list.length>0){
						this.list = [...this.list,...res.data.list];
						this.page++;
					}else{
						this.finish = 1;
					}
				}
			},
			async onDeleted(id=0,index){
				if(id == 0) return;
				let res = await request({
					method:'POST',
					data:{id},
					url:"/appuser/sharedelete"
				});
				this.loading = 0;
				console.log("my share delete",res);
				showMsg("this",{title:res.msg});
				if(res?.code){
					return;
				}
				this.list.splice(index,1);
			}
		}
	}
</script>

<style>
	.share-list{
		padding-top: 10px;
	}
	.user-box{
		display: flex;
		margin-bottom: 10px;
	}
	.user-avatar{
		margin-left: 20rpx;
		width: 100rpx;
		height: 100rpx;
		border-radius: 50rpx;
		background-color: #929292;
	}
	.user-info{
		margin-left: 10rpx;
		flex: 1;
	}
	.user-tool{
		width: 60rpx;
		height: 30px;
		line-height: 30px;
		text-align: center;
		position: relative;
	}
	.user-tool-list{
		position: absolute;
		bottom: -70px;
		right: 3px;
		z-index: 9999;
		width: 140rpx;
		background-color: rgba(0, 0, 0, 0.3);
		color: #fff;
	}
	.share-imgs{
		width: 750rpx;
		overflow-x: auto;
		display: flex;
		align-items: center;
		flex-wrap: nowrap;/*不换行*/
	}
	.share-img{
		width: 150rpx;
		height: 350px;
		flex-shrink: 0;/*防止项目缩小*/
		margin-left: 5px;
		border: 1rpx solid #626262;
	}
	.share-content{
		width: 710rpx;
		padding: 0 20rpx;
	}
	.share-add{
		position: fixed;
		bottom: 60px;
		right: 40px;
		z-index: 999;
	}
</style>
