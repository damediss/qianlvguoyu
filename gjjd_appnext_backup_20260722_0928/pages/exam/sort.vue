<template>
	<view class="content">
		<view class="header bg-ff">
			<view class="w-full text-C">已答:<text class="mar-L10">{{answer}}</text></view>
			<view style="width: 1rpx;height: 25px;background-color: #929292;"></view>
			<view class="w-full text-C">未答:<text class="mar-L10">{{list.length-answer}}</text></view>
		</view>
		<view class="body" v-if="list.length>0">
			<template v-for="(item,i) in list">
				<view class="body-item mar-B10">
					<view class="body-item-text" :class="isSubjectExam(item) || hasResult(item)?'bg-00b6ff color-ff':'border-92'" @click="toExam(item)">{{i+1}}</view>
				</view>
			</template>
		</view>
		<view class="text-C color-92 mar-T20" v-if="list.length>0 && subjectCount==0">未接收到简答题</view>
	</view>
</template>

<script>
	import {request,uploads,rectTo,navTo,showMsg} from "@/utils";
	export default {
		data() {
			return {
				paper_id:0,
				exam_id:0,
				list:[],
				answer:0,
				subjectCount:0
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
			}
		},
		onShow() {
			this.getList();
		},
		methods: {
			isSubjectExam(item){
				if(!item) return false;
				if(!item.type) return false;
				return !['multi','single','judge'].includes(item.type + '');
			},
			hasResult(item){
				return item && item.result && Object.keys(item.result).length>0;
			},
			logPracticeStats(label,list){
				let stats = {};
				if(Array.isArray(list)){
					for(let i in list){
						let type = list[i] && list[i].type ? list[i].type : 'unknown';
						stats[type] = (stats[type] || 0) + 1;
					}
				}
				console.log(label,{
					total: Array.isArray(list) ? list.length : 0,
					stats
				});
			},
			toExam(item){
				console.log("exam item",item);
				let param = {
					paper_id:this.paper_id,
					exam_id:this.exam_id,
					detail_id:item.id,
					detail_exam_id:item.exam_id
				};
				navTo("/pages/exam/sortExam",param);
			},
			async getList(){
				uni.showLoading({
					title:'数据加载中...'
				});
				let res = await request({
					method:'POST',
					data:{exam_id:this.exam_id,exam_type:3},
					url:"/appreview/sortlist"
				});
				uni.hideLoading();
				console.log('exam sort getData',res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg});
				}
				if(res?.data?.list){
					this.list = res.data.list;
					this.logPracticeStats('sortlist practice stats',this.list);
					this.answer = 0;
					this.subjectCount = 0;
					for(let i in this.list){
						if(this.isSubjectExam(this.list[i])){
							this.subjectCount++;
						}
						if(this.isSubjectExam(this.list[i]) || this.hasResult(this.list[i])){
							this.answer++;
						}
					}
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
		display: flex;
		align-items: center;
		justify-content: space-between;
		height: 40px;
		padding: 0 20rpx;
	}
	.body{
		display: flex;
		align-items: center;
		flex-wrap: wrap;
		padding-top: 20px;
	}
	.body-item{
		width: 150rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}
	.body-item-text{
		width: 100rpx;
		height: 24px;
		line-height: 24px;
		border-radius: 12px;
		text-align: center;
	}
</style>
