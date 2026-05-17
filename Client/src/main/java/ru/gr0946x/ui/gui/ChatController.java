package ru.gr0946x.ui.gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ru.gr0946x.net.Client;
import ru.gr0946x.net.MessageType;
import ru.gr0946x.net.ProtocolConstants;

public class ChatController {

    @FXML private TextArea chatArea;
    @FXML private TextField messageField;
    @FXML private TextField searchField;
    @FXML private ListView<String> userList;
    @FXML private Label chatWithLabel;
    @FXML private Label allLabel;

    private Client client;
    private String myNickname;
    private String selectedUser = null; // null = общий чат

    public void setClient(Client client, String nickname) {
        this.client = client;
        this.myNickname = nickname;
        client.addDataListener(this::onServerMessage);

        // клик на "Все" — переключаемся в общий чат
        allLabel.setOnMouseClicked(e -> {
            selectedUser = null;
            chatWithLabel.setText("Общий чат");
            chatArea.clear();
        });

        // клик на пользователя в списке
        userList.setOnMouseClicked(e -> {
            String selected = userList.getSelectionModel().getSelectedItem();
            if (selected != null && !selected.equals(myNickname)) {
                selectedUser = selected;
                chatWithLabel.setText("Чат с " + selected);
                chatArea.clear();
                // запросить историю
                client.sendData("HISTORY" + ProtocolConstants.COMMAND_SEPARATOR + selected);
            }
        });
    }

    private void onServerMessage(String data, MessageType type) {
        Platform.runLater(() -> {
            switch (type) {
                case MESSAGE -> {
                    if (selectedUser == null) {
                        var parts = data.split("\\" + ProtocolConstants.AUTHOR_SEPARATOR, 2);
                        if (parts.length == 2)
                            appendMessage(parts[0], parts[1]);
                    }
                }
                case PRIVATE -> {
                    var parts = data.split("\\" + ProtocolConstants.AUTHOR_SEPARATOR, 2);
                    if (parts.length == 2) {
                        String sender = parts[0];
                        String text = parts[1];
                        if (selectedUser != null &&
                                (sender.equalsIgnoreCase(selectedUser) || sender.equalsIgnoreCase(myNickname))) {
                            appendMessage(sender, text);
                        }
                    }
                }
                case USER_LIST -> {
                    userList.getItems().clear();
                    if (!data.isBlank()) {
                        for (String u : data.split(ProtocolConstants.FIELD_SEPARATOR)) {
                            if (!u.equals(myNickname))
                                userList.getItems().add(u);
                        }
                    }
                }
                case HISTORY, SEARCH_RESULT -> {
                    chatArea.clear();
                    if (!data.isBlank()) {
                        for (String line : data.split(ProtocolConstants.FIELD_SEPARATOR)) {
                            chatArea.appendText(line + "\n");
                        }
                    }
                }
                case INFO -> chatArea.appendText("» " + data + "\n");
                case ERROR -> chatArea.appendText("⚠ " + data + "\n");
                default -> {}
            }
        });
    }

    private void appendMessage(String sender, String text) {
        chatArea.appendText(sender + ": " + text + "\n");
    }

    @FXML
    private void onSend() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;

        if (selectedUser == null) {
            client.sendData(text);
        } else {
            client.sendData("PRIVATE" + ProtocolConstants.COMMAND_SEPARATOR
                    + selectedUser + ProtocolConstants.COMMAND_SEPARATOR + text);
        }
        messageField.clear();
    }

    @FXML
    private void onSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty() || selectedUser == null) {
            chatArea.appendText("⚠ Выберите собеседника и введите слово для поиска\n");
            return;
        }
        client.sendData("SEARCH" + ProtocolConstants.COMMAND_SEPARATOR
                + selectedUser + ProtocolConstants.COMMAND_SEPARATOR + keyword);
    }
}