import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph

import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date

File outputDir = new File("Reports/evidence-docx")
outputDir.mkdirs()

String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())
File outputFile = new File(outputDir, "POI_SMOKE_TEST_${timestamp}.docx")

XWPFDocument document = new XWPFDocument()

XWPFParagraph paragraph = document.createParagraph()
paragraph.createRun().setText("Apache POI funcionando correctamente.")
paragraph.createRun().addBreak()
paragraph.createRun().setText("Fecha/Hora: ${new Date()}")

FileOutputStream fos = new FileOutputStream(outputFile)
document.write(fos)

fos.close()
document.close()

println "======================================"
println "WORD GENERADO CORRECTAMENTE"
println outputFile.absolutePath
println "EXISTE: ${outputFile.exists()}"
println "======================================"