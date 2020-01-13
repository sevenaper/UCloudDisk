package com.netdiskteam.netdisk.service;

import com.netdiskteam.netdisk.dao.FileOwnersDao;
import com.netdiskteam.netdisk.entity.FileOwners;
import com.netdiskteam.netdisk.ticket.FileTicket;
import com.netdiskteam.netdisk.utils.CommonValues;
import com.netdiskteam.netdisk.repo.LastErrorRepo;
import com.netdiskteam.netdisk.dao.FileDao;
import com.netdiskteam.netdisk.entity.Files;
import com.netdiskteam.netdisk.utils.Pair;
import org.apache.commons.lang.StringUtils;
import org.apache.tomcat.util.buf.HexUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.*;
import java.io.*;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

@Service
public class FileService {

    private static final int errorNumberBase = 2000;

    // 文件存储目录，测试环境/服务器环境
    //private static final String location = "E:\\test";
    private static final String location = "/var/netdisk";

    private static final String temp_location = location + "/temp/";
    private static final String upload_location = location + "/upload_files/";

    private static final int expiration_time_in_minute = 1;

    @Autowired
    private LastErrorRepo lastErrorRepo;

    @Autowired
    private FileDao fileDao;

    @Autowired
    private FileOwnersDao fileOwnersDao;

    private final HashMap<String, FileTicket> files = new HashMap<>();

    /** 开始一个文件的传输
     *
     * @author star
     * @param userID  用户ID，必须已经登陆
     * @param hash 文件hash，hash不存在于数据库中
     * @param size 文件大小
     * @return 如果发生错误，返回-1；传输过程成功建立，返回1
     */
    public int beginFileTransmission(Integer userID, String hash, Long size) {
        synchronized (files) {
            if (files.containsKey(hash)) {
                if (files.get(hash).getExpirationDate().isBeforeNow()) {
                    files.remove(hash);
                } else {
                    lastErrorRepo.setErrorNumber(errorNumberBase + 1);
                    lastErrorRepo.setLastError("MD5相同的文件正在上传中（可能不是该用户）");
                    return -1;
                }
            }
            files.put(hash, new FileTicket(userID, size, DateTime.now().plusMinutes(expiration_time_in_minute)));
        }
        // 在本地磁盘创建文件
        try {
            File file = new File(temp_location + hash + "." + size);
            if (file.exists()) {
                // 文件存在可能是因为某次传输失败/中断，
                // 应当以数据库记录为依据判断，故删除该文件
                if (!file.delete()) {
                    lastErrorRepo.setErrorNumber(CommonValues.ERRORNO_INTERNALERROR);
                    lastErrorRepo.setLastError(CommonValues.ERRORMSG_INTERNALERROR);
                    return -1;
                }
            }
            if (!file.createNewFile()) {
                lastErrorRepo.setErrorNumber(CommonValues.ERRORNO_INTERNALERROR);
                lastErrorRepo.setLastError(CommonValues.ERRORMSG_INTERNALERROR);
                return -1;
            }
        }
        catch (IOException exception) {
            lastErrorRepo.setErrorNumber(CommonValues.ERRORNO_INTERNALERROR);
            lastErrorRepo.setLastError(CommonValues.ERRORMSG_INTERNALERROR);
            return -1;
        }
        return 1;
    }

