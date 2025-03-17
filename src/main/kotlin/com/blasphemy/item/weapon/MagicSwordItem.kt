package com.blasphemy.item.weapon

import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.item.SwordItem
import net.minecraft.item.ToolMaterial

/**
 * 魔法剑武器类，基础实现
 */
open class MagicSwordItem(
    material: ToolMaterial,
    attackDamage: Int,
    attackSpeed: Float,
    settings: Settings
) : SwordItem(material, attackDamage, attackSpeed, settings) {

    private val mainHandAttributes: Multimap<EntityAttribute, EntityAttributeModifier>
    private val offHandAttributes: Multimap<EntityAttribute, EntityAttributeModifier>

    init {
        val mhBuilder = ImmutableMultimap.builder<EntityAttribute, EntityAttributeModifier>()
        val ohBuilder = ImmutableMultimap.builder<EntityAttribute, EntityAttributeModifier>()
        
        // 添加基础武器属性（攻击伤害和攻击速度）
        mhBuilder.put(
            EntityAttributes.GENERIC_ATTACK_DAMAGE,
            EntityAttributeModifier(
                ATTACK_DAMAGE_MODIFIER_ID,
                "Weapon modifier",
                attackDamage.toDouble(),
                EntityAttributeModifier.Operation.ADDITION
            )
        )
        
        mhBuilder.put(
            EntityAttributes.GENERIC_ATTACK_SPEED,
            EntityAttributeModifier(
                ATTACK_SPEED_MODIFIER_ID,
                "Weapon modifier",
                attackSpeed.toDouble(),
                EntityAttributeModifier.Operation.ADDITION
            )
        )
        
        mainHandAttributes = mhBuilder.build()
        offHandAttributes = ohBuilder.build()
    }

    /**
     * 获取设备槽属性修饰符
     */
    override fun getAttributeModifiers(slot: EquipmentSlot): Multimap<EntityAttribute, EntityAttributeModifier> {
        return when (slot) {
            EquipmentSlot.MAINHAND -> mainHandAttributes
            EquipmentSlot.OFFHAND -> offHandAttributes
            else -> super.getAttributeModifiers(slot)
        }
    }
} 