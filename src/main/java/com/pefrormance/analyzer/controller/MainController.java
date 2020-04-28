package com.pefrormance.analyzer.controller;

import com.pefrormance.analyzer.config.InjectedSettings;
import com.pefrormance.analyzer.service.TimeService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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
    public Button reset;
    @FXML
    private Button start;

    private final TimeService timeService;
    private final InjectedSettings settings;

    @FXML
    private void onMouseClicked(MouseEvent event) {
        settings.print();
        timeService.call();
        start.setDisable(true);

        /* Task<Void> task = new TasksTimeAnalyzer(settings);
        progressBar.progressProperty().bind(task.progressProperty());
        progressLabel.textProperty().bind(task.messageProperty());
        start.disableProperty().bind(task.runningProperty());
        reset.disableProperty().bind(task.runningProperty());
        new Thread(task).start();*/
    }
}
