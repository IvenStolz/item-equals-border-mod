package itemequalsborder.modid.client;

import com.mojang.blaze3d.platform.InputConstants;
import itemequalsborder.modid.KnownItemsUpdatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class ItemEqualsBorderClient implements ClientModInitializer {

	private static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath("item-equals-border", "test"));

	public static final KeyMapping OPEN_COLLECTED_ITEMS = new KeyMapping(
			"key.item-equals-border.open_collected",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_J,
			CATEGORY
	);

	@Override
	public void onInitializeClient() {
		KeyMappingHelper.registerKeyMapping(OPEN_COLLECTED_ITEMS);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (OPEN_COLLECTED_ITEMS.consumeClick()) {
				Minecraft mc = Minecraft.getInstance();
				mc.gui.setScreen(new CollectedItemScreen(
						Component.literal("Gesammelte Items"),
						mc.gui.screen()
				));
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(KnownItemsUpdatePayload.TYPE, (payload, context) -> {
			context.client().execute(() ->
					ClientKnownItemsCache.update(payload.items(), payload.borderSize())
			);
		});
	}
}