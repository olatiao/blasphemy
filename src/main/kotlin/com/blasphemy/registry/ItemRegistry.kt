package com.blasphemy.registry

import com.blasphemy.Blasphemy
import com.blasphemy.config.ModConfig
import com.blasphemy.item.NpcSpawnEggItem
import com.blasphemy.item.weapon.SpecialSwordItem
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.item.Item
import net.minecraft.item.ToolMaterials
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import net.minecraft.util.Rarity

/**
 * 物品注册类，负责注册模组中的自定义物品和武器
 */
object ItemRegistry {
    
    // 从配置中获取武器属性
    private val config = ModConfig.getConfig()
    
    // 特殊武器 - 天使剑
    val SERAPHIM_SWORD = register(
        "seraphim_sword",
        SpecialSwordItem(
            ToolMaterials.NETHERITE,
            config.seraphimSword.baseDamage, 
            config.seraphimSword.attackSpeed,
            FabricItemSettings().fireproof().rarity(Rarity.EPIC),
            SpecialSwordItem.SwordType.SERAPHIM
        )
    )
    
    // 特殊武器 - 急流剑
    val RAPIDS_SWORD = register(
        "rapids_sword",
        SpecialSwordItem(
            ToolMaterials.DIAMOND,
            config.rapidsSword.baseDamage, 
            config.rapidsSword.attackSpeed,
            FabricItemSettings().rarity(Rarity.RARE),
            SpecialSwordItem.SwordType.RAPIDS
        )
    )
    
    // 特殊武器 - 村正剑
    val MURAMASA_SWORD = register(
        "muramasa_sword",
        SpecialSwordItem(
            ToolMaterials.NETHERITE,
            config.muramasaSword.baseDamage, 
            config.muramasaSword.attackSpeed,
            FabricItemSettings().fireproof().rarity(Rarity.EPIC),
            SpecialSwordItem.SwordType.MURAMASA
        )
    )
    
    // NPC生成蛋
    val NPC_SPAWN_EGG = register(
        "npc_spawn_egg",
        NpcSpawnEggItem(FabricItemSettings().rarity(Rarity.UNCOMMON))
    )

    /**
     * 辅助方法，用于注册物品并返回该物品
     */
    private fun <T : Item> register(id: String, item: T): T {
        return Registry.register(Registries.ITEM, Identifier("blasphemy", id), item)
    }

    /**
     * 注册所有物品
     */
    fun register() {
        // 物品已经在类初始化时注册，这里可以添加更多注册逻辑
        Blasphemy.logger.info("物品注册完成")
    }
} 