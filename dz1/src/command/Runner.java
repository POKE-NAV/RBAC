package command;

import java.util.Scanner;

public class Runner {

    public static void main(String[] args) {
        System.out.println("_________________________________________");
        System.out.println("   RBAC КОНСОЛЬНАЯ УТИЛИТА v1.0");
        System.out.println("_________________________________________");

        // Шаг 1: Создаем систему
        RBACSystem system = new RBACSystem();

        // Шаг 2: Связываем менеджеры
        system.getAssignmentManager().setUserManager(system.getUserManager());
        system.getAssignmentManager().setRoleManager(system.getRoleManager());
        system.getRoleManager().setAssignmentManager(system.getAssignmentManager());

        // Шаг 3: Инициализируем систему (создаем admin пользователя и базовые роли)
        System.out.println("Инициализация системы...");
        system.initialize();
        system.setCurrentUser("admin");

        // Шаг 4: Создаем парсер команд и регистрируем все команды
        CommandParser parser = new CommandParser();
        CommandRegistry.registerAllCommands(parser);

        // Шаг 5: Запускаем главный цикл обработки команд
        runCommandLoop(parser, system);
    }

    private static void runCommandLoop(CommandParser parser, RBACSystem system) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("\nСистема готова к работе!");
        System.out.println("Текущий пользователь: " + system.getCurrentUser());
        System.out.println("Введите 'help' для списка команд\n");

        while (true) {
            System.out.print("rbac> ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            // Парсим и выполняем команду
            parser.parseAndExecute(input, scanner, system);
        }
    }

}
