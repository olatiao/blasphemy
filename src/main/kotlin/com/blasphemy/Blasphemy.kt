package com.blasphemy

import com.blasphemy.compat.LeylinesCompat
import com.blasphemy.config.ModConfig
import com.blasphemy.dialogue.DialogueManager
import com.blasphemy.network.ServerNetworking
import com.blasphemy.registry.EnchantmentRegistry
import com.blasphemy.registry.EntityRegistry
import com.blasphemy.registry.ItemGroupRegistry
import com.blasphemy.registry.ItemRegistry
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.ResourceType
import net.minecraft.util.Identifier
import net.minecraft.util.profiler.Profiler
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import org.slf4j.LoggerFactory

/**
 * 模组主类，负责初始化并注册所有内容
 */
object Blasphemy : ModInitializer {
    // 模组ID
    const val MOD_ID = "blasphemy"
    
    // 日志记录器
    val logger = LoggerFactory.getLogger(MOD_ID)
    
    // 兼容性检查
    val BETTERCOMBAT_ENABLED = FabricLoader.getInstance().isModLoaded("bettercombat")
    val LEYLINES_ENABLED = FabricLoader.getInstance().isModLoaded("l2complements") // 莱特兰模组

    /**
     * 模组初始化方法
     */
    override fun onInitialize() {
        logger.info("正在初始化魔法武器模组...")
        
        // 加载配置
        ModConfig.load()
        
        // 注册附魔
        EnchantmentRegistry.register()
        
        // 注册物品和武器
        ItemRegistry.register()
        
        // 注册物品组
        ItemGroupRegistry.register()
        
        // 注册实体属性
        EntityRegistry.registerAttributes()
        
        // 注册服务器网络处理器
        ServerNetworking.registerServerNetworking()
        
        // 注册资源加载器，用于加载对话配置
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(
            object : SimpleResourceReloadListener<Unit> {
                override fun getFabricId(): Identifier = Identifier(MOD_ID, "dialogues")
                
                override fun load(
                    manager: ResourceManager,
                    profiler: Profiler,
                    executor: Executor
                ): CompletableFuture<Unit> {
                    return CompletableFuture.supplyAsync {
                        profiler.startTick()
                        profiler.push("加载对话配置")
                        
                        DialogueManager.loadDialogues(manager)
                        
                        profiler.pop()
                        profiler.endTick()
                    }
                }
                
                override fun apply(
                    data: Unit,
                    manager: ResourceManager,
                    profiler: Profiler,
                    executor: Executor
                ): CompletableFuture<Void> {
                    return CompletableFuture.runAsync {}
                }
            }
        )
        
        // 初始化兼容性
        if (LEYLINES_ENABLED) {
            logger.info("检测到莱特兰模组，正在加载兼容...")
            LeylinesCompat.init()
        }
        
        logger.info("亵渎武器模组初始化完成！")
    }
}