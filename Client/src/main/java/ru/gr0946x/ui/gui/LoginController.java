package ru.gr0946x.ui;

import ru.gr0946x.ui.gui.ChatController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ru.gr0946x.net.Client;
import ru.gr0946x.net.MessageType;

import java.io.IOException;

public class LoginController {

    @FXML private TextField nickField;
    @FXML private PasswordField passField;
    @FXML private Label errorLabel;

    private Client client;
    private String pendingNick;

    @FXML
    public void initialize() {
        try {
            client = new Client("localhost", 9460);
            client.addDataListener(this::onServerMessage);
            client.start();
        } catch (IOException e) {
            Platform.runLater(() -> errorLabel.setText("Нет связи с сервером"));
        }
    }

    private void onServerMessage(String data, MessageType type) {
        Platform.runLater(() -> {
            switch (type) {
                case AUTH_SUCCESS -> openChatWindow();
                case AUTH_FAIL, ERROR -> errorLabel.setText(data);
                default -> {}
            }
        });
    }

    @FXML
    private void onLogin() {
        String nick = nickField.getText().trim();
        String pass = passField.getText();
        if (nick.isEmpty() || pass.isEmpty()) {
            errorLabel.setText("Заполните все поля");
            return;
        }
        pendingNick = nick;
        errorLabel.setText("");
        client.sendData(nick);
        client.sendData(pass);
    }

    @FXML
    private void onRegister() {
        onLogin();
    }

    private void openChatWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ru/gr0946x/ui/gui/chat.fxml")
            );
            Scene scene = new Scene(loader.load(), 800, 600);
            ChatController controller = loader.getController();
            controller.setClient(client, pendingNick);

            Stage stage = (Stage) nickField.getScene().getWindow();
            stage.setTitle("Карета");
            stage.setScene(scene);
        } catch (IOException e) {
            errorLabel.setText("Ошибка открытия чата");
        }
    }
}