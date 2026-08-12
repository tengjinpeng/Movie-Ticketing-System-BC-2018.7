package com.hgd.ebp.alert;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 统一告警事件模型。
 *
 * <p>所有外部监控系统（例如 Zabbix、Prometheus Alertmanager）进入本系统后，
 * 都应该先转换成这个对象。后续的去重、聚合、路由和 Incident 生命周期管理只依赖
 * 该统一模型，避免在业务流程中到处判断来源系统的字段差异。</p>
 */
public class AlertEvent {
	/** 告警来源系统，例如 zabbix、prometheus，用于追踪数据来源和生成指纹。 */
	private String source;

	/** 外部监控系统中的原始事件 ID，用于回查源系统事件。 */
	private String externalId;

	/** 告警标题，通常来自 Zabbix triggerName 或 Prometheus alertname。 */
	private String title;

	/** 统一后的告警级别，用于路由、升级和聚合阈值判断。 */
	private AlertSeverity severity;

	/** 统一后的告警状态，FIRING 表示触发中，RESOLVED 表示已恢复。 */
	private AlertStatus status;

	/** 稳定指纹，用于 Redis SETNX 或内存 TTL 去重。 */
	private String fingerprint;

	/** 机器可读标签，用于聚合键、路由规则和后续抑制规则匹配。 */
	private Map<String, String> labels = new HashMap<String, String>();

	/** 人类可读注解，用于展示描述、处理建议、面板链接等补充信息。 */
	private Map<String, String> annotations = new HashMap<String, String>();

	/** 告警开始时间；适配器未提供时默认使用事件创建时间。 */
	private Date startsAt;

	/** 告警恢复时间；未恢复或源系统未提供时为空。 */
	private Date endsAt;

	/**
	 * 默认构造方法，便于框架、测试或手动 setter 方式创建事件。
	 */
	public AlertEvent() {
	}

	/**
	 * 常用构造方法，用最关键的标准字段创建告警事件。
	 *
	 * @param source 来源系统名称，例如 zabbix 或 prometheus
	 * @param externalId 来源系统事件 ID 或指纹
	 * @param title 告警标题
	 * @param severity 统一告警级别
	 * @param status 统一告警状态
	 */
	public AlertEvent(String source, String externalId, String title,
			AlertSeverity severity, AlertStatus status) {
		this.source = source;
		this.externalId = externalId;
		this.title = title;
		this.severity = severity;
		this.status = status;
		this.startsAt = new Date();
	}

	/** @return 告警来源系统。 */
	public String getSource() { return source; }

	/** @param source 设置告警来源系统。 */
	public void setSource(String source) { this.source = source; }

	/** @return 外部监控系统中的原始事件 ID。 */
	public String getExternalId() { return externalId; }

	/** @param externalId 设置外部监控系统中的原始事件 ID。 */
	public void setExternalId(String externalId) { this.externalId = externalId; }

	/** @return 告警标题。 */
	public String getTitle() { return title; }

	/** @param title 设置告警标题。 */
	public void setTitle(String title) { this.title = title; }

	/** @return 统一告警级别。 */
	public AlertSeverity getSeverity() { return severity; }

	/** @param severity 设置统一告警级别。 */
	public void setSeverity(AlertSeverity severity) { this.severity = severity; }

	/** @return 统一告警状态。 */
	public AlertStatus getStatus() { return status; }

	/** @param status 设置统一告警状态。 */
	public void setStatus(AlertStatus status) { this.status = status; }

	/** @return 用于去重的稳定指纹。 */
	public String getFingerprint() { return fingerprint; }

	/** @param fingerprint 设置用于去重的稳定指纹。 */
	public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }

	/** @return 告警开始时间。 */
	public Date getStartsAt() { return startsAt; }

	/** @param startsAt 设置告警开始时间。 */
	public void setStartsAt(Date startsAt) { this.startsAt = startsAt; }

	/** @return 告警恢复时间。 */
	public Date getEndsAt() { return endsAt; }

	/** @param endsAt 设置告警恢复时间。 */
	public void setEndsAt(Date endsAt) { this.endsAt = endsAt; }

	/**
	 * 返回只读标签，避免调用方绕过 addLabel 或 setLabels 随意修改内部状态。
	 *
	 * @return 只读标签 Map
	 */
	public Map<String, String> getLabels() {
		return Collections.unmodifiableMap(labels);
	}

	/**
	 * 批量设置标签；会复制传入 Map，避免调用方后续修改影响事件对象。
	 *
	 * @param labels 标签 Map，允许为空
	 */
	public void setLabels(Map<String, String> labels) {
		this.labels = new HashMap<String, String>();
		if (labels != null) this.labels.putAll(labels);
	}

	/**
	 * 添加单个标签；key 或 value 为空时忽略，避免产生无效聚合维度。
	 *
	 * @param key 标签名，例如 host、service、cluster、team
	 * @param value 标签值
	 */
	public void addLabel(String key, String value) {
		if (key != null && value != null) labels.put(key, value);
	}

	/**
	 * 按标签名读取标签值，聚合规则和路由规则会频繁使用该方法。
	 *
	 * @param key 标签名
	 * @return 标签值；不存在时返回 null
	 */
	public String getLabel(String key) {
		return labels.get(key);
	}

	/**
	 * 返回只读注解，供页面展示或通知模板使用。
	 *
	 * @return 只读注解 Map
	 */
	public Map<String, String> getAnnotations() {
		return Collections.unmodifiableMap(annotations);
	}

	/**
	 * 批量设置注解；会复制传入 Map，避免外部引用修改内部状态。
	 *
	 * @param annotations 注解 Map，允许为空
	 */
	public void setAnnotations(Map<String, String> annotations) {
		this.annotations = new HashMap<String, String>();
		if (annotations != null) this.annotations.putAll(annotations);
	}

	/**
	 * 添加单个注解；常用于保存 description、runbook、dashboard 等展示信息。
	 *
	 * @param key 注解名
	 * @param value 注解内容
	 */
	public void addAnnotation(String key, String value) {
		if (key != null && value != null) annotations.put(key, value);
	}
}
