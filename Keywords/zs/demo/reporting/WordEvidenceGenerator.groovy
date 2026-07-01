package zs.demo.reporting

import com.kms.katalon.core.configuration.RunConfiguration
import internal.GlobalVariable
import org.apache.poi.xwpf.usermodel.*
import org.apache.poi.util.Units
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*

class WordEvidenceGenerator {

	private static final String COLOR_BLUE = "1F4E79"
	private static final String COLOR_GREEN = "008000"
	private static final String COLOR_RED = "C00000"
	private static final String COLOR_GRAY = "666666"
	private static final String COLOR_LIGHT_GREEN = "D9EAD3"
	private static final String COLOR_LIGHT_RED = "F4CCCC"
	private static final String COLOR_LIGHT_GRAY = "EDEDED"
	private static final String COLOR_WHITE = "FFFFFF"

	static File generate(String caseName, String executionKey, String status, Map data, Map executionItem) {
		try {
			if (!"true".equalsIgnoreCase(GlobalVariable.ZS_ENABLE_INTEGRATION?.toString())) {
				println "[WORD-EVIDENCE] Generación Word omitida porque ZS_ENABLE_INTEGRATION=false"
				return null
			}
			
			File dir = new File("${RunConfiguration.getProjectDir()}/Reports/evidence-docx")
			if (!dir.exists()) {
				dir.mkdirs()
			}

			File out = new File(dir, "${safeFileName(caseName)}_Evidence.docx")

			XWPFDocument doc = new XWPFDocument()

			title(doc, "Evidencia Automatizada de Ejecución")
			subtitle(doc, "Framework Katalon + Cucumber + Zephyr")

			statusBanner(doc, status)

			heading(doc, "1. Resumen de ejecución")
			summaryTable(doc, caseName, executionKey, status, data, executionItem)

			def report = executionItem?.cucumberReport

			if (report?.hasData()) {
				heading(doc, "2. Feature / Scenario")
				infoBox(doc, [
					"Feature: ${report.feature ?: '-'}",
					"Scenario: ${report.scenario ?: '-'}",
					"Tag: ${data?.tags ?: '-'}"
				])

				heading(doc, "3. Step by Step con evidencias")
				writeCucumberSteps(doc, report, executionItem)
			} else {
				heading(doc, "3. Step by Step con evidencias")
				writeManualSteps(doc, executionItem)
			}

			if (status?.equalsIgnoreCase("FAILED")) {
				heading(doc, "4. Detalle técnico del error")
				errorBox(doc, extractFailureMessage(executionItem))

				String errorScreenshot = executionItem?.errorScreenshot ?: ''
				if (errorScreenshot) {
					heading(doc, "5. Screenshot final del error")
					addImage(doc, errorScreenshot)
				}
			}

			FileOutputStream fos = new FileOutputStream(out)
			doc.write(fos)
			fos.close()
			doc.close()

			println "[WORD-EVIDENCE] Documento generado: ${out.absolutePath}"
			return out

		} catch (Throwable e) {
			println "[WORD-EVIDENCE][WARN] No se pudo generar Word: ${e.message}"
			return null
		}
	}

	private static void writeCucumberSteps(XWPFDocument doc, def report, Map executionItem) {
		List<String> screenshots = executionItem?.stepScreenshots ?: []

		report.steps.eachWithIndex { Map step, int index ->
			String stepStatus = step.status ?: '-'
			String stepText = "${step.keyword ?: ''} ${step.name ?: ''}".trim()
			String duration = formatDurationNanos(step.duration)

			stepHeader(doc, index + 1, stepStatus, stepText, duration)

			if (step.error) {
				errorBox(doc, step.error.toString())
			}

			String screenshot = index < screenshots.size() ? screenshots[index] : ''
			if (screenshot) {
				addImage(doc, screenshot)
			}

			spacer(doc)
		}
	}

	private static void writeManualSteps(XWPFDocument doc, Map executionItem) {
		List<Map> steps = executionItem?.steps ?: []
		List<String> screenshots = executionItem?.stepScreenshots ?: []

		steps.eachWithIndex { Map step, int index ->
			String status = step.status ?: '-'
			String stepText = step.stepName ?: '-'

			stepHeader(doc, index + 1, status, stepText, '')

			paragraph(doc, "Resultado esperado: ${step.expectedResult ?: '-'}")
			paragraph(doc, "Resultado actual: ${step.actualResult ?: '-'}")

			String screenshot = index < screenshots.size() ? screenshots[index] : ''
			if (screenshot) {
				addImage(doc, screenshot)
			}

			spacer(doc)
		}
	}

