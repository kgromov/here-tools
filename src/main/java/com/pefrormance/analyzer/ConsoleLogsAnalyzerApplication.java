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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
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
    private ChoiceBox<String> logLevel;
    @FXML
    private TextField updateRegion;
    @FXML
    private TextField expressionToFind;
    @FXML
    private CheckBox regExp;
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
        String mapPathText = mapPath.getText();
        StringBuilder warningMessages = new StringBuilder();
        if (mapPathText == null || mapPathText.isEmpty() || !mapPathText.startsWith(S3_PREFIX))
        {
            warningMessages.append("* Specify valid path to s3 map logs!").append('\n');
        }
        if (logFile == LogFile.NONE)
        {
            warningMessages.append("* Specify valid log level to parse!").append('\n');
        }
        String expression = expressionToFind.getText();
        if (expression == null || expression.isEmpty())
        {
            warningMessages.append("* Specify valid expression to find!").append('\n');
        }
        else if (regExp.isSelected())
        {
            try
            {
               Pattern.compile(expression);
            }
            catch (PatternSyntaxException e)
            {
                warningMessages.append("* Specify valid regular expression!");
            }
        }

        String warningMessage = warningMessages.toString();
        if (!warningMessage.isEmpty())
        {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setHeaderText(null);
            alert.setTitle("Input data filled incorrectly!");
            alert.setContentText(warningMessage);
            alert.showAndWait();
            LOGGER.warn(warningMessage);
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

            if (!validate(logFile)) {
                return;
            }

            String expression = expressionToFind.getText();
            Pattern pattern = regExp.isSelected() ? Pattern.compile(expression) : null;

            settings = new Settings.Builder()
                    .product(productCheckBoxes.stream().map(Labeled::getText).map(Product::getProductByName).collect(Collectors.toSet()))
                    .updateRegion(updateRegion.getText())
                    .mapPath(mapPath.getText())
                    .expressionToFind(expression)
                    .expressionPattern(pattern)
                    .logFile(logFile)
                    .outputDir(outputDirPath.getText())
                    .build();
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
            regExp.setSelected(false);
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
        regExp = (CheckBox) root.lookup("#regExp");
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
