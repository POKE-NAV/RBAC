package util;

public interface AuditActions {
    // Действия с пользователями
    String CREATE_USER = "CREATE_USER";
    String UPDATE_USER = "UPDATE_USER";
    String DELETE_USER = "DELETE_USER";
    String VIEW_USER = "VIEW_USER";

    // Действия с ролями
    String CREATE_ROLE = "CREATE_ROLE";
    String UPDATE_ROLE = "UPDATE_ROLE";
    String DELETE_ROLE = "DELETE_ROLE";
    String ADD_PERMISSION = "ADD_PERMISSION";
    String REMOVE_PERMISSION = "REMOVE_PERMISSION";
    String VIEW_ROLE = "VIEW_ROLE";

    // Действия с назначениями
    String ASSIGN_ROLE = "ASSIGN_ROLE";
    String REVOKE_ROLE = "REVOKE_ROLE";
    String EXTEND_ASSIGNMENT = "EXTEND_ASSIGNMENT";

    // Системные действия
    String SYSTEM_START = "SYSTEM_START";
    String SYSTEM_STOP = "SYSTEM_STOP";
    String LOGIN = "LOGIN";
    String DATA_SAVE = "DATA_SAVE";
    String DATA_LOAD = "DATA_LOAD";
    String REPORT_EXPORT = "REPORT_EXPORT";
    String REPORT_EXPORT_ALL = "REPORT_EXPORT_ALL";
    String REPORT_VIEW = "REPORT_VIEW";
}
