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
import java.util.Collection;

/**
 * Created by konstantin on 22.12.2019.
 */
public class SqliteExporter implements Exporter {
    private static final String DB_URI_PREFIX = "jdbc:sqlite:file:";
    private static final String CREATE_TABLE_TEMPLATE = "CREATE TABLE %s (UpdateRegion TEXT, Details CLOB)";
    private static final String INSERT_TABLE_TEMPLATE = "INSERT INTO %s (UpdateRegion, Details) VALUES (?, ?)";
    private  final String product;
    private  final String resultDbFile;

    private SqliteExporter(SqliteExporterBuilder builder) {
        Settings settings = builder.settings;
        this.product = builder.product;
        this.resultDbFile = settings.getResultsFolder().resolve("AGGREGATED_LOG" + settings.getOutputFormat().getFormat()).toString();
    }

    @Override
    public void init(String product, Settings settings) throws IOException {
        Path outputDbFile = Paths.get(resultDbFile);
        Files.createDirectories(settings.getResultsFolder());
        if (!Files.exists(outputDbFile))
        {
            Files.createFile(outputDbFile);
        }
      /*  Files.deleteIfExists(outputDbFile);
        Files.createFile(outputDbFile);*/
    }

    @Override
    public void export(Collection<String> data) {
        synchronized (SqliteExporter.class)
        {
            try(Connection connection = getConnection())
            {
                connection.createStatement().execute(getCreateQuery(product));
                connection.setAutoCommit(false);
                PreparedStatement statement = connection.prepareStatement(getInsertQuery(product));

                for (String row : data)
                {
                    String [] values = row.split(",");
                    statement.setString(1, values[0]);
                    statement.setString(2, values[1]);
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

    public static final class SqliteExporterBuilder implements ExporterBuilder
    {
        private String product;
        private Settings settings;

        @Override
        public ExporterBuilder product(String product) {
            this.product = product;
            return this;
        }

        @Override
        public ExporterBuilder settings(Settings settings) {
            this.settings = settings;
            return this;
        }

        @Override
        public Exporter build() {
            return new SqliteExporter(this);
        }
    }
}
