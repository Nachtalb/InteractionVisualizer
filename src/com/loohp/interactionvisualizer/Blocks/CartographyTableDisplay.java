package com.loohp.interactionvisualizer.Blocks;

import java.util.HashMap;

import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.CartographyInventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.loohp.interactionvisualizer.InteractionVisualizer;
import com.loohp.interactionvisualizer.Utils.EntityCreator;
import com.loohp.interactionvisualizer.Utils.PacketSending;

public class CartographyTableDisplay implements Listener {
	
	public static HashMap<Block, HashMap<String, Object>> openedCTable = new HashMap<Block, HashMap<String, Object>>();
	

	@EventHandler
	public void onUseCartographyTable(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		if (player.getGameMode().equals(GameMode.SPECTATOR)) {
			return;
		}
		if (event.getView().getTopInventory() == null) {
			return;
		}
		if (!(event.getView().getTopInventory() instanceof CartographyInventory)) {
			return;
		}
		if (!player.getTargetBlockExact(7, FluidCollisionMode.NEVER).getType().equals(Material.CARTOGRAPHY_TABLE)) {
			return;
		}
		
		if (event.getRawSlot() <= 1) {
			PacketSending.sendHandMovement(InteractionVisualizer.getOnlinePlayers(), (Player) event.getWhoClicked());
		}
	}
	
	@EventHandler
	public void onDragCartographyTable(InventoryDragEvent event) {
		Player player = (Player) event.getWhoClicked();
		if (player.getGameMode().equals(GameMode.SPECTATOR)) {
			return;
		}
		if (event.getView().getTopInventory() == null) {
			return;
		}
		if (!(event.getView().getTopInventory() instanceof CartographyInventory)) {
			return;
		}
		if (!player.getTargetBlockExact(7, FluidCollisionMode.NEVER).getType().equals(Material.CARTOGRAPHY_TABLE)) {
			return;
		}
		
		for (int slot : event.getRawSlots()) {
			if (slot <= 1) {
				PacketSending.sendHandMovement(InteractionVisualizer.getOnlinePlayers(), (Player) event.getWhoClicked());
				break;
			}
		}
	}
	
	@EventHandler
	public void onCloseCartographyTable(InventoryCloseEvent event) {
		Player player = (Player) event.getPlayer();
		if (player.getGameMode().equals(GameMode.SPECTATOR)) {
			return;
		}
		if (event.getView().getTopInventory() == null) {
			return;
		}
		if (!(event.getView().getTopInventory() instanceof CartographyInventory)) {
			return;
		}
		if (!player.getTargetBlockExact(7, FluidCollisionMode.NEVER).getType().equals(Material.CARTOGRAPHY_TABLE)) {
			return;
		}
		
		Block block = player.getTargetBlockExact(7, FluidCollisionMode.NEVER);
		
		if (!openedCTable.containsKey(block)) {
			return;
		}
		
		HashMap<String, Object> map = openedCTable.get(block);
		if (!map.get("Player").equals((Player) event.getPlayer())) {
			return;
		}
		
		if (map.get("Item") instanceof ItemFrame) {
			Entity entity = (Entity) map.get("Item");
			PacketSending.removeItemFrame(InteractionVisualizer.getOnlinePlayers(), (ItemFrame) entity);
			entity.remove();
		}
		openedCTable.remove(block);
	}
	
	public static int run() {		
		return new BukkitRunnable() {
			public void run() {
				
				for (Player player : InteractionVisualizer.getOnlinePlayers()) {
					if (player.getGameMode().equals(GameMode.SPECTATOR)) {
						continue;
					}
					if (player.getOpenInventory() == null) {
						continue;
					}
					if (player.getOpenInventory().getTopInventory() == null) {
						continue;
					}
					if (!(player.getOpenInventory().getTopInventory() instanceof CartographyInventory)) {
						continue;
					}
					if (!player.getTargetBlockExact(7, FluidCollisionMode.NEVER).getType().equals(Material.CARTOGRAPHY_TABLE)) {
						continue;
					}
					
					InventoryView view = player.getOpenInventory();
					Block block = player.getTargetBlockExact(7, FluidCollisionMode.NEVER);
					if (!openedCTable.containsKey(block)) {
						HashMap<String, Object> map = new HashMap<String, Object>();
						map.put("Player", player);
						map.put("Item", "N/A");
						openedCTable.put(block, map);
					}
					HashMap<String, Object> map = openedCTable.get(block);
					
					if (!map.get("Player").equals(player)) {
						continue;
					}
				
					ItemStack input = view.getItem(0);
					if (input != null) {
						if (input.getType().equals(Material.AIR)) {
							input = null;
						}
					}
					ItemStack output = view.getItem(2);
					if (output != null) {
						if (output.getType().equals(Material.AIR)) {
							output = null;
						}
					}
					
					ItemStack itemstack = null;
					if (output == null) {
						if (input != null) {
							itemstack = input;
						}
					} else {
						itemstack = output;
					}
						
					ItemFrame item = null;
					if (map.get("Item") instanceof String) {
						if (itemstack != null) {
							item = (ItemFrame) EntityCreator.create(block.getRelative(BlockFace.UP).getLocation(), EntityType.ITEM_FRAME);
							item.setItem(itemstack, false);
							item.setFacingDirection(BlockFace.UP);
							map.put("Item", item);
							PacketSending.sendItemFrameSpawn(InteractionVisualizer.itemStand, item);
							PacketSending.updateItemFrame(InteractionVisualizer.getOnlinePlayers(), item);
						} else {
							map.put("Item", "N/A");
						}
					} else {
						item = (ItemFrame) map.get("Item");
						if (itemstack != null) {
							if (!item.getItem().equals(itemstack)) {
								item.setItem(itemstack, false);
								PacketSending.updateItemFrame(InteractionVisualizer.getOnlinePlayers(), item);
							}
							item.setGravity(false);
						} else {
							map.put("Item", "N/A");
							PacketSending.removeItemFrame(InteractionVisualizer.getOnlinePlayers(), item);
							item.remove();
						}
					}
				}
				
			}
		}.runTaskTimer(InteractionVisualizer.plugin, 0, 5).getTaskId();
	}
}