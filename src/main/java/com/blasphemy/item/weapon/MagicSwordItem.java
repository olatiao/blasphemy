package com.blasphemy.item.weapon;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;

/**
 * 魔法剑武器类，基础实现
 */
public class MagicSwordItem extends SwordItem {
    private final Multimap<EntityAttribute, EntityAttributeModifier> mainHandAttributes;
    private final Multimap<EntityAttribute, EntityAttributeModifier> offHandAttributes;

    public MagicSwordItem(
        ToolMaterial material,
        int attackDamage,
        float attackSpeed,
        Settings settings
    ) {
        super(material, attackDamage, attackSpeed, settings);
        
        ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> mhBuilder = ImmutableMultimap.builder();
        ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> ohBuilder = ImmutableMultimap.builder();
        
        // 添加基础武器属性（攻击伤害和攻击速度）
        mhBuilder.put(
            EntityAttributes.GENERIC_ATTACK_DAMAGE,
            new EntityAttributeModifier(
                ATTACK_DAMAGE_MODIFIER_ID,
                "Weapon modifier",
                attackDamage,
                EntityAttributeModifier.Operation.ADDITION
            )
        );
        
        mhBuilder.put(
            EntityAttributes.GENERIC_ATTACK_SPEED,
            new EntityAttributeModifier(
                ATTACK_SPEED_MODIFIER_ID,
                "Weapon modifier",
                attackSpeed,
                EntityAttributeModifier.Operation.ADDITION
            )
        );
        
        mainHandAttributes = mhBuilder.build();
        offHandAttributes = ohBuilder.build();
    }

    /**
     * 获取设备槽属性修饰符
     */
    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> mainHandAttributes;
            case OFFHAND -> offHandAttributes;
            default -> super.getAttributeModifiers(slot);
        };
    }
} 