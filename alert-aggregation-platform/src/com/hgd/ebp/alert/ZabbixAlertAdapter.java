package com.hgd.ebp.alert;

import java.util.Map;

/**
 * Zabbix 告警适配器。
 *
 * <p>接收 Zabbix webhook 解析后的字段 Map，并转换为统一 AlertEvent。调用方可以在
 * Controller 中先把 HTTP 请求体解析成 Map，再调用 convert 方法进入聚合管线。</p>
 */
public class ZabbixAlertAdapter {
	/**
	 * 将 Zabbix payload 转换为统一告警事件。
	 *
	 * @param payload Zabbix 字段 Map，建议包含 eventId、triggerName、severity、status、host、service、cluster、team
	 * @return 标准化后的 AlertEvent
	 */
	public AlertEvent convert(Map<String, String> payload) {
		AlertEvent event = new AlertEvent("zabbix", payload.get("eventId"),
				payload.get("triggerName"), parseSeverity(payload.get("severity")),
				"RESOLVED".equalsIgnoreCase(payload.get("status")) ? AlertStatus.RESOLVED : AlertStatus.FIRING);
		event.addLabel("alertname", payload.get("triggerName"));
		event.addLabel("host", payload.get("host"));
		event.addLabel("service", payload.get("service"));
		event.addLabel("cluster", payload.get("cluster"));
		event.addLabel("team", payload.get("team"));
		event.addAnnotation("description", payload.get("description"));
		event.setFingerprint(AlertFingerprint.create(event));
		return event;
	}

	/**
	 * 将 Zabbix 原始级别转换为统一级别。
	 *
	 * @param value Zabbix 级别名称或数字，例如 Disaster、Critical、5、1
	 * @return 统一告警级别；无法识别时默认 WARNING
	 */
	private AlertSeverity parseSeverity(String value) {
		if (value == null) return AlertSeverity.WARNING;
		String normalized = value.toLowerCase();
		if (normalized.indexOf("critical") >= 0 || normalized.indexOf("disaster") >= 0 || "5".equals(value)) {
			return AlertSeverity.CRITICAL;
		}
		if (normalized.indexOf("info") >= 0 || "1".equals(value)) {
			return AlertSeverity.INFO;
		}
		return AlertSeverity.WARNING;
	}
}
