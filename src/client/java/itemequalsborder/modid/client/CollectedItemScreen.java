package itemequalsborder.modid.client;

import itemequalsborder.modid.ItemPoints;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CollectedItemScreen extends Screen {
    public Screen parent;
    private static final int SIDEBAR_WIDTH = 155;

    public CollectedItemScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        Button closeButton = Button.builder(Component.literal("X"), button -> onClose())
                .bounds(this.width - 25, 5, 20, 20).build();
        this.addRenderableWidget(closeButton);

        Button sortButton = Button.builder(
                Component.literal("Sortierung: " + sortLabel()),
                button -> {
                    sortMode = (sortMode == SortMode.ALPHABETICAL) ? SortMode.POINTS : SortMode.ALPHABETICAL;
                    this.rebuildWidgets();
                }
        ).bounds(5, 5, 150, 20).build();
        this.addRenderableWidget(sortButton);

        List<Item> items = getSortedItems();

        int margin = SIDEBAR_WIDTH + 20; // Abstand von BEIDEN Seiten
        int gridWidth = this.width - (2 * margin);

        int startY = 40;
        int gridHeight = this.height - 80;

        this.addRenderableWidget(new CollectedItemGridWidget(
                margin, startY, gridWidth, gridHeight,
                Component.literal("Items"), items
        ));

        // Sidebar als eigenes Widget, rechts angedockt
        this.addRenderableWidget(new LeaderboardWidget(
                this.width - SIDEBAR_WIDTH, startY, SIDEBAR_WIDTH, gridHeight
        ));
    }

    private String sortLabel() {
        return switch (sortMode) {
            case ALPHABETICAL -> "A-Z";
            case POINTS -> "Punkte";
        };
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta); // rendert jetzt auch das LeaderboardWidget

        double borderSize = ClientKnownItemsCache.getBorderSize();
        int discoveredItems = ClientKnownItemsCache.getItems().size();
        graphics.text(this.font, "Current Border Size: " + borderSize, 10, 35, 0xFFFFFFFF, true);
        graphics.text(this.font, "Item Count: " + discoveredItems, 10, 45 + this.font.lineHeight, 0xFFFFFFFF, true);
    }

    private enum SortMode {
        ALPHABETICAL,
        POINTS
    }

    private SortMode sortMode = SortMode.ALPHABETICAL;

    private List<Item> getSortedItems() {
        List<Item> items = new ArrayList<>(ClientKnownItemsCache.getItems());

        Comparator<Item> alphabetical = Comparator.comparing(
                item -> item.getDefaultInstance().getHoverName().getString()
        );

        if (sortMode == SortMode.ALPHABETICAL) {
            items.sort(alphabetical);
        } else {
            // Primär nach Punktzahl absteigend (höchste Punktzahl zuerst),
            // sekundär alphabetisch bei gleicher Punktzahl
            Comparator<Item> byPoints = Comparator.comparingInt(ItemPoints::getPoints).reversed();
            items.sort(byPoints.thenComparing(alphabetical));
        }

        return items;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }
}
