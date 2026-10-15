-- 查证证书图片：上游响应 licensefile 为 base64 图片，解码后存平台文件存储（COS），
-- 本列只存文件 ID，前端经 /api/files/{id}/download 鉴权预览（不在库里存文件内容）。
--
-- 取号：2026-10-14 已查两库 MAX(version) = 测试库 20261009.003 / 生产库 20261009.003，
--       共用目录接 V20261014_002 之后取 V20261014_003（撞号会导致 jar 全环境无法启动）。

ALTER TABLE chazheng_cert
  ADD COLUMN license_file_id VARCHAR(64) NULL COMMENT '证书图片文件ID(FileApplicationService)' AFTER cert_date;
