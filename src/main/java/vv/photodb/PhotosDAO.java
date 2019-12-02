package vv.photodb;

import java.nio.file.Path;
import java.sql.*;
import java.text.ParseException;
import java.util.Date;
import java.util.function.Consumer;

public class PhotosDAO implements AutoCloseable {

    private Connection conn;
    public String tableForSave;

    public PhotosDAO() throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite:D:/Private/PhotoDB/db/photodb");
        //conn = DriverManager.getConnection("jdbc:sqlite:D:/Private/PhotoDB/home/photodb");
        tableForSave = PhotoDB.args.table;
    }

    public Connection getConnection() {
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
                    "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(path) DO " +
                    "update set created = ?, md5 = ?, equipment = ?, comment = ?, comment = ?, destination = ?");
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
            st.setString(14, photoInfo.folder);
            st.setString(15, photoInfo.destination);

            st.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void list(String filter, Consumer<? super PhotoInfo> action) throws SQLException {
        ResultSet resultSet = getConnection().createStatement().executeQuery("select * from " + tableForSave +
                " " + filter);
        while (resultSet.next()) {
            action.accept(getPhotoInfo(resultSet));
        }
    }

    public void listForCopy(Consumer<? super PhotoInfo> action) throws SQLException {
        String sql = "select path, name, ext, size, created as createDate, COALESCE (alias,equipment) equipment, md5, comment, folder, destination " +
                "from photos_for_copy p left join equipments e using(equipment) " +
                "where destination is null";
        ResultSet resultSet = getConnection().createStatement().executeQuery(sql);
        while (resultSet.next()) {
            action.accept(getPhotoInfo(resultSet));
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

    public Long getLong(ResultSet resultSet, String name) {
        try {
            return resultSet.getLong(name);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String escape(String source) {
        return source.replace("\'", "\'\'");
    }

    public PhotoInfo getPhotoInfo(ResultSet resultSet) {
        PhotoInfo photoInfo = new PhotoInfo();
        photoInfo.path = getString(resultSet, "path");
        photoInfo.name = getString(resultSet, "name");
        photoInfo.ext = getString(resultSet, "ext");
        photoInfo.equipment = getString(resultSet, "equipment");
        photoInfo.folder = getString(resultSet, "folder");
        photoInfo.createDate = getDate(resultSet, "createDate");
        photoInfo.comment = getString(resultSet, "comment");
        photoInfo.md5 = getString(resultSet, "md5");
        photoInfo.size = getLong(resultSet, "size");
        photoInfo.destination = getString(resultSet, "destination");
        return photoInfo;
    }

    private Date getDate(ResultSet resultSet, String name) {
        String str = getString(resultSet, name);
        try {
            return str == null ? null : Utils.formatter.parse(str);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateDestination(String path, Path destFile) throws SQLException {
        getConnection().createStatement().executeUpdate("update photos set destination='" + escape(destFile.toString()) + "' where path='" + escape(path) + "'");

    }
}
