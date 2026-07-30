<template>
	<view class="content">
		<view class="filter-bar bg-ff">
			<view
				v-for="tab in filterTabs"
				:key="tab.value"
				class="filter-item"
				:class="filterType===tab.value?'filter-item-active':'filter-item-normal'"
				@click="setFilter(tab.value)"
			>{{tab.label}}</view>
		</view>
		<view class="header bg-ff">
			<view class="w-full text-C">已答:<text class="mar-L10">{{filteredAnswer}}</text></view>
			<view style="width: 1rpx;height: 25px;background-color: #929292;"></view>
			<view class="w-full text-C">未答:<text class="mar-L10">{{displayList.length-filteredAnswer}}</text></view>
		</view>
		<view class="body" v-if="displayList.length>0">
			<view class="body-item mar-B10" v-for="(item,i) in displayList" :key="item.id || i">
				<view class="body-item-text" :class="isSubjectExam(item) || hasResult(item)?'bg-00b6ff color-ff':'border-92'" @click="toExam(item)">{{getOriginNo(item)}}</view>
			</view>
		</view>
		<view class="text-C color-92 mar-T20" v-else-if="list.length>0">暂无该题型</view>
		<view class="text-C color-92 mar-T20" v-if="filterType==='all' && list.length>0 && subjectCount==0">未接收到简答题</view>
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
				subjectCount:0,
				filterType:'all',
				filterTabs:[
					{label:'全部',value:'all'},
					{label:'单选题',value:'single'},
					{label:'多选题',value:'multi'},
					{label:'判断题',value:'judge'}
				]
			}
		},
		computed: {
			displayList(){
				if(this.filterType === 'all'){
					return this.list;
				}
				return this.list.filter(item => item && (item.type + '') === this.filterType);
			},
			filteredAnswer(){
				let count = 0;
				for(let i = 0; i < this.displayList.length; i++){
					const item = this.displayList[i];
					if(this.isSubjectExam(item) || this.hasResult(item)){
						count++;
					}
				}
				return count;
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
			setFilter(type){
				this.filterType = type;
			},
			getOriginNo(item){
				const idx = this.list.indexOf(item);
				return idx >= 0 ? idx + 1 : '';
			},
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
					this.subjectCount = 0;
					for(let i in this.list){
						if(this.isSubjectExam(this.list[i])){
							this.subjectCount++;
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
	.filter-bar{
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 16rpx 20rpx 8rpx;
	}
	.filter-item{
		flex: 1;
		margin: 0 8rpx;
		height: 56rpx;
		line-height: 56rpx;
		text-align: center;
		border-radius: 28rpx;
		font-size: 26rpx;
	}
	.filter-item-active{
		background-color: #00b6ff;
		color: #fff;
	}
	.filter-item-normal{
		background-color: #fff;
		color: #333;
		border: 1px solid #929292;
		box-sizing: border-box;
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
