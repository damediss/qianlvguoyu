# 双库结构差异报告（生成物，勿手改）

> 生成时间：2026-10-20 17:15
> 生成命令：`node scripts/agent/schema-diff.mjs --left test --right prod`
> 左侧库：`gjjddb2026`；右侧库：`gjjddbpro`。只读 information_schema，不含任何业务数据。
> 口径：结构漂移只能来自"两侧迁移应用进度不同"；收敛动作见 AGENTS.md 数据库铁律 2（分叉收敛）。

**结论：存在 225 张表级差异、800 处列差异、188 处索引差异。**

### 仅左侧存在的表

无

### 仅右侧存在的表

| 位置 | 说明 |
|---|---|
| `booking_card` | - |
| `booking_card_plan` | - |
| `booking_category` | - |
| `booking_emergency` | - |
| `booking_field` | - |
| `booking_integral` | - |
| `booking_integral_log` | - |
| `booking_item` | - |
| `booking_lesson` | - |
| `booking_period` | - |
| `booking_teacher` | - |
| `booking_user_card` | - |
| `booking_user_card_log` | - |
| `booking_user_coupon` | - |
| `booking_user_invoice` | - |
| `cms_ad` | - |
| `cms_ad_record` | - |
| `cms_album` | - |
| `cms_app_version` | - |
| `cms_article_comment` | - |
| `cms_article_count` | - |
| `cms_article_like` | - |
| `cms_design` | - |
| `cms_design_record` | - |
| `cms_docs` | - |
| `cms_docs_book` | - |
| `cms_docs_content` | - |
| `cms_domain` | - |
| `cms_form` | - |
| `cms_form_record` | - |
| `cms_getcertconfig` | - |
| `cms_liuyanban` | - |
| `cms_mp_menu` | - |
| `cms_navigation` | - |
| `cms_org_album` | - |
| `cms_org_article` | - |
| `cms_share` | - |
| `cms_share_auditlog` | - |
| `cms_website` | - |
| `cms_website_field` | - |
| `comments` | - |
| `company_employee` | - |
| `course_kejian_practice` | - |
| `course_kejian_practice_log` | - |
| `course_kejian_tocompany` | - |
| `course_teach` | - |
| `course_teach_tocompany` | - |
| `device_bind_whitelist` | - |
| `device_unbind_requests` | - |
| `exam_correction` | - |
| `exam_courseware_exam_detail` | - |
| `exam_courseware_exam_list` | - |
| `exam_courseware_plan` | - |
| `exam_courseware_task` | - |
| `exam_courseware_task_detail` | - |
| `exam_courseware_task_detail_list` | - |
| `exam_edit_log` | - |
| `exam_exam_detail_bak_dedup_20260914` | - |
| `exam_exam_detail_bak_dedup_20260915` | - |
| `exam_exam_detail_bak_kfix_20260929` | - |
| `exam_exam_detail_bak_kfix_20261002` | - |
| `exam_exam_detail_bak_kfix_20261003` | - |
| `exam_exam_list` | - |
| `exam_exam_plan` | - |
| `exam_knowledge_structure_bak20260930` | - |
| `exam_knowledge_structure_bak_20260918` | - |
| `exam_knowledge_structure_newrows_20260918` | - |
| `exam_operate_score` | - |
| `exam_org_knowledge_buy_log` | - |
| `exam_plan` | - |
| `exam_plan_detail` | - |
| `exam_room_people` | - |
| `exam_rooms_config` | - |
| `exam_task` | - |
| `exam_task_detail` | - |
| `exam_task_detail_info_detail` | - |
| `exam_task_detail_list` | - |
| `exam_upgrade` | - |
| `exam_user_wrong_list` | - |
| `file_template` | - |
| `master_activity` | - |
| `master_activity_registration` | - |
| `master_album` | - |
| `master_cuiqu` | - |
| `master_cuiqu_answer` | - |
| `master_outcome` | - |
| `master_publish_record` | - |
| `master_publish_train` | - |
| `master_user` | - |
| `master_vote` | - |
| `match_activity` | - |
| `match_activity_people_list` | - |
| `match_adventure_detail` | - |
| `match_adventure_log` | - |
| `match_answer_log` | - |
| `match_article` | - |
| `match_article_comment` | - |
| `match_article_like` | - |
| `match_article_new` | - |
| `match_courseware` | - |
| `match_daily_train` | - |
| `match_exam_list` | - |
| `match_exam_score_config` | - |
| `match_info` | - |
| `match_multi_rule` | - |
| `match_notice` | - |
| `match_path_checkpoint` | - |
| `match_path_rule` | - |
| `match_people_list` | - |
| `match_qa` | - |
| `match_result` | - |
| `match_sign_rule` | - |
| `match_single_rule` | - |
| `match_task_info` | - |
| `match_task_info_detail` | - |
| `match_task_list` | - |
| `match_team_pk` | - |
| `match_user_daily_train` | - |
| `match_user_point_log` | - |
| `match_user_sign_log` | - |
| `oa_app` | - |
| `oa_app_field` | - |
| `oa_app_renew` | - |
| `oa_app_url` | - |
| `oa_app_user` | - |
| `oa_assets` | - |
| `oa_assets_code` | - |
| `oa_assets_domain` | - |
| `oa_assets_email` | - |
| `oa_assets_server` | - |
| `oa_assets_site` | - |
| `oa_assets_user` | - |
| `oa_assets_vhost` | - |
| `oa_company` | - |
| `oa_company_field` | - |
| `oa_company_user` | - |
| `oa_link` | - |
| `oa_product` | - |
| `oa_product_tabs` | - |
| `oa_task` | - |
| `oa_task_count` | - |
| `oa_task_record` | - |
| `oa_task_user` | - |
| `platform_account` | - |
| `platform_jubao` | - |
| `platform_organization` | - |
| `platform_organization_detail` | - |
| `platform_permission` | - |
| `platform_permission_btn` | - |
| `platform_user_caifu` | - |
| `platform_user_certs` | - |
| `platform_user_copy1` | - |
| `platform_user_copy2` | - |
| `platform_user_copy3` | - |
| `platform_user_guzhi` | - |
| `platform_user_jifen` | - |
| `qlgt_business_data` | - |
| `qlgt_business_worktype_data` | - |
| `qlgt_kjexam` | - |
| `qlgt_kjexam_banji` | - |
| `qlgt_kjexam_banji_people` | - |
| `qlgt_kjexam_files` | - |
| `qlgt_kjexam_list` | - |
| `qlgt_kjexam_user_log` | - |
| `qlgt_kjexam_worktype` | - |
| `qlgt_match_company` | - |
| `qlgt_match_exam` | - |
| `qlgt_match_exam_detail` | - |
| `qlgt_match_exam_log` | - |
| `qlgt_match_exam_logexport_task` | - |
| `qlgt_match_people` | - |
| `qlgt_match_people_facehelp` | - |
| `qlgt_match_plan` | - |
| `qlgt_match_shicao_exam` | - |
| `qlgt_match_shicao_exam_item` | - |
| `qlgt_match_shicao_exam_user_log` | - |
| `qlgt_match_shicao_score` | - |
| `qlgt_match_wenda` | - |
| `qlgt_match_wenda_log` | - |
| `shop_brand` | - |
| `shop_cart` | - |
| `shop_charge_package` | - |
| `shop_chat_conversation` | - |
| `shop_chat_message` | - |
| `shop_express_list` | - |
| `shop_goods` | - |
| `shop_goods_category` | - |
| `shop_goods_category_rel` | - |
| `shop_goods_comment` | - |
| `shop_goods_coupon` | - |
| `shop_goods_image` | - |
| `shop_goods_log` | - |
| `shop_goods_relation` | - |
| `shop_goods_service` | - |
| `shop_goods_service_rel` | - |
| `shop_goods_sku` | - |
| `shop_goods_spec` | - |
| `shop_goods_spec_rel` | - |
| `shop_merchant` | - |
| `shop_merchant_account` | - |
| `shop_merchant_apply` | - |
| `shop_merchant_type` | - |
| `shop_message` | - |
| `shop_order` | - |
| `shop_order_goods` | - |
| `shop_order_info` | - |
| `shop_order_info_log` | - |
| `shop_spec` | - |
| `shop_spec_value` | - |
| `shop_user_address` | - |
| `shop_user_balance_log` | - |
| `shop_user_collection` | - |
| `shop_users` | - |
| `shop_wechat_deposit` | - |
| `sys_area` | - |
| `sys_dictionary` | - |
| `sys_logs` | - |
| `sys_qlgt` | - |
| `sys_user_detail` | - |
| `sys_user_follow` | - |
| `sys_user_value_log` | - |
| `tiku_exam` | - |
| `tiku_exam_log` | - |
| `tiku_knowledge` | - |
| `tiku_knowledge_rate` | - |

