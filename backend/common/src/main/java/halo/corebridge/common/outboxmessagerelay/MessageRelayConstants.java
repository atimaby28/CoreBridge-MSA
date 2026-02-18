package halo.corebridge.common.outboxmessagerelay;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessageRelayConstants {
    public static final int SHARD_COUNT = 4;
    public static final int MAX_RETRY_COUNT = 3;
    public static final String DEAD_LETTER_TOPIC = "corebridge-dead-letter";
}
