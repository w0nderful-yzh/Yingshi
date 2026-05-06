# 影视监控系统 - 前端接口文档

> Base URL: `http://localhost:8080`
> 认证方式: `Authorization: Bearer <token>`
> 所有接口统一返回 `ApiResponse<T>` 结构

## 统一响应格式

```json
{
  "code": 0,          // 0=成功, 其他为错误码
  "message": "success",
  "data": {},
  "requestId": "uuid",
  "timestamp": "2026-05-05T10:00:00+08:00"
}
```

### 错误码

| code | 含义 |
|------|------|
| 0 | 成功 |
| 40001 | 参数校验失败 |
| 40004 | 资源不存在 |
| 40009 | 状态冲突 |
| 40100 | 未认证/token无效 |
| 40300 | 无权限 |
| 50000 | 服务器内部错误 |
| 50010 | AI模型服务异常 |

---

## 一、认证模块 `/api/auth`

### 1. 注册 `POST /api/auth/register`

**无需认证**

请求体:
```json
{
  "username": "string",  // 必填, 3-50位
  "password": "string",  // 必填, 6-100位
  "nickname": "string"   // 可选
}
```

响应 `data`:
```json
{
  "token": "eyJhbG...",
  "tokenType": "Bearer",
  "expiresIn": 7200,
  "userId": 1,
  "username": "test",
  "role": "user"
}
```

### 2. 登录 `POST /api/auth/login`

**无需认证**

请求体:
```json
{
  "username": "string",  // 必填
  "password": "string"   // 必填
}
```

响应 `data`: 同注册接口, 返回 `AuthLoginVO`

### 3. 获取当前用户 `GET /api/auth/me`

**需认证**

响应 `data`:
```json
{
  "id": 1,
  "username": "test",
  "nickname": "测试用户",
  "role": "user"
}
```

### 4. 登出 `POST /api/auth/logout`

**需认证** | 无请求参数 | 响应 `data`: `null`

> 提示: 登出为前端清除token即可, 此接口仅做预留

---

## 二、设备管理 `/api/devices`

### 5. 同步设备 `POST /api/devices/sync`

从萤石云平台同步设备列表到本地数据库

响应 `data`:
```json
{
  "total": 10,
  "inserted": 3,
  "updated": 7,
  "message": "同步成功"
}
```

### 6. 设备列表 `GET /api/devices`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | String | 否 | 设备状态筛选 |
| sourceType | String | 否 | 来源类型筛选 |
| keyword | String | 否 | 搜索关键词(设备名/序列号) |

响应 `data`: `DeviceVO[]`
```json
{
  "id": 1,
  "deviceSerial": "D12345678",
  "channelNo": 1,
  "deviceName": "客厅摄像头",
  "deviceType": "IPC",
  "sourceType": "EZVIZ",
  "status": "ONLINE",
  "remark": "",
  "createdAt": "2026-05-01T10:00:00",
  "updatedAt": "2026-05-01T10:00:00"
}
```

### 7. 设备详情 `GET /api/devices/{id}`

响应 `data`: `DeviceVO` (同上)

### 8. 更新设备 `PUT /api/devices/{id}`

请求体:
```json
{
  "deviceName": "string",  // 必填, 最长100
  "remark": "string",      // 可选, 最长255
  "status": "string"       // 可选
}
```

响应 `data`: `DeviceVO`

### 9. 禁用设备 `PUT /api/devices/{id}/disable`

响应 `data`: `null`

### 10. 启用设备 `PUT /api/devices/{id}/enable`

响应 `data`: `null`

### 11. 删除设备 `DELETE /api/devices/{id}`

响应 `data`: `null`

---

## 三、视频服务 `/api/video`

### 12. 获取直播地址 `GET /api/video/live-url`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| deviceId | Long | **是** | 设备ID |
| protocol | Integer | 否 | 协议类型 |
| quality | Integer | 否 | 清晰度 |
| expireTime | Integer | 否 | 过期时间(秒) |

