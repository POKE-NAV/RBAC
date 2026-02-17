package role;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import abstracts.AbstractRoleAssignment;
import interfaces.RoleAssignment;
import record.AssignmentMetadata;
import record.Permission;
import record.User;

public class TemporaryAssignment extends AbstractRoleAssignment {

    private String expiresAt;
    private boolean autoRenew;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public TemporaryAssignment(User user, Role role, AssignmentMetadata metadata,
                               String expiresAt, boolean autoRenew) {
        super(user, role, metadata);

        if (expiresAt == null || expiresAt.isBlank()) {
            throw new IllegalArgumentException("expiresAt не может быть null или пустым");
        }

        this.expiresAt = expiresAt;
        this.autoRenew = autoRenew;
    }

    @Override
    public boolean isActive() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiry = LocalDateTime.parse(expiresAt, FORMATTER);
            return now.isBefore(expiry);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    @Override
    public String assignmentType() {
        return RoleAssignment.TYPE_TEMPORARY;
    }

    public boolean isExpired() {
        return !isActive();
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public void extend(String newExpirationDate) {
        if (newExpirationDate == null || newExpirationDate.isBlank()) {
            throw new IllegalArgumentException("Новая дата не может быть null или пустой");
        }

        try {
            LocalDateTime.parse(newExpirationDate, FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Неверный формат даты. Используйте: yyyy-MM-dd HH:mm");
        }

        this.expiresAt = newExpirationDate;
    }

    public String getTimeRemaining() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiry = LocalDateTime.parse(expiresAt, FORMATTER);

            if (now.isAfter(expiry)) {
                return "Expired";
            }

            long days = ChronoUnit.DAYS.between(now, expiry);
            long hours = ChronoUnit.HOURS.between(now, expiry) % 24;
            long minutes = ChronoUnit.MINUTES.between(now, expiry) % 60;

            if (days > 0) {
                return String.format("%d days %d hours %d minutes remaining", days, hours, minutes);
            } else if (hours > 0) {
                return String.format("%d hours %d minutes remaining", hours, minutes);
            } else {
                return String.format("%d minutes remaining", minutes);
            }
        } catch (DateTimeParseException e) {
            return "Invalid date format";
        }
    }

    @Override
    public String summary() {
        String baseSummary = super.summary();
        String autoRenewText = autoRenew ? "Yes" : "No";
        String status = isActive() ? "ACTIVE" : "EXPIRED";

        return String.format("%s\nExpires: %s\nAuto-renew: %s\nTime remaining: %s\nStatus: %s",
                baseSummary.split("\n")[0],
                expiresAt,
                autoRenewText,
                getTimeRemaining(),
                status
        );
    }

