package org.jackhuang.CU.ui.animation;

import org.jackhuang.CU.setting.ConfigHolder;

public final class AnimationUtils {

    private AnimationUtils() {
    }

    /**
     * Trigger initialization of this class.
     * Should be called from {@link org.jackhuang.CU.setting.Settings#init()}.
     */
    @SuppressWarnings("JavadocReference")
    public static void init() {
    }

    private static final boolean enabled = !ConfigHolder.config().isAnimationDisabled();

    public static boolean isAnimationEnabled() {
        return enabled;
    }
}
