import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestBcrypt {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        boolean matches = encoder.matches("password123", "$2a$10$dXJ3SW6G7P50lGmMQoeGUORFrZCYgCPQKb6DQ.YeGAjqkc2gCmKHG");
        System.out.println("Matches: " + matches);
    }
}
