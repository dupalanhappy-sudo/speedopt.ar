package com.speedopt;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class SpeedOpt implements ClientModInitializer {
    public static Config config;

    @Override
    public void onInitializeClient() {
        config = Config.load();
    }

    /** Current world time in ticks (0 if not in a world). */
    public static long now() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0 : mc.level.getGameTime();
    }

    public static void say(String s) {
        Minecraft.getInstance().gui.getChat().addMessage(Component.literal("[SpeedOpt] " + s));
    }

    /**
     * Chat commands (start with a dot, never sent to the server):
     *   .speedopt crystal <1-10>
     *   .speedopt anchor <1-10>
     *   .speedopt setdelay crystal|anchor <level> <ticks>
     *   .speedopt toggle | togglecrystal | toggleanchor | status
     * Returns true if the message was a SpeedOpt command.
     */
    public static boolean handle(String msg) {
        if (!msg.toLowerCase().startsWith(".speedopt")) return false;
        String[] a = msg.trim().split("\\s+");
        try {
            if (a.length >= 3 && a[1].equalsIgnoreCase("crystal")) {
                config.crystalLevel = range(a[2], 1, 10);
                say("Crystal speed = " + config.crystalLevel + " (delay " + config.crystalDelay() + " ticks)");
            } else if (a.length >= 3 && a[1].equalsIgnoreCase("anchor")) {
                config.anchorLevel = range(a[2], 1, 10);
                say("Anchor speed = " + config.anchorLevel + " (delay " + config.anchorDelay() + " ticks)");
            } else if (a.length >= 5 && a[1].equalsIgnoreCase("setdelay")) {
                int lvl = range(a[3], 1, 10);
                int t = range(a[4], 0, 40);
                if (a[2].equalsIgnoreCase("crystal")) config.crystalDelays[lvl - 1] = t;
                else if (a[2].equalsIgnoreCase("anchor")) config.anchorDelays[lvl - 1] = t;
                else { say("Use: .speedopt setdelay crystal|anchor <level> <ticks>"); return true; }
                say(a[2] + " level " + lvl + " delay = " + t + " ticks");
            } else if (a.length >= 2 && a[1].equalsIgnoreCase("toggle")) {
                config.enabled = !config.enabled;
                say("Speed Optimizer " + (config.enabled ? "ON" : "OFF"));
            } else if (a.length >= 2 && a[1].equalsIgnoreCase("togglecrystal")) {
                config.crystalEnabled = !config.crystalEnabled;
                say("Crystal optimizer " + (config.crystalEnabled ? "ON" : "OFF"));
            } else if (a.length >= 2 && a[1].equalsIgnoreCase("toggleanchor")) {
                config.anchorEnabled = !config.anchorEnabled;
                say("Anchor optimizer " + (config.anchorEnabled ? "ON" : "OFF"));
            } else if (a.length >= 2 && a[1].equalsIgnoreCase("status")) {
                say("Enabled: " + config.enabled);
                say("Crystal: " + (config.crystalEnabled ? "ON" : "OFF") + " | level " + config.crystalLevel
                        + " | delay " + config.crystalDelay() + "t");
                say("Anchor: " + (config.anchorEnabled ? "ON" : "OFF") + " | level " + config.anchorLevel
                        + " | delay " + config.anchorDelay() + "t");
            } else {
                say("Commands: crystal <1-10>, anchor <1-10>, setdelay crystal|anchor <level> <ticks>, toggle, togglecrystal, toggleanchor, status");
            }
            config.save();
        } catch (NumberFormatException e) {
            say("That needs to be a number in range.");
        }
        return true;
    }

    private static int range(String s, int lo, int hi) {
        int v = Integer.parseInt(s);
        if (v < lo || v > hi) throw new NumberFormatException();
        return v;
    }
}
