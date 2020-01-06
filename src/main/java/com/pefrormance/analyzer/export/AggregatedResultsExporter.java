package com.pefrormance.analyzer.export;

import com.pefrormance.analyzer.model.Product;
import com.pefrormance.analyzer.model.ResultRowBean;
import com.pefrormance.analyzer.model.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;

public class AggregatedResultsExporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AggregatedResultsExporter.class);
    private static final String DB_URI_PREFIX = "jdbc:sqlite:file:";
    private static final String CREATE_TABLE_TEMPLATE = "CREATE TABLE %s (UpdateRegion TEXT, TaskName TEXT, '%s' REAL, '%s' REAL)";
    private static final String INSERT_TABLE_TEMPLATE = "INSERT INTO %s (UpdateRegion, TaskName, '%s', '%s') VALUES (?, ?, ?, ?)";

    private final String version1;
    private final String version2;
    private String resultDbFile;

    public AggregatedResultsExporter(Settings settings) {
        String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH_mm_ss").format(LocalDateTime.now());
        Path regionDbFile = settings.getResultsFolder().resolve(String.format("performance-analyzer%s.sq3", timestamp));
        this.resultDbFile = regionDbFile.toString();
        this.version1 = settings.getMap1Name();
        this.version2 = settings.getMap2Name();
    }

    public void init (Path resultsFolder)
    {
        Path regionDbPath = Paths.get(resultDbFile);
        try {
            Files.createDirectories(resultsFolder);
            Files.deleteIfExists(regionDbPath);
            Files.createFile(regionDbPath);
        } catch (IOException e) {
            LOGGER.error("Unable to init performance analyzer result file", e);
            throw new RuntimeException("Unable to init performance analyzer result file", e);
        }
    }

    public void export(Product product, Map<String, Collection<ResultRowBean>> resultRowsPerRegion)
    {
        LOGGER.info("Export data for product = "+ product);
        synchronized (AggregatedResultsExporter.class)
        {
            try(Connection connection = getConnection())
            {
                connection.createStatement().execute(getCreateQuery(product.getTableName()));
                connection.setAutoCommit(false);
                PreparedStatement statement = connection.prepareStatement(getInsertQuery(product.getTableName()));

                for (Map.Entry<String, Collection<ResultRowBean>> entry : resultRowsPerRegion.entrySet())
                {
                    String region = entry.getKey();
                    Collection<ResultRowBean> resultRows = entry.getValue();
                    for (ResultRowBean row : resultRows)
                    {
                        statement.setString(1, region);
                        statement.setString(2, row.getTaskName());
                        statement.setObject(3, row.getDurationMap1());
                        statement.setObject(4, row.getDurationMap2());
                        statement.addBatch();
                    }
                }

                statement.executeBatch();
                connection.commit();
                statement.close();
            }
            catch (SQLException e) {
                LOGGER.error(String.format("Unable to export data for product = %s", product), e);
            }
        }
    }

    public void vacuum()
    {
        try(Connection connection = getConnection();
            Statement statement = connection.createStatement();)
        {
            connection.setAutoCommit(true);
            statement.execute("VACUUM");
        }
        catch (SQLException e) {
            LOGGER.error("Unable to execute VACUUM", e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URI_PREFIX + resultDbFile);
    }

    private String getCreateQuery(String region)
    {
        return String.format(CREATE_TABLE_TEMPLATE, region, version1, version2);
    }

    private String getInsertQuery(String region)
    {
        return String.format(INSERT_TABLE_TEMPLATE, region, version1, version2);
    }
}
