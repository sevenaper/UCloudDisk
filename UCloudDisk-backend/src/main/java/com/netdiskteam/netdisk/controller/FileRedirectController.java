package com.netdiskteam.netdisk.controller;

import com.netdiskteam.netdisk.repo.LastErrorRepo;
import com.netdiskteam.netdisk.service.FileService;
import com.netdiskteam.netdisk.service.UserService;
import com.netdiskteam.netdisk.utils.Pair;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.netdiskteam.netdisk.controller.ParamsChecker.*;

@Controller
@RequestMapping(value = "/interfaces")
public class FileRedirectController {
    @Autowired
    private UserService userService;

    @Autowired
    private LastErrorRepo lastErrorRepo;

    @Autowired
    private FileService fileService;

    @RequestMapping(value = "/my_files/{file_path}/**")
    @ResponseBody
    public void get_file(@PathVariable String file_path, HttpServletRequest request, HttpSession session, HttpServletResponse response) throws IOException
    {
        final String path =
                request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString();
        final String bestMatchingPattern =
                request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).toString();
        String arguments = new AntPathMatcher().extractPathWithinPattern(bestMatchingPattern, path);
        if (null != arguments && !arguments.isEmpty()) {
            file_path = file_path + '/' + arguments;
        }
        System.out.println(file_path);

        String range = request.getHeader("Range");
        file_path = "/" + file_path;
        Integer userID = userService.checkLoginState(request, session);
        response.reset();
        if (userID < 0) {
            response.setStatus(response.SC_UNAUTHORIZED);
            response.sendError(401);
        } else if (!checkFileFullPath(file_path)) {
            response.setStatus(response.SC_FORBIDDEN);
            response.sendError(403);
        } else {
            Pair<String, String> pair = fileService.parseFullPath(file_path);
            long fileLength = fileService.getFileSize(userID, pair.first, pair.second);
            if (fileLength == -1) {
                response.setStatus(response.SC_FORBIDDEN);
                response.sendError(403);
            } else {
                long startByte = 0;
                long endByte = fileLength - 1;
                if (range != null && range.contains("bytes=") && range.contains("-")) {
                    range = range.substring(range.lastIndexOf("=") + 1).trim();
                    String ranges[] = range.split("-");
                    try {
                        if (ranges.length == 1) {
                            // bytes=-最后几个字节
                            if (range.startsWith("-")) {
                                endByte = fileLength - 1;
                                startByte = fileLength - Long.parseLong(ranges[0]);
                            }
                            // bytes=第几个字节开始-
                            else if (range.endsWith("-")) {
                                startByte = Long.parseLong(ranges[0]);
                            }
                        }
                        // bytes=开始-结束
                        else if (ranges.length == 2) {
                            startByte = Long.parseLong(ranges[0]);
                            endByte = Long.parseLong(ranges[1]);
                        }
                    } catch (NumberFormatException e) {
                        startByte = 0;
                        endByte = fileLength - 1;
                    }
                }
                if (startByte > endByte) {
                    response.setStatus(response.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                    return;
                }
                long contentLength = endByte - startByte + 1;
                //文件名
                String fileName = pair.second;
                //文件类型
                String contentType = request.getServletContext().getMimeType(fileName);
                response.setHeader("Accept-Ranges", "bytes");
                if (range != null) response.setStatus(response.SC_PARTIAL_CONTENT);
                else response.setStatus(response.SC_OK);
                String inlineOrAttachment = "attachment";
                if (contentType == null) contentType = "application/octet-stream";
                else if (contentType.startsWith("audio") || contentType.startsWith("text") || contentType.startsWith("image") ||
                        contentType.startsWith("video") || contentType.equals("application/pdf")) inlineOrAttachment = "inline";
                response.setContentType(contentType);
                String encName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
                encName = encName.replace("+", "%20");
                response.setHeader("Content-Disposition", inlineOrAttachment + ";filename=\"" + encName + "\";filename*=UTF-8''" + encName);
                response.setHeader("Content-Length", String.valueOf(contentLength));
                response.setHeader("Content-Range", "bytes " + startByte + "-" + endByte + "/" + fileLength);
                System.out.println("user " + userID + " transfer file " + DateTime.now() + "  file path = " + file_path + "  file size = " + fileLength);
                OutputStream outputStream = response.getOutputStream();
                fileService.getFileData(outputStream, userID, pair.first, pair.second, startByte, contentLength);
                outputStream.close();
            }
        }
    }
}
