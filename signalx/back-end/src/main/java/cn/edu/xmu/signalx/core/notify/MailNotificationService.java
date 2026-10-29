package cn.edu.xmu.signalx.core.notify;

import cn.edu.xmu.signalx.core.config.MailNotificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MailNotificationService {

    private final JavaMailSender mailSender;
    private final MailNotificationProperties properties;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public void sendReportCompleted(String to, String reportName, Long taskId, Long reportId, String markdown) {
        if (!properties.isEnabled()) {
            log.info("notify.mail.enabled=false; skip email sending for report [{}]", reportId);
            return;
        }

        if (to == null || to.isBlank()) {
            log.warn("notify.mail is enabled but receiver email is blank; skip email sending for report [{}]", reportId);
            return;
        }

        String subject = String.format("%s - %s (taskId=%s, reportId=%s)",
                properties.getSubjectPrefix(),
                reportName == null ? "Report" : reportName,
                taskId,
                reportId);

        String content = buildPlainTextBody(reportName, taskId, reportId, markdown);

        SimpleMailMessage message = new SimpleMailMessage();
        String from = properties.getFrom();
        if (from == null || from.isBlank()) {
            from = mailUsername;
        }
        if (from != null && !from.isBlank()) {
            message.setFrom(from);
        }
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);

        mailSender.send(message);
        log.info("Report email sent to {} for report [{}]", to, reportId);
    }

    private String buildPlainTextBody(String reportName, Long taskId, Long reportId, String markdown) {
        StringBuilder sb = new StringBuilder();
        sb.append("SignalX 报告推送\n");
        sb.append("Report: ").append(reportName == null ? "" : reportName).append("\n");
        sb.append("taskId: ").append(taskId).append("\n");
        sb.append("reportId: ").append(reportId).append("\n\n");

        if (markdown == null || markdown.isBlank()) {
            sb.append("(empty content)\n");
            return sb.toString();
        }

        // avoid super long content in a demo email
        int limit = 8000;
        if (markdown.length() > limit) {
            sb.append(markdown, 0, limit).append("\n\n...(truncated)\n");
        } else {
            sb.append(markdown);
        }
        return sb.toString();
    }
}
