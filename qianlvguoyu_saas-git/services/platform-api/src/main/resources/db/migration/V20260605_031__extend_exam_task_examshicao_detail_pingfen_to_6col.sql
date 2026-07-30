-- ===========================================================================
-- 把 exam_task_examshicao_detail.pingfen 从 2 列[{item,score}] 升级到 6 列完整版
-- 对齐 PHP /yuntiku/task-examshicao-list 的"评分"子表展示
--   序号 / 要素 / 配分 / 评分标准 / 最大扣分 / 步长
-- 日期: 2026-06-05
-- 触发: 管理端实操题详情页"评分"列以前是 2 列简版,前端已按图2 改成 6 列子表
--
-- 字段 schema(对前端 PingfenItem 渲染):
--   item        要素
--   score       配分
--   scoreDesc   评分标准
--   maxDeduct   最大扣分
--   step        步长(每次扣分)
--
-- 范围: 仅 demo 种子(id=1..7),task_id=1 (4 条) + task_id=2 (3 条)
-- 兼容性: JSON 字段,UPDATE 同结构,不影响其他列
-- ===========================================================================

UPDATE exam_task_examshicao_detail
   SET pingfen = CASE id
     WHEN 1 THEN '[{"item":"接线工艺","score":20,"scoreDesc":"接线牢固无虚接,线号清晰,工艺美观","maxDeduct":20,"step":5},{"item":"通电测试","score":30,"scoreDesc":"通电一次成功,正反转切换正常,无异常声响","maxDeduct":30,"step":10},{"item":"功能验证","score":30,"scoreDesc":"正反转控制功能完整,联锁可靠,符合工艺要求","maxDeduct":30,"step":10},{"item":"安全规范","score":20,"scoreDesc":"穿戴防护用品,工具摆放整齐,操作符合安全规程","maxDeduct":20,"step":5}]'
     WHEN 2 THEN '[{"item":"主电路接线","score":25,"scoreDesc":"主电路接线正确,接触器触点无氧化","maxDeduct":25,"step":5},{"item":"控制电路接线","score":25,"scoreDesc":"控制电路接线正确,互锁可靠","maxDeduct":25,"step":5},{"item":"时间继电器整定","score":25,"scoreDesc":"时间继电器整定值符合工艺要求,动作可靠","maxDeduct":25,"step":5},{"item":"通电测试","score":25,"scoreDesc":"通电一次成功,星三角切换正常,电机运转平稳","maxDeduct":25,"step":5}]'
     WHEN 3 THEN '[{"item":"I/O 分配","score":20,"scoreDesc":"I/O 地址分配合理,功能定义清晰","maxDeduct":20,"step":5},{"item":"梯形图","score":40,"scoreDesc":"梯形图逻辑正确,符合工艺要求,语句简洁","maxDeduct":40,"step":10},{"item":"联机调试","score":30,"scoreDesc":"联机一次成功,功能完整,异常处理到位","maxDeduct":30,"step":10},{"item":"安全规范","score":10,"scoreDesc":"断电接线,完成后通电调试,符合安全规程","maxDeduct":10,"step":5}]'
     WHEN 4 THEN '[{"item":"故障分析","score":30,"scoreDesc":"故障分析思路清晰,定位准确","maxDeduct":30,"step":10},{"item":"测量过程","score":30,"scoreDesc":"测量方法规范,数据准确,记录完整","maxDeduct":30,"step":10},{"item":"故障处理","score":30,"scoreDesc":"故障处理过程规范,措施有效","maxDeduct":30,"step":10},{"item":"结果验证","score":10,"scoreDesc":"结果验证完整,设备运行正常","maxDeduct":10,"step":5}]'
     WHEN 5 THEN '[{"item":"元件布局","score":25,"scoreDesc":"元件布局合理,符合安装规范","maxDeduct":25,"step":5},{"item":"布线工艺","score":35,"scoreDesc":"布线整齐美观,强弱电分离,符合工艺","maxDeduct":35,"step":10},{"item":"标识规范","score":20,"scoreDesc":"线号清晰完整,标识统一规范","maxDeduct":20,"step":5},{"item":"通电测试","score":20,"scoreDesc":"通电一次成功,功能完整,无异常","maxDeduct":20,"step":5}]'
     WHEN 6 THEN '[{"item":"I/O 分配","score":20,"scoreDesc":"I/O 地址分配合理,功能定义清晰","maxDeduct":20,"step":5},{"item":"SFC 流程","score":40,"scoreDesc":"SFC 流程设计正确,符合工艺要求","maxDeduct":40,"step":10},{"item":"调试运行","score":30,"scoreDesc":"调试一次成功,功能完整,异常处理到位","maxDeduct":30,"step":10},{"item":"安全互锁","score":10,"scoreDesc":"安全互锁设计完善,符合规范","maxDeduct":10,"step":5}]'
     WHEN 7 THEN '[{"item":"信号测试","score":25,"scoreDesc":"信号测试完整,数据准确,记录清晰","maxDeduct":25,"step":5},{"item":"逻辑验证","score":35,"scoreDesc":"逻辑验证过程规范,覆盖完整","maxDeduct":35,"step":10},{"item":"故障排除","score":25,"scoreDesc":"故障排除过程规范,措施有效","maxDeduct":25,"step":5},{"item":"优化建议","score":15,"scoreDesc":"优化建议合理,具有可操作性","maxDeduct":15,"step":5}]'
     ELSE pingfen
   END
 WHERE id IN (1,2,3,4,5,6,7) AND tenant_id = 1 AND deleted = 0;
