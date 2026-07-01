import zs.demo.reporting.WordEvidenceGenerator

File doc =
	WordEvidenceGenerator.generate(
		"CP001_CHECKOUT_SUCCESS",
		"DGDC-E999",
		"PASSED",
		[
			description : "Checkout exitoso con usuario standard"
		],
		[
			durationMs : 15000,
			steps : []
		]
	)

println "WORD=" + doc?.absolutePath