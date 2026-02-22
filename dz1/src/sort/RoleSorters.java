package sort;

import role.Role;

import java.util.Comparator;

public class RoleSorters {
    public static Comparator<Role> byName() {
        return (role1, role2) -> role1.getName().compareTo(role2.getName());
    }

    public static Comparator<Role> byPermissionCount() {
        return (role1, role2) -> {
            int count1 = role1.getPermissions().size();
            int count2 = role2.getPermissions().size();
            return Integer.compare(count1, count2);
        };
    }

}