### 仅左侧存在的列

无

### 仅右侧存在的列

| 位置 | 说明 |
|---|---|
| `cms_article.show_type` | - |
| `cms_article.topic` | - |
| `cms_article.source` | - |
| `cms_article.platform` | - |
| `cms_article.files` | - |
| `cms_article.video` | - |
| `cms_article.accept` | - |
| `cms_article.longitude` | - |
| `cms_article.latitude` | - |
| `cms_article.province` | - |
| `cms_article.city` | - |
| `cms_article.region` | - |
| `cms_article.address` | - |
| `cms_article.to_users` | - |
| `cms_article.user_id` | - |
| `cms_article.company_id` | - |
| `cms_article.company_name` | - |
| `cms_article.comments` | - |
| `cms_article.tenant_id` | - |
| `cms_article.template_name` | - |
| `cms_article.update_time` | - |
| `cms_article.read_times` | - |
| `cms_article.match_id` | - |
| `cms_article.work_type_id` | - |
| `cms_article.work_type_level` | - |
| `cms_article_category.category_code` | - |
| `cms_article_category.type` | - |
| `cms_article_category.image` | - |
| `cms_article_category.path` | - |
| `cms_article_category.component` | - |
| `cms_article_category.page_id` | - |
| `cms_article_category.user_id` | - |
| `cms_article_category.count` | - |
| `cms_article_category.sort_number` | - |
| `cms_article_category.comments` | - |
| `cms_article_category.hide` | - |
| `cms_article_category.recommend` | - |
| `cms_article_category.show_index` | - |
| `cms_article_category.status` | - |
| `cms_article_category.tenant_id` | - |
| `cms_article_category.create_time` | - |
| `cms_article_category.update_time` | - |
| `cms_article_content.id` | - |
| `cms_article_content.tenant_id` | - |
| `demand_science.addtime` | - |
| `exam_exam_detail.suggest` | - |
| `exam_exam_detail.sno` | - |
| `exam_paper.work_type_id` | - |
| `exam_paper.level` | - |
| `exam_paper.nums` | - |
| `exam_paper.file_path` | - |
| `platform_user.device_id` | - |
| `platform_user.device_info` | - |
| `platform_user.device_bindtime` | - |
| `platform_user_detail.point` | - |
| `platform_user_detail.point_sign` | - |
| `platform_user_detail.school_paper_has` | - |
| `platform_user_detail.work_type_id` | - |
| `task_exam.is_archived` | - |
| `task_exam.archived_at` | - |
| `task_exam.archived_reason` | - |
| `task_examshicao.is_archived` | - |
| `task_examshicao.archived_at` | - |
| `task_examshicao.archived_reason` | - |
| `task_examsub.is_archived` | - |
| `task_examsub.archived_at` | - |
| `task_examsub.archived_reason` | - |
| `task_examsub_detail.isJisuan` | - |
| `task_ximu.is_archived` | - |
| `task_ximu.archived_at` | - |
| `task_ximu.archived_reason` | - |
| `train_sign_up_selftask.running_key` | - |

### 列类型变化

