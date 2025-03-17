package com.blasphemy.registry

import com.blasphemy.enchantment.CleaveEnchantment
import net.minecraft.enchantment.Enchantment
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

/**
 * 附魔注册类，负责注册模组中的自定义附魔
 */
object EnchantmentRegistry {
    
    // 群体斩击附魔
    val CLEAVE = CleaveEnchantment(
        Enchantment.Rarity.RARE // 设置为稀有级别
    )

    /**
     * 注册所有附魔
     */
    fun register() {
        Registry.register(Registries.ENCHANTMENT, Identifier("blasphemy", "cleave"), CLEAVE)
    }
} 