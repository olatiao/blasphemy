package com.blasphemy.client

import com.blasphemy.Blasphemy
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

/** 客户端初始化类 */
@Environment(EnvType.CLIENT)
class BlasphemyClient : ClientModInitializer {

    override fun onInitializeClient() {
        Blasphemy.Companion.logger.info("初始化客户端...")

        // 这里只保留武器和附魔相关的客户端功能

        Blasphemy.Companion.logger.info("客户端初始化完成！")
    }
}
