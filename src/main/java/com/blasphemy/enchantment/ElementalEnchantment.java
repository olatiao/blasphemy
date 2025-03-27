package com.blasphemy.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;

import java.util.Random;

/**
 * 元素附魔类，为武器添加元素效果
 */
public class ElementalEnchantment extends Enchantment {
    private final ElementType elementType;
    private static final Random random = new Random();

    /**
     * 元素类型枚举
     */
    public enum ElementType {
        FIRE,   // 火焰元素
        ICE,    // 冰霜元素
        SHADOW  // 暗影元素
    }

    /**
     * 构造函数
     */
    public ElementalEnchantment(
        ElementType elementType,
        Rarity weight,
        EnchantmentTarget target,
        EquipmentSlot[] equipmentSlots
    ) {
        super(weight, target, equipmentSlots);
        this.elementType = elementType;
    }

    /**
     * 构造函数（使用默认参数）
     */
    public ElementalEnchantment(ElementType elementType, Rarity weight) {
        this(elementType, weight, EnchantmentTarget.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    /**
     * 获取最小附魔等级
     */
    @Override
    public int getMinPower(int level) {
        return 5 + (level - 1) * 8;
    }

    /**
     * 获取最大附魔等级
     */
    @Override
    public int getMaxPower(int level) {
        return getMinPower(level) + 20;
    }

    /**
     * 获取最大附魔等级
     */
    @Override
    public int getMaxLevel() {
        return 3;
    }

    /**
     * 当攻击实体时触发附魔效果
     */
    @Override
    public void onTargetDamaged(LivingEntity user, Entity target, int level) {
        if (!(target instanceof LivingEntity livingTarget)) return;
        
        float chance = 0.15f * level;
        if (random.nextFloat() <= chance) {
            switch (elementType) {
                case FIRE -> {
                    // 火焰元素：点燃目标
                    livingTarget.setFireTicks(20 * level);
                    livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100 * level, 0));
                }
                case ICE -> {
                    // 冰霜元素：减速目标
                    livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100 * level, level - 1));
                    livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 60 * level, 0));
                }
                case SHADOW -> {
                    // 暗影元素：致盲和虚弱目标
                    livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60 * level, 0));
                    livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 80 * level, 0));
                }
            }
        }
    }

    /**
     * 检查是否可以接受其他附魔
     */
    @Override
    public boolean canAccept(Enchantment other) {
        // 不能与其他元素附魔共存
        return !(other instanceof ElementalEnchantment) && super.canAccept(other);
    }

    /**
     * 检查指定物品是否可以被附魔
     */
    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        // 任何武器类物品都可以接受此附魔
        return super.isAcceptableItem(stack);
    }
} 