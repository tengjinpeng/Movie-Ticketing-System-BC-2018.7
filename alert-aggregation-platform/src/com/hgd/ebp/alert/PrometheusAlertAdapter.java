package com.hgd.ebp.alert;

import java.util.Map;

/**
 * Prometheus Alertmanager 告警适配器。
 *
 * <p>Alertmanager webhook 通常将关键信息放在 labels 和 annotations 中。该适配器直接
 * 接收这两个 Map，并转换为统一 AlertEvent。</p>
 */
public class PrometheusAlertAdapter {
	/**
	 * 将 Alertmanager labels、annotations 和状态转换为统一告警事件。
	 *
	 * @param labels Alertmanager labels，建议包含 alertname、severity、host、service、cluster、team
	 * @param annotations Alertmanager annotations，通常包含 summary、description、runbook 等
	 * @param status Alertmanager 状态，resolved 表示恢复，其它值按 firing 处理
	 * @return 标准化后的 AlertEvent
	 */
	public AlertEvent convert(Map<String, String> labels, Map<String, String> annotations, String status) {
		String title = labels == null ? null : labels.get("alertname");
		String fingerprint = labels == null ? null : labels.get("fingerprint");
		String severity = labels == null ? null : labels.get("severity");
		AlertEvent event = new AlertEvent("prometheus", fingerprint, title,
				parseSeverity(severity),
				"resolved".equalsIgnoreCase(status) ? AlertStatus.RESOLVED : AlertStatus.FIRING);
		event.setLabels(labels);
		event.setAnnotations(annotations);
		event.setFingerprint(AlertFingerprint.create(event));
		return event;
	}

	/**
	 * 将 Prometheus severity 标签转换为统一级别。
	 *
	 * @param value severity 标签值，例如 critical、warning、info
	 * @return 统一告警级别；无法识别时默认 WARNING
	 */
	private AlertSeverity parseSeverity(String value) {
		if (value == null) return AlertSeverity.WARNING;
		if ("critical".equalsIgnoreCase(value)) return AlertSeverity.CRITICAL;
		if ("info".equalsIgnoreCase(value)) return AlertSeverity.INFO;
		return AlertSeverity.WARNING;
	}
}
