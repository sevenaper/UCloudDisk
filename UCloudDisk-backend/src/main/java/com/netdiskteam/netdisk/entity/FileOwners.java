package com.netdiskteam.netdisk.entity;

public class FileOwners {
    public String getFile_hash() {
        return file_hash;
    }

    public void setFile_hash(String file_hash) {
        this.file_hash = file_hash;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public int getFile_status() {
        return file_status;
    }

    public void setFile_status(int file_status) {
        this.file_status = file_status;
    }

    public String getFile_name() {
        return file_name;
    }

    public void setFile_name(String file_name) {
        this.file_name = file_name;
    }

    public String getFile_path() {
        return file_path;
    }

    public void setFile_path(String file_path) {
        this.file_path = file_path;
    }

    public Long getFile_size() {
        return file_size;
    }

    public void setFile_size(Long file_size) {
        this.file_size = file_size;
    }

    private String file_hash;
    private int user_id;
    private int file_status;
    private String file_name;
    private String file_path;
    private long file_size;

    public FileOwners() {

    }

    public FileOwners(String hash, Long filesize, Integer userID, String filename, String filepath, int filestatus) {
        file_hash = hash;
        user_id = userID;
        file_name = filename;
        file_path = filepath;
        file_status = filestatus;
        file_size = filesize;
    }
}
