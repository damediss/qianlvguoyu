<template>
	<view>
		<template v-if="ximus.length>0">
			<view class="row-box mar-B10" v-for="(ximu,i) in ximus">
				<view class="font-wb">{{ximu.title}}</view>
				<view class="">题量:{{ximu.nums}}</view>
				<view>状态:{{ximu.status}}</view>
				<text class="text-audit" @click="onAudit(i)">去审核</text>
			</view>
		</template>
		<template v-else>
			<view class="text-C dis-flex-hv" style="height: 100px;">
				<view v-if="loading">
					<view><i class="gjjd-iconfont icon-record color-92 font-s30"></i></view>
					<view><text class="color-92">暂无审核任务</text></view>
				</view>
			</view>
		</template>
		<uni-popup ref="ximu" type="bottom" :mask-click="false">
			<view class="ximu-list">
				<view class="popup-top">
					<text class="ximu-list-close" @click="onXimuClose">关闭</text>
				</view>
				<view class="popup-head text-C">
					{{current.title}} ({{current.nums}})
				</view>
				<view class="popup-body">
					<view class="ximu-itemh">
						<view class="w-full">鉴定点</view>
						<view class="w-100 text-C">题量</view>
					</view>
					<template v-if="ximuDetails.length>0">
						<view class="ximu-itemb scrollbar-none">
							<template v-for="(xm,i) in ximuDetails">
							<view class="ximu-item" @click="onAuditDetail(xm,i)" v-if="xm.list.length>0">
								<view class="w-full">
									<view v-for="(txt,sno) in xm.title"><{{sno}}>{{txt}}</view>
								</view>
								<view class="w-100 text-C">
									<view>{{xm.list.length}}</view>
									<view class="ximu-btn"><text>处理</text></view>
								</view>
							</view>
							</template>
						</view>
					</template>
				</view>
			</view>
		</uni-popup>
		<uni-popup ref="ximudetail" type="center" :mask-click="false">
			<view class="ximu-detail">
				<view style="padding:20rpx;margin-bottom: 20;">
					<view v-for="(txt,sno) in currentXimu.title"><{{sno}}>{{txt}}</view>
					<view class="color-red">共{{currentXimu.list.length}}题</view>
				</view>
				<view class="ximu-detial-audit" v-if="currentXimu.list.length>0">
					<view class="dis-flex mar-B10">
						<view class="w-140 w-input">编号</view>
						<view class="w-full">
							<input class="input" v-model="currentXimu.list[0].sno_item" disabled />
						</view>
					</view>
					<view class="dis-flex mar-B10">
						<view class="w-140 w-input">内容</view>
						<view class="w-full">
							<textarea class="textarea" v-model="currentXimu.list[0].sno_title"></textarea>
						</view>
					</view>
					<view class="ximu-sumit" @click="ximuSubmit()">提交</view>
					<view class="mar-T20 text-C font-s12 color-red"><text>注:提交成功后会自动显示一下题</text></view>
				</view>
			</view>
			<view class="ximu-detail-close mar-T20">
				<i @click="onXimuDetailClose" class="gjjd-iconfont icon-shanchuguanbiquxiaowubiankuang color-ff font-s30"></i>
			</view>
		</uni-popup>
	</view>
</template>

