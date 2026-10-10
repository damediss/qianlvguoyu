# 千铝国昱云平台 / 公网测试环境部署文档

> **目标:** 把当前 dev 环境的 Spring Boot 后端 + Vite 前端部署到腾讯云 Ubuntu 服务器,公网 IP 访问,方便大家测试。
> **架构:** 单机部署 —— MySQL / Redis / Spring Boot / Vite dist / nginx 全在一台云主机上,文件存腾讯云 COS。
> **准备清单:**
>
> | 必需 | 说明 |
> |---|---|
> | 腾讯云 CVM 1 台 | Ubuntu 22.04+,1C2G 起步(2C4G 推荐),公网 IP |
> | 域名(可选) | 当前先用 IP,后续可绑已备案域名(你有) |
> | COS 凭证 | 1 个子账号 + COS 读写权限 → SecretId/SecretKey |
> | SSH 访问 | sudo 权限 |

---

## 0. 准备 4 个值(部署前给我)

| 变量 | 例子 | 用在哪里 |
|---|---|---|
| 公网 IP | `1.2.3.4` | CORS 白名单、登录页面访问入口 |
| COS SecretId | `AKIDxxx` | `COS_SECRET_ID` |
| COS SecretKey | `xxx` | `COS_SECRET_KEY` |
| COS Bucket + Region | `guoyu-test-123` / `ap-guangzhou` | `COS_BUCKET` / `COS_REGION` |

> COS 凭证创建路径:腾讯云控制台 → 访问管理 → 用户 → 用户列表 → 新建用户 → 自定义创建 → 策略关联 `QcloudCOSFullAccess`(或更严格的 `QcloudCOSBucketReadWrite` 只授权本 bucket)→ 完成创建 → 复制 SecretId/SecretKey。

---

## 1. 上传部署资产

在你**本地**这台 Windows 上:

```powershell
# 把整个 deploy 目录拷到云服务器
scp -r D:\workspace\qianlvguoyu_saas\deploy ubuntu@<PUBLIC_IP>:/tmp/guoyu-deploy
scp -r D:\workspace\qianlvguoyu_saas ubuntu@<PUBLIC_IP>:/tmp/guoyu-repo
```

> 也可以先 `git clone` 到云上,只 scp `deploy/` 目录。

## 2. 在云服务器上一键部署

```bash
ssh ubuntu@<PUBLIC_IP>
cd /tmp/guoyu-repo
sudo bash /tmp/guoyu-deploy/deploy/deploy-public.sh
```

脚本会做:
1. 安装 JDK 21 / Maven / Nginx / MySQL 8 / Redis / `fonts-wqy-zenhei` 中文 TrueType 字体
2. 创建 `guoyu` 系统用户 + `/data/guoyu` 目录结构
3. 初始化 MySQL `gjjd_test` 库 + 应用账号
4. 给 Redis 设密码
5. `mvn package` 构建 jar + `npm run build` 构建前端
6. 生成 `/data/guoyu/.env`(随机 JWT / 随机密码 / 公网 IP 自动注入)，并配置 `COLLECTION_PDF_FONT_PATH`
7. 部署 systemd + nginx + 启动 + 冒烟

## 3. 补 COS 凭证

脚本生成的 `.env` 里 `COS_SECRET_ID/SECRET_KEY/BUCKET` 都是 `CHANGE_ME_xxx` 占位:

```bash
sudo nano /data/guoyu/.env
```

把 4 个 CHANGE_ME 改成实际值,保存后:

```bash
sudo systemctl restart platform-api
sudo journalctl -u platform-api -f   # 看启动日志
```

## 4. 验收清单

> 假设公网 IP 为 `43.138.163.7`(替换为你自己的)

```bash
# 4.1 nginx 直接返 200
curl -sI http://43.138.163.7/
# 期望:HTTP/1.1 200 OK,Content-Type: text/html

# 4.2 后端 health
curl -s http://43.138.163.7/api/actuator/health
# 期望:{"status":"UP"}

# 4.3 登录(看能否跨域)
curl -s -X POST http://43.138.163.7/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"tenantCode":"demo","username":"admin","password":"Admin@123456"}'
# 期望:{"code":"0","data":{"accessToken":"...","tenantId":1,...}}

# 4.4 浏览器打开 http://43.138.163.7/
# 登录页能渲染,登录后菜单能加载,列表能查
```

## 5. 关键配置项

