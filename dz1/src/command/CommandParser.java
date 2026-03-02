package command;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class CommandParser {
    private final Map<String, Command> commands= new HashMap<>();
    private final Map<String, String> commandDescriptions = new HashMap<>();

    public void registerCommand(String name, String description, Command command) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя команды не может быть пустым");
        }
        if (command == null) {
            throw new IllegalArgumentException("Команда не может быть null");
        }
        commands.put(name.toLowerCase(), command);
        commandDescriptions.put(name.toLowerCase(), description);
    }

    public void executeCommand(String commandName, Scanner scanner, RBACSystem system) {
        if (commandName == null || commandName.isBlank()) {
            System.out.println("Ошибка: Имя команды не может быть пустым");
            return;
        }

        String normalizeName = commandName.toLowerCase();
        Command command = commands.get(normalizeName);

        if (command == null) {
            System.out.println("Неизвестная команда" + commandName);
            return;
        }

        try {
            command.execute(scanner, system);
        } catch (Exception e) {
            System.out.println("Ошибка выполнения команды " + e.getMessage());
        }
    }

    public void parseAndExecute(String input, Scanner scanner, RBACSystem system) {
        if (input == null || input.isBlank()) {
            return;
        }

        String[] part = input.split("\\s+", 2);
        String commandName = part[0];

        executeCommand(commandName, scanner, system);
    }

    public void printHelp() {
        System.out.println("Доступные команды");

        if (commands.isEmpty()) {
            System.out.println("Нет зарегистрированных команд");
            return;
        }

        int maxLength = commands.keySet().stream().mapToInt(String::length).max().orElse(10);
        commands.keySet().stream().sorted().forEach(name -> {
            String description = commandDescriptions.getOrDefault(name, "Нет описания");
            System.out.printf("  %-" + maxLength + "s - %s%n", name, description);
        });
        System.out.println("_____________________________________");
    }

    public boolean hasCommand(String commandName) {
        if (commandName == null || commandName.isBlank()) {
            return false;
        }
        return commands.containsKey(commandName.toLowerCase());
    }

    public int getCommandCount() {
        return commands.size();
    }

    public void clearCommand() {
        commands.clear();
        commandDescriptions.clear();
    }
}
