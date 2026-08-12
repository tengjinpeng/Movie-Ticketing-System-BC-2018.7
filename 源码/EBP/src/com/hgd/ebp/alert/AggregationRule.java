package com.hgd.ebp.alert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 聚合规则模型。
 *
 * <p>该对象描述“什么样的告警可以聚合、按哪些标签聚合、达到多少数量才形成有效
 * Incident”。生产环境中可以由 YAML/JSON DSL 解析生成该对象，避免把聚合规则写死在
 * Java 代码里。</p>
 */
public class AggregationRule {
	/** 规则唯一 ID，用于审计、回放和生成聚合键。 */
	private String id;

	/** 规则名称，用于管理界面展示和排障。 */
	private String name;

	/** 是否启用该规则；关闭后不会匹配任何告警。 */
	private boolean enabled = true;

	/** 该规则产出的聚合层级，例如 HOST、SERVICE、CLUSTER。 */
	private AggregateLevel aggregateLevel;

	/** 构建聚合键时使用的标签列表，所有标签都存在时才会命中。 */
	private List<String> groupBy = new ArrayList<String>();

	/** 触发 Incident 输出所需的最小告警数量。 */
	private int thresholdCount = 1;

	/** 告警需要达到的最低级别，低于该级别则不参与聚合。 */
	private AlertSeverity severityAtLeast = AlertSeverity.INFO;

	/**
	 * 创建聚合规则，并默认按聚合层级对应标签分组。
	 *
	 * @param id 规则唯一 ID
	 * @param name 规则名称
	 * @param aggregateLevel 聚合层级
	 */
	public AggregationRule(String id, String name, AggregateLevel aggregateLevel) {
		this.id = id;
		this.name = name;
		this.aggregateLevel = aggregateLevel;
		this.groupBy.add(aggregateLevel.getLabelName());
	}

	/**
	 * 判断某条告警是否满足规则的基础条件。
	 *
	 * @param event 待判断告警事件
	 * @return true 表示该告警可进入本规则的分组聚合
	 */
	public boolean matches(AlertEvent event) {
		return enabled && event.getStatus() == AlertStatus.FIRING
				&& event.getSeverity() != null && event.getSeverity().isAtLeast(severityAtLeast)
				&& buildGroupKey(event).length() > 0;
	}

	/**
	 * 基于 groupBy 标签构建聚合键。
	 *
	 * <p>如果任一必需标签缺失，返回空字符串，表示该告警无法按本规则聚合。</p>
	 *
	 * @param event 待聚合告警事件
	 * @return 聚合键，例如 service-default|service=order-api；无法聚合时返回空字符串
	 */
	public String buildGroupKey(AlertEvent event) {
		StringBuilder key = new StringBuilder(id);
		for (String label : groupBy) {
			String value = event.getLabel(label);
			if (value == null || value.length() == 0) return "";
			key.append('|').append(label).append('=').append(value);
		}
		return key.toString();
	}

	/** @return 规则唯一 ID。 */
	public String getId() { return id; }

	/** @return 规则名称。 */
	public String getName() { return name; }

	/** @return 该规则产出的聚合层级。 */
	public AggregateLevel getAggregateLevel() { return aggregateLevel; }

	/** @return 触发 Incident 输出所需的最小告警数量。 */
	public int getThresholdCount() { return thresholdCount; }

	/** @param thresholdCount 设置触发 Incident 输出所需的最小告警数量。 */
	public void setThresholdCount(int thresholdCount) { this.thresholdCount = thresholdCount; }

	/** @return 告警需要达到的最低级别。 */
	public AlertSeverity getSeverityAtLeast() { return severityAtLeast; }

	/** @param severityAtLeast 设置告警需要达到的最低级别。 */
	public void setSeverityAtLeast(AlertSeverity severityAtLeast) { this.severityAtLeast = severityAtLeast; }

	/** @return 构建聚合键时使用的只读标签列表。 */
	public List<String> getGroupBy() { return Collections.unmodifiableList(groupBy); }

	/**
	 * 设置聚合维度标签列表。
	 *
	 * @param groupBy 标签名列表，例如 cluster + service 表示按集群内服务聚合
	 */
	public void setGroupBy(List<String> groupBy) {
		this.groupBy = new ArrayList<String>();
		if (groupBy != null) this.groupBy.addAll(groupBy);
	}
}