响应 `data`:
```json
{
  "deviceId": 1,
  "deviceSerial": "D12345678",
  "channelNo": 1,
  "protocol": 1,
  "quality": 1,
  "url": "rtmp://...",
  "expireTime": "2026-05-05T12:00:00"
}
```

### 13. 云录像文件列表 `GET /api/video/cloud/records`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| deviceId | Long | **是** | 设备ID |
| startTime | String | **是** | 开始时间 |
| endTime | String | **是** | 结束时间 |

响应 `data`:
```json
[{
  "deviceId": 1,
  "deviceSerial": "D12345678",
  "channelNo": 1,
  "startTime": "2026-05-05 08:00:00",
  "endTime": "2026-05-05 09:00:00",
  "recordType": "timeRecord",
  "fileType": "mp4",
  "source": "CLOUD"
}]
```

### 14. 云录像回放地址 `GET /api/video/cloud/playback-url`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| deviceId | Long | **是** | 设备ID |
| startTime | String | **是** | 开始时间 |
| endTime | String | **是** | 结束时间 |
| protocol | Integer | 否 | 协议类型 |
| quality | Integer | 否 | 清晰度 |
| expireTime | Integer | 否 | 过期时间(秒) |

响应 `data`:
```json
{
  "deviceId": 1,
  "deviceSerial": "D12345678",
  "channelNo": 1,
  "protocol": 1,
  "quality": 1,
  "startTime": "2026-05-05 08:00:00",
  "endTime": "2026-05-05 09:00:00",
  "url": "https://...",
  "expireTime": "2026-05-05T12:00:00"
}
```

---

## 四、告警管理 `/api/alarms`

### 15. 同步告警 `POST /api/alarms/sync`

从萤石云拉取所有设备的告警消息并入库

响应 `data`:
```json
{
  "deviceCount": 5,
  "fetchedCount": 50,
  "insertedCount": 12,
  "message": "同步完成"
}
```

### 16. 告警列表 `GET /api/alarms`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| deviceId | Long | 否 | 按设备筛选 |
| readStatus | Integer | 否 | 已读状态(0=未读, 1=已读) |
| startTime | String | 否 | 开始时间 |
| endTime | String | 否 | 结束时间 |
| keyword | String | 否 | 搜索关键词 |

响应 `data`: `AlarmMessageVO[]`
```json
{
  "id": 1,
  "deviceId": 1,
  "deviceSerial": "D12345678",
  "deviceName": "客厅摄像头",
  "channelNo": 1,
  "alarmType": "pir",
  "alarmName": "移动侦测",
  "alarmTime": "2026-05-05T08:30:00",
  "alarmPicUrl": "https://...",
  "alarmContent": "检测到移动",
  "readStatus": 0,
  "source": "EZVIZ",
  "createdAt": "2026-05-05T08:30:05"
}
```

### 17. 未读数量 `GET /api/alarms/unread-count`

响应 `data`:
```json
{ "count": 5 }
```

### 18. 标记已读 `PUT /api/alarms/{id}/read`

响应 `data`: `null`

### 19. 全部已读 `PUT /api/alarms/read-all`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| deviceId | Long | 否 | 指定设备全部已读, 不传则全部标记 |

响应 `data`: `null`

### 20. 删除告警 `DELETE /api/alarms/{id}`

响应 `data`: `null`

---

## 五、宠物管理 `/api/pets`

### 21. 添加宠物 `POST /api/pets`

请求体:
```json
{
  "petName": "string",   // 必填
  "petType": "string",   // 必填 (如: cat / dog)
  "age": 1,              // 可选
  "gender": "string",    // 可选
  "remark": "string",    // 可选
  "avatarUrl": "string"  // 可选
}
```

响应 `data`:
```json
{
  "id": 1,
  "petName": "小橘",
  "petType": "cat",
  "age": 2,
  "gender": "male",
  "remark": "",
  "avatarUrl": "",
  "createdAt": "2026-05-05T10:00:00"
}
```

### 22. 更新宠物 `PUT /api/pets/{id}`

