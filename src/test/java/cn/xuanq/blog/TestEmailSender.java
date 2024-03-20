package cn.xuanq.blog;

import cn.xuanq.blog.utils.EmailSender;

import javax.mail.MessagingException;

public class TestEmailSender {

    public static void main(String[] args) throws MessagingException {
        EmailSender.subject("测试邮件发送")
                .from("inis系统")
                .text("这是发送的内容：lin")
                .to("3373876585@qq.com")
                .send();
    }
}