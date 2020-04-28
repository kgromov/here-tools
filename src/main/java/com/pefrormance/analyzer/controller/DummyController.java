package com.pefrormance.analyzer.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

/**
 * Created by konstantin on 28.04.2020.
 */
@Slf4j
@Controller
public class DummyController {

    @FXML
    public Button start;

    public void onMouseClicked(MouseEvent mouseEvent) {
        log.info("Hello from dummy!");
    }
}
