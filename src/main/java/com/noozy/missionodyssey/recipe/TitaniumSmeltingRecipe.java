package com.noozy.missionodyssey.recipe;

import com.noozy.missionodyssey.MissionOdyssey;
import com.noozy.missionodyssey.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

public class TitaniumSmeltingRecipe implements Recipe<RecipeWrapper> {
    private final Ingredient input;
    private final ItemStack output;
    private final int energyUsage;
    private final int cookingTime;

    public TitaniumSmeltingRecipe(Ingredient input, ItemStack output, int energyUsage, int cookingTime) {
        this.input = input;
        this.output = output;
        this.energyUsage = energyUsage;
        this.cookingTime = cookingTime;
    }

    @Override
    public boolean matches(RecipeWrapper container, Level level) {
        return input.test(container.getItem(0));
    }

    @Override
    public ItemStack assemble(RecipeWrapper container, HolderLookup.Provider registries) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return output;
    }

    public Ingredient getInput() { return input; }
    public int getEnergyUsage() { return energyUsage; }
    public int getCookingTime() { return cookingTime; }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.TITANIUM_SMELTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.TITANIUM_SMELTING_TYPE.get();
    }
}
