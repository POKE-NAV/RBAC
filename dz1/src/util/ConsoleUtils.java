package util;

import interfaces.RoleAssignment;
import record.Permission;
import record.User;
import role.Role;

import java.util.List;
import java.util.Scanner;

public class ConsoleUtils {

    public static final String RESET = "\u001B[0m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String RED = "\u001B[31m";
    public static final String CYAN = "\u001B[36m";
    public static final String BOLD = "\u001B[1m";

    private static final String PROMPT_SYMBOL = "▶";

    public static String promptString(Scanner scanner, String message, boolean required) {
        while (true) {
            System.out.print(CYAN + PROMPT_SYMBOL + " " + message + ": " + RESET);
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                if (required) {
                    System.out.println(RED + "  ⚠ Ошибка: поле не может быть пустым" + RESET);
                } else {
                    return "";
                }
            } else {
                return input;
            }
        }
    }

    public static int promptInt(Scanner scanner, String message, int min, int max) {
        while (true) {
            System.out.print(CYAN + PROMPT_SYMBOL + " " + message + " [" + min + "-" + max + "]: " + RESET);
            String input = scanner.nextLine().trim();

            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                } else {
                    System.out.println(RED + "  ⚠ Ошибка: введите число от " + min + " до " + max + RESET);
                }
            } catch (NumberFormatException e) {
                System.out.println(RED + "  ⚠ Ошибка: введите целое число" + RESET);
            }
        }
    }

    public static boolean promptYesNo(Scanner scanner, String message) {
        System.out.print(YELLOW + PROMPT_SYMBOL + " " + message + " (да/нет): " + RESET);
        String input = scanner.nextLine().trim().toLowerCase();

        return input.equals("да") || input.equals("д") ||
                input.equals("yes") || input.equals("y");
    }

    public static <T> T promptChoice(Scanner scanner, String message, List<T> options) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Список вариантов не может быть пустым");
        }

        System.out.println("\n" + BOLD + message + ":" + RESET);

        // Выводим варианты с номерами
        for (int i = 0; i < options.size(); i++) {
            T option = options.get(i);
            System.out.printf("  %s%d.%s %s\n",
                    GREEN, i + 1, RESET,
                    optionToString(option));
        }

        // Добавляем опцию отмены, если список большой
        if (options.size() > 1) {
            System.out.printf("  %s0.%s Отмена\n", RED, RESET);
        }

        while (true) {
            System.out.print(CYAN + PROMPT_SYMBOL + " Ваш выбор: " + RESET);
            String input = scanner.nextLine().trim();

            try {
                int choice = Integer.parseInt(input);

                if (choice == 0) {
                    return null; // пользователь выбрал отмену
                }

                if (choice >= 1 && choice <= options.size()) {
                    return options.get(choice - 1);
                } else {
                    System.out.println(RED + "  ⚠ Ошибка: выберите номер от 1 до " + options.size() + RESET);
                }
            } catch (NumberFormatException e) {
                System.out.println(RED + "  ⚠ Ошибка: введите номер варианта" + RESET);
            }
        }
    }

    private static <T> String optionToString(T option) {
        if (option == null) return "null";

        // Специальная обработка для разных типов
        if (option instanceof User) {
            User user = (User) option;
            return user.username() + " (" + user.fullname() + ")";
        } else if (option instanceof Role) {
            Role role = (Role) option;
            return role.getName() + " - " + role.getDescription();
        } else if (option instanceof Permission) {
            Permission perm = (Permission) option;
            return perm.name() + " on " + perm.resource();
        } else if (option instanceof RoleAssignment) {
            RoleAssignment ra = (RoleAssignment) option;
            return ra.user().username() + " → " + ra.role().getName();
        }

        return option.toString();
    }

    public static void printHeader(String title) {
        String line = "_".repeat(title.length() + 4);
        System.out.println("\n" + GREEN + "+" + line + "+" + RESET);
        System.out.println(GREEN + "|  " + BOLD + title + RESET + GREEN + "  |" + RESET);
        System.out.println(GREEN + "+" + line + "+" + RESET);
    }

    public static void printSubheader(String title) {
        System.out.println("\n" + YELLOW + "▶ " + title + ":" + RESET);
    }

    public static void printSuccess(String message) {
        System.out.println(GREEN + "  ✅ " + message + RESET);
    }

    public static void printError(String message) {
        System.out.println(RED + "  ❌ " + message + RESET);
    }

    public static void printInfo(String message) {
        System.out.println(CYAN + "  ℹ " + message + RESET);
    }

    public static void printWarning(String message) {
        System.out.println(YELLOW + "  ⚠ " + message + RESET);
    }

    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static void pressEnterToContinue(Scanner scanner) {
        System.out.print(YELLOW + "\nНажмите Enter для продолжения..." + RESET);
        scanner.nextLine();
    }
}
