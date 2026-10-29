package cn.edu.xmu;

import cn.edu.xmu.mapper.ConfigMapper;
import cn.edu.xmu.service.impl.ReportService;
import cn.edu.xmu.utils.PDFUtil;
import cn.edu.xmu.utils.PDFTableBuilder;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@SpringBootTest(properties = "spring.profiles.active=dev")
@Slf4j
public class PDFTest {
    @Autowired
    private ReportService reportService;
    @Autowired
    private ConfigMapper configMapper;

    @Test
    public void testPdf() throws Exception {
        reportService.generateReportPDFByConfig(configMapper.selectById(27));
    }

}
