package halo.corebridge.common.snowflake;

import java.util.random.RandomGenerator;

/**
 * Twitter Snowflake 알고리즘 기반 분산 ID 생성기
 *
 * 64bit: | 1 unused | 41 timestamp | 10 node | 12 sequence |
 */
public class Snowflake {

    private static final int NODE_ID_BITS = 10;
    private static final int SEQUENCE_BITS = 12;

    private static final long MAX_NODE_ID = (1L << NODE_ID_BITS) - 1;
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;

    // UTC = 2026-01-01T00:00:00Z
    private static final long EPOCH = 1767225600000L;

    private final long nodeId;
    private long lastTimeMillis = EPOCH;
    private long sequence = 0L;

    public Snowflake() {
        this.nodeId = RandomGenerator.getDefault().nextLong(MAX_NODE_ID + 1);
    }

    public Snowflake(long nodeId) {
        if (nodeId < 0 || nodeId > MAX_NODE_ID) {
            throw new IllegalArgumentException("Node ID must be between 0 and " + MAX_NODE_ID);
        }
        this.nodeId = nodeId;
    }

    public synchronized long nextId() {
        long currentTimeMillis = System.currentTimeMillis();

        if (currentTimeMillis < lastTimeMillis) {
            throw new IllegalStateException("Clock moved backwards");
        }

        if (currentTimeMillis == lastTimeMillis) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                currentTimeMillis = waitNextMillis(currentTimeMillis);
            }
        } else {
            sequence = 0;
        }

        lastTimeMillis = currentTimeMillis;

        return ((currentTimeMillis - EPOCH) << (NODE_ID_BITS + SEQUENCE_BITS))
                | (nodeId << SEQUENCE_BITS)
                | sequence;
    }

    private long waitNextMillis(long currentTimestamp) {
        while (currentTimestamp <= lastTimeMillis) {
            currentTimestamp = System.currentTimeMillis();
        }
        return currentTimestamp;
    }
}
