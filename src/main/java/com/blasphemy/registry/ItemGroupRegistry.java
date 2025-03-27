package com.blasphemy.registry;

import com.blasphemy.Blasphemy;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * 物品组注册表类
 * 负责注册模组的物品分类组
 */
public class ItemGroupRegistry {
    // 主物品组
    public static final RegistryKey<ItemGroup> BLASPHEMY_GROUP = RegistryKey.of(
            RegistryKeys.ITEM_GROUP,
            new Identifier(Blasphemy.MOD_ID, "main")
    );
    
    /**
     * 注册物品组
     */
    public static void register() {
        Blasphemy.LOGGER.info("正在注册物品组...");
        
        // 注册主物品组
        Registry.register(Registries.ITEM_GROUP, BLASPHEMY_GROUP, FabricItemGroup.builder()
                .displayName(Text.translatable("itemGroup." + Blasphemy.MOD_ID + ".main"))
                .icon(() -> new ItemStack(Registries.ITEM.get(new Identifier("minecraft:nether_star"))))
                .build()
        );
        
        Blasphemy.LOGGER.info("物品组注册完成！");
    }
} 