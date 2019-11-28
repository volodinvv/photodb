package vv.photodb;

import java.sql.*;
import java.util.ArrayList;
import java.util.function.Consumer;

public class PhotosDAO implements AutoCloseable {

    private Connection conn;
    public String tableForSave;

    public PhotosDAO() throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite:D:/Private/PhotoDB/db/photodb");
        //conn = DriverManager.getConnection("jdbc:sqlite:D:/Private/PhotoDB/home/photodb");
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


    public void save(PhotoInfo photoInfo) {
        try {
            PreparedStatement st = conn.prepareStatement("INSERT INTO " + tableForSave +
                    " (path, name, folder, ext, size, created, md5, equipment, comment) " +
                    "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(path) DO update set created = ?, md5 = ?, equipment = ?, comment = ?");
            st.setString(1, photoInfo.path);
            st.setString(2, photoInfo.name);
            st.setString(3, photoInfo.folder);
            st.setString(4, photoInfo.ext);
            st.setLong(5, photoInfo.size);
            st.setString(6, photoInfo.createDate == null ? null : Utils.formatter.format(photoInfo.createDate));
            st.setString(7, photoInfo.md5);
            st.setString(8, photoInfo.equipment);
            st.setString(9, photoInfo.comment);
            st.setString(10, photoInfo.createDate == null ? null : Utils.formatter.format(photoInfo.createDate));
            st.setString(11, photoInfo.md5);
            st.setString(12, photoInfo.equipment);
            st.setString(13, photoInfo.comment);

            st.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateFolder(String path, String folder) {
        try {
            PreparedStatement st = conn.prepareStatement("UPDATE " + tableForSave + " set folder=? where path =?");
            st.setString(1, folder);
            st.setString(2, path);
            st.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Can't update folder " + path + " -> " + folder + " " + e.getMessage(), e);
        }
    }

    public void list(String filter, Consumer<? super ResultSet> action) throws SQLException {
        ResultSet resultSet = getConnection().createStatement().executeQuery("select * from " + tableForSave +
                " " + filter);
        while (resultSet.next()) {
            action.accept(resultSet);
        }
    }

    @Override
    public void close() throws Exception {
        conn.close();
    }

    public String getString(ResultSet resultSet, String name) {
        try {
            return resultSet.getString(name);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String escape(String source) {
        return source.replace("\'", "\'\'");
    }
}
