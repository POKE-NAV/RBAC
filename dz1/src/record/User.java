package record;

public record User(String username, String fullname, String email) {

    public User {
        if (username == null || fullname == null || email == null) {
            throw new IllegalArgumentException("Поля конструктора User не могут быть равны null");
        }

        if (username.isBlank() || fullname.isBlank() || email.isBlank()) {
            throw new IllegalArgumentException("Поля конструктора User не могут быть пустыми");
        }

        if (username.length() < 3 || username.length() > 20) {
            throw new IllegalArgumentException("Длина username должна быть < 3 и  > 20");
        }

        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Username может содержать только латинские буквы, цифры и подчеркивание");
        }

        if (!email.contains("@") || !email.substring(email.indexOf("@") + 1).contains(".")) {
            throw new IllegalArgumentException("Email должен содержать @ и точку после @");
        }
    }

    public static User validate(String username, String fullname, String email) {
        return new User(username, fullname, email);
    }

    public String format() {
        return String.format("%s (%s) <%s>", username, fullname, email);
    }

    static void main(String[] args) {
        System.out.println("Тестирование пользователей");

        //Успешное создание
        try {
            User user1 = User.validate("Alex_Ignatov", "Ignatov Alexander", "alexander@mail.ru");
            System.out.println("Успех " + user1.format());
        } catch (IllegalArgumentException ex) {
            System.out.println("Ошибка " + ex.getMessage());
        }

        //Короткий username
        try {
            User user2 = User.validate("Al", "Ignatov Alexander", "alexander@mail.ru");
            System.out.println("Успех " + user2.format());
        } catch (IllegalArgumentException ex) {
            System.out.println("Ошибка  " + ex.getMessage());
        }

        //Недопустимые символы в username
        try {
            User user3 = User.validate("Alexander@", "Ignatov Alexander", "alexander@mail.ru");
            System.out.println("Успех " + user3.format());
        } catch (IllegalArgumentException ex) {
            System.out.println("Ошибка  " + ex.getMessage());
        }

        //Null
        try {
            User user4 = User.validate(null, null, null);
            System.out.println("Успех " + user4.format());
        } catch (IllegalArgumentException ex) {
            System.out.println("Ошибка  " + ex.getMessage());
        }

        //Неправильный email
        try {
            User user5 = User.validate("Alexander", "Ignatov Alexander", "alexander@mailru");
            System.out.println("Успех " + user5.format());
        } catch (IllegalArgumentException ex) {
            System.out.println("Ошибка  " + ex.getMessage());
        }
    }
}
