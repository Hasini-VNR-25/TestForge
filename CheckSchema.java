import java.sql.*;

public class CheckSchema {
    public static void main(String[] args) throws Exception {
        try (Connection con = DriverManager.getConnection("jdbc:oracle:thin:@//localhost:1521/orcl", "SCOTT", "Oracle1234")) {
            DatabaseMetaData meta = con.getMetaData();
            System.out.println("=== TF_PROBLEMS COLUMNS ===");
            try (ResultSet rs = meta.getColumns(null, "SCOTT", "TF_PROBLEMS", "%")) {
                while (rs.next()) {
                    System.out.println(rs.getString("COLUMN_NAME") + " - " + rs.getString("TYPE_NAME") + "(" + rs.getInt("COLUMN_SIZE") + ")");
                }
            }
            System.out.println("=== TF_TEST_CASES COLUMNS ===");
            try (ResultSet rs = meta.getColumns(null, "SCOTT", "TF_TEST_CASES", "%")) {
                while (rs.next()) {
                    System.out.println(rs.getString("COLUMN_NAME") + " - " + rs.getString("TYPE_NAME") + "(" + rs.getInt("COLUMN_SIZE") + ")");
                }
            }
        }
    }
}
