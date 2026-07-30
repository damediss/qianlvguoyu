<template>
	<view class="container">
		<view style="position:fixed;top:0;left:20rpx;right:20rpx;z-index:9999;background-color:rgba(0, 0, 0, 0.6);padding: 10rpx;color:#fff;border-radius:8rpx;" v-if="showpermissionTip">
			<view style="font-weight: bold;">相册、相机授权提示</view>
			<view style="margin-top: 10px;">为了实现扫码功能、相关材料上传，需要访问您的相册、相机权限，您如果拒绝开启,将无法使用上述功能。</view>
		</view>
		<!-- 选项卡导航栏 -->
		<view class="tab-bar">
			<view 
			:class="['tab-item', currentTab === 0 ? 'active' : '']" 
			@click="switchTab(0)"
			>基本信息</view>
			<view 
			:class="['tab-item', currentTab === 1 ? 'active' : '']" 
			@click="switchTab(1)"
			>教育信息</view>
			<view 
			:class="['tab-item', currentTab === 2 ? 'active' : '']"
			@click="switchTab(2)"
			>工作信息</view>
		</view>

		<!-- 基本信息 -->
		<view class="tab-content" v-show="currentTab === 0">
			<view class="form-item">
				<view class="label">姓名</view>
				<input class="input" placeholder="请输入姓名" v-model="userDetail.realname"/>
			</view>
			<view class="form-item">
				<view class="label">一寸白底证件照</view>
				<view v-if="userDetail.idcard_header">
					<image class="pic-header" :src="userDetail.idcard_header" mode="heightFix"></image>
				</view>
				<view class="text-R" v-else>
					<text>请上传</text>
					<i class="gjjd-iconfont icon-xiangyou"></i>
				</view>
			</view>

			<view class="form-item" style="display: flex;justify-content: space-between;">
				<view class="pic-idcard">
					<view class="label">身份证正面</view>
					<view v-if="userDetail.idcard_front_show" class="dis-flex-hv">
						<image class="pic-front" :src="userDetail.idcard_front_show" mode="heightFix"></image>
					</view>
					<view class="pic-idcar-upload" v-else>
						<view @click="uploadIdCardFront">
							<text>点击上传</text>
							<i class="gjjd-iconfont icon-xiangyou"></i>
						</view>
					</view>
				</view>
				<view class="pic-idcard">
					<view class="label">身份证反面</view>
					<view v-if="userDetail.idcard_back_show" class="dis-flex-hv">
						<image class="pic-back" :src="userDetail.idcard_back_show" mode="heightFix"></image>
					</view>
					<view class="pic-idcar-upload" v-else>
						<view @click="uploadIdCardBack">
							<text>点击上传</text>
							<i class="gjjd-iconfont icon-xiangyou"></i>
						</view>
					</view>
				</view>
			</view>
			<view class="form-item" >
				<view class="label" >身份证号</view>
				<input class="input" placeholder="请输入身份证号" v-model="userDetail.idcard" />
			</view>
			<view class="form-item">
				<view class="label" >身份证地址</view>
				<input class="input" placeholder="请输入身份证地址" v-model="userDetail.address" />
			</view>
			<!-- <view class="form-item">
				<view class="label">性别</view>
				<input class="input" placeholder="请输入性别"/>
			</view> -->
			<view class="form-item">
				<view class="label">手机号码</view>
				<input class="input" placeholder="请输入手机号码" v-model="userDetail.phone"/>
			</view>
			<view class="form-item">
				<view class="label">民族</view>
				<input class="input" placeholder="请输入民族" v-model="userDetail.nation"/>
			</view>
			<view class="form-item">
				<view class="label">政治面貌</view>
				<input class="input" placeholder="请选择政治面貌" v-model="userDetail.political" disabled @click="selectPoliticalStatus"/>
			</view>
			<view class="form-item">
				<view class="label">户口性质</view>
				<input class="input" placeholder="请选择户口性质" v-model="userDetail.house_type" disabled @click="selectHouseholdType"/>
			</view>
		</view>

		<!-- 教育信息 -->
		<view class="tab-content" v-show="currentTab === 1">
			<view class="form-item">
				<view class="label">毕业学校</view>
				<input class="input" placeholder="毕业学校" v-model="userDetail.school"/>
			</view>
			<view class="form-item">
				<view class="label">学历</view>
				<view class="input" @click="selectEducation" style="position: relative;">
					{{userDetail.education}}
					<i class="gjjd-iconfont icon-xiangxia" style="position: absolute;top:2rpx;right: 5rpx;"></i>
				</view>
			</view>
			<view class="form-item">
				<view class="label">专业</view>
				<input class="input" placeholder="请输入学历" v-model="userDetail.major" />
			</view>
			<view class="form-item">
				<view class="label" >是否有毕业证</view>
				<switch :checked="userDetail.school_paper_has>0" @change="onSchoolPaperHas" style="transform:scale(0.8)"/>
			</view>
			<view class="form-item">
				<view class="label">毕业证编号</view>
				<input class="input" placeholder="请输入毕业证编号" v-model="userDetail.school_no" />
			</view>
			<view class="form-item">
				<view class="label">入学时间</view>
				<picker mode="date" fields="day" :value="userDetail.in_school_date" :start="startDate" :end="endDate" @change="bindDateChange($event,'in_school_date')" placeholder="请选择">
					<view class="picker">{{userDetail.in_school_date}}</view>
				</picker>
			</view>
			<view class="form-item">
				<view class="label">毕业时间</view>
				<picker mode="date" fields="day" :value="userDetail.graduate_date" :start="startDate" :end="endDate" @change="bindDateChange($event,'graduate_date')" placeholder="请选择">
					<view class="picker">{{userDetail.graduate_date}}</view>
				</picker>
			</view>
			<view class="form-item">
				<view class="label">毕业证照片</view>
				<view v-if="userDetail.school_paper">
					<image class="pic-school" :src="userDetail.school_paper" mode="aspectFit"></image>
				</view>
				<view v-else class="text-R" @click="uploadImage('school_paper')">
					<text>请上传</text>
					<i class="gjjd-iconfont icon-xiangyou"></i>
				</view>
			</view>
		</view>
		<!-- 工作信息 -->
		<view class="tab-content" v-show="currentTab === 2">
			<view class="form-item">
				<view class="label">参加工作时间</view>
				<picker mode="date" fields="day" placeholder="请选择">
					<view class="picker">{{userDetail.work_time}}</view>
				</picker>
			</view>
			<view class="form-item">
				<view class="">职称</view>
				<input class="input" placeholder="请选择" v-model="userDetail.title_type" @click="selectTitleType"/>
			</view>
		</view>

		<view class="save-but">
			<view class="color-ff" @click="onSave">保存</view>
		</view>
	</view>
