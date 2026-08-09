package itemequalsborder.modid;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.List;

public record KnownItemsUpdatePayload(List<Item> items, List<String> discoverers, double borderSize) implements CustomPacketPayload {

    public static final Identifier ID = ItemEqualsBorder.id("known_items_update");
    public static final CustomPacketPayload.Type<KnownItemsUpdatePayload> TYPE = new CustomPacketPayload.Type<>(ID);

    private static final StreamCodec<RegistryFriendlyByteBuf, List<Item>> ITEM_LIST_CODEC =
            ByteBufCodecs.registry(BuiltInRegistries.ITEM.key()).apply(ByteBufCodecs.list());

    private static final StreamCodec<RegistryFriendlyByteBuf, List<String>> STRING_LIST_CODEC =
            ByteBufCodecs.STRING_UTF8.<RegistryFriendlyByteBuf>cast().apply(ByteBufCodecs.list());

    public static final StreamCodec<RegistryFriendlyByteBuf, KnownItemsUpdatePayload> CODEC = StreamCodec.composite(
            ITEM_LIST_CODEC, KnownItemsUpdatePayload::items,
            STRING_LIST_CODEC, KnownItemsUpdatePayload::discoverers,
            ByteBufCodecs.DOUBLE, KnownItemsUpdatePayload::borderSize,
            KnownItemsUpdatePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}