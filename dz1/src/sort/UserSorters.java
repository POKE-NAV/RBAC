package sort;

import record.User;

import java.util.*;

public class UserSorters {
    public static Comparator<User> byUsername() {
        return (user1, user2) -> user1.username().compareTo(user2.username());
    }
    public static Comparator<User> byFullName() {
        return (user1, user2) -> user1.fullname().compareTo(user2.fullname());
    }
    public static Comparator<User> byEmail() {
        return (user1, user2) -> user1.email().compareTo(user2.email());
    }

    public static void main(String[] args) {
        // Создаем список пользователей в произвольном порядке
        List<User> users = new ArrayList<>();
        users.add(User.validate("john_doe", "John Doe", "john@gmail.com"));
        users.add(User.validate("admin", "Admin User", "admin@company.com"));
        users.add(User.validate("alice", "Alice Wonder", "alice@company.com"));
        users.add(User.validate("bob", "Bob Johnson", "bob@gmail.com"));

        System.out.println("ИСХОДНЫЙ СПИСОК ");
        printUsers(users);

        Collections.sort(users, UserSorters.byUsername());
        System.out.println("\nПОСЛЕ СОРТИРОВКИ ПО USERNAME");
        printUsers(users);

        Collections.sort(users, UserSorters.byFullName());
        System.out.println("\nПОСЛЕ СОРТИРОВКИ ПО FULL NAME");
        printUsers(users);


        Collections.sort(users, UserSorters.byEmail());
        System.out.println("\nПОСЛЕ СОРТИРОВКИ ПО EMAIL");
        printUsers(users);
    }

    private static void printUsers(List<User> users) {
        for (User user : users) {
            System.out.println("  " + user.format());
        }
    }
}
