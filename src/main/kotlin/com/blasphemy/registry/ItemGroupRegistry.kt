package com.blasphemy.registry

import com.blasphemy.Blasphemy
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.text.Text
import net.minecraft.util.Identifier

/**
 * 物品组注册类，负责注册模组中的物品组
 */
object ItemGroupRegistry {
    
    // 模组主物品组
    val MAIN_GROUP: ItemGroup = FabricItemGroup.builder()
        .icon { ItemStack(ItemRegistry.SERAPHIM_SWORD) } // 使用天使剑作为图标
        .displayName(Text.translatable("itemGroup.blasphemy.main"))
        .entries { _, entries ->
            // 添加武器
            entries.add(ItemRegistry.SERAPHIM_SWORD)
            entries.add(ItemRegistry.RAPIDS_SWORD)
            entries.add(ItemRegistry.MURAMASA_SWORD)
            
            // 未来可以在这里添加更多物品
        }
        .build()
    
    /**
     * 注册所有物品组
     */
    fun register() {
        Registry.register(Registries.ITEM_GROUP, Identifier(Blasphemy.MOD_ID, "main"), MAIN_GROUP)
        
        Blasphemy.logger.info("物品组注册完成")
    }
} 