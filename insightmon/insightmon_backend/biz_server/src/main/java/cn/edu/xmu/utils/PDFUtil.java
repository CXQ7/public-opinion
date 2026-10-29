package cn.edu.xmu.utils;

import cn.edu.xmu.domain.po.Config;
import cn.edu.xmu.domain.po.Event;
import cn.edu.xmu.domain.po.Keyword;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PDFUtil {

    static final String LOGO_TEXT = "";

    /**
     * 给pdf 添加水印
     *
     * @param doc Document 对象
     * @throws Exception
     */
    public static void generateWatermark(Document doc) throws Exception {
//        PdfDocument pdfDoc = new PdfDocument(new PdfReader(fis), new PdfWriter(fos));
//        Document doc = new Document(pdfDoc);
//        PdfFont font = PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H");

        PdfDocument pdfDoc = doc.getPdfDocument();
        // 将字体包拖到路径下
        PdfFont pdfFont = getZhFont();
        Paragraph paragraph = new Paragraph(LOGO_TEXT).setFont(pdfFont).setFontSize(14);
        ImageData img = ImageDataFactory.create(PDFUtil.class.getClassLoader().getResource("static/logo.png"));
        float w = img.getWidth();
        float h = img.getHeight();
        PdfExtGState gs1 = new PdfExtGState().setFillOpacity(0.1f);
        for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
            PdfPage pdfPage = pdfDoc.getPage(i);
            Rectangle pageSize = pdfPage.getPageSizeWithRotation();
            pdfPage.setIgnorePageRotationForContent(true);
            PdfCanvas over = new PdfCanvas(pdfDoc.getPage(i));
            over.saveState();
            over.setExtGState(gs1);
            float x = pageSize.getX();
            float y = pageSize.getY();
            float pageWidth = pageSize.getWidth();
            float pageHeight = pageSize.getHeight();
            while (y < pageHeight) {
                if (y == 0) y = y + 10;
                while (x < pageWidth) {
//                    if(x==0) x = x+48;
                    if (x == 0) x = x + 68;
                    over.addImageWithTransformationMatrix(img, w / 2, 0, 0, h / 2, x, y, false);
//                    doc.showTextAligned(paragraph, x+32, y+38, i, TextAlignment.LEFT, VerticalAlignment.TOP, 0.3f);
                    x = x + 180;
                }
                x = pageSize.getX();
                y = y + 120;
            }
            over.restoreState();
        }
//        doc.close();
    }

    /**
     * 获取中文字体
     *
     * @return
     * @throws IOException
     */
    public static PdfFont getZhFont() throws IOException {
        return PdfFontFactory.createFont(PDFUtil.class.getClassLoader().getResource("./siyuanheiti.otf").toString(),
                PdfEncodings.IDENTITY_H);
    }


    /**
     * 获得一个 PDF table cell
     *
     * @param txt text
     * @param v   最大宽度百分比
     * @return
     */
    public static Cell newPdfCell(String txt, float v) {
        return new Cell().add(
                        new Paragraph(txt).setTextAlignment(TextAlignment.CENTER))
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setMaxWidth(UnitValue.createPercentValue(v)
                );
    }

    /**
     * 新建 pdfDoc
     *
     * @param pdfPath  pdfPath
     * @param fileName 文件名
     * @return
     * @throws FileNotFoundException
     */
    public static Document newPdfDoc(String pdfPath, String fileName) throws FileNotFoundException {
        String filePath = pdfPath + fileName;
        // pdf加密
        // PdfWriter writer = new PdfWriter(filePath, new WriterProperties().setStandardEncryption(null, "Hz123456".getBytes(), EncryptionConstants.ALLOW_SCREENREADERS,
        //         EncryptionConstants.ENCRYPTION_AES_128));
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        pdf.setDefaultPageSize(PageSize.A4);
        return new Document(pdf);
    }

    public static String generateReportPdf(Config config, Map<Keyword, List<Event>> reportData) throws Exception{
        String filename = config.getUserId().toString() + "_" + config.getName() + "_"
                + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".pdf";
        Document document = PDFUtil.newPdfDoc("./pdf/", filename);
        // 这里的字体对象一个pdf用一次，不能重复(多个pdf)使用，否则会报错
        PdfFont pdfFont = PDFUtil.getZhFont();
        Div head1Div = new Div().setWidth(UnitValue.createPercentValue(100))
                .setHeight(UnitValue.createPercentValue(100))
                .setHorizontalAlignment(HorizontalAlignment.CENTER);
        // 标题，22号字体，居中对齐，加粗
        Paragraph title = new Paragraph("Public Eye report for daily news")
                .setFont(pdfFont).setFontSize(22).setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setTextAlignment(TextAlignment.CENTER).setBold();
        head1Div.add(title);
        Paragraph description = new Paragraph("检索关键词： " + config.getSearchKey() + "  |   事件日期： " + LocalDate.now().minusDays(1))
                .setFont(pdfFont).setFontSize(14);
        head1Div.add(description);

        List<String> header = Arrays.asList("描述", "热度", "情感值", "事件时间", "相关新闻数");
                AtomicInteger id = new AtomicInteger(1);
                if (reportData == null || reportData.isEmpty()) {
                                System.out.println("[PDFUtil] reportData is empty or null - no tables will be generated");
                                // 在 PDF 中加入一条无数据提示，避免只看到标题
                                Paragraph empty = new Paragraph("(No data available for the selected period or search key)")
                                                .setFont(pdfFont).setFontSize(12).setTextAlignment(TextAlignment.CENTER);
                                head1Div.add(empty);
                } else {
                        System.out.println("[PDFUtil] reportData size=" + reportData.size());
                        reportData.forEach((k, v) -> System.out.println("[PDFUtil] keyword='" + (k == null ? "null" : k.getKeyword()) + "' events=" + (v == null ? 0 : v.size())));
                }
        reportData.forEach((keyword, eventList) -> {
            head1Div.add(
                    new Paragraph(id + ". " + keyword.getKeyword())
                            .setFont(pdfFont).setFontSize(16).setBold()
            );
            List<List<String>> data = new ArrayList<>();
            for (Event event : eventList) {
//                String urls = event.getUrls();
//                int first_space_index = urls.indexOf(" ");
//                first_space_index = first_space_index == -1 ? urls.length() : first_space_index;
                data.add(Arrays.asList(event.getDescription(),
                        Long.toString(Math.round(event.getHeat())),
                        String.format("%.2f", event.getSentiment() * 100) +
                                " (" + (event.getSentiment() > 0.6 ? "正面"
                                : event.getSentiment() >= 0.4 ? "中性"
                                : "负面") + ")",
                        event.getEventDatetime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        Integer.toString(event.getNewsIds().split(" ").length)
//                        event.getUrls().substring(0, first_space_index)
                ));
            }
            id.incrementAndGet();

            // 表格，8列，字体为pdfFont
            Table table = PDFTableBuilder.builder(5, pdfFont)
                    .addRow(header).addRows(data).build();
            head1Div.add(table);
        });

        document.add(head1Div);
        document.close();
        return filename;
    }
}
