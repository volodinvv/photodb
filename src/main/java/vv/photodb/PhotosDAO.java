package vv.photodb;

import java.sql.*;

public class PhotosDAO implements AutoCloseable {

    private Connection conn;
    private String tableForSave;

    public PhotosDAO() throws SQLException {
        //return DriverManager.getConnection("jdbc:sqlite:D:/Private/PhotoDB/db/photodb");
        conn = DriverManager.getConnection("jdbc:sqlite:D:/Private/PhotoDB/home/photodb");
        tableForSave = PhotoDB.args.table;
    }

    public Connection getConnection() throws SQLException {
        return conn;
    }

    public boolean isSaved(String path) throws SQLException {
        try (ResultSet resultSet = conn.createStatement().executeQuery("select * from " + tableForSave + " where path='" + path.replace("'", "''") + "'")) {
            return resultSet.next();
        }
    }


    public void save(PhotoInfo photoInfo) throws
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

    public void updateFolder(String path, String folder) throws
            SQLException {
        PreparedStatement st = conn.prepareStatement("UPDATE " + tableForSave + " set folder=? where path =?");
        st.setString(1, folder);
        st.setString(2, path);
        st.executeUpdate();
    }

    @Override
    public void close() throws Exception {
        conn.close();
    }
}
