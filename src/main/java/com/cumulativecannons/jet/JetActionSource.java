package com.cumulativecannons.jet;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import team.creative.littletiles.common.action.source.LittleActionSource;
import team.creative.littletiles.common.ingredient.LittleInventory;

public class JetActionSource implements LittleActionSource {
    private final Level level;

    public JetActionSource(Level level) {
        this.level = level;
    }

    @Override
    public Level getActionLevel() {
        return level;
    }

    @Override
    public ItemStack getActionItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isPlayer() {
        return false;
    }

    @Override
    public HolderLookup.Provider getActionRegistry() {
        return level.registryAccess();
    }

    @Override
    public LittleInventory createInventory() {
        return null;
    }

    @Override
    public boolean needsIngredients() {
        return false;
    }

    @Override
    public void broadcastChanges() {
    }

    @Override
    public void playSound(SoundEvent event, SoundSource source, float volume, float pitch) {
    }

    @Override
    public boolean addStack(ItemStack stack) {
        return false;
    }

    @Override
    public void dropStack(ItemStack stack) {
    }

    @Override
    public boolean isClient() {
        return false;
    }

    @Override
    public void sendText(Component translatable) {
    }

    @Override
    public void requestInventoryUpdate() {
    }
}
