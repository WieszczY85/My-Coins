package pl.mymc.mycoins.myc;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class RankMultiplier {
    private final Map<String, Double> rankMultipliers;

    public RankMultiplier(FileConfiguration config) {
        this.rankMultipliers = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("reward.multipliers");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                rankMultipliers.put(key, config.getDouble("reward.multipliers." + key));
            }
        }
    }

    public double getMultiplier(Player player) {
        for (String rank : rankMultipliers.keySet()) {
            if (player.hasPermission("mycoins.rank." + rank)) {
                return rankMultipliers.get(rank);
            }
        }
        return 1.0;
    }
}
