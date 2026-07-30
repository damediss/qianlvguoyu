import util from "@/utils/util.js";
import { ensureToken, handleTokenExpired, isKickedOut, isLoginExpired, updateTokenInUrl, clearLocalCredentials } from "./auth.js";
const g = util;
export const request = async (options)=>{
	// ★ 重试场景：handleTokenExpired 传入完整 _url（已含新 token），直接使用
	if (options._url) {
		let url = options._url;
		let postdata = options.data;
		let paramsdata = postdata;
		if(options.method=='POST'){
			paramsdata = JSON.stringify(postdata);
		}
		console.log('retry:',url,paramsdata);
		return new Promise((resolve) => {
			uni.request({
				header: { "Content-Type": "application/json" },
				method: options.method,
				data: paramsdata,
				url: url,
				success: (res) => { resolve(res.data); },
				fail: (err) => {
					console.log('err',err);
					uni.showToast({ title: '网络请求失败，请检查网络', icon: "none", duration: 2000 });
					resolve(false);
				}
			});
		});
	}

	let url = g.domain[g.runMode]+options.url;
	url += '?ver='+g.ver;

	// ★ 修正缺陷 #1：登录请求自身不打 token（skipAuth 豁免，避免 ensureToken 递归）
	if (!options.skipAuth) {
		let itoken = uni.getStorageSync('itoken');
		if(itoken) {
			url = url + '&istate=' + itoken;
		} else {
			// 控制点 ①：发请求前确保 istate 非空，无 token 先尝试静默获取
			itoken = await ensureToken(request);
			if (itoken) {
				url = url + '&istate=' + itoken;
			} else {
				return Promise.reject('NO_TOKEN');
			}
		}
	}

	// 处理 URL 查询参数 (params)
	if(options.params && Object.keys(options.params).length > 0) {
		for(let key in options.params) {
			if(options.params[key] !== undefined && options.params[key] !== null) {
				url += '&' + key + '=' + options.params[key];
			}
		}
	}

	let postdata = options.data;
	let paramsdata = postdata;
	if(options.method=='POST'){
		paramsdata = JSON.stringify(postdata);
	}
	console.log(url,paramsdata);
	return new Promise((resolve) => {
		uni.request({
			header: {
				"Content-Type": "application/json"
			},
			method: options.method,
			data: paramsdata,
			url: url,
			success: async (res) => {
				// ★ 修正缺陷 #3：区分"被踢下线"与"自然过期"
				if (!options.skipAuth && isLoginExpired(res.data) && isKickedOut(res.data.msg)) {
					clearLocalCredentials();
					showMsg('/pages/auth/login',{title: res.data.msg || '您的账号已在其他设备登录',timeout:1500});
					resolve(false);
					return;
				}
				// 响应拦截：静默重登
				if (!options.skipAuth && isLoginExpired(res.data)) {
					const retried = await handleTokenExpired(options, url, request);
					if (retried) {
						resolve(retried);
					} else {
						resolve(false);
					}
					return;
				}
				resolve(res.data);
			},
			fail: (err)=>{
				console.log('err',err);
				uni.showToast({
					title: '网络请求失败，请检查网络',
					icon: "none",
					duration: 2000
				});
				resolve(false);
			}
		});
	});
}