| 位置 | 说明 |
|---|---|
| `cms_article.article_id` | int → int unsigned |
| `cms_article.title` | varchar(500) → varchar(300) |
| `cms_article.type` | tinyint → int unsigned |
| `cms_article.category_id` | int → int unsigned |
| `cms_article.image` | varchar(500) → varchar(300) |
| `cms_article.actual_views` | int → int unsigned |
| `cms_article.virtual_views` | int → int unsigned |
| `cms_article.likes` | int → int unsigned |
| `cms_article.comment_numbers` | int → int unsigned |
| `cms_article.sort_number` | int → int unsigned |
| `cms_article.status` | tinyint → int |
| `cms_article.publish_time` | datetime → timestamp |
| `cms_article.create_time` | datetime → timestamp |
| `cms_article.deleted` | tinyint → int |
| `cms_article_category.category_id` | int → int unsigned |
| `cms_article_category.parent_id` | int → int unsigned |
| `cms_article_category.title` | varchar(200) → varchar(50) |
| `cms_article_category.deleted` | tinyint → int |
| `cms_article_content.article_id` | int → int unsigned |
| `cms_article_content.content` | mediumtext → longtext |
| `cms_article_content.create_time` | datetime → timestamp |
| `demand_employee.id` | bigint → int |
| `demand_employee.org_id` | bigint → int |
| `demand_employee.work` | varchar(255) → varchar(50) |
| `demand_employee.education` | varchar(64) → varchar(50) |
| `demand_employee.major` | varchar(255) → varchar(50) |
| `demand_employee.deleted` | tinyint → int |
| `demand_science.id` | bigint → int |
| `demand_science.org_id` | bigint → int |
| `demand_science.content` | text → varchar(500) |
| `demand_science.deleted` | tinyint → int |
| `exam_course.level` | tinyint(1) → tinyint |
| `exam_course.is_general` | tinyint(1) → tinyint |
| `exam_course.tenant_id` | bigint → int |
| `exam_exam_detail.org_id` | bigint → int |
| `exam_exam_detail.is_private` | int → tinyint |
| `exam_exam_detail.difficulty` | int → tinyint |
| `exam_exam_detail.type` | varchar(50) → varchar(255) |
| `exam_exam_detail.keyword` | varchar(500) → varchar(255) |
| `exam_exam_detail.audio` | varchar(500) → varchar(255) |
| `exam_exam_detail.video` | varchar(500) → varchar(255) |
| `exam_exam_detail.score` | decimal(8,2) → decimal(10,1) unsigned |
| `exam_exam_detail.resolve` | text → varchar(255) |
| `exam_exam_detail.check_status` | int → tinyint |
| `exam_exam_detail.final_check_status` | int → tinyint |
| `exam_exam_detail.create_time` | datetime → timestamp |
| `exam_exam_detail.check_time` | datetime → timestamp |
| `exam_exam_detail.final_check_time` | datetime → timestamp |
| `exam_exam_detail.deleted` | int → tinyint |
| `exam_knowledge_rate.type` | int → tinyint |
| `exam_knowledge_rate.is_standard` | int → tinyint |
| `exam_knowledge_rate.sort_number` | int → int unsigned |
| `exam_knowledge_rate.comments` | varchar(500) → varchar(1000) |
| `exam_knowledge_rate.create_time` | datetime → timestamp |
| `exam_knowledge_rate_detail.sort_number` | int → int unsigned |
| `exam_knowledge_rate_detail.comments` | varchar(500) → varchar(1000) |
| `exam_knowledge_rate_detail.create_time` | datetime → timestamp |
| `exam_knowledge_structure.type` | int → tinyint |
| `exam_knowledge_structure.use_type` | int → tinyint |
| `exam_knowledge_structure.cover` | varchar(500) → varchar(255) |
| `exam_knowledge_structure.intro` | varchar(1000) → varchar(255) |
| `exam_knowledge_structure.level` | int → tinyint |
| `exam_knowledge_structure.amount` | int → decimal(10,2) |
| `exam_knowledge_structure.create_time` | datetime → timestamp |
| `exam_make_exam_plan.type` | int → tinyint |
| `exam_make_exam_plan.exam_type` | int → tinyint |
| `exam_make_exam_plan.is_computer` | int → tinyint |
| `exam_make_exam_plan.is_print` | int → tinyint |
| `exam_make_exam_plan.open_rate` | int → tinyint |
| `exam_make_exam_plan.total_point` | int → tinyint |
| `exam_make_exam_plan.pass_point` | int → tinyint |
| `exam_make_exam_plan.comments` | varchar(500) → varchar(255) |
| `exam_make_exam_plan.is_all` | int → tinyint |
| `exam_make_exam_plan.create_time` | datetime → timestamp |
| `exam_make_exam_plan_detail.type` | varchar(50) → enum('judge','single','multi','sort','answer','single_discrete','multi_discrete','match','simple_fill','fill','cal','essay','explain','drafting','correct','design','action','sim','example','context','cal-analysis','drag','analysis') |
| `exam_make_exam_plan_detail.start_point` | int → tinyint |
| `exam_make_exam_plan_detail.end_point` | int → tinyint |
| `exam_make_exam_plan_detail.difficulty_start` | int → tinyint |
| `exam_make_exam_plan_detail.difficulty_end` | int → tinyint |
| `exam_make_exam_plan_detail.create_time` | datetime → timestamp |
| `exam_operate_detail.title` | varchar(255) → varchar(500) |
| `exam_operate_detail.score` | decimal(10,2) → decimal(10,1) |
| `exam_operate_detail.create_time` | datetime → timestamp |
| `exam_operate_detail.update_time` | datetime → timestamp |
| `exam_paper.type` | int → tinyint |
| `exam_paper.code` | varchar(100) → varchar(255) |
| `exam_paper.is_computer` | int → tinyint |
| `exam_paper.is_print` | int → tinyint |
| `exam_paper.comments` | varchar(500) → varchar(255) |
| `exam_paper.create_time` | datetime → timestamp |
| `exam_paper_detail.type` | varchar(50) → varchar(255) |
| `exam_paper_detail.diff` | int → tinyint |
| `exam_paper_detail.score` | decimal(10,2) → decimal(10,1) unsigned |
| `exam_paper_detail.resolve` | text → varchar(255) |
| `exam_paper_detail.create_time` | datetime → timestamp |
| `exam_result.id` | bigint → int |
| `exam_result.exam_plan_id` | bigint → int |
| `exam_result.paper_id` | bigint → int |
| `exam_result.detail_id` | bigint → int |
| `exam_result.get_score` | decimal(10,2) → decimal(5,1) |
| `exam_result.exam_type` | int → tinyint |
| `exam_result.exam_id` | bigint → int |
| `exam_result.user_id` | bigint → int |
| `exam_result.platform_user_id` | bigint → int |
| `exam_result.tenant_id` | bigint → int |
| `exam_result.create_time` | datetime → timestamp |
| `exam_result.deleted` | tinyint → int |
| `exam_work_type.tenant_id` | bigint → int |
| `platform_sms.id` | bigint → int |
| `platform_sms.phone` | varchar(32) → varchar(11) |
| `platform_sms.sms` | varchar(16) → varchar(10) |
| `platform_sms.addtime` | bigint → int |
| `platform_user.org_id` | int → bigint |
| `platform_user.company_id` | int → bigint |
| `platform_user_detail.id` | bigint → int |
| `platform_user_detail.realname` | varchar(100) → varchar(20) |
| `platform_user_detail.phone` | varchar(32) → varchar(11) |
| `platform_user_detail.idcard` | varchar(32) → varchar(20) |
| `platform_user_detail.nation` | varchar(50) → varchar(20) |
| `platform_user_detail.education` | varchar(100) → varchar(50) |
| `platform_user_detail.school` | varchar(128) → varchar(100) |
| `platform_user_detail.major` | varchar(64) → varchar(100) |
| `platform_user_detail.school_no` | varchar(64) → varchar(50) |
| `platform_user_detail.in_school_date` | varchar(16) → varchar(20) |
| `platform_user_detail.graduate_date` | varchar(16) → varchar(20) |
| `platform_user_detail.work_time` | varchar(16) → varchar(20) |
| `platform_user_detail.title_type` | varchar(32) → varchar(30) |
| `platform_user_detail.political` | varchar(100) → varchar(10) |
| `platform_user_detail.address` | varchar(500) → varchar(100) |
| `platform_user_detail.house_type` | varchar(32) → varchar(20) |
| `platform_user_detail.idcard_header` | varchar(500) → varchar(255) |
| `platform_user_detail.birthday` | varchar(32) → varchar(20) |
| `platform_user_detail.idcard_front` | varchar(500) → varchar(255) |
| `platform_user_detail.idcard_back` | varchar(500) → varchar(255) |
| `platform_user_token.id` | bigint → int |
| `platform_user_token.user_id` | bigint → int |
| `platform_user_token.token` | varchar(128) → varchar(100) |
| `platform_user_token.addtime` | bigint → int |
| `task_exam.status` | tinyint → int |
| `task_exam.file_path` | varchar(500) → varchar(200) |
| `task_exam_audit_records.audit_role` | tinyint → int |
| `task_exam_audit_records.exam_content` | text → varchar(300) |
| `task_exam_audit_records.exam_a` | varchar(2000) → varchar(300) |
| `task_exam_audit_records.exam_b` | varchar(2000) → varchar(300) |
| `task_exam_audit_records.exam_c` | varchar(2000) → varchar(300) |
| `task_exam_audit_records.exam_d` | varchar(2000) → varchar(300) |
| `task_exam_audit_records.exam_e` | varchar(2000) → varchar(300) |
| `task_exam_audit_records.exam_f` | varchar(2000) → varchar(300) |
| `task_exam_audit_records.exam_g` | varchar(2000) → varchar(300) |
| `task_exam_audit_records.exam_h` | varchar(2000) → varchar(300) |
| `task_exam_audit_records.exam_answer` | varchar(50) → varchar(20) |
| `task_exam_audit_records.audit_status` | tinyint → int |
| `task_exam_audit_records.ymd` | varchar(20) → varchar(10) |
| `task_exam_audit_records.exam_idea` | text → varchar(500) |
| `task_exam_detail.xingwei` | text → varchar(100) |
| `task_exam_detail.exam_type` | varchar(50) → varchar(10) |
| `task_exam_detail.exam_content` | text → varchar(300) |
| `task_exam_detail.exam_a` | varchar(2000) → varchar(300) |
| `task_exam_detail.exam_b` | varchar(2000) → varchar(300) |
| `task_exam_detail.exam_c` | varchar(2000) → varchar(300) |
| `task_exam_detail.exam_d` | varchar(2000) → varchar(300) |
| `task_exam_detail.exam_e` | varchar(2000) → varchar(300) |
| `task_exam_detail.exam_f` | varchar(2000) → varchar(300) |
| `task_exam_detail.exam_g` | varchar(2000) → varchar(300) |
| `task_exam_detail.exam_h` | varchar(2000) → varchar(300) |
| `task_exam_detail.exam_answer` | varchar(50) → varchar(20) |
| `task_exam_detail.kaohe1` | text → varchar(100) |
| `task_exam_detail.kaohe2` | text → varchar(100) |
| `task_exam_detail.kaohe3` | text → varchar(50) |
| `task_exam_detail.kaohe4` | text → varchar(50) |
| `task_exam_detail.jianding` | text → varchar(100) |
| `task_exam_detail.nanyicd` | varchar(20) → varchar(10) |
| `task_examshicao.status` | tinyint → int |
| `task_examshicao.file_path` | varchar(500) → varchar(200) |
| `task_examshicao_audit_records.audit_role` | tinyint → int |
| `task_examshicao_audit_records.content` | text → varchar(500) |
| `task_examshicao_audit_records.kaochang` | text → varchar(500) |
| `task_examshicao_audit_records.kaosheng` | text → varchar(500) |
| `task_examshicao_audit_records.audit_status` | tinyint → int |
| `task_examshicao_audit_records.ymd` | varchar(20) → varchar(10) |
| `task_examshicao_detail.jianding` | varchar(500) → varchar(100) |
| `task_examshicao_detail.content` | text → varchar(500) |
| `task_examshicao_detail.kaochang` | text → varchar(500) |
| `task_examshicao_detail.kaosheng` | text → varchar(500) |
| `task_examsub.status` | tinyint → int |
| `task_examsub.file_path` | varchar(500) → varchar(200) |
| `task_examsub_audit_records.audit_role` | tinyint → int |
| `task_examsub_audit_records.content` | text → varchar(500) |
| `task_examsub_audit_records.pic` | varchar(500) → varchar(200) |
| `task_examsub_audit_records.audit_status` | tinyint → int |
| `task_examsub_audit_records.ymd` | varchar(20) → varchar(10) |
| `task_examsub_detail.zhishidian` | varchar(500) → varchar(100) |
| `task_examsub_detail.content` | text → varchar(500) |
| `task_examsub_detail.pic` | varchar(500) → varchar(200) |
| `task_expert.task_role` | tinyint → int |
| `task_expert.tag` | varchar(20) → varchar(30) |
| `task_expert.addtime` | datetime → int |
| `task_expert.endtime` | datetime → int |
| `task_ximu.types` | tinyint → int |
| `task_ximu.status` | tinyint → int |
| `task_ximu.file_path` | varchar(500) → varchar(200) |
| `task_ximu.addtime` | datetime → int |
| `task_ximu_detail.sno` | varchar(100) → varchar(50) |
| `task_ximu_detail.sno_0` | json → varchar(100) |
| `task_ximu_detail.sno_1` | json → varchar(100) |
| `task_ximu_detail.title` | varchar(1000) → varchar(500) |
| `task_ximu_detail.sno_item` | varchar(100) → varchar(50) |
| `task_ximu_detail.sno_title` | varchar(2000) → varchar(500) |
| `task_ximu_detail.row_column` | varchar(20) → varchar(30) |
| `train_budget.id` | bigint → int |
| `train_budget.train_id` | bigint → int |
| `train_budget.province` | varchar(100) → varchar(255) |
| `train_budget.city` | varchar(100) → varchar(255) |
| `train_budget.region` | varchar(100) → varchar(255) |
| `train_budget.train_date` | varchar(255) → varchar(50) |
| `train_budget.train_work_type_id` | bigint → int |
| `train_budget.train_level` | varchar(100) → varchar(255) |
| `train_budget.expect_income` | decimal(16,2) → decimal(10,2) |
| `train_budget.actual_income` | decimal(16,2) → decimal(10,2) |
| `train_budget.school_manage_fee` | decimal(16,2) → decimal(10,2) |
| `train_budget.manage_fee` | decimal(16,2) → decimal(10,2) |
| `train_budget.research_fee` | decimal(16,2) → decimal(10,2) |
| `train_budget.company_require_commission` | decimal(16,2) → decimal(10,2) |
| `train_budget.agent_require_commission` | decimal(16,2) → decimal(10,2) |
| `train_budget.teacher_course_fee` | decimal(16,2) → decimal(10,2) |
| `train_budget.teacher_fee` | decimal(16,2) → decimal(10,2) |
| `train_budget.staff_fee` | decimal(16,2) → decimal(10,2) |
| `train_budget.staff_travel_fee` | decimal(16,2) → decimal(10,2) |
| `train_budget.train_fee` | decimal(16,2) → decimal(10,2) |
| `train_budget.point_rewards` | decimal(16,2) → decimal(10,2) |
| `train_budget.business_fee` | decimal(16,2) → decimal(10,0) |
| `train_budget.service_fee` | decimal(16,2) → decimal(10,2) |
| `train_budget.review_fee` | decimal(16,2) → decimal(10,2) |
| `train_budget.maintain_fee` | decimal(16,2) → decimal(10,2) |
| `train_budget.staff_makeup_train_fee` | decimal(16,2) → decimal(10,2) |
| `train_budget.review_makeup_train_fee` | decimal(16,2) → decimal(10,2) |
| `train_budget.profit` | decimal(16,2) → decimal(10,2) |
| `train_budget.comments` | text → varchar(300) |
| `train_budget.user_id` | bigint → int |
| `train_budget.tenant_id` | bigint → int |
| `train_budget.create_time` | datetime → timestamp |
| `train_budget.update_time` | datetime → timestamp |
| `train_budget.deleted` | tinyint → int |
| `train_course.id` | bigint → int |
| `train_course.train_id` | bigint → int |
| `train_course.teacher_id_card` | varchar(50) → varchar(255) |
| `train_course.teacher_name` | varchar(255) → varchar(20) |
| `train_course.teacher_user_id` | bigint → int |
| `train_course.type` | int → tinyint |
| `train_course.course_date` | varchar(255) → varchar(20) |
| `train_course.title` | varchar(255) → varchar(500) |
| `train_course.start_time` | varchar(50) → varchar(20) |
| `train_course.end_time` | varchar(50) → varchar(20) |
| `train_course.tenant_id` | bigint → int |
| `train_course.create_time` | datetime → timestamp |
| `train_course.deleted` | tinyint → int |
| `train_declare_fields.deleted` | tinyint(1) → tinyint |
| `train_declare_user_data.deleted` | tinyint(1) → tinyint |
| `train_dev_organization.is_coop` | tinyint(1) → tinyint |
| `train_exam_list.id` | bigint → int |
| `train_exam_list.level` | varchar(100) → varchar(255) |
| `train_exam_list.train_id` | bigint → int |
| `train_exam_list.type` | int → tinyint |
| `train_exam_list.source` | int → tinyint |
| `train_exam_list.invigilate_user_id` | bigint → int |
| `train_exam_list.evaluate_user_id` | bigint → int |
| `train_exam_list.watcher_user_id` | bigint → int |
| `train_exam_list.staff_user_id` | bigint → int |
| `train_exam_list.proposition_user_id` | bigint → int |
| `train_exam_list.marking_user_id` | bigint → int |
| `train_exam_list.exam_id` | bigint → int |
| `train_exam_list.exam_date` | varchar(255) → varchar(20) |
| `train_exam_list.exam_date_mode` | int → tinyint |
| `train_exam_list.need_finish_adventure` | int → tinyint |
| `train_exam_list.need_finish_normal` | int → tinyint |
| `train_exam_list.user_id` | bigint → int |
| `train_exam_list.tenant_id` | bigint → int |
| `train_exam_list.jigou_id` | bigint → int |
| `train_exam_list.create_time` | datetime → timestamp |
| `train_exam_list.deleted` | tinyint → int |
| `train_exam_people_in_room.gender` | tinyint(1) → tinyint |
| `train_exam_people_list.work_type_level` | tinyint(1) → tinyint |
| `train_exam_people_list.gender` | tinyint(1) → tinyint |
| `train_exam_people_list.has_set_real_score` | tinyint(1) → tinyint |
| `train_exam_people_list.high_level_status` | tinyint(1) → tinyint |
| `train_exam_people_list.in_white_list` | tinyint(1) → tinyint |
| `train_exam_work_type_list.level` | tinyint(1) → tinyint |
| `train_file_detail.id` | bigint → int |
| `train_file_detail.file_id` | bigint → int |
| `train_file_detail.user_id` | bigint → int |
| `train_file_detail.tenant_id` | bigint → int |
| `train_file_detail.create_time` | datetime → timestamp |
| `train_file_list.id` | bigint → int |
| `train_file_list.pk` | varchar(100) → varchar(255) |
| `train_file_list.user_id` | bigint → int |
| `train_file_list.tenant_id` | bigint → int |
| `train_file_list.create_time` | datetime → timestamp |
| `train_group_knowledge.check_status` | tinyint(1) → tinyint |
| `train_group_knowledge.final_check_status` | tinyint(1) → tinyint |
| `train_info_class.id` | bigint → int |
| `train_info_class.train_info_id` | bigint → int |
| `train_info_class.title` | varchar(255) → varchar(50) |
| `train_info_class.work_type_id` | bigint → int |
| `train_info_class.date_start` | varchar(50) → varchar(10) |
| `train_info_class.date_end` | varchar(50) → varchar(10) |
| `train_info_class.file_temps` | text → varchar(100) |
| `train_info_class.kcbiao_file` | varchar(500) → varchar(255) |
| `train_info_class.deleted` | tinyint → int |
| `train_info_list.id` | bigint → int |
| `train_info_list.cover` | varchar(500) → varchar(255) |
| `train_info_list.intro` | text → varchar(255) |
| `train_info_list.group_type` | int → tinyint |
| `train_info_list.master_type` | int → tinyint |
| `train_info_list.type` | int → tinyint |
| `train_info_list.cate` | varchar(100) → varchar(255) |
| `train_info_list.work_type_id` | bigint → int |
| `train_info_list.course_id` | bigint → int |
| `train_info_list.position` | varchar(255) → varchar(500) |
| `train_info_list.head_confirm` | int → tinyint |
| `train_info_list.user_id` | bigint → int |
| `train_info_list.company_has_confirm` | int → tinyint |
| `train_info_list.company_confirm_time` | datetime → timestamp |
| `train_info_list.company_confirm_user_id` | bigint → int |
| `train_info_list.pass_status` | int → tinyint |
| `train_info_list.check_user_id` | bigint → int |
| `train_info_list.is_sorted_learn` | int → tinyint |
| `train_info_list.start_date` | varchar(50) → varchar(20) |
| `train_info_list.end_date` | varchar(50) → varchar(20) |
| `train_info_list.tenant_id` | bigint → int |
| `train_info_list.create_time` | datetime → timestamp |
| `train_info_list.album` | varchar(500) → text |
| `train_info_list.province_id` | bigint → int |
| `train_info_list.city_id` | bigint → int |
| `train_info_list.ability_score` | decimal(10,2) → decimal(10,3) |
| `train_info_list.region_id` | bigint → int |
| `train_info_list.in_charge_user_id` | bigint → int |
| `train_info_list.traindemand_id` | bigint → int |
| `train_info_list.deleted` | tinyint → int |
| `train_need.id` | bigint → int |
| `train_need.target` | varchar(500) → varchar(255) |
| `train_need.teacher_require` | varchar(500) → varchar(255) |
| `train_need.user_id` | bigint → int |
| `train_need.tenant_id` | bigint → int |
| `train_need.create_time` | datetime → timestamp |
| `train_need.deleted` | tinyint → int |
| `train_request.id` | bigint → int |
| `train_request.type` | varchar(100) → varchar(255) |
| `train_request.contact` | varchar(255) → varchar(20) |
| `train_request.contact_phone` | varchar(50) → varchar(20) |
| `train_request.student_form` | varchar(255) → varchar(500) |
| `train_request.user_id` | bigint → int |
| `train_request.tenant_id` | bigint → int |
| `train_request.create_time` | datetime → timestamp |
| `train_request.deleted` | tinyint → int |
| `train_research_people_list.gender` | tinyint(1) → tinyint |
| `train_research_people_list.is_unemployment` | tinyint(1) → tinyint |
| `train_research_people_list.is_from_countryside` | tinyint(1) → tinyint |
| `train_research_people_list.is_middle_school_urban` | tinyint(1) → tinyint |
| `train_research_people_list.is_middle_school_countryside` | tinyint(1) → tinyint |
| `train_research_people_list.is_poor_urban` | tinyint(1) → tinyint |
| `train_research_people_list.is_poor_countryside` | tinyint(1) → tinyint |
| `train_research_people_list.is_soldier` | tinyint(1) → tinyint |
| `train_research_people_list.is_soldier_family` | tinyint(1) → tinyint |
| `train_research_people_list.is_farmer` | tinyint(1) → tinyint |
| `train_research_people_list.is_staff` | tinyint(1) → tinyint |
| `train_resource.id` | bigint → int |
| `train_resource.work_type_id` | bigint → int |
| `train_resource.type` | varchar(100) → varchar(20) |
| `train_resource.user_id` | bigint → int |
| `train_resource.tenant_id` | bigint → int |
| `train_resource.create_time` | datetime → timestamp |
| `train_resource.deleted` | tinyint → int |
| `train_sign_up.is_new` | tinyint(1) → tinyint |
| `train_sign_up.level` | tinyint(1) → tinyint |
| `train_sign_up.has_open` | tinyint(1) → tinyint |
| `train_sign_up.deleted` | tinyint(1) → tinyint |
| `train_sign_up.need_contact` | tinyint(1) → tinyint |
| `train_sign_up_people.deleted` | tinyint(1) → tinyint |
| `train_sign_up_people.has_pay` | tinyint(1) → tinyint |
| `train_sign_up_self.has_open` | tinyint(1) → tinyint |
| `train_sign_up_self.deleted` | tinyint(1) → tinyint |
| `train_sign_up_self.need_contact` | tinyint(1) → tinyint |
| `train_solution.id` | bigint → int |
| `train_solution.train_info_id` | bigint → int |
| `train_solution.tenant_id` | bigint → int |
| `train_solution.create_time` | datetime → timestamp |
| `train_solution.user_id` | bigint → int |
| `train_solution.content` | text → longtext |
| `train_solution.file_path` | varchar(500) → varchar(255) |
| `train_solution.reject_reason` | varchar(500) → text |
| `train_talent_needs.id` | bigint → int |
| `train_talent_needs.edu` | varchar(100) → varchar(255) |
| `train_talent_needs.work_year` | varchar(50) → varchar(200) |
| `train_talent_needs.in_position_date` | varchar(50) → varchar(255) |
| `train_talent_needs.user_id` | bigint → int |
| `train_talent_needs.tenant_id` | bigint → int |
| `train_talent_needs.create_time` | datetime → timestamp |
| `train_talent_needs.deleted` | tinyint → int |
| `train_task_info.id` | bigint → int |
| `train_task_info.train_id` | bigint → int |
| `train_task_info.cover` | varchar(500) → varchar(255) |
| `train_task_info.course_id` | bigint → int |
| `train_task_info.train_task_id` | bigint → int |
| `train_task_info.knowledge_id` | bigint → int |
| `train_task_info.is_required` | int → tinyint |
| `train_task_info.user_id` | bigint → int |
| `train_task_info.tenant_id` | bigint → int |
| `train_task_info.create_time` | datetime → timestamp |
| `train_task_info.deleted` | tinyint → int |
| `train_task_info_detail.id` | bigint → int |
| `train_task_info_detail.train_id` | bigint → int |
| `train_task_info_detail.task_list_id` | bigint → int |
| `train_task_info_detail.work_type_id` | bigint → int |
| `train_task_info_detail.course_id` | bigint → int |
| `train_task_info_detail.knowledge_id` | bigint → int |
| `train_task_info_detail.type` | varchar(100) → varchar(20) |
| `train_task_info_detail.content` | text → varchar(500) |
| `train_task_info_detail.get_learn_time` | decimal(10,2) → decimal(10,1) |
| `train_task_info_detail.is_required` | int → tinyint |
| `train_task_info_detail.user_id` | bigint → int |
| `train_task_info_detail.tenant_id` | bigint → int |
| `train_task_info_detail.create_time` | datetime → timestamp |
| `train_task_info_detail.deleted` | tinyint → int |
| `train_task_list.id` | bigint → int |
| `train_task_list.work_type_id` | bigint → int |
| `train_task_list.course_id` | bigint → int |
| `train_task_list.knowledge_id` | bigint → int |
| `train_task_list.type` | varchar(100) → varchar(20) |
| `train_task_list.content` | text → varchar(500) |
| `train_task_list.get_learn_time` | decimal(10,2) → decimal(10,1) |
| `train_task_list.is_required` | int → tinyint |
| `train_task_list.user_id` | bigint → int |
| `train_task_list.tenant_id` | bigint → int |
| `train_task_list.create_time` | datetime → timestamp |
| `train_task_list.check_status` | int → tinyint |
| `train_task_list.final_check_status` | int → tinyint |
| `train_task_list.check_time` | datetime → timestamp |
| `train_task_list.check_user_id` | bigint → int |
| `train_task_list.final_check_time` | datetime → timestamp |
| `train_task_list.final_check_user_id` | bigint → int |
| `train_task_list.deleted` | tinyint → int |
| `train_user_ability.id` | bigint → int |
| `train_user_ability.train_id` | bigint → int |
| `train_user_ability.course_id` | bigint → int |
| `train_user_ability.knowledge_id` | bigint → int |
| `train_user_ability.result` | varchar(500) → varchar(20) |
| `train_user_ability.config_id` | bigint → int |
| `train_user_ability.score` | int → tinyint |
| `train_user_ability.current_score` | int → tinyint |
| `train_user_ability.user_id` | bigint → int |
| `train_user_ability.platform_user_id` | bigint → int |
| `train_user_ability.comments` | text → varchar(20) |
| `train_user_ability.tenant_id` | bigint → int |
| `train_user_ability.create_time` | datetime → timestamp |
| `train_user_ability.deleted` | tinyint → int |
| `train_user_course_paper.id` | bigint → int |
| `train_user_course_paper.user_id` | bigint → int |
| `train_user_course_paper.platform_user_id` | bigint → int |
| `train_user_course_paper.course_id` | bigint → int |
| `train_user_course_paper.work_type_id` | bigint → int |
| `train_user_course_paper.tenant_id` | bigint → int |
| `train_user_course_paper.create_time` | datetime → timestamp |
| `train_user_course_paper.deleted` | tinyint → int |
| `train_user_course_plan.id` | bigint → int |
| `train_user_course_plan.user_id` | bigint → int |
| `train_user_course_plan.platform_user_id` | bigint → int |
| `train_user_course_plan.add_from` | int → tinyint |
| `train_user_course_plan.course_id` | bigint → int |
| `train_user_course_plan.date` | varchar(50) → varchar(20) |
| `train_user_course_plan.tenant_id` | bigint → int |
| `train_user_course_plan.create_time` | datetime → timestamp |
| `train_user_course_plan.deleted` | tinyint → int |
| `train_user_exam_log.id` | bigint → int |
| `train_user_exam_log.train_id` | bigint → int |
| `train_user_exam_log.knowledge_id` | bigint → int |
| `train_user_exam_log.user_id` | bigint → int |
| `train_user_exam_log.tenant_id` | bigint → int |
| `train_user_exam_log.create_time` | datetime → timestamp |
| `train_user_exam_log.update_time` | datetime → timestamp |
| `train_user_exam_log.deleted` | tinyint → int |
| `train_user_info.id` | bigint → int |
| `train_user_info.user_id` | bigint → int |
| `train_user_info.person_type` | varchar(50) → varchar(200) |
| `train_user_info.person_supplementary` | varchar(100) → varchar(200) |
| `train_user_info.house_type` | varchar(50) → varchar(200) |
| `train_user_info.nationality` | varchar(50) → varchar(20) |
| `train_user_info.education` | varchar(100) → varchar(200) |
| `train_user_info.job_intention` | varchar(255) → varchar(200) |
| `train_user_info.is_party_member` | int → tinyint |
| `train_user_info.id_card_front` | varchar(500) → varchar(255) |
| `train_user_info.id_card_back` | varchar(500) → varchar(255) |
| `train_user_info.photo` | varchar(500) → varchar(255) |
| `train_user_info.edu_photo` | varchar(500) → text |
| `train_user_info.work_contract` | varchar(500) → text |
| `train_user_info.tenant_id` | bigint → int |
| `train_user_info.create_time` | datetime → timestamp |
| `train_user_info.deleted` | tinyint → int |
| `train_user_learn_log.id` | bigint → int |
| `train_user_learn_log.train_id` | bigint → int |
| `train_user_learn_log.match_id` | bigint → int |
| `train_user_learn_log.train_task_id` | bigint → int |
| `train_user_learn_log.user_id` | bigint → int |
| `train_user_learn_log.tenant_id` | bigint → int |
| `train_user_learn_log.create_time` | datetime → timestamp |
| `train_user_learn_log.deleted` | tinyint → int |
| `train_user_paper.id` | bigint → int |
| `train_user_paper.type` | varchar(100) → varchar(20) |
| `train_user_paper.user_id` | bigint → int |
| `train_user_paper.platform_user_id` | bigint → int |
| `train_user_paper.tenant_id` | bigint → int |
| `train_user_paper.create_time` | datetime → timestamp |
| `train_user_paper.pk` | bigint → int |
| `train_user_paper.deleted` | tinyint → int |
| `train_user_patent.id` | bigint → int |
| `train_user_patent.user_id` | bigint → int |
| `train_user_patent.tenant_id` | bigint → int |
| `train_user_patent.create_time` | datetime → timestamp |
| `train_user_patent.update_time` | datetime → timestamp |

