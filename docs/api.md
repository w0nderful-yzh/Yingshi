# API 接口文档

完整接口清单、请求参数、响应示例和错误码统一维护在仓库根目录的
[API-DOC.md](../API-DOC.md)。

## 在线调试

- 本地后端地址：`http://localhost:8080`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

生产 profile 默认关闭 Swagger 和 OpenAPI 文档，避免暴露接口结构。

## 认证约定

除登录、注册和萤石平台回调外，业务接口均需携带 JWT：

```http
Authorization: Bearer <token>
```

接口统一返回 `ApiResponse<T>`，包含 `code`、`message`、`data`、
`requestId` 和 `timestamp`。角色写权限与用户设备数据范围由后端统一校验。

## 维护规则

Controller 路由是接口行为的最终依据。新增或修改接口时，应同步更新
[API-DOC.md](../API-DOC.md)，并通过 Swagger 检查实际请求模型。
