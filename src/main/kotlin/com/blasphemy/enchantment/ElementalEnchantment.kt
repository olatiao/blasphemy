package com.blasphemy.enchantment

import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentTarget
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.ItemStack
import kotlin.random.Random

/**
 * 元素附魔类，为武器添加元素效果
 */
class ElementalEnchantment(
    private val elementType: ElementType,
    weight: Rarity,
    target: EnchantmentTarget = EnchantmentTarget.WEAPON,
    equipmentSlots: Array<EquipmentSlot> = arrayOf(EquipmentSlot.MAINHAND)
) : Enchantment(weight, target, equipmentSlots) {

    /**
     * 元素类型枚举
     */
    enum class ElementType {
        FIRE,   // 火焰元素
        ICE,    // 冰霜元素
        SHADOW  // 暗影元素
    }

    /**
     * 获取最小附魔等级
     */
    override fun getMinPower(level: Int): Int {
        return 5 + (level - 1) * 8
    }

    /**
     * 获取最大附魔等级
     */
    override fun getMaxPower(level: Int): Int {
        return getMinPower(level) + 20
    }

    /**
     * 获取最大附魔等级
     */
    override fun getMaxLevel(): Int {
        return 3
    }

    /**
     * 当攻击实体时触发附魔效果
     */
    override fun onTargetDamaged(user: LivingEntity, target: Entity, level: Int) {
        if (target !is LivingEntity) return
        
        val chance = 0.15f * level
        if (Random.nextFloat() <= chance) {
            when (elementType) {
                ElementType.FIRE -> {
                    // 火焰元素：点燃目标
                    target.fireTicks = 20 * level
                    target.addStatusEffect(StatusEffectInstance(StatusEffects.WEAKNESS, 100 * level, 0))
                }
                ElementType.ICE -> {
                    // 冰霜元素：减速目标
                    target.addStatusEffect(StatusEffectInstance(StatusEffects.SLOWNESS, 100 * level, level - 1))
                    target.addStatusEffect(StatusEffectInstance(StatusEffects.MINING_FATIGUE, 60 * level, 0))
                }
                ElementType.SHADOW -> {
                    // 暗影元素：致盲和虚弱目标
                    target.addStatusEffect(StatusEffectInstance(StatusEffects.BLINDNESS, 60 * level, 0))
                    target.addStatusEffect(StatusEffectInstance(StatusEffects.WEAKNESS, 80 * level, 0))
                }
            }
        }
    }

    /**
     * 检查是否可以接受其他附魔
     */
    override fun canAccept(other: Enchantment): Boolean {
        // 不能与其他元素附魔共存
        return !(other is ElementalEnchantment) && super.canAccept(other)
    }

    /**
     * 检查指定物品是否可以被附魔
     */
    override fun isAcceptableItem(stack: ItemStack): Boolean {
        // 任何武器类物品都可以接受此附魔
        return super.isAcceptableItem(stack)
    }
} 