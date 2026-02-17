package role;

import abstracts.AbstractRoleAssignment;
import interfaces.RoleAssignment;
import record.AssignmentMetadata;
import record.Permission;
import record.User;

import java.security.Permissions;

public class PermanentAssignment extends AbstractRoleAssignment {
    private boolean revoked;

    public PermanentAssignment(User user, Role role, AssignmentMetadata metadata) {
        super(user, role, metadata);
        this.revoked = false;
    }

    public void revoke() {
        this.revoked = true;
    }

    public boolean isRevoked() {
        return revoked;
    }

    @Override
    public String assignmentType() {
        return RoleAssignment.TYPE_PERMANENT;
    }

    @Override
    public boolean isActive() {
        return !revoked;
    }

    @Override
    public String summary() {
        String baseFormat = super.summary();
        if (revoked) {
            return baseFormat + " [REVOKED]";
        }
        return baseFormat;
    }

    public static void main(String[] args) {
        System.out.println("Тестирование PermanentAssignment \n");

        // Шаг 1: Создаем необходимые объекты
        System.out.println("Шаг 1: Создание пользователей, прав, ролей и метаданных\n");

        // Создаем пользователей
        User admin = User.validate("admin", "Admin User", "admin@example.com");
        User john = User.validate("Alexander", "Alexander Ignatov", "alexander@example.com");
        User jane = User.validate("Evgeni", "Evgeni Grishko", "evgeni@example.com");

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
        AssignmentMetadata meta1 = AssignmentMetadata.now(admin.username(), "Назначение администратором");
        AssignmentMetadata meta2 = AssignmentMetadata.now(admin.username(), "Назначение для проекта");
//        AssignmentMetadata meta3 = AssignmentMetadata.now(admin.username());

        System.out.println("Пользователи:");
        System.out.println("  " + admin.format());
        System.out.println("  " + john.format());
        System.out.println("  " + jane.format());
        System.out.println();

        System.out.println("Роли:");
        System.out.println("  " + adminRole.getName());
        System.out.println("  " + moderatorRole.getName());
        System.out.println();

        // Шаг 2: Создаем постоянные назначения
        System.out.println("Шаг 2: Создание постоянных назначений\n");

        PermanentAssignment assign1 = new PermanentAssignment(
                john, moderatorRole, meta1
        );

        System.out.println("Созданы назначения:");
        System.out.println("  Назначение 1 (Moderator): " + assign1.assignmentId());
        System.out.println();

        // Шаг 3: Тестируем базовые методы
        System.out.println("Шаг 3: Тестирование базовых методов\n");

        System.out.println("Назначение 1:");
        System.out.println("  Type: " + assign1.assignmentType());
        System.out.println("  Active: " + assign1.isActive());
        System.out.println("  Revoked: " + assign1.isRevoked());
        System.out.println("  Format: " + assign1.summary());
        System.out.println();


        // Шаг 4: Тестируем метод summary() из родительского класса
        System.out.println("Шаг 4: Тестирование метода summary()\n");

        System.out.println("Сводка по назначению 1 (активное)");
        System.out.println(assign1.summary());
        System.out.println();

        // Шаг 5: Демонстрация полиморфизма
        System.out.println("Шаг 5: Демонстрация полиморфизма\n");

        // Работаем через интерфейс RoleAssignment
        RoleAssignment[] assignments = {assign1};

        System.out.println("Все назначения (через интерфейс):");
        for (RoleAssignment ra : assignments) {
            System.out.println("  " + ra.format());
        }
        System.out.println();

        System.out.println("Только активные назначения:");
        for (RoleAssignment ra : assignments) {
            if (ra.isActive()) {
                System.out.println(ra.format());
            }
        }
        System.out.println();

        System.out.println("Только PERMANENT назначения:");
        for (RoleAssignment ra : assignments) {
            if (ra.isPermanent()) {  // default метод из интерфейса
                System.out.println(ra.format());
            }
        }
        System.out.println();

        // Шаг 6: Работа со специфичными методами PermanentAssignment
        System.out.println("Шаг 6: Работа со специфичными методами PermanentAssignment\n");

        for (RoleAssignment ra : assignments) {
            if (ra instanceof PermanentAssignment) {
                PermanentAssignment pa = (PermanentAssignment) ra;
                System.out.println("Назначение " + pa.assignmentId() +
                        ": revoked=" + pa.isRevoked() +
                        ", active=" + pa.isActive());
            }
        }
        System.out.println();

        // Шаг 7: Тестирование equals и hashCode (из родительского класса)
        System.out.println("Шаг 7: Тестирование equals и hashCode\n");

        PermanentAssignment assign4 = new PermanentAssignment(
                john, moderatorRole, meta1  // Те же данные, но другой ID
        );

        System.out.println("assign1 ID: " + assign1.assignmentId());
        System.out.println("assign4 ID: " + assign4.assignmentId());
        System.out.println("assign1.equals(assign4): " + assign1.equals(assign4));
        System.out.println("assign1.equals(assign1): " + assign1.equals(assign1));
        System.out.println("assign1.hashCode(): " + assign1.hashCode());
        System.out.println("assign4.hashCode(): " + assign4.hashCode());
        System.out.println();

        // Шаг 8: Проверка валидации (через родительский конструктор)
        System.out.println("Шаг 8: Проверка валидации\n");

        try {
            PermanentAssignment invalid = new PermanentAssignment(
                    null, moderatorRole, meta1
            );
            System.out.println("Должно было выбросить исключение");
        } catch (IllegalArgumentException e) {
            System.out.println("Успешно перехвачено: " + e.getMessage());
        }

        try {
            PermanentAssignment invalid = new PermanentAssignment(
                    john, null, meta1
            );
            System.out.println("Должно было выбросить исключение");
        } catch (IllegalArgumentException e) {
            System.out.println("Успешно перехвачено: " + e.getMessage());
        }

        // Шаг 10: Демонстрация жизненного цикла
        System.out.println("\nШаг 10: Демонстрация жизненного цикла назначения\n");

        PermanentAssignment lifeCycleDemo = new PermanentAssignment(
                jane, moderatorRole,
                AssignmentMetadata.now(admin.username(), "Пробное назначение")
        );

        System.out.println("Начальное состояние:");
        System.out.println("  " + lifeCycleDemo.format());
        System.out.println("  Active: " + lifeCycleDemo.isActive());
        System.out.println("  Revoked: " + lifeCycleDemo.isRevoked());
        System.out.println();

        System.out.println("Через некоторое время решили отменить назначение...");
        lifeCycleDemo.revoke();

        System.out.println("Состояние после отмены:");
        System.out.println("  " + lifeCycleDemo.format());
        System.out.println("  Active: " + lifeCycleDemo.isActive());
        System.out.println("  Revoked: " + lifeCycleDemo.isRevoked());
    }

}
