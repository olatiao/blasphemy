package com.blasphemy.items;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 基础剑类，提取共同功能
 */
public abstract class BaseSword extends SwordItem {
    
    protected final String tooltipKey;
    
    public BaseSword(ToolMaterial material, int attackDamage, float attackSpeed, Settings settings, String id) {
        super(material, attackDamage, attackSpeed, settings);
        this.tooltipKey = "item.blasphemy." + id + ".tooltip";
    }
    
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        
        // 添加自定义描述，直接从语言文件获取
        String[] lines = Text.translatable(tooltipKey).getString().split("\n");
        for (String line : lines) {
            tooltip.add(Text.literal(line));
        }
    }
} 