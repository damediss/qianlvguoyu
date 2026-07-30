<template>
	<view class="content">
		<view class="tabs">
			<view class="tabs-item">
				<view @click="onTabs(0)">
					<view class="text-C mar-B5">我的</view>
					<view class="dis-flex-hv">
						<view class="tabs-active" v-if="tabIndex==0"></view>
					</view>
				</view>
			</view>
			<view class="tabs-item">
				<view @click="onTabs(1)">
					<view class="text-C mar-B5">单位</view>
					<view class="dis-flex-hv">
						<view class="tabs-active" v-if="tabIndex==1"></view>
					</view>
				</view>
			</view>
		</view>
	<view class="body" :class="{'bg-ff':list.length>0}">
		<template v-if="list.length==0">
			<view class="mar-T20 text-C">您当前还未添加任何学习路径</view>
		</template>
			<template v-else>
				<view class="row-item border-top-cc" v-for="item in list">
					<view class="item-left">
						<view>{{item.title}}</view>
						<view class="color-92 font-s12">{{item.date}}</view>
					</view>
					<view class="item-right text-R">
						<text class="font-s16">{{item.level}}</text>
					</view>
					<view class="item-icon text-R">
						<i class="gjjd-iconfont icon-shanchu color-red font-s20" @click="onDelete(item.id)"></i>
					</view>
				</view>
			</template>
		</view>
		<view class="footer">
			<view class="footer-btn bg-00b6ff color-ff text-C" @click="$refs.optionadd.open()">添加</view>
		</view>
		<uni-popup ref="optionadd" type="bottom" :is-mask-click="false">
			<view class="popup">
				<view class="popup-header dis-flex-hsb">
					<view class="mar-L20">添加学习路径</view>
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
						<view class="item-right">日期</view>
						<view class="item-left text-R font-s14">
							<picker mode="date" :value="date" :start="startDate" :end="endDate" @change="binddateChange">
								<view class="uni-input">{{date}}</view>
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
				tabIndex:0,
				list:[],
				page:1,
				course:'选择科目',
				course_id:0,
				cindex:-1,
				courseAll:[],
				courseList:[],
				date:'选择日期',
				isSubmit:false,
				load_finish:0
			}
		},
		onLoad() {
			this.getList(false);
		},
		onReachBottom() {
			if(this.loading) return;
			this.getList();
		},
		computed: {
			startDate() {
				return this.getDate('start');
			},
			endDate() {
				return this.getDate('end');
			}
		},
		methods: {
			onTabs(i){
				if(this.loading) return;
				console.log('i='+i);
				if(i != this.tabIndex){
					this.tabIndex = i;
					this.list = [];
					this.getList(true);
				}
			},
			optionaddClose(){
				this.course_id = 0;
				this.course = '选择科目';
				this.date = '请选择日期';
				this.$refs.optionadd.close();
			},
			bindcourseChange(e){
				this.cindex = Number(e.detail.value);
				this.course = this.courseAll[this.cindex].title;
				this.course_id = this.courseAll[this.cindex].id;
			},
			binddateChange(e){
				this.date = e.detail.value;
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
			async getList(flag=false){
				if(flag){
					this.load_finish = 0;
					this.page = 1;
					this.list = [];
				}
		if(this.loading) return;
		this.loading = 1;
		uni.showLoading({
			title:'数据加载中...'
		});
				let res = await request({
					method:'POST',
					data:{page:this.page,type:this.tabIndex},
					url:"/apppromote/path"
				});
				uni.hideLoading();
				this.loading = 0;
				console.log('promote path getData',res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg});
					return;
				}
				if(res?.data?.list){
					if(res.data.list.length>0){
						this.list = [...this.list,...res.data.list];
						this.page++;
					}else{
						this.load_finish = 1;
					}
				}
				if(res?.data?.courseAll){
					this.courseAll = res.data.courseAll;
					this.courseList = [];
					for(let i in this.courseAll){
						this.courseList.push(this.courseAll[i].title);
					}
				}
			},
			async onSubmit(){
				if(!this.course_id){
					showMsg('this',{title:'请选择科目'});
					return;
				}
				if(!this.date){
					showMsg('this',{title:'请选择日期'});
					return;
				}
		if(this.isSubmit) return;
		this.isSubmit = 1;
		let param = {course_id:this.course_id,date:this.date,type:this.tabIndex}
		uni.showLoading({
			title:'数据加载中...'
		});
				let res = await request({
					method:'POST',
					data:param,
					url:"/apppromote/pathsave"
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
			},
			onDelete(id){
				let that = this;
				uni.showModal({
					title:'友情提示',
					content:'确定要删除该项吗?',
					success: async (e)=>{
						if(e.confirm){
							let res = await request({
								method:'POST',
								data:{id,type:this.tabIndex},
								url:"/apppromote/pathdelete"
							});
							if(res.code){
								let msg = '请求失败';
								if(res?.msg && res.msg){
									msg = res.msg;
								}
								showMsg('this',{title:msg});
								return;
							}
							showMsg('this',{title:res.msg});
							let tid = setTimeout(()=>{
								clearTimeout(tid);
								that.getList(true);
							},2000);
						}
					}
				})
			}
		}
	}
</script>

<style>
	page{
		background-color: #f8f8f8;
	}
	.tabs{
		height: 44px;
		display: flex;
		align-items: center;
		justify-content: space-between;
		background-color: #fff;
	}
	.tabs-item{
		width: 50%;
		padding: 5px;
		display: flex;
		align-items: center;
		justify-content: center;
	}
	.tabs-active{
		width: 60%;
		height: 4px;
		background-color: #73a9fa;
		border-radius: 2px;
	}
	.body{
		
	}
	.border-top-cc{
		border-top: 1rpx solid #ccc;
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
