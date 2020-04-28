package com.pefrormance.analyzer.repository;

import com.pefrormance.analyzer.config.InjectedSettings;
import com.pefrormance.analyzer.model.Product;
import com.pefrormance.analyzer.old_stuff.model.ResultRowBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;

@Slf4j
@Repository
public class ResultsExporter {
    private static final String DB_URI_PREFIX = "jdbc:sqlite:file:";
    private static final String CREATE_TABLE_TEMPLATE = "CREATE TABLE %s (UpdateRegion TEXT, TaskName TEXT, '%s' REAL, '%s' REAL)";
    private static final String INSERT_TABLE_TEMPLATE = "INSERT INTO %s (UpdateRegion, TaskName, '%s', '%s') VALUES (?, ?, ?, ?)";

    private final String version1;
    private final String version2;
    private final Path resultDbFile;

    public ResultsExporter(InjectedSettings settings) {
        String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH_mm_ss").format(LocalDateTime.now());
        this.resultDbFile = settings.getResultsFolder().resolve(String.format("performance-analyzer%s.sq3", timestamp));
        this.version1 = settings.getMap1Name();
        this.version2 = settings.getMap2Name();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(resultDbFile.getParent());
            Files.deleteIfExists(resultDbFile);
            Files.createFile(resultDbFile);
        } catch (IOException e) {
            log.error("Unable to init performance analyzer result file", e);
            throw new RuntimeException("Unable to init performance analyzer result file", e);
        }
    }

    public void export(Product product, Map<String, Collection<ResultRowBean>> resultRowsPerRegion) {
        log.info("Export data for product = " + product);
        synchronized (ResultsExporter.class) {
            try (Connection connection = getConnection()) {
                connection.createStatement().execute(getCreateQuery(product.getTableName()));
                connection.setAutoCommit(false);
                PreparedStatement statement = connection.prepareStatement(getInsertQuery(product.getTableName()));

                for (Map.Entry<String, Collection<ResultRowBean>> entry : resultRowsPerRegion.entrySet()) {
                    String region = entry.getKey();
                    Collection<ResultRowBean> resultRows = entry.getValue();
                    for (ResultRowBean row : resultRows) {
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
            } catch (SQLException e) {
                log.error(String.format("Unable to export data for product = %s", product), e);
            }
        }
    }

    public void vacuum() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();) {
            connection.setAutoCommit(true);
            statement.execute("VACUUM");
        } catch (SQLException e) {
            log.error("Unable to execute VACUUM", e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URI_PREFIX + resultDbFile);
    }

    private String getCreateQuery(String region) {
        return String.format(CREATE_TABLE_TEMPLATE, region, version1, version2);
    }

    private String getInsertQuery(String region) {
        return String.format(INSERT_TABLE_TEMPLATE, region, version1, version2);
    }
}
