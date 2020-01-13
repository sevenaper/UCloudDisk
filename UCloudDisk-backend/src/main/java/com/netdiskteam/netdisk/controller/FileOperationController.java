package com.netdiskteam.netdisk.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.netdiskteam.netdisk.entity.FileOwners;
import com.netdiskteam.netdisk.repo.LastErrorRepo;
import com.netdiskteam.netdisk.service.FileService;
import com.netdiskteam.netdisk.service.UserService;
import com.netdiskteam.netdisk.utils.CommonValues;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

import static com.netdiskteam.netdisk.controller.ParamsChecker.*;

@Controller
@RequestMapping(value = "/interfaces/file")
public class FileOperationController {
    @Autowired
    private UserService userService;

    @Autowired
    private LastErrorRepo lastErrorRepo;

    @Autowired
    private FileService fileService;

    @ResponseBody
    @RequestMapping(value = "/create_dir", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String create_dir(@RequestParam String[] paths, HttpServletRequest request, HttpSession session)
    {
        Integer userID = userService.checkLoginState(request, session);
        JSONObject result = new JSONObject();
        boolean paramsOK = true;

        if (userID < 0) {
            result.put("status", 1);
            result.put("error_code", CommonValues.ERRORNO_NOTLOGIN);
            result.put("msg", CommonValues.ERRORMSG_NOTLOGIN);
            paramsOK = false;
        } else {
            for (String path : paths) {
                if (!checkFilePath(path)) {
                    result.put("status", 1);
                    result.put("error_code", CommonValues.ERRORNO_INVALIDPARAMETERS);
                    result.put("msg", CommonValues.ERRORMSG_INVALIDPARAMETERS);
                    result.put("invalid_parameter", "invalid file/path " + path);
                    paramsOK = false;
                    break;
                }
            }
        }
        if (paramsOK) {
            for (String path : paths) {
                if (!fileService.createDirectory(path, userID)) {
                    if (lastErrorRepo.getErrorNumber() == 2020) continue;  // 存在同名目录，忽略该错误
                    result.put("status", 1);
                    result.put("error_code", lastErrorRepo.getErrorNumber());
                    result.put("msg", lastErrorRepo.getLastError());
                    paramsOK = false;
                    break;
                }
            }
        }
        if (paramsOK) {
            result.put("status", 0);
            result.put("msg", "成功创建目录");
        }
        return result.toJSONString();
    }

    @ResponseBody
    @RequestMapping(value = "/move_file", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String move_file(@RequestParam String destination, @RequestParam String filename, @RequestParam String filepath, HttpServletRequest request, HttpSession session)
    {
        Integer userID = userService.checkLoginState(request, session);
        JSONObject result = new JSONObject();
        if (userID < 0) {
            result.put("status", 1);
            result.put("error_code", CommonValues.ERRORNO_NOTLOGIN);
            result.put("msg", CommonValues.ERRORMSG_NOTLOGIN);
        } else if (!checkFileName(filename) || !checkFilePath(filepath) || !checkFilePath(destination)) {
            result.put("status", 1);
            result.put("error_code", CommonValues.ERRORNO_INVALIDPARAMETERS);
            result.put("msg", CommonValues.ERRORMSG_INVALIDPARAMETERS);
            result.put("invalid_parameter", "invalid file/path");
        } else if (!fileService.moveFile(filepath, filename, userID, destination)) {
            result.put("status", 1);
            result.put("error_code", lastErrorRepo.getErrorNumber());
            result.put("msg", lastErrorRepo.getLastError());
        } else {
            result.put("status", 0);
            result.put("msg", "成功移动文件至 " + destination);
        }
        return result.toJSONString();
    }

    @ResponseBody
    @RequestMapping(value = "/rename_file", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String rename_file(@RequestParam String new_value, @RequestParam String filename, @RequestParam String filepath, HttpServletRequest request, HttpSession session)
    {
        Integer userID = userService.checkLoginState(request, session);
        JSONObject result = new JSONObject();
        if (userID < 0) {
            result.put("status", 1);
            result.put("error_code", CommonValues.ERRORNO_NOTLOGIN);
            result.put("msg", CommonValues.ERRORMSG_NOTLOGIN);
        } else if (!checkFileName(filename) || !checkFilePath(filepath) || !checkFileName(new_value)) {
            result.put("status", 1);
            result.put("error_code", CommonValues.ERRORNO_INVALIDPARAMETERS);
            result.put("msg", CommonValues.ERRORMSG_INVALIDPARAMETERS);
            result.put("invalid_parameter", "invalid file/path");
        } else if (!fileService.renameFile(filepath, filename, userID, new_value)) {
            result.put("status", 1);
            result.put("error_code", lastErrorRepo.getErrorNumber());
            result.put("msg", lastErrorRepo.getLastError());
        } else {
            result.put("status", 0);
            result.put("msg", "成功重命名文件为 " + new_value);
        }
        return result.toJSONString();
    }

    @ResponseBody
    @RequestMapping(value = "/recycle_file", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String recycle_file(@RequestParam String filename, @RequestParam String filepath, HttpServletRequest request, HttpSession session)
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
            result.put("invalid_parameter", "invalid file/path");
        } else if (!fileService.moveFileToRecycleBin(filepath, filename, userID)) {
            result.put("status", 1);
            result.put("error_code", lastErrorRepo.getErrorNumber());
            result.put("msg", lastErrorRepo.getLastError());
        } else {
            result.put("status", 0);
            result.put("msg", "成功");
        }
        return result.toJSONString();
    }

    @ResponseBody
    @RequestMapping(value = "/delete_file", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String delete_file(@RequestParam String filename, @RequestParam String filepath, HttpServletRequest request, HttpSession session)
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
            result.put("invalid_parameter", "invalid file/path");
        } else if (!fileService.deleteFile(filepath, filename, userID)) {
            result.put("status", 1);
            result.put("error_code", lastErrorRepo.getErrorNumber());
            result.put("msg", lastErrorRepo.getLastError());
        } else {
            result.put("status", 0);
            result.put("msg", "成功");
        }
        return result.toJSONString();
    }

    @ResponseBody
    @RequestMapping(value = "/restore_file", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String restore_file(@RequestParam String filename, @RequestParam String filepath, HttpServletRequest request, HttpSession session)
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
            result.put("invalid_parameter", "invalid file/path");
        } else if (!fileService.moveFileOutofRecycleBin(filepath, filename, userID)) {
            result.put("status", 1);
            result.put("error_code", lastErrorRepo.getErrorNumber());
            result.put("msg", lastErrorRepo.getLastError());
        } else {
            result.put("status", 0);
            result.put("msg", "成功");
        }
        return result.toJSONString();
    }