    public static void main(String[] args) {
        System.out.println("Тестирование TemporaryAssignment\n");

        // Шаг 1: Создаем необходимые объекты
        System.out.println("Шаг 1: Создание пользователей, прав, ролей и метаданных\n");

        // Создаем пользователей
        User admin = User.validate("admin", "Admin User", "admin@example.com");
        User john = User.validate("alexander", "alexander ignatov", "alexander@example.com");
        User jane = User.validate("evgen", "evgen grishko", "evgen@example.com");

        // Создаем права
        Permission readUsers = new Permission("READ", "users", "Can view users");
        Permission writeUsers = new Permission("WRITE", "users", "Can edit users");

        // Создаем роли
        Role adminRole = new Role("Administrator", "Full system access");
        adminRole.addPermissions(readUsers);
        adminRole.addPermissions(writeUsers);

        Role moderatorRole = new Role("Moderator", "Can manage users");
        moderatorRole.addPermissions(readUsers);

        // Создаем метаданные
        AssignmentMetadata meta1 = AssignmentMetadata.now(admin.username(), "Временный доступ");
        AssignmentMetadata meta2 = AssignmentMetadata.now(admin.username(), "Пробный период");
        AssignmentMetadata meta3 = AssignmentMetadata.now(admin.username(), "Пробный период");

        System.out.println("Пользователи:");
        System.out.println("  " + admin.format());
        System.out.println("  " + john.format());
        System.out.println("  " + jane.format());
        System.out.println();

        System.out.println("Роли:");
        System.out.println("  " + adminRole.getName());
        System.out.println("  " + moderatorRole.getName());
        System.out.println();

        // Шаг 2: Создаем временные назначения с разными датами
        System.out.println("Шаг 2: Создание временных назначений\n");

        // Текущая дата + 30 дней
        LocalDateTime futureDate = LocalDateTime.now().plusDays(30);
        String futureDateStr = futureDate.format(FORMATTER);

        // Дата в прошлом (для теста истечения)
        LocalDateTime pastDate = LocalDateTime.now().minusDays(5);
        String pastDateStr = pastDate.format(FORMATTER);

        // Дата через 2 часа (для теста оставшегося времени)
        LocalDateTime soonDate = LocalDateTime.now().plusHours(2).plusMinutes(30);
        String soonDateStr = soonDate.format(FORMATTER);

        TemporaryAssignment assign1 = new TemporaryAssignment(
                john, moderatorRole, meta1, futureDateStr, true
        );

        TemporaryAssignment assign2 = new TemporaryAssignment(
                jane, adminRole, meta2, pastDateStr, false
        );

        TemporaryAssignment assign3 = new TemporaryAssignment(
                john, adminRole, meta3, soonDateStr, true
        );

        System.out.println("Созданы назначения:");
        System.out.println("  Назначение 1 (действует 30 дней, авто-продление: да): " + assign1.assignmentId());
        System.out.println("  Назначение 2 (истекло 5 дней назад, авто-продление: нет): " + assign2.assignmentId());
        System.out.println("  Назначение 3 (истекает через 2.5 часа, авто-продление: да): " + assign3.assignmentId());
        System.out.println();

        // Шаг 3: Тестируем базовые методы
        System.out.println("Шаг 3: Тестирование базовых методов\n");

        System.out.println("Назначение 1:");
        System.out.println("  Type: " + assign1.assignmentType());
        System.out.println("  Expires: " + assign1.getExpiresAt());
        System.out.println("  Auto-renew: " + assign1.isAutoRenew());
        System.out.println("  Active: " + assign1.isActive());
        System.out.println("  Expired: " + assign1.isExpired());
        System.out.println("  Time remaining: " + assign1.getTimeRemaining());
        System.out.println("  Format: " + assign1.format());
        System.out.println();

        System.out.println("Назначение 2:");
        System.out.println("  Type: " + assign2.assignmentType());
        System.out.println("  Expires: " + assign2.getExpiresAt());
        System.out.println("  Auto-renew: " + assign2.isAutoRenew());
        System.out.println("  Active: " + assign2.isActive());
        System.out.println("  Expired: " + assign2.isExpired());
        System.out.println("  Time remaining: " + assign2.getTimeRemaining());
        System.out.println("  Format: " + assign2.format());
        System.out.println();

        System.out.println("Назначение 3 (до продления):");
        System.out.println("  Expires: " + assign3.getExpiresAt());
        System.out.println("  Time remaining: " + assign3.getTimeRemaining());
        System.out.println();

        // Шаг 4: Тестируем метод extend()
        System.out.println("Шаг 4: Тестирование метода extend()\n");

        System.out.println("Продлеваем назначение 3...");
        // Продлеваем на 15 дней
        LocalDateTime extendedDate = LocalDateTime.now().plusDays(15);
        String extendedDateStr = extendedDate.format(FORMATTER);

        System.out.println("  Новая дата: " + extendedDateStr);
        assign3.extend(extendedDateStr);

        System.out.println("Назначение 3 (после продления):");
        System.out.println("  Expires: " + assign3.getExpiresAt());
        System.out.println("  Time remaining: " + assign3.getTimeRemaining());
        System.out.println();

        // Шаг 5: Тестируем метод summary()
        System.out.println("Шаг 5: Тестирование метода summary()\n");

        System.out.println("=== Сводка по назначению 1 (активное) ===");
        System.out.println(assign1.summary());
        System.out.println();

        System.out.println("=== Сводка по назначению 2 (истекшее) ===");
        System.out.println(assign2.summary());
        System.out.println();

        System.out.println("=== Сводка по назначению 3 (продленное) ===");
        System.out.println(assign3.summary());
        System.out.println();

        // Шаг 6: Демонстрация полиморфизма с PermanentAssignment
        System.out.println("Шаг 6: Демонстрация полиморфизма\n");

        // Создаем постоянное назначение для сравнения
        PermanentAssignment permAssign = new PermanentAssignment(
                jane, moderatorRole,
                AssignmentMetadata.now(admin.username(), "Постоянное назначение")
        );

        // Работаем через интерфейс RoleAssignment
        RoleAssignment[] assignments = {assign1, assign2, assign3, permAssign};

        System.out.println("Все назначения (через интерфейс):");
        for (RoleAssignment ra : assignments) {
            System.out.println("  " + ra.format());
        }
        System.out.println();

        System.out.println("Только активные назначения:");
        for (RoleAssignment ra : assignments) {
            if (ra.isActive()) {
                System.out.println("  [АКТИВНО] " + ra.format());
            }
        }
        System.out.println();

        System.out.println("Только TEMPORARY назначения:");
        for (RoleAssignment ra : assignments) {
            if (ra.isTemporary()) {
                System.out.println("  [ВРЕМЕННОЕ] " + ra.format() + " (expires: " +
                        ((TemporaryAssignment)ra).getExpiresAt() + ")");
            }
        }
        System.out.println();

        // Шаг 7: Работа со специфичными методами TemporaryAssignment
        System.out.println("Шаг 7: Работа со специфичными методами TemporaryAssignment\n");

        for (RoleAssignment ra : assignments) {
            if (ra instanceof TemporaryAssignment) {
                TemporaryAssignment ta = (TemporaryAssignment) ra;
                System.out.println("Временное назначение " + ta.assignmentId());
                System.out.println("  Expires: " + ta.getExpiresAt());
                System.out.println("  Auto-renew: " + ta.isAutoRenew());
                System.out.println("  Time remaining: " + ta.getTimeRemaining());
                System.out.println();
            }
        }

        // Шаг 8: Тестирование валидации
        System.out.println("Шаг 8: Тестирование валидации\n");

        try {
            TemporaryAssignment invalid = new TemporaryAssignment(
                    john, moderatorRole, meta1, null, true
            );
            System.out.println("ОШИБКА: Должно было выбросить исключение (null дата)");
        } catch (IllegalArgumentException e) {
            System.out.println("Успешно перехвачено (null дата): " + e.getMessage());
        }

        try {
            TemporaryAssignment invalid = new TemporaryAssignment(
                    john, moderatorRole, meta1, "invalid-date", true
            );
            System.out.println("ОШИБКА: Должно было выбросить исключение (неверный формат)");
        } catch (IllegalArgumentException e) {
            System.out.println("Успешно перехвачено (неверный формат даты): " + e.getMessage());
        }

        try {
            assign1.extend("not-a-date");
            System.out.println("ОШИБКА: Должно было выбросить исключение (неверный формат при продлении)");
        } catch (IllegalArgumentException e) {
            System.out.println("Успешно перехвачено (неверный формат при продлении): " + e.getMessage());
        }
        System.out.println();

        // Шаг 9: Демонстрация автоматического обновления статуса
        System.out.println("Шаг 9: Демонстрация автоматического обновления статуса\n");

        // Создаем назначение, которое истекает через 10 секунд
        LocalDateTime shortExpiry = LocalDateTime.now().plusSeconds(10);
        String shortExpiryStr = shortExpiry.format(FORMATTER);

        TemporaryAssignment shortTerm = new TemporaryAssignment(
                john, moderatorRole,
                AssignmentMetadata.now(admin.username(), "Очень короткое назначение"),
                shortExpiryStr, false
        );

        System.out.println("Назначение создано. Истекает через 10 секунд.");
        System.out.println("Текущее состояние: Active=" + shortTerm.isActive() +
                ", Time remaining: " + shortTerm.getTimeRemaining());

        System.out.println("Ожидаем 11 секунд...");
        try {
            Thread.sleep(11000); // Ждем 11 секунд
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Состояние после ожидания: Active=" + shortTerm.isActive() +
                ", Time remaining: " + shortTerm.getTimeRemaining());
        System.out.println();

        // Шаг 10: Сравнение с PermanentAssignment
        System.out.println("Шаг 10: Сравнение с PermanentAssignment\n");

        System.out.println("PermanentAssignment vs TemporaryAssignment:");
        System.out.println("  Permanent - всегда активно (если не отменено)");
        System.out.println("  Temporary - зависит от даты истечения");
        System.out.println();

        System.out.println("Пример сравнения:");
        System.out.println("  Permanent: " + permAssign.format() + " - " +
                (permAssign.isActive() ? "ACTIVE" : "INACTIVE"));
        System.out.println("  Temporary (истекшее): " + assign2.format() + " - " +
                (assign2.isActive() ? "ACTIVE" : "EXPIRED"));
        System.out.println("  Temporary (продленное): " + assign3.format() + " - " +
                (assign3.isActive() ? "ACTIVE" : "EXPIRED"));
    }
}