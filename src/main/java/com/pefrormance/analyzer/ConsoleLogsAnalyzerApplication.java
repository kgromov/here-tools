package com.pefrormance.analyzer;

import com.pefrormance.analyzer.model.LogFile;
import com.pefrormance.analyzer.model.Product;
import com.pefrormance.analyzer.model.Settings;
import com.pefrormance.analyzer.service.ConsoleLogsManager;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

public class ConsoleLogsAnalyzerApplication extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleLogsAnalyzerApplication.class);
    private static final String S3_PREFIX = "s3://";

    private Settings settings;
    @FXML
    private TextField mapPath;
    @FXML
    private VBox products;
    @FXML
    private HBox outputFormat;
    @FXML
    private ChoiceBox<String> logLevel;
    @FXML
    private TextField updateRegion;
    @FXML
    private TextField expressionToFind;
    @FXML
    private TextField outputDirPath;
    @FXML
    private Button outputDir;
    @FXML
    private Button start;
    @FXML
    private Button reset;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label progressLabel;

    private boolean validate(LogFile logFile)
    {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);

        String mapPathText = mapPath.getText();
        if (mapPathText == null || mapPathText.isEmpty() || !mapPathText.startsWith(S3_PREFIX))
        {
            alert.setTitle("Invalid path to s3");
            alert.setContentText("Specify valid path to s3 map logs!");
            alert.showAndWait();
            LOGGER.warn("Specified invalid path to s3://" + mapPathText);
            return false;
        }
        if (logFile == LogFile.NONE)
        {
            alert.setTitle("LogLevel is not specified");
            alert.setContentText("Specify valid log level to parse!");
            alert.showAndWait();
            LOGGER.warn("Log level to parse is not specified");
            return false;
        }
        return true;
    }

    @Override
    public void start(Stage stage) throws Exception {
        /*
         * 1) FXMLLoader.load(getClass().getResource("/template.fxml"));
         * 2) Thread.currentThread().getContextClassLoader().getResourceAsStream("template.fxml");
         * 3) getClass().getClassLoader().getResourceAsStream("/template.fxml");
         */
        // TODO: or try first without controllers
        Parent root = FXMLLoader.load(getClass().getResource("/template.fxml"));

        initElements(root);

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setInitialDirectory(Paths.get(".").normalize().toAbsolutePath().toFile());
        //start
        start.setOnAction(e ->
        {
            Set<CheckBox> productCheckBoxes = products.getChildren().stream()
                    .filter(c -> c.getClass().equals(CheckBox.class))
                    .map(c -> (CheckBox) c)
                    .filter(CheckBox::isSelected)
                    .collect(Collectors.toSet());

            LogFile logFile = LogFile.getLogFile(logLevel.getValue());

            settings = new Settings.Builder()
                    .product(productCheckBoxes.stream().map(Labeled::getText).map(Product::getProductByName).collect(Collectors.toSet()))
                    .updateRegion(updateRegion.getText())
                    .mapPath(mapPath.getText())
                    .expressionToFind(expressionToFind.getText())
                    .logFile(logFile)
                    .outputDir(outputDirPath.getText())
                    .build();

            if (!validate(logFile)) {
                return;
            }
            LOGGER.info(settings.toString());

            progressBar.progressProperty().unbind();
            progressLabel.textProperty().unbind();
            start.disableProperty().unbind();
            reset.disableProperty().unbind();

            Task<Void> task = new ConsoleLogsManager(settings);
            progressBar.progressProperty().bind(task.progressProperty());
            progressLabel.textProperty().bind(task.messageProperty());
            start.disableProperty().bind(task.runningProperty());
            reset.disableProperty().bind(task.runningProperty());
            new Thread(task).start();
        });
        // reset
        reset.setOnAction(action ->
        {
            mapPath.setText(null);
            updateRegion.setText(null);
            products.getChildren().stream()
                    .filter(c -> c.getClass().equals(CheckBox.class))
                    .map(c -> (CheckBox) c)
                    .forEach(c -> c.setSelected(false));
            logLevel.setValue("--None--");
            expressionToFind.setText(null);
            outputDirPath.setText(null);
            outputDirPath.setEditable(true);
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
            progressLabel.textProperty().unbind();
            progressLabel.setText("Progress: ");
            if (settings != null)
            {
                settings.reset();
            }
        });
        outputDir.setOnAction(action ->
        {
            File selectedDirectory = chooser.showDialog(stage);
            outputDirPath.setText(selectedDirectory.getAbsolutePath());
            outputDirPath.setEditable(false);

        });

        stage.setTitle("Tasks performance analyzer");
        Scene scene = new Scene(root, 500, 500);
        stage.setScene(scene);
        stage.show();
    }

    private void initElements(Parent root)
    {
        mapPath = (TextField) root.lookup("#mapPath");
        updateRegion = (TextField) root.lookup("#updateRegion");
        products = (VBox) root.lookup("#products");
        logLevel = (ChoiceBox<String>) root.lookup("#logLevel");
        expressionToFind = (TextField) root.lookup("#expression");
        outputDir = (Button) root.lookup("#outputDir");
        outputDirPath = (TextField) root.lookup("#outputDirPath");
        start = (Button) root.lookup("#start");
        reset = (Button) root.lookup("#reset");
        progressBar = (ProgressBar) root.lookup("#progressBar");
        progressBar.setProgress(0);
        progressLabel = (Label) root.lookup("#progressLabel");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
