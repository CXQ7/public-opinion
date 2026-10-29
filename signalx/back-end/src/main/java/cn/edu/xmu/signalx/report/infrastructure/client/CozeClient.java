package cn.edu.xmu.signalx.report.infrastructure.client;

import java.util.List;
import java.util.Map;

public interface CozeClient {
    String submitTask(List<String> keywords, List<Map<String, Object>> newsList);
    CozeResult query(String runId);

    // 恢复接口内的Result VO定义
    class CozeResult {
        private final boolean completed;
        private final boolean failed;
        private final String markdown;

        private CozeResult(boolean completed, boolean failed, String markdown) {
            this.completed = completed;
            this.failed = failed;
            this.markdown = markdown;
        }

        public static CozeResult completed(String markdown) {
            return new CozeResult(true, false, markdown);
        }

        public static CozeResult failed() {
            return new CozeResult(false, true, "");
        }

        public static CozeResult running() {
            return new CozeResult(false, false, "");
        }

        public boolean isCompleted() {
            return completed;
        }

        public boolean isFailed() {
            return failed;
        }

        public String getMarkdown() {
            return markdown;
        }
    }
}
