<template>
	<view class="content">
		<view class="logo">
			<image src="/static/images/logo.jpg"></image>
		</view>
		<view class="form">
			<view class="form-row mar-B20">
				<view class="form-label">手机号</view>
				<view class="form-input">
					<input v-model="phone" placeholder="请输入手机号"/>
				</view>
			</view>
			<view class="form-row" v-if="loginType=='pwd'">
				<view class="form-label">密码</view>
				<view class="form-input">
					<input type="password" v-model="password" placeholder="请输入密码"/>
				</view>
			</view>
			<template v-if="loginType=='sms'">
				<view class="form-row mar-B20">
					<view class="form-label">图形验证码</view>
					<view class="form-input">
						<input v-model="code" placeholder="请输入图形验证码"/>
						<view class="get-captcha">
							<image mode="widthFix" @click="getCode" :src="imgCode"></image>
						</view>
					</view>
				</view>
				<view class="form-row">
					<view class="form-label">短信验证码</view>
					<view class="form-input">
						<input v-model="smscode" placeholder="请输入短信验证码"/>
						<text class="get-sms" @click="getSMS">{{smsText}}</text>
					</view>
				</view>
			</template>
		</view>
		<view class="login-btn" @click="onSubmit()">登录</view>
		<view class="login-type bg-f9d52b color-62" @click="onLoginType()">
			<text v-if="loginType=='pwd'">短信登录</text>
			<text v-if="loginType=='sms'">密码登录</text>
		</view>
		<view class="competition-scan" @click="goCompetitionLogin">
			<text>进入竞赛端</text>
		</view>
	</view>
</template>

<script>
import {request,uploads,rectTo,navTo,showMsg} from '../../utils';
import { getDeviceId, getDeviceInfo } from '@/utils/device';
import { useUserStore } from '@/store/user';
import { setCachedPassword } from '@/utils/auth.js';

export default {
	setup() {
		const userStore = useUserStore()
		return {
			userStore
		}
	},
	data() {
		return {
			frompage:'',
			phone:'',
			password:'',
			code:'',
			codeText:'5347',
			smscode:'',
			loginType:'pwd',
			smsText:'获取验证码',
			isgetsms:0,
			imgCode:this.$util.domain[this.$util.runMode]+'/captcha/index',
			isSubmit:0
		}
	},
	onLoad(options) {
		console.log(options)
		if(options.item){
			let item = JSON.parse(decodeURIComponent(options.item));
			console.log(item);
			if(item.from) this.frompage = item.from;
		}
	},
	methods: {
		getCode(){
			this.imgCode = '';
			let tid = setTimeout(()=>{
				let t = new Date().getTime();
				console.log('t='+t);
				this.imgCode = this.$util.domain[this.$util.runMode]+'/captcha/index?t='+t;
			},200);
		},
		onLoginType(){
			if(this.loginType == 'sms'){
				this.loginType = 'pwd';
			}else{
				this.loginType = 'sms';
			}
		},
		async getSMS(){
			let that = this;
			if(!this.phone){
				showMsg('this',{title:'请输入手机号'});
				return;
			}
			if(!this.$util.validatePhone(this.phone)){
				showMsg('this',{title:'手机号格式不正确'});
				return;
			}
			if(!this.code){
				showMsg('this',{title:'请输入图形验证码'});
				return;
			}
			if(this.isgetsms){
				showMsg('this',{title:'请勿频繁获取'});
				return;
			}
			this.isgetsms = 1;
			let res = await request({
				method:'POST',
				data:{code:this.code},
				url:"/captcha/check"
			});
			console.log('check code',res);
			if(res.code){
				this.getCode();
				this.isgetsms = 0;
				showMsg('this',{title:'图形验证码不正确'});
				return;
			}
			//获取验证码
			let rs = await request({
				method:'POST',
				data:{phone:this.phone},
				url:"/appsms/send"
			});
			if(rs.code){
				this.getCode();
				this.isgetsms = 0;
				showMsg('this',{title:'短信验证码获失败'});
				return;
			}
			let times = 60;
			this.smsText = times+'秒';
			let timeid = setInterval(()=>{
				if(times==0){
					clearInterval(timeid);
					that.isgetsms = 0;
					that.smsText = '获取验证码';
					return;
				}else{
					times--;
					that.smsText = times+'秒';
				}
			},1000);
		},
		async onSubmit(){
			if(!this.phone){
				showMsg('this',{title:'请输入手机号'});
				return;
			}
			if(this.loginType == 'pwd' && !this.password){
				showMsg('this',{title:'请输入密码'});
				return;
			}
			if(this.loginType == 'sms' && !this.smscode){
				showMsg('this',{title:'请输入短信验证码'});
				return;
			}
			if(this.isSubmit){
				return;
			}
			this.isSubmit = 1;
			uni.showLoading({
				title:'正在登录'
			});
			let res = await request({
				method:'POST',
				data:{
					phone:this.phone,
					password:this.password,
					smscode:this.smscode,
					loginType:this.loginType,
					deviceId:getDeviceId(),
					deviceInfo:getDeviceInfo()
				},
				url:"/appauth/login",
				// ★ 修正缺陷 #1：登录请求自身不打 token（避免 ensureToken 递归）
				skipAuth: true
			});
			uni.hideLoading();
			if(res.code === 2){
				this.isSubmit = 0;
				const that = this;
				uni.showModal({
					title:'设备未授权',
					content:res.msg || '当前设备未授权，请联系管理员审批解绑',
					confirmText:'申请解绑',
					cancelText:'取消',
					success: async (r) => {
						if(!r.confirm){
							return;
						}
						const rs = await request({
							method:'POST',
							url:'/appauth/deviceunbindapply',
							data:{
								phone:that.phone,
								new_device_id:getDeviceId(),
								new_device_info:getDeviceInfo()
							}
						});
						showMsg('this',{
							title: rs && rs.msg ? rs.msg : (rs && rs.code ? '申请失败' : '申请已提交，请等待管理员审批')
						});
					}
				});
				return;
			}
			// 检查登录是否失败
			if(res.code){
				this.isSubmit = 0;
				let msg = '登陆失败';
				if(res.msg) msg = res.msg;
				showMsg('this',{title:msg});
				return;  // 登录失败，直接返回
			}
			console.log('登陆成功',res);
			// ★ 控制点 ③：校验 token 非空后再写入，避免"看似登录成功实际没有 token"
			if (!res.data || !res.data.token || typeof res.data.token !== 'string' || res.data.token.length < 10) {
				this.isSubmit = 0;
				showMsg('this',{title:'登录失败：未获取到有效令牌，请重试'});
				return;
			}
			// 使用 Pinia store 保存用户信息
			const payload = res.data || {};
			payload.user = payload.user || {};
			payload.user.phone = this.phone;
			const loginOk = this.userStore.login(payload);
			if (!loginOk) {
				this.isSubmit = 0;
				showMsg('this',{title:'登录状态保存失败，请重试'});
				return;
			}
			// ★ 修正缺陷 #8：密码只存内存（App 重启即丢），不持久化
			if (this.password && setCachedPassword) {
				setCachedPassword(this.password);
			}
			uni.setStorageSync('iusername', this.phone);
			//绑定websocket clientid
			request({
				method:'POST',
				data:{clientid:this.userStore.clientId},
				url:"/appauth/bind",
				// ★ 修正缺陷 #1：bind 请求不需要用户态 token（URL 里会自动带 itoken）
				skipAuth: true
			}).then((rs)=>{
				console.log('绑定结果'+uni.getStorageSync('clientId'),rs);
			});
			if(this.frompage){
				rectTo(this.frompage);
			}else{
				rectTo('/pages/user/index');
			}
		},
		goCompetitionLogin(){
			uni.navigateTo({ url: '/pages/competition-app/login' });
		}
	}
}
</script>

