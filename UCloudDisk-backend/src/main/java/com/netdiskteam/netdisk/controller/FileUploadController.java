package com.netdiskteam.netdisk.controller;

import com.netdiskteam.netdisk.repo.LastErrorRepo;
import com.netdiskteam.netdisk.service.FileService;
import com.netdiskteam.netdisk.service.UserService;
import com.netdiskteam.netdisk.utils.CommonValues;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import static com.netdiskteam.netdisk.controller.ParamsChecker.*;

/** FileUploadController 提供文件上传接口
 *
 * 文件接口限制：
 * 目录名、文件名不能包含以下字符：< > / \ | : " * ?
 * 大小写不敏感，一个目录的路径名不能超过 256 个字符，一个文件的文件名不能超过 256 个字符
 * 目录以路径名表示，用 / 表示层级（注意不是 Windows 下的 \  后端程序不保证使用 \ 也能通过参数检查）
 * 文件名不能是 . 或者 ..
 * 这些限制与 Windows 的限制类似
 *
 * 所有用户都有根目录 /     根目录不能被移动、删除、重命名
 *
 * 在数据库中，目录和文件存储于同一个表中，表中包含字段 path 和 name
 * 为了区分，目录的 name 始终为 . 目录名则保存在 path 中
 * 例如：path = dir1/dir2, name = . 表示这是一个目录，目录名为 dir2，dir2 在目录 dir1 下
 *       显然，表中也存在 path = dir1, name = . 的行
 *
 * 根目录在数据库中表示为 path = "" (空字符串), name = .
 *
 * 这些信息有助于前端解析文件结构，后端不负责生成文件树
 *
 * @author star
 */

@Controller
@RequestMapping(value = "/interfaces/file")
public class FileUploadController {
    @Autowired
    private UserService userService;

    @Autowired
    private LastErrorRepo lastErrorRepo;

    @Autowired
    private FileService fileService;

    private static final int MAX_FILE_POS = 1024*1024*1024; // 最大1G文件写入指针

