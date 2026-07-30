# Pinia Store 使用说明

## H5 开发与构建

项目仍使用本机 HBuilderX 自带的 uni-app 编译器，命令行脚本只是调用该编译器，不会安装另一套 uni-app 依赖。

```bash
npm run dev:h5
npm run build:h5
```

`build:h5` 输出到 `dist/`。如果 HBuilderX 安装在非默认目录，通过 `HBUILDERX_PATH` 指定安装根目录。

竞赛选手端页面位于 `pages/competition-app/`，使用独立的 `competition_player_token` 登录态，不复用旧平台 `itoken`。

赛事消息中的 PDF 仍通过选手 Token 和租户头受控读取；H5 会交给浏览器或设备默认 PDF 阅读器打开，
原生 App 使用本机可用的 PDF 阅读器打开。

## 目录结构

```
store/
├── index.js           # 统一导出
├── user.js            # 用户状态管理
├── competition.js     # 竞赛状态管理
└── index.js.bak       # Vuex 旧文件备份
```

## 使用方法

### 方式1：在 Options API 中使用（推荐）

```vue
<script>
import { useUserStore } from '@/store/user'
import { useCompetitionStore } from '@/store/competition'

export default {
    setup() {
        const userStore = useUserStore()
        const competitionStore = useCompetitionStore()
        
        return {
            userStore,
            competitionStore
        }
    },
    
    onLoad() {
        // 使用 store
        console.log('用户ID:', this.userStore.userId)
        console.log('竞赛ID:', this.competitionStore.currentMatchId)
        
        // 检查登录状态
        if (!this.userStore.checkLogin()) {
            return
        }
        
        // 检查竞赛ID
        if (!this.competitionStore.checkMatchId()) {
            return
        }
    }
}
</script>
```

### 方式2：在 Composition API 中使用

```vue
<script setup>
import { useUserStore } from '@/store/user'
import { useCompetitionStore } from '@/store/competition'

const userStore = useUserStore()
const competitionStore = useCompetitionStore()

console.log('用户ID:', userStore.userId)
console.log('竞赛ID:', competitionStore.currentMatchId)
</script>
```

## User Store API

### State
- `userId` - 用户ID
- `userInfo` - 用户信息对象
- `token` - 登录令牌

### Actions
- `setUserId(userId)` - 设置用户ID
- `setUserInfo(userInfo)` - 设置用户信息
- `setToken(token)` - 设置令牌
- `login(userData)` - 登录（自动保存用户信息）
- `logout()` - 退出登录
- `checkLogin()` - 检查登录状态

### Getters
- `isLoggedIn` - 是否已登录
- `userName` - 用户昵称
- `userAvatar` - 用户头像

## Competition Store API

### State
- `currentMatchId` - 当前竞赛ID
- `registrationData` - 报名数据
- `participationData` - 参赛情况
- `currentStageData` - 当前赛段
- `styleDisplayList` - 风采展示列表
- `competitionInfoList` - 竞赛资讯列表

### Actions
- `setMatchId(matchId)` - 设置竞赛ID
- `clearMatchId()` - 清除竞赛ID
- `fetchCompetitionHome()` - 加载竞赛首页数据
- `checkMatchId()` - 检查是否已选择竞赛

### Getters
- `hasMatchId` - 是否已选择竞赛
- `competitionName` - 竞赛名称
- `registrationCount` - 报名人数
- `participantCount` - 参赛人数

## 迁移说明

### 从 utils/user.js 迁移

**旧代码：**
```javascript
import { getUserId } from '@/utils/user'
const userId = getUserId()
```

**新代码：**
```javascript
import { useUserStore } from '@/store/user'
// 在 setup() 中
const userStore = useUserStore()
return { userStore }
// 在方法中使用
const userId = this.userStore.userId
```

### 从硬编码迁移

**旧代码：**
```javascript
data() {
    return {
        matchId: 56  // 硬编码
    }
}
```

**新代码：**
```javascript
import { useCompetitionStore } from '@/store/competition'
setup() {
    const competitionStore = useCompetitionStore()
    return { competitionStore }
},
onLoad() {
    this.matchId = this.competitionStore.currentMatchId
}
```