| 文件 | 关键变量 | 说明 |
|---|---|---|
| `/data/guoyu/.env` | `MYSQL_URL/PASSWORD` | MySQL 连接,改完需 `systemctl restart` |
| `/data/guoyu/.env` | `REDIS_HOST/PASSWORD` | Redis 连接,Sa-Token 共享 |
| `/data/guoyu/.env` | `JWT_SECRET` | 至少 32 字符,生产必须重生成 |
| `/data/guoyu/.env` | `CORS_ALLOWED_ORIGINS` | 浏览器跨域白名单,逗号分隔,不能用 `*` |
| `/data/guoyu/.env` | `FILE_STORAGE_PROVIDER` | `cos` / `local` |
| `/data/guoyu/.env` | `COLLECTION_PDF_FONT_PATH` | `/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc`，必须是可读的 TrueType `.ttf` 或 `.ttc` |
| `/data/guoyu/.env` | `COS_*` | COS 凭证 + bucket + region |
| `/etc/nginx/sites-enabled/guoyu` | 80 监听 | 改完 `sudo nginx -t && sudo systemctl reload nginx` |
| `/etc/systemd/system/platform-api.service` | JVM 参数 / 重启策略 | 一般不用改 |

## 6. 改完代码重新部署

```bash
# 本地
git pull && cd services/platform-api && mvn package -DskipTests
cd ../../apps/admin-web && npm run build
scp services/platform-api/target/platform-api-0.1.0-SNAPSHOT.jar ubuntu@<PUBLIC_IP>:/data/guoyu/
scp -r apps/admin-web/dist/* ubuntu@<PUBLIC_IP>:/data/guoyu/web/

# 云服务器
ssh ubuntu@<PUBLIC_IP> 'sudo systemctl restart platform-api'
```

## 7. 切域名(等备案后)

1. 腾讯云控制台 → 域名解析 → 添加 A 记录 `your.domain → <PUBLIC_IP>`
2. 申请免费 SSL 证书(DV),或 `certbot --nginx` 申请 Let's Encrypt
3. nginx 加 443 server 块(参考 deploy/nginx-ssl.conf.example,待补)
4. `.env` 里 `CORS_ALLOWED_ORIGINS` 改成 `https://your.domain`
5. 重启后端

## 8. 故障排查

| 现象 | 检查 |
|---|---|
| 502 Bad Gateway | 后端没起,`journalctl -u platform-api -n 50` 看 |
| CORS 报错 | `.env` 里 `CORS_ALLOWED_ORIGINS` 没填对,或者来源不在白名单 |
| 登录 401 | MySQL 没起来 / 密码错;`mysql -u guoyu_app -p` 验证 |
| 登录后菜单 403 | Redis 没起 / 密码错;`redis-cli -a <pwd> ping` 验证 |
| 文件上传 500 | `client_max_body_size` 是否 ≥ 50M;COS 凭证是否对;`journalctl -u platform-api` 看堆栈 |
| 打印版 PDF 生成失败 | `test -r /usr/share/fonts/truetype/wqy/wqy-zenhei.ttc`;检查 `.env` 的 `COLLECTION_PDF_FONT_PATH`;不要使用 CFF `.otf` |
| 前端白屏 | 浏览器 F12 看 console,大概率是 JS 404;`/data/guoyu/web/` 下 dist 拷齐了吗 |
| Flyway 报错 | MySQL 版本必须 8.4(脚本装的 8.0 也行,但 Flyway 会 warn 兼容),字符集 utf8mb4 |

## 9. 备份与恢复

```bash
# 备份 MySQL(每天 cron)
mysqldump -u root gjjd_test | gzip > /data/guoyu/backup/gjjd_test_$(date +%F).sql.gz

# 备份 local 文件(如用 local provider)
tar czf /data/guoyu/backup/files_$(date +%F).tgz -C /data/guoyu/storage files

# COS 文件存储无需备份(腾讯云 11 个 9 持久性)
```

## 10. 不要做的事

- ❌ 不要把 dev 数据库直接暴露到公网(本方案已经自建新库)
- ❌ 不要把 `.env` commit 到 git(`.gitignore` 已加,确认下)
- ❌ 不要用 root 跑后端进程(本方案用 `guoyu` 系统用户)
- ❌ 不要在 nginx 配 `cors *`(AllowCredentials=true 时会被浏览器拒绝)
- ❌ 不要用 `vite dev` 服务前端(本方案用 `vite build` + nginx)
- ❌ 不要把 Sa-Token 默认 3 天过期调太长(7 天以上需要 refresh token 配合)
