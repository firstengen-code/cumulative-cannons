package com.cumulativecannons.client;

import com.cumulativecannons.CumulativeCannons;
import com.cumulativecannons.jet.JetMaterialCosts;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Searchable per-block penetration editor, opened from the mod config screen. */
public final class BlockPenetrationScreen extends Screen {
    private final Screen parent;
    private EditBox search;
    private EditBox cost;
    private BlockList list;
    private Block selected;
    private Component status = Component.empty();
    private int rightX;

    public BlockPenetrationScreen(Screen parent) {
        super(Component.translatable("cumulative_cannons.materials.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = 10;
        int listWidth = Math.max(160, this.width / 2 - 20);
        int top = 58;
        this.rightX = this.width / 2 + 10;

        this.search = new EditBox(this.font, left, 30, listWidth, 20,
                Component.translatable("cumulative_cannons.materials.search"));
        this.search.setHint(Component.translatable("cumulative_cannons.materials.search_hint"));
        this.search.setResponder(this::refresh);
        this.addRenderableWidget(this.search);

        this.list = new BlockList(this.minecraft, listWidth, Math.max(90, this.height - 130), top, 22);
        this.addRenderableWidget(this.list);

        this.cost = new EditBox(this.font, this.rightX, 90, 100, 20,
                Component.translatable("cumulative_cannons.materials.cost"));
        this.cost.setFilter(value -> value.matches("\\d*(\\.\\d*)?"));
        this.cost.setMaxLength(10);
        this.cost.setHint(Component.literal("0.0"));
        this.addRenderableWidget(this.cost);

        this.addRenderableWidget(Button.builder(
                        Component.translatable("cumulative_cannons.materials.set"),
                        button -> applyCost())
                .bounds(this.rightX, 116, 100, 20)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("cumulative_cannons.materials.reset"),
                        button -> resetCost())
                .bounds(this.rightX, 142, 100, 20)
                .build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(this.width / 2 - 60, this.height - 30, 120, 20)
                .build());

        this.refresh("");
    }

    private void refresh(String query) {
        if (this.list == null) {
            return;
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        Set<ResourceLocation> ids = new LinkedHashSet<>();

        if (needle.isEmpty()) {
            for (String key : JetMaterialCosts.blockOverrides().keySet()) {
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id != null) {
                    ids.add(id);
                }
            }
            ids.addAll(JetMaterialCosts.defaultExactBlocks());
        } else {
            for (Block block : BuiltInRegistries.BLOCK) {
                ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
                if (id.toString().toLowerCase(Locale.ROOT).contains(needle)) {
                    ids.add(id);
                    if (ids.size() >= 120) {
                        break;
                    }
                }
            }
        }

        List<Block> blocks = new ArrayList<>();
        for (ResourceLocation id : ids) {
            Block block = BuiltInRegistries.BLOCK.get(id);
            if (block != Blocks.AIR) {
                blocks.add(block);
            }
        }
        this.list.setBlocks(blocks);
        if (this.selected != null && blocks.contains(this.selected)) {
            this.list.selectBlock(this.selected);
        }
    }

    private void selectBlock(Block block) {
        this.selected = block;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        Double override = JetMaterialCosts.getBlockOverride(id);
        this.cost.setValue(override == null ? "" : format(override));
        this.cost.setHint(Component.literal(format(JetMaterialCosts.getBaseCost(block.defaultBlockState()))));
        this.status = Component.translatable("cumulative_cannons.materials.selected", id.toString());
    }

    private void applyCost() {
        if (this.selected == null) {
            this.status = Component.translatable("cumulative_cannons.materials.empty");
            return;
        }
        String value = this.cost.getValue().trim();
        if (value.isEmpty()) {
            resetCost();
            return;
        }
        try {
            double parsed = Double.parseDouble(value);
            if (parsed < 0.0D) {
                throw new NumberFormatException();
            }
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(this.selected);
            JetMaterialCosts.setBlockOverride(id, parsed);
            JetMaterialCosts.saveBlockOverrides();
            this.status = Component.translatable("cumulative_cannons.materials.saved");
            this.refresh(this.search.getValue());
        } catch (NumberFormatException e) {
            this.status = Component.translatable("cumulative_cannons.materials.invalid");
        }
    }

    private void resetCost() {
        if (this.selected == null) {
            this.status = Component.translatable("cumulative_cannons.materials.empty");
            return;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(this.selected);
        JetMaterialCosts.removeBlockOverride(id);
        JetMaterialCosts.saveBlockOverrides();
        this.cost.setValue("");
        this.status = Component.translatable("cumulative_cannons.materials.reset_done");
        this.refresh(this.search.getValue());
    }

    private static String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001D) {
            return Long.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        graphics.drawString(this.font, Component.translatable("cumulative_cannons.materials.cost_label"),
                this.rightX, 76, 0xD8D8C8, false);
        if (this.selected != null) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(this.selected);
            graphics.drawString(this.font, id.toString(), this.rightX, 52, 0xFFFFFF, false);
            graphics.drawString(this.font, Component.translatable("cumulative_cannons.materials.base",
                            format(JetMaterialCosts.getBaseCost(this.selected.defaultBlockState()))),
                    this.rightX, 172, 0x9AA08A, false);
        } else {
            graphics.drawString(this.font, Component.translatable("cumulative_cannons.materials.pick"),
                    this.rightX, 52, 0x9AA08A, false);
        }
        graphics.drawCenteredString(this.font, this.status, this.width / 2, this.height - 48, 0xD8D8C8);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    private final class BlockList extends ObjectSelectionList<BlockEntry> {
        private BlockList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return Math.max(140, this.width - 10);
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.getWidth() - 6;
        }

        private void setBlocks(List<Block> blocks) {
            this.clearEntries();
            this.setSelected(null);
            for (Block block : blocks) {
                this.addEntry(new BlockEntry(block));
            }
        }

        private void selectBlock(Block block) {
            for (BlockEntry entry : this.children()) {
                if (entry.block == block) {
                    this.setSelected(entry);
                    return;
                }
            }
        }
    }

    private final class BlockEntry extends ObjectSelectionList.Entry<BlockEntry> {
        private final Block block;
        private final Component label;
        private final String costLabel;

        private BlockEntry(Block block) {
            this.block = block;
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            this.label = Component.literal(id.toString());
            Double override = JetMaterialCosts.getBlockOverride(id);
            double value = override != null ? override : JetMaterialCosts.getBaseCost(block.defaultBlockState());
            this.costLabel = format(value);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            int color = hovering ? 0xFFFFD966 : 0xE6E6D8;
            graphics.drawString(BlockPenetrationScreen.this.font, this.label, left + 2, top + 6, color, false);
            graphics.drawString(BlockPenetrationScreen.this.font, this.costLabel,
                    left + width - 42, top + 6, 0x9AA08A, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                BlockPenetrationScreen.this.list.setSelected(this);
                BlockPenetrationScreen.this.selectBlock(this.block);
                return true;
            }
            return false;
        }

        @Override
        public Component getNarration() {
            return this.label;
        }
    }
}
