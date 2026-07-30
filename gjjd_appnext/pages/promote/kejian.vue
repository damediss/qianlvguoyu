<template>
	<view>
		
	</view>
</template>

<script>
	export default {
		data() {
			return {
				file:{}
			}
		},
		onLoad(options) {
			if(options.item){
				let item = JSON.parse(decodeURIComponent(options.item));
				console.log(item);
				if(item.obj){
					this.file = item.obj;
					this.openFile(this.file.fileUrl)
				}
			}
		},
		methods: {
			openFile(url){
				uni.downloadFile({
					url: url,
					success: function (res) {
						var filePath = res.tempFilePath;
						uni.openDocument({
							filePath: filePath,
							success: function (res) {
								console.log('打开文档成功');
							}
						});
					}
				});
			}
		}
	}
</script>

<style>

</style>