# Alert Aggregation Platform

这是一个从电影购票系统中拆分出来的独立告警聚合项目骨架，目标是先落地第一版核心能力：

1. Zabbix + Prometheus 双接入
2. 统一 `AlertEvent` 模型
3. 指纹去重（当前内存实现，接口语义对齐 Redis `SETNX + TTL`）
4. Host / Service / Cluster 多级聚合
5. Incident 生命周期管理：`OPEN -> ACK -> RESOLVED`
6. 告警路由 + 升级通知策略输出

第二版可继续扩展根因分析、CMDB 拓扑、告警抑制和智能关联。

## 项目结构

```text
alert-aggregation-platform/
├── README.md
├── docs/
│   └── alert-platform-roadmap.md
└── src/
    └── com/hgd/ebp/alert/
        ├── AlertEvent.java
        ├── ZabbixAlertAdapter.java
        ├── PrometheusAlertAdapter.java
        ├── AlertFingerprint.java
        ├── FingerprintDeduplicator.java
        ├── AggregationRule.java
        ├── AlertAggregationService.java
        ├── Incident.java
        ├── AlertRouter.java
        └── NotificationRoute.java
```

## 如何作为独立 Git 项目使用

如果需要把该目录单独推送到一个新仓库，可以在该目录内执行：

```bash
cd alert-aggregation-platform
git init
git add .
git commit -m "Initial alert aggregation platform"
```

## 快速编译检查

该项目当前不引入外部依赖，可以直接使用 JDK 编译核心类：

```bash
mkdir -p /tmp/alert-classes
javac -encoding UTF-8 -d /tmp/alert-classes src/com/hgd/ebp/alert/*.java
```

## 典型接入流程

1. Webhook Controller 接收 Zabbix 或 Prometheus Alertmanager 请求。
2. Controller 将请求解析为 `Map<String, String>`。
3. 调用 `ZabbixAlertAdapter` 或 `PrometheusAlertAdapter` 转换成 `AlertEvent`。
4. 调用 `AlertAggregationService.accept(event)` 进行去重和聚合。
5. 对返回的 Incident 调用 `AlertAggregationService.route(incident)` 获取通知团队和升级策略。
6. 外层通知模块根据 `NotificationRoute` 对接邮件、企业微信、钉钉、Slack 或值班平台。

## 生产化建议

- 将 `FingerprintDeduplicator` 的内存 Map 替换为 Redis `SETNX + EXPIRE`。
- 将 `AlertAggregationService` 的内存 Incident 索引替换为数据库表或缓存。
- 将 `registerDefaultRules()` 迁移为 YAML/JSON DSL 配置加载。
- 为适配器、指纹、去重、聚合、生命周期和路由补充单元测试。
