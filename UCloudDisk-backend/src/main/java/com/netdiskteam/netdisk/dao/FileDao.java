package com.netdiskteam.netdisk.dao;

import com.netdiskteam.netdisk.entity.Files;
import org.apache.ibatis.annotations.*;

@Mapper
public interface FileDao {

    @Insert({"insert into files(file_location, file_hash, file_size, reference_count) values (#{file_location},#{file_hash},#{file_size},#{reference_count})"})
    void insertFile(Files file);

    @Select({"select file_location,file_hash,file_size,reference_count from files where file_hash=#{file_hash} and file_size=#{file_size}"})
    Files selectByHashAndSize(@Param("file_hash") String file_hash, @Param("file_size") long file_size);

    @Update({"update files set reference_count=reference_count+1 where file_hash=#{file_hash} and file_size=#{file_size}"})
    void incrementReferenceCount(@Param("file_hash") String file_hash, @Param("file_size") long file_size);

    @Update({"update files set reference_count=reference_count-1 where file_hash=#{file_hash} and file_size=#{file_size}"})
    void decrementReferenceCount(@Param("file_hash") String file_hash, @Param("file_size") long file_size);
}
