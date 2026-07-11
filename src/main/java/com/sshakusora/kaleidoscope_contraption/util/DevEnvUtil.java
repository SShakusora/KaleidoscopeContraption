package com.sshakusora.kaleidoscope_contraption.util;

import net.neoforged.fml.loading.FMLLoader;

/**
 * 开发环境检测工具类
 */
public class DevEnvUtil {

    private static final boolean IS_DEV_ENVIRONMENT = !FMLLoader.isProduction();

    /**
     * 检查当前是否在开发环境中运行
     * @return true如果是开发环境，false如果是生产环境
     */
    public static boolean isDevEnvironment() {
        return IS_DEV_ENVIRONMENT;
    }
}
