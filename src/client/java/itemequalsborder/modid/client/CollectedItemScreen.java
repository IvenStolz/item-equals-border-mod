package itemequalsborder.modid.client;

import itemequalsborder.modid.ItemPoints;
import itemequalsborder.modid.Lang;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.NonNull;

import java.util.*;

/**
 * Screen that displays all game items, their discovery status, and a player leaderboard.
 * Provides filtering by discovery status, a search bar, and sorting options.
 */
public class CollectedItemScreen extends Screen {
    public Screen parent;
    private static final int SIDEBAR_WIDTH = 155;
    private static final int BUTTON_HEIGHT = 20;
    private boolean showDiscovered = true;
    private boolean showUndiscovered = true;

    private String searchQuery = "";
    private EditBox searchBox;

    private Button sortButton;
    private CollectedItemGridWidget gridWidget;
    private SortMode sortMode = SortMode.ALPHABETICAL;

    /**
     * Constructs the collected items screen.
     *
     * @param title  The screen title component.
     * @param parent The parent screen to return to when closing.
     */
    public CollectedItemScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    /**
     * Initializes the UI components, layout bounds, and event listeners.
     */
    @Override
    protected void init() {
        // Close button in the top-right corner
        Button closeButton = Button.builder(Lang.tr("button.close"), button -> onClose())
                .bounds(this.width - 25, 5, 20, BUTTON_HEIGHT).build();
        this.addRenderableWidget(closeButton);

        // Sort mode toggle button
        sortButton = Button.builder(
                getSortButtonMessage(),
                button -> {
                    sortMode = (sortMode == SortMode.ALPHABETICAL) ? SortMode.POINTS : SortMode.ALPHABETICAL;
                    button.setMessage(getSortButtonMessage());
                    refreshItems();
                }
        ).bounds(5, this.height - 2 * (BUTTON_HEIGHT + 5), SIDEBAR_WIDTH - 5, BUTTON_HEIGHT).build();
        this.addRenderableWidget(sortButton);

        // Filter checkbox for discovered items
        Checkbox discoveredCheckbox = Checkbox.builder(Lang.tr("checkbox.discovered"), this.font)
                .pos(5, this.height - 3 * (BUTTON_HEIGHT + 5))
                .selected(showDiscovered)
                .onValueChange((checkbox, selected) -> {
                    this.showDiscovered = selected;
                    refreshItems();
                }).build();
        this.addRenderableWidget(discoveredCheckbox);

        // Filter checkbox for undiscovered items
        Checkbox undiscoveredCheckbox = Checkbox.builder(Lang.tr("checkbox.undiscovered"), this.font)
                .pos(5, this.height - 4 * (BUTTON_HEIGHT + 5))
                .selected(showUndiscovered)
                .onValueChange((checkbox, selected) -> {
                    this.showUndiscovered = selected;
                    refreshItems();
                }).build();
        this.addRenderableWidget(undiscoveredCheckbox);

        // Search input box at the bottom left
        if (this.searchBox == null) {
            this.searchBox = new EditBox(
                    this.font, 5, this.height - (BUTTON_HEIGHT + 5),
                    SIDEBAR_WIDTH - 5, BUTTON_HEIGHT, Lang.tr("screen.search")
            );
            this.searchBox.setHint(Lang.tr("screen.search"));
            this.searchBox.setResponder(text -> {
                this.searchQuery = text;
                refreshItems();
            });
        } else {
            this.searchBox.setPosition(5, this.height - (BUTTON_HEIGHT + 5));
        }
        this.addRenderableWidget(this.searchBox);

        // Main item grid in the center with equal margins on both sides
        int margin = SIDEBAR_WIDTH + 20;
        int gridWidth = this.width - (2 * margin);
        int startY = 40;
        int gridHeight = this.height - 80;

        gridWidget = new CollectedItemGridWidget(
                margin, startY, gridWidth, gridHeight,
                Lang.tr("screen.items"), getSortedAllItems(), new HashSet<>(ClientKnownItemsCache.getItems())
        );
        this.addRenderableWidget(gridWidget);

        // Leaderboard sidebar docked to the right side of the screen
        this.addRenderableWidget(new LeaderboardWidget(
                this.width - SIDEBAR_WIDTH, startY, SIDEBAR_WIDTH, gridHeight
        ));
    }

    /**
     * Builds the localized label for the sort button based on the active sort mode.
     */
    private Component getSortButtonMessage() {
        String modeKey = (sortMode == SortMode.ALPHABETICAL) ? "sort.alphabetical" : "sort.points";
        return Lang.tr("button.sort", Lang.tr(modeKey));
    }

    /**
     * Updates the item grid widget with the currently filtered and sorted item list.
     */
    private void refreshItems() {
        gridWidget.setItems(getSortedAllItems(), new HashSet<>(ClientKnownItemsCache.getItems()));
    }

    /**
     * Collects, filters, and sorts all registered items according to current UI settings.
     *
     * @return A sorted and filtered list of items.
     */
    private List<Item> getSortedAllItems() {
        List<Item> items = new ArrayList<>();
        List<Item> discoveredItems = ClientKnownItemsCache.getItems();

        String query = this.searchQuery.toLowerCase(Locale.ROOT);

        net.minecraft.core.registries.BuiltInRegistries.ITEM.forEach(item -> {
            boolean isDiscovered = discoveredItems.contains(item);
            if ((isDiscovered && showDiscovered) || (!isDiscovered && showUndiscovered)) {
                String itemName = item.getDefaultInstance().getHoverName().getString().toLowerCase(Locale.ROOT);
                if (query.isEmpty() || itemName.contains(query)) {
                    items.add(item);
                }
            }
        });

        Comparator<Item> alphabetical = Comparator.comparing(
                item -> item.getDefaultInstance().getHoverName().getString()
        );

        if (sortMode == SortMode.ALPHABETICAL) {
            items.sort(alphabetical);
        } else {
            Comparator<Item> byPoints = Comparator.comparingInt(ItemPoints::getPoints).reversed();
            items.sort(byPoints.thenComparing(alphabetical));
        }
        return items;
    }

    /**
     * Renders the screen elements and status text overlays (border size and item count).
     */
    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        double borderSize = ClientKnownItemsCache.getBorderSize();
        int discoveredItems = ClientKnownItemsCache.getItems().size();
        graphics.text(this.font, Lang.tr("screen.border_size", borderSize), 10, 10, 0xFFFFFFFF, true);
        graphics.text(this.font, Lang.tr("screen.item_count", discoveredItems), 10, 15 + this.font.lineHeight, 0xFFFFFFFF, true);
    }

    /**
     * Available sorting modes for the item grid.
     */
    private enum SortMode {
        ALPHABETICAL,
        POINTS
    }

    /**
     * Closes the current screen and returns to the parent screen.
     */
    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }
}