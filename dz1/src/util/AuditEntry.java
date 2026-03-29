package util;

public record AuditEntry(
        String timestamp,   // время события
        String action,      // действие
        String performer,    // кто выполнил действие
        String target,      // объект действия (имя пользователя, имя роли)
        String details      // дополнительные детали
) {
    public String format() {
        return String.format("[%s] %s | %s -> %s | %s",
                timestamp, action, performer, target, details);
    }

    public String toFileString() {
        return String.join("|",
                timestamp,
                action,
                performer,
                target,
                details.replace("\n", " ").replace("|", ";"));
    }

    public static AuditEntry fromFileString(String line) {
        String[] parts = line.split("\\|", 5);
        if (parts.length >= 5) {
            return new AuditEntry(
                    parts[0], parts[1], parts[2], parts[3], parts[4]
            );
        }
        throw new IllegalArgumentException("Неверный формат строки аудита: " + line);
    }
}
