package itemequalsborder.modid;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
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

/**
 * Main mod initializer handling server lifecycle events, inventory scanning,
 * world border expansion, and client-server synchronization.
 */
public class ItemEqualsBorder implements ModInitializer {
	public static final String MOD_ID = "item-equals-border";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final double BORDER_GROWTH_PER_POINT = 2.0;
	private static final long BORDER_LERP_TIME_TICKS = 20;

	private int tickCounter = 0;

	/**
	 * Initializes item point configurations, networking payloads, and server event listeners.
	 */
	@Override
	public void onInitialize() {
		ItemPoints.load();
		PayloadTypeRegistry.clientboundPlay().register(KnownItemsUpdatePayload.TYPE, KnownItemsUpdatePayload.CODEC);

		// Configure world spawn and initialize world borders when the server starts
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			ServerLevel level = server.overworld();

			// Set world spawn to (0, surface, 0) with zero respawn radius
			int surfaceHeight = level.getChunkSource().getGenerator().getSpawnHeight(level);
			BlockPos exactSpawn = new BlockPos(0, surfaceHeight, 0);
			LevelData.RespawnData respawnData = LevelData.RespawnData.of(level.dimension(), exactSpawn, 0.0F, 0.0F);
			level.setRespawnData(respawnData);
			server.getGameRules().set(GameRules.RESPAWN_RADIUS, 0, server);

			// Apply saved border state to the Overworld
			KnownItemsData data = getKnownItemsData(level);
			WorldBorder overworldBorder = level.getWorldBorder();
			overworldBorder.setCenter(0.5, 0.5);
			overworldBorder.setSize(data.getBorderSize());

			// Apply saved border state to the Nether if already unlocked
			if (data.hasReachedNether()) {
				ServerLevel nether = server.getLevel(Level.NETHER);
				if (nether != null) {
					WorldBorder netherBorder = nether.getWorldBorder();
					netherBorder.setCenter(data.getNetherBorderCenterX(), data.getNetherBorderCenterZ());
					netherBorder.setSize(data.getBorderSize());
				}
			}
		});

		// Periodically check for Nether entry and scan player inventories for new items
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

		// Sync known items and border state to players when they join the server
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			KnownItemsData data = getKnownItemsData(server.overworld());
			KnownItemsUpdatePayload payload = buildPayload(data);
			ServerPlayNetworking.send(handler.player, payload);
		});
	}

	/**
	 * Checks if any player has entered the Nether for the first time and centers
	 * the Nether world border at their arrival coordinates.
	 */
	private void checkNetherEntry(MinecraftServer server) {
		ServerLevel overworld = server.overworld();
		KnownItemsData data = getKnownItemsData(overworld);

		if (data.hasReachedNether()) return;

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ResourceKey<Level> dimension = player.level().dimension();

			if (dimension.equals(Level.NETHER)) {
				BlockPos arrivalPos = player.blockPosition();

				if (data.setNetherBorderIfFirstEntry(arrivalPos.getX() + 0.5, arrivalPos.getZ() + 0.5)) {
					ServerLevel nether = (ServerLevel) player.level();
					WorldBorder netherBorder = nether.getWorldBorder();
					netherBorder.setCenter(data.getNetherBorderCenterX(), data.getNetherBorderCenterZ());
					netherBorder.setSize(data.getBorderSize());

					broadcastKnownItems(server, data);

					server.getPlayerList().broadcastSystemMessage(
							Lang.tr("message.nether_entered", player.getName()),
							false
					);
				}
			}
		}
	}

	/**
	 * Expands the world border, updates persistent state, and notifies all players
	 * when a new item is discovered.
	 */
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
				Lang.tr("message.discovered",
						discoverer.getName(),
						item.getDefaultInstance().getHoverName(),
						points),
				false
		);
	}

	/**
	 * Constructs the network payload containing discovered items, their discoverers, and border size.
	 */
	private KnownItemsUpdatePayload buildPayload(KnownItemsData data) {
		List<Item> items = new ArrayList<>();
		List<String> discoverers = new ArrayList<>();

		Map<Item, String> discovererMap = data.getDiscoverersView();

		for (Item item : data.getKnownItemsView()) {
			items.add(item);
			discoverers.add(discovererMap.getOrDefault(item, ""));
		}

		return new KnownItemsUpdatePayload(items, discoverers, data.getBorderSize());
	}

	/**
	 * Sends the updated known items payload to all connected players.
	 */
	private void broadcastKnownItems(MinecraftServer server, KnownItemsData data) {
		KnownItemsUpdatePayload payload = buildPayload(data);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	/**
	 * Retrieves or creates the persistent {@link KnownItemsData} storage for the given level.
	 */
	private KnownItemsData getKnownItemsData(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(KnownItemsData.TYPE);
	}

	/**
	 * Creates a namespaced {@link Identifier} for this mod.
	 */
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}