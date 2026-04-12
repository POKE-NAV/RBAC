package managers;

import interfaces.RoleAssignment;
import record.Permission;
import record.User;
import role.Role;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import filters.*;
import role.*;
import java.util.concurrent.*;

public class AssignmentManager implements Repository<RoleAssignment>{
    private final ConcurrentMap<String, RoleAssignment> assignmentsById = new ConcurrentHashMap<>();

    // Ссылки на другие менеджеры для проверки существования
    private UserManager userManager;
    private RoleManager roleManager;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }

    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    @Override
    public void add(RoleAssignment assignment) {
        if (assignment == null) {
            throw new IllegalArgumentException("Назначение не может быть null");
        }

        if (userManager == null || roleManager == null) {
            throw new IllegalStateException(
                    "UserManager и RoleManager должны быть установлены перед использованием"
            );
        }

        String assignmentId = assignment.assignmentId();
        synchronized (this){
            if (assignmentsById.containsKey(assignmentId)) {
                throw new IllegalStateException(
                        "Назначение с ID '" + assignmentId + "' уже существует"
                );
            }

            User user = assignment.user();
            Optional<User> existingUser = userManager.findByUsername(user.username());
            if (existingUser.isEmpty()) {
                throw new IllegalStateException(
                        "Пользователь '" + user.username() + "' не существует в системе"
                );
            }

            Role role = assignment.role();
            Optional<Role> existingRole = roleManager.findByName(role.getName());
            if (existingRole.isEmpty()) {
                throw new IllegalStateException(
                        "Роль '" + role.getName() + "' не существует в системе"
                );
            }

            if (hasActiveAssignment(user, role)) {
                throw new IllegalStateException(
                        "Пользователь '" + user.username() + "' уже имеет активное назначение на роль '" +
                                role.getName() + "'"
                );
            }

            assignmentsById.put(assignmentId, assignment);
        }
    }

    private boolean hasActiveAssignment(User user, Role role) {
        for (RoleAssignment ra : assignmentsById.values()) {
            if (ra.user().equals(user) &&
                    ra.role().equals(role) &&
                    ra.isActive()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean remove(RoleAssignment assignment) {
        if (assignment == null) {
            return false;
        }

        RoleAssignment removed = assignmentsById.remove(assignment.assignmentId());
        return removed != null;
    }

    @Override
    public Optional<RoleAssignment> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(assignmentsById.get(id));
    }

    @Override
    public List<RoleAssignment> findAll() {
        return new ArrayList<>(assignmentsById.values());
    }

    @Override
    public int count() {
        return assignmentsById.size();
    }

    @Override
    public void clear() {
        assignmentsById.clear();
    }

    public List<RoleAssignment> findByUser(User user) {
        if (user == null) {
            return new ArrayList<>();
        }

        return assignmentsById.values().stream()
                .filter(ra -> ra.user().equals(user))
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return new ArrayList<>();
        }

        return assignmentsById.values().stream()
                .filter(ra -> ra.user().username().equals(username))
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findByRole(Role role) {
        if (role == null) {
            return new ArrayList<>();
        }

        return assignmentsById.values().stream()
                .filter(ra -> ra.role().equals(role))
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findByRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return new ArrayList<>();
        }

        return assignmentsById.values().stream()
                .filter(ra -> ra.role().getName().equals(roleName))
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findByFilter(AssignmentFilter filter) {
        if (filter == null) {
            return findAll();
        }

        return assignmentsById.values().stream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findAll(AssignmentFilter filter, Comparator<RoleAssignment> sorter) {
        Stream<RoleAssignment> stream = assignmentsById.values().stream();

        if (filter != null) {
            stream = stream.filter(filter::test);
        }

        List<RoleAssignment> result = stream.collect(Collectors.toList());

        if (sorter != null) {
            result.sort(sorter);
        }

        return result;
    }

    public List<RoleAssignment> getActiveAssignments() {
        return assignmentsById.values().stream()
                .filter(RoleAssignment::isActive)
                .collect(Collectors.toList());
    }


    public List<RoleAssignment> getExpiredAssignments() {
        return assignmentsById.values().stream()
                .filter(ra -> !ra.isActive())
                .collect(Collectors.toList());
    }

    public boolean userHasRole(User user, Role role) {
        if (user == null || role == null) {
            return false;
        }

        return assignmentsById.values().stream()
                .anyMatch(ra -> ra.user().equals(user) &&
                        ra.role().equals(role) &&
                        ra.isActive());
    }

    public boolean userHasRole(String username, String roleName) {
        if (username == null || username.isBlank() ||
                roleName == null || roleName.isBlank()) {
            return false;
        }

        return assignmentsById.values().stream()
                .anyMatch(ra -> ra.user().username().equals(username) &&
                        ra.role().getName().equals(roleName) &&
                        ra.isActive());
    }

    public boolean userHasPermission(User user, String permissionName, String resource) {
        if (user == null || permissionName == null || resource == null) {
            return false;
        }

        List<RoleAssignment> userAssignments = findByUser(user).stream()
                .filter(RoleAssignment::isActive)
                .collect(Collectors.toList());

        for (RoleAssignment ra : userAssignments) {
            if (ra.role().hasPermission(permissionName, resource)) {
                return true;
            }
        }

        return false;
    }

    public Set<Permission> getUserPermissions(User user) {
        if (user == null) {
            return new HashSet<>();
        }

        Set<Permission> allPermissions = new HashSet<>();

        List<RoleAssignment> userAssignments = findByUser(user).stream()
                .filter(RoleAssignment::isActive)
                .collect(Collectors.toList());

        // Собираем все права из всех ролей
        for (RoleAssignment ra : userAssignments) {
            allPermissions.addAll(ra.role().getPermissions());
        }

        return allPermissions;
    }

    public void revokeAssignment(String assignmentId) {
        if (assignmentId == null || assignmentId.isBlank()) {
            throw new IllegalArgumentException("ID назначения не может быть пустым");
        }

        synchronized (this) {
            RoleAssignment assignment = assignmentsById.get(assignmentId);
            if (assignment == null) {
                throw new IllegalArgumentException(
                        "Назначение с ID '" + assignmentId + "' не найдено"
                );
            }

            if (assignment instanceof PermanentAssignment) {
                PermanentAssignment permanent = (PermanentAssignment) assignment;
                permanent.revoke();
            } else {

                throw new UnsupportedOperationException(
                        "Отмена временных назначений будет реализована позже"
                );
            }
        }
    }

    public void extendTemporaryAssignment(String assignmentId, String newExpirationDate) {
        if (assignmentId == null || assignmentId.isBlank()) {
            throw new IllegalArgumentException("ID назначения не может быть пустым");
        }

        if (newExpirationDate == null || newExpirationDate.isBlank()) {
            throw new IllegalArgumentException("Новая дата не может быть пустой");
        }

        // Проверяем формат даты
        try {
            LocalDateTime.parse(newExpirationDate, FORMATTER);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Неверный формат даты. Используйте: yyyy-MM-dd HH:mm"
            );
        }

        synchronized (this){
            RoleAssignment assignment = assignmentsById.get(assignmentId);
            if (assignment == null) {
                throw new IllegalArgumentException(
                        "Назначение с ID '" + assignmentId + "' не найдено"
                );
            }

            // Проверяем, что это временное назначение
            if (!(assignment instanceof TemporaryAssignment)) {
                throw new IllegalArgumentException(
                        "Назначение с ID '" + assignmentId + "' не является временным"
                );
            }

            TemporaryAssignment temporary = (TemporaryAssignment) assignment;
            temporary.extend(newExpirationDate);
        }
    }


    public boolean hasActiveAssignment(String username, String roleName) {
        if (username == null || username.isBlank() ||
                roleName == null || roleName.isBlank()) {
            return false;
        }

        return assignmentsById.values().stream()
                .anyMatch(ra -> ra.user().username().equals(username) &&
                        ra.role().getName().equals(roleName) &&
                        ra.isActive());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AssignmentManager that = (AssignmentManager) o;

        // Сравниваем размеры хранилищ
        if (this.assignmentsById.size() != that.assignmentsById.size()) {
            return false;
        }

        for (Map.Entry<String, RoleAssignment> entry : this.assignmentsById.entrySet()) {
            String assignmentId = entry.getKey();
            RoleAssignment thisAssignment = entry.getValue();
            RoleAssignment thatAssignment = that.assignmentsById.get(assignmentId);

            if (thatAssignment == null || !thisAssignment.equals(thatAssignment)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = 17;
        for (RoleAssignment assignment : assignmentsById.values()) {
            result = 31 * result + (assignment != null ? assignment.hashCode() : 0);
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AssignmentManager{");
        sb.append("size=").append(assignmentsById.size());

        if (!assignmentsById.isEmpty()) {
            sb.append(", assignments=[\n");

            List<RoleAssignment> sortedAssignments = new ArrayList<>(assignmentsById.values());
            sortedAssignments.sort(Comparator.comparing(
                    ra -> ra.metadata().assignedAt()
            ));

            for (RoleAssignment ra : sortedAssignments) {
                sb.append("    ID: ").append(ra.assignmentId())
                        .append(", User: ").append(ra.user().username())
                        .append(", Role: ").append(ra.role().getName())
                        .append(", Type: ").append(ra.assignmentType())
                        .append(", Active: ").append(ra.isActive())
                        .append(", Date: ").append(ra.metadata().assignedAt());

                if (ra instanceof TemporaryAssignment) {
                    TemporaryAssignment temp = (TemporaryAssignment) ra;
                    sb.append(", Expires: ").append(temp.getExpiresAt());
                }

                sb.append("\n");
            }
            sb.append("  ]");
        }

        sb.append("}");
        return sb.toString();
    }
}
