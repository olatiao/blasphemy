package com.blasphemy.config

import com.blasphemy.Blasphemy
import com.google.gson.GsonBuilder
import java.io.FileReader
import java.io.FileWriter
import net.fabricmc.loader.api.FabricLoader

/** 模组配置类，负责管理和存储模组配置 */
object ModConfig {

    // 配置文件
    private val configFile =
            FabricLoader.getInstance().configDir.resolve("${Blasphemy.MOD_ID}.json").toFile()

    // 默认配置
    private val defaultConfig = Config()

    // 当前配置
    private var config = defaultConfig.copy()

    /** 加载配置 */
    fun load() {
        if (configFile.exists()) {
            try {
                FileReader(configFile).use { reader ->
                    val gson = GsonBuilder().setPrettyPrinting().create()
                    config = gson.fromJson(reader, Config::class.java)
                    Blasphemy.logger.info("配置文件已加载")
                }
            } catch (e: Exception) {
                Blasphemy.logger.error("加载配置文件失败，使用默认配置", e)
                config = defaultConfig.copy()
                save() // 保存默认配置
            }
        } else {
            Blasphemy.logger.info("配置文件不存在，创建默认配置")
            save() // 保存默认配置
        }
    }

    /** 保存配置 */
    fun save() {
        try {
            if (!configFile.parentFile.exists()) {
                configFile.parentFile.mkdirs()
            }

            FileWriter(configFile).use { writer ->
                val gson = GsonBuilder().setPrettyPrinting().create()
                gson.toJson(config, writer)
            }
            Blasphemy.logger.info("配置文件已保存")
        } catch (e: Exception) {
            Blasphemy.logger.error("保存配置文件失败", e)
        }
    }

    /** 获取当前配置 */
    fun getConfig(): Config {
        return config
    }

    /** 配置数据类 */
    data class Config(
            // 天使剑配置
            val seraphimSword: SeraphimSwordConfig = SeraphimSwordConfig(),

            // 急流剑配置
            val rapidsSword: RapidsSwordConfig = RapidsSwordConfig(),

            // 村正剑配置
            val muramasaSword: MuramasaSwordConfig = MuramasaSwordConfig(),

            // 群体斩击附魔配置
            val cleaveConfig: CleaveConfig = CleaveConfig(),

            // 传送门配置
            val portalConfig: PortalConfig = PortalConfig()
    )

    /** 天使剑配置数据类 */
    data class SeraphimSwordConfig(
            val baseDamage: Int = 7, // 基础伤害
            val attackSpeed: Float = -2.4f, // 攻击速度
            val specialDamageMultiplier: Float = 0.1f, // 特殊伤害乘数
            val pushStrength: Float = 1.5f, // 推力强度
            val cooldownSeconds: Int = 60, // 冷却时间
            val particleCount: Int = 40, // 粒子效果数量
            val enableParticles: Boolean = true, // 是否启用粒子效果
            val undeadDamageMultiplier: Float = 0.3f, // 亡灵伤害倍率
            val undeadEntityTags: List<String> =
                    listOf( // 亡灵实体标签列表
                            "minecraft:undead",
                            "forge:undead",
                            "c:undead"
                    )
    )

    /** 急流剑配置数据类 */
    data class RapidsSwordConfig(
            val baseDamage: Int = 6, // 基础伤害
            val attackSpeed: Float = -1.9f, // 攻击速度
            val specialDamageMultiplier: Float = 0.05f, // 特殊伤害乘数
            val executionThreshold: Float = 0.2f, // 斩杀阈值
            val executionChance: Float = 0.3f // 斩杀几率
    )

    /** 村正剑配置数据类 */
    data class MuramasaSwordConfig(
            val baseDamage: Int = 8, // 基础伤害
            val attackSpeed: Float = -2.6f, // 攻击速度
            val specialDamageMultiplier: Float = 0.025f, // 特殊伤害乘数
            val executionThreshold: Float = 0.2f, // 斩杀阈值
            val executionChance: Float = 0.3f, // 斩杀几率
            val armorThreshold: Int = 10 // 护甲阈值
    )

    /** 群体斩击附魔配置数据类 */
    data class CleaveConfig(
            val attackRange: Double = 3.0, // 攻击范围（方块）
            val baseMultiplier: Float = 0.15f, // 基础伤害倍率（15%）
            val levelMultiplier: Float = 0.05f, // 每级增加的伤害倍率（5%）
            val maxLevel: Int = 5 // 最大等级
    )

    /** 传送门配置数据类 */
    data class PortalConfig(
            val enabled: Boolean = true, // 是否启用自定义传送门
            val portalBlocks: List<String> =
                    listOf( // 允许的传送门方块ID列表
                            "minecraft:obsidian",
                            "minecraft:crying_obsidian"
                    ),
            val ignitionItem: String = "minecraft:flint_and_steel", // 开启传送门的物品ID
            val messages: PortalMessages = PortalMessages() // 提示语配置
    )

    /** 传送门提示语配置数据类 */
    data class PortalMessages(
            val invalidBlock: String = "message.blasphemy.portal.invalid_block", // 使用错误方块时的提示
            val invalidItem: String = "message.blasphemy.portal.invalid_item", // 使用错误物品时的提示
            val portalCreated: String = "message.blasphemy.portal.created" // 传送门创建成功时的提示
    )
}