### 列可空性变化

| 位置 | 说明 |
|---|---|
| `cms_article.category_id` | NULL → NOT NULL |
| `cms_article.likes` | NOT NULL → NULL |
| `cms_article.comment_numbers` | NOT NULL → NULL |
| `cms_article.recommend` | NOT NULL → NULL |
| `cms_article_content.article_id` | NOT NULL → NULL |
| `demand_employee.tenant_id` | NOT NULL → NULL |
| `demand_employee.addtime` | NULL → NOT NULL |
| `demand_science.tenant_id` | NOT NULL → NULL |
| `demand_science.created_at` | NOT NULL → NULL |
| `demand_science.updated_at` | NOT NULL → NULL |
| `exam_exam_detail.create_time` | NULL → NOT NULL |
| `exam_exam_detail.deleted` | NOT NULL → NULL |
| `exam_knowledge_rate.sort_number` | NULL → NOT NULL |
| `exam_knowledge_rate.status` | NULL → NOT NULL |
| `exam_knowledge_rate.deleted` | NULL → NOT NULL |
| `exam_knowledge_rate.create_time` | NULL → NOT NULL |
| `exam_knowledge_rate_detail.knowledge_rate_id` | NOT NULL → NULL |
| `exam_knowledge_rate_detail.sort_number` | NULL → NOT NULL |
| `exam_knowledge_rate_detail.status` | NULL → NOT NULL |
| `exam_knowledge_rate_detail.deleted` | NULL → NOT NULL |
| `exam_knowledge_rate_detail.create_time` | NULL → NOT NULL |
| `exam_knowledge_structure.type` | NOT NULL → NULL |
| `exam_knowledge_structure.title` | NOT NULL → NULL |
| `exam_knowledge_structure.level` | NOT NULL → NULL |
| `exam_knowledge_structure.create_time` | NULL → NOT NULL |
| `exam_make_exam_plan.exam_type` | NOT NULL → NULL |
| `exam_make_exam_plan.status` | NULL → NOT NULL |
| `exam_make_exam_plan.sort_number` | NULL → NOT NULL |
| `exam_make_exam_plan.create_time` | NULL → NOT NULL |
| `exam_make_exam_plan_detail.make_id` | NOT NULL → NULL |
| `exam_make_exam_plan_detail.status` | NULL → NOT NULL |
| `exam_make_exam_plan_detail.sort_number` | NULL → NOT NULL |
| `exam_make_exam_plan_detail.create_time` | NULL → NOT NULL |
| `exam_operate_detail.status` | NULL → NOT NULL |
| `exam_operate_detail.sort_number` | NULL → NOT NULL |
| `exam_operate_detail.create_time` | NULL → NOT NULL |
| `exam_operate_detail.update_time` | NULL → NOT NULL |
| `exam_paper.source` | NULL → NOT NULL |
| `exam_paper.status` | NULL → NOT NULL |
| `exam_paper.sort_number` | NULL → NOT NULL |
| `exam_paper.create_time` | NULL → NOT NULL |
| `exam_paper_detail.paper_id` | NOT NULL → NULL |
| `exam_paper_detail.status` | NULL → NOT NULL |
| `exam_paper_detail.sort_number` | NULL → NOT NULL |
| `exam_paper_detail.create_time` | NULL → NOT NULL |
| `exam_result.type` | NOT NULL → NULL |
| `exam_result.exam_plan_id` | NULL → NOT NULL |
| `exam_result.platform_user_id` | NULL → NOT NULL |
| `exam_result.user_answer` | NOT NULL → NULL |
| `exam_result.sort_number` | NULL → NOT NULL |
| `exam_result.create_time` | NULL → NOT NULL |
| `platform_sms.phone` | NOT NULL → NULL |
| `platform_sms.sms` | NOT NULL → NULL |
| `platform_sms.addtime` | NULL → NOT NULL |
| `platform_user_detail.gender` | NULL → NOT NULL |
| `platform_user_detail.deleted` | NOT NULL → NULL |
| `platform_user_token.token` | NOT NULL → NULL |
| `platform_user_token.addtime` | NULL → NOT NULL |
| `task_exam.expert_start` | NULL → NOT NULL |
| `task_exam.expert_end` | NULL → NOT NULL |
| `task_exam.tenant_id` | NOT NULL → NULL |
| `task_exam.deleted` | NOT NULL → NULL |
| `task_exam.create_time` | NOT NULL → NULL |
| `task_exam.updated_at` | NOT NULL → NULL |
| `task_exam.addtime` | NULL → NOT NULL |
| `task_exam_audit_records.tenant_id` | NOT NULL → NULL |
| `task_exam_audit_records.deleted` | NOT NULL → NULL |
| `task_exam_audit_records.create_time` | NOT NULL → NULL |
| `task_exam_audit_records.updated_at` | NOT NULL → NULL |
| `task_exam_audit_records.addtime` | NULL → NOT NULL |
| `task_exam_detail.tenant_id` | NOT NULL → NULL |
| `task_exam_detail.deleted` | NOT NULL → NULL |
| `task_exam_detail.create_time` | NOT NULL → NULL |
| `task_exam_detail.updated_at` | NOT NULL → NULL |
| `task_examshicao.expert_start` | NULL → NOT NULL |
| `task_examshicao.expert_end` | NULL → NOT NULL |
| `task_examshicao.tenant_id` | NOT NULL → NULL |
| `task_examshicao.deleted` | NOT NULL → NULL |
| `task_examshicao.create_time` | NOT NULL → NULL |
| `task_examshicao.updated_at` | NOT NULL → NULL |
| `task_examshicao.addtime` | NULL → NOT NULL |
| `task_examshicao_audit_records.tenant_id` | NOT NULL → NULL |
| `task_examshicao_audit_records.deleted` | NOT NULL → NULL |
| `task_examshicao_audit_records.create_time` | NOT NULL → NULL |
| `task_examshicao_audit_records.updated_at` | NOT NULL → NULL |
| `task_examshicao_audit_records.addtime` | NULL → NOT NULL |
| `task_examshicao_detail.tenant_id` | NOT NULL → NULL |
| `task_examshicao_detail.deleted` | NOT NULL → NULL |
| `task_examshicao_detail.create_time` | NOT NULL → NULL |
| `task_examshicao_detail.updated_at` | NOT NULL → NULL |
| `task_examshicao_detail.isblod` | NOT NULL → NULL |
| `task_examsub.expert_start` | NULL → NOT NULL |
| `task_examsub.expert_end` | NULL → NOT NULL |
| `task_examsub.tenant_id` | NOT NULL → NULL |
| `task_examsub.deleted` | NOT NULL → NULL |
| `task_examsub.create_time` | NOT NULL → NULL |
| `task_examsub.updated_at` | NOT NULL → NULL |
| `task_examsub.addtime` | NULL → NOT NULL |
| `task_examsub_audit_records.content` | NULL → NOT NULL |
| `task_examsub_audit_records.answer` | NULL → NOT NULL |
| `task_examsub_audit_records.tenant_id` | NOT NULL → NULL |
| `task_examsub_audit_records.deleted` | NOT NULL → NULL |
| `task_examsub_audit_records.create_time` | NOT NULL → NULL |
| `task_examsub_audit_records.updated_at` | NOT NULL → NULL |
| `task_examsub_audit_records.addtime` | NULL → NOT NULL |
| `task_examsub_detail.is_jisuan` | NOT NULL → NULL |
| `task_examsub_detail.tenant_id` | NOT NULL → NULL |
| `task_examsub_detail.deleted` | NOT NULL → NULL |
| `task_examsub_detail.create_time` | NOT NULL → NULL |
| `task_examsub_detail.updated_at` | NOT NULL → NULL |
| `task_examsub_detail.isblod` | NOT NULL → NULL |
| `task_expert.tenant_id` | NOT NULL → NULL |
| `task_expert.task_type` | NOT NULL → NULL |
| `task_expert.tag` | NOT NULL → NULL |
| `task_expert.addtime` | NULL → NOT NULL |
| `task_expert.endtime` | NULL → NOT NULL |
| `task_expert.created_at` | NOT NULL → NULL |
| `task_expert.updated_at` | NOT NULL → NULL |
| `task_expert.deleted` | NOT NULL → NULL |
| `task_ximu.expert_start` | NULL → NOT NULL |
| `task_ximu.expert_end` | NULL → NOT NULL |
| `task_ximu.file_path` | NULL → NOT NULL |
| `task_ximu.addtime` | NULL → NOT NULL |
| `task_ximu.tenant_id` | NOT NULL → NULL |
| `task_ximu.deleted` | NOT NULL → NULL |
| `task_ximu.create_time` | NOT NULL → NULL |
| `task_ximu.updated_at` | NOT NULL → NULL |
| `task_ximu_detail.sno` | NOT NULL → NULL |
| `task_ximu_detail.tenant_id` | NOT NULL → NULL |
| `task_ximu_detail.deleted` | NOT NULL → NULL |
| `task_ximu_detail.create_time` | NOT NULL → NULL |
| `task_ximu_detail.updated_at` | NOT NULL → NULL |
| `train_budget.status` | NULL → NOT NULL |
| `train_budget.user_id` | NULL → NOT NULL |
| `train_budget.create_time` | NULL → NOT NULL |
| `train_budget.update_time` | NULL → NOT NULL |
| `train_course.sort_number` | NULL → NOT NULL |
| `train_course.create_time` | NULL → NOT NULL |
| `train_exam_list.sort_number` | NULL → NOT NULL |
| `train_exam_list.jigou_id` | NULL → NOT NULL |
| `train_exam_list.create_time` | NULL → NOT NULL |
| `train_file_detail.status` | NULL → NOT NULL |
| `train_file_detail.sort_number` | NULL → NOT NULL |
| `train_file_detail.create_time` | NULL → NOT NULL |
| `train_file_detail.deleted` | NOT NULL → NULL |
| `train_file_list.status` | NULL → NOT NULL |
| `train_file_list.sort_number` | NULL → NOT NULL |
| `train_file_list.create_time` | NULL → NOT NULL |
| `train_file_list.deleted` | NOT NULL → NULL |
| `train_info_class.train_info_id` | NULL → NOT NULL |
| `train_info_class.title` | NULL → NOT NULL |
| `train_info_class.nums` | NULL → NOT NULL |
| `train_info_class.work_type_id` | NULL → NOT NULL |
| `train_info_class.addtime` | NULL → NOT NULL |
| `train_info_class.schedule_manual_edited` | NOT NULL → NULL |
| `train_info_class.company_confirm_status` | NOT NULL → NULL |
| `train_info_class.graduate_status` | NOT NULL → NULL |
| `train_info_list.work_type_level` | NULL → NOT NULL |
| `train_info_list.status` | NULL → NOT NULL |
| `train_info_list.sort_number` | NULL → NOT NULL |
| `train_info_list.create_time` | NULL → NOT NULL |
| `train_info_list.traindemand_id` | NULL → NOT NULL |
| `train_need.sort_number` | NULL → NOT NULL |
| `train_need.create_time` | NULL → NOT NULL |
| `train_request.sort_number` | NULL → NOT NULL |
| `train_request.create_time` | NULL → NOT NULL |
| `train_resource.sort_number` | NULL → NOT NULL |
| `train_resource.create_time` | NULL → NOT NULL |
| `train_sign_up_self.collection_source` | NOT NULL → NULL |
| `train_sign_up_selftask.tenant_id` | NOT NULL → NULL |
| `train_solution.train_info_id` | NULL → NOT NULL |
| `train_solution.status` | NULL → NOT NULL |
| `train_solution.sort_number` | NULL → NOT NULL |
| `train_solution.create_time` | NULL → NOT NULL |
| `train_solution.deleted` | NOT NULL → NULL |
| `train_talent_needs.sort_number` | NULL → NOT NULL |
| `train_talent_needs.create_time` | NULL → NOT NULL |
| `train_task_info.sort_number` | NULL → NOT NULL |
| `train_task_info.create_time` | NULL → NOT NULL |
| `train_task_info_detail.sort_number` | NULL → NOT NULL |
| `train_task_info_detail.create_time` | NULL → NOT NULL |
| `train_task_list.title` | NOT NULL → NULL |
| `train_task_list.sort_number` | NULL → NOT NULL |
| `train_task_list.create_time` | NULL → NOT NULL |
| `train_user_ability.platform_user_id` | NULL → NOT NULL |
| `train_user_ability.status` | NULL → NOT NULL |
| `train_user_ability.sort_number` | NULL → NOT NULL |
| `train_user_ability.create_time` | NULL → NOT NULL |
| `train_user_course_paper.platform_user_id` | NULL → NOT NULL |
| `train_user_course_paper.status` | NULL → NOT NULL |
| `train_user_course_paper.sort_number` | NULL → NOT NULL |
| `train_user_course_paper.create_time` | NULL → NOT NULL |
| `train_user_course_plan.platform_user_id` | NULL → NOT NULL |
| `train_user_course_plan.type` | NULL → NOT NULL |
| `train_user_course_plan.status` | NULL → NOT NULL |
| `train_user_course_plan.sort_number` | NULL → NOT NULL |
| `train_user_course_plan.create_time` | NULL → NOT NULL |
| `train_user_exam_log.sort_number` | NULL → NOT NULL |
| `train_user_exam_log.create_time` | NULL → NOT NULL |
| `train_user_exam_log.update_time` | NULL → NOT NULL |
| `train_user_info.sort_number` | NULL → NOT NULL |
| `train_user_info.create_time` | NULL → NOT NULL |
| `train_user_learn_log.sort_number` | NULL → NOT NULL |
| `train_user_learn_log.create_time` | NULL → NOT NULL |
| `train_user_paper.platform_user_id` | NULL → NOT NULL |
| `train_user_paper.sort_number` | NULL → NOT NULL |
| `train_user_paper.create_time` | NULL → NOT NULL |
| `train_user_patent.create_time` | NULL → NOT NULL |
| `train_user_patent.update_time` | NULL → NOT NULL |
| `train_user_patent.deleted` | NOT NULL → NULL |

