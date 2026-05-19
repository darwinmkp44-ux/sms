package com.disparasms.app.data.repository

import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.xssf.eventusermodel.XSSFReader
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler
import org.apache.poi.xssf.usermodel.XSSFComment
import org.xml.sax.InputSource
import java.io.InputStream
import javax.xml.parsers.SAXParserFactory

class StreamingExcelReader(
    private val stream: InputStream,
    private val rowCallback: (row: List<String>, rowNum: Int) -> Unit
) {
    fun read() {
        val opc = OPCPackage.open(stream)
        try {
            val reader = XSSFReader(opc)
            val sharedStringsTable = reader.sharedStringsTable
            val stylesTable = reader.stylesTable

            val sheetStream = reader.getSheetsData().next()
            val parserFactory = SAXParserFactory.newInstance()
            val xmlReader = parserFactory.newSAXParser().xmlReader

            val sheetHandler = XSSFSheetXMLHandler(
                stylesTable,
                sharedStringsTable,
                object : XSSFSheetXMLHandler.SheetContentsHandler {
                    private var currentRow = mutableListOf<String>()

                    override fun startRow(rowNum: Int) {
                        currentRow = mutableListOf()
                    }

                    override fun cell(cellReference: String?, formattedValue: String?, comment: XSSFComment?) {
                        currentRow.add(formattedValue ?: "")
                    }

                    override fun endRow(rowNum: Int) {
                        rowCallback(currentRow.toList(), rowNum)
                    }

                    override fun headerFooter(text: String?, isHeader: Boolean, tagName: String?) {}
                },
                false
            )
            xmlReader.contentHandler = sheetHandler
            xmlReader.parse(InputSource(sheetStream))
        } finally {
            opc.close()
        }
    }
}
