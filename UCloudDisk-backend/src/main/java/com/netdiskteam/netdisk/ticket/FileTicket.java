package com.netdiskteam.netdisk.ticket;

import org.joda.time.DateTime;

public class FileTicket {
    private Integer userID;
    private Long fileSize;
    private DateTime expirationDate;

    public FileTicket(Integer userID, Long fileSize, DateTime expirationDate) {
        this.userID = userID;
        this.fileSize = fileSize;
        this.expirationDate = expirationDate;
    }

    public Integer getUserID() {
        return userID;
    }

    public void setUserID(Integer userID) {
        this.userID = userID;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public DateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(DateTime expirationDate) {
        this.expirationDate = expirationDate;
    }
}
