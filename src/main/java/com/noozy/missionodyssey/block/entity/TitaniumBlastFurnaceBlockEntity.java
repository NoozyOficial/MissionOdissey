package com.noozy.missionodyssey.block.entity;

import com.noozy.missionodyssey.block.TitaniumBlastFurnaceBlock;
import com.noozy.missionodyssey.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.noozy.missionodyssey.network.TitaniumBlastFurnaceStatePayload;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.PacketDistributor;

import com.noozy.missionodyssey.recipe.TitaniumSmeltingRecipe;
import com.noozy.missionodyssey.registry.ModRecipes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import org.jetbrains.annotations.Nullable;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import java.util.Optional;

public class TitaniumBlastFurnaceBlockEntity extends BlockEntity implements MenuProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    public enum State { PRE_ASSEMBLE, ASSEMBLE, IDLE, RUNNING }
    private State currentState = State.PRE_ASSEMBLE;
    private long stateStartTime = 0;

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0;
        }

    };

    private final EnergyStorage energyStorage = new EnergyStorage(10000, 1000, 1000) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
            }
            return received;
        }
    };

    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = 100;

    public TitaniumBlastFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TITANIUM_BLAST_FURNACE.get(), pos, state);
        if (state.getValue(TitaniumBlastFurnaceBlock.ASSEMBLED)) {
            currentState = State.IDLE;
        }
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> TitaniumBlastFurnaceBlockEntity.this.progress;
                    case 1 -> TitaniumBlastFurnaceBlockEntity.this.maxProgress;
                    case 2 -> TitaniumBlastFurnaceBlockEntity.this.energyStorage.getEnergyStored();
                    case 3 -> TitaniumBlastFurnaceBlockEntity.this.energyStorage.getMaxEnergyStored();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> TitaniumBlastFurnaceBlockEntity.this.progress = value;
                    case 1 -> TitaniumBlastFurnaceBlockEntity.this.maxProgress = value;
                }
            }

            @Override
            public int getCount() { return 4; }
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.missionodyssey.titanium_blast_furnace");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new com.noozy.missionodyssey.screen.TitaniumBlastFurnaceMenu(containerId, inventory, this, this.data);
    }

    public void setState(State newState) {
        if (this.currentState != newState) {
            this.currentState = newState;
            this.stateStartTime = level != null ? level.getGameTime() : 0;
            if (level != null && !level.isClientSide) {
                for (net.minecraft.server.level.ServerPlayer player : ((ServerLevel) level).players()) {
                    player.connection.send(new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(new TitaniumBlastFurnaceStatePayload(this.getBlockPos(), newState, this.stateStartTime)));
                }
            }
            setChanged();
            if (level != null && level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    public void clientSetState(State newState, long serverTime) {
        this.currentState = newState;
        this.stateStartTime = serverTime;
        setChanged();
    }

    public void onAssembled() {
        setState(State.ASSEMBLE);
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, TitaniumBlastFurnaceBlockEntity be) {
        if (level.isClientSide) return;

        if (be.currentState == State.ASSEMBLE && level.getGameTime() - be.stateStartTime > 40) {
            be.setState(State.IDLE);
        }

        if (state.getValue(TitaniumBlastFurnaceBlock.ASSEMBLED)) {
            if (be.hasRecipe()) {
                Optional<TitaniumSmeltingRecipe> recipe = be.getRecipe();
                if (recipe.isPresent()) {
                    TitaniumSmeltingRecipe r = recipe.get();
                    be.maxProgress = r.getCookingTime();
                    if (be.energyStorage.getEnergyStored() >= r.getEnergyUsage()) {
                        be.setState(State.RUNNING);
                        be.energyStorage.extractEnergy(r.getEnergyUsage(), false);
                        be.progress++;
                        if (be.progress >= be.maxProgress) {
                            be.craftItem(r);
                            be.progress = 0;
                        }
                        be.setChanged();
                    } else {
                        be.setState(State.IDLE);
                    }
                }
            } else {
                be.progress = 0;
                if (be.currentState == State.RUNNING) {
                    be.setState(State.IDLE);
                }
                be.setChanged();
            }
        }
    }

    private boolean hasRecipe() {
        Optional<TitaniumSmeltingRecipe> recipe = getRecipe();
        if (recipe.isEmpty()) return false;

        ItemStack result = recipe.get().getResultItem(level.registryAccess());
        return canInsertItemIntoOutputSlot(result) && canInsertAmountIntoOutputSlot(result.getCount());
    }

    private Optional<TitaniumSmeltingRecipe> getRecipe() {
        return level.getRecipeManager().getRecipeFor(ModRecipes.TITANIUM_SMELTING_TYPE.get(), new RecipeWrapper(itemHandler), level)
                .map(net.minecraft.world.item.crafting.RecipeHolder::value);
    }

    private void craftItem(TitaniumSmeltingRecipe recipe) {
        itemHandler.extractItem(0, 1, false);
        ItemStack result = recipe.getResultItem(level.registryAccess()).copy();
        ItemStack current = itemHandler.getStackInSlot(1);
        if (current.isEmpty()) {
            itemHandler.setStackInSlot(1, result);
        } else {
            current.grow(result.getCount());
        }
    }

    private boolean canInsertItemIntoOutputSlot(ItemStack stack) {
        return itemHandler.getStackInSlot(1).isEmpty() || itemHandler.getStackInSlot(1).is(stack.getItem());
    }

    private boolean canInsertAmountIntoOutputSlot(int count) {
        return itemHandler.getStackInSlot(1).getMaxStackSize() >= itemHandler.getStackInSlot(1).getCount() + count;
    }

    public State getCurrentState() {
        return currentState;
    }

    public float getAnimTime(float partialTicks) {
        if (level == null) return 0;
        return (level.getGameTime() - stateStartTime + partialTicks) / 20f;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("state", currentState.name());
        tag.putLong("startTime", stateStartTime);
        tag.putInt("progress", progress);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("energy", energyStorage.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("state")) {
            currentState = State.valueOf(tag.getString("state"));
        }
        stateStartTime = tag.getLong("startTime");
        progress = tag.getInt("progress");
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        energyStorage.receiveEnergy(tag.getInt("energy"), false);
    }

    public IItemHandler getItemHandler(@Nullable Direction side, Direction facing) {
        if (side == null) return itemHandler;
        if (side == getLeft(facing)) {
            return new net.neoforged.neoforge.items.wrapper.RangedWrapper(itemHandler, 0, 1);
        }
        if (side == getRight(facing)) {
            return new net.neoforged.neoforge.items.wrapper.RangedWrapper(itemHandler, 1, 2);
        }
        return null;
    }

    public IEnergyStorage getEnergyHandler(@Nullable Direction side, Direction facing) {
        if (side == null) return energyStorage;
        Direction left = getLeft(facing);
        Direction right = getRight(facing);
        if (side == facing || side == left || side == right) {
            return null;
        }
        return energyStorage;
    }

    private Direction getLeft(Direction facing) { return facing.getClockWise(); }
    private Direction getRight(Direction facing) { return facing.getCounterClockWise(); }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}
