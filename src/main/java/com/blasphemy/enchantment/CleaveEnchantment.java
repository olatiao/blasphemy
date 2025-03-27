package com.blasphemy.enchantment;

import com.blasphemy.config.ModConfig;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;

import java.util.List;

/**
 * 群体斩击附魔
 * 攻击时可同时伤害周围敌人
 */
public class CleaveEnchantment extends Enchantment {
    
    public CleaveEnchantment() {
        super(Rarity.UNCOMMON, EnchantmentTarget.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }
    
    @Override
    public int getMinPower(int level) {
        return 5 + (level - 1) * 8;
    }
    
    @Override
    public int getMaxPower(int level) {
        return getMinPower(level) + 20;
    }
    
    @Override
    public int getMaxLevel() {
        return ModConfig.getConfig().cleaveConfig.maxLevel;
    }
    
    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return stack.getItem() instanceof SwordItem;
    }
    
    @Override
    public float getAttackDamage(int level, EntityGroup group) {
        return 0; // 不增加基础攻击伤害
    }
    
    @Override
    public void onTargetDamaged(LivingEntity user, Entity target, int level) {
        if (target instanceof LivingEntity livingTarget && user instanceof PlayerEntity player) {
            applyEffect(player, livingTarget, level);
        }
    }
    
    /**
     * 应用群体斩击效果
     */
    public static void applyEffect(PlayerEntity player, LivingEntity target, int level) {
        if (level <= 0 || player.getWorld().isClient) {
            return;
        }
        
        // 获取范围和伤害
        double range = ModConfig.getConfig().cleaveConfig.attackRange;
        float baseMultiplier = ModConfig.getConfig().cleaveConfig.baseMultiplier;
        float levelMultiplier = ModConfig.getConfig().cleaveConfig.levelMultiplier;
        float damageMultiplier = baseMultiplier + (level * levelMultiplier);
        
        // 获取玩家攻击伤害
        double attackDamage = player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        float cleaveDamage = (float) (attackDamage * damageMultiplier);
        
        // 获取范围内的所有生物
        List<Entity> entities = player.getWorld().getOtherEntities(player, 
                new Box(target.getX() - range, target.getY() - range, target.getZ() - range,
                        target.getX() + range, target.getY() + range, target.getZ() + range),
                entity -> entity instanceof LivingEntity && entity != player && entity != target);
        
        if (!entities.isEmpty()) {
            int hitCount = 0;
            
            // 应用效果到周围实体
            for (Entity entity : entities) {
                if (entity instanceof LivingEntity livingEntity && player.canSee(livingEntity)) {
                    livingEntity.damage(player.getDamageSources().playerAttack(player), cleaveDamage);
                    hitCount++;
                    
                    // 粒子效果
                    if (player.getWorld() instanceof ServerWorld serverWorld) {
                        serverWorld.spawnParticles(
                                ParticleTypes.SWEEP_ATTACK,
                                livingEntity.getX(), livingEntity.getY() + 0.5, livingEntity.getZ(),
                                5, 0.2, 0.2, 0.2, 0
                        );
                    }
                }
            }
            
            // 通知玩家
            if (hitCount > 0) {
                player.sendMessage(
                        Text.translatable("message.blasphemy.cleave.activate", hitCount)
                                .formatted(Formatting.RED),
                        true
                );
            }
        }
    }
} 