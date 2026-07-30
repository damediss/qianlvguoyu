var g = {
	ver:'3.3.11',
	// dev=本地 PHP 后台（start-api-server.bat，8080）；pro=线上
	runMode:'pro',

	domain:{
		dev:'http://192.168.1.200:8080',
		pro:'https://gjjdp.gjjd168.com'
	},
	uploadURL:{
		dev:'http://192.168.1.200:8080',
		pro:'https://gjjdp.gjjd168.com'
	},
	wss:{
		dev:'wss://cdn.app.gjjd168.com/websocket',
		pro:'wss://cdn.app.gjjd168.com/websocket'
	},

	// 新域名 - 竞赛接口
	java:{
		dev:'http://192.168.1.110:8081',
		pro:'https://java.app.gjjd168.com'
	},
	javaURL:{
		dev:'http://192.168.1.110:8081',
		pro:'https://java.app.gjjd168.com'
	},

	isAndroid: uni.getSystemInfoSync().osName === 'android'
}

g.wssUrl = ()=>{
	return g.wss[g.runMode];
}

//获取一定范围内n个随机整数
g.randIntNumber = (start, end, n)=>{
	var arr = [];
	for (var i = 0; i < n; i++) {
		var number = Math.floor(Math.random() * (end - start + 1) + start);
		if (arr.indexOf(number) < 0) {
			arr.push(number);
		} else {
			i--;
		}
	}
	return arr;
}

//校验手机号
g.validatePhone = (phone)=>{
	const phoneRegex = /^1[3|4|5|7|8][0-9]{9}$/;
	return phoneRegex.test(phone);
}

g.rectTo = (router,options={})=>{
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

g.navTo = (router,options={})=>{
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
		uni.navigateTo({
			url: pathurl
		});
	}
}

g.isLogin = ()=>{
	let token = uni.getStorageSync('itoken');
	if(token){
		return 1;
	}else{
		return 0;
	}
}

g.checkLogin = ()=>{
	if(!g.isLogin()){
		g.rectTo("/pages/auth/login");
	}
}

g.dateFormat = (format='')=>{
	const dt = new Date();
	const y = dt.getFullYear()
	const m = dt.getMonth() + 1 < 10 ? '0' + (dt.getMonth() + 1) : dt.getMonth() + 1 // 获取当前月份的日期，不足10补0
	const d = dt.getDate() < 10 ? '0' + dt.getDate() : dt.getDate() // 获取当前几号，不足10补0
	if(!format){
		if(format == 'Y-d-m'){
			return y + '-' + m + '-' + d;
		}else if(format == 'Y/d/m'){
			return y + '/' + m + '/' + d;
		}
	}
	return y + '-' + m + '-' + d;
}

// 获取关卡中文名称（将数字1-5转换为"一"-"五"）
g.getLevelName = (levelId) => {
	const names = ['一', '二', '三', '四', '五'];
	return names[levelId - 1] || levelId;
}

export default g