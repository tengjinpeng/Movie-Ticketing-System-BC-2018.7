package com.hgd.ebp.alert;

/**
 * 告警路由结果。
 *
 * <p>聚合服务只计算应该通知哪个团队、使用哪个升级策略；真正的邮件、
 * 企业微信、钉钉或值班系统发送逻辑可以在外层根据该对象完成。</p>
 */
public class NotificationRoute {
	/** 接收该 Incident 的团队，例如 sre-platform、infrastructure。 */
	private final String team;

	/** 升级策略名称，例如 standard、prod-critical。 */
	private final String escalationPolicy;

	/**
	 * 创建路由结果。
	 *
	 * @param team 接收团队
	 * @param escalationPolicy 升级策略
	 */
	public NotificationRoute(String team, String escalationPolicy) {
		this.team = team;
		this.escalationPolicy = escalationPolicy;
	}

	/** @return 接收团队。 */
	public String getTeam() { return team; }

	/** @return 升级策略名称。 */
	public String getEscalationPolicy() { return escalationPolicy; }
}
