package cn.xuanq.blog.utils;

public interface Constants {

    interface User {
        String ROLE_ADMIN = "role_admin";
        String ROLE_NORMAL = "role_normal";
        String DEFAULT_AVATAR = "https://images.sunofbeaches.com/content/2022_05_21/977732823475552256.png";
        String DEFAULT_STATE = "1";
        String COOKIE_TOKEN_KEY = "inis_blog_token";
        // redis的key
        String KEY_CAPTCHA_CONTENT = "key_captcha_content_";
        String KEY_EMAIL_CODE_CONTENT = "key_email_code_content_";
        String KEY_EMAIL_SEND_IP = "key_email_send_ip_";
        String KEY_EMAIL_SEND_ADDRESS = "key_email_send_address_";
        String KEY_TOKEN = "key_token_";
    }

    interface ImageType {
        String PREFIX = "image/";
        String TYPE_JPG = "jpg";
        String TYPE_PNG = "png";
        String TYPE_GIF = "gif";
        String TYPE_JPG_WITH_PREFIX = PREFIX + "jpeg";
        String TYPE_JPEG_WITH_PREFIX = PREFIX + "jpeg";
        String TYPE_PNG_WITH_PREFIX = PREFIX + "png";
        String TYPE_GIF_WITH_PREFIX = PREFIX + "gif";
    }

    interface Settings {
        String MANGER_ACCOUNT_INIT_STATE = "manger_account_init_state";
        String WEB_SITE_TITLE = "web_site_title";
        String WEB_SITE_DESCRIPTION = "web_site_description";
        String WEB_SITE_KEYWORDS = "web_site_keywords";
        String WEB_SITE_VIEW_COUNT = "web_site_view_count";
    }

    interface Page {
        int DEFAULT_PAGE = 1;
        int DEFAULT_SIZE = 1;
    }

    /**
     * 单位是毫秒
     */
    interface TimeValueMillions {
        long MIN = 60 * 1000;
        long HOUR = 60 * MIN;
        long HOUR_2 = 2 * 60 * MIN;
        long DAY = 24 * HOUR;
        long WEEK = 7 * DAY;
        long MONTH = 30 * DAY;
    }

    /**
     * 单位是秒
     */
    interface TimeValueInSecond {
        int MIN = 60;
        int MIN_15 = 60 * 15;
        int HOUR = 60 * MIN;
        int HOUR_2 = 2 * 60 * MIN;
        int DAY = 24 * HOUR;
        int WEEK = 7 * DAY;
        int MONTH = 30 * DAY;
    }

    interface Article {
        int TITLE_MAX_LENGTH = 128;
        int SUMMARY_MAX_LENGTH = 256;
        // 0表示删除、1表示已经发布、2表示草稿、3表示置顶
        String STATE_DELETE = "0";
        String STATE_PUBLISH = "1";
        String STATE_DRAFT = "2";
        String STATE_TOP = "3";
        String KEY_ARTICLE_LIST_FIRST_PAGE = "key_article_list_first_page";
    }

    interface Comment {
        // 1表示发表中、3表示置顶中
        String STATE_PUBLISH = "1";
        String STATE_TOP = "3";
        String KEY_COMMENT_FIRST_PAGE_CACHE = "key_comment_first_page_cache_";
    }

}
