<script>
	import {request,uploads,rectTo,navTo,showMsg,cameraPermission} from "@/utils";
	import { useUserStore } from '@/store/user';
	import { ensureToken, rebindWebSocket } from '@/utils/auth.js';
	export default {
		setup() {
			const userStore = useUserStore();
			return {
				userStore
			}
		},
		data(){
			return {
				timeout: 15000,
				timeoutObj:0
			}
		},
		onLaunch: function(options) {
			console.log('App Launch')
			const launchPath = options && options.path ? String(options.path) : ''
			const isCompetitionPlayerRoute = launchPath.indexOf('pages/competition-app/') === 0
			if (isCompetitionPlayerRoute) {
				console.log('竞赛选手端使用独立登录态，跳过旧平台登录预检和 WebSocket')
				return
			}
			// ★ 启动时预检登录态：有 token 直接用，无 token 尝试静默刷新，都失败则清缓存
			ensureToken(request).then(token => {
				console.log('启动预检 token:', token ? '有效' : '无效');
			});
			this.checkOpenSocket();
		},
		onShow: function() {
			console.log('App Show')
		},
		onHide: function() {
			console.log('App Hide')
		},
		methods:{
			checkOpenSocket(){
				/*if(!this.$util.isLogin()){
					return;
				}*/
				let self = this;
				uni.sendSocketMessage({
					data: 'ping',
					success: (res) => {
						return;
					},
					fail: (err) => { // 未连接打开websocket连接
						console.log('no ok');
						self.openConnection(); 
					}
				});
			},
			openConnection(){//打开连接
				uni.closeSocket();//确保已经关闭后再重新打开
				console.log('wssUrl='+this.$util.wssUrl());
				uni.connectSocket({
					url: this.$util.wssUrl(),
					method: 'POST',
					success: (res) => {
						console.log('连接成功 connectSocket=', res);
					},
					fail: (err) => {
						console.log('连接失败 connectSocket=', err);
					}
				});
				uni.onSocketOpen((res) => {
					console.log('WebSocket连接已打开');
				});
				uni.onSocketError(function (res) {
				  console.log("WebSocket连接打开失败,请检查");
				});
				this.onSocketMessage();//打开成功监听服务器返回的消息
			},
			onSocketMessage(){//消息
				console.log('onSocketMessage');
				uni.onSocketMessage(async (res) => {
					//console.log("websocket message",res);
					let obj = JSON.parse(res.data);
					console.log("websocket message obj",obj);
					if(obj.type=='init'){
						this.userStore.setClientId(obj.client_id);
						this.reset();
						// ★ 控制点：bind 不需要用户态 token，但 URL 里会带 itoken 如果有的话
						let rs = await request({
							method:'POST',
							data:{clientid:obj.client_id},
							url:"/appauth/bind",
							skipAuth: true
						});
						console.log('绑定结果',rs);
					}else if(obj.type == 'logout'){
						this.userStore.logout();
						showMsg('/pages/index/index',{title:'您的账号已在其他设备登录',timeout:1500});
					}
				});
			},
			reset(){
				clearInterval(this.timeoutObj);
				this.start();//启动心跳
			},
			start(){//启动心跳 start
				let self = this;
				this.timeoutObj = setInterval(function(){
					uni.sendSocketMessage({
						data: 'ping',
						success: (res) => {
							console.log('心跳持续中....');
						},
						fail: (err) => {
							console.log('连接失败重新连接....');
							self.openConnection();
						}
					});
				}, this.timeout);
			}
		}
	}
</script>

<style>
	/* 引入图标字体 */
	@import "@/static/iconfont/iconfont.css";
	
	/* 引入全局样式 - 按功能分类 */
	@import "@/styles/colors.css";      /* 颜色类 */
	@import "@/styles/layout.css";      /* 布局类 */
	@import "@/styles/typography.css";  /* 文字类 */
	@import "@/styles/utilities.css";   /* 工具类 */
</style>
