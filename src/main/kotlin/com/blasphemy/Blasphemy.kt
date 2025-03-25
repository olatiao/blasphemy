package com.blasphemy

import com.blasphemy.compat.LeylinesCompat
import com.blasphemy.config.ModConfig
import com.blasphemy.registry.EnchantmentRegistry
import com.blasphemy.registry.ItemGroupRegistry
import com.blasphemy.registry.ItemRegistry
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory

/** 模组主类，负责初始化并注册所有内容 */
class Blasphemy : ModInitializer {

    companion object {
        // 模组ID
        const val MOD_ID = "blasphemy"

        // 日志记录器
        val logger = LoggerFactory.getLogger(MOD_ID)

        // 兼容性检查
        val BETTERCOMBAT_ENABLED = FabricLoader.getInstance().isModLoaded("bettercombat")
        val LEYLINES_ENABLED = FabricLoader.getInstance().isModLoaded("l2complements") // 莱特兰模组
    }

    /** 模组初始化方法 */
    override fun onInitialize() {
        logger.info("正在初始化武器和附魔模组...")

        // 加载配置
        ModConfig.load()

        // 注册附魔
        EnchantmentRegistry.register()

        // 注册物品和武器
        ItemRegistry.register()

        // 注册物品组
        ItemGroupRegistry.register()

        // 初始化兼容性
        if (LEYLINES_ENABLED) {
            logger.info("检测到莱特兰模组，正在加载兼容...")
            LeylinesCompat.init()
        }

        logger.info("武器和附魔模组初始化完成！")
    }
}
