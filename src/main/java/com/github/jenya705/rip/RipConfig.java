package com.github.jenya705.rip;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.Duration;

/**
 * @author Jenya705
 */
@Data
@NoArgsConstructor
public class RipConfig {

    private Duration chestDespawnTime;
    private Duration locationNotifyTime;
    private Duration deathNotifyTime;

    private boolean spawnSign;

    private boolean tryNotToReplace;
    private int tryingRadius;

    private int customModelData;

    private String locationNotifyMessage;

    private String sqlType;
    private String sqlHost;
    private String sqlUser;
    private String sqlPassword;
    private String sqlDatabase;

    public RipConfig(FileConfiguration config) {
        chestDespawnTime = RipUtils.parseTime(config.getString("chest-despawn-time"));
        locationNotifyTime = RipUtils.parseTime(config.getString("location-notify-time"));
        deathNotifyTime = RipUtils.parseTime(config.getString("death-notify-time"));
        spawnSign = config.getBoolean("spawn-sign");
        tryNotToReplace = config.getBoolean("try-not-to-replace");
        tryingRadius = config.getInt("trying-radius");
        customModelData = config.getInt("custom-model-data");
        locationNotifyMessage = config.getString("location-notify-message");
        sqlType = config.getString("sql.type");
        sqlHost = config.getString("sql.host");
        sqlUser = config.getString("sql.user");
        sqlPassword = config.getString("sql.password");
        sqlDatabase = config.getString("sql.database");
    }

}
