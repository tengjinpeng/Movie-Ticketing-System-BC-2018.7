package com.hgd.ebp.alert;

/**
 * 统一告警级别。
 *
 * <p>不同监控系统的级别命名可能不同，例如 Zabbix 的 Disaster、High，
 * Prometheus 常见的 critical、warning。适配器会先转换为该枚举，后续规则只按
 * 统一级别处理。</p>
 */
public enum AlertSeverity {
	/** 信息级别，通常只记录或低优先级通知。 */
	INFO(1),

	/** 警告级别，通常需要关注但不一定立即升级。 */
	WARNING(2),

	/** 严重级别，通常需要立即通知值班人员并触发升级策略。 */
	CRITICAL(3);

	/** 数值化级别，用于比较当前级别是否达到规则阈值。 */
	private final int level;

	/**
	 * 创建告警级别。
	 *
	 * @param level 数值化优先级，数字越大表示越严重
	 */
	AlertSeverity(int level) {
		this.level = level;
	}

	/**
	 * 获取数值化优先级。
	 *
	 * @return 数字越大表示级别越严重
	 */
	public int getLevel() {
		return level;
	}

	/**
	 * 判断当前级别是否大于等于传入级别。
	 *
	 * @param severity 规则要求的最低告警级别
	 * @return 当前级别达到或超过要求时返回 true
	 */
	public boolean isAtLeast(AlertSeverity severity) {
		return this.level >= severity.level;
	}
}
