package com.pefrormance.analyzer;

import com.pefrormance.analyzer.model.Settings;
import com.pefrormance.analyzer.service.TasksTimeAnalyzer;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TaskPerformanceApplication extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskPerformanceApplication.class);
    private static final String S3_PREFIX = "s3://";

    private Settings settings;
    @FXML
    private TextField map1Path;
    @FXML
    private TextField map2Path;
    @FXML
    private TextField map1Name;
    @FXML
    private TextField map2Name;
    @FXML
    private VBox products;
    @FXML
    private TextField updateRegion;
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

    private boolean validate() {
        if (map1Path.getText() == null || map1Path.getText().isEmpty() || !map1Path.getText().startsWith(S3_PREFIX)
                || map2Path.getText() == null || map2Path.getText().isEmpty() || !map2Path.getText().startsWith(S3_PREFIX)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Invalid path(s) to s3");
            alert.setHeaderText(null);
            alert.setContentText("Specify valid path to s3 for first and second paths!");
            alert.showAndWait();
            LOGGER.warn("Specified invalid path to s3://");
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
        AtomicInteger progress = new AtomicInteger();
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

            settings = new Settings.Builder()
                    .product(productCheckBoxes.stream().map(Labeled::getText).collect(Collectors.toSet()))
                    .updateRegion(updateRegion.getText())
                    .map1Path(map1Path.getText())
                    .map2Path(map2Path.getText())
                    .map1Name(map1Name.getText())
                    .map2Name(map2Name.getText())
                    .outputDir(outputDirPath.getText())
                    .build();

            if (!validate()) {
                return;
            }
            LOGGER.info(settings.toString());

            progressBar.progressProperty().unbind();
            progressLabel.textProperty().unbind();
            start.disableProperty().unbind();
            reset.disableProperty().unbind();

            Task<Void> task = new TasksTimeAnalyzer(settings);
            progressBar.progressProperty().bind(task.progressProperty());
            progressLabel.textProperty().bind(task.messageProperty());
            start.disableProperty().bind(task.runningProperty());
            reset.disableProperty().bind(task.runningProperty());
            new Thread(task).start();
        });
        // reset
        reset.setOnAction(action ->
        {
            map1Path.setText(null);
            map1Name.setText(null);
            map2Path.setText(null);
            map2Name.setText(null);
            updateRegion.setText(null);
            products.getChildren().stream()
                    .filter(c -> c.getClass().equals(CheckBox.class))
                    .map(c -> (CheckBox) c)
                    .forEach(c -> c.setSelected(false));
            outputDirPath.setText(null);
            outputDirPath.setEditable(true);
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
            progress.set(0);
            progressLabel.textProperty().unbind();
            progressLabel.setText("Progress: ");
            if (settings != null) {
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

    private void initElements(Parent root) {
        map1Path = (TextField) root.lookup("#map1Path");
        map1Name = (TextField) root.lookup("#map1Name");
        map2Path = (TextField) root.lookup("#map2Path");
        map2Name = (TextField) root.lookup("#map2Name");
        updateRegion = (TextField) root.lookup("#updateRegion");
        products = (VBox) root.lookup("#products");
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
