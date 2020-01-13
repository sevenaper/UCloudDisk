package com.netdiskteam.netdisk.dao;

import com.netdiskteam.netdisk.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserDao {

    @Insert({"insert into user(username, password, role) values (#{username},#{password},#{role})"})
    void insertUser(User user);

    @Select({"select id,username,password,role from user where id=#{id}"})
    User selectByID(@Param("id") int id);

    @Select({"select id,username,password,role from user where username=#{username}"})
    User selectByName(@Param("username") String username);

}
