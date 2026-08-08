package itemequalsborder.modid.client;

import itemequalsborder.modid.ItemPoints;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

public class CollectedItemWidget extends AbstractWidget {
    private final ItemStack stack;
    public CollectedItemWidget(int x, int y, int size, ItemStack stack) {
        super(x, y, size, size, Component.empty());
        this.stack = stack;

        int points = ItemPoints.getPoints(stack.getItem());
        String tooltipText = stack.getHoverName().getString() + "\n" + points + " Punkt";
        if (points != 1) {
            tooltipText += "e";
        }
        this.setTooltip(Tooltip.create(Component.literal(tooltipText)));
    }

    @Override
    protected void extractWidgetRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int points = ItemPoints.getPoints(stack.getItem());
        int color = ItemPoints.getColor(points);

        int itemX = this.getX() + (this.getWidth() - 16) / 2;
        int itemY = this.getY() + 2;
        graphics.item(stack, itemX, itemY);

        int textY = itemY + 24;
        Component nameComponent = stack.getHoverName();
        Font font = Minecraft.getInstance().font;

        int expandedTextWidth = (int) (this.getWidth() * 1.5);
        int centerX = this.getX() + this.getWidth() / 2;

        if (this.isHovered()) {
            net.minecraft.client.gui.ActiveTextCollector textCollector = graphics.textRenderer();

            int leftBound = centerX - (expandedTextWidth / 2);
            int rightBound = centerX + (expandedTextWidth / 2);

            Component coloredName = nameComponent.copy().withStyle(style -> style.withColor(color));

            textCollector.acceptScrolling(
                    coloredName,
                    centerX,
                    leftBound,
                    rightBound,
                    textY,
                    textY + font.lineHeight
            );
        } else {
            String rawName = nameComponent.getString();
            String displayName = truncateText(font, rawName, expandedTextWidth);
            int displayNameWidth = font.width(displayName);
            int textX = centerX - (displayNameWidth / 2);
            graphics.text(font, displayName, textX, textY, color, true);
        }
    }

    private String truncateText(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int ellipsisWidth = font.width(ellipsis);
        StringBuilder builder = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (font.width(builder.toString() + c) + ellipsisWidth > maxWidth) {
                break;
            }
            builder.append(c);
        }
        return builder + ellipsis;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        // For brevity, we'll just skip this for now - if you want to add narration to your widget, you can do so here.
        return;
    }
}
