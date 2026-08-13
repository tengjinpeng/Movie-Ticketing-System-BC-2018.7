package com.hgd.ebp.alert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 聚合后的事故对象。
 *
 * <p>Incident 是值班人员真正需要处理的对象，一条 Incident 可以包含多条相关告警。
 * 例如同一个 service 在 5 分钟内出现多个实例错误率升高，可聚合为一个服务级 Incident。</p>
 */
public class Incident {
	/** Incident 唯一 ID，当前由聚合键 hash 生成，后续可替换为数据库序列。 */
	private final String id;

	/** 命中并创建该 Incident 的聚合规则 ID。 */
	private final String ruleId;

	/** 聚合键，表示哪些告警会被归入同一个 Incident。 */
	private final String groupKey;

	/** 聚合层级，用于展示影响面和选择默认路由。 */
	private final AggregateLevel aggregateLevel;

	/** 归属于该 Incident 的原始告警事件列表。 */
	private final List<AlertEvent> events = new ArrayList<AlertEvent>();

	/** 当前生命周期状态，默认新建为 OPEN。 */
	private IncidentStatus status = IncidentStatus.OPEN;

	/** Incident 首次创建时间。 */
	private Date createdAt = new Date();

	/** Incident 最近一次更新时间，例如新增告警、ACK、RESOLVE。 */
	private Date updatedAt = new Date();

	/** ACK 该 Incident 的处理人；未 ACK 时为空。 */
	private String owner;

	/**
	 * 创建 Incident。
	 *
	 * @param id Incident 唯一 ID
	 * @param ruleId 命中的聚合规则 ID
	 * @param groupKey 聚合键
	 * @param aggregateLevel 聚合层级
	 */
	public Incident(String id, String ruleId, String groupKey, AggregateLevel aggregateLevel) {
		this.id = id;
		this.ruleId = ruleId;
		this.groupKey = groupKey;
		this.aggregateLevel = aggregateLevel;
	}

	/**
	 * 向 Incident 中追加一条告警事件。
	 *
	 * <p>如果已经 RESOLVED 的 Incident 又收到 FIRING 告警，说明问题复发，状态会重新打开。</p>
	 *
	 * @param event 需要关联到该 Incident 的告警事件
	 */
	public void addEvent(AlertEvent event) {
		events.add(event);
		updatedAt = new Date();
		if (event.getStatus() == AlertStatus.FIRING && status == IncidentStatus.RESOLVED) {
			status = IncidentStatus.OPEN;
		}
	}

	/**
	 * 确认 Incident，表示已有人员接手。
	 *
	 * @param owner 处理人标识，例如用户名、工号或值班组名
	 */
	public void ack(String owner) {
		if (status == IncidentStatus.OPEN) {
			status = IncidentStatus.ACK;
			this.owner = owner;
			updatedAt = new Date();
		}
	}

	/**
	 * 关闭 Incident，表示告警已恢复或人工判定处理完成。
	 */
	public void resolve() {
		status = IncidentStatus.RESOLVED;
		updatedAt = new Date();
	}

	/** @return Incident 唯一 ID。 */
	public String getId() { return id; }

	/** @return 命中的聚合规则 ID。 */
	public String getRuleId() { return ruleId; }

	/** @return 聚合键。 */
	public String getGroupKey() { return groupKey; }

	/** @return 聚合层级。 */
	public AggregateLevel getAggregateLevel() { return aggregateLevel; }

	/** @return 当前生命周期状态。 */
	public IncidentStatus getStatus() { return status; }

	/** @return Incident 首次创建时间。 */
	public Date getCreatedAt() { return createdAt; }

	/** @return Incident 最近一次更新时间。 */
	public Date getUpdatedAt() { return updatedAt; }

	/** @return ACK 处理人；未 ACK 时为空。 */
	public String getOwner() { return owner; }

	/** @return 只读关联告警事件列表。 */
	public List<AlertEvent> getEvents() { return Collections.unmodifiableList(events); }
}