    /** 写文件
     *
     * @author star
     * @param userID  用户ID，必须已经登陆
     * @param hash 文件hash
     * @param data 文件数据
     * @param writePos 写入位置
     * @return 如果成功，返回true
     */
    public boolean writeFile(Integer userID, String hash, byte[] data, long writePos) {
        long filesize;
        synchronized (files) {
            if (!files.containsKey(hash) || !userID.equals(files.get(hash).getUserID())) {
                lastErrorRepo.setErrorNumber(errorNumberBase + 3);
                lastErrorRepo.setLastError("待上传文件的记录不存在！");
                return false;
            }
            files.get(hash).setExpirationDate(DateTime.now().plusMinutes(expiration_time_in_minute));
            filesize = files.get(hash).getFileSize();
        }
        if (filesize == 0 || writePos + data.length > filesize) {
            lastErrorRepo.setErrorNumber(errorNumberBase + 205);
            lastErrorRepo.setLastError("写入指针越界, writePos = " + writePos + ", data.length = " + data.length + ", filesize = " + filesize);
            return false;
        }
        File file = new File(temp_location + hash + "." + filesize);
        RandomAccessFile raf = null;
        boolean ret = true;
        try {
            raf = new RandomAccessFile(file, "rw");
            raf.seek(writePos);
            raf.write(data);
        }
        catch (IOException e) {
            lastErrorRepo.setErrorNumber(CommonValues.ERRORNO_INTERNALERROR);
            lastErrorRepo.setLastError(CommonValues.ERRORMSG_INTERNALERROR);
            ret = false;
        }
        finally {
            try {
                if (raf != null) raf.close();
            } catch (IOException e) {
                lastErrorRepo.setErrorNumber(CommonValues.ERRORNO_INTERNALERROR);
                lastErrorRepo.setLastError(CommonValues.ERRORMSG_INTERNALERROR);
                ret = false;
            }
        }
        return ret;
    }

