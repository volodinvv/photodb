package vv.photodb;

import java.sql.*;

public class PhotosDAO {


    public static Connection getConnection() throws SQLException {
        //return DriverManager.getConnection("jdbc:sqlite:D:/Private/PhotoDB/db/photodb");
        return DriverManager.getConnection("jdbc:sqlite:D:/Private/PhotoDB/home/photodb");
    }


    public static boolean isSaved(Connection conn, String path, String tableForSave) throws SQLException {
        try (ResultSet resultSet = conn.createStatement().executeQuery("select * from " + tableForSave + " where path='" + path.replace("'", "''") + "'")) {
            return resultSet.next();
        }
    }


    public static void save(Connection conn, String tableForSave, PhotoInfo photoInfo) throws
            SQLException {

        PreparedStatement st = conn.prepareStatement("INSERT INTO " + tableForSave +
                " (path, name, ext, size, created, md5, equipment, comment) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(path) DO update set created = ?, md5 = ?, equipment = ?, comment = ?");
        st.setString(1, photoInfo.path);
        st.setString(2, photoInfo.name);
        st.setString(3, photoInfo.ext);
        st.setLong(4, photoInfo.size);
        st.setString(5, photoInfo.createDate == null ? null : Utils.formatter.format(photoInfo.createDate));
        st.setString(6, photoInfo.md5);
        st.setString(7, photoInfo.equipment);
        st.setString(8, photoInfo.comment);
        st.setString(9, photoInfo.createDate == null ? null : Utils.formatter.format(photoInfo.createDate));
        st.setString(10, photoInfo.md5);
        st.setString(11, photoInfo.equipment);
        st.setString(12, photoInfo.comment);

        st.executeUpdate();
    }

    public static void updateFolder(Connection conn, String tableForSave, String path, String folder) throws
            SQLException {
        PreparedStatement st = conn.prepareStatement("UPDATE " + tableForSave + " set folder=? where path =?");
        st.setString(1, folder);
        st.setString(2, path);
        st.executeUpdate();
    }
}