export const uploads = (obj,self,useDownloadUrl = false) => {
	let limit = 1;
	if(obj.limit) limit = obj.limit;
	return new Promise(resolveMain => {
		uni.showActionSheet({
			itemList: ['拍照', '从手机相册选择'],
			success: async (res) => {
				let type = "album";
				if(res.tapIndex==0){
					type = 'camera';
				}
				if(!g.isAndroid){//判断IOS
					let rs = await chooseFile(limit,type,useDownloadUrl);
					if(obj.success) obj.success(rs);
					Promise.all(rs).then(result => {
						uni.hideLoading();
					    resolveMain(result);
					}).catch(() => {
						uni.hideLoading();
					});
				}else{
					var Build = plus.android.importClass("android.os.Build");
					console.log('SDK_INT',Build.VERSION.SDK_INT); // 输出当前SDK版本号
					var permisId = 'android.permission.READ_MEDIA_VISUAL_USER_SELECTED';
					if(Build.VERSION.SDK_INT < 33){
						permisId = 'android.permission.READ_EXTERNAL_STORAGE';
					}else if(Build.VERSION.SDK_INT == 33){
						permisId = 'android.permission.WRITE_EXTERNAL_STORAGE';
					}
					if(type == 'camera'){
						permisId = 'CAMERA';
					}
					let pp = plus.navigator.checkPermission(permisId);
					if(pp == 'authorized'){
						//已获得权限
						self.showpermissionTip = false;
						let rs = await chooseFile(limit,type,useDownloadUrl);
						if(obj.success) obj.success(rs);
						Promise.all(rs).then(result => {
						    uni.hideLoading();
						    resolveMain(result);
						}).catch(() => {
							uni.hideLoading();
						});
					}else{
						self.showpermissionTip = true;//提示请求权限的理由
						if(permisId == 'CAMERA'){
							permisId = 'android.permission.'+permisId;
						}
						plus.android.requestPermissions([permisId], async (e)=>{
							self.showpermissionTip = false;
							if(e.deniedAlways.length == 0 && e.deniedPresent.length == 0){
								//获得权限
								let rs = await chooseFile(limit,type,useDownloadUrl);
								if(obj.success) obj.success(rs);
								Promise.all(rs).then(result => {
									uni.hideLoading();
								    resolveMain(result);
								}).catch(() => {
									uni.hideLoading();
								});
							}else{
								uni.showToast({
									title: '您还未授权',
									icon: "none",
									duration: 2000
								});
								if(permisId == 'android.permission.READ_MEDIA_VISUAL_USER_SELECTED'){
									self.showpermissionTip = true;
									let rs = await chooseFile(limit,type,useDownloadUrl);
									if(obj.success) obj.success(rs);
									Promise.all(rs).then(result => {
										uni.hideLoading();
									    resolveMain(result);
									}).catch(() => {
										uni.hideLoading();
									});
								}
							}
						},(e)=>{});
					}
				}
			}
		});
	});
}

export const chooseFile = (limit=1,type='album',useDownloadUrl=false) => {
	return new Promise(resolveMain => {
	    uni.chooseImage({
	        count: limit,
	        sourceType:[type],
	        sizeType: ['original', 'compressed'],
	        success: ({tempFiles}) => {
	            const resList = []
	            tempFiles.forEach(file => {
	                //console.log(tempFiles, file);
	                const res = uploadFile(file.path,useDownloadUrl);
					if(res){
						resList.push(res);
					}
	            });
	            uni.showLoading({title: '上传中'});
	            Promise.all(resList).then(result => {
	                uni.hideLoading();
	                resolveMain(result);
	            }).catch(() => {
	                uni.hideLoading();
	            });
	        }
	    });
	});
}

export const uploadFile = (fileUrl,useDownloadUrl=false) => {
	return new Promise(resolve => {
		let itoken = uni.getStorageSync('itoken');
		uni.uploadFile({
			url: g.uploadURL[g.runMode]+"/upload",
			name: 'file',
			filePath: fileUrl,
			header: {
				Authorization: itoken,
				ContentType : 'multipart/form-data'
			},
			success: uploadRes => {
				console.log(uploadRes);
				if(uploadRes.statusCode !=200){
					uni.showToast({
						title: '上传失败',
						icon: "none",
						duration: 2000
					});
					resolve("");
				}else{
					const data = JSON.parse(uploadRes.data);
					if(useDownloadUrl){
						resolve(data.data.downloadUrl);
					}else{
						resolve(data.data.url);
					}
				}
			},
			fail: err => {
				uni.hideLoading();
				console.log(err);
				uni.showToast({
					title: '上传失败，请检查网络',
					icon: "none",
					duration: 2000
				});
				resolve("");
			}
		})
	});
}

