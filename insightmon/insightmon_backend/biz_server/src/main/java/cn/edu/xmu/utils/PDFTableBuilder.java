package cn.edu.xmu.utils;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import lombok.Data;

import java.util.List;

@Data
public class PDFTableBuilder {
    /**
     * pdf表格对象
     */
    Table table;
    /**
     * 表格列数
     */
    Integer column;
    /**
     * 每列宽度百分比
     */
    Float columnWidthPercent;

    /**
     * 创建表格
     *
     * @param column  列数
     * @param pdfFont 字体
     * @return
     */
    public static PDFTableBuilder builder(int column, PdfFont pdfFont) {
        PDFTableBuilder PDFTableBuilder = new PDFTableBuilder();
        PDFTableBuilder.setColumn(column);
        PDFTableBuilder.setColumnWidthPercent(100f / column);
        float[] values = new float[column];
        for (int i = 0; i < column; i++) {
            values[i] = 3;
        }
        Table table = new Table(UnitValue.createPercentArray(values))
                // 这里是设置表格占整个pdf的宽度百分比，这里设置的是100%
                .setWidth(UnitValue.createPercentValue(100))
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                // 设置字体
                .setFont(pdfFont)
                // 设置字体大小
                .setFontSize(10);
        PDFTableBuilder.setTable(table);
        return PDFTableBuilder;
    }

    // 这里添加数据时，需要注意，每一行的数据个数必须与表格列数一致，单元格无数据时，添加空字符串
    /**
     * 添加一行数据
     * @param row 一行数据
     * @return
     */
    public PDFTableBuilder addRow(List<String> row) {
        for (String item : row) {
            this.table.addCell(PDFUtil.newPdfCell(item, this.columnWidthPercent));
        }
        return this;
    }

    /**
     * 添加多行数据
     * @param rowList 多行数据
     * @return
     */
    public PDFTableBuilder addRows(List<List<String>> rowList) {
        for (List<String> row : rowList) {
            this.addRow(row);
        }
        return this;
    }

    /**
     * 获取表格对象
     * @return
     */
    public Table build() {
        return this.table;
    }

}
