package filters;

import record.User;

import java.util.List;
import java.util.Locale;

public class UserFilters {
    public static UserFilter byUsername(String username) {
        return user -> user.username().equals(username);
    }

    public static UserFilter byUsernameContains(String substring) {
        return user -> user.username().toLowerCase().contains(substring.toLowerCase());
    }

    public static UserFilter byEmail(String email) {
        return user -> user.email().equals(email);
    }

    public static UserFilter byEmailDomain(String domain) {
        return user -> user.email().toLowerCase().endsWith(domain.toLowerCase());
    }

    public static UserFilter byFullNameContains(String substring) {
        return user -> user.fullname().toLowerCase().contains(substring.toLowerCase());
    }

    static void main(String[] args) {
        System.out.println("ТЕСТИРОВАНИЕ ФИЛЬТРОВ ПОЛЬЗОВАТЕЛЕЙ");

        System.out.println("ШАГ 1: Создание тестовых пользователей\n");
        User admin = User.validate("admin", "Admin User", "admin@company.com");
        User john = User.validate("john_doe", "John Doe", "john@gmail.com");
        User jane = User.validate("jane_smith", "Jane Smith", "jane@gmail.com");
        User bob = User.validate("bob_johnson", "Bob Johnson", "bob@company.com");
        User alice = User.validate("alice_wonder", "Alice Wonder", "alice@company.com");

        List<User> users = List.of(admin, john, jane, bob, alice);
        System.out.println("Тестовые пользователи:");
        for (User user : users) {
            System.out.println("  " + user.format());
        }
        System.out.println();

        System.out.println("ШАГ 2: Тестирование простых фильтров\n");

        // Тест 2.1: Фильтр по точному username
        System.out.println("ТЕСТ 1: byUsername(\"admin\")");
        UserFilter filter1 = byUsername("admin");
        for (User user : users) {
            if (filter1.test(user)) {
                System.out.println(user.format());
            }
        }
        System.out.println();
        System.out.println("ТЕСТ 1.2: byUsername(\"not_exist\") - нет такого");
        UserFilter filter1_2 = byUsername("not_exist");
        boolean found = false;
        for (User user : users) {
            if (filter1_2.test(user)) {
                System.out.println(user.format());
                found = true;
            }
        }
        if (!found) {
            System.out.println("  (нет пользователей)");
        }
        System.out.println();

        System.out.println("ТЕСТ 2: byEmail(\"john@gmail.com\")");
        UserFilter filter2 = byEmail("john@gmail.com");
        for (User user : users) {
            if (filter2.test(user)) {
                System.out.println(user.format());
            }
        }
        System.out.println();

        System.out.println("ТЕСТ 3: byUsernameContains(\"john\")");
        UserFilter filter3 = byUsernameContains("john");
        for (User user : users) {
            if (filter3.test(user)) {
                System.out.println(user.format());
            }
        }
        System.out.println();

        System.out.println("ТЕСТ 3.2: byUsernameContains(\"JANE\") - проверка регистра");
        UserFilter filter3_2 = byUsernameContains("JANE");
        for (User user : users) {
            if (filter3_2.test(user)) {
                System.out.println(user.format());
            }
        }
        System.out.println();

        System.out.println("ТЕСТ 4: byFullNameContains(\"john\")");
        UserFilter filter4 = byFullNameContains("john");
        for (User user : users) {
            if (filter4.test(user)) {
                System.out.println(user.format());
            }
        }
        System.out.println();

        System.out.println("ТЕСТ 4.2: byFullNameContains(\"SMITH\") - проверка регистра");
        UserFilter filter4_2 = byFullNameContains("SMITH");
        for (User user : users) {
            if (filter4_2.test(user)) {
                System.out.println(user.format());
            }
        }
        System.out.println();

        System.out.println("ТЕСТ 5: byEmailDomain(\"@gmail.com\")");
        UserFilter filter5 = byEmailDomain("@gmail.com");
        for (User user : users) {
            if (filter5.test(user)) {
                System.out.println(user.format());
            }
        }
        System.out.println();

        System.out.println("ТЕСТ 5.2: byEmailDomain(\"@COMPANY.COM\") - проверка регистра");
        UserFilter filter5_2 = byEmailDomain("@COMPANY.COM");
        for (User user : users) {
            if (filter5_2.test(user)) {
                System.out.println(user.format());
            }
        }
        System.out.println();

        System.out.println("ТЕСТ 6: byEmailDomain(\"@gmail.com\").and(byFullNameContains(\"john\"))");
        UserFilter filter6 = byEmailDomain("@gmail.com").and(byFullNameContains("john"));
        for (User user : users) {
            if (filter6.test(user)) {
                System.out.println(user.format());
            }
        }
        System.out.println();

        System.out.println("ТЕСТ 7: byEmailDomain(\"@gmail.com\").or(byFullNameContains(\"john\"))");
        UserFilter filter7 = byEmailDomain("@gmail.com").or(byFullNameContains("john"));
        for (User user : users) {
            if (filter7.test(user)) {
                System.out.println(user.format());
            }
        }
        System.out.println();

        System.out.println("ТЕСТ 8: (gmail AND john) OR (company AND alice)");
        UserFilter filter8 = (byEmailDomain("@gmail.com").and(byFullNameContains("john")))
                .or(byEmailDomain("@company.com").and(byFullNameContains("alice")));
        for (User user : users) {
            if (filter8.test(user)) {
                System.out.println(user.format());
            }
        }
        System.out.println();
    }

}
