package com.hgd.ebp.alert;

/**
 * 告警聚合层级。
 *
 * <p>聚合层级决定把哪些告警合并到同一个 Incident 中。当前第一版支持
 * Host、Service、Cluster 三层，后续可继续扩展到机房、业务线等更高维度。</p>
 */
public enum AggregateLevel {
	/** 主机级聚合：相同 host 的告警合并，适合单机 CPU、内存、磁盘等故障。 */
	HOST("host"),

	/** 服务级聚合：相同 service 的告警合并，适合多实例错误率、延迟等故障。 */
	SERVICE("service"),

	/** 集群级聚合：相同 cluster 的告警合并，适合集群或基础设施面故障。 */
	CLUSTER("cluster");

	/** 与该聚合层级对应的 AlertEvent.labels 标签名。 */
	private final String labelName;

	/**
	 * 创建聚合层级枚举值。
	 *
	 * @param labelName 该层级在 AlertEvent.labels 中使用的标签名
	 */
	AggregateLevel(String labelName) {
		this.labelName = labelName;
	}

	/**
	 * 获取用于构建聚合键的标签名。
	 *
	 * @return 标签名，例如 host、service、cluster
	 */
	public String getLabelName() {
		return labelName;
	}
}
