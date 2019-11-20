package vv.photodb;

import java.io.File;
import java.sql.*;
import java.util.Date;

public class PhotosDAO {


    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:D:/Private/PhotoDB/db/photodb");
    }


    public static boolean isSaved(Connection conn, String path) throws SQLException {
        try (ResultSet resultSet = conn.createStatement().executeQuery("select * from photos where path='" + path + "'")) {
            return resultSet.next();
        }
    }


    public static void save(Connection conn, File file, Date createDate, String md5, String equipment, String comment) throws
            SQLException {

        equipment = equipment != null ? equipment.trim() : equipment;

        PreparedStatement st = conn.prepareStatement("INSERT INTO photos (path, name, ext, size, created, md5, equipment, comment) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(path) DO update set created = ?, md5 = ?, equipment = ?, comment = ?");
        st.setString(1, file.getAbsolutePath());
        st.setString(2, file.getName());
        st.setString(3, Utils.getFileExtension(file.getName()));
        st.setLong(4, file.length());
        st.setString(5, createDate == null ? null : Utils.formatter.format(createDate));
        st.setString(6, md5);
        st.setString(7, equipment);
        st.setString(8, comment);
        st.setString(9, createDate == null ? null : Utils.formatter.format(createDate));
        st.setString(10, md5);
        st.setString(11, equipment);
        st.setString(12, comment);

        st.executeUpdate();
    }
}
