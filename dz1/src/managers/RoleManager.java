package managers;

import filters.RoleFilter;
import record.Permission;
import record.User;
import role.Role;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RoleManager implements Repository<Role>{
    private final Map<String, Role> rolesById = new HashMap<>();
    private final Map<String, Role> rolesByName = new HashMap<>();
    private UserManager userManager;

    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }

    @Override
    public void add(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role не может быть равен null");
        }

        String roleName = role.getName();
        if (rolesByName.containsKey(roleName)) {
            throw new IllegalStateException("Роль с именем " + roleName + " уже существует");
        }

        String roleId = role.getId();
        if (rolesById.containsKey(roleId)) {
            throw new IllegalStateException("Роль с ID " + roleId + " уже существует");
        }

        rolesById.put(roleId, role);
        rolesByName.put(roleName, role);
    }

    @Override
    public boolean remove(Role role) {
        if (role == null) {
            return false;
        }

        if (!rolesById.containsKey(role.getId())) {
            return false;
        }

        if (userManager != null || isRoleAssigned(role)) {
            throw new IllegalStateException(  "Невозможно удалить роль '" + role.getName() + "', так как она назначена пользователям");
        }

        rolesById.remove(role.getId());
        rolesByName.remove(role.getName());
        return true;
    }

    private boolean isRoleAssigned(Role role) {
        List<User> allUsers = userManager.findAll();


        for (User user : allUsers) {
        }

        return false;
    }

    @Override
    public Optional<Role> findById(String id) {
        if (id.isBlank() || id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(rolesById.get(id));
    }

    @Override
    public List<Role> findAll() {
        return new ArrayList<>(rolesById.values());
    }

    @Override
    public int count() {
        return rolesById.size();
    }

    @Override
    public void clear() {
        rolesById.clear();
        rolesByName.clear();
    }

    public Optional<Role> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(rolesByName.get(name));
    }

    public boolean exists(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return rolesByName.containsKey(name);
    }

    public List<Role> findByFilter(RoleFilter filter) {
        if (filter == null) {
            throw new IllegalStateException("Filter не может быть равен null");
        }

        return rolesById.values().stream().filter(filter::test).collect(Collectors.toList());
    }

    public List<Role> findAll(RoleFilter filter, Comparator<Role> sorter) {
        Stream<Role> stream = rolesById.values().stream();

        if (filter != null) {
            stream.filter(filter::test);
        }

        List<Role> result = stream.collect(Collectors.toList());

        if (sorter != null) {
            result.sort(sorter);
        }
        return result;
    }

    public void addPermissionToRole(String roleName, Permission permission) {
        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("Имя роли не может быть пустым");
        }

        if (permission == null) {
            throw new IllegalArgumentException("Право не может быть null");
        }

        Role role = rolesByName.get(roleName);

        if (role == null) {
            throw new IllegalArgumentException("Роль с именем '" + roleName + "' не найдена");
        }

        role.addPermissions(permission);
    }

    public void removePermissionFromRole(String roleName, Permission permission) {
        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("Имя роли не может быть пустым");
        }

        if (permission == null) {
            throw new IllegalArgumentException("Право не может быть null");
        }

        Role role = rolesByName.get(roleName);

        if (role == null) {
            throw new IllegalArgumentException("Роль с именем '" + roleName + "' не найдена");
        }

        role.removePermission(permission);
    }

    public List<Role> findRolesWithPermission(String permissionName, String resource) {
        if (permissionName == null || permissionName.isBlank() ||
                resource == null || resource.isBlank()) {
            return new ArrayList<>();
        }

        return rolesById.values().stream().filter(role -> role.hasPermission(permissionName, resource)).collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RoleManager that = (RoleManager) o;

        if (this.rolesById.size() != that.rolesById.size()) {
            return false;
        }

        for (Map.Entry<String, Role> entry : this.rolesById.entrySet()) {
            String roleId = entry.getKey();
            Role thisRole = entry.getValue();
            Role thatRole = that.rolesById.get(roleId);

            if (thatRole == null || !thisRole.equals(thatRole)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = 17;
        for (Role role : rolesById.values()) {
            result = 31 * result + (role != null ? role.hashCode() : 0);
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RoleManager{");
        sb.append("size=").append(rolesById.size());

        if (!rolesById.isEmpty()) {
            sb.append(", roles=[\n");

            List<Role> sortedRoles = new ArrayList<>(rolesById.values());
            sortedRoles.sort(Comparator.comparing(Role::getName));

            for (Role role : sortedRoles) {
                sb.append("    ").append(role.getName())
                        .append(" (ID: ").append(role.getId())
                        .append(", прав: ").append(role.getPermissions().size())
                        .append(")\n");
            }
            sb.append("  ]");
        }

        sb.append("}");
        return sb.toString();
    }

}




