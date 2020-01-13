package com.netdiskteam.netdisk.entity;

public class Files {
    public int getReference_count() {
        return reference_count;
    }

    public void setReference_count(int reference_count) {
        this.reference_count = reference_count;
    }

    public String getFile_location() {
        return file_location;
    }

    public void setFile_location(String file_location) {
        this.file_location = file_location;
    }

    public String getFile_hash() {
        return file_hash;
    }

    public void setFile_hash(String file_hash) {
        this.file_hash = file_hash;
    }

    public long getFile_size() {
        return file_size;
    }

    public void setFile_size(long file_size) {
        this.file_size = file_size;
    }

    private int reference_count;
    private String file_location;
    private String file_hash;

    private long file_size;

}

