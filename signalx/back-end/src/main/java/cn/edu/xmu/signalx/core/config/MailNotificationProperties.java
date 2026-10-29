package cn.edu.xmu.signalx.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "notify.mail")
public class MailNotificationProperties {
    /** Enable/disable email notification. */
    private boolean enabled = false;

    /**
     * Default receiver when task owner has no email.
     * For demo/test you can set it to 359198768@qq.com.
     */
    private String defaultTo;

    /** Sender email address. Usually equals spring.mail.username. */
    private String from;

    /** Subject prefix for report email. */
    private String subjectPrefix = "SignalX 报告";
}
