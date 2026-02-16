package record;

import java.util.Locale;

public record Permission(String name, String resource, String description) {

    //канонический конструктор
    public Permission(String name, String resource, String description) {
        if (name == null || resource == null || description == null) {
            throw new IllegalArgumentException("Поля Permission не могут быть равны null");
        }

        if (name.isBlank() || resource.isBlank() || description.isBlank()) {
            throw new IllegalArgumentException("Поля Permission не могут быть пустыми");
        }

        if (name.contains(" ")) {
            throw new IllegalArgumentException("Поле name не должен содержать пробелы");
        }

        this.name = name.toUpperCase();
        this.resource = resource.toLowerCase();
        this.description = description.trim();
    }

    public String format() {
        return String.format("%s on %s: %s", name, resource, description);
    }

    public boolean matches(String namePattern, String resourcePattern) {
        boolean nameMatches = namePattern == null ||
                                namePattern.isBlank() ||
                                name.contains(namePattern.toUpperCase());

        boolean resourceMatches = resourcePattern == null ||
                                    resourcePattern.isBlank() ||
                                    resource.contains(resourcePattern.toLowerCase());

        return nameMatches && resourceMatches;
    }

    static void main(String[] args) {
        System.out.println("\nТестирование Permission");

        //Успешное создание
        try {
            Permission readUsers = new Permission("read", "USERS", "  Разрешение на чтение пользователей  ");
            System.out.println("\nСоздано: " + readUsers);
            System.out.println("format(): " + readUsers.format());
        } catch (IllegalArgumentException ex) {
            System.out.println("\nОшибка: " + ex.getMessage());
        }

        //Проверка нормализации
        try {
            Permission writeReports = new Permission("write", "REPORTS", "Запись в отчеты");
            System.out.println("\nСоздано: " + writeReports);
            System.out.println("name после нормализации: " + writeReports.name());
            System.out.println("resource после нормализации: " + writeReports.resource());
            System.out.println("description после нормализации: '" + writeReports.description() + "'");
        } catch (IllegalArgumentException ex) {
            System.out.println("\nОшибка: " + ex.getMessage());
        }

        //Пробел в name
        try {
            Permission badName = new Permission("delete all", "settings", "Удаление настроек");
            System.out.println("\nСоздано: " + badName);
        } catch (IllegalArgumentException ex) {
            System.out.println("\nОшибка : " + ex.getMessage());
        }

        //Пустое описание
        try {
            Permission emptyDesc = new Permission("execute", "scripts", "   ");
            System.out.println("\nСоздано: " + emptyDesc);
        } catch (IllegalArgumentException ex) {
            System.out.println("\nОшибка : " + ex.getMessage());
        }

        System.out.println("\nТестирование метода matches()");

        //Создаем набор прав для тестирования
        Permission[] permissions = {
                new Permission("READ", "users", "Чтение пользователей"),
                new Permission("WRITE", "users", "Запись пользователей"),
                new Permission("READ", "reports", "Чтение отчетов"),
                new Permission("DELETE", "users", "Удаление пользователей")
        };

        System.out.println("\nВсе права:");
        for (Permission p : permissions) {
            System.out.println(p.format());
        }

        System.out.println("\nПоиск по шаблону name='READ':");
        for (Permission p : permissions) {
            if (p.matches("READ", null)) {
                System.out.println(p.format());
            }
        }

        System.out.println("\nПоиск по шаблону resource='users':");
        for (Permission p : permissions) {
            if (p.matches(null, "users")) {
                System.out.println(p.format());
            }
        }

        System.out.println("\nПоиск по обоим шаблонам name='READ', resource='users':");
        for (Permission p : permissions) {
            if (p.matches("READ", "users")) {
                System.out.println(p.format());
            }
        }

        System.out.println("\nПоиск с частичным совпадением (name='RE', resource='rep'):");
        for (Permission p : permissions) {
            if (p.matches("RE", "rep")) {
                System.out.println(p.format());
            }
        }

        System.out.println("\nДемонстрация equals() и hashCode() от record");

        Permission perm1 = new Permission("read", "users", "Чтение");
        Permission perm2 = new Permission("READ", "USERS", "Чтение");
        Permission perm3 = new Permission("write", "users", "Запись");

        System.out.println("perm1: " + perm1);
        System.out.println("perm2: " + perm2);
        System.out.println("perm3: " + perm3);
        System.out.println("perm1.equals(perm2): " + perm1.equals(perm2));
        System.out.println("perm1.equals(perm3): " + perm1.equals(perm3));
        System.out.println("perm1.hashCode(): " + perm1.hashCode());
        System.out.println("perm2.hashCode(): " + perm2.hashCode());
    }
}