### 仅左侧存在的索引

| 位置 | 说明 |
|---|---|
| `cms_article.idx_cms_article_category` | - |
| `cms_article.idx_cms_article_deleted` | - |
| `demand_employee.idx_demand_employee_tenant_org` | - |
| `demand_science.idx_demand_science_status` | - |
| `demand_science.idx_demand_science_tenant_org` | - |
| `exam_exam_detail.idx_ed_difficulty` | - |
| `exam_exam_detail.idx_ed_knowledge` | - |
| `exam_exam_detail.idx_ed_org` | - |
| `exam_exam_detail.idx_ed_status` | - |
| `exam_exam_detail.idx_ed_tenant` | - |
| `exam_exam_detail.idx_ed_tenant_knowledge_deleted` | - |
| `exam_exam_detail.idx_ed_tenant_sort_desc` | - |
| `exam_exam_detail.idx_ed_type` | - |
| `exam_knowledge_rate.idx_knowledge_rate_course` | - |
| `exam_knowledge_rate_detail.idx_rate_detail_rate` | - |
| `exam_knowledge_structure.idx_ks_course` | - |
| `exam_knowledge_structure.idx_ks_level` | - |
| `exam_knowledge_structure.idx_ks_parent` | - |
| `exam_knowledge_structure.idx_ks_tenant` | - |
| `exam_knowledge_structure.idx_ks_tenant_status_course` | - |
| `exam_make_exam_plan.idx_make_plan_course` | - |
| `exam_make_exam_plan.idx_make_plan_status` | - |
| `exam_make_exam_plan_detail.idx_plan_detail_make` | - |
| `exam_operate_detail.idx_operate_detail_course` | - |
| `exam_paper.idx_paper_make` | - |
| `exam_paper.idx_paper_status` | - |
| `exam_paper_detail.idx_paper_detail_paper` | - |
| `exam_result.idx_exam_result_exam` | - |
| `exam_result.idx_exam_result_tenant` | - |
| `exam_result.idx_exam_result_user` | - |
| `exam_work_type.idx_worktype_tenant_sort` | - |
| `platform_user_detail.idx_platform_user_detail_idcard` | - |
| `platform_user_token.idx_platform_user_token` | - |
| `platform_user_token.idx_platform_user_token_user` | - |
| `task_exam.idx_status` | - |
| `task_exam.idx_tenant_id` | - |
| `task_exam.idx_work_type_id` | - |
| `task_exam_audit_records.idx_exam_id` | - |
| `task_exam_audit_records.idx_expert_id` | - |
| `task_exam_audit_records.idx_tenant_id` | - |
| `task_exam_detail.idx_tenant_id` | - |
| `task_examshicao.idx_status` | - |
| `task_examshicao.idx_tenant_id` | - |
| `task_examshicao.idx_work_type_id` | - |
| `task_examshicao_audit_records.idx_exam_id` | - |
| `task_examshicao_audit_records.idx_expert_id` | - |
| `task_examshicao_audit_records.idx_task_id` | - |
| `task_examshicao_audit_records.idx_tenant_id` | - |
| `task_examshicao_detail.idx_task_id` | - |
| `task_examshicao_detail.idx_tenant_id` | - |
| `task_examsub.idx_status` | - |
| `task_examsub.idx_tenant_id` | - |
| `task_examsub.idx_work_type_id` | - |
| `task_examsub_audit_records.idx_exam_id` | - |
| `task_examsub_audit_records.idx_expert_id` | - |
| `task_examsub_audit_records.idx_task_id` | - |
| `task_examsub_audit_records.idx_tenant_id` | - |
| `task_examsub_detail.idx_task_id` | - |
| `task_examsub_detail.idx_tenant_id` | - |
| `task_expert.idx_expert` | - |
| `task_expert.idx_task` | - |
| `task_expert.uk_exam_task_expert` | - |
| `task_ximu.idx_status` | - |
| `task_ximu.idx_tenant_id` | - |
| `task_ximu.idx_work_type_id` | - |
| `task_ximu_detail.idx_task_id` | - |
| `task_ximu_detail.idx_tenant_id` | - |
| `train_budget.idx_train_budget_tenant` | - |
| `train_budget.idx_train_budget_train` | - |
| `train_course.idx_train_course_tenant` | - |
| `train_course.idx_train_course_train` | - |
| `train_exam_list.idx_train_exam_tenant` | - |
| `train_exam_list.idx_train_exam_train` | - |
| `train_file_detail.idx_train_file_detail_file` | - |
| `train_file_detail.idx_train_file_detail_tenant` | - |
| `train_file_list.idx_train_file_list_tenant` | - |
| `train_info_class.idx_train_info_class_plan` | - |
| `train_info_class.idx_train_info_class_train` | - |
| `train_info_list.idx_til_plan` | - |
| `train_info_list.idx_train_info_list_status` | - |
| `train_info_list.idx_train_info_list_tenant` | - |
| `train_need.idx_train_need_org` | - |
| `train_need.idx_train_need_tenant` | - |
| `train_request.idx_train_request_tenant` | - |
| `train_resource.idx_train_resource_tenant` | - |
| `train_sign_up_self.idx_sign_up_research` | - |
| `train_sign_up_self.uk_sign_up_tenant_class` | - |
| `train_solution.idx_train_solution_status` | - |
| `train_solution.idx_train_solution_tenant` | - |
| `train_talent_needs.idx_train_talent_needs_org` | - |
| `train_talent_needs.idx_train_talent_needs_tenant` | - |
| `train_task_info.idx_train_task_info_tenant` | - |
| `train_task_info.idx_train_task_info_train` | - |
| `train_task_info_detail.idx_train_task_info_detail_tenant` | - |
| `train_task_info_detail.idx_train_task_info_detail_train` | - |
| `train_task_list.idx_train_task_course` | - |
| `train_task_list.idx_train_task_tenant` | - |
| `train_task_list.idx_train_task_work_type` | - |
| `train_user_ability.idx_train_user_ability_tenant` | - |
| `train_user_ability.idx_train_user_ability_user` | - |
| `train_user_course_paper.idx_train_user_course_paper_tenant` | - |
| `train_user_course_paper.idx_train_user_course_paper_user` | - |
| `train_user_course_plan.idx_train_user_course_plan_tenant` | - |
| `train_user_course_plan.idx_train_user_course_plan_user` | - |
| `train_user_exam_log.idx_train_user_exam_log_tenant` | - |
| `train_user_exam_log.idx_train_user_exam_log_user` | - |
| `train_user_info.idx_train_user_info_tenant` | - |
| `train_user_info.idx_train_user_info_user` | - |
| `train_user_learn_log.idx_train_user_learn_log_tenant` | - |
| `train_user_learn_log.idx_train_user_learn_log_train` | - |
| `train_user_learn_log.idx_train_user_learn_log_user` | - |
| `train_user_paper.idx_train_user_paper_tenant` | - |
| `train_user_paper.idx_train_user_paper_user` | - |
| `train_user_patent.idx_train_user_patent_tenant` | - |
| `train_user_patent.idx_train_user_patent_user` | - |

