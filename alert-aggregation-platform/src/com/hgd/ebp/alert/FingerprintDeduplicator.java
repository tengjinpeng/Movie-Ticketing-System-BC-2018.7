package com.hgd.ebp.alert;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 指纹去重器。
 *
 * <p>该类模拟 Redis SETNX + TTL 的行为：第一次看到某个指纹时放行，TTL 窗口内再次
 * 看到相同指纹则丢弃。当前使用内存 Map，方便项目在不增加 Redis 依赖的情况下先跑通
 * 聚合流程；生产环境可将 shouldAccept 方法替换为 Redis set-if-absent 操作。</p>
 */
public class FingerprintDeduplicator {
	/** 指纹到过期时间戳的映射，值为毫秒时间戳。 */
	private final Map<String, Long> fingerprints = new HashMap<String, Long>();

	/** 去重窗口时长，单位毫秒。 */
	private final long ttlMillis;

	/**
	 * 创建去重器。
	 *
	 * @param ttlMillis 指纹保留时长，建议第一版使用 3 到 10 分钟
	 */
	public FingerprintDeduplicator(long ttlMillis) {
		this.ttlMillis = ttlMillis;
	}

	/**
	 * 判断告警是否应该进入后续聚合流程。
	 *
	 * @param event 待处理告警事件
	 * @return true 表示本窗口内首次出现，可以继续处理；false 表示重复告警，应忽略
	 */
	public synchronized boolean shouldAccept(AlertEvent event) {
		long now = System.currentTimeMillis();
		cleanExpired(now);
		String fingerprint = event.getFingerprint();
		if (fingerprint == null || fingerprint.length() == 0) {
			fingerprint = AlertFingerprint.create(event);
			event.setFingerprint(fingerprint);
		}
		if (fingerprints.containsKey(fingerprint)) {
			return false;
		}
		fingerprints.put(fingerprint, Long.valueOf(now + ttlMillis));
		return true;
	}

	/**
	 * 清理已经过期的指纹，避免内存 Map 无限增长。
	 *
	 * @param now 当前毫秒时间戳
	 */
	private void cleanExpired(long now) {
		Iterator<Map.Entry<String, Long>> iterator = fingerprints.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, Long> entry = iterator.next();
			if (entry.getValue().longValue() <= now) {
				iterator.remove();
			}
		}
	}
}