请求体同添加宠物, 响应 `data`: `PetVO`

### 23. 删除宠物 `DELETE /api/pets/{id}`

响应 `data`: `null`

### 24. 宠物详情 `GET /api/pets/{id}`

响应 `data`: `PetVO`

### 25. 宠物列表 `GET /api/pets`

响应 `data`: `PetVO[]`

---

## 六、宠物检测 `/api/pet-detection`

### 检测配置

### 26. 创建检测配置 `POST /api/pet-detection/configs`

请求体:
```json
{
  "petId": 1,                         // 必填, 宠物ID
  "deviceId": 1,                      // 必填, 设备ID
  "enabled": true,                    // 可选, 默认true
  "cooldownSeconds": 300,             // 可选, 告警冷却秒数, 默认300
  "remark": "string",                 // 可选
  "petAbsentMinutes": 60,             // 可选, 长时间未出现阈值(分钟), 默认60
  "activityWindowMinutes": 10,        // 可选, 异常活跃时间窗口(分钟), 默认10
  "activityCountThreshold": 5,        // 可选, 窗口内触发次数阈值, 默认5
  "stillnessMinutes": 30              // 可选, 长时间静止阈值(分钟), 默认30
}
```

响应 `data`:
```json
{
  "id": 1,
  "userId": 1,
  "petId": 1,
  "petName": "小橘",
  "deviceId": 1,
  "deviceName": "客厅摄像头",
  "deviceSerial": "D12345678",
  "enabled": 1,
  "cooldownSeconds": 300,
  "remark": "",
  "petAbsentMinutes": 60,
  "activityWindowMinutes": 10,
  "activityCountThreshold": 5,
  "stillnessMinutes": 30,
  "safeZones": [],
  "createdAt": "2026-05-05T10:00:00",
  "updatedAt": "2026-05-05T10:00:00"
}
```

### 27. 更新检测配置 `PUT /api/pet-detection/configs/{id}`

请求体同创建, 响应 `data`: `PetDetectionConfigVO`

### 28. 删除检测配置 `DELETE /api/pet-detection/configs/{id}`

响应 `data`: `null`

### 29. 检测配置详情 `GET /api/pet-detection/configs/{id}`

响应 `data`: `PetDetectionConfigVO` (含嵌套 `safeZones`)

### 30. 检测配置列表 `GET /api/pet-detection/configs`

响应 `data`: `PetDetectionConfigVO[]`

---

### 安全区域

### 31. 创建安全区域 `POST /api/pet-detection/zones`

请求体 - 矩形:
```json
{
  "detectionConfigId": 1,      // 必填
  "zoneName": "客厅区域",      // 可选
  "zoneType": "RECTANGLE",     // 必填, RECTANGLE 或 POLYGON
  "rectLeft": 10.0,            // 左上角X (百分比 0-100)
  "rectTop": 10.0,             // 左上角Y (百分比 0-100)
  "rectRight": 80.0,           // 右下角X (百分比 0-100)
  "rectBottom": 80.0           // 右下角Y (百分比 0-100)
}
```

请求体 - 多边形:
```json
{
  "detectionConfigId": 1,
  "zoneName": "自定义区域",
  "zoneType": "POLYGON",
  "polygonPoints": [
    {"x": 10.0, "y": 10.0},
    {"x": 80.0, "y": 10.0},
    {"x": 80.0, "y": 80.0},
    {"x": 10.0, "y": 80.0}
  ]
}
```

响应 `data`:
```json
{
  "id": 1,
  "detectionConfigId": 1,
  "zoneName": "客厅区域",
  "zoneType": "RECTANGLE",
  "rectLeft": 10.0,
  "rectTop": 10.0,
  "rectRight": 80.0,
  "rectBottom": 80.0,
  "polygonPoints": null,
  "createdAt": "2026-05-05T10:00:00"
}
```

> 提示: 坐标均为百分比(0-100), 表示在视频画面中的相对位置

### 32. 更新安全区域 `PUT /api/pet-detection/zones/{id}`

