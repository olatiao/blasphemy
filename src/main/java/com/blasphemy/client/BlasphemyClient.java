package com.blasphemy.client;

import com.blasphemy.Blasphemy;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** 客户端初始化类 */
@Environment(EnvType.CLIENT)
public class BlasphemyClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Blasphemy.LOGGER.info("初始化客户端...");

        // 这里只保留武器和附魔相关的客户端功能

        Blasphemy.LOGGER.info("客户端初始化完成！");
    }
} 