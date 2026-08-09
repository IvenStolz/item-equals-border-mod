package itemequalsborder.modid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

public class KnownItemsData extends SavedData {

    private final Set<Item> knownItems;
    private final Map<Item, String> discoverers;
    private double borderSize;
    private boolean netherReached;
    private double netherBorderCenterX;
    private double netherBorderCenterZ;

    public KnownItemsData() {
        this(new HashSet<>(), new LinkedHashMap<>(), 1.0, false, 0.5, 0.5);
    }

    private KnownItemsData(Set<Item> knownItems, Map<Item, String> discoverers, double borderSize, boolean netherReached, double netherBorderCenterX, double netherBorderCenterZ) {
        this.knownItems = new HashSet<>(knownItems);
        this.discoverers = new LinkedHashMap<>(discoverers);
        this.borderSize = borderSize;
        this.netherReached = netherReached;
        this.netherBorderCenterX = netherBorderCenterX;
        this.netherBorderCenterZ = netherBorderCenterZ;
    }

    // true = das Item war neu
    public boolean registerIfNew(Item item, String discoverer) {
        boolean wasNew = knownItems.add(item);
        if (wasNew) {
            discoverers.put(item, discoverer);
            setDirty();
        }
        return wasNew;
    }

    private Map<Item, String> getDiscoverersRaw() {
        return discoverers;
    }

    public Map<Item, String> getDiscoverersView() {
        return Collections.unmodifiableMap(discoverers);
    }

    private Set<Item> getKnownItems() {
        return knownItems;
    }

    public double getBorderSize() {
        return borderSize;
    }

    public Set<Item> getKnownItemsView() {
        return java.util.Collections.unmodifiableSet(knownItems);
    }

    public void setBorderSize(double size) {
        this.borderSize = size;
        setDirty();
    }

    public boolean hasReachedNether() {
        return netherReached;
    }

    public double getNetherBorderCenterX() {
        return netherBorderCenterX;
    }

    public double getNetherBorderCenterZ() {
        return netherBorderCenterZ;
    }

    public boolean setNetherBorderIfFirstEntry(double x, double z) {
        if (netherReached) {return false;}
        this.netherReached = true;
        this.netherBorderCenterX = x;
        this.netherBorderCenterZ = z;
        setDirty();
        return true;
    }

    private record DiscoveryEntry(Item item, String discoverer) {
        static final Codec<DiscoveryEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(DiscoveryEntry::item),
                Codec.STRING.fieldOf("discoverer").forGetter(DiscoveryEntry::discoverer)
        ).apply(instance, DiscoveryEntry::new));
    }

    private static final Codec<Map<Item, String>> DISCOVERERS_CODEC = DiscoveryEntry.CODEC.listOf().xmap(
            list -> {
                Map<Item, String> map = new LinkedHashMap<>();
                for (DiscoveryEntry e : list) map.put(e.item(), e.discoverer());
                return map;
            },
            map -> map.entrySet().stream().map(e -> new DiscoveryEntry(e.getKey(), e.getValue())).toList()
    );

    private static final Codec<Set<Item>> ITEM_SET_CODEC =
            BuiltInRegistries.ITEM.byNameCodec()
                    .listOf()
                    .xmap(HashSet::new, List::copyOf);

    private static final Codec<KnownItemsData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ITEM_SET_CODEC.fieldOf("known_items").forGetter(KnownItemsData::getKnownItems),
            DISCOVERERS_CODEC.optionalFieldOf("discoverers", Map.of()).forGetter(KnownItemsData::getDiscoverersRaw),
            Codec.DOUBLE.fieldOf("border_size").forGetter(KnownItemsData::getBorderSize),
            Codec.BOOL.optionalFieldOf("nether_reached", false).forGetter(KnownItemsData::hasReachedNether),
            Codec.DOUBLE.optionalFieldOf("center_x", 0.5).forGetter(KnownItemsData::getNetherBorderCenterX),
            Codec.DOUBLE.optionalFieldOf("center_z", 0.5).forGetter(KnownItemsData::getNetherBorderCenterZ)
    ).apply(instance, KnownItemsData::new));

    public static final SavedDataType<KnownItemsData> TYPE = new SavedDataType<>(
            ItemEqualsBorder.id("known_items"),
            KnownItemsData::new,
            CODEC,
            DataFixTypes.LEVEL
    );
}