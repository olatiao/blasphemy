package com.blasphemy.items;

import com.blasphemy.config.ModConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * 妖刀村正武器类
 * 基础伤害高，对高护甲目标有额外伤害
 */
public class MuramasaSword extends BaseSword {
    
    public MuramasaSword(ToolMaterial material, int attackDamage, float attackSpeed, Settings settings) {
        super(material, attackDamage, attackSpeed, settings, "muramasa_sword");
    }
    
    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // 基础伤害处理
        boolean result = super.postHit(stack, target, attacker);
        
        if (!target.getWorld().isClient) {
            float extraDamage = 0;
            float targetHealthRatio = target.getHealth() / target.getMaxHealth();
            
            // 计算目标的护甲值
            double armorValue = target.getAttributeValue(EntityAttributes.GENERIC_ARMOR);
            
            // 对高护甲目标造成额外伤害
            if (armorValue >= ModConfig.getConfig().muramasaSword.armorThreshold) {
                // 护甲穿透效果
                extraDamage += armorValue * ModConfig.getConfig().muramasaSword.specialDamageMultiplier;
                
                // 播放穿透护甲特效
                target.getWorld().playSound(null, target.getX(), target.getY(), target.getZ(), 
                        SoundEvents.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 0.8f, 1.2f);
                
                if (target.getWorld() instanceof ServerWorld serverWorld) {
                    // 生成护甲破碎粒子
                    serverWorld.spawnParticles(
                            ParticleTypes.CRIT,
                            target.getX(), target.getY() + 1.0, target.getZ(),
                            15, 0.5, 0.5, 0.5, 0.1
                    );
                }
                
                // 通知攻击者
                if (attacker instanceof PlayerEntity player) {
                    player.sendMessage(Text.literal("护甲穿透!").formatted(Formatting.RED), true);
                }
            }
            
            // 斩杀效果 - 如果目标生命值低于阈值，有几率直接斩杀
            if (targetHealthRatio <= ModConfig.getConfig().muramasaSword.executionThreshold) {
                float roll = target.getWorld().random.nextFloat();
                if (roll <= ModConfig.getConfig().muramasaSword.executionChance) {
                    // 斩杀成功
                    extraDamage = 100; // 足够大的伤害值，确保斩杀
                    
                    // 效果展示
                    if (attacker instanceof PlayerEntity player) {
                        player.sendMessage(Text.literal("斩杀!").formatted(Formatting.DARK_RED), true);
                    }
                    
                    // 播放音效与粒子效果
                    target.getWorld().playSound(null, target.getX(), target.getY(), target.getZ(), 
                            SoundEvents.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.PLAYERS, 0.5f, 1.5f);
                    
                    if (target.getWorld() instanceof ServerWorld serverWorld) {
                        serverWorld.spawnParticles(
                                ParticleTypes.SOUL,
                                target.getX(), target.getY() + 1.0, target.getZ(),
                                20, 0.5, 0.5, 0.5, 0.05
                        );
                    }
                }
            }
            
            // 应用额外伤害
            if (extraDamage > 0) {
                target.damage(target.getDamageSources().generic(), extraDamage);
            }
        }
        
        return result;
    }
    
    @Override
    public boolean isDamageable() {
        return true;
    }
} 