package com.blasphemy;

import com.blasphemy.config.ModConfig;
import com.blasphemy.portal.PortalDebugTool;
import com.blasphemy.portal.PortalFrameValidator;
import com.blasphemy.registry.EnchantmentRegistry;
import com.blasphemy.registry.ItemGroupRegistry;
import com.blasphemy.registry.ItemRegistry;
import com.blasphemy.util.BlockEventListener;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * 模组主类
 */
public class Blasphemy implements ModInitializer {
	// 模组ID
	public static final String MOD_ID = "blasphemy";
	public static final String MOD_VERSION = "1.0.0";

	// 日志记录器
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Random RANDOM = new Random();

	@Override
	public void onInitialize() {
		LOGGER.info("粵神模组 {} 初始化中...", MOD_VERSION);
		
		// 加载配置
		ModConfig.load();
		
		// 注册物品组（必须在物品注册前完成）
		LOGGER.info("注册物品组...");
		ItemGroupRegistry.register();
		
		// 注册附魔
		LOGGER.info("注册附魔...");
		EnchantmentRegistry.register();
		
		// 注册物品
		LOGGER.info("注册物品...");
		ItemRegistry.register();
		
		// 注册传送门调试工具
		LOGGER.info("注册传送门调试工具...");
		PortalDebugTool.register();
		
		// 初始化传送门框架验证器
		LOGGER.info("初始化传送门框架验证器...");
		PortalFrameValidator.init();
		
		// 注册事件监听器
		LOGGER.info("注册方块事件监听器...");
		BlockEventListener.init();
		
		// 完成初始化
		LOGGER.info("粵神模组初始化完成!");
	}
}