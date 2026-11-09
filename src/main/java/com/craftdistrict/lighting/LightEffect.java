package com.craftdistrict.lighting;

import net.minecraft.util.StringRepresentable;

/** Six settings with deterministic, deliberately low-frequency Minecraft light changes. */
public enum LightEffect implements StringRepresentable {
    STEADY, SLOW_BLINK, FAST_BLINK, PULSE, DOUBLE_FLASH, FLICKER;

    private static final LightEffect[] EFFECTS = values();
    public static boolean valid(int id) { return id >= 0 && id < EFFECTS.length; }
    public static LightEffect byId(int id) { return valid(id) ? EFFECTS[id] : STEADY; }
    @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }

    public int levelAt(long time) {
        return switch (this) {
            case STEADY -> 15;
            case SLOW_BLINK -> Math.floorMod(time, 80) < 40 ? 15 : 0;
            case FAST_BLINK -> Math.floorMod(time, 24) < 12 ? 15 : 0;
            case PULSE -> switch ((int)(Math.floorMod(time, 72) / 12)) {
                case 0 -> 0; case 1, 5 -> 5; case 2, 4 -> 10; default -> 15;
            };
            case DOUBLE_FLASH -> {
                int tick = (int)Math.floorMod(time, 80);
                yield tick < 8 || tick >= 16 && tick < 24 ? 15 : 0;
            }
            case FLICKER -> {
                int tick = (int)Math.floorMod(time, 112);
                yield tick < 16 || tick >= 24 && tick < 64 || tick >= 76 ? 15 : 0;
            }
        };
    }

    /** Keep the six original saved names. Only Pulse needs extra positive brightness states. */
    public enum Stage implements StringRepresentable {
        STEADY(LightEffect.STEADY, 15), SLOW_BLINK(LightEffect.SLOW_BLINK, 15),
        FAST_BLINK(LightEffect.FAST_BLINK, 15), PULSE(LightEffect.PULSE, 15),
        DOUBLE_FLASH(LightEffect.DOUBLE_FLASH, 15), FLICKER(LightEffect.FLICKER, 15),
        PULSE_LOW(LightEffect.PULSE, 5), PULSE_MID(LightEffect.PULSE, 10);

        public final LightEffect effect;
        public final int level;
        Stage(LightEffect effect, int level) { this.effect = effect; this.level = level; }
        public static Stage of(LightEffect effect, int level) {
            return switch (effect) {
                case STEADY -> STEADY; case SLOW_BLINK -> SLOW_BLINK; case FAST_BLINK -> FAST_BLINK;
                case PULSE -> level == 5 ? PULSE_LOW : level == 10 ? PULSE_MID : PULSE;
                case DOUBLE_FLASH -> DOUBLE_FLASH; case FLICKER -> FLICKER;
            };
        }
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
}