<style>
	.logo{
		height: 180px;
		display: flex;
		align-items: center;
		justify-content: center;
	}
	.logo image{
		width: 150px;
		height: 150px;
	}
	.form{
		width: 590rpx;
		margin: 20px 80rpx;
	}
	.form-row{
		display: flex;
		align-items: center;
	}
	.form-label{
		width: 180rpx;
	}
	.form-input{
		flex: 1;
		position: relative;
	}
	.form-input input{
		width: 100%;
		padding: 5px 10rpx;
		border-bottom: 1rpx solid #929292;
	}
	.get-captcha{
		position: absolute;
		top: 0;
		right: 0;
		width: 140rpx;
		height: 30px;
		border:1rpx solid #929292;
		display: flex;
		align-items: center;
	}
	.get-captcha image{
		width: 140rpx;
	}
	.get-sms{
		position: absolute;
		top: 0;
		right: 0;
		padding: 4px 8rpx;
		border:1rpx solid #929292;
		border-radius: 6rpx;
		font-size: 12px;
	}
	.login-btn{
		margin-top: 30px;
		margin-left: 80rpx;
		margin-right: 80rpx;
		width: 590rpx;
		height: 40px;
		line-height: 40px;
		background-color: #73a9fa;
		border-radius: 20px;
		color: #fff;
		text-align: center;
	}
	.login-type{
		margin-top: 30px;
		margin-left: 80rpx;
		margin-right: 80rpx;
		width: 590rpx;
		height: 40px;
		line-height: 40px;
		border-radius: 20px;
		text-align: center;
	}
	.competition-scan{
		margin: 18px 80rpx 0;
		width: 590rpx;
		height: 40px;
		line-height: 40px;
		box-sizing: border-box;
		border: 1rpx solid #73a9fa;
		border-radius: 20px;
		color: #3f7fd3;
		text-align: center;
	}
</style>
