package com.hgd.ebp.alert;

/**
 * 统一告警状态。
 *
 * <p>外部系统的状态字段会被适配器转换成这里的状态，聚合流程只处理
 * FIRING 告警；RESOLVED 后续可用于关闭或更新 Incident。</p>
 */
public enum AlertStatus {
	/** 告警正在触发，应该进入去重、聚合和通知流程。 */
	FIRING,

	/** 告警已经恢复，可用于关闭 Incident 或记录恢复事件。 */
	RESOLVED
}
