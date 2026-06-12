# 生产部署说明

## 1. 上线前必须处理

1. 在萤石、DeepSeek、MiMo 控制台轮换当前开发密钥。历史提交中出现过萤石密钥，不能继续用于公网环境。
2. 准备一个已备案/可解析的域名和 HTTPS 证书。建议只开放 `80/443`，不要开放 MySQL `3306` 和后端 `8080`。
3. 服务器至少准备 Docker Engine、Docker Compose、2 GB 内存和持久化磁盘。
4. 复制 `.env.example` 为 `.env`，使用密码学随机值填写所有生产必填项：

```bash
cp .env.example .env
openssl rand -base64 64
```

回调地址必须使用同一个公网域名：

```dotenv
EZVIZ_REDIRECT_URI=https://pet.example.com/api/ezviz/oauth/callback
EZVIZ_FRONTEND_URL=https://pet.example.com
EZVIZ_WEBHOOK_ENABLED=true
EZVIZ_WEBHOOK_SECRET=使用随机字符串并与萤石控制台保持一致
```

## 2. 启动应用

生产编排不会导入默认 `admin/123456`，也不会把数据库和后端端口暴露到公网。

```bash
docker compose -f docker-compose.prod.yml config
docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f backend
```

首次部署可将 `.env` 中的 `REGISTRATION_ENABLED` 临时设为 `true`，注册答辩账号后立即改回 `false` 并重启后端：

```bash
docker compose -f docker-compose.prod.yml up -d backend
```

应用只监听宿主机 `127.0.0.1:8088`，需要由宿主机 Nginx 或 Caddy 提供 HTTPS。

Nginx 示例：

```nginx
server {
    listen 80;
    server_name pet.example.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name pet.example.com;

    ssl_certificate /etc/letsencrypt/live/pet.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/pet.example.com/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:8088;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_buffering off;
        proxy_read_timeout 1800s;
    }
}
```

## 3. 配置萤石回调

项目包含的是“设备托管 OAuth 授权回调”：

```text
https://pet.example.com/api/ezviz/oauth/callback
```

在萤石开放平台应用配置中填写完全一致的地址。授权完成后，后端使用 `auth_code` 换取 token，再跳转到：

```text
https://pet.example.com/oauth/ezviz/callback
```

实时告警使用另一条 Webhook：

```text
https://pet.example.com/api/ezviz/webhook
```

在萤石开放平台控制台的“云信令 > 消息推送”中：

1. Webhook 地址填写上述 HTTPS 地址。
2. 消息类型至少勾选 `ys.alarm`。
3. 设置一个随机签名密钥，并与 `.env` 的 `EZVIZ_WEBHOOK_SECRET` 完全一致。
4. 失败重试次数建议设置为 `3`。
5. 将 `EZVIZ_WEBHOOK_ENABLED` 设置为 `true` 并重启后端。

如果数据库是在本次改动前创建的，先执行一次幂等索引迁移：

```bash
docker compose -f docker-compose.prod.yml exec -T mysql \
  sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' \
  < backend/src/main/resources/sql/migration-webhook.sql
```

项目会验证请求头 `t` 和 `signature`。验签算法为
`HMAC-SHA1(原始HTTP请求体 + t, EZVIZ_WEBHOOK_SECRET)`，并拒绝超过 10 分钟的请求。
消息按 `messageId` 去重，只有本地已同步且存在有效用户绑定的设备才会入库；入库后立即通过 SSE 推送给该设备的绑定用户。

可以在服务器本地模拟一次签名推送：

```bash
SECRET='替换为EZVIZ_WEBHOOK_SECRET'
T=$(($(date +%s) * 1000))
BODY='{"header":{"messageId":"local-test-1","messageTime":'"$T"',"type":"ys.alarm","deviceId":"替换为已绑定设备序列号","channelNo":1},"body":{"data":"{\"alarmType\":\"motiondetect\",\"alarmName\":\"Webhook测试告警\"}"}}'
SIGNATURE=$(printf '%s' "${BODY}${T}" | openssl dgst -sha1 -hmac "$SECRET" | awk '{print $2}')

curl -i http://127.0.0.1:8088/api/ezviz/webhook \
  -H 'Content-Type: text/plain' \
  -H "t: $T" \
  -H "signature: $SIGNATURE" \
  -H 'message_type: ys.alarm' \
  --data-binary "$BODY"
```

正确响应为：

```json
{"messageId":"local-test-1"}
```

先保持 `ALARM_SYNC_ENABLED=true`，确认真实推送连续稳定后可改为 `false`，关闭每 60 秒一次的萤石告警轮询。该开关不会关闭宠物检测、异常分析和日报任务。萤石控制台开通消息推送可能需要约两小时生效。

## 4. 上线验收

```bash
curl -I https://pet.example.com/
curl https://pet.example.com/actuator/health
curl https://pet.example.com/api/ezviz/oauth/callback
```

检查项：

- `/api/ezviz/oauth/callback` 无参数访问返回 `{"code":"200"}`。
- Swagger 在生产 profile 下不可访问。
- 公网无法连接服务器的 `3306` 和 `8080`。
- 新注册测试用户只能看到自己绑定的设备、告警、检测配置和 AI 报告。
- OAuth 授权后能回到设备绑定页，并能同步设备、查看直播。
- 萤石控制台测试推送返回成功，数据库产生一条告警，浏览器在线时立即收到 SSE 提醒。
- 重复推送同一个 `messageId` 不会生成第二条告警。
- 重启容器后 MySQL 数据仍保留。
- 配置每日数据库备份，并实际做一次恢复演练。
