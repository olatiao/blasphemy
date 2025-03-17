package com.blasphemy.compat

import com.blasphemy.Blasphemy
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * 莱兰特模组兼容类
 * 用于处理与莱兰特模组的兼容性功能
 */
object LeylinesCompat {
    
    // 魂火效果的ID (根据莱兰特模组实际ID可能需要调整)
    private const val SOUL_FIRE_EFFECT_ID = "l2complements:flame"
    
    // 缓存魂火效果实例
    private var soulFireEffect: StatusEffect? = null
    
    /**
     * 初始化兼容
     */
    fun init() {
        if (Blasphemy.LEYLINES_ENABLED) {
            // 尝试获取魂火效果
            try {
                soulFireEffect = Registries.STATUS_EFFECT.get(Identifier(SOUL_FIRE_EFFECT_ID))
                if (soulFireEffect != null) {
                    Blasphemy.logger.info("成功加载莱兰特模组魂火效果")
                } else {
                    Blasphemy.logger.warn("无法找到莱兰特模组的魂火效果")
                }
            } catch (e: Exception) {
                Blasphemy.logger.error("加载莱兰特模组魂火效果时出错", e)
            }
        }
    }
    
    /**
     * 应用魂火效果
     * @param entity 目标实体
     * @param level 效果等级
     * @param duration 持续时间(以刻为单位)
     * @return 是否成功应用效果
     */
    fun applySoulFireEffect(entity: LivingEntity, level: Int, duration: Int): Boolean {
        if (!Blasphemy.LEYLINES_ENABLED || soulFireEffect == null) return false
        
        try {
            entity.addStatusEffect(StatusEffectInstance(soulFireEffect!!, duration, level - 1))
            return true
        } catch (e: Exception) {
            Blasphemy.logger.error("应用魂火效果时出错", e)
            return false
        }
    }
} 