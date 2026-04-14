package com.noozy.missionodyssey.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class ModKeybindings {

    public static final String CATEGORY = "key.categories.missionodyssey";

    public static final KeyMapping TEMPORAL_JUMP = new KeyMapping(
            "key.missionodyssey.temporal_jump",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    );
}
