const STORAGE_KEY = 'app_device_id';

function createUuid(){
	return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c){
		const r = Math.random() * 16 | 0;
		const v = c === 'x' ? r : (r & 0x3 | 0x8);
		return v.toString(16);
	});
}

export function getDeviceId(){
	// #ifdef APP-PLUS
	try {
		const uuid = plus.device.uuid;
		if(uuid){
			return uuid;
		}
	} catch (e) {}
	// #endif

	let deviceId = uni.getStorageSync(STORAGE_KEY);
	if(!deviceId){
		deviceId = createUuid();
		uni.setStorageSync(STORAGE_KEY, deviceId);
	}
	return deviceId;
}

export function getDeviceInfo(){
	const info = uni.getSystemInfoSync();
	return {
		brand: info.brand || '',
		model: info.model || '',
		system: info.system || '',
		platform: info.platform || ''
	};
}
