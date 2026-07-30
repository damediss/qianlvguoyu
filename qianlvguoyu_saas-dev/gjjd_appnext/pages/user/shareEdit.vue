<template>
	<view class="content">
		<view style="position:fixed;top:5px;left:20rpx;right:20rpx;z-index:9999;background-color:rgba(0, 0, 0, 0.6);padding: 10rpx;color:#fff;border-radius:8rpx;" v-if="showpermissionTip">
			<view style="font-weight: bold;">相册、相机授权提示</view>
			<view style="margin-top: 10px;">为了实现扫码功能、相关材料上传，需要访问您的相册、相机权限，您如果拒绝开启,将无法使用上述功能。</view>
		</view>
		<!-- 选择图片 -->
		<view class="imgs">
			<view class="imgs-item" v-for="(img) in imgs">
				<image class="imgs-item-pic" :src="img.src" model="widthFix"></image>
			</view>
			<view class="imgs-add dis-flex-hv" v-if="imgs.length<3">
				<view class="imgs-add-icon" @click="uploadImgs">+</view>
			</view>
		</view>
		<view class="input">
			<textarea v-model="content" placeholder="请输入内容"></textarea>
		</view>
		<view class="input-tip">
			分享你的学习、生活，发布工种相关的图文动态有机会上精选
		</view>
		<view class="btn-submit" @click="onSubmit">
			<text v-if="id>0">保存</text>
			<text v-else>发布</text>
		</view>
	</view>
</template>

<script>
	import {request,uploads,makeCall,rectTo,navTo,showMsg} from "@/utils";
	export default {
		data() {
			return {
				showpermissionTip:0,
				id:0,
				imgs:[],
				content:'',
				isSubmit:0
			}
		},
		onLoad(options) {
			//console.log("shareEdit",options);
			if(options.item){
				let item = JSON.parse(decodeURIComponent(options.item));
				//console.log("shareEdit item",item);
				if(item.shareId){
					this.id = item.shareId;
				}
			}
			if(this.id>0){
				this.getData();
			}
		},
		methods: {
			async getData(){
				let res = await request({
					method:'POST',
					data:{id:this.id},
					url:"/appuser/shareinfo"
				});
				console.log("share getData",res);
				if(res?.data?.share){
					this.imgs = res.data.share.imgs;
					this.content = res.data.share.content;
				}
			},
			async uploadImgs(){
				if(this.imgs.length==3){
					showMsg('this',{title:'图片最多选3张'});
					return;
				}
				let res = await uploads({limit:3},this);
				console.log('pic',res);
				if(res.length>0){
					for(let i in res){
						console.log('**pic=',res[i]);
						if(this.imgs.length>=3) break;
						this.imgs.push({pic:res[i],src:"https://cdn.app.gjjd168.com/"+res[i]});
					}
				}
			},
			async onSubmit(){
				if(this.imgs.length==0){
					showMsg('this',{title:'请选择图片'});
					return;
				}
				if(!this.content){
					showMsg('this',{title:'请输入内容'});
					return;
				}
				if(this.isSubmit){
					return;
				}
				this.isSubmit = 1;
				let imgs = [];
				for(let i in this.imgs){
					imgs.push(this.imgs[i].pic);
				}
				let param = {
					imgs,
					content:this.content
				};
				let res = await request({
					method:'POST',
					data:param,
					url:"/appuser/sharesave"
				});
				console.log("submit share",res);
				showMsg("this",{title:res.msg});
				if(res?.code){
					return;
				}
				if(res?.data?.info){
					uni.navigateBack();
				}
			}
		}
	}
</script>

<style>
	.imgs{
		width: 690px;
		margin-top: 10px;
		margin-left: 30rpx;
		margin-right: 30rpx;
		display: flex;
	}
	.imgs-item{
		width: 230rpx;
		height: 230rpx;
		display: flex;
		align-items: center;
	}
	.imgs-item-pic{
		width: 220rpx;
		height: 230rpx;
	}
	.imgs-add{
		width: 230rpx;
		height: 230rpx;
	}
	.imgs-add-icon{
		width: 220rpx;
		height: 228rpx;
		line-height: 228rpx;
		text-align: center;
		border: 1px dashed #dedede;
		font-size: 40px;
	}
	.input{
		width: 690px;
		margin-top: 5px;
		margin-left: 30rpx;
		margin-right: 30rpx;
	}
	.input textarea{
		width: 686rpx;
		padding: 2px;
		border: 1rpx solid #e0e0e0;
		border-radius: 6rpx;
		color: #626262;
	}
	.input-tip{
		margin-top: 5px;
		margin-left: 30rpx;
		margin-right: 30rpx;
		color: #929292;
		font-size: 12px;
	}
	.btn-submit{
		height: 44px;
		line-height: 44px;
		background-color: #e0e0e0;
		text-align: center;
		border-radius: 22px;
		position: fixed;
		bottom: 20px;
		left: 20rpx;
		right: 20rpx;
	}
</style>