    /** 完成一个文件的传输
     *
     * @author star
     * @param userID  用户ID，必须已经登陆
     * @param hash 文件hash
     * @return 如果成功，返回true
     */
    public boolean finishFileTransmission(Integer userID, String hash) {
        long filesize;
        synchronized (files) {
            if (!files.containsKey(hash) || !userID.equals(files.get(hash).getUserID())) {
                CommonValues.DoNothing();
                lastErrorRepo.setErrorNumber(errorNumberBase + 3);
                lastErrorRepo.setLastError("待上传文件的记录不存在！");
                return false;
            }
            files.get(hash).setExpirationDate(DateTime.now().plusMinutes(expiration_time_in_minute));
            filesize = files.get(hash).getFileSize();
        }
        boolean ret = true;
        File file = new File(temp_location + hash + "." + filesize);
        RandomAccessFile in = null, out = null;
        MD5.reset();
        try {
            if (!file.exists()) {
                lastErrorRepo.setErrorNumber(CommonValues.ERRORNO_INTERNALERROR);
                lastErrorRepo.setLastError(CommonValues.ERRORMSG_INTERNALERROR);
                return false;
            }
            if (file.length() != filesize) {
                lastErrorRepo.setErrorNumber(errorNumberBase + 102);
                lastErrorRepo.setLastError("文件大小与预期的不一致，请确定文件指针正确");
                return false;
            }
            in = new RandomAccessFile(file, "r");
            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) != -1) {
                MD5.update(buffer, 0, length);
            }
            String md5 = HexUtils.toHexString(MD5.digest());
            if (!StringUtils.equals(md5, hash)) {
                lastErrorRepo.setErrorNumber(errorNumberBase + 100);
                lastErrorRepo.setLastError("哈希值不一致，请重新上传文件数据");
                return false;
            }
            // 复制临时文件到存储目录，因为 writeFile 函数重入可能导致文件内容发生变化
            File fileoutput = new File(upload_location +  hash + "." + filesize);
            if (fileoutput.exists()) {
                if (!fileoutput.delete()) {
                    lastErrorRepo.setErrorNumber(CommonValues.ERRORNO_INTERNALERROR);
                    lastErrorRepo.setLastError(CommonValues.ERRORMSG_INTERNALERROR);
                    return false;
                }
            }
            out = new RandomAccessFile(fileoutput, "rw");
            out.setLength(filesize);
            in.seek(0);
            out.seek(0);
            FileChannel inputChannel = in.getChannel();
            FileChannel outputChannel = out.getChannel();
            long traferredBytes = outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
            inputChannel.close();
            outputChannel.close();
            if (traferredBytes != filesize) {
                lastErrorRepo.setErrorNumber(CommonValues.ERRORNO_INTERNALERROR);
                lastErrorRepo.setLastError(CommonValues.ERRORMSG_INTERNALERROR);
                return false;
            }
        } catch (IOException e) {
            lastErrorRepo.setErrorNumber(CommonValues.ERRORNO_INTERNALERROR);
            lastErrorRepo.setLastError(CommonValues.ERRORMSG_INTERNALERROR);
            return false;
        }
        finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (!file.delete()) {
                    lastErrorRepo.setErrorNumber(CommonValues.ERRORNO_INTERNALERROR);
                    lastErrorRepo.setLastError(CommonValues.ERRORMSG_INTERNALERROR + ", cannot delete temporary file");
                    ret = false;
                }
            } catch (IOException e) {
                lastErrorRepo.setErrorNumber(CommonValues.ERRORNO_INTERNALERROR);
                lastErrorRepo.setLastError(CommonValues.ERRORMSG_INTERNALERROR);
                ret = false;
            }
        }
        if (ret) {
            Files newFiles = new Files();
            newFiles.setFile_hash(hash);
            newFiles.setFile_location(upload_location +  hash + "." + filesize);
            newFiles.setReference_count(0);
            newFiles.setFile_size(filesize);
            fileDao.insertFile(newFiles);
            synchronized (files) {
                files.remove(hash);
            }
        }
        return ret;
    }

    /** 判断文件hash是否已经存在于数据库中
     *
     * @author star
     * @param hash 文件hash
     * @return 如果是，返回true
     */
    public boolean findFileHash(String hash, Long size) {
        Files files = fileDao.selectByHashAndSize(hash, size);
        return files != null;
    }

    /* ------------------------------------------------------------------------------------- */
    /* 文件上传函数结束，以下为文件/文件夹操作函数
    /* ------------------------------------------------------------------------------------- */

    /** 获取文件大小
     *
     * @author star
     * @param userID 用户ID
     * @param filepath 文件路径，不能超过256字节
     * @param filename 文件名称，不能超过256字节
     * @return 返回 Long 类型，-1表示出错，>=0表示文件大小
     */
    public Long getFileSize(Integer userID, String filepath, String filename) {
        FileOwners f = checkFileExistence(filepath, filename, userID);
        if (f == null) return -1L;
        return f.getFile_size();
    }

    /** 获取指定区间的文件数据
     *
     * @author star
     * @param userID 用户ID
     * @param filepath 文件路径，不能超过256字节
     * @param filename 文件名称，不能超过256字节
     * @param offset 起始偏移量
     * @param size 请求的数据大小
     * @return 成功，返回 true
     */
    public boolean getFileData(OutputStream outputStream, Integer userID, String filepath, String filename, long offset, long size) {
        FileOwners f = checkFileExistence(filepath, filename, userID);
        if (f == null) return false;
        long filesize = f.getFile_size();
        if (offset >= filesize || size > filesize || offset + size > filesize || size == 0) {
            lastErrorRepo.setErrorNumber(errorNumberBase + 304);
            lastErrorRepo.setLastError("文件偏移量或块大小错误");
            return false;
        }
        String hash = f.getFile_hash();
        Files file = fileDao.selectByHashAndSize(hash, filesize);
        if (file == null) {
            lastErrorRepo.setErrorNumber(CommonValues.ERRORNO_INTERNALERROR);
            lastErrorRepo.setLastError(CommonValues.ERRORMSG_INTERNALERROR);
            return false;
        }
        boolean ret = true;
        String location = file.getFile_location();
        RandomAccessFile in = null;
        long transmitted = 0;
        byte[] buffer = new byte[1024 * 1024];
        try {
            in = new RandomAccessFile(location, "r");
            in.seek(offset);
            int len = 0;
            while ((transmitted + len) <= size && (len = in.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
                transmitted += len;
            }
            if (transmitted < size) {
                len = in.read(buffer, 0, (int)(size - transmitted));
                outputStream.write(buffer, 0, len);
                transmitted += len;
            }
        } catch (IOException e){
            lastErrorRepo.setErrorNumber(CommonValues.ERRORNO_INTERNALERROR);
            lastErrorRepo.setLastError(CommonValues.ERRORMSG_INTERNALERROR);
            ret = false;
        } finally {
            try {
                if (in != null) in.close();
            }
            catch (IOException e){
                CommonValues.DoNothing();
                lastErrorRepo.setErrorNumber(CommonValues.ERRORNO_INTERNALERROR);
                lastErrorRepo.setLastError(CommonValues.ERRORMSG_INTERNALERROR);
                ret = false;
            }
        }
        return ret;
    }

    /** 添加用户和文件关联
     *
     * @author star
     * @param hash 文件hash
     * @param userID 用户ID
     * @param filename 文件名称，不能超过256字节
     * @param filepath 文件路径，不能超过256字节
     * @return 如果成功，返回true
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean addFile(String hash, Long size, Integer userID, String filename, String filepath) {
        Files files = fileDao.selectByHashAndSize(hash, size);
        if (files == null) {
            lastErrorRepo.setErrorNumber(errorNumberBase + 4);
            lastErrorRepo.setLastError("该文件没有上传！");
            return false;
        }
        if (checkDirectoryExistence(filepath, userID) == null) return false;
        if (!checkPathAvailable(filepath, filename, userID)) return false;

        fileDao.incrementReferenceCount(hash, size);
        FileOwners fileOwners = new FileOwners(hash, size, userID, filename, filepath, 0);
        fileOwnersDao.insertFile(fileOwners);
        return true;
    }

    /** 创建文件夹
     *
     * @author star
     * @return 如果成功，返回true
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean createDirectory(String path, Integer userID) {
        if (!checkPathAvailable(path, userID)) {
            return false;
        }
        FileOwners fileOwners = new FileOwners("DIR", 0L, userID, ".", path, 0);
        fileOwnersDao.insertFile(fileOwners);
        return true;
    }

    /** 创建根目录
     *
     * @author star
     * @return 如果成功，返回true
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean createRootDirectory(Integer userID) {
        if (checkDirectoryExistence("", userID) != null) {
            return false;
        }
        FileOwners fileOwners = new FileOwners("DIR", 0L, userID, ".", "", 0);
        fileOwnersDao.insertFile(fileOwners);
        return true;
    }

    /** 移动文件
     *
     * @author star
     * @return 如果成功，返回true
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean moveFile(String filepath, String filename, Integer userID, String newpath) {
        if (checkFileExistence(filepath, filename, userID) == null) {
            return false;
        }
        if (checkDirectoryExistence(newpath, userID) == null) {
            return false;
        }
        if (!checkPathAvailable(newpath, filename, userID)) {
            return false;
        }
        fileOwnersDao.updateFilepath(filepath, filename, userID, newpath);
        return true;
    }

    /** 重命名文件
     *
     * @author star
     * @return 如果成功，返回true
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean renameFile(String filepath, String filename, Integer userID, String newname) {
        if (checkFileExistence(filepath, filename, userID) == null) {
            return false;
        }
        if (!checkPathAvailable(filepath, newname, userID)) {
            return false;
        }
        fileOwnersDao.updateFilepath(filepath, filename, userID, newname);
        return true;
    }

    /** 移动文件至回收站
     *
     * @author star
     * @return 如果成功，返回true
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean moveFileToRecycleBin(String filepath, String filename, Integer userID) {
        if (checkFileExistence(filepath, filename, userID) == null) {
            return false;
        }
        fileOwnersDao.updateFileStatus(filepath, filename, userID, 2);
        return true;
    }

    /** 从回收站移除文件
     *
     * @author star
     * @return 如果成功，返回true
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean moveFileOutofRecycleBin(String filepath, String filename, Integer userID) {
        if (checkFileExistence(filepath, filename, userID) == null) {
            return false;
        }
        fileOwnersDao.updateFileStatus(filepath, filename, userID, 0);
        return true;
    }

    /** 彻底删除文件
     *
     * @author star
     * @return 如果成功，返回true
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean deleteFile(String filepath, String filename, Integer userID) {
        FileOwners fileOwners = checkFileExistence(filepath, filename, userID);
        if (fileOwners == null) {
            return false;
        }
        fileOwnersDao.deleteFile(filepath, filename, userID);
        fileDao.decrementReferenceCount(fileOwners.getFile_hash(), fileOwners.getFile_size());
        return true;
    }

    /** 移动文件夹至回收站（包括子文件和子目录）
     *
     * @author star
     * @return 如果成功，返回true
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean moveDirToRecycleBin(String dirpath, Integer userID) {
        if (checkDirectoryExistence(dirpath, userID) == null) {
            return false;
        }
        List<FileOwners> list = fileOwnersDao.selectByDir(dirpath, userID);
        if (list != null) {
            for (FileOwners fo : list) {
                fileOwnersDao.updateFileStatus(fo.getFile_path(), fo.getFile_name(), userID, 2);
            }
        }
        return true;
    }

    /** 从回收站移除文件夹（包括子文件和子目录）
     *
     * @author star
     * @return 如果成功，返回true
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean moveDirOutofRecycleBin(String dirpath, Integer userID) {
        if (checkDirectoryExistence(dirpath, userID) == null) {
            return false;
        }
        List<FileOwners> list = fileOwnersDao.selectByDir(dirpath, userID);
        if (list != null) {
            for (FileOwners fo : list) {
                fileOwnersDao.updateFileStatus(fo.getFile_path(), fo.getFile_name(), userID, 0);
            }
        }
        return true;
    }

    /** 彻底删除文件夹（递归模式）
     *
     * @author star
     * @return 如果成功，返回true
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean deleteDir(String dirpath, Integer userID) {
        if (checkDirectoryExistence(dirpath, userID) == null) {
            return false;
        }
        List<FileOwners> list = fileOwnersDao.selectByDir(dirpath, userID);
        if (list != null) {
            for(FileOwners fo : list) {
                fileOwnersDao.deleteFile(fo.getFile_path(), fo.getFile_name(), userID);
                if (!StringUtils.equals(fo.getFile_name(), ".")) {
                    fileDao.decrementReferenceCount(fo.getFile_hash(), fo.getFile_size());
                }
            }
        }
        return true;
    }

    /** 移动目录
     *
     * @param dirpath 原目录路径
     * @param newpath 新目录路径，即 dirpath ---> newpath/dirpath
     * @author star
     * @return 如果成功，返回true
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean moveDir(String dirpath, Integer userID, String newpath) {
        if (checkDirectoryExistence(dirpath, userID) == null) {
            return false;
        }
        if (checkDirectoryExistence(newpath, userID) == null) {
            return false;
        }

        // 取出目录名
        String dir_name = StringUtils.substring(dirpath, StringUtils.lastIndexOf(dirpath, '/') + 1);
        String dirname = newpath + '/' + dir_name;
        int p_dir_len = dirpath.length();  // 需删除的前导路径长度，不删除 /

        // 先检查是否存在同名情况
        if (!checkPathAvailable(dirname, userID)) {
            return false;
        }

        List<FileOwners> list = fileOwnersDao.selectByDir(dirpath, userID);

/* 如果要实现 merge 使用以下代码判断
        if (list != null) {
            for(FileOwners fo : list) {
                String newfilepath = dirname + fo.getFile_path().substring(p_dir_len);
                if (!checkPathAvailable(newfilepath, fo.getFile_name(), userID)) {
                    return false;
                }
            }
        }
*/
        // 执行操作
        if (list != null) {
            for (FileOwners fo : list) {
                String newfilepath = dirname + fo.getFile_path().substring(p_dir_len);
                fileOwnersDao.updateFilepath(fo.getFile_path(), fo.getFile_name(), userID, newfilepath);
            }
        }
        return true;
    }

    /** 重命名目录
     *
     * @author star
     * @return 如果成功，返回true
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean renameDir(String dirpath, Integer userID, String newname) {
        if (checkDirectoryExistence(dirpath, userID) == null) {
            return false;
        }

        // 获得 dirpath 的父目录
        String parent_dir_name = StringUtils.substring(dirpath, 0, StringUtils.lastIndexOf(dirpath, '/'));
        int p_dir_len = dirpath.length();  // 需删除的前导路径长度，不删除 /
        String dirname = parent_dir_name + '/' + newname;

        if (!checkPathAvailable(parent_dir_name, newname, userID)) {
            return false;
        }

        List<FileOwners> list = fileOwnersDao.selectByDir(dirpath, userID);

        // 执行操作
        if (list != null) {
            for (FileOwners fo : list) {
                String newfilepath = dirname + fo.getFile_path().substring(p_dir_len);
                fileOwnersDao.updateFilepath(fo.getFile_path(), fo.getFile_name(), userID, newfilepath);
            }
        }
        return true;
    }

    /** 判断目录是否存在
     *
     * @author star
     * @return 如果存在，返回 FileOwners 对象
     */
    private FileOwners checkDirectoryExistence(String dirpath, Integer userID) {
        FileOwners fileOwners = fileOwnersDao.selectByFilepath(dirpath, ".", userID);
        if (fileOwners == null) {
            lastErrorRepo.setErrorNumber(errorNumberBase + 8);
            lastErrorRepo.setLastError("指定的目录路径不存在！dir path = " + dirpath);
            return null;
        }
        return fileOwners;
    }

    /** 判断文件是否存在
     *
     * @author star
     * @return 如果存在，返回 FileOwners 对象
     */
    private FileOwners checkFileExistence(String filepath, String filename, Integer userID) {
        FileOwners fileOwners = fileOwnersDao.selectByFilepath(filepath, filename, userID);
        if (fileOwners == null) {
            lastErrorRepo.setErrorNumber(errorNumberBase + 6);
            lastErrorRepo.setLastError("指定的文件不存在！filepath = " + filepath + " filename = " + filename);
            return null;
        }
        return fileOwners;
    }

    /** 判断某完整路径（包含文件名）是否已有文件/文件夹
     *
     * @author star
     * @return 如果路径可用，返回 true；已经存在文件/文件夹，返回 false
     */
    private boolean checkPathAvailable(String filepath, String filename, Integer userID) {
        FileOwners fileOwners = fileOwnersDao.selectByFilepath(filepath, filename, userID);
        if (fileOwners != null) {
            lastErrorRepo.setErrorNumber(errorNumberBase + 18);
            lastErrorRepo.setLastError("指定路径已经有同名文件！filepath = " + filepath + " filename = " + filename);
            return false;
        }
        fileOwners = fileOwnersDao.selectByFilepath(filepath + "/" + filename, ".", userID);
        if (fileOwners != null) {
            lastErrorRepo.setErrorNumber(errorNumberBase + 20);
            lastErrorRepo.setLastError("指定路径已经有同名目录！path = " + filepath + "/" + filename);
            return false;
        }
        return true;
    }

    private boolean checkPathAvailable(String fullpath, Integer userID) {
        Pair<String, String> pair = parseFullPath(fullpath);
        return checkPathAvailable(pair.first, pair.second, userID);
    }

    public Pair<String, String> parseFullPath(String fullpath) {
        int index = StringUtils.lastIndexOf(fullpath, '/');
        // 获得目录名
        String dir_name = StringUtils.substring(fullpath, 0, index);
        // 取出文件名
        String file_name = StringUtils.substring(fullpath, index + 1);
        return new Pair<>(dir_name, file_name);
    }

    /** 获取文件列表
     *
     * @author star
     * @return 返回文件列表，可能为null
     */
    public List<FileOwners> listFile(Integer userID) {
        return fileOwnersDao.selectByUserID(userID);
    }


    /* ------------------------------------------------------------------------------------- */
    /* 静态属性和方法
    /* ------------------------------------------------------------------------------------- */

    private static MessageDigest MD5 = null;
    static{
        try{
            MD5 = MessageDigest.getInstance("MD5");
        }catch(NoSuchAlgorithmException e){
            e.printStackTrace();
        }
    }
}
