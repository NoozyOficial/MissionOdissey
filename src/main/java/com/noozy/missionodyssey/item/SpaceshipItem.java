package com.noozy.missionodyssey.item;

import com.noozy.missionodyssey.entity.SpaceshipEntity;
import com.noozy.missionodyssey.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class SpaceshipItem extends Item {

    public SpaceshipItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();

        if (!level.isClientSide()) {
            BlockPos pos = context.getClickedPos().above();
            SpaceshipEntity ship = new SpaceshipEntity(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            ship.setYRot(context.getPlayer() != null ? context.getPlayer().getYRot() : 0);
            level.addFreshEntity(ship);

            if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
