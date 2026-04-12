package util;

import interfaces.RoleAssignment;
import record.User;
import role.Role;

import java.util.List;

public class FormatUtils {

    private static final char HORIZONTAL = '-';
    private static final char VERTICAL = '|';
    private static final String CROSS = "+";

    public static String formatTable(String[] headers, List<String[]> rows) {
        if (headers == null || headers.length == 0) {
            return "";
        }

        int columnCount = headers.length;
        int[] columnWidths = calculateColumnWidths(headers, rows, columnCount);

        StringBuilder table = new StringBuilder();

        // Верхняя граница
        table.append(CROSS);
        for (int i = 0; i < columnCount; i++) {
            table.append(repeatChar(HORIZONTAL, columnWidths[i] + 2));
            if (i < columnCount - 1) {
                table.append(CROSS);
            }
        }
        table.append(CROSS).append("\n");

        // Заголовки
        table.append(VERTICAL);
        for (int i = 0; i < columnCount; i++) {
            table.append(" ").append(padRight(headers[i], columnWidths[i])).append(" ").append(VERTICAL);
        }
        table.append("\n");

        // Разделитель между заголовками и данными
        table.append(CROSS);
        for (int i = 0; i < columnCount; i++) {
            table.append(repeatChar(HORIZONTAL, columnWidths[i] + 2));
            if (i < columnCount - 1) {
                table.append(CROSS);
            }
        }
        table.append(CROSS).append("\n");

        // Данные
        if (rows != null) {
            for (String[] row : rows) {
                if (row.length < columnCount) {
                    continue;
                }

                table.append(VERTICAL);
                for (int i = 0; i < columnCount; i++) {
                    String cell = row[i] != null ? row[i] : "";
                    table.append(" ").append(padRight(cell, columnWidths[i])).append(" ").append(VERTICAL);
                }
                table.append("\n");
            }
        }

        // Нижняя граница
        table.append(CROSS);
        for (int i = 0; i < columnCount; i++) {
            table.append(repeatChar(HORIZONTAL, columnWidths[i] + 2));
            if (i < columnCount - 1) {
                table.append(CROSS);
            }
        }
        table.append(CROSS).append("\n");

        return table.toString();
    }

    private static int[] calculateColumnWidths(String[] headers, List<String[]> rows, int columnCount) {
        int[] widths = new int[columnCount];

        // Учитываем заголовки
        for (int i = 0; i < columnCount; i++) {
            widths[i] = headers[i].length();
        }

        // Учитываем данные
        if (rows != null) {
            for (String[] row : rows) {
                if (row.length >= columnCount) {
                    for (int i = 0; i < columnCount; i++) {
                        if (row[i] != null && row[i].length() > widths[i]) {
                            widths[i] = row[i].length();
                        }
                    }
                }
            }
        }

        return widths;
    }

    public static String formatBox(String text) {
        if (text == null || text.isEmpty()) {
            return "++\n||\n++";
        }

        String[] lines = text.split("\n");
        int maxLength = 0;
        for (String line : lines) {
            maxLength = Math.max(maxLength, line.length());
        }

        StringBuilder box = new StringBuilder();

        // Верхняя граница
        box.append("+").append(repeatChar(HORIZONTAL, maxLength + 2)).append("+\n");

        // Строки текста
        for (String line : lines) {
            box.append("| ").append(padRight(line, maxLength)).append(" |\n");
        }

        // Нижняя граница
        box.append("+").append(repeatChar(HORIZONTAL, maxLength + 2)).append("+\n");

        return box.toString();
    }

    public static String formatHeader(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder header = new StringBuilder();
        String line = repeatChar(HORIZONTAL, text.length() + 4);

        header.append("+").append(line).append("+\n");
        header.append("|  ").append(text).append("  |\n");
        header.append("+").append(line).append("+\n");

        return header.toString();
    }

    public static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        if (maxLength < 3) return text.substring(0, maxLength);
        return text.substring(0, maxLength - 3) + "...";
    }

    public static String padRight(String text, int length) {
        if (text == null) text = "";
        if (text.length() >= length) return text;
        return text + repeatChar(' ', length - text.length());
    }

    public static String padLeft(String text, int length) {
        if (text == null) text = "";
        if (text.length() >= length) return text;
        return repeatChar(' ', length - text.length()) + text;
    }

    private static String repeatChar(char ch, int count) {
        if (count <= 0) return "";
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }

    public static String formatUsersTable(List<User> users) {
        if (users == null || users.isEmpty()) {
            return "Нет пользователей для отображения";
        }

        String[] headers = {"Username", "Full Name", "Email"};
        List<String[]> rows = users.stream()
                .map(user -> new String[]{
                        truncate(user.username(), 20),
                        truncate(user.fullname(), 30),
                        truncate(user.email(), 25)
                })
                .toList();

        return formatTable(headers, rows);
    }

    public static String formatRolesTable(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return "Нет ролей для отображения";
        }

        String[] headers = {"Name", "Description", "Permissions"};
        List<String[]> rows = roles.stream()
                .map(role -> new String[]{
                        truncate(role.getName(), 20),
                        truncate(role.getDescription(), 30),
                        String.valueOf(role.getPermissions().size())
                })
                .toList();

        return formatTable(headers, rows);
    }


    public static String formatAssignmentsTable(List<RoleAssignment> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return "Нет назначений для отображения";
        }

        String[] headers = {"User", "Role", "Type", "Status", "Date"};
        List<String[]> rows = assignments.stream()
                .map(ra -> new String[]{
                        truncate(ra.user().username(), 15),
                        truncate(ra.role().getName(), 20),
                        ra.assignmentType(),
                        ra.isActive() ? "ACTIVE" : "INACTIVE",
                        ra.metadata().assignedAt()
                })
                .toList();

        return formatTable(headers, rows);
    }
}