	private static void summaryTable(XWPFDocument doc, String caseName, String executionKey, String status, Map data, Map executionItem) {
		XWPFTable table = doc.createTable(10, 2)
		table.setWidth("100%")

		headerCell(table, 0, 0, "Campo")
		headerCell(table, 0, 1, "Valor")

		valueRow(table, 1, "Caso de prueba", caseName)
		valueRow(table, 2, "Descripción", data?.description ?: "-")
		valueRow(table, 3, "Estado", status ?: "-")
		valueRow(table, 4, "Ejecución Zephyr", executionKey ?: "-")
		valueRow(table, 5, "Ejecutor", getGlobal("ZS_JIRA_USER"))
		valueRow(table, 6, "Ambiente", getGlobal("ZS_ENVIRONMENT_NAME"))
		valueRow(table, 7, "Versión", getGlobal("ZS_VERSION_NAME"))
		valueRow(table, 8, "Feature", data?.featurePath ?: "-")
		valueRow(table, 9, "Duración total", formatDurationSeconds(executionItem?.durationMs))
	}

	private static void stepHeader(XWPFDocument doc, int number, String status, String stepText, String duration) {
		XWPFTable table = doc.createTable(1, 3)
		table.setWidth("100%")

		String bg = statusColorBackground(status)
		String fg = statusColor(status)

		setCellStyled(table, 0, 0, "Step ${number}", bg, fg, true)
		setCellStyled(table, 0, 1, "${statusIcon(status)} ${status}".trim(), bg, fg, true)
		setCellStyled(table, 0, 2, duration ?: "-", bg, fg, true)

		paragraph(doc, stepText)
	}

	private static void statusBanner(XWPFDocument doc, String status) {
		String value = status ?: "-"
		String bg = statusColorBackground(value)
		String fg = statusColor(value)

		XWPFTable table = doc.createTable(1, 1)
		table.setWidth("100%")
		setCellStyled(table, 0, 0, "ESTADO FINAL: ${value}", bg, fg, true)
	}

	private static void infoBox(XWPFDocument doc, List<String> lines) {
		XWPFTable table = doc.createTable(lines.size(), 1)
		table.setWidth("100%")

		lines.eachWithIndex { String line, int index ->
			setCellStyled(table, index, 0, line, "EAF2F8", COLOR_BLUE, false)
		}
	}

	private static void errorBox(XWPFDocument doc, String text) {
		XWPFTable table = doc.createTable(1, 1)
		table.setWidth("100%")
		setCellStyled(table, 0, 0, clean(text), COLOR_LIGHT_RED, COLOR_RED, false)
	}

	private static void title(XWPFDocument doc, String text) {
		XWPFParagraph p = doc.createParagraph()
		p.setAlignment(ParagraphAlignment.CENTER)

		XWPFRun r = p.createRun()
		r.setBold(true)
		r.setFontSize(20)
		r.setColor(COLOR_BLUE)
		r.setText(clean(text))
	}

	private static void subtitle(XWPFDocument doc, String text) {
		XWPFParagraph p = doc.createParagraph()
		p.setAlignment(ParagraphAlignment.CENTER)

		XWPFRun r = p.createRun()
		r.setItalic(true)
		r.setFontSize(11)
		r.setColor(COLOR_GRAY)
		r.setText(clean(text))
	}

	private static void heading(XWPFDocument doc, String text) {
		XWPFParagraph p = doc.createParagraph()

		XWPFRun r = p.createRun()
		r.setBold(true)
		r.setFontSize(14)
		r.setColor(COLOR_BLUE)
		r.setText(clean(text))
	}

	private static void paragraph(XWPFDocument doc, String text) {
		XWPFParagraph p = doc.createParagraph()

		XWPFRun r = p.createRun()
		r.setFontSize(10)
		r.setText(clean(text))
	}

	private static void spacer(XWPFDocument doc) {
		XWPFParagraph p = doc.createParagraph()
		p.createRun().setText("")
	}

