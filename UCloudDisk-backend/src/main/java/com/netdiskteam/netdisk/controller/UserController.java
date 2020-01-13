package com.netdiskteam.netdisk.controller;

import com.netdiskteam.netdisk.repo.LastErrorRepo;
import com.netdiskteam.netdisk.service.FileService;
import com.netdiskteam.netdisk.service.UserService;
import com.netdiskteam.netdisk.utils.CommonValues;
import net.minidev.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Controller
@RequestMapping(value = "/interfaces/user")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private FileService fileService;

    @Autowired
    private LastErrorRepo lastErrorRepo;

    @ResponseBody
    @RequestMapping(value = "/get_login_state", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public String get_login_state(HttpServletRequest request, HttpSession session)
    {
        JSONObject result = new JSONObject();
        if (userService.checkLoginState(request, session) >= 0) {
            result.put("status", 0);
            result.put("result", 1);
            result.put("msg", "Already logged in.");
        } else {
            result.put("status", 0);
            result.put("result", 0);
            result.put("msg", "Not Logged in.");
        }
        return result.toJSONString();
    }

    @ResponseBody
    @RequestMapping(value = "/login", produces = "application/json;charset=UTF-8")
    public String login(HttpServletResponse response, HttpSession session, @RequestParam String username, @RequestParam String password) {
        Integer id = userService.checkPassword(username, password);
        JSONObject result = new JSONObject();
        if (id >= 0) {
            String ticket = userService.generateTicket(id);
            session.setAttribute("user_id", id);
            //session.setAttribute("ticket", ticket);
            Cookie cookie = new Cookie("user_id", id.toString());
            cookie.setPath("/");
            response.addCookie(cookie);
            cookie = new Cookie("ticket", ticket);
            cookie.setPath("/");
            response.addCookie(cookie);
            result.put("status", 0);
            result.put("msg", "Success");
            System.out.println("user " + id + " logged in " + DateTime.now() + "  user name = " + username);
        } else {
            session.removeAttribute("user_id");
            result.put("status", 1);
            result.put("error_code", lastErrorRepo.getErrorNumber());
            result.put("msg", lastErrorRepo.getLastError());
            System.out.println("user " + id + " failed to log in " + DateTime.now() + "  user name = " + username);
        }
        return result.toJSONString();
    }

    @ResponseBody
    @RequestMapping(value = "/reg", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String reg(HttpSession session, @RequestParam String username, @RequestParam String password) {
        JSONObject result = new JSONObject();
        if (userService.addUser(username, password)) {
            Integer id = userService.checkPassword(username, password);
            if (id >= 0 && fileService.createRootDirectory(id)) {
                result.put("status", 0);
                result.put("msg", "Success");
                System.out.println("user " + id + " registered " + DateTime.now() + "  user name = " + username);
            } else {
                result.put("status", 1);
                result.put("error_code", CommonValues.ERRORNO_INTERNALERROR);
                result.put("msg", CommonValues.ERRORMSG_INTERNALERROR);
            }
        }
        else {
            result.put("status", 1);
            result.put("error_code", lastErrorRepo.getErrorNumber());
            result.put("msg", lastErrorRepo.getLastError());
        }
        return result.toJSONString();
    }

    @ResponseBody
    @RequestMapping(value = "/logout", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String logout(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
        JSONObject result = new JSONObject();
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            String user_id = null, ticket = null;
            for (int i = 0; i < cookies.length; i++) {
                String cookieName = cookies[i].getName();
                if (cookieName.equals("ticket")) ticket = cookies[i].getValue();
                else if (cookieName.equals("user_id")) user_id = cookies[i].getValue();
                if (StringUtils.isNotBlank(cookieName) && (cookieName.equals("ticket") || cookieName.equals("user_id"))) {
                    Cookie cookie = new Cookie(cookies[i].getName(), "");
                    cookie.setMaxAge(0);
                    cookie.setPath(request.getContextPath());
                    cookie.setDomain(request.getServerName());
                    response.addCookie(cookie);
                }
            }
            session.removeAttribute("user_id");
            if (user_id != null && ticket != null) userService.removeTicket(Integer.parseInt(user_id), ticket);
        }
        result.put("status", 0);
        result.put("msg", "Success");
        return result.toJSONString();
    }

}
