package vv.photodb;

import java.io.File;
import java.sql.*;
import java.util.Date;

public class PhotosDAO {


    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:D:/Private/PhotoDB/db/photodb");
    }


    public static boolean isSaved(Connection conn, String path, String tableForSave) throws SQLException {
        try (ResultSet resultSet = conn.createStatement().executeQuery("select * from " + tableForSave + " where path='" + path + "'")) {
            return resultSet.next();
        }
    }


    public static void save(Connection conn, String tableForSave, File file, Metadata metadata, String md5) throws
            SQLException {

        PreparedStatement st = conn.prepareStatement("INSERT INTO " + tableForSave +
                " (path, name, ext, size, created, md5, equipment, comment) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(path) DO update set created = ?, md5 = ?, equipment = ?, comment = ?");
        st.setString(1, file.getAbsolutePath());
        st.setString(2, file.getName());
        st.setString(3, Utils.getFileExtension(file.getName()));
        st.setLong(4, file.length());
        st.setString(5, metadata.createDate == null ? null : Utils.formatter.format(metadata.createDate));
        st.setString(6, md5);
        st.setString(7, metadata.equipment);
        st.setString(8, metadata.comment);
        st.setString(9, metadata.createDate == null ? null : Utils.formatter.format(metadata.createDate));
        st.setString(10, md5);
        st.setString(11, metadata.equipment);
        st.setString(12, metadata.comment);

        st.executeUpdate();
    }
}
