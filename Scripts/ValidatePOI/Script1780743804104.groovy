import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph

XWPFDocument doc = new XWPFDocument()
XWPFParagraph p = doc.createParagraph()
p.createRun().setText("Apache POI funcionando correctamente")

println "POI OK: " + XWPFDocument.class.getName()

doc.close()