<template>
	<view class="content">
		<view class="steps-box mar-B10" v-for="(item,wkid) in list">
			<uni-steps :options="Object.values(item)" direction="column" active-icon="locked" :active="active[wkid]-1"></uni-steps>
		</view>
		<view class="footer">
			<view class="footer-btn bg-00b6ff color-ff text-C" @click="$refs.optionadd.open()">添加</view>
		</view>
		<uni-popup ref="optionadd" type="bottom" :is-mask-click="false">
			<view class="popup">
				<view class="popup-header dis-flex-hsb">
					<view class="mar-L20">添加已获得证书</view>
					<view class="mar-R20"><i class="gjjd-iconfont icon-shanchuguanbiquxiaowubiankuang" @click="optionaddClose"></i></view>
				</view>
				<view class="popup-body">
					<view class="row-item">
						<view class="item-right">科目</view>
						<view class="item-left text-R font-s14">
							<picker @change="bindcourseChange" :value="cindex" :range="courseList">
								<view class="uni-input">{{course}}</view>
							</picker>
						</view>
						<view class="item-icon">
							<i class="gjjd-iconfont icon-xiangyou color-92 font-s18"></i>
						</view>
					</view>
					<view class="row-item">
						<view class="item-right">获得年份</view>
						<view class="item-left text-R font-s14">
							<picker @change="bindyearChange" :value="yindex" :range="years">
								<view class="uni-input">{{year}}</view>
							</picker>
						</view>
						<view class="item-icon">
							<i class="gjjd-iconfont icon-xiangyou color-92 font-s18"></i>
						</view>
					</view>
				</view>
				<view class="popup-footer">
					<view class="bg-00b6ff color-ff" @click="onSubmit">提交</view>
				</view>
			</view>
		</uni-popup>
	</view>
</template>

<script>
	import {request,uploads,rectTo,navTo,showMsg} from "@/utils";
	export default {
		data() {
			return {
				list:{},
				course:'选择科目',
				course_id:0,
				work_type_id:0,
				cindex:-1,
				courseAll:[],
				courseList:[],
				active:{},
				year:'选择年份',
				years:[],
				yindex:0,
				isSubmit:false
			}
		},
		onLoad() {
			this.getList(false);
		},
		computed: {
			
		},
		methods: {
			optionaddClose(){
				this.course_id = 0;
				this.work_type_id = 0;
				this.course = '选择科目';
				this.year = '请选择年份';
				this.$refs.optionadd.close();
			},
			bindcourseChange(e){
				this.cindex = Number(e.detail.value);
				this.course = this.courseAll[this.cindex].title;
				this.course_id = this.courseAll[this.cindex].id;
				this.work_type_id = this.courseAll[this.cindex].work_type_id;
			},
			bindyearChange(e){
				this.year = this.years[e.detail.value];
			},
			getDate(type) {
				const date = new Date();
				let year = date.getFullYear();
				let month = date.getMonth() + 1;
				let day = date.getDate();
				
				if (type === 'start') {
					year = year - 30;
				} else if (type === 'end') {
					year = year + 10;
				}
				month = month > 9 ? month : '0' + month;
				day = day > 9 ? day : '0' + day;
				return `${year}-${month}-${day}`;
			},
		async getList(){
			uni.showLoading({
				title:'数据加载中...'
			});
				let res = await request({
					method:'POST',
					data:{},
					url:"/apppromote/map"
				});
				uni.hideLoading();
				console.log('promote map getData',res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg});
					return;
				}
				if(res?.data?.list){
					this.list = res.data.list;
				}
				if(res?.data?.courseAll){
					this.courseAll = res.data.courseAll;
					this.courseList = [];
					for(let i in this.courseAll){
						this.courseList.push(this.courseAll[i].title);
					}
				}
				if(res?.data?.active){
					this.active = res.data.active;
				}
				if(res?.data?.years){
					this.years = res.data.years;
					this.yindex = this.years.length - 1;
				}
			},
			async onSubmit(){
				if(!this.course_id){
					showMsg('this',{title:'请选择科目'});
					return;
				}
				if(!this.year){
					showMsg('this',{title:'请选择年份'});
					return;
				}
				if(this.isSubmit) return;
		this.isSubmit = 1;
		let param = {course_id:this.course_id,year:this.year,work_type_id:this.work_type_id}
		uni.showLoading({
			title:'数据加载中...'
		});
				let res = await request({
					method:'POST',
					data:param,
					url:"/apppromote/mapsave"
				});
				uni.hideLoading();
				this.isSubmit = 0;
				if(res.code){
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg});
					return;
				}
				showMsg('this',{title:res.msg});
				this.optionaddClose();
				let tid = setTimeout(()=>{
					clearTimeout(tid);
					this.getList(true);
				},2000);
			}
		}
	}
</script>

<style>
	page{
		background-color: #F3F6FB;
	}
	.content{
		padding: 10px 0;
	}
	.steps-box{
		margin-left: 20rpx;
		margin-right: 20rpx;
		padding: 10px 20rpx;
		background-color: #fff;
		border-radius: 12rpx;
	}
	.row-item{
		display: flex;
		align-items: center;
		padding: 5px 20rpx;
	}
	.item-left{
		width: 70%;
	}
	.item-right{
		width: 22%;
	}
	.item-icon{
		width: 8%;
	}
	.footer{
		position: fixed;
		bottom: 10px;
		left: 0;
		right: 0;
	}
	.footer-btn{
		margin:  0 20rpx;
		height: 44px;
		line-height: 44px;
		border-radius: 22px;
	}
	.popup{
		width: 750rpx;
		border-top-left-radius: 20rpx;
		border-top-right-radius: 20rpx;
		background-color: #fff;
	}
	.popup-header{
		padding-top: 10px;
	}
	.popup-body{
		padding: 10px 20rpx;
	}
	.popup-footer{
		padding-top: 10px;
		padding-bottom: 10px;
	}
	.popup-footer view{
		margin: 0 20rpx;
		height: 40px;
		line-height: 40px;
		text-align: center;
		border-radius: 20px;
	}
</style>
