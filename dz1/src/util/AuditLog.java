package util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AuditLog {
    private final List<AuditEntry> entries = new ArrayList<>();
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void log(String action, String performer, String target, String details) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);

        AuditEntry entry = new AuditEntry(timestamp, action, performer, target, details);
        entries.add(entry);

        System.out.println("[AUDIT]" + entry.format());
    }

    public List<AuditEntry> getAll() {
        return new ArrayList<>(entries);
    }

    public List<AuditEntry> getByPerformer(String performer) {
        if (performer == null || performer.isBlank()) {
            return new ArrayList<>();
        }

        return entries.stream().filter(entry -> entry.performer().equalsIgnoreCase(performer)).collect(Collectors.toList());
    }

    public List<AuditEntry> getByAction(String action) {
        if (action == null || action.isBlank()) {
            return new ArrayList<>();
        }
        return entries.stream().filter(entry -> entry.action().equalsIgnoreCase(action)).collect(Collectors.toList());
    }

    public void printLog() {
        System.out.println("Журнал аудита");

        if (entries.isEmpty()) {
            System.out.println("Журнал пуст");
            return;
        }

        for (int i = 0; i < entries.size(); i++){
            System.out.println(i + 1 + " " + entries.get(i).format());
        }
        System.out.println("Всего записей: " + entries.size());
    }

    public void saveToFile(String filename) {
        try(PrintWriter writer = new PrintWriter(new FileWriter(filename, true))) {
            for (AuditEntry entry : entries){
                writer.println(entry.toFileString());
            }
            System.out.println("Лог сохранен в файл: " + filename);
        } catch (IOException e) {
            System.out.println("Ошибка при сохранении лога в файл" + e.getMessage());
        }
    }

    public void clear() {
        entries.clear();
        System.out.println("Лог очищен");
    }

    public int size() {
        return entries.size();
    }
}
