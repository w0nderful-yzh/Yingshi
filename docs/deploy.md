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

“萤石消息推送 Webhook”是另一套能力。当前项目尚未实现该接收接口，告警仍通过 `AlarmSyncTask` 每 60 秒轮询。若答辩要求实时回调，还需要新增独立的 Webhook Controller、签名验签、消息幂等和设备归属校验，再到萤石控制台“云信令/消息推送”配置地址和签名密钥。

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
- 重启容器后 MySQL 数据仍保留。
- 配置每日数据库备份，并实际做一次恢复演练。
