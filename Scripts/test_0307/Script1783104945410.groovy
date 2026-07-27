import org.apache.poi.xwpf.usermodel.XWPFDocument

Package pkg = XWPFDocument.class.getPackage()

println "=============================="
println "Clase      : " + XWPFDocument.class.getName()
println "Implement. : " + pkg.getImplementationTitle()
println "Versión    : " + pkg.getImplementationVersion()
println "Especific. : " + pkg.getSpecificationVersion()
println "=============================="