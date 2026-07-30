<template>
	<view class="content">
		<view class="header text-C">共{{ examList.length }} 题</view>
		<view class="body">
			<template v-for="type in orderedTypes">
				<view class="dis-flex-hsb">
					<view>{{typeLabel(type)}}</view>
					<view>{{typeList[type].num}}</view>
				</view>
				<view class="border-line"></view>
				<view class="dis-flex-hsb">
					<view>抽取({{typeLabel(type)}})</view>
					<view class="dis-flex-hsb">
						<text class="add-jian" @click="changeNum(type,0)">-</text>
						<input v-model="typeList[type].takeNum" :min="0" :max="typeList[type].num" class="add-num">
						<text class="add-jia" @click="changeNum(type,1)">+</text>
					</view>
				</view>
				<view class="border-line"></view>
			</template>
		</view>
		<view class="footer">
			<view class="footer-btn bg-73a9fa color-ff" @click="onConfirm">确定</view>
		</view>
	</view>
</template>

<script>
	import {request,uploads,rectTo,navTo,showMsg} from "@/utils";
	export default {
		data() {
			return {
				paper_id:0,
				exam_id:0,
				type:0,
				examList:[],
				typeList:{
					single:{num:0,takeNum:0},
					multi:{num:0,takeNum:0},
					judge:{num:0,takeNum:0}
				},
				typeLabels:{
					single:'单选题',
					multi:'多选题',
					judge:'判断题',
					answer:'简答题',
					analysis:'分析题',
					cal:'计算题',
					essay:'论述题',
					explain:'解释题',
					fill:'填空题',
					drafting:'制图题'
				},
				typeOrder:['single','multi','judge','answer','analysis','cal','essay','explain','fill','drafting','']
			}
		},
		onLoad(options) {
			if(options.item){
				let item = JSON.parse(decodeURIComponent(options.item));
				console.log(item);
				if(item.paper_id){
					this.paper_id = item.paper_id;
				}
				if(item.exam_id){
					this.exam_id = item.exam_id;
				}
				if(item.type){
					this.type = item.type;
				}
				this.getList();
			}
		},
		methods: {
			async getList(){
				uni.showLoading({
					title:'数据加载中...'
				});
				let res = await request({
					method:'POST',
					data:{paper_id:this.paper_id,exam_id:this.exam_id,exam_type:4},
					url:"/appreview/randlist"
				});
				uni.hideLoading();
				console.log('exam rand getData',res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('back',{title:msg});
				}
				if(res?.data?.list){
					this.examList = res.data.list;
				}
				if(res?.data?.typelist){
					this.typeList = res.data.typelist;
				}
			},
			changeNum(type,tag){
				if(!this.typeList[type]){
					return;
				}
				if(tag>0){
					this.typeList[type]['takeNum']++;
					if(this.typeList[type]['takeNum']>=this.typeList[type]['num']){
						this.typeList[type]['takeNum'] = this.typeList[type]['num'];
					}
				}else{
					this.typeList[type]['takeNum']--;
					if(this.typeList[type]['takeNum']<0){
						this.typeList[type]['takeNum'] = 0;
					}
				}
			},
			typeLabel(type){
				return this.typeLabels[type] || (type ? type : '其他题型');
			},
			onConfirm(){
				let param = {
					paper_id:this.paper_id,
					exam_id:this.exam_id,
					typelist:JSON.stringify(this.typeList)
				};
				navTo("/pages/exam/randExam",param);
			}
		},
		computed:{
			orderedTypes(){
				let keys = Object.keys(this.typeList || {});
				keys.sort((a,b)=>{
					let ai = this.typeOrder.indexOf(a);
					let bi = this.typeOrder.indexOf(b);
					if(ai < 0) ai = 999;
					if(bi < 0) bi = 999;
					if(ai == bi) return a.localeCompare(b);
					return ai - bi;
				});
				return keys;
			}
		}
	}
</script>

<style>
	page{
		background-color: #f8f8f8;
	}
	.header{
		height: 40px;
		line-height: 40px;
		background-color: #fff;
	}
	.body{
		margin-top: 10px;
		margin-left: 20rpx;
		margin-right: 20rpx;
		padding: 20rpx;
		background-color: #fff;
		border-radius: 12rpx;
	}
	.border-line{
		width: 100%;
		height: 1rpx;
		margin: 10px 0;
		background-color: #ccc;
	}
	.add-jian{
		background-color: #ccc;
		border-radius: 4rpx;
		color: #000;
		padding: 3px 20rpx;
		font-size: 16;
	}
	.add-jia{
		background-color: #ddd;
		border-radius: 4rpx;
		color: #000;
		padding: 3px 20rpx;
		font-size: 16;
	}
	.add-num{
		width: 60rpx;
		padding: 2px 6rpx;
		margin: 0 20rpx;
		font-size: 14px;
		text-align: center;
	}
	.footer{
		margin-top: 20px;
		padding: 0 20rpx;
	}
	.footer-btn{
		height: 40px;
		line-height: 40px;
		border-radius: 20px;
		text-align: center;
	}
</style>