    @ResponseBody
    @RequestMapping(value = "/upload_file_begin", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String upload_file_begin(@RequestParam String hash, @RequestParam Long size, HttpServletRequest request, HttpSession session) {
        Integer userID = userService.checkLoginState(request, session);
        JSONObject result = new JSONObject();
        if (userID < 0) {
            result.put("status", 1);
            result.put("error_code", CommonValues.ERRORNO_NOTLOGIN);
            result.put("msg", CommonValues.ERRORMSG_NOTLOGIN);
        } else if (!Pattern.matches("[a-zA-Z0-9]{32}", hash)) {
            result.put("status", 1);
            result.put("error_code", CommonValues.ERRORNO_INVALIDPARAMETERS);
            result.put("msg", CommonValues.ERRORMSG_INVALIDPARAMETERS);
            result.put("invalid_parameter", "hash");
        } else if (size < 0 || size > MAX_FILE_POS) {
            result.put("status", 1);
            result.put("error_code", CommonValues.ERRORNO_INVALIDPARAMETERS);
            result.put("msg", CommonValues.ERRORMSG_INVALIDPARAMETERS);
            result.put("invalid_parameter", "size");
        } else if (fileService.findFileHash(hash, size)) {
            result.put("status", 0);
            result.put("result", 1);
            result.put("msg", "文件已经存在于数据库中，可以直接添加文件");
        } else if (fileService.beginFileTransmission(userID, hash, size) != 1){
            result.put("status", 1);
            result.put("error_code", lastErrorRepo.getErrorNumber());
            result.put("msg", lastErrorRepo.getLastError());
        } else {
            result.put("status", 0);
            result.put("result", 0);
            result.put("msg", "可以开始上传文件了");
        }
        return result.toJSONString();
    }

    @ResponseBody
    @RequestMapping(value = "/upload_file_data", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String upload_file_data(@RequestParam String hash, @RequestParam MultipartFile data, @RequestParam Long pos, HttpServletRequest request, HttpSession session)
    {
        Integer userID = userService.checkLoginState(request, session);
        JSONObject result = new JSONObject();
        byte[] bytes;
        try {
            bytes = data.getBytes();
        } catch (IOException e) {
            result.put("status", 1);
            result.put("error_code", CommonValues.ERRORNO_INTERNALERROR);
            result.put("msg", CommonValues.ERRORMSG_INTERNALERROR);
            return result.toJSONString();
        }
        if (userID < 0) {
            result.put("status", 1);
            result.put("error_code", CommonValues.ERRORNO_NOTLOGIN);
            result.put("msg", CommonValues.ERRORMSG_NOTLOGIN);
        } else if (!Pattern.matches("[a-zA-Z0-9]{32}", hash)) {
            result.put("status", 1);
            result.put("error_code", CommonValues.ERRORNO_INVALIDPARAMETERS);
            result.put("msg", CommonValues.ERRORMSG_INVALIDPARAMETERS);
            result.put("invalid_parameter", "hash");
        } else if (pos >= MAX_FILE_POS) {
            result.put("status", 1);
            result.put("error_code", CommonValues.ERRORNO_INVALIDPARAMETERS);
            result.put("msg", CommonValues.ERRORMSG_INVALIDPARAMETERS);
            result.put("invalid_parameter", "pos");
        } else if (!fileService.writeFile(userID, hash, bytes, pos)) {
            result.put("status", 1);
            result.put("error_code", lastErrorRepo.getErrorNumber());
            result.put("msg", lastErrorRepo.getLastError());
        } else {
            result.put("status", 0);
            result.put("msg", "成功写入数据");
        }
        return result.toJSONString();
    }

    @ResponseBody
    @RequestMapping(value = "/upload_file_end", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String upload_file_end(@RequestParam String hash, HttpServletRequest request, HttpSession session)
    {
        Integer userID = userService.checkLoginState(request, session);
        JSONObject result = new JSONObject();
        if (userID < 0) {
            result.put("status", 1);
            result.put("error_code", CommonValues.ERRORNO_NOTLOGIN);
            result.put("msg", CommonValues.ERRORMSG_NOTLOGIN);
        } else if (!Pattern.matches("[a-zA-Z0-9]{32}", hash)) {
            result.put("status", 1);
            result.put("error_code", CommonValues.ERRORNO_INVALIDPARAMETERS);
            result.put("msg", CommonValues.ERRORMSG_INVALIDPARAMETERS);
            result.put("invalid_parameter", "hash");
        } else if (!fileService.finishFileTransmission(userID, hash)) {
            result.put("status", 1);
            result.put("error_code", lastErrorRepo.getErrorNumber());
            result.put("msg", lastErrorRepo.getLastError());
        } else {
            result.put("status", 0);
            result.put("msg", "成功完成文件传输");
        }
        return result.toJSONString();
    }

    @ResponseBody
    @RequestMapping(value = "/add_file", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String add_file(@RequestParam String hash, @RequestParam Long size, @RequestParam String filename, @RequestParam String filepath, HttpServletRequest request, HttpSession session)
    {
        Integer userID = userService.checkLoginState(request, session);
        JSONObject result = new JSONObject();
        if (userID < 0) {
            result.put("status", 1);
            result.put("error_code", CommonValues.ERRORNO_NOTLOGIN);
            result.put("msg", CommonValues.ERRORMSG_NOTLOGIN);
        } else if (!checkFileName(filename) || !checkFilePath(filepath)) {
            result.put("status", 1);
            result.put("error_code", CommonValues.ERRORNO_INVALIDPARAMETERS);
            result.put("msg", CommonValues.ERRORMSG_INVALIDPARAMETERS);
            result.put("invalid_parameter", "invalid file/path, filename = " + filename + ", filepath = " + filepath);
        } else if (size < 0 || size > MAX_FILE_POS) {
            result.put("status", 1);
            result.put("error_code", CommonValues.ERRORNO_INVALIDPARAMETERS);
            result.put("msg", CommonValues.ERRORMSG_INVALIDPARAMETERS);
            result.put("invalid_parameter", "size");
        } else if (!Pattern.matches("[a-zA-Z0-9]{32}", hash)) {
            result.put("status", 1);
            result.put("error_code", CommonValues.ERRORNO_INVALIDPARAMETERS);
            result.put("msg", CommonValues.ERRORMSG_INVALIDPARAMETERS);
            result.put("invalid_parameter", "hash");
        } else if (!fileService.addFile(hash, size, userID, filename, filepath)) {
            result.put("status", 1);
            result.put("error_code", lastErrorRepo.getErrorNumber());
            result.put("msg", lastErrorRepo.getLastError());
        } else {
            result.put("status", 0);
            result.put("msg", "成功建立文件/用户关联, filename = " + filename + ", filepath = " + filepath);
        }
        return result.toJSONString();
    }
}