export const makeCall = (phone='',self) => {
	if(!phone){
		uni.showToast({
			title: '号码为空',
			icon: 'none',
			duration: 2000
		});
		return;
	}
	if(!g.isAndroid){//IOS
		uni.makePhoneCall({
			phoneNumber:phone
		});
	}else{//android
		let pp = plus.navigator.checkPermission('CALL_PHONE');
		if(pp == 'authorized'){
			//已获得权限
			console.log('CALL_PHONE='+pp);
			self.showpermissionTip = false;
			uni.makePhoneCall({
				phoneNumber:phone
			});
		}else{
			self.showpermissionTip = true;//提示请求权限的理由
			plus.android.requestPermissions(['android.permission.CALL_PHONE'],(e)=>{
				self.showpermissionTip = false;
				if(e.deniedAlways.length == 0 && e.deniedPresent.length == 0){
					//获得权限
					uni.makePhoneCall({
						phoneNumber:phone
					});
				}else{
					uni.showToast({
						title: '您还未授权',
						icon: "none",
						duration: 2000
					});
				}
			},(e)=>{});
		}
	}
}

export const cameraPermission = (res,self) => {
	let isAndroid = uni.getSystemInfoSync().osName === 'android';
	if(isAndroid){
		let permisId = 'CAMERA';
		var pp = plus.navigator.checkPermission(permisId);
		if(pp == 'authorized'){
			//已获得权限
			self.showpermissionTip = false;
			res(1);
		}else{
			self.showpermissionTip = true;//提示请求权限的理由
			if(permisId == 'CAMERA'){
				permisId = 'android.permission.'+permisId;
			}
			plus.android.requestPermissions([permisId], (e)=>{
				self.showpermissionTip = false;
				if(e.deniedAlways.length == 0 && e.deniedPresent.length == 0){
					//获得权限
					res(1);
				}else{
					res(0);
					uni.showToast({
						title: '您还未授权',
						icon: "none",
						duration: 2000
					});
				}
			},(e)=>{});
		}
	}else{
		res(1);
	}
}

export const rectTo = (router,options={}) => {
	// 优化：支持两种参数传递方式
	let pathurl;
	
	// 如果 options 包含 id 等简单参数，使用标准 URL 方式
	if (Object.keys(options).length === 1 && options.id !== undefined) {
		pathurl = router + '?id=' + options.id;
	} else if (Object.keys(options).length > 0) {
		// 复杂参数使用 item 方式
		pathurl = router + '?item=' + encodeURIComponent(JSON.stringify(options));
	} else {
		pathurl = router;
	}
	
	// 判断是否是 tabBar 页面
	if(pathurl.indexOf('pages/index/index') != -1 || 
	   pathurl.indexOf('pages/team/index') != -1 || 
	   pathurl.indexOf('pages/promote/index') != -1 || 
	   pathurl.indexOf('pages/shop/index') != -1 || 
	   pathurl.indexOf('pages/user/index') != -1){
		uni.switchTab({
			url: router, // tabBar 页面不能带参数
		});
	} else {
		uni.redirectTo({
			url: pathurl
		});
	}
}

export const navTo = (router,options={}) => {
	// 优化：支持两种参数传递方式
	// 1. 通过 item 参数（兼容旧代码）
	// 2. 通过标准 URL 参数（推荐）
	let pathurl;
	
	// 如果 options 包含 id 等简单参数，使用标准 URL 方式
	if (Object.keys(options).length === 1 && options.id !== undefined) {
		pathurl = router + '?id=' + options.id;
	} else if (Object.keys(options).length > 0) {
		// 复杂参数使用 item 方式
		pathurl = router + '?item=' + encodeURIComponent(JSON.stringify(options));
	} else {
		pathurl = router;
	}
	
	// 判断是否是 tabBar 页面
	if(pathurl.indexOf('pages/index/index') != -1 || 
	   pathurl.indexOf('pages/team/index') != -1 || 
	   pathurl.indexOf('pages/promote/index') != -1 || 
	   pathurl.indexOf('pages/shop/index') != -1 || 
	   pathurl.indexOf('pages/user/index') != -1){
		uni.switchTab({
			url: router, // tabBar 页面不能带参数
		});
	} else {
		uni.navigateTo({
			url: pathurl
		});
	}
}

