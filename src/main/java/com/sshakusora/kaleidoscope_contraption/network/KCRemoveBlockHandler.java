package com.sshakusora.kaleidoscope_contraption.network;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 移除方块按键状态管理器
 * 用于在服务端跟踪哪些玩家当前按下了移除方块键
 */
public class KCRemoveBlockHandler {

    // 存储当前按下移除键的玩家UUID
    private static final Map<UUID, Boolean> removeKeyPressed = new ConcurrentHashMap<>();

    /**
     * 设置玩家的移除键按下状态
     * 由KCRemoveBlockPacket在处理时调用
     */
    public static void setRemoveKeyPressed(UUID playerUUID, boolean pressed) {
        if (pressed) {
            removeKeyPressed.put(playerUUID, true);
        } else {
            removeKeyPressed.remove(playerUUID);
        }
    }

    /**
     * 检查指定玩家是否按下了移除键
     * 在InteractionBehaviour中调用此方法来判断是否应该执行移除逻辑
     */
    public static boolean isRemoveKeyPressed(UUID playerUUID) {
        return removeKeyPressed.getOrDefault(playerUUID, false);
    }
}
