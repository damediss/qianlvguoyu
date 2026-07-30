<template>
    <view class="content bg-f8">
        <view class="header bg-ff border-b-ee" :style="'padding-top:' + $statusBarHeight + 'px;'">
            <view class="header-box dis-flex-vc dis-flex-hsb">
                <view class="header-back dis-flex-vc" @click="goBack">
                    <i class="gjjd-iconfont icon-xiangzuo font-s20 color-33"></i>
                </view>
                <!-- 动态标题 -->
                <view class="header-title font-s18 font-wb color-33 text-C">{{ pageTitle }}</view>
                <view class="header-placeholder w-60"></view>
            </view>
        </view>

        <view class="body" :style="'margin-top:' + (50 + $statusBarHeight) + 'px;'">
            <view v-for="(item, index) in contentList" :key="index"
                class="content-item bg-ff radius-16 shadow-sm pad-30 mar-B20 transition-fast" @click="goToDetail(item)">
                <view class="content-item-body dis-flex-vc dis-flex-hsb">
                    <text class="content-title font-s16 color-33">{{ item.title }}</text>
                    <i class="gjjd-iconfont icon-xiangyou font-s14 color-99"></i>
                </view>
            </view>

        </view>

        <!-- 空状态提示 -->
        <view v-if="contentList.length === 0 && !loading" class="empty-state dis-flex-hv flex-column">
            <text class="empty-text font-s18 color-99">暂无{{ contentTypeName }}</text>
        </view>
    </view>
</template>

<script>
import { getCoursewareList, getArticleList } from "@/api/competition";
import { isSuccess, showError } from '@/utils/response';

export default {
    data() {
        return {
            matchId: null,
            contentType: 'courseware',  // 内容类型：article 或 courseware
            contentList: [],            // 内容列表
            loading: false              // 加载状态
        }
    },
    computed: {
        /**
         * 动态页面标题
         */
        pageTitle() {
            return this.contentType === 'article' ? '阅读文章' : '阅读课件';
        },

        /**
         * 内容类型中文名
         */
        contentTypeName() {
            return this.contentType === 'article' ? '文章' : '课件';
        },

        /**
         * 日志标签
         */
        logTag() {
            return this.contentType === 'article' ? '【阅读文章】' : '【阅读课件】';
        }
    },
    onLoad(options) {
        // 1. 获取内容类型
        this.contentType = options.type || 'courseware';

        // 2. 获取竞赛ID（优先从URL参数获取）
        if (options.matchId) {
            this.matchId = parseInt(options.matchId);
        } else {
            // 其次从本地存储获取（entrance.vue进入时会存储）
            this.matchId = uni.getStorageSync('currentMatchId') || null;
        }

        // matchId为null时，提示用户并返回
        if (!this.matchId) {
            uni.showToast({
                title: '未找到竞赛信息',
                icon: 'none',
                duration: 2000
            });
            setTimeout(() => {
                uni.navigateBack();
            }, 2000);
            return;
        }

        // 加载内容列表
        this.loadContentList();
    },
    methods: {
        goBack() {
            uni.navigateBack();
        },

        /**
         * 加载内容列表
         */
        async loadContentList() {
            try {
                this.loading = true;
                uni.showLoading({ title: '加载中...' });

                // 根据类型调用不同接口
                let res;
                if (this.contentType === 'article') {
                    res = await getArticleList({ matchId: this.matchId });
                } else {
                    res = await getCoursewareList({ matchId: this.matchId });
                }

                uni.hideLoading();
                this.loading = false;

                if (isSuccess(res) && res.data) {
                    this.contentList = res.data.list || [];
                } else {
                    console.error(`${this.logTag}列表加载失败:`, res.msg);
                    showError(res, '加载失败');
                }
            } catch (error) {
                console.error(`${this.logTag}加载列表异常:`, error);
                uni.hideLoading();
                this.loading = false;
                uni.showToast({
                    title: '加载失败，请重试',
                    icon: 'none'
                });
            }
        },

        /**
         * 跳转到详情页
         */
        goToDetail(item) {


            uni.navigateTo({
                url: `/pages/competition/learning/content-detail?id=${item.id}&title=${encodeURIComponent(item.title)}&matchId=${this.matchId}&type=${this.contentType}`
            });
        }
    }
}
</script>

<style scoped>
.content {
    min-height: 100vh;
}

.header {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    z-index: 9999;
    box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
}

.header-box {
    height: 50px;
    padding: 0 30rpx;
}

.header-back {
    width: 60rpx;
    height: 60rpx;
}

.header-title {
    flex: 1;
}

.body {
    padding: 20rpx;
    padding-top: 40rpx;
}

.content-item {
    cursor: pointer;
}

.content-item:active {
    transform: scale(0.98);
    opacity: 0.9;
}

.content-title {
    flex: 1;
    line-height: 1.5;
}

.empty-state {
    min-height: 60vh;
}

.empty-text {
    text-align: center;
}
</style>
