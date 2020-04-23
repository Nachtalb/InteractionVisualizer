package com.loohp.interactionvisualizer.Blocks;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import thito.beaconplus.objects.BeaconData;
import thito.beaconplus.BeaconPlusCore;

import com.loohp.interactionvisualizer.InteractionVisualizer;
import com.loohp.interactionvisualizer.Holder.ArmorStand;
import com.loohp.interactionvisualizer.Manager.CustomBlockDataManager;
import com.loohp.interactionvisualizer.Manager.EffectManager;
import com.loohp.interactionvisualizer.Manager.PacketManager;
import com.loohp.interactionvisualizer.Manager.PlayerRangeManager;
import com.loohp.interactionvisualizer.Manager.TileEntityManager;
import com.loohp.interactionvisualizer.Utils.RomanNumberUtils;

import net.md_5.bungee.api.ChatColor;
import thito.beaconplus.objects.BeaconEffect;

import static thito.beaconplus.BeaconPlusCore.getRangeByPower;

public class BeaconDisplay implements Listener {
	
	public static ConcurrentHashMap<Block, HashMap<String, Object>> beaconMap = new ConcurrentHashMap<Block, HashMap<String, Object>>();
	public static ConcurrentHashMap<Block, float[]> placemap = new ConcurrentHashMap<Block, float[]>();
	private static int checkingPeriod = InteractionVisualizer.beaconChecking;
	private static int gcPeriod = InteractionVisualizer.gcPeriod;

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlaceBeacon(BlockPlaceEvent event) {
		if (event.isCancelled()) {
			return;
		}
		Block block = event.getBlockPlaced();
		if (beaconMap.containsKey(block)) {
			return;
		}

		if (!block.getType().equals(Material.BEACON)) {
			return;
		}

		placemap.put(block, new float[]{event.getPlayer().getLocation().getYaw(), event.getPlayer().getLocation().getPitch()});
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onBreakBeacon(BlockBreakEvent event) {
		if (event.isCancelled()) {
			return;
		}
		Block block = event.getBlock();
		if (!beaconMap.containsKey(block)) {
			return;
		}

		HashMap<String, Object> map = beaconMap.get(block);
		if (map.get("1") instanceof ArmorStand) {
			ArmorStand stand = (ArmorStand) map.get("1");
			PacketManager.removeArmorStand(InteractionVisualizer.getOnlinePlayers(), stand);
		}
		if (map.get("2") instanceof ArmorStand) {
			ArmorStand stand = (ArmorStand) map.get("2");
			PacketManager.removeArmorStand(InteractionVisualizer.getOnlinePlayers(), stand);
		}
		if (map.get("3") instanceof ArmorStand) {
			ArmorStand stand = (ArmorStand) map.get("3");
			PacketManager.removeArmorStand(InteractionVisualizer.getOnlinePlayers(), stand);
		}
		beaconMap.remove(block);
		CustomBlockDataManager.removeBlock(CustomBlockDataManager.locKey(block.getLocation()));
	}

	public static int gc() {
		return Bukkit.getScheduler().runTaskTimerAsynchronously(InteractionVisualizer.plugin, () -> {
			Iterator<Entry<Block, HashMap<String, Object>>> itr = beaconMap.entrySet().iterator();
			int count = 0;
			int maxper = (int) Math.ceil((double) beaconMap.size() / (double) gcPeriod);
			int delay = 1;
			while (itr.hasNext()) {
				count++;
				if (count > maxper) {
					count = 0;
					delay++;
				}
				Entry<Block, HashMap<String, Object>> entry = itr.next();
				Bukkit.getScheduler().runTaskLater(InteractionVisualizer.plugin, () -> {
					Block block = entry.getKey();
					boolean active = false;
					if (isActive(block.getLocation())) {
						active = true;
					}
					if (active == false) {
						HashMap<String, Object> map = entry.getValue();
						if (map.get("1") instanceof ArmorStand) {
							ArmorStand stand = (ArmorStand) map.get("1");
							PacketManager.removeArmorStand(InteractionVisualizer.getOnlinePlayers(), stand);
						}
						if (map.get("2") instanceof ArmorStand) {
							ArmorStand stand = (ArmorStand) map.get("2");
							PacketManager.removeArmorStand(InteractionVisualizer.getOnlinePlayers(), stand);
						}
						if (map.get("3") instanceof ArmorStand) {
							ArmorStand stand = (ArmorStand) map.get("3");
							PacketManager.removeArmorStand(InteractionVisualizer.getOnlinePlayers(), stand);
						}
						beaconMap.remove(block);
						return;
					}
					if (!block.getType().equals(Material.BEACON)) {
						HashMap<String, Object> map = entry.getValue();
						if (map.get("1") instanceof ArmorStand) {
							ArmorStand stand = (ArmorStand) map.get("1");
							PacketManager.removeArmorStand(InteractionVisualizer.getOnlinePlayers(), stand);
						}
						if (map.get("2") instanceof ArmorStand) {
							ArmorStand stand = (ArmorStand) map.get("2");
							PacketManager.removeArmorStand(InteractionVisualizer.getOnlinePlayers(), stand);
						}
						if (map.get("3") instanceof ArmorStand) {
							ArmorStand stand = (ArmorStand) map.get("3");
							PacketManager.removeArmorStand(InteractionVisualizer.getOnlinePlayers(), stand);
						}
						beaconMap.remove(block);
						CustomBlockDataManager.removeBlock(CustomBlockDataManager.locKey(block.getLocation()));
						return;
					}
				}, delay);
			}
		}, 0, gcPeriod).getTaskId();
	}

	public static int run() {
		return Bukkit.getScheduler().runTaskTimerAsynchronously(InteractionVisualizer.plugin, () -> {
			Bukkit.getScheduler().runTask(InteractionVisualizer.plugin, () -> {
				List<Block> list = nearbyBeacon();
				for (Block block : list) {
					if (beaconMap.get(block) == null && isActive(block.getLocation())) {
						if (block.getType().equals(Material.BEACON)) {
							HashMap<String, Object> map = new HashMap<String, Object>();
							map.put("Item", "N/A");
							boolean done = false;
							HashMap<String, Object> datamap = CustomBlockDataManager.getBlock(CustomBlockDataManager.locKey(block.getLocation()));
							if (datamap != null) {
								try {
									String data = (String) datamap.get("Directional");
									BlockFace face = BlockFace.valueOf(data);
									map.putAll(spawnArmorStands(block, face));
									done = true;
								} catch (Exception e) {
									done = false;
								}
							}
							if (!done) {
								float[] dir = placemap.containsKey(block) ? placemap.remove(block) : new float[]{0.0F, 0.0F};
								BlockFace face = getCardinalFacing(dir);
								map.putAll(spawnArmorStands(block, face));
								HashMap<String, Object> savemap = (datamap != null) ? datamap : new HashMap<String, Object>();
								savemap.put("Directional", face.toString().toUpperCase());
								savemap.put("BlockType", block.getType().toString().toUpperCase());
								CustomBlockDataManager.setBlock(CustomBlockDataManager.locKey(block.getLocation()), savemap);
							}
							beaconMap.put(block, map);
						}
					}
				}
			});

			Iterator<Entry<Block, HashMap<String, Object>>> itr = beaconMap.entrySet().iterator();
			int count = 0;
			int maxper = (int) Math.ceil((double) beaconMap.size() / (double) checkingPeriod);
			int delay = 1;
			while (itr.hasNext()) {
				Entry<Block, HashMap<String, Object>> entry = itr.next();

				count++;
				if (count > maxper) {
					count = 0;
					delay++;
				}
				Bukkit.getScheduler().runTaskLater(InteractionVisualizer.plugin, () -> {
					Block block = entry.getKey();

					if (!isActive(block.getLocation())) {
						return;
					}

					if (!block.getType().equals(Material.BEACON)) {
						return;
					}

					ArmorStand line1 = (ArmorStand) entry.getValue().get("1");
					ArmorStand line2 = (ArmorStand) entry.getValue().get("2");
					ArmorStand line3 = (ArmorStand) entry.getValue().get("3");

					org.bukkit.block.Beacon beacon = (org.bukkit.block.Beacon) block.getState();

					String one = "";
					String two = "";
					String three = "";

					boolean visible = false;

					if (beacon.getTier() != 0) {
						ChatColor color = getBeaconColor(block);
						List<String> effects = getEffectLines(beacon, color);

						one = getTierLine(beacon, color);
						two = effects.get(0);
						three = effects.get(1);

						visible = true;
					}

					if (!line1.getCustomName().equals(one)) {
						line1.setCustomName(one);
						line1.setCustomNameVisible(visible);
						PacketManager.updateArmorStand(InteractionVisualizer.getOnlinePlayers(), line1, true);
					}
					if (!line2.getCustomName().equals(two)) {
						line2.setCustomName(two);
						line2.setCustomNameVisible(visible);
						PacketManager.updateArmorStand(InteractionVisualizer.getOnlinePlayers(), line2, true);
					}
					if (!line3.getCustomName().equals(three)) {
						line3.setCustomName(three);
						line3.setCustomNameVisible(visible);
						PacketManager.updateArmorStand(InteractionVisualizer.getOnlinePlayers(), line3, true);
					}
				}, delay);
			}
		}, 0, checkingPeriod).getTaskId();
	}

	private static String getTierLine(Beacon beacon, ChatColor color) {
		if (InteractionVisualizer.bp2) {
			int powerValue = BeaconPlusCore.getData(beacon).getPowerValue();
			return color + String.format("P%s \u27f9 %sm", powerValue, getRangeByPower(powerValue));
		}

		int tier = beacon.getTier();
		return color + String.format("T%s \u27f9 %sm", tier, getRange(tier));
	}

	private static List<String> getEffectLines(Beacon beacon, ChatColor color) {
		String firstLine;
		String secondLine;
		if (InteractionVisualizer.bp2) {
			BeaconData bp2Data = BeaconPlusCore.getData(beacon);
			List<BeaconEffect> effects = new ArrayList<>(bp2Data.getEffects());
			Random rand = new Random();
			int firstIndex = rand.nextInt(effects.size());
			firstIndex = firstIndex + 1 == effects.size() ? firstIndex - 1: firstIndex;

			firstLine = String.format("%s %s", effects.get(firstIndex).getDisplayName(), bp2Data.getLevel(effects.get(firstIndex)));
			secondLine = String.format("%s %s", effects.get(firstIndex + 1).getDisplayName(), bp2Data.getLevel(effects.get(firstIndex + 1)));
		} else {
			PotionEffect primaryEffect = beacon.getPrimaryEffect();
			PotionEffect secondaryEffect = beacon.getSecondaryEffect();

			firstLine = primaryEffect == null ? "" : EffectManager.getEffectConfig().getString("Effect." + primaryEffect.getType().getName().toUpperCase())
					+ " " + RomanNumberUtils.toRoman(primaryEffect.getAmplifier() + 1);

			secondLine = secondaryEffect == null ? "" : EffectManager.getEffectConfig().getString("Effect." + secondaryEffect.getType().getName().toUpperCase())
					+ " " + RomanNumberUtils.toRoman(secondaryEffect.getAmplifier() + 1);
		}

		return Arrays.asList(color + firstLine, color + secondLine);
	}

	public static List<Block> nearbyBeacon() {
		return TileEntityManager.getTileEntites("beacon");
	}

	public static boolean isActive(Location loc) {
		return PlayerRangeManager.hasPlayerNearby(loc);
	}

	public static HashMap<String, ArmorStand> spawnArmorStands(Block block, BlockFace face) {
		HashMap<String, ArmorStand> map = new HashMap<String, ArmorStand>();
		Location origin = block.getLocation();

		Location target = block.getRelative(face).getLocation();
		Vector direction = target.toVector().subtract(origin.toVector()).multiply(0.7);

		Location loc = block.getLocation().clone().add(direction).add(0.5, 0.25, 0.5);
		ArmorStand line1 = new ArmorStand(loc.clone().add(0.0, 0.25, 0.0));
		setStand(line1);
		ArmorStand line2 = new ArmorStand(loc.clone());
		setStand(line2);
		ArmorStand line3 = new ArmorStand(loc.clone().add(0.0, -0.25, 0.0));
		setStand(line3);

		map.put("1", line1);
		map.put("2", line2);
		map.put("3", line3);

		PacketManager.sendArmorStandSpawn(InteractionVisualizer.holograms, line1);
		PacketManager.sendArmorStandSpawn(InteractionVisualizer.holograms, line2);
		PacketManager.sendArmorStandSpawn(InteractionVisualizer.holograms, line3);

		return map;
	}

	public static void setStand(ArmorStand stand) {
		stand.setBasePlate(false);
		stand.setMarker(true);
		stand.setGravity(false);
		stand.setSmall(true);
		stand.setSilent(true);
		stand.setInvulnerable(true);
		stand.setVisible(false);
		stand.setCustomName("");
		stand.setRightArmPose(new EulerAngle(0.0, 0.0, 0.0));
	}

	public static BlockFace getCardinalFacing(float[] dir) {

		double rotation = (dir[0] - 90.0F) % 360.0F;

		if (rotation < 0.0D) {
			rotation += 360.0D;
		}
		if ((0.0D <= rotation) && (rotation < 45.0D))
			return BlockFace.EAST;
		if ((45.0D <= rotation) && (rotation < 135.0D))
			return BlockFace.SOUTH;
		if ((135.0D <= rotation) && (rotation < 225.0D))
			return BlockFace.WEST;
		if ((225.0D <= rotation) && (rotation < 315.0D))
			return BlockFace.NORTH;
		if ((315.0D <= rotation) && (rotation < 360.0D)) {
			return BlockFace.EAST;
		}
		return BlockFace.NORTH;
	}

	public static ChatColor getBeaconColor(Block block) {
		Block glass = block.getRelative(BlockFace.UP);
		if (!glass.getType().toString().toUpperCase().contains("GLASS")) {
			return ChatColor.WHITE;
		}
		if (!InteractionVisualizer.version.contains("legacy")) {
			if (glass.getType().equals(Material.GLASS) || glass.getType().equals(Material.GLASS_PANE)) {
				return ChatColor.WHITE;
			}
			String color = glass.getType().toString().toUpperCase().substring(0, glass.getType().toString().toUpperCase().indexOf("_"));
			if (color.equals("LIGHT")) {
				String temp = glass.getType().toString().toUpperCase().substring(glass.getType().toString().toUpperCase().indexOf("_") + 1);
				color = color + "_" + temp.substring(0, temp.indexOf("_"));
			}
			switch (color) {
			case "WHITE":
				return ChatColor.WHITE;
			case "ORANGE":
				return ChatColor.GOLD;
			case "MAGENTA":
				return ChatColor.LIGHT_PURPLE;
			case "LIGHT_BLUE":
				return ChatColor.AQUA;
			case "YELLOW":
				return ChatColor.YELLOW;
			case "LIME":
				return ChatColor.GREEN;
			case "PINK":
				return ChatColor.LIGHT_PURPLE;
			case "GRAY":
				return ChatColor.DARK_GRAY;
			case "LIGHT_GRAY":
				return ChatColor.GRAY;
			case "CYAN":
				return ChatColor.DARK_AQUA;
			case "PURPLE":
				return ChatColor.DARK_PURPLE;
			case "BLUE":
				return ChatColor.BLUE;
			case "BROWN":
				return ChatColor.GOLD;
			case "GREEN":
				return ChatColor.DARK_GREEN;
			case "RED":
				return ChatColor.RED;
			case "BLACK":
				return ChatColor.WHITE;
			default:
				return ChatColor.WHITE;
			}
		} else {
			@SuppressWarnings("deprecation")
			DyeColor color = DyeColor.getByWoolData(glass.getData());
			switch (color.toString().toUpperCase()) {
			case "BLACK":
				return ChatColor.WHITE;
			case "BLUE":
				return ChatColor.BLUE;
			case "BROWN":
				return ChatColor.GOLD;
			case "CYAN":
				return ChatColor.DARK_AQUA;
			case "GRAY":
				return ChatColor.DARK_GRAY;
			case "GREEN":
				return ChatColor.DARK_GREEN;
			case "LIGHT_BLUE":
				return ChatColor.AQUA;
			case "SILVER":
			case "LIGHT_GRAY":
				return ChatColor.GRAY;
			case "LIME":
				return ChatColor.GREEN;
			case "MAGENTA":
				return ChatColor.LIGHT_PURPLE;
			case "ORANGE":
				return ChatColor.GOLD;
			case "PINK":
				return ChatColor.LIGHT_PURPLE;
			case "PURPLE":
				return ChatColor.DARK_PURPLE;
			case "RED":
				return ChatColor.RED;
			case "WHITE":
				return ChatColor.WHITE;
			case "YELLOW":
				return ChatColor.YELLOW;
			default:
				return ChatColor.WHITE;
			}
		}
	}

	public static int getRange(int tier) {
		switch (tier) {
		case 0:
			return 0;
		case 1:
			return 20;
		case 2:
			return 30;
		case 3:
			return 40;
		case 4:
			return 50;
		default:
			return 0;
		}
	}

}
