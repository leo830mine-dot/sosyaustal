package dev.flr.visuals;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

public final class FlrKeys {
    public static final KeyMapping MENU = new KeyMapping("key.flrvisuals.menu", InputConstants.Type.KEYSYM,
            InputConstants.KEY_RSHIFT, "key.categories.flrvisuals");
    public static final KeyMapping MASTER = new KeyMapping("key.flrvisuals.master", InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(), "key.categories.flrvisuals");
    private FlrKeys() {}
}
