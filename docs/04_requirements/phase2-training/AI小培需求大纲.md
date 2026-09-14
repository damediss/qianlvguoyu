# AI 小培 — 需求大纲（一页）



> 目标项目：`qianlvguoyu_saas`（`platform-api`）+ App：`gjjd_appnext`  

> 关联：  

> - [学员AI助教实现方案.md](./学员AI助教实现方案.md)（编排脑；数字人另立）  

> - [AI同步学（学员复习）实现方案.md](../AI同步学（学员复习）实现方案.md)  

> - [AI精准学实现方案.md](./AI精准学实现方案.md)（学情只读；开练隔离）  

> - 画布：`ai-xiaopei-digital-human-plan.canvas.tsx`  

> 状态：**P0 已落地**；**WeKnora「AI助教_智能问答」已接入**；P1 TTS / P2 数字人 SDK 未做  

> 编写日期：2026-09-10；实现：2026-09-10  



---



## 〇、一句话



**AI 小培** = 学员可点击的学习伙伴（形象壳）+ 学习编排脑：进度类用真数据 Tool；自由提问走 WeKnora「AI助教_智能问答」；**不新建答卷域**。



---



## 一、目标用户与入口



| 项 | 结论 |

|----|------|

| 用户 | **仅学员**（班长工作台共用 FAB，走小培意图） |

| 主入口 | 「我的班级」工作台右下角 **小培 FAB** → 半屏弹窗 |

| 与助教关系 | **同一入口**：复用 `assistant/bootstrap|chat`；删除学员桌「AI 班级助理」磁贴 |

| 全页备用 | `/pages/class/assistant`（标题 AI小培） |

| 后置入口 | 同步学/精准学页内唤起；TTS；数字人 SDK |



---



## 二、核心对话能力（必须答准）



| 意图 | intent | 真数据来源 |

|------|--------|------------|

| **现在做什么** | `what_now` | `ReviewPlanApplicationService.overview` today |

| **完成得什么** | `reward_now` | 阶段话术包 + 今日剩余题量 |

| **未来得什么** | `reward_future` | 班级工种等级 + examDate / examTitle |

| **学习详情** | `learn_detail` | overview.progress/wrongStats + 精准学 wrongOverview |

| **该刷哪** | `go_sync` | 深链 `/pages/exam/syncHome?exam_id=`（默认同步学 C） |



**交互：** 开场欢迎 + summary 卡 + chips + 自由输入；回复带 navigate 深链。



---



## 三、非目标（首期不做）



聊天内答题、口型数字人、老师催交学情、A/B/C 综合分、主动推送、搜题。



---



## 四、实现落点



| 层 | 路径 |

|----|------|

| 脑 | `ClassAssistantApplicationService`（persona=`xiaopei`） |

| API | `GET/POST .../assistant/bootstrap|chat`（不变） |

| 壳 | `pages/class/components/XiaopeiSheet.vue` + `workspace.vue` FAB |

| 全页 | `pages/class/assistant.vue` |



---



## 五、分期



| 阶段 | 状态 |

|------|------|

| **P0** 立绘 + 四意图 + 深链同步学 | **已实现** |

| **P0.5** 合并入口、删旧磁贴 | **已实现**（学员/班长） |

| **P1** TTS + 场景页唤起 | 待做 |

| **P2** 数字人 SDK + 讲题链 | 待做 |



---



## 六、拍板（已按默认冻结）



1. 小培与助教同一入口 → **是**  

2. 「该刷哪」默认同步学 C → **是**  

3. 首期不要口型 → **是**  

4. 收益话术 → P0 用阶段内置包；运营配置表后置  

5. 无同步学仍展示小培 → **是**（降级话术）  



---



## 七、结论



P0 用半屏立绘小培落地四类必答意图；数字来自同步学 overview / 精准学 Hub；开练只深链，不新建答卷域。


