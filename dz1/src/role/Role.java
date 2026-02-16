package role;

import record.Permission;

import java.util.*;

public class Role {
    private final String id;
    private final String name;
    private String description;
    private Set<Permission> permissions;

    public Role(String name, String description) {
        //Каноническая форма представления
        // UUID — это 36-символьная строка
        // (32 шестнадцатеричных цифры + 4 дефиса

        // Для генерации UUID на основе email
        //byte[] emailBytes = "user@example.com".getBytes();
        //UUID id3 = UUID.nameUUIDFromBytes(emailBytes);
        // Всегда будет одинаковым для этого email

        this.id = "role_" + UUID.randomUUID().toString(); //Всегда уникальный
        this.name = name;
        this.description = description;
        this.permissions = new HashSet<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<Permission> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }

    public void addPermissions(Permission permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Permissions не может быть равен null");
        }
        permissions.add(permission);
    }

    public void removePermission(Permission permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Permissions не может быть равен null");
        }
        permissions.remove(permission);
    }
    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    public boolean hasPermission(String permissionName, String resource) {
        for (Permission p : permissions) {
            if(p.name().equalsIgnoreCase(permissionName) && p.resource().equalsIgnoreCase(resource)) {
                return true;
            }
        }
        return false;
    }

    public String format() {
        StringBuilder str = new StringBuilder();
        str.append(String.format("Role: %s [ID: %s]\n", name, id));
        str.append(String.format("Description: %s\n", description));
        str.append(String.format("Permissions (%d):\n", permissions.size()));

        if (permissions.isEmpty()) {
            str.append(" No permissions\\n");
        } else {
            for (Permission p : permissions) {
                str.append("- ").append(p.format()).append("\n");
            }
        }
        return str.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return getId() == role.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "Role{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", permissions=" + permissions +
                '}';
    }
    public static void main(String[] args) {
        System.out.println("Тестирование создания ролей\n");

        // Создаем несколько прав для тестов
        Permission readUsers = new Permission("READ", "users", "Can view user list");
        Permission writeUsers = new Permission("WRITE", "users", "Can create and edit users");
        Permission deleteUsers = new Permission("DELETE", "users", "Can delete users");
        Permission readReports = new Permission("READ", "reports", "Can view reports");

        System.out.println("Созданные права:");
        System.out.println("  " + readUsers.format());
        System.out.println("  " + writeUsers.format());
        System.out.println("  " + deleteUsers.format());
        System.out.println("  " + readReports.format());

        System.out.println("\nТест 1: Создание роли администратора\n");

        Role adminRole = new Role("Administrator", "Full system access");
        System.out.println("Создана роль:");
        System.out.println(adminRole.format());

        System.out.println("Добавляем права администратору");
        adminRole.addPermissions(readUsers);
        adminRole.addPermissions(writeUsers);
        adminRole.addPermissions(deleteUsers);
        adminRole.addPermissions(readReports);

        System.out.println(adminRole.format());

        System.out.println("\nТест 2: Создание роли наблюдателя\n");

        Role viewerRole = new Role("Viewer", "Read-only access");
        viewerRole.addPermissions(readUsers);
        viewerRole.addPermissions(readReports);

        System.out.println(viewerRole.format());

        System.out.println("\nТест 3: Проверка наличия прав\n");

        System.out.println("Проверка прав администратора:");
        System.out.println("  hasPermission(READ, users): " +
                adminRole.hasPermission("READ", "users"));
        System.out.println("  hasPermission(DELETE, users): " +
                adminRole.hasPermission("DELETE", "users"));
        System.out.println("  hasPermission(EXECUTE, scripts): " +
                adminRole.hasPermission("EXECUTE", "scripts"));

        System.out.println("\nПроверка прав наблюдателя:");
        System.out.println("  hasPermission(READ, users): " +
                viewerRole.hasPermission("READ", "users"));
        System.out.println("  hasPermission(WRITE, users): " +
                viewerRole.hasPermission("WRITE", "users"));

        System.out.println("\nТест 4: Удаление прав\n");

        System.out.println("Удаляем право DELETE у администратора");
        adminRole.removePermission(deleteUsers);
        System.out.println("  hasPermission(DELETE, users) после удаления: " +
                adminRole.hasPermission("DELETE", "users"));

        System.out.println("\nТест 5: Неизменяемость возвращаемого Set\n");

        try {
            Set<Permission> perms = adminRole.getPermissions();
            System.out.println("Пытаемся изменить неизменяемый Set...");
            perms.add(deleteUsers);
        } catch (UnsupportedOperationException e) {
            System.out.println("Ошибка: " + e.getClass().getSimpleName() + " - нельзя изменить неизменяемую коллекцию");
        }

        System.out.println("\nТест 6: Сравнение ролей по equals\n");

        Role anotherAdmin = new Role("Administrator", "Another admin");
        System.out.println("adminRole ID: " + adminRole.getId());
        System.out.println("anotherAdmin ID: " + anotherAdmin.getId());
        System.out.println("adminRole.equals(anotherAdmin): " +
                adminRole.equals(anotherAdmin));

        System.out.println("\nadminRole.name: " + adminRole.getName());
        System.out.println("anotherAdmin.name: " + anotherAdmin.getName());
        System.out.println("adminRole.equals(anotherAdmin) при одинаковых name: " +
                adminRole.equals(anotherAdmin));

        System.out.println("\nТест 7: toString()\n");
        System.out.println("toString администратора:");
        System.out.println(adminRole.toString());
    }
}
