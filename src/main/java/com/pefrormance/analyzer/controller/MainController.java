package com.pefrormance.analyzer.controller;

import com.pefrormance.analyzer.component.Progresser;
import com.pefrormance.analyzer.config.InjectedSettings;
import com.pefrormance.analyzer.service.TimeService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.MouseEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

/**
 * Created by konstantin on 28.04.2020.
 */
@Controller
@RequiredArgsConstructor
public class MainController {
    @FXML
    public ProgressBar progressBar;
    @FXML
    public Label progressLabel;
    @FXML
    public Button reset;
    @FXML
    private Button start;

    private final TimeService timeService;
    private final InjectedSettings settings;
    private final Progresser progresser;

    public void initialize()
    {
        progressBar.setProgress(0);
    }

    @FXML
    private void onMouseClicked(MouseEvent event) {
        settings.print();

        // tempalate2
        progressBar.progressProperty().bind(progresser.progressProperty());
        progressLabel.textProperty().bind(progresser.messageProperty());
        start.disableProperty().bind(progresser.runningProperty());
        reset.disableProperty().bind(progresser.runningProperty());

        timeService.call();
    }
}
