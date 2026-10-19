package itemequalsborder.modid.client;

import itemequalsborder.modid.ItemPoints;
import itemequalsborder.modid.Lang;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

/**
 * Widget representing a single item slot in the collection grid, including its icon, label, and tooltip.
 */
public class CollectedItemWidget extends AbstractWidget {
    private final ItemStack stack;
    private final boolean discovered;

    // 60% black, semi-transparent overlay for undiscovered items
    private static final int UNDISCOVERED_OVERLAY_COLOR = 0x99000000;
    private static final int UNDISCOVERED_TEXT_COLOR = 0xFF888888;

    /**
     * Constructs an item widget with a localized tooltip showing point value and discovery status.
     *
     * @param x          The X position.
     * @param y          The Y position.
     * @param size       The width and height of the widget.
     * @param stack      The item stack to display.
     * @param discovered Whether the item has been discovered yet.
     */
    public CollectedItemWidget(int x, int y, int size, ItemStack stack, boolean discovered) {
        super(x, y, size, size, Component.empty());
        this.stack = stack;
        this.discovered = discovered;

        int points = ItemPoints.getPoints(stack.getItem());
        String discoverer = ClientKnownItemsCache.getDiscoverer(stack.getItem());

        Component pointsComponent = Lang.tr(points == 1 ? "points.one" : "points.other", points);
        Component statusComponent = discovered
                ? Lang.tr("tooltip.discovered_by", discoverer)
                : Lang.tr("tooltip.undiscovered");

        Component tooltipComponent = Component.empty()
                .append(stack.getHoverName())
                .append("\n")
                .append(pointsComponent)
                .append("\n")
                .append(statusComponent);

        this.setTooltip(Tooltip.create(tooltipComponent));
    }

    /**
     * Renders the item icon, undiscovered overlay, and item name (scrolling on hover, truncated otherwise).
     */
    @Override
    protected void extractWidgetRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int color = ItemPoints.getColor(ItemPoints.getPoints(stack.getItem()));

        int itemX = this.getX() + (this.getWidth() - 16) / 2;
        int itemY = this.getY() + 2;

        graphics.item(stack, itemX, itemY);
        if (!discovered) {
            graphics.fill(itemX, itemY, itemX + 16, itemY + 16, UNDISCOVERED_OVERLAY_COLOR);
        }

        int textY = itemY + 24;
        Font font = Minecraft.getInstance().font;

        int expandedTextWidth = (int) (this.getWidth() * 1.5);
        int centerX = this.getX() + this.getWidth() / 2;

        int activeColor = discovered ? color : UNDISCOVERED_TEXT_COLOR;

        if (this.isHovered()) {
            net.minecraft.client.gui.ActiveTextCollector textCollector = graphics.textRenderer();
            int leftBound = centerX - (expandedTextWidth / 2);
            int rightBound = centerX + (expandedTextWidth / 2);
            Component coloredName = stack.getHoverName().copy().withStyle(style -> style.withColor(activeColor & 0xFFFFFF));

            textCollector.acceptScrolling(coloredName, centerX, leftBound, rightBound, textY, textY + font.lineHeight);
        } else {
            String rawName = stack.getHoverName().getString();
            String displayName = truncateText(font, rawName, expandedTextWidth);
            int displayNameWidth = font.width(displayName);
            int textX = centerX - (displayNameWidth / 2);

            graphics.text(font, displayName, textX, textY, activeColor, true);
        }
    }

    /**
     * Truncates a text string with an ellipsis ("...") if its pixel width exceeds maxWidth.
     */
    private String truncateText(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "...";
        int ellipsisWidth = font.width(ellipsis);
        StringBuilder builder = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (font.width(builder.toString() + c) + ellipsisWidth > maxWidth) break;
            builder.append(c);
        }
        return builder + ellipsis;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
    }
}