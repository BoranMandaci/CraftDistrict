package com.craftdistrict.lighting;

public enum LightMode {
    MANUAL, REDSTONE, PROXIMITY;

    private static final LightMode[] MODES = values();
    public static boolean valid(int id) { return id >= 0 && id < MODES.length; }
    public static LightMode byId(int id) { return valid(id) ? MODES[id] : MANUAL; }
    public String key() { return name().toLowerCase(java.util.Locale.ROOT); }
}
