package com.blasphemy.items;

import com.blasphemy.config.ModConfig;
import net.minecraft.entity.LivingEntity;
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
 * 激流之剑武器类
 * 攻击速度更快，对低血量目标有斩杀效果
 */
public class RapidsSword extends BaseSword {

    public RapidsSword(ToolMaterial material, int attackDamage, float attackSpeed, Settings settings) {
        super(material, attackDamage, attackSpeed, settings, "rapids_sword");
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // 基础伤害处理
        boolean result = super.postHit(stack, target, attacker);

        if (!target.getWorld().isClient) {
            float targetHealthRatio = target.getHealth() / target.getMaxHealth();
            float maxHealthDamage = target.getMaxHealth() * ModConfig.getConfig().rapidsSword.specialDamageMultiplier;

            // 造成基于目标最大生命值的额外伤害
            target.damage(target.getDamageSources().generic(), maxHealthDamage);

            // 斩杀效果 - 如果目标生命值低于阈值，有几率直接斩杀
            if (targetHealthRatio <= ModConfig.getConfig().rapidsSword.executionThreshold) {
                float roll = target.getWorld().random.nextFloat();
                if (roll <= ModConfig.getConfig().rapidsSword.executionChance) {
                    // 斩杀成功
                    target.damage(target.getDamageSources().generic(), 100); // 足够大的伤害值，确保斩杀

                    // 效果展示
                    if (attacker instanceof PlayerEntity player) {
                        player.sendMessage(Text.literal("斩杀!").formatted(Formatting.RED), true);
                    }

                    // 播放音效与粒子效果
                    target.getWorld().playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0f, 1.2f);

                    if (target.getWorld() instanceof ServerWorld serverWorld) {
                        serverWorld.spawnParticles(
                                ParticleTypes.SWEEP_ATTACK,
                                target.getX(), target.getY() + 1.0, target.getZ(),
                                10, 0.5, 0.5, 0.5, 0.1);
                    }
                }
            }
        }

        return result;
    }
}