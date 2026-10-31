package com.speedopt;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Saved to .minecraft/config/speedoptimizer.json. Delays are in ticks (20 ticks = 1 second). */
public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("speedoptimizer.json");

    public boolean enabled = true;
    public boolean crystalEnabled = true;
    public boolean anchorEnabled = true;

    public int crystalLevel = 10;
    public int anchorLevel = 10;

    // Index 0 = level 1 (slowest) ... index 9 = level 10 (fastest, 0 delay)
    public int[] crystalDelays = {10, 8, 7, 6, 5, 4, 3, 2, 1, 0};
    public int[] anchorDelays  = {12, 10, 8, 7, 6, 5, 4, 3, 1, 0};

    public int crystalDelay() { return crystalDelays[clamp(crystalLevel) - 1]; }
    public int anchorDelay()  { return anchorDelays[clamp(anchorLevel) - 1]; }

    public static int clamp(int lvl) { return Math.max(1, Math.min(10, lvl)); }

    public static Config load() {
        try {
            if (Files.exists(FILE)) {
                Config c = GSON.fromJson(Files.readString(FILE), Config.class);
                if (c != null && c.crystalDelays != null && c.crystalDelays.length == 10
                        && c.anchorDelays != null && c.anchorDelays.length == 10) {
                    c.crystalLevel = clamp(c.crystalLevel);
                    c.anchorLevel = clamp(c.anchorLevel);
                    return c;
                }
            }
        } catch (Exception ignored) {}
        Config c = new Config();
        c.save();
        return c;
    }

    public void save() {
        try { Files.writeString(FILE, GSON.toJson(this)); } catch (IOException ignored) {}
    }
}
