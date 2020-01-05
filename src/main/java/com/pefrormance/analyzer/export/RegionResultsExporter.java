package com.pefrormance.analyzer.export;

import com.pefrormance.analyzer.model.TaskBean;
import com.pefrormance.analyzer.model.TasksHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RegionResultsExporter
{
    private static final Logger LOGGER = LoggerFactory.getLogger(RegionResultsExporter.class);
    private static final String DB_URI_PREFIX = "jdbc:sqlite:file:";
    private static final String CREATE_TABLE_TEMPLATE = "CREATE TABLE %s (TaskName TEXT, '%s' REAL, '%s' REAL)";
    private static final String INSERT_TABLE_TEMPLATE = "INSERT INTO %s (TaskName, '%s', '%s') VALUES (?, ?, ?)";

    private final String product;
    private final String version1;
    private final String version2;
    private String resultDbFile;

    public RegionResultsExporter(String product, String version1, String version2) {
        this.product = product;
        this.version1 = version1;
        this.version2 = version2;
    }

    // create dbFile + table
    public void init(Path resultsFolder) throws IOException
    {
        Path regionDbFile = resultsFolder.resolve(product + ".sq3");
        this.resultDbFile = regionDbFile.toString();
        Files.createDirectories(resultsFolder);
        Files.deleteIfExists(regionDbFile);
        Files.createFile(regionDbFile);
    }

    public void exportRegion(String region, TasksHolder first, TasksHolder second)
    {
        LOGGER.info("Export data for region = "+ region);
        Map<String, Double> tasksFirst =  first.getTasks().stream()
                .collect(Collectors.toMap(TaskBean::getName, TaskBean::getDuration));

        Map<String, Double> tasksSecond =  second.getTasks().stream()
                .collect(Collectors.toMap(TaskBean::getName, TaskBean::getDuration));

        Set<String> allTaskNames = new HashSet<>(tasksFirst.keySet());
        allTaskNames.addAll(tasksSecond.keySet());

        synchronized (RegionResultsExporter.class)
        {
            try(Connection connection = getConnection())
            {
                connection.createStatement().execute(getCreateQuery(region));
                connection.setAutoCommit(false);
                PreparedStatement statement = connection.prepareStatement(getInsertQuery(region));

                for (String taskName : allTaskNames)
                {
                    statement.setString(1, taskName);
                    statement.setObject(2, tasksFirst.get(taskName));
                    statement.setObject(3, tasksSecond.get(taskName));
                    statement.addBatch();
                }
                // add total
                statement.setString(1, "Total");
                statement.setDouble(2, first.getTotalTime().sum());
                statement.setDouble(3, second.getTotalTime().sum());
                statement.addBatch();
                statement.executeBatch();
                connection.commit();
                statement.close();
            }
            catch (SQLException e) {
               LOGGER.error(String.format("Unable to export data for region = %s", region), e);
            }
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
