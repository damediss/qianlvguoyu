# Pinia Store 使用说明

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
