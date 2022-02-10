package com.github.jenya705.rip;

import lombok.SneakyThrows;

import java.io.File;
import java.sql.*;

/**
 * @author Jenya705
 */
public class RipDatabase {

    private final Connection connection;

    public RipDatabase() throws ClassNotFoundException, SQLException {
        RipConfig config = Rip.getInstance().config();
        if (config.getSqlType().equalsIgnoreCase("sqlite")) {
            Class.forName("org.sqlite.jdbc4.JDBC4Connection");
            connection = DriverManager.getConnection(String.format(
                    "jdbc:sqlite://%s", new File(Rip.getInstance().getDataFolder(), "database.db").getAbsolutePath()
            ));
        }
        else if (config.getSqlType().equalsIgnoreCase("mysql")) {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(
                    String.format(
                            "jdbc:mysql://%s/%s",
                            config.getSqlHost(),
                            config.getSqlDatabase()
                    ),
                    config.getSqlUser(),
                    config.getSqlPassword()
            );
        }
        else {
            throw new IllegalArgumentException("Configured sql type is not supported");
        }
        update("""
                CREATE TABLE IF NOT EXISTS deaths (
                    uuidmost bigint not null,
                    uuidleast bigint not null,
                    locx bigint not null,
                    locy bigint not null,
                    locz bigint not null,
                    world text not null,
                    time timestamp DEFAULT CURRENT_TIMESTAMP
                );
                """);
    }

    @SneakyThrows
    public void update(String sql, Object... objects) {
        if (objects.length == 0) {
            Statement statement = connection.createStatement();
            statement.executeUpdate(sql);
        }
        else {
            PreparedStatement statement = connection.prepareStatement(sql);
            for (int i = 0; i < objects.length; ++i) {
                statement.setObject(i + 1, objects[i]);
            }
            statement.executeUpdate();
        }

    }

    @SneakyThrows
    public ResultSet query(String sql, Object... objects) {
        if (objects.length == 0) {
            return connection.createStatement().executeQuery(sql);
        }
        else {
            PreparedStatement statement = connection.prepareStatement(sql);
            for (int i = 0; i < objects.length; ++i) {
                statement.setObject(i + 1, objects[i]);
            }
            return statement.executeQuery();
        }

    }

}
