package vv.photodb;

import java.util.Date;

class PhotoInfo {
    public String path;
    public String name;
    public String folder;
    public String ext;
    public Long size;
    public Date createDate;
    public String equipment;
    public String comment;
    public String md5;

    @Override
    public String toString() {
        return "Metadata{" +
                "path='" + path + '\'' +
                ", name='" + name + '\'' +
                ", folder='" + folder + '\'' +
                ", ext='" + ext + '\'' +
                ", size=" + size +
                ", createDate=" + (createDate!= null? Utils.formatter.format(createDate):createDate) +
                ", equipment='" + equipment + '\'' +
                ", comment='" + comment + '\'' +
                '}';
    }
}
