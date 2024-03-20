package cn.xuanq.blog.services.impl;

import cn.xuanq.blog.utils.EmailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

    @Async
    public void sendRegisterVerifyCode(String verifyCode, String emailAddress) throws Exception {
        EmailSender.sendRegisterVerifyCode(String.valueOf(verifyCode), emailAddress);
    }
}
