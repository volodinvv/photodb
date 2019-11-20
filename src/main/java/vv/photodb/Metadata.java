package vv.photodb;

import java.util.Date;

class Metadata {
    Metadata(String equipment) {
        this.equipment = equipment;
    }

    Date createDate;
    String equipment;
}
