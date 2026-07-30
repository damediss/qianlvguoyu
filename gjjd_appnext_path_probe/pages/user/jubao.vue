<template>
	<view class="content">
		<view class="mar-T10 mar-L20 mar-R20">举报内容:</view>
		<view class="mar-T10 mar-L20 mar-R20">
			<textarea class="textarea" v-model="content"></textarea>
		</view>
		<view class="btn bg-73a9fa color-ff" @click="onSubmit">提交</view>
	</view>
</template>

<script>
	import {request,uploads,makeCall,rectTo,navTo,showMsg} from "@/utils";
	export default {
		data() {
			return {
				content:'',
				isSubmit:0
			}
		},
		methods: {
			async onSubmit(){
				if(!this.content){
					showMsg('this',{title:'请输入内容'});
					return;
				}
				if(this.isSubmit) return;
				this.isSubmit = 1;
				uni.showLoading({
					title:'数据提交中...'
				});
				const res = await request({
					method:'POST',
					data:{content:this.content},
					url:"/appuser/jubao"
				});
				uni.hideLoading();
				console.log(res);
				showMsg('this',{title:res.msg});
				if(!res.code){
					let timeid = setTimeout(()=>{
						clearTimeout(timeid);
						uni.navigateBack();
					},2000);
				}
			}
		}
	}
</script>

<style>
	.textarea{
		width: 670rpx;
		padding: 20rpx;
		border: 1rpx solid #929292;
		border-radius: 10rpx;
	}
	.btn{
		margin-top: 10px;
		margin-left: 20rpx;
		margin-right: 20rpx;
		height: 40px;
		line-height: 40px;
		text-align: center;
		border-radius: 20px;
	}
</style>
