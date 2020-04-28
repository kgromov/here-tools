package com.pefrormance.analyzer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Created by konstantin on 27.04.2020.
 */
@Slf4j
@EnableAsync
@SpringBootApplication
public class TaskPerformanceSpringApplication extends Application
{
    private ConfigurableApplicationContext context;
    private Parent root;

    @Override
    public void init() throws Exception {
        context = SpringApplication.run(TaskPerformanceSpringApplication.class);
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/static/template.fxml"));
//        fxmlLoader.setControllerFactory(context::getBean);
        fxmlLoader.setControllerFactory((clazz) ->
        {
            System.out.println(clazz);
            return context.getBean(clazz);
        });
        root = fxmlLoader.load();
    }

    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("Tasks performance analyzer");
        Scene scene = new Scene(root, 500, 500);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        context.close();
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }
}
