package com.pefrormance.analyzer.export;

import com.pefrormance.analyzer.model.Settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

/**
 * Created by konstantin on 22.12.2019.
 */
public class SqliteExporter implements Exporter {
    private static final String DB_URI_PREFIX = "jdbc:sqlite:file:";
    // TODO: or move to enum {value, tableName}
    private static final String CREATE_TABLE_TEMPLATE = "CREATE TABLE PRODUCT_%s (UpdateRegion TEXT, Details CLOB)";
    private static final String INSERT_TABLE_TEMPLATE = "INSERT INTO PRODUCT_%s (UpdateRegion, Details) VALUES (?, ?)";
    private  final String resultDbFile;


    public SqliteExporter(Settings settings) {
        this.resultDbFile = settings.getResultsFolder().resolve("AGGREGATED_LOG" + settings.getOutputFormat().getFormat()).toString();
    }

    @Override
    public void init(Settings settings) throws IOException {
        Path outputDbFile = Paths.get(resultDbFile);
        Files.createDirectories(settings.getResultsFolder());
        Files.deleteIfExists(outputDbFile);
        Files.createFile(outputDbFile);
    }

    @Override
    public void export(String product, Collection<String> data) {
        synchronized (SqliteExporter.class)
        {
            try(Connection connection = getConnection())
            {
                connection.createStatement().execute(getCreateQuery(product));
                connection.setAutoCommit(false);
                PreparedStatement statement = connection.prepareStatement(getInsertQuery(product));

                for (String row : data)
                {
                    // TODO: exclude data; logLevel to another column
                    // AUS,2019-12-13 00:53:16,512 - WARN  -> [com.navteq.psf.basicnav.bmd.jts.JtsIO] Found a not simple geometry with 621 points
                    // [AUS, 2019-12-13 00:53:16, 512 - WARN  -> [com.navteq.psf.basicnav.bmd.jts.JtsIO] Found a not simple geometry with 621 points]
                    String [] values = row.split(",");
                    statement.setString(1, values[0]);
                    statement.setString(2, values.length > 2 ? values[2] : values[1]);
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();
                statement.close();
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void vacuum()
    {
        try(Connection connection = getConnection();
            Statement statement = connection.createStatement();)
        {
            statement.execute("VACUUM");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URI_PREFIX + resultDbFile);
    }

    private String getCreateQuery(String product)
    {
        return String.format(CREATE_TABLE_TEMPLATE, product);
    }

    private String getInsertQuery(String product)
    {
        return String.format(INSERT_TABLE_TEMPLATE, product);
    }
}
