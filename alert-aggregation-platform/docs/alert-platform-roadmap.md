# 告警平台分阶段建设方案

本文档记录告警平台的第一版核心能力、第二版演进方向，以及面向大型生产环境的可配置聚合规则 DSL 示例。

## 第一版核心能力

第一版优先打通告警接入、标准化、去重、聚合、事件生命周期和通知闭环，避免过早引入复杂智能能力。

1. **Zabbix + Prometheus 双接入**
   - 提供 Zabbix webhook 接入端点，解析 trigger、host、severity、event id 等字段。
   - 提供 Prometheus Alertmanager webhook 接入端点，解析 labels、annotations、status、startsAt、endsAt 等字段。
   - 两类来源统一转换为内部 `AlertEvent`，后续流程不再感知原始监控系统差异。

2. **统一 AlertEvent 模型**
   - 统一字段建议包括：`source`、`externalId`、`title`、`severity`、`status`、`fingerprint`、`labels`、`annotations`、`startsAt`、`endsAt`。
   - `labels` 用于机器可读的路由、聚合和抑制条件；`annotations` 用于人类可读的告警描述、处理建议和面板链接。

3. **Redis 指纹去重**
   - 根据来源、告警名称、资源标识、关键标签和严重级别计算稳定指纹。
   - 使用 Redis `SETNX` 或带 TTL 的 key 进行短窗口去重，避免同一告警在抖动期重复创建 Incident。
   - 去重窗口建议先从 3 到 10 分钟开始，再根据真实告警噪声调优。

4. **Host / Service / Cluster 多级聚合**
   - Host 维度用于识别单机故障。
   - Service 维度用于识别同一业务服务的多实例异常。
   - Cluster 维度用于识别集群级故障或基础设施面故障。
   - 聚合结果应写入 Incident 的关联告警列表，便于值班人员看到影响面。

5. **Incident 生命周期管理**
   - 生命周期状态：`OPEN -> ACK -> RESOLVED`。
   - `OPEN` 表示新建或重新触发的事故。
   - `ACK` 表示已经有人确认并接手处理。
   - `RESOLVED` 表示监控恢复或人工关闭。
   - 状态流转需要记录操作者、时间、原因和备注，形成审计日志。

6. **告警路由 + 升级通知**
   - 路由条件可基于 `severity`、`service`、`cluster`、`env`、`team` 等标签。
   - 通知渠道可先支持邮件、企业微信、钉钉或 Slack 中的一到两种。
   - 升级策略建议包含首次通知、无人 ACK 后升级、持续未恢复重复提醒。

## 第二版演进方向

第二版适合在第一版稳定运行并沉淀足够历史数据后建设：

- **根因分析**：结合时间窗口、拓扑关系和告警传播路径推断可能根因。
- **CMDB 拓扑**：接入服务、主机、集群、依赖关系，为聚合和根因分析提供上下文。
- **告警抑制**：当上游或集群级告警已经触发时，抑制低价值的下游衍生告警。
- **智能关联**：基于历史相似事件、标签相似度、时间序列和拓扑邻近性做关联推荐。

## 可配置聚合规则 DSL 示例

大型生产环境不建议把聚合规则写死在 Java 代码中。推荐将规则配置化，便于 SRE 和业务团队在不发版的情况下调整策略。

```yaml
rules:
  - id: service-high-error-rate
    name: 同一服务高错误率聚合
    enabled: true
    match:
      source: prometheus
      labels:
        alertname: HighErrorRate
        env: prod
    groupBy:
      - cluster
      - service
    window: 5m
    threshold:
      count: 3
      severityAtLeast: warning
    incident:
      title: "{{ service }} 在 {{ cluster }} 出现高错误率"
      severity: critical
      aggregateLevel: service
    route:
      teamLabel: team
      defaultTeam: sre-platform
      escalationPolicy: prod-critical

  - id: host-resource-saturation
    name: 单主机资源饱和聚合
    enabled: true
    match:
      labels:
        alertname:
          in:
            - HighCpuUsage
            - HighMemoryUsage
            - DiskAlmostFull
    groupBy:
      - host
    window: 10m
    threshold:
      count: 2
    incident:
      title: "{{ host }} 出现多项资源饱和告警"
      severity: warning
      aggregateLevel: host
    route:
      defaultTeam: infrastructure
```

## DSL 执行建议

- 规则加载后应进行 schema 校验，避免运行期才发现配置错误。
- `match` 只负责筛选候选告警，`groupBy` 决定聚合键，`window` 决定时间范围，`threshold` 决定是否创建或更新 Incident。
- 模板变量只允许引用标准化后的 `AlertEvent.labels` 和白名单字段，避免执行任意表达式带来的安全风险。
- 每次规则命中都应记录规则 ID、版本、聚合键和参与告警 ID，便于排障和回放。

## 当前代码落地范围

本仓库已提供一组轻量级 Java 组件用于表达第一版聚合主链路：

- `AlertEvent`、`AlertSeverity`、`AlertStatus` 负责承载 Zabbix 和 Prometheus 转换后的统一告警模型。
- `ZabbixAlertAdapter` 与 `PrometheusAlertAdapter` 负责把两类监控输入转换为统一事件。
- `FingerprintDeduplicator` 与 `AlertFingerprint` 提供 Redis `SETNX` 风格的指纹去重抽象；当前默认实现为内存版，便于后续替换为 Redis 客户端。
- `AggregationRule` 与 `AlertAggregationService` 支持 Host、Service、Cluster 三级默认聚合规则。
- `Incident` 支持 `OPEN -> ACK -> RESOLVED` 生命周期流转。
- `AlertRouter` 与 `NotificationRoute` 输出团队和升级策略，供邮件、IM 或值班系统集成。
