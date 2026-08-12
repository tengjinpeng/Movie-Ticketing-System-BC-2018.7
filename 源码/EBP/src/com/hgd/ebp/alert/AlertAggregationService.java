package com.hgd.ebp.alert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 告警聚合主服务。
 *
 * <p>该服务串联第一版告警平台的核心主链路：指纹去重、规则匹配、多级聚合、
 * Incident 创建/更新、ACK/RESOLVED 生命周期入口和路由查询。</p>
 */
public class AlertAggregationService {
	/** 指纹去重器，用于过滤短时间内重复触发的同一告警。 */
	private final FingerprintDeduplicator deduplicator;

	/** 聚合规则列表；第一版内置默认规则，后续可从 DSL 或数据库加载。 */
	private final List<AggregationRule> rules = new ArrayList<AggregationRule>();

	/** 聚合键到 Incident 的内存索引；后续生产环境可替换为数据库或缓存。 */
	private final Map<String, Incident> incidents = new HashMap<String, Incident>();

	/** 告警路由器，用于计算通知团队和升级策略。 */
	private final AlertRouter router = new AlertRouter();

	/**
	 * 创建聚合服务并注册默认 Host、Service、Cluster 三级规则。
	 *
	 * @param deduplicator 指纹去重器，调用方可传入内存版或 Redis 版实现
	 */
	public AlertAggregationService(FingerprintDeduplicator deduplicator) {
		this.deduplicator = deduplicator;
		registerDefaultRules();
	}

	/**
	 * 接收一条标准化告警并尝试聚合成 Incident。
	 *
	 * <p>处理步骤：先做指纹去重；未重复则逐条匹配聚合规则；命中后按 groupKey 找到
	 * 或创建 Incident；当关联告警数量达到规则阈值时返回该 Incident，供外层触发通知。</p>
	 *
	 * @param event 已经由适配器转换好的统一告警事件
	 * @return 本次处理后达到通知条件的 Incident 列表；如果告警重复或未达阈值则为空列表
	 */
	public List<Incident> accept(AlertEvent event) {
		List<Incident> changed = new ArrayList<Incident>();
		if (!deduplicator.shouldAccept(event)) {
			return changed;
		}
		for (AggregationRule rule : rules) {
			if (!rule.matches(event)) continue;
			String groupKey = rule.buildGroupKey(event);
			Incident incident = incidents.get(groupKey);
			if (incident == null) {
				incident = new Incident("INC-" + Integer.toHexString(groupKey.hashCode()),
						rule.getId(), groupKey, rule.getAggregateLevel());
				incidents.put(groupKey, incident);
			}
			incident.addEvent(event);
			if (incident.getEvents().size() >= rule.getThresholdCount()) {
				changed.add(incident);
			}
		}
		return changed;
	}

	/**
	 * 查询某个 Incident 的通知路由。
	 *
	 * @param incident 聚合后的 Incident
	 * @return 通知团队和升级策略
	 */
	public NotificationRoute route(Incident incident) {
		return router.route(incident);
	}

	/**
	 * 将 Incident 标记为 ACK，表示有人确认接手。
	 *
	 * @param incidentId Incident ID
	 * @param owner 处理人标识
	 */
	public void ack(String incidentId, String owner) {
		Incident incident = findIncident(incidentId);
		if (incident != null) incident.ack(owner);
	}

	/**
	 * 将 Incident 标记为 RESOLVED，表示事故已恢复或人工关闭。
	 *
	 * @param incidentId Incident ID
	 */
	public void resolve(String incidentId) {
		Incident incident = findIncident(incidentId);
		if (incident != null) incident.resolve();
	}

	/**
	 * 返回当前规则列表的只读视图，便于页面展示或测试断言。
	 *
	 * @return 只读聚合规则列表
	 */
	public List<AggregationRule> getRules() {
		return Collections.unmodifiableList(rules);
	}

	/**
	 * 根据 Incident ID 查找内存中的 Incident。
	 *
	 * @param incidentId Incident ID
	 * @return 找到则返回 Incident；不存在时返回 null
	 */
	private Incident findIncident(String incidentId) {
		for (Incident incident : incidents.values()) {
			if (incident.getId().equals(incidentId)) return incident;
		}
		return null;
	}

	/**
	 * 注册第一版默认聚合规则。
	 *
	 * <p>Host 级 1 条即输出，Service 级 2 条输出，Cluster 级 3 条且至少 WARNING 输出。
	 * 这些默认值便于快速落地，生产环境应逐步迁移到可配置 DSL。</p>
	 */
	private void registerDefaultRules() {
		AggregationRule hostRule = new AggregationRule("host-default", "Host alert aggregation", AggregateLevel.HOST);
		hostRule.setThresholdCount(1);
		rules.add(hostRule);

		AggregationRule serviceRule = new AggregationRule("service-default", "Service alert aggregation", AggregateLevel.SERVICE);
		serviceRule.setThresholdCount(2);
		rules.add(serviceRule);

		AggregationRule clusterRule = new AggregationRule("cluster-default", "Cluster alert aggregation", AggregateLevel.CLUSTER);
		clusterRule.setThresholdCount(3);
		clusterRule.setSeverityAtLeast(AlertSeverity.WARNING);
		rules.add(clusterRule);
	}
}
