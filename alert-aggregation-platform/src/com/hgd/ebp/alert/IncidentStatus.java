package com.hgd.ebp.alert;

/**
 * Incident 生命周期状态。
 *
 * <p>第一版只保留最核心的状态流转：OPEN -> ACK -> RESOLVED。
 * 这样既能满足值班闭环，又不会过早引入复杂工作流。</p>
 */
public enum IncidentStatus {
	/** 新建或重新触发的事故，还没有人确认处理。 */
	OPEN,

	/** 已被值班人员确认，表示有人接手处理。 */
	ACK,

	/** 已恢复或人工关闭，表示本次事故处理完成。 */
	RESOLVED
}
