package ru.gr0946x.net;

public enum MessageType {
    MESSAGE,        // обычное сообщение в общий чат
    PRIVATE,        // личное сообщение
    INFO,           // информация (вошёл/вышел)
    REQUEST,        // сервер просит что-то ввести
    ERROR,          // ошибка
    AUTH_SUCCESS,   // авторизация успешна
    AUTH_FAIL,      // авторизация неуспешна
    REGISTER,       // запрос регистрации
    LOGIN,          // запрос входа
    USER_LIST,      // список онлайн пользователей
    HISTORY,        // история сообщений
    SEARCH_RESULT   // результат поиска
}