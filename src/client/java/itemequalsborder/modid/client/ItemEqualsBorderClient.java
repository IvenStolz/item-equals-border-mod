package itemequalsborder.modid.client;

import itemequalsborder.modid.ItemEqualsBorder;
import itemequalsborder.modid.KnownItemsUpdatePayload;
import itemequalsborder.modid.Lang;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;

/**
 * Client-side mod initializer handling keybindings, screen navigation,
 * and clientbound network payload receivers.
 */
public class ItemEqualsBorderClient implements ClientModInitializer {

	/**
	 * Custom keybinding category for the mod settings menu.
	 */
	private static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(ItemEqualsBorder.id("test"));

	/**
	 * Keybinding to open the collected items screen (defaults to 'J').
	 * Uses the 3-argument constructor for compatibility across Minecraft 26.2 and 26.3.
	 */
	public static final KeyMapping OPEN_COLLECTED_ITEMS = new KeyMapping(
			"key.item-equals-border.open_collected",
			InputConstants.KEY_J,
			CATEGORY
	);

	/**
	 * Registers client keybindings, tick listeners for opening the UI,
	 * and network handlers for synchronizing discovered items.
	 */
	@Override
	public void onInitializeClient() {
		KeyMappingHelper.registerKeyMapping(OPEN_COLLECTED_ITEMS);

		// Open the collected items screen when the keybinding is pressed
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (OPEN_COLLECTED_ITEMS.consumeClick()) {
				Minecraft mc = Minecraft.getInstance();
				mc.gui.setScreen(new CollectedItemScreen(Lang.tr("screen.title"), mc.gui.screen()));
			}
		});

		// Receive and cache updated item discovery data and border size from the server
		ClientPlayNetworking.registerGlobalReceiver(KnownItemsUpdatePayload.TYPE, (payload, context) -> {
			context.client().execute(() ->
					ClientKnownItemsCache.update(payload.items(), payload.discoverers(), payload.borderSize())
			);
		});
	}
}