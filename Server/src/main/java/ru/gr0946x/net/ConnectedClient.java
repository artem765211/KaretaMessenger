package ru.gr0946x.net;

import ru.gr0946x.db.MessageRepository;
import ru.gr0946x.db.UserRepository;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ConnectedClient {
    private final Communicator communicator;
    private final static List<ConnectedClient> clients = new ArrayList<>();
    private final UserRepository userRepo = new UserRepository();
    private final MessageRepository messageRepo = new MessageRepository();

    private String nickname = null;
    private Long userId = null;

    // состояние авторизации
    private enum AuthState { WAITING_CHOICE, WAITING_LOGIN_NICK, WAITING_LOGIN_PASS, WAITING_REG_NICK, WAITING_REG_PASS }
    private AuthState authState = AuthState.WAITING_CHOICE;
    private String tempNickname = null;

    public ConnectedClient(Socket socket) throws IOException {
        communicator = new Communicator(socket);
        communicator.addDataListener(this::parseData);
        synchronized (clients) {
            clients.add(this);
        }
    }

    public void start() {
        communicator.start();
        sendData(MessageType.REQUEST, "Добро пожаловать в Карету!\n1 - Войти\n2 - Зарегистрироваться");
    }

    public void sendData(MessageType type, String data) {
        communicator.sendData(type + ProtocolConstants.COMMAND_SEPARATOR + data);
    }

    private void parseData(String data) {
        if (nickname == null) {
            handleAuth(data);
        } else {
            handleMessage(data);
        }
    }

    private void handleAuth(String data) {
        switch (authState) {
            case WAITING_CHOICE -> {
                if (data.equals("1")) {
                    authState = AuthState.WAITING_LOGIN_NICK;
                    sendData(MessageType.REQUEST, "Введите ник:");
                } else if (data.equals("2")) {
                    authState = AuthState.WAITING_REG_NICK;
                    sendData(MessageType.REQUEST, "Придумайте ник:");
                } else {
                    sendData(MessageType.ERROR, "Введите 1 или 2");
                }
            }
            case WAITING_LOGIN_NICK -> {
                tempNickname = data;
                authState = AuthState.WAITING_LOGIN_PASS;
                sendData(MessageType.REQUEST, "Введите пароль:");
            }
            case WAITING_LOGIN_PASS -> {
                if (userRepo.login(tempNickname, data)) {
                    finishAuth(tempNickname);
                } else {
                    authState = AuthState.WAITING_CHOICE;
                    sendData(MessageType.AUTH_FAIL, "Неверный ник или пароль");
                    sendData(MessageType.REQUEST, "1 - Войти\n2 - Зарегистрироваться");
                }
            }
            case WAITING_REG_NICK -> {
                if (!data.matches("[a-zA-Zа-яА-Я].*")) {
                    sendData(MessageType.ERROR, "Ник должен начинаться с буквы");
                    sendData(MessageType.REQUEST, "Придумайте ник:");
                    return;
                }
                if (userRepo.existsByNickname(data)) {
                    sendData(MessageType.ERROR, "Такой ник уже занят");
                    sendData(MessageType.REQUEST, "Придумайте ник:");
                    return;
                }
                tempNickname = data;
                authState = AuthState.WAITING_REG_PASS;
                sendData(MessageType.REQUEST, "Придумайте пароль:");
            }
            case WAITING_REG_PASS -> {
                if (userRepo.register(tempNickname, data)) {
                    finishAuth(tempNickname);
                } else {
                    sendData(MessageType.ERROR, "Ошибка регистрации");
                    authState = AuthState.WAITING_CHOICE;
                    sendData(MessageType.REQUEST, "1 - Войти\n2 - Зарегистрироваться");
                }
            }
        }
    }

    private void finishAuth(String nick) {
        nickname = nick;
        userId = userRepo.findIdByNickname(nick);
        sendData(MessageType.AUTH_SUCCESS, "Добро пожаловать, " + nickname + "!");
        broadcastUserList();
        sendForAll(MessageType.INFO, nickname + " вошёл в чат");
    }

    private void handleMessage(String raw) {
        // формат личного сообщения: PRIVATE|получатель|текст
        if (raw.startsWith("PRIVATE" + ProtocolConstants.COMMAND_SEPARATOR)) {
            var parts = raw.split("\\" + ProtocolConstants.COMMAND_SEPARATOR, 3);
            if (parts.length == 3) {
                sendPrivate(parts[1], parts[2]);
            }
            return;
        }
        // обычное сообщение в общий чат
        messageRepo.save(userId, null, raw);
        sendForAll(MessageType.MESSAGE, nickname + ProtocolConstants.AUTHOR_SEPARATOR + raw);
    }

    private void sendPrivate(String toNickname, String text) {
        ConnectedClient target = findByNickname(toNickname);
        if (target == null) {
            sendData(MessageType.ERROR, "Пользователь " + toNickname + " не в сети");
            return;
        }
        Long receiverId = userRepo.findIdByNickname(toNickname);
        messageRepo.save(userId, receiverId, text);

        String msg = nickname + ProtocolConstants.AUTHOR_SEPARATOR + text;
        target.sendData(MessageType.PRIVATE, msg);
        sendData(MessageType.PRIVATE, msg);
    }

    private void sendForAll(MessageType type, String data) {
        synchronized (clients) {
            clients.stream()
                    .filter(c -> c.nickname != null)
                    .forEach(c -> c.sendData(type, data));
        }
    }

    private ConnectedClient findByNickname(String nick) {
        synchronized (clients) {
            return clients.stream()
                    .filter(c -> c.nickname != null && c.nickname.equalsIgnoreCase(nick))
                    .findFirst().orElse(null);
        }
    }

    private void broadcastUserList() {
        synchronized (clients) {
            var onlineNames = clients.stream()
                    .filter(c -> c.nickname != null)
                    .map(c -> c.nickname)
                    .reduce("", (a, b) -> a.isEmpty() ? b : a + ProtocolConstants.FIELD_SEPARATOR + b);
            clients.stream()
                    .filter(c -> c.nickname != null)
                    .forEach(c -> c.sendData(MessageType.USER_LIST, onlineNames));
        }
    }

    public void stop() {
        if (nickname != null) {
            sendForAll(MessageType.INFO, nickname + " покинул чат");
        }
        synchronized (clients) {
            clients.remove(this);
        }
        broadcastUserList();
        communicator.stop();
    }
}