### 仅右侧存在的索引

| 位置 | 说明 |
|---|---|
| `cms_article.category_id` | - |
| `cms_article.tenant_id` | - |
| `cms_article_category.parent_id` | - |
| `cms_article_category.tenant_id` | - |
| `cms_article_content.article_id` | - |
| `cms_article_content.tenant_id` | - |
| `exam_exam_detail.check_status` | - |
| `exam_exam_detail.check_user_id` | - |
| `exam_exam_detail.deleted` | - |
| `exam_exam_detail.difficulty` | - |
| `exam_exam_detail.exam_id` | - |
| `exam_exam_detail.final_check_status` | - |
| `exam_exam_detail.final_check_user_id` | - |
| `exam_exam_detail.id` | - |
| `exam_exam_detail.knowledge_id` | - |
| `exam_exam_detail.org_id` | - |
| `exam_exam_detail.org_name` | - |
| `exam_exam_detail.source` | - |
| `exam_exam_detail.status` | - |
| `exam_exam_detail.task_detail_id` | - |
| `exam_exam_detail.type` | - |
| `exam_knowledge_structure.course_id` | - |
| `exam_knowledge_structure.create_time` | - |
| `exam_knowledge_structure.deleted` | - |
| `exam_knowledge_structure.id` | - |
| `exam_knowledge_structure.level` | - |
| `exam_knowledge_structure.org_id` | - |
| `exam_knowledge_structure.parent_id` | - |
| `exam_knowledge_structure.series_no` | - |
| `exam_knowledge_structure.sort_number` | - |
| `exam_knowledge_structure.status` | - |
| `exam_knowledge_structure.tenant_id` | - |
| `exam_knowledge_structure.title` | - |
| `exam_knowledge_structure.type` | - |
| `exam_paper.id` | - |
| `exam_paper.source` | - |
| `exam_paper.title` | - |
| `exam_paper.type` | - |
| `exam_paper_detail.create_time` | - |
| `exam_paper_detail.diff` | - |
| `exam_paper_detail.exam_id` | - |
| `exam_paper_detail.id` | - |
| `exam_paper_detail.paper_id` | - |
| `exam_paper_detail.sort_number` | - |
| `exam_paper_detail.status` | - |
| `exam_paper_detail.tenant_id` | - |
| `exam_paper_detail.type` | - |
| `exam_result.create_time` | - |
| `exam_result.deleted` | - |
| `exam_result.detail_id` | - |
| `exam_result.exam_id` | - |
| `exam_result.exam_type` | - |
| `exam_result.id` | - |
| `exam_result.paper_id` | - |
| `exam_result.sort_number` | - |
| `exam_result.tenant_id` | - |
| `exam_result.type` | - |
| `exam_result.user_id` | - |
| `platform_user.idx_device_id` | - |
| `task_exam.idx_task_exam_wt_lvl_arch` | - |
| `task_exam_audit_records.idx_expert_role_status` | - |
| `task_examshicao.idx_task_examshicao_wt_lvl_arch` | - |
| `task_examsub.idx_task_examsub_wt_lvl_arch` | - |
| `task_ximu.idx_task_ximu_wt_lvl_type_arch` | - |
| `train_info_list.course_id` | - |
| `train_info_list.deleted` | - |
| `train_info_list.id` | - |
| `train_info_list.pass_status` | - |
| `train_info_list.status` | - |
| `train_info_list.work_type_id` | - |
| `train_sign_up_selftask.uk_sign_up_self_task_running` | - |

### 索引列构成变化

| 位置 | 说明 |
|---|---|
| `cms_article_content.PRIMARY` | article_id → id |
| `train_exam_list.PRIMARY` | id → id,org_id |

