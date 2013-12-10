package us.mn.state.health.lims.analyzerimport.analyzerreaders;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.validator.GenericValidator;
import org.hibernate.Transaction;

import us.mn.state.health.lims.analyzerimport.util.AnalyzerTestNameCache;
import us.mn.state.health.lims.analyzerimport.util.MappedTestName;
import us.mn.state.health.lims.analyzerimport.util.AnalyzerTestNameCache.AnalyzerType;
import us.mn.state.health.lims.analyzerresults.valueholder.AnalyzerResults;
import us.mn.state.health.lims.common.exception.LIMSRuntimeException;
import us.mn.state.health.lims.common.util.DateUtil;
import us.mn.state.health.lims.common.util.HibernateProxy;
import us.mn.state.health.lims.common.util.StringUtil;
import us.mn.state.health.lims.hibernate.HibernateUtil;
import us.mn.state.health.lims.test.dao.TestDAO;
import us.mn.state.health.lims.test.daoimpl.TestDAOImpl;

public class Gerber4000Reader extends AnalyzerLineInserter {

	private static final String VALID_ACCESSION_PREFIX = "LART";
	private static final String SAMPLE_ID_HEADER = "Sample ID";
	private static final String DATE_ANALYZED_HEADER = "Order Date/Time";
	private static final String OUT_HEADER = "out";

	private static final String DATE_PATTERN = "MM/dd/yyyy HH:mm";

	private int sampleId;
	private int dateAnalyzed;
	private int out;

	private String[] testNameIndex;
	private String[] unitsIndex;
	private int maxViewedIndex = 0;
	private Pattern pattern = Pattern.compile("[0-9]+[\\.]*[0-9]+E[\\+]*[0-9]+");
	Matcher matcher ;
	

	public boolean insert(List<String> lines, String currentUserId) {

		boolean successful = true;

		List<AnalyzerResults> results = new ArrayList<AnalyzerResults>();

		manageColumns(lines.get(0));

		for (int i = 1; i < lines.size(); i++) {

			addAnalyzerResultFromLine(results, lines.get(i));

		}

		if (results.size() > 0) {

			Transaction tx = HibernateProxy.beginTransaction();

			try {

				persistResults(results, currentUserId);

				tx.commit();

			} catch (LIMSRuntimeException lre) {
				tx.rollback();
				successful = false;
			} finally {
				HibernateProxy.closeSession();
			}
		}

		return successful;
	}

	private void manageColumns(String line) {
		String[] fields = StringUtil.separateCSVWithEmbededQuotes(line);
		if (fields.length < 18) {
			fields = line.split(",");
		}

		for (int i = 0; i < fields.length; i++) {
			String header = fields[i].replace("\"", "");

			if (SAMPLE_ID_HEADER.equals(header)) {
				sampleId = i;
				maxViewedIndex = Math.max(maxViewedIndex, i);
			} else if (DATE_ANALYZED_HEADER.equals(header)) {
				dateAnalyzed = i;
				maxViewedIndex = Math.max(maxViewedIndex, i);
			} else if (OUT_HEADER.equals(header)) {
				out = i;
				maxViewedIndex = Math.max(maxViewedIndex, i);
			}
		}

		testNameIndex = new String[fields.length];
		unitsIndex = new String[fields.length];

		testNameIndex[out] = "VIH_P";

		unitsIndex[out] = "C/mL";

	}

	private void addAnalyzerResultFromLine(List<AnalyzerResults> results,
			String line) {
		
		
		
		String[] fields = StringUtil.separateCSVWithMixedEmbededQuotes(line);

		// This insures that the row has not been truncated
		if (fields.length < maxViewedIndex) {
			return;
		}

		AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();
		String analyzerAccessionNumber = fields[sampleId].replace("\"", "");
		analyzerAccessionNumber = StringUtil
				.strip(analyzerAccessionNumber, " ");

		String date = fields[dateAnalyzed].replace("\"", "");

		// this is sort of dumb, we have the indexes we are interested in
		for (int i = 0; i < testNameIndex.length; i++) {
			if (!GenericValidator.isBlankOrNull(testNameIndex[i])) {
				MappedTestName mappedName = AnalyzerTestNameCache.instance()
						.getMappedTest(AnalyzerType.GERBER_4000,
								testNameIndex[i].replace("\"", "")); //

				if (mappedName == null) {
					mappedName = AnalyzerTestNameCache.instance()
							.getEmptyMappedTestName(AnalyzerType.GERBER_4000,
									testNameIndex[i].replace("\"", "")); //
				}

				AnalyzerResults analyzerResults = new AnalyzerResults();

				analyzerResults.setAnalyzerId(mappedName.getAnalyzerId());

				String result = fields[i].replace("\"", "");
				result = formatResult(result);
				result = roundTwoDigits(result);
				analyzerResults.setResult(result);
				analyzerResults.setUnits(unitsIndex[i]);

				analyzerResults.setCompleteDate(DateUtil
						.convertStringDateToTimestampWithPatternNoLocale(date,
								DATE_PATTERN));
				// analyzerResults.setCompleteTime(DateUtil.convertStringDateToTimestamp(date));
				analyzerResults.setTestId(mappedName.getTestId());
				analyzerResults.setAccessionNumber(analyzerAccessionNumber);
				analyzerResults.setTestName(mappedName.getOpenElisTestName());

				if (analyzerAccessionNumber != null) {
					analyzerResults.setIsControl(!analyzerAccessionNumber
							.startsWith(VALID_ACCESSION_PREFIX));
				} else {
					analyzerResults.setIsControl(false);
				}

				results.add(analyzerResults);

				AnalyzerResults resultFromDB = readerUtil
						.createAnalyzerResultFromDB(analyzerResults);
				if (resultFromDB != null) {
					results.add(resultFromDB);
				}
			}
		}
	}

	public String formatResult(String param) {

		String var = param.trim();
		String out = "";

		if (var.equalsIgnoreCase("Target Not Detected")) {
			out = "Not Detected	";
		} else {
			try {				
				matcher = pattern.matcher(var);
				while (matcher.find()) {
					String value = matcher.group().replace("+", "");
					String[] expo = value.split("E");
					int i = (int) ((Double.valueOf(expo[0]) * ((int) Math.pow(
							10, Double.valueOf(expo[1])))));
					out = String.valueOf(i);
				}
			} catch (PatternSyntaxException exception) {

			}
		}

		return out;
	}

	private String roundTwoDigits(String result) {

		try {
			Double doubleResult = Double.parseDouble(result);
			StringBuilder sb = new StringBuilder();
			Formatter formatter = new Formatter(sb);
			formatter.format("%.2f", (Math.rint(doubleResult * 100.0) / 100.0));
			return sb.toString();
		} catch (NumberFormatException e) {
			return result;
		}
	}

	@Override
	public String getError() {

		return "Gerber_4000 analyzer unable to write to database";
	}

}