	private static void addImage(XWPFDocument doc, String path) {
		try {
			File img = new File(path)

			if (!img.exists() || !img.isFile()) {
				paragraph(doc, "Screenshot no encontrado: ${path}")
				return
			}

			XWPFParagraph p = doc.createParagraph()
			p.setAlignment(ParagraphAlignment.CENTER)

			XWPFRun run = p.createRun()
			FileInputStream is = new FileInputStream(img)

			run.addPicture(
				is,
				XWPFDocument.PICTURE_TYPE_PNG,
				img.name,
				Units.toEMU(520),
				Units.toEMU(300)
			)

			is.close()

		} catch (Throwable e) {
			paragraph(doc, "No se pudo insertar screenshot: ${path}")
		}
	}

	private static void headerCell(XWPFTable table, int row, int col, String text) {
		setCellStyled(table, row, col, text, COLOR_BLUE, COLOR_WHITE, true)
	}

	private static void valueRow(XWPFTable table, int row, String field, String value) {
		setCellStyled(table, row, 0, field, COLOR_LIGHT_GRAY, "000000", true)

		String bg = COLOR_WHITE
		String fg = "000000"

		if (field.equalsIgnoreCase("Estado")) {
			bg = statusColorBackground(value)
			fg = statusColor(value)
		}

		setCellStyled(table, row, 1, value, bg, fg, false)
	}

	private static void setCellStyled(XWPFTable table, int row, int col, String text, String bgColor, String fontColor, boolean bold) {
		XWPFTableCell cell = table.getRow(row).getCell(col)
		cell.removeParagraph(0)

		CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr()
		CTShd shd = tcPr.isSetShd() ? tcPr.getShd() : tcPr.addNewShd()
		shd.setFill(bgColor)

		XWPFParagraph p = cell.addParagraph()
		XWPFRun r = p.createRun()
		r.setBold(bold)
		r.setColor(fontColor)
		r.setFontSize(10)
		r.setText(clean(text))
	}

	private static String statusIcon(String status) {
	String value = status?.toUpperCase() ?: ""

	if (value == "PASSED" || value == "PASS") {
		return "✓"
	}

	if (value == "FAILED" || value == "FAIL") {
		return "✕"
	}

	if (value == "SKIPPED" || value == "SKIP") {
		return "-"
	}

	return ""
}

	private static String statusColor(String status) {
		String value = status?.toUpperCase() ?: ""

		if (value == "PASSED" || value == "PASS") {
			return COLOR_GREEN
		}

		if (value == "FAILED" || value == "FAIL") {
			return COLOR_RED
		}

		if (value == "SKIPPED" || value == "SKIP") {
			return COLOR_GRAY
		}

		return "000000"
	}

	private static String statusColorBackground(String status) {
		String value = status?.toUpperCase() ?: ""

		if (value == "PASSED" || value == "PASS") {
			return COLOR_LIGHT_GREEN
		}

		if (value == "FAILED" || value == "FAIL") {
			return COLOR_LIGHT_RED
		}

		if (value == "SKIPPED" || value == "SKIP") {
			return COLOR_LIGHT_GRAY
		}

		return COLOR_LIGHT_GRAY
	}

	private static String extractFailureMessage(Map executionItem) {
		def report = executionItem?.cucumberReport

		if (report?.message) {
			return "${report.exceptionType ?: ''}\n${report.message ?: ''}\n${report.location ?: ''}"
		}

		return executionItem?.error ?: "-"
	}

	private static String formatDurationSeconds(def millisValue) {
		try {
			long millis = millisValue ? millisValue.toString().toLong() : 0L
			if (millis <= 0L) {
				return "-"
			}
			return String.format(Locale.US, "%.2f segundos", millis / 1000.0D)
		} catch (Throwable ignored) {
			return "-"
		}
	}

	private static String formatDurationNanos(def nanosValue) {
		try {
			long nanos = nanosValue ? nanosValue.toString().toLong() : 0L
			if (nanos <= 0L) {
				return "-"
			}
			return String.format(Locale.US, "%.2f segundos", nanos / 1000000000.0D)
		} catch (Throwable ignored) {
			return "-"
		}
	}

	private static String getGlobal(String name) {
		try {
			def field = GlobalVariable.class.getField(name)
			def value = field.get(null)
			return value?.toString()?.trim() ?: "-"
		} catch (Throwable ignored) {
			return "-"
		}
	}

	private static String safeFileName(String value) {
		return (value ?: "Evidence").replaceAll("[^A-Za-z0-9._-]", "_")
	}

	private static String clean(def value) {
		return value?.toString()
			?.replaceAll("\\u001B\\[[;\\d]*m", "")
			?.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "")
			?.trim() ?: "-"
	}
}