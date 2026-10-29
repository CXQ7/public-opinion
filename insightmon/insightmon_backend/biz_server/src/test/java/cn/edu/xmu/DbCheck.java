package cn.edu.xmu;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DbCheck {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://127.0.0.1:3306/public_eye?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true";
        String user = "dev";
        String pass = "zh1319716110";
        System.out.println("Connecting to: " + url);
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            String sql = "SELECT id, user_id, name, search_key, expire_date, create_time FROM config WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, 2);
                try (ResultSet rs = ps.executeQuery()) {
                    boolean found = false;
                    while (rs.next()) {
                        found = true;
                        System.out.println("FOUND ROW:");
                        System.out.println("id=" + rs.getLong("id") + ", user_id=" + rs.getObject("user_id") + ", name=" + rs.getString("name") + ", search_key=" + rs.getString("search_key") + ", expire_date=" + rs.getObject("expire_date") + ", create_time=" + rs.getObject("create_time"));
                    }
                    if (!found) {
                        System.out.println("NO ROW FOUND for id=2");
                        System.exit(0);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("DB check failed:");
            e.printStackTrace();
            System.exit(2);
        }
    }
}