请求体同创建, 响应 `data`: `PetSafeZoneVO`

### 33. 删除安全区域 `DELETE /api/pet-detection/zones/{id}`

响应 `data`: `null`

### 34. 安全区域列表 `GET /api/pet-detection/zones`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| detectionConfigId | Long | **是** | 检测配置ID |

响应 `data`: `PetSafeZoneVO[]`

---

### 检测记录

### 35. 检测记录列表 `GET /api/pet-detection/records`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| detectionConfigId | Long | 否 | 检测配置ID |
| petId | Long | 否 | 宠物ID |
| deviceId | Long | 否 | 设备ID |
| alarmTriggered | Integer | 否 | 1=仅查触发告警的记录 |
| startTime | String | 否 | 开始时间 |
| endTime | String | 否 | 结束时间 |

响应 `data`: `PetDetectionRecordVO[]`
```json
{
  "id": 1,
  "detectionConfigId": 1,
  "petId": 1,
  "petName": "小橘",
  "deviceId": 1,
  "deviceName": "客厅摄像头",
  "deviceSerial": "D12345678",
  "detectTime": "2026-05-05T10:30:00",
  "petCoordX": 50.0,
  "petCoordY": 60.0,
  "petWidth": 20.0,
  "petHeight": 15.0,
  "inSafeZone": 1,
  "alarmTriggered": 0,
  "snapshotUrl": "https://...",
  "createdAt": "2026-05-05T10:30:05"
}
```

---

### 手动检测与分析

### 36. 执行一次检测 `POST /api/pet-detection/configs/{id}/detect`

立即截取当前视频画面并进行AI宠物检测

响应 `data`:
```json
{
  "recordId": 1,
  "petId": 1,
  "petName": "小橘",
  "deviceId": 1,
  "deviceName": "客厅摄像头",
  "detectTime": "2026-05-05T10:30:00",
  "inSafeZone": true,
  "alarmTriggered": false,
  "snapshotUrl": "https://...",
  "message": "检测完成, 宠物在安全区域内"
}
```

> 提示: 此接口会调用AI模型, 响应较慢, 前端应设置较长超时

### 37. AI分析宠物状态 `POST /api/pet-detection/configs/{id}/analyze`

对当前视频画面进行AI分析, 返回文字描述

响应 `data`: `"String"` (AI生成的文字描述)

> 提示: 同上, 需要较长超时时间

---

### 检测告警

### 38. 检测告警列表 `GET /api/pet-detection/alarms`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| alarmType | String | 否 | 告警类型(如: absent/abnormal_activity/stillness) |
| readStatus | Integer | 否 | 已读状态 |

响应 `data`: `AlarmMessageVO[]`

### 39. 未出现告警 `GET /api/pet-detection/alarms/absent`

响应 `data`: `AlarmMessageVO[]` (宠物长时间未出现的告警列表)

### 40. 异常活跃告警 `GET /api/pet-detection/alarms/abnormal-activity`

响应 `data`: `AlarmMessageVO[]` (宠物异常活跃的告警列表)

### 41. 长时间静止告警 `GET /api/pet-detection/alarms/stillness`

响应 `data`: `AlarmMessageVO[]` (宠物长时间静止的告警列表)

---

## 附录: 关键提醒

1. **认证**: 除注册/登录外, 所有接口必须携带 `Authorization: Bearer <token>`, token有效期 **2小时**
2. **视频相关接口**: 直播/回放地址有有效期, 过期需重新获取
3. **AI检测接口**: `/detect` 和 `/analyze` 会调用AI模型, 响应时间较长, 前端建议设置 **30秒以上超时**, 并加loading状态
4. **同步接口**: `/devices/sync` 和 `/alarms/sync` 会请求萤石云API, 有频率限制, 不要高频调用
5. **安全区域坐标**: 采用百分比(0-100), 非像素值, 方便适配不同分辨率
6. **删除设备/告警**: 使用逻辑删除, 数据不会物理移除
