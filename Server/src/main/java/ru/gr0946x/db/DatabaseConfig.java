package ru.gr0946x.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class DatabaseConfig {

    private static JdbcTemplate jdbcTemplate;

    public static JdbcTemplate getJdbcTemplate() {
        if (jdbcTemplate == null) {
            init();
        }
        return jdbcTemplate;
    }

    private static void init() {
        try {
            Properties props = new Properties();
            InputStream is = DatabaseConfig.class
                    .getClassLoader()
                    .getResourceAsStream("db.properties");
            props.load(is);

            var dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName(props.getProperty("db.driver"));
            dataSource.setUrl(props.getProperty("db.url"));
            dataSource.setUsername(props.getProperty("db.username"));
            dataSource.setPassword(props.getProperty("db.password"));

            jdbcTemplate = new JdbcTemplate(dataSource);

            runSchema();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка загрузки настроек БД", e);
        }
    }

    private static void runSchema() {
        try {
            InputStream is = DatabaseConfig.class
                    .getClassLoader()
                    .getResourceAsStream("schema.sql");
            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            for (String statement : sql.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    jdbcTemplate.execute(trimmed);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка создания таблиц", e);
        }
    }
}