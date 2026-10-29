package cn.edu.xmu.signalx.ai.domain.request;

import lombok.Data;

@Data
public class AskEventRequest {
    private Long eventId;
    private String question;
}
