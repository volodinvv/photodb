package vv.photodb;

import java.util.Date;

class Metadata {
    Date createDate;
    String equipment;
    String comment;

    @Override
    public String toString() {
        return "Metadata{" +
                "createDate=" + Utils.formatter.format(createDate) +
                ", equipment='" + equipment + '\'' +
                ", comment='" + comment + '\'' +
                '}';
    }
}
