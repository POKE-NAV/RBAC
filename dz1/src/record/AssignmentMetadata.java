package record;
import role.Role;

import java.text.Format;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record AssignmentMetadata(String assignedBy, String assignedAt, String reason) {
    //Компактный конструктор
    public AssignmentMetadata {
        if (assignedBy == null || assignedAt == null) {
            throw new IllegalArgumentException("Поля assignedBy, assignedAt конструктора AssignmentMetadata не могут быть равны null");
        }

        if (assignedBy.isBlank() || assignedAt.isBlank()) {
            throw new IllegalArgumentException("Поля assignedBy, assignedAt конструктора AssignmentMetadata не могут быть пустыми");
        }

        if (reason == null) {
            reason = "";
        }
    }

    public static AssignmentMetadata now(String assignedBy, String reason) {
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        return new AssignmentMetadata(assignedBy, currentDate, reason);
    }

    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Assigned by: %s\n", assignedBy));
        sb.append(String.format("Assigned at: %s\n", assignedAt));

        if (!reason.isEmpty()) {
            sb.append(String.format("Reason: %s\n", reason));
        }

        return sb.toString();
    }

    static void main(String[] args) {

        System.out.println("Тестирование AssignmentMetadata\n");

        // ТЕСТ 1: Создание через конструктор (все поля)
        System.out.println("ТЕСТ 1: Создание через конструктор");
        AssignmentMetadata meta1 = new AssignmentMetadata(
                "admin",
                "2024-01-15 10:30:00",
                "Назначение прав администратора"
        );
        System.out.println(meta1.format());
        System.out.println();

        // ТЕСТ 2: Создание через now() с причиной
        System.out.println("ТЕСТ 2: Создание через now() с причиной");
        AssignmentMetadata meta2 = AssignmentMetadata.now(
                "john_doe",
                "Требуется доступ к отчетам"
        );
        System.out.println(meta2.format());
        System.out.println();

        // ТЕСТ 3: Создание через now() без причины
        System.out.println("ТЕСТ 3: Создание через now() без причины");
        AssignmentMetadata meta3 = AssignmentMetadata.now("alexander", "");
        System.out.println(meta3.format());
        System.out.println();

        // ТЕСТ 4: Проверка валидации (пустой assignedBy)
        System.out.println("ТЕСТ 4: Проверка валидации (пустой assignedBy)");
        try {
            AssignmentMetadata invalid = new AssignmentMetadata("", "2024-01-15 10:30:00", "test");
            System.out.println("Должно было выбросить исключение");
        } catch (IllegalArgumentException e) {
            System.out.println("Успешно перехвачено: " + e.getMessage());
        }
        System.out.println();

        // ТЕСТ 5: Проверка валидации (null assignedAt)
        System.out.println("ТЕСТ 5: Проверка валидации (null assignedAt)");
        try {
            AssignmentMetadata invalid = new AssignmentMetadata("admin", null, "test");
            System.out.println("Должно было выбросить исключение");
        } catch (IllegalArgumentException e) {
            System.out.println("Успешно перехвачено: " + e.getMessage());
        }
        System.out.println();

        // ТЕСТ 6: Проверка обработки null reason
        System.out.println("ТЕСТ 6: Проверка обработки null reason");
        AssignmentMetadata meta4 = new AssignmentMetadata("admin", "2024-01-15 10:30:00", null);
        System.out.println("Создано с null reason:");
        System.out.println(meta4.format()); // reason не выводится, так как пустая строка
        System.out.println();

        // ТЕСТ 7: Демонстрация неизменяемости record
        System.out.println("ТЕСТ 7: Демонстрация неизменяемости record");
        AssignmentMetadata original = AssignmentMetadata.now("admin", "Первоначальная причина");
        System.out.println("Оригинал:");
        System.out.println(original.format());

        // Создаем новый объект с изменениями (так как record неизменяемый)
        AssignmentMetadata modified = new AssignmentMetadata(
                "new_admin",
                original.assignedAt(),
                "Измененная причина"
        );
        System.out.println("Новый объект с изменениями:");
        System.out.println(modified.format());
        System.out.println();

        // ТЕСТ 8: Пример использования с другими классами
        System.out.println("ТЕСТ 8: Пример использования с Role и Permission");

        // Создаем Permission
        Permission readUsers = new Permission("READ", "users", "Can view users");

        // Создаем Role (из предыдущего задания)
        Role moderator = new Role("Moderator", "User moderator");
        moderator.addPermissions(readUsers);

        // Создаем метаданные назначения
        AssignmentMetadata assignment = AssignmentMetadata.now(
                "chief_admin",
                "Назначен модератором"
        );

        System.out.println("Информация о назначении роли:");
        System.out.println("Роль: " + moderator.getName() + " [ID: " + moderator.getId() + "]");
        System.out.println("Права роли: " + moderator.getPermissions().size());
        for (Permission p : moderator.getPermissions()) {
            System.out.println("  - " + p.format());
        }
        System.out.println("\nМетаданные назначения:");
        System.out.println(assignment.format());

    }
}
