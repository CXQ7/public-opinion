package cn.edu.xmu.signalx.ai.domain.request;

import lombok.Data;

@Data
public class AskReportRequest {
    private Long reportId;
    private String question;
}
