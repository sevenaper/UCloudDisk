package com.netdiskteam.netdisk.service;

import com.netdiskteam.netdisk.repo.LastErrorRepo;
import com.netdiskteam.netdisk.dao.UserDao;
import com.netdiskteam.netdisk.entity.User;
import com.netdiskteam.netdisk.ticket.UserTickets;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.regex.Pattern;

@Service
public class UserService {
    @Autowired
    private UserDao userDao;

    @Autowired
    public LastErrorRepo lastErrorRepo;

    private static final int errorNumberBase = 1000;

    /** 判断Session是否已经登陆
     *
     * @author star
     * @param request 客户端请求
     * @param session 会话
     * @return 如果已经登陆，userID，否则返回-1
     */
    public Integer checkLoginState(HttpServletRequest request, HttpSession session) {
        String user_id = null, ticket = null;
        int result = -1;
        Cookie[] cookies = request.getCookies();
        if (cookies != null && session.getAttribute("user_id") != null) {
            for (Cookie cookie : cookies) {
                if (StringUtils.equals(cookie.getName(), "user_id")) user_id = cookie.getValue();
                if (StringUtils.equals(cookie.getName(), "ticket")) ticket = cookie.getValue();
            }
            if (StringUtils.equals(user_id, session.getAttribute("user_id").toString())) {
                if (user_id != null && checkLoginTicket(Integer.parseInt(user_id), ticket)) {
                    result = Integer.parseInt(user_id);
                }
            }
        }
        return result;
    }

    /** 判断Ticket是否有效
     *
     * @author star
     * @param userID Session中保存的userID
     * @param ticket 请求/Cookie中附带的ticket信息
     * @return 如果有效，返回true
     */
    public boolean checkLoginTicket(Integer userID, String ticket) {
        return StringUtils.equals(UserTickets.getTicket(userID), ticket);
    }

    /** 检查Username是否有效
     *
     * @author star
     * @param username reg请求中附带的username
     * @return 如果有效，返回true
     */
    public boolean isValidUsername(String username) {
        if (!Pattern.matches("[a-zA-Z][a-zA-Z0-9_]{1,15}", username)) {
            lastErrorRepo.setErrorNumber(errorNumberBase + 1);
            lastErrorRepo.setLastError("用户名只能由字母、数字、下划线组成，开头必须是字母，不能超过16位");
            return false;
        }
        if (userDao.selectByName(username) != null) {
            lastErrorRepo.setErrorNumber(errorNumberBase + 2);
            lastErrorRepo.setLastError("用户名已存在");
            return false;
        }
        return true;
    }

    /** 向数据库添加用户信息
     *
     * @author star
     * @param username reg请求中附带的username
     * @return 如果成功，返回true
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean addUser(String username, String password) {
        if (!isValidUsername(username)) return false;
        User user = new User();
        user.setUsername(username);
        user.setPassword(DigestUtils.md5DigestAsHex(password.getBytes()));
        user.setRole("user");
        userDao.insertUser(user);
        return true;
    }

    /** 生成ticket并存进tickets
     *
     * @author star
     * @param userID 用户ID，必须已经通过密码校验！
     * @return 返回ticket
     */
    public String generateTicket(Integer userID) {
        return UserTickets.addTicket(userID);
    }

    /** 删除一个ticket
     *
     * @author star
     * @param user_id 用户ID
     * @param ticket ticket字串
     */
    public void removeTicket(Integer user_id, String ticket) {
        UserTickets.removeTicket(user_id, ticket);
    }

    /** 判断密码是否正确
     *
     * @author star
     * @param username 用户名
     * @param password 密码
     * @return 如果密码正确，返回用户ID，否则返回-1
     */
    public Integer checkPassword(String username, String password) {
        User user = userDao.selectByName(username);
        if (user == null) {
            lastErrorRepo.setErrorNumber(errorNumberBase + 3);
            lastErrorRepo.setLastError("用户不存在");
            return -1;
        }
        if (StringUtils.equals(user.getPassword(), DigestUtils.md5DigestAsHex(password.getBytes())))
        {
            return user.getId();
        }
        lastErrorRepo.setErrorNumber(errorNumberBase + 4);
        lastErrorRepo.setLastError("密码错误");
        return -1;
    }

}