    @ResponseBody
    @RequestMapping(value = "/get_file_list", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public String get_file_list(HttpServletRequest request, HttpSession session)
    {
        Integer userID = userService.checkLoginState(request, session);
        JSONObject result = new JSONObject();
        if (userID < 0) {
            result.put("status", 1);
            result.put("error_code", CommonValues.ERRORNO_NOTLOGIN);
            result.put("msg", CommonValues.ERRORMSG_NOTLOGIN);
        } else {
            List<FileOwners> fileList = fileService.listFile(userID);
            if (fileList == null) {
                result.put("status", 1);
                result.put("error_code", CommonValues.ERRORNO_NORESULT);
                result.put("msg", CommonValues.ERRORMSG_NORESULT);
            } else {
                JSONArray jsonArray = new JSONArray();
                for (FileOwners fo : fileList) {
                    JSONObject jo = new JSONObject();
                    jo.put("file_size", fo.getFile_size());
                    jo.put("file_name", fo.getFile_name());
                    jo.put("file_path", fo.getFile_path());
                    jo.put("file_status", fo.getFile_status());
                    jo.put("file_hash", fo.getFile_hash());
                    jsonArray.add(jo);
                }
                result.put("status", 0);
                result.put("result", jsonArray);
                result.put("msg", "成功");
            }
        }
        return result.toJSONString();
    }

    /*            文件夹相关函数              */

    @ResponseBody
    @RequestMapping(value = "/move_dir", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String move_dir(@RequestParam String destination, @RequestParam String dirpath, HttpServletRequest request, HttpSession session)
    {
        Integer userID = userService.checkLoginState(request, session);
        JSONObject result = new JSONObject();
        if (userID < 0) {
            result.put("status", 1);
            result.put("error_code", CommonValues.ERRORNO_NOTLOGIN);
            result.put("msg", CommonValues.ERRORMSG_NOTLOGIN);
        } else if (!checkFilePath(dirpath) || !checkFilePath(destination) || "".equals(dirpath)) {
            result.put("status", 1);
            result.put("error_code", CommonValues.ERRORNO_INVALIDPARAMETERS);
            result.put("msg", CommonValues.ERRORMSG_INVALIDPARAMETERS);
            result.put("invalid_parameter", "invalid file/path");
        } else if (!fileService.moveDir(dirpath, userID, destination)) {
            result.put("status", 1);
            result.put("error_code", lastErrorRepo.getErrorNumber());
            result.put("msg", lastErrorRepo.getLastError());
        } else {
            result.put("status", 0);
            result.put("msg", "成功移动至 " + destination);
        }
        return result.toJSONString();
    }

    @ResponseBody
    @RequestMapping(value = "/rename_dir", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String rename_dir(@RequestParam String new_value, @RequestParam String dirpath, HttpServletRequest request, HttpSession session)
    {
        Integer userID = userService.checkLoginState(request, session);
        JSONObject result = new JSONObject();
        if (userID < 0) {
            result.put("status", 1);
            result.put("error_code", CommonValues.ERRORNO_NOTLOGIN);
            result.put("msg", CommonValues.ERRORMSG_NOTLOGIN);
        } else if (!checkFileName(new_value) || !checkFilePath(dirpath) || "".equals(dirpath)) {
            result.put("status", 1);
            result.put("error_code", CommonValues.ERRORNO_INVALIDPARAMETERS);
            result.put("msg", CommonValues.ERRORMSG_INVALIDPARAMETERS);
            result.put("invalid_parameter", "invalid file/path");
        } else if (!fileService.renameDir(dirpath, userID, new_value)) {
            result.put("status", 1);
            result.put("error_code", lastErrorRepo.getErrorNumber());
            result.put("msg", lastErrorRepo.getLastError());
        } else {
            result.put("status", 0);
            result.put("msg", "成功重命名为 " + new_value);
        }
        return result.toJSONString();
    }

    @ResponseBody
    @RequestMapping(value = "/recycle_dir", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String recycle_dir(@RequestParam String dirpath, HttpServletRequest request, HttpSession session)
    {
        Integer userID = userService.checkLoginState(request, session);
        JSONObject result = new JSONObject();
        if (userID < 0) {
            result.put("status", 1);
            result.put("error_code", CommonValues.ERRORNO_NOTLOGIN);
            result.put("msg", CommonValues.ERRORMSG_NOTLOGIN);
        } else if (!checkFilePath(dirpath) || "".equals(dirpath)) {
            result.put("status", 1);
            result.put("error_code", CommonValues.ERRORNO_INVALIDPARAMETERS);
            result.put("msg", CommonValues.ERRORMSG_INVALIDPARAMETERS);
            result.put("invalid_parameter", "invalid file/path");
        } else if (!fileService.moveDirToRecycleBin(dirpath, userID)) {
            result.put("status", 1);
            result.put("error_code", lastErrorRepo.getErrorNumber());
            result.put("msg", lastErrorRepo.getLastError());
        } else {
            result.put("status", 0);
            result.put("msg", "成功");
        }
        return result.toJSONString();
    }

    @ResponseBody
    @RequestMapping(value = "/delete_dir", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String delete_dir(@RequestParam String dirpath, HttpServletRequest request, HttpSession session)
    {
        Integer userID = userService.checkLoginState(request, session);
        JSONObject result = new JSONObject();
        if (userID < 0) {
            result.put("status", 1);
            result.put("error_code", CommonValues.ERRORNO_NOTLOGIN);
            result.put("msg", CommonValues.ERRORMSG_NOTLOGIN);
        } else if (!checkFilePath(dirpath) || "".equals(dirpath)) {
            result.put("status", 1);
            result.put("error_code", CommonValues.ERRORNO_INVALIDPARAMETERS);
            result.put("msg", CommonValues.ERRORMSG_INVALIDPARAMETERS);
            result.put("invalid_parameter", "invalid file/path");
        } else if (!fileService.deleteDir(dirpath, userID)) {
            result.put("status", 1);
            result.put("error_code", lastErrorRepo.getErrorNumber());
            result.put("msg", lastErrorRepo.getLastError());
        } else {
            result.put("status", 0);
            result.put("msg", "成功");
        }
        return result.toJSONString();
    }

    @ResponseBody
    @RequestMapping(value = "/restore_dir", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String restore_dir(@RequestParam String dirpath, HttpServletRequest request, HttpSession session)
    {
        Integer userID = userService.checkLoginState(request, session);
        JSONObject result = new JSONObject();
        if (userID < 0) {
            result.put("status", 1);
            result.put("error_code", CommonValues.ERRORNO_NOTLOGIN);
            result.put("msg", CommonValues.ERRORMSG_NOTLOGIN);
        } else if (!checkFilePath(dirpath) || "".equals(dirpath)) {
            result.put("status", 1);
            result.put("error_code", CommonValues.ERRORNO_INVALIDPARAMETERS);
            result.put("msg", CommonValues.ERRORMSG_INVALIDPARAMETERS);
            result.put("invalid_parameter", "invalid file/path");
        } else if (!fileService.moveDirOutofRecycleBin(dirpath, userID)) {
            result.put("status", 1);
            result.put("error_code", lastErrorRepo.getErrorNumber());
            result.put("msg", lastErrorRepo.getLastError());
        } else {
            result.put("status", 0);
            result.put("msg", "成功");
        }
        return result.toJSONString();
    }
}
