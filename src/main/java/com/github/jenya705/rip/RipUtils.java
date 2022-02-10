package com.github.jenya705.rip;

import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Jenya705
 */
@UtilityClass
public class RipUtils {

    private String subStrEnd(String str, int last) {
        return str.substring(0, str.length() - last);
    }

    public Duration parseTime(String str) {
        Objects.requireNonNull(str, "string");
        if (str.endsWith("h")) {
            return Duration.ofHours(Integer.parseInt(subStrEnd(str, 1)));
        }
        if (str.endsWith("m")) {
            return Duration.ofMinutes(Integer.parseInt(subStrEnd(str, 1)));
        }
        if (str.endsWith("s")) {
            return Duration.ofSeconds(Integer.parseInt(subStrEnd(str, 1)));
        }
        if (str.endsWith("mi")) {
            return Duration.ofMillis(Integer.parseInt(subStrEnd(str, 2)));
        }
        throw new IllegalArgumentException("Given string is not parsable time");
    }

    public UUID parseUUID(long[] array) {
        return array.length == 2 ? new UUID(array[0], array[1]) : null;
    }

    public Location getLocationToFace(Location from, BlockFace face) {
        return getLocationToFace(from, face, 1);
    }

    public Location getLocationToFace(Location from, BlockFace face, int coefficient) {
        return from.add(
                face.getModX() * coefficient,
                face.getModY() * coefficient,
                face.getModZ() * coefficient
        );
    }


}
