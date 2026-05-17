package ru.gr0946x.db;

import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class MessageRepository {

    private final JdbcTemplate jdbc;

    public MessageRepository() {
        this.jdbc = DatabaseConfig.getJdbcTemplate();
    }

    // Сохранить сообщение
    public void save(Long senderId, Long receiverId, String text) {
        jdbc.update(
                "INSERT INTO messages (sender_id, receiver_id, text, sent_at, is_read) VALUES (?, ?, ?, ?, ?)",
                senderId, receiverId, text, Timestamp.valueOf(LocalDateTime.now()), false
        );
    }

    // Последние N сообщений между двумя пользователями
    public List<Map<String, Object>> getHistory(Long userId1, Long userId2, int limit) {
        String sql = """
            SELECT m.text, m.sent_at, m.is_read,
                   u.nickname AS sender_name
            FROM messages m
            JOIN users u ON u.id = m.sender_id
            WHERE (m.sender_id = ? AND m.receiver_id = ?)
               OR (m.sender_id = ? AND m.receiver_id = ?)
            ORDER BY m.sent_at DESC
            LIMIT ?
        """;
        return jdbc.queryForList(sql, userId1, userId2, userId2, userId1, limit);
    }

    // Поиск сообщений по части слова между двумя пользователями
    public List<Map<String, Object>> search(Long userId1, Long userId2, String keyword) {
        String sql = """
            SELECT m.text, m.sent_at,
                   u.nickname AS sender_name
            FROM messages m
            JOIN users u ON u.id = m.sender_id
            WHERE ((m.sender_id = ? AND m.receiver_id = ?)
               OR  (m.sender_id = ? AND m.receiver_id = ?))
              AND LOWER(m.text) LIKE LOWER(?)
            ORDER BY m.sent_at ASC
        """;
        return jdbc.queryForList(sql, userId1, userId2, userId2, userId1, "%" + keyword + "%");
    }

    // Последние N сообщений общего чата
    public List<Map<String, Object>> getPublicHistory(int limit) {
        String sql = """
            SELECT m.text, m.sent_at,
                   u.nickname AS sender_name
            FROM messages m
            JOIN users u ON u.id = m.sender_id
            WHERE m.receiver_id IS NULL
            ORDER BY m.sent_at DESC
            LIMIT ?
        """;
        return jdbc.queryForList(sql, limit);
    }
}