package itemequalsborder.modid;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;

import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class ItemEqualsBorder implements ModInitializer {
	public static final String MOD_ID = "item-equals-border";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final double BORDER_GROWTH_PER_POINT = 2.0;
	private static final long BORDER_LERP_TIME_TICKS = 20;

	private int tickCounter = 0;

	private final Map<UUID, BlockPos> lastOverworldPos = new HashMap<>();

	@Override
	public void onInitialize() {
		ItemPoints.load();
		PayloadTypeRegistry.clientboundPlay().register(KnownItemsUpdatePayload.TYPE, KnownItemsUpdatePayload.CODEC);
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {

			ServerLevel level = server.overworld();

			// Set Worldspawn to 0,0
			int surfaceHeight = level.getChunkSource().getGenerator().getSpawnHeight(level);
			BlockPos exactSpawn = new BlockPos(0, surfaceHeight, 0);
			LevelData.RespawnData respawnData = LevelData.RespawnData.of(level.dimension(), exactSpawn, 0.0F, 0.0F);
			level.setRespawnData(respawnData);
			server.getGameRules().set(GameRules.RESPAWN_RADIUS, 0, server);

			// Set Border
			KnownItemsData data = getKnownItemsData(level);
			WorldBorder overworldBorder = level.getWorldBorder();
			overworldBorder.setCenter(0.5, 0.5); // Overworld-Border bleibt bei 0,0 (Spawn)
			overworldBorder.setSize(data.getBorderSize());

			if (data.hasReachedNether()) {
				ServerLevel nether = server.getLevel(Level.NETHER);
				if (nether != null) {
					WorldBorder netherBorder = nether.getWorldBorder();
					netherBorder.setCenter(data.getNetherBorderCenterX(), data.getNetherBorderCenterZ());
					netherBorder.setSize(data.getBorderSize());
				}
			}

		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			checkNetherEntry(server);
			if (++tickCounter % 10 != 0) return;
			KnownItemsData data = getKnownItemsData(server.overworld());
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
					if (stack.isEmpty()) continue;
					if (data.registerIfNew(stack.getItem(), player.getName().getString())) {
						onNewItemDiscovered(server, stack.getItem(), player);
					}
				}
			}
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			KnownItemsData data = getKnownItemsData(server.overworld());
			KnownItemsUpdatePayload payload = buildPayload(data);
			ServerPlayNetworking.send(handler.player, payload);
		});
	}

	private void checkNetherEntry(MinecraftServer server) {
		ServerLevel overworld = server.overworld();
		KnownItemsData data = getKnownItemsData(overworld);

		if (data.hasReachedNether()) return; // spart unnötige Arbeit, sobald einmal gesetzt

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ResourceKey<Level> dimension = player.level().dimension();

			if (dimension.equals(Level.NETHER)) {
				BlockPos arrivalPos = player.blockPosition(); // aktuelle Position IM Nether

				if (data.setNetherBorderIfFirstEntry(arrivalPos.getX() + 0.5, arrivalPos.getZ() + 0.5)) {
					ServerLevel nether = (ServerLevel) player.level();
					WorldBorder netherBorder = nether.getWorldBorder();
					netherBorder.setCenter(data.getNetherBorderCenterX(), data.getNetherBorderCenterZ());
					netherBorder.setSize(data.getBorderSize());

					broadcastKnownItems(server, data);

					server.getPlayerList().broadcastSystemMessage(
							Component.literal(player.getName().getString() + " hat zum ersten Mal den Nether betreten – Border-Zentrum im Nether gesetzt!"),
							false
					);
				}
			}
		}
	}

	private void onNewItemDiscovered(MinecraftServer server, Item item, ServerPlayer discoverer) {
		ServerLevel overworld = server.overworld();
		KnownItemsData data = getKnownItemsData(overworld);
		WorldBorder border = overworld.getWorldBorder();

		int points = ItemPoints.getPoints(item);
		double oldSize = data.getBorderSize();
		double newSize = oldSize + points * BORDER_GROWTH_PER_POINT;

		border.lerpSizeBetween(oldSize, newSize, BORDER_LERP_TIME_TICKS, overworld.getGameTime());
		data.setBorderSize(newSize);

		if (data.hasReachedNether()) {
			ServerLevel nether = server.getLevel(Level.NETHER);
			if (nether != null) {
				nether.getWorldBorder().lerpSizeBetween(oldSize, newSize, BORDER_LERP_TIME_TICKS, nether.getGameTime());
			}
		}

		broadcastKnownItems(server, data);

		server.getPlayerList().broadcastSystemMessage(
				Component.literal(discoverer.getName().getString() + " hat entdeckt: "
						+ item.getDefaultInstance().getHoverName().getString() + " (+" + points + " Punkte)"),
				false
		);
	}

	private KnownItemsUpdatePayload buildPayload(KnownItemsData data) {
		List<Item> items = new ArrayList<>();
		List<String> discoverers = new ArrayList<>();

		Map<Item, String> discovererMap = data.getDiscoverersView();

		for (Item item : data.getKnownItemsView()) {
			items.add(item);
			discoverers.add(discovererMap.getOrDefault(item, "Unbekannt"));
		}

		return new KnownItemsUpdatePayload(items, discoverers, data.getBorderSize());
	}

	private void broadcastKnownItems(MinecraftServer server, KnownItemsData data) {
		KnownItemsUpdatePayload payload = buildPayload(data);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	private KnownItemsData getKnownItemsData(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(KnownItemsData.TYPE);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
