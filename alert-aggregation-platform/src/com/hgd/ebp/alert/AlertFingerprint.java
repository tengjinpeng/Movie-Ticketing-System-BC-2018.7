package com.hgd.ebp.alert;

/**
 * 告警指纹工具类。
 *
 * <p>指纹用于判断两条告警是否代表同一个问题。为了保证抖动期间的重复告警能被
 * 去重，指纹只使用相对稳定的字段，例如来源、标题、集群、服务、主机、告警名称和级别。</p>
 */
public final class AlertFingerprint {
	/** 工具类不允许实例化。 */
	private AlertFingerprint() {
	}

	/**
	 * 根据统一告警事件生成稳定指纹。
	 *
	 * <p>当前实现返回 Java hash 的十六进制字符串，适合轻量级场景演示。生产环境可以
	 * 替换为 SHA-256 或 MurmurHash，并保持相同字段拼接策略。</p>
	 *
	 * @param event 统一告警事件
	 * @return 指纹字符串，用于 Redis key 或内存去重 key
	 */
	public static String create(AlertEvent event) {
		StringBuilder value = new StringBuilder();
		append(value, event.getSource());
		append(value, event.getTitle());
		append(value, event.getLabel("cluster"));
		append(value, event.getLabel("service"));
		append(value, event.getLabel("host"));
		append(value, event.getLabel("alertname"));
		append(value, event.getSeverity() == null ? null : event.getSeverity().name());
		return Integer.toHexString(value.toString().hashCode());
	}

	/**
	 * 规范化并追加单个字段。
	 *
	 * @param builder 指纹原文构造器
	 * @param value 待追加字段，允许为空
	 */
	private static void append(StringBuilder builder, String value) {
		builder.append(value == null ? "" : value.trim().toLowerCase()).append('|');
	}
}
