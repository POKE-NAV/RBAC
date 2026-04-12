package managers;
import filters.UserFilter;
import record.User;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.concurrent.*;
public class UserManager implements Repository<User> {
    private final ConcurrentMap<String, User> users = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public void add(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User не может быть равен null");
        }

        String username = user.username();
        User error = users.putIfAbsent(username, user);
        if (error != null) {
            throw new IllegalStateException("Пользователь с username " + username + " уже существует");
        }
    }

    @Override
    public boolean remove(User user) {
        if (user == null) {
            return false;
        }

        User removed = users.remove(user.username());
        return removed != null;
    }

    @Override
    public Optional<User> findById(String id) {
       if (id == null || id.isBlank()) {
           return Optional.empty();
       }

       User user = users.get(id);
       return Optional.ofNullable(user);
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public int count() {
        return users.size();
    }

    @Override
    public void clear() {
        users.clear();
    }

    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(users.get(username));
    }

    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        return users.values().stream().filter(user -> user.email().equalsIgnoreCase(email)).findFirst();
    }

    public boolean exists(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }

        return users.containsKey(username);
    }

    public void update(String username, String newFullName, String newEmail) {
        lock.lock();
        try {
            if (username == null || username.isBlank()) {
                throw new IllegalArgumentException("Username не может быть пустым");
            }

            User tempUser = users.get(username);
            if (tempUser == null) {
                throw new IllegalArgumentException("Пользователь с username: " + username + " не найден");
            }

            User updateUser = User.validate(username, newFullName, newEmail);
            users.put(username, updateUser);
        } finally {
          lock.unlock();
        }

    }

    public List<User> findByFilter(UserFilter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("Filter не может быть null");
        }
        return users.values().stream().filter(filter::test).collect(Collectors.toList());
    }

    public List<User> findAll(UserFilter filter, Comparator<User> sorter) {
        Stream<User> stream = users.values().stream();

        if (filter != null) {
            stream = stream.filter(filter::test);
        }

        // Собираем в список
        List<User> result = stream.collect(Collectors.toList());

        // Сортируем, если задан компаратор
        if (sorter != null) {
            result.sort(sorter);
        }

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserManager that = (UserManager) o;

        // Сравниваем размеры хранилищ
        if (this.users.size() != that.users.size()) {
            return false;
        }

        // Сравниваем каждого пользователя
        for (Map.Entry<String, User> entry : this.users.entrySet()) {
            String username = entry.getKey();
            User thisUser = entry.getValue();
            User thatUser = that.users.get(username);

            // Если у другого менеджера нет такого username или пользователи разные
            if (thatUser == null || !thisUser.equals(thatUser)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = 17;
        for (User user : users.values()) {
            result = 31 * result + (user != null ? user.hashCode() : 0);
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("UserManager{");
        sb.append("size=").append(users.size());

        if (!users.isEmpty()) {
            sb.append(", users=[\n");
            List<User> sortedUsers = new ArrayList<>(users.values());
            sortedUsers.sort(Comparator.comparing(User::username));

            for (User user : sortedUsers) {
                sb.append("    ").append(user.format()).append("\n");
            }
            sb.append("  ]");
        }

        sb.append("}");
        return sb.toString();
    }
}
