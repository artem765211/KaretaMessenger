package ru.gr0946x.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class KaretaApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/ru/gr0946x/ui/gui/login.fxml")
        );
        Scene scene = new Scene(loader.load(), 400, 300);
        stage.setTitle("Карета — Вход");
        stage.setScene(scene);
        stage.show();
    }
}