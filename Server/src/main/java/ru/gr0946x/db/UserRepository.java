package ru.gr0946x.db;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.jdbc.core.JdbcTemplate;

public class UserRepository {

    private final JdbcTemplate jdbc;

    public UserRepository() {
        this.jdbc = DatabaseConfig.getJdbcTemplate();
    }

    // Регистрация нового пользователя
    public boolean register(String nickname, String password) {
        if (existsByNickname(nickname)) return false;
        String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
        jdbc.update(
                "INSERT INTO users (nickname, password) VALUES (?, ?)",
                nickname, hashed
        );
        return true;
    }

    // Проверка логина и пароля
    public boolean login(String nickname, String password) {
        String sql = "SELECT password FROM users WHERE LOWER(nickname) = LOWER(?)";
        var rows = jdbc.queryForList(sql, nickname);
        if (rows.isEmpty()) return false;
        String hashed = (String) rows.get(0).get("PASSWORD");
        return BCrypt.checkpw(password, hashed);
    }

    // Получить id пользователя по нику
    public Long findIdByNickname(String nickname) {
        String sql = "SELECT id FROM users WHERE LOWER(nickname) = LOWER(?)";
        var rows = jdbc.queryForList(sql, nickname);
        if (rows.isEmpty()) return null;
        return (Long) rows.get(0).get("ID");
    }

    // Проверить существует ли пользователь
    public boolean existsByNickname(String nickname) {
        return findIdByNickname(nickname) != null;
    }
}