</template>

<script>
	import {request,uploads,rectTo,navTo,showMsg} from "@/utils";
	export default {
		data() {
			return {
				showpermissionTip: false,
				currentTab: 0 ,// 默认从基本信息开始
				//用户信息
				userDetail:{
					phone:'',
					realname:'',
					idcard:'',
					idcard_front:'',
					idcard_front_show:'',
					idcard_back:'',
					idcard_back_show:'',
					idcard_header:'',
					nation:'',
					house_type:'',
					political:'',
					
					school:'',
					education:'',
					major:'',
					school_paper_has:0,
					school_no:'',
					in_school_date:'',
					graduate_date:'',
					school_paper:'',
					
					work_time:'',
					title_type:'无'
				}
			};
		},
		onLoad() {
			this.$util.checkLogin();
			this.getData();
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
			// 切换选项卡
			switchTab(tabIndex) {
				this.currentTab = tabIndex;
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
			async uploadIdCardFront() {
				let res = await uploads({limit:1},this);
				console.log('uploadIdCardFront',res);
				if(res[0]){
					uni.showLoading({title: '身份证识别中'});
					let rs = await request({
						method:'POST',
						data:{pic:res[0],type:'front'},
						url:"/appuser/idcardorc"
					});
					uni.hideLoading();
					console.log('idcard orc',rs);
					if(rs.code){
						showMsg('this',{title:'识别失败'});
					}
					let card = rs.data.idcard;
					this.userDetail.realname = card.realname;
					this.userDetail.idcard = card.id_card_number;
					this.userDetail.nation = card.nation;
					this.userDetail.address = card.address;
					this.userDetail.idcard_front = card.id_card_front;
					this.userDetail.idcard_front_show = "https://cdn.app.gjjd168.com/idCard/headerOrigin/"+card.id_card_number+"_front.jpg";
				}else{
					showMsg('this',{title:'上传失败'});
				}
			},
			async uploadIdCardBack() {
				if(!this.formData.idcard){
					showMsg('this',{title:'请先上传正面'});
					return;
				}
				let res = await uploads({limit:1},this);
				console.log('uploadIdCardBack',res);
				if(res[0]){
					uni.showLoading({title: '身份证识别中'});
					let rs = await request({
						method:'POST',
						data:{pic:res[0],type:'back',idcard:this.userDetail.idcard},
						url:"/appuser/idcardorc"
					});
					uni.hideLoading();
					console.log('idcard orc',rs);
					if(rs.code){
						showMsg('this',{title:'识别失败'});
					}
					this.userDetail.idcard_back = rs.data.id_card_back;
					this.userDetail.idcard_back_show = "https://cdn.app.gjjd168.com/idCard/headerOrigin/"+this.userDetail.idcard+"_back.jpg"
				}else{
					showMsg('this',{title:'上传失败'});
				}
			},
			async uploadImage(field) {
				let res = await uploads({limit:1},this);
				if(res[0]){
					this.userDetail[field] = res[0];
				}else{
					showMsg('this',{title:'上传失败'});
				}
			},
			selectPoliticalStatus() {
				const options = ['群众', '中共党员', '中共预备党员', '共青团员', '民主党派'];
				uni.showActionSheet({
					itemList: options,
					success: (res) => {
						this.userDetail.political = options[res.tapIndex];
					}
				});
			},
			selectHouseholdType() {
				const options = ['农业户口', '非农业户口', '居民户口'];
				uni.showActionSheet({
					itemList: options,
					success: (res) => {
						this.userDetail.house_type = options[res.tapIndex];
					}
				});
			},
			selectEducation() {
				const options = ['小学', '初中', '普通高中', '技工学校', '职业高中', '中等专科', '大学专科', '大学本科','硕士研究生','博士研究生','其他'];
				uni.showActionSheet({
					itemList: options,
					success: (res) => {
						this.userDetail.education = options[res.tapIndex];
					}
				});
			},
			onSchoolPaperHas(e){
				this.userDetail.school_paper_has = e.detail.value?1:0;
			},
			bindDateChange(e,field){
				//console.log(e,field);
				let date = e.detail.value;
				this.userDetail[field] = date;
			},
			selectTitleType() {
				const options = ['无', '助理工程师', '工程师', '高级工程师', '其他'];
				uni.showActionSheet({
					itemList: options,
					success: (res) => {
						this.userDetail.title_type = options[res.tapIndex];
					}
				});
			},
			async getData(){
				let res = await request({
					method:'POST',
					data:{},
					url:"/appuser/detail"
				});
				console.log("user detail",res);
				if(res.code){
					showMsg('this',{title:res.msg});
					return;
				}
				this.userDetail = res.data.userDetail;
			},
			//保存信息
			async onSave(){
				let res = await request({
					method:'POST',
					data:{form:this.userDetail},
					url:"/appuser/detailsave"
				});
				console.log("user detail",res);
				showMsg('this',{title:res.msg});
			},
		}
	};
</script>

<style scoped>
.container {
  padding: 20rpx;
}
.tab-bar{
	position: sticky;
	top: 0;
	z-index: 2;
	background-color: #fff;
}
.tab-content{
	padding-bottom: 50px;
}
.save-but{
	position: fixed;
	bottom: 0;
	right: 0;
	left: 0%;
	padding: 10px;
	padding-bottom: 10px;
	
}

.save-but view{
	margin: 0 20rpx;
	height: 40px;
	line-height: 40px;
	text-align: center;
	border-radius: 20px;
	background-color: #007AFF;
}

.tab-bar {
  display: flex;
  border-bottom: 1rpx solid #eee;
  margin-bottom: 30rpx;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 20rpx 0;
  font-size: 32rpx;
  color: #666;
}

.tab-item.active {
  color: #007AFF;
  border-bottom: 4rpx solid #007AFF;
}

.form-item {
  margin-bottom: 30rpx;
}

.label {
  font-size: 28rpx;
  color: #333;
  margin-bottom: 10rpx;
}

.input, .picker {
  width: 100%;
  height: 40px;
  line-height: 40px;
  border: 1rpx solid #ddd;
  border-radius: 8rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}
.pic-header{
	width: 120rpx;
	height: 168rpx;
}
.pic-idcard{
	width: 45%;
	padding: 0 10rpx;
	border-radius: 6rpx;
	border: 1rpx solid #626262;
}
.pic-idcar-upload{
	flex: 1;
	height: 153rpx;
	display: flex;
	align-items: center;
	justify-content: center;
}
.pic-front,.pic-back{
	width: 243rpx;
	height: 153rpx;
}
.pic-school{
	flex:1;
	height: 200px;
}
</style>
