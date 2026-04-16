package com.noozy.missionodyssey.registry;

import com.noozy.missionodyssey.MissionOdyssey;
import com.noozy.missionodyssey.recipe.TitaniumSmeltingRecipe;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, MissionOdyssey.MODID);
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, MissionOdyssey.MODID);

    public static final Supplier<RecipeType<TitaniumSmeltingRecipe>> TITANIUM_SMELTING_TYPE = TYPES.register("titanium_smelting", () -> new RecipeType<>() {
        @Override
        public String toString() { return "titanium_smelting"; }
    });

    public static final Supplier<RecipeSerializer<TitaniumSmeltingRecipe>> TITANIUM_SMELTING_SERIALIZER = SERIALIZERS.register("titanium_smelting",
            () -> new Serializer());

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
        TYPES.register(eventBus);
    }

    public static class Serializer implements RecipeSerializer<TitaniumSmeltingRecipe> {
        private static final MapCodec<TitaniumSmeltingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(TitaniumSmeltingRecipe::getInput),
                ItemStack.STRICT_CODEC.fieldOf("output").forGetter(r -> r.getResultItem(null)),
                Codec.INT.fieldOf("energy_usage").forGetter(TitaniumSmeltingRecipe::getEnergyUsage),
                Codec.INT.fieldOf("cooking_time").forGetter(TitaniumSmeltingRecipe::getCookingTime)
        ).apply(inst, TitaniumSmeltingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, TitaniumSmeltingRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, TitaniumSmeltingRecipe::getInput,
                ItemStack.STREAM_CODEC, r -> r.getResultItem(null),
                ByteBufCodecs.VAR_INT, TitaniumSmeltingRecipe::getEnergyUsage,
                ByteBufCodecs.VAR_INT, TitaniumSmeltingRecipe::getCookingTime,
                TitaniumSmeltingRecipe::new
        );

        @Override
        public MapCodec<TitaniumSmeltingRecipe> codec() { return CODEC; }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, TitaniumSmeltingRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