export const showMsg = (goto,options={}) => {
	let time = 100;
	if(options.timeout){
		time = options.timeout;
	}
	uni.showToast({
		title: options.title,
		icon: "none",
		duration: 2000,
		success: () => {
			if(goto != 'this'){
				if(goto==='back'){
					setTimeout(() => {uni.navigateBack()}, 2000);
				}else{
					if(goto.indexOf('pages/index/index') != -1 || goto.indexOf('pages/team/index') != -1 || goto.indexOf('pages/promote/index') != -1 || goto.indexOf('pages/shop/index') != -1 || goto.indexOf('pages/user/index') != -1){
						setTimeout(() => {uni.switchTab({url:goto})}, time);
					}else{
						setTimeout(() => {uni.redirectTo({url:goto})}, time);
					}
				}
			}
		}
	});
}

export const checkUpgrade = (options={},self=false,fun) => {
	// #ifdef APP-PLUS
	plus.runtime.getProperty(plus.runtime.appid, async (widgetInfo) => {
		const appVersionCode = parseInt(widgetInfo.versionCode)
		const {osName} = uni.getSystemInfoSync();
		let userId = options.userId || 0;
		let url = g.domain[g.runMode]+"/apphome/checkversion";
		let params = {
			os: osName.toUpperCase(),
			versionNum:appVersionCode,
			userId:userId
		};
		uni.request({
			header: {
				"Content-Type": "application/json"
			},
			method: 'POST',
			data: params,
			url: url,
			success: (callback) => {
				console.log('success',callback.data);
				let res = callback.data;
				if(fun) fun(res);
				if(res.code){
					console.log('更新下载');
					if(res?.data?.info){
						let info = res.data.info;
						if(info.type == 'ipa'){
							plus.runtime.launchApplication({
							    action: info.url
							}, function(e) {
							    //console.log('Open system default browser failed: ' + e.message);
							});
						}else{
							//下载
							let msg = "有新版本更新,请立即更新";
							if(info.desc){
								msg = info.desc;
							}
							uni.showModal({
								title: '提示',
								content: msg,
								showCancel: false,
								success: ({confirm}) => {
									let downloadTask = uni.downloadFile({
										url: info.url,
										success: downloadRes => {
											self.upgrade_progress = '';
											uni.removeInterceptor('switchTab');
											if (downloadRes.statusCode === 200) {
												plus.runtime.install(downloadRes.tempFilePath, {
													force: true
												}, function () {
													console.log('安装成功');
													uni.showToast({
														title: '更新成功,请重新打开APP',
														icon: 'none',
														mask: false,
														duration: 1500
													});
													let tid = setTimeout(()=>{
														clearTimeout(tid);
														plus.runtime.restart();
													},1500);
												}, function (error) {
													console.log('install fail',error);
													uni.showToast({
														title: JSON.stringify(error),
														icon: 'none',
														mask: false,
														duration: 1500
													});
												});
											}
										},
										fail() {
											self.upgrade_progress = '';
											uni.removeInterceptor('switchTab');
											uni.showToast({
												title:'下载失败'
											});
										}
									});
									downloadTask.onProgressUpdate((rs) => {
										console.log('下载进度:' + rs.progress+"%");
										console.log('已经下载的数据长度' + rs.totalBytesWritten);
										console.log('预期需要下载的数据总长度' + rs.totalBytesExpectedToWrite);
										self.upgrade_progress = "下载中...("+rs.progress+"%)";
										// 满足测试条件，取消下载任务。
										if (rs.progress > 50) {
											//downloadTask.abort();
										}
									});
								}
							});
						}
					}
				}
			},
			fail: (err)=>{
				console.log('err',err);
				uni.showToast({
					title: '网络请求失败，请检查网络',
					icon: "none",
					duration: 2000
				});
			}
		});
	});
	// #endif
}