<script>
	import {request,uploads,makeCall,rectTo,navTo,showMsg} from "@/utils";
	export default {
		data() {
			return {
				loading:0,
				role:0,
				ximus:[],
				index:-1,
				current:{title:'',nums:0},
				ximuDetails:[],
				currentXimu:{title:'',list:[]},
				isSubmit:0
			}
		},
		onLoad(options) {
			//console.log("options",options);
			if(options.item){
				let item = JSON.parse(decodeURIComponent(options.item));
				console.log("item",item);
				this.role = item.role;
				this.getData();
			}
		},
		onShow() {
			
		},
		methods: {
			async getData(){
				this.loading = 0;
				uni.showLoading({
					title:'数据加载中'
				});
				let res = await request({
					method:'POST',
					data:{role:this.role},
					url:"/apptask/ximu"
				});
				uni.hideLoading();
				this.loading = 1;
				console.log(res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg})
				}
				if(res?.data?.ximus){
					this.ximus = res.data.ximus;
				}
			},
			async onAudit(index){
				this.index = index;
				this.current = this.ximus[index];console.log("****",this.current)
				uni.showLoading({
					title:'数据加载中'
				});
				let res = await request({
					method:'POST',
					data:{task_id:this.current.id,role:this.role},
					url:"/apptask/ximudetail"
				});
				uni.hideLoading();
				console.log(res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg});
					return;
				}
				if(res?.data?.list && res.data.list.length>0){
					this.ximuDetails = res.data.list;
					//console.log('****',this.ximuDetails)
					this.$refs.ximu.open();
				}else{
					showMsg('this',{title:'当前没有要审的题了'});
				}
			},
			onXimuClose(){
				this.index = -1;
				this.current = {title:'',nums:0};
				this.ximuDetails = [];
				this.$refs.ximu.close();
			},
			onAuditDetail(ximu,i){
				this.currentXimu = ximu;
				this.$refs.ximudetail.open();
			},
			onXimuDetailClose(){
				this.currentXimu = {title:'',list:[]};
				this.$refs.ximudetail.close();
			},
			async ximuSubmit(){
				let param = this.currentXimu.list[0];
				param.role = this.role;
				//console.log(param);return;
				if(this.isSubmit) return;
				this.isSubmit = 1;
				uni.showLoading({
					title:'数据加载中'
				});
				let res = await request({
					method:'POST',
					data:param,
					url:"/apptask/taskximusubmit"
				});
				console.log(res);
				if(res.code){
					this.isSubmit = 0;
					uni.hideLoading();
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg});
					return;
				}
				showMsg('this',{title:'成功'});
				let tid = setTimeout(()=>{
					clearTimeout(tid);
					this.currentXimu.list.splice(0,1);
					this.ximus[this.index].nums--;
					this.isSubmit = 0;
					uni.hideLoading();
					if(this.currentXimu.list.length == 0){
						this.onXimuDetailClose();
					}
				},1500);
			}
		}
	}
</script>

<style>
	page{
		background-color: #f8f8f8;
	}
	.row-box{
		width: 690rpx;
		margin-left: 20rpx;
		margin-right: 20rpx;
		padding: 10rpx;
		background-color: #fff;
		border-radius: 12rpx;
		position: relative;
	}
	.text-audit{
		position: absolute;
		top: 4px;
		right: 10rpx;
		padding: 4px 20rpx;
		color: #fff;
		background-color: #4ec6f6;
		border-radius: 6rpx;
		font-size: 12px;
	}
	.ximu-list{
		width: 750rpx;
		height: 600px;
		background-color: #fff;
		border-top-left-radius: 12rpx;
		border-top-right-radius: 12rpx;
	}
	.ximu-itemh{
		display: flex;
		align-items: center;
		margin-bottom: 5px;
		background-color: #B68320;
		border: 1rpx solid #B68320;
		padding: 0 6rpx;
		color: #fff;
	}
	.ximu-itemb{
		height: 500px;
		overflow-y: auto;
	}
	.ximu-item{
		display: flex;
		align-items: center;
		margin-bottom: 10px;
		border: 1rpx solid #B68320;
		padding: 0 6rpx;
	}
	.ximu-btn text{
		background-color: #4ec6f6;
		padding: 2px 8rpx;
		border-radius: 6rpx;
		color: #fff;
		font-size: 12px;
	}
	.ximu-list-close{
		position: absolute;
		top: 3px;
		right: 6px;
		z-index: 9;
		font-size: 14px;
		border: 1rpx solid #B68320;
		border-radius: 6rpx;
		padding: 2px 6rpx;
	}
	.ximu-detail{
		width: 690rpx;
		background-color: #fff;
		border-radius: 12rpx;
		padding-bottom: 20px;
	}
	.ximu-detail-close{
		height: 30px;
		text-align: center;
	}
	.ximu-detial-audit{
		padding-left: 10rpx;
		padding-right: 10rpx;
	}
	.popup-top{
		height: 20px;
	}
	.popup-head{
		height: 40px;
		line-height: 40px;
		font-weight: bold;
	}
	.popup-body{;
		padding: 10px;
	}
	.textarea{
		width: 90%;
		height: 80px;
		padding: 4px 6px;
		border: 1rpx solid #dedede;
		border-radius: 8rpx;
	}
	.input{
		width: 90%;
		height: 30px;
		line-height: 30px;
		padding: 4px 6px;
		border: 1rpx solid #dedede;
		border-radius: 8rpx;
	}
	.w-input{
		height: 30px;
		line-height: 30px;
	}
	.ximu-sumit{
		height: 44px;
		line-height: 44px;
		margin-top:10px;
		margin-left: 40rpx;
		margin-right: 40rpx;
		font-size: 16px;
		font-weight: bold;
		text-align: center;
		color: #fff;
		background-color: #4ec6f6;
		border-radius: 22px;
	}
</style>
