package com.hgd.ebp.alert;

/**
 * 告警路由器。
 *
 * <p>该类根据 Incident 的聚合层级和第一条告警的标签，计算应该通知哪个团队以及使用
 * 哪个升级策略。第一版先提供简单默认规则，后续可以替换为数据库配置或 DSL 路由规则。</p>
 */
public class AlertRouter {
	/**
	 * 计算 Incident 的通知路由。
	 *
	 * @param incident 聚合后的事故对象
	 * @return 包含接收团队和升级策略的路由结果
	 */
	public NotificationRoute route(Incident incident) {
		AlertEvent first = incident.getEvents().isEmpty() ? null : incident.getEvents().get(0);
		String team = first == null ? null : first.getLabel("team");
		if (team == null || team.length() == 0) {
			team = incident.getAggregateLevel() == AggregateLevel.HOST ? "infrastructure" : "sre-platform";
		}
		String policy = incident.getAggregateLevel() == AggregateLevel.CLUSTER ? "prod-critical" : "standard";
		return new NotificationRoute(team, policy);
	}
}
