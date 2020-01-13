package com.netdiskteam.netdisk.dao;

import com.netdiskteam.netdisk.entity.FileOwners;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FileOwnersDao {

    @Insert({"insert into file_owners(file_hash, file_size, user_id, file_status, file_name, file_path) values (#{file_hash},#{file_size},#{user_id},#{file_status},#{file_name},#{file_path})"})
    void insertFile(FileOwners file);

    @Select({"select file_hash,file_size,user_id,file_status,file_name,file_path from file_owners where user_id=#{user_id}"})
    List<FileOwners> selectByUserID(@Param("user_id") int user_id);

    @Select({"select file_hash,file_size,user_id,file_status,file_name,file_path from file_owners where file_path=#{file_path} and file_name=#{file_name} and user_id=#{user_id}"})
    FileOwners selectByFilepath(@Param("file_path") String file_path, @Param("file_name") String file_name, @Param("user_id") int user_id);

    @Update({"update file_owners set file_status=#{file_status} where file_path=#{file_path} and file_name=#{file_name} and user_id=#{user_id}"})
    void updateFileStatus(@Param("file_path") String file_path, @Param("file_name") String file_name, @Param("user_id") int user_id, @Param("file_status") int file_status);

    @Update({"update file_owners set file_name=#{newfilename} where file_path=#{file_path} and file_name=#{file_name} and user_id=#{user_id}"})
    void updateFilename(@Param("file_path") String file_path, @Param("file_name") String file_name, @Param("user_id") int user_id, @Param("newfilename") String newfilename);

    @Update({"update file_owners set file_path=#{newfilepath} where file_path=#{file_path} and file_name=#{file_name} and user_id=#{user_id}"})
    void updateFilepath(@Param("file_path") String file_path, @Param("file_name") String file_name, @Param("user_id") int user_id, @Param("newfilepath") String newfilepath);

    @Delete({"delete from file_owners where file_path=#{file_path} and file_name=#{file_name} and user_id=#{user_id}"})
    void deleteFile(@Param("file_path") String file_path, @Param("file_name") String file_name, @Param("user_id") int user_id);

    @Select({"select file_hash,file_size,user_id,file_status,file_name,file_path from file_owners where file_path like CONCAT(#{file_path},'/%') and user_id=#{user_id}"})
    List<FileOwners> selectByDir(@Param("file_path") String file_path, @Param("user_id") int user_id);

}
