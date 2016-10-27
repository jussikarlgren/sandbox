package com.gavagai.jussisandbox;


import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import com.gavagai.mockrabbit.DistancePolarizationServiceImpl;
import com.gavagai.mockrabbit.ExactSkipAmplifyPolarizationServiceImpl;
import com.gavagai.mockrabbit.GavagaiStringUtils;
import com.gavagai.mockrabbit.Language;
import com.gavagai.mockrabbit.PolarizationAlgorithm;
import com.gavagai.mockrabbit.PolarizationResult;
import com.gavagai.mockrabbit.PolarizationService;
import com.gavagai.mockrabbit.PolarizationStatistics;
import com.gavagai.mockrabbit.Pole;
import com.gavagai.mockrabbit.PoleGroup;
import com.gavagai.mockrabbit.PolePart;
import com.gavagai.mockrabbit.Target;
import com.gavagai.mockrabbit.TargetMonitor;
import com.gavagai.mockrabbit.Utterance;
import com.gavagai.mockrabbit.WordSpace;
import com.gavagai.jussiutil.ConfusionMatrix;
import com.gavagai.jussiutil.Sentiment;
import com.gavagai.jussiutil.TextWithSentiment;
import com.gavagai.rabbit.language.LanguageAnalyzerWrapper;

public class TestPolariser {
	private int MAX_UTTERANCE_TOKENS = 50;

	HashMap<Sentiment,Double> POSWEIGHTS;
	HashMap<Sentiment,Double> NEGWEIGHTS;

	int POSITIVEPOLE = 41;
	int NEGATIVEPOLE = 42;

	double POSITIVE_DAMPER = 0; //0.2;
	double NEGATIVE_DAMPER = 0;// 0.2;
	double NEUTRAL_CORRIDOR = 0; // 0.1;
	int BUCKETSIZE = 100000;
	private Log logger = LogFactory.getLog(TestPolariser.class);
	HashMap<TextWithSentiment,List<PolarizationResult>> results;
	PolarizationAlgorithm algo;
	WordSpace wordSpace;

	Hashtable<Integer,Pole> poles = new Hashtable<Integer,Pole>();
	Vector<TextWithSentiment> bucket = new Vector<TextWithSentiment>();
	Language language = Language.EN; // default
	ConfusionMatrix errors = new ConfusionMatrix(poles.size()+1);

	private String testIdentifier;
	//	private String testFileName;
	//	private String testDirectory;

	private String testCondition;

	public void setTestIdentifier(String testIdentifier) {
		this.testIdentifier = testIdentifier;
	}
	//
	//	public void setTestFileName(String testFileName) {
	//		this.testFileName = testFileName;
	//	}
	//
	//	public void setTestDirectory(String testDirectory) {
	//		this.testDirectory = testDirectory;
	//	}

	public TestPolariser() {
		results = new HashMap<TextWithSentiment,List<PolarizationResult>>();

		POSWEIGHTS = new HashMap<Sentiment,Double>();
		NEGWEIGHTS = new HashMap<Sentiment,Double>();

		POSWEIGHTS.put(Sentiment.POSITIVE, 0.3);
		POSWEIGHTS.put(Sentiment.NEGATIVE, -0.3);
		POSWEIGHTS.put(Sentiment.LOVE, 0.0);
		POSWEIGHTS.put(Sentiment.HATE, 0.0);
		POSWEIGHTS.put(Sentiment.FEAR, -0.1);
		POSWEIGHTS.put(Sentiment.SEXY, 0.2);
		POSWEIGHTS.put(Sentiment.SKEPTIC, -0.1);
		POSWEIGHTS.put(Sentiment.VIOLENT, -0.3);
		POSWEIGHTS.put(Sentiment.BORING, 0.0);
		POSWEIGHTS.put(Sentiment.PROFANITY, 0.0);

		NEGWEIGHTS.put(Sentiment.POSITIVE, -0.29);
		NEGWEIGHTS.put(Sentiment.NEGATIVE, 0.3);
		NEGWEIGHTS.put(Sentiment.LOVE, 0.0);
		NEGWEIGHTS.put(Sentiment.HATE, 0.0);
		NEGWEIGHTS.put(Sentiment.FEAR, 0.13);
		NEGWEIGHTS.put(Sentiment.SEXY, -0.1);
		NEGWEIGHTS.put(Sentiment.SKEPTIC, 0.1);
		NEGWEIGHTS.put(Sentiment.VIOLENT, 0.2);
		NEGWEIGHTS.put(Sentiment.BORING, 0.1);
		NEGWEIGHTS.put(Sentiment.PROFANITY, 0.5);

		readPoles();

	}

	public PolarizationAlgorithm getAlgo() {
		return algo;
	}
	public void setAlgo(PolarizationAlgorithm algo) {
		this.algo = algo;
	}



	public void readPoles() {
		wordSpace = new WordSpace();
		readAPole("pos",801,"/home/jussi/source/rabbit-data/poles/en/enposBingLiu.list");
		readAPole("neg",802,"/home/jussi/source/rabbit-data/poles/en/ennegBingLiu.list");
		readAPole("pos",101,"/home/jussi/source/rabbit-data/poles/en/enposMINI.list");
		readAPole("neg",102,"/home/jussi/source/rabbit-data/poles/en/ennegMINI.list");
		readAPole("pos",41,"/home/jussi/source/rabbit-data/poles/en/enpos41.list");
		readAPole("neg",42,"/home/jussi/source/rabbit-data/poles/en/enneg42.list");
		readAPole("boring",637,"/home/jussi/source/rabbit-data/poles/en/enboring637.list");
		readAPole("skeptic",702,"/home/jussi/source/rabbit-data/poles/en/enskeptic702.list");
		readAPole("fear",703,"/home/jussi/source/rabbit-data/poles/en/enfear703.list");
		readAPole("hate",705,"/home/jussi/source/rabbit-data/poles/en/enhate705.list");
		readAPole("love",704,"/home/jussi/source/rabbit-data/poles/en/enlove704.list");
		readAPole("sexy",29,"/home/jussi/source/rabbit-data/poles/en/ensexy29.list");
		readAPole("violent",98,"/home/jussi/source/rabbit-data/poles/en/enbang98.list");
		readAPole("profanity",764,"/home/jussi/source/rabbit-data/poles/en/enprofanity764.list");
		Pole negationPole = readAPole("negation",107,"/home/jussi/source/rabbit-data/poles/en/ennegation107.list");
		Pole amplifierPole = readAPole("amplify",242,"/home/jussi/source/rabbit-data/poles/en/enamplify242.list");		
		wordSpace.setAmplifierPole(amplifierPole);
		wordSpace.setNegationPole(negationPole);
	}
	private Pole readAPole(String name, int id, String filename) {
		Pole aPole = new Pole();
		aPole.setName(name);
		aPole.setId(id);
		List<PolePart> members = new ArrayList<PolePart>();
		Scanner fileScanner;
		try {
			fileScanner = new Scanner(new BufferedInputStream(new FileInputStream(filename)));
			while (fileScanner.hasNextLine()) {	
				String fileLine = fileScanner.nextLine();
				String bits[] = new String[2];
				bits = fileLine.split(",");
				String hep = bits[0].replace("\"", "");
				PolePart pp = new PolePart();
				pp.setValue(hep);
				pp.setPole(aPole);
				members.add(pp);
			}
			fileScanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			logger.error("No pole for "+name+" found.");
		}
		aPole.setMembers(members);
		poles.put(id,aPole);
		return aPole;
	}


	private void polarise(String algo, boolean butter, boolean v, String lexicon, String negation, String tag) throws IOException {
		PolarizationService doer;
		bucket = TextGetter.getTexts(testIdentifier);
		if (lexicon.equals("bingliu")) {POSITIVEPOLE = 801; NEGATIVEPOLE = 802;}
		if (lexicon.equals("mini")) {POSITIVEPOLE = 101; NEGATIVEPOLE = 102;}
		if (lexicon.equals("gavagai")) {POSITIVEPOLE = 41; NEGATIVEPOLE = 42;}
		PoleGroup domain = new PoleGroup();
		List<Pole> activepoles = new ArrayList<Pole>();
		activepoles.add(poles.get(POSITIVEPOLE));
		activepoles.add(poles.get(NEGATIVEPOLE));
		domain.setPoles(activepoles);
		setTestCondition(tag);
		if (algo.equals("esa")) {
			doer = new ExactSkipAmplifyPolarizationServiceImpl();
			if (butter) ((ExactSkipAmplifyPolarizationServiceImpl)doer).setBut(true);
			if (v) ((ExactSkipAmplifyPolarizationServiceImpl)doer).setV(true);
		} else {
			doer = new DistancePolarizationServiceImpl();
			if (negation.equals("skip")) ((DistancePolarizationServiceImpl) doer).setSkip(true);
			if (negation.equals("flip")) ((DistancePolarizationServiceImpl) doer).setFlip(true);
			//			((DistancePolarizationServiceImpl) doer).setHedge(hedge);
			//			((DistancePolarizationServiceImpl) doer).setAmplifier(amp);
//			if (algo.equals("cont")) ((DistancePolarizationServiceImpl) doer).setVariant(true);
//			else if (algo.equals("disc")) ((DistancePolarizationServiceImpl) doer).setVariant2(true);
//			else if (algo.equals("reverse")) ((DistancePolarizationServiceImpl) doer).setReverse(true);
			if (v) ((DistancePolarizationServiceImpl) doer).setV(true);
			if (butter) {
				((DistancePolarizationServiceImpl) doer).setBut(true); 
			}
		}
		logger.info(testIdentifier);
		List<PolarizationResult> polarisationResults;
		TargetMonitor dummyTargetMonitor = new TargetMonitor();
		dummyTargetMonitor.setTarget(new Target());
		dummyTargetMonitor.setPoleGroup(domain);
		dummyTargetMonitor.setWordSpace(wordSpace);
		LanguageAnalyzerWrapper languageAnalyzerWrapper = new LanguageAnalyzerWrapper();
		Set<String> targetPartsSet = dummyTargetMonitor.getTarget().getTargetExpansionPartsAsSet();
		boolean shouldFilter = (targetPartsSet == null || targetPartsSet.size() == 0) ? false : true;
		int nGramComplexity = 1;
		for (TextWithSentiment tws: bucket) {
			List<Utterance> utterances = new ArrayList<Utterance>();
			Utterance utterance = new Utterance();
			String utteranceAsText = tws.getText();
			// from DocumentFilterServiceImpl:			private Utterance parseAndFilterUtteranceText()
			//			TokenStream stream = languageAnalyzerWrapper.getAnalyzer(language.name())
			//					 .reusableTokenStream("thread-local-polarization-stream",new StringReader(utteranceAsText));
			// SOME CHG IN UTIL CLASS MADE THE BELOW EDIT NECESSARY (cf line above)
			TokenStream stream = (languageAnalyzerWrapper.getAnalyzer(language.name()))
					.tokenStream("thread-local-polarization-stream",new StringReader(utteranceAsText));
			CharTermAttribute termAtt = stream.getAttribute(CharTermAttribute.class);
			stream.reset();
			boolean isMatchedByTargetParts = false;
			utterance.setUtteranceText(utteranceAsText);
			List<String> previousTerms = new ArrayList<String>();
			int matchingTermPositionIndex = 0;
			int tokenCounter = 0;
			while (stream.incrementToken()) {
				String utteranceTerm = termAtt.toString();
				String previousTerm = utteranceTerm;
				if (shouldFilter && !isMatchedByTargetParts) {
					if (targetPartsSet.contains(utteranceTerm)) {
						isMatchedByTargetParts = true;
						matchingTermPositionIndex = tokenCounter;
					} else if (!previousTerms.isEmpty()) {
						String tail = matchPreviousTerms(targetPartsSet, previousTerms,
								utteranceTerm);
						if (tail != null) {
							isMatchedByTargetParts = true;
							matchingTermPositionIndex = tokenCounter;
							utteranceTerm = tail;
							int spaceCount = GavagaiStringUtils.countOccurrences(tail, ' ');
							matchingTermPositionIndex -= spaceCount;
							for (int i = 0; i < spaceCount; i++) {
								utterance.getTokenizedTerms().remove(
										utterance.getTokenizedTerms().size() - 1);
							}
						}

					}
				}
				tokenCounter++;
				utterance.getTokenizedTerms().add(utteranceTerm);
				previousTerms.add(0, previousTerm);
				if (previousTerms.size() == nGramComplexity) {
					previousTerms.remove(previousTerms.size() - 1);
				}
			}
			stream.close();
			if (!shouldFilter || isMatchedByTargetParts) {
				if (utterance.getTokenizedTerms().size() > MAX_UTTERANCE_TOKENS) {
					utterance.setTokenizedTerms(compressUtterance(utterance.getTokenizedTerms(),
							matchingTermPositionIndex));
				}
			}
			utterances.add(utterance);
			polarisationResults = doer.generatePolarizationResults(utterances, dummyTargetMonitor, new HashMap<String,String>(), new PolarizationStatistics(activepoles));
			results.put(tws, polarisationResults);
		}
	}

	public List<String> compressUtterance(List<String> utteranceAsWords, int termPositionIndex) {
		List<String> newWordsList = new ArrayList<String>();
		if (termPositionIndex < (MAX_UTTERANCE_TOKENS / 2)) {
			newWordsList.addAll(utteranceAsWords.subList(0, MAX_UTTERANCE_TOKENS));
		} else {
			int offset = termPositionIndex - (MAX_UTTERANCE_TOKENS / 2);
			newWordsList.addAll(utteranceAsWords.subList(
					Math.min(offset,
							Math.min(MAX_UTTERANCE_TOKENS + offset, utteranceAsWords.size())
							- MAX_UTTERANCE_TOKENS),
							Math.min(MAX_UTTERANCE_TOKENS + offset, utteranceAsWords.size())));
		}
		return newWordsList;
	}
	private String matchPreviousTerms(Set<String> targetPartsSet, List<String> previousTerms,
			String utteranceTerm) {
		String tail = utteranceTerm;
		for (String previousTerm : previousTerms) {
			tail = previousTerm + " " + tail;
			if (targetPartsSet.contains(tail)) {
				return tail;
			}
		}
		return null;

	}

	//	for (TextWithSentiment f : bucket) {
	//		Sentiment s = interpretScores(response);
	//		boolean result = reportAndKeepBookOfAnalysis(out,antal,response.getUtterances(),response.getStatistics(),f.getSentiment(),s,f.getText());
	//		logger.debug("Polarised "+result+" "+s+"<->"+f.getSentiment()+" "+f.getText()+" ("+antal+"): "+response.getStatistics());
	//	HashMap<String, Integer> errorWords = new HashMap<String, Integer>();
	//	HashMap<String, Integer> missedWords = new HashMap<String, Integer>();
	//	HashMap<String, Integer> neverGuessedWords = new HashMap<String, Integer>();		
	//		logger.info("Top words leading to false classification: " + getErrorWords(30)+"\n");
	//		logger.info("Top missing words: " + getMissedWords(30)+"\n");
	//		logger.info("Top missing words from non-classified utterances: " + getMissedWordsFromNeverGuessed(30)+"\n");
	//		logger.info("Top chi square values: " + getTopChiSquareValues(30)+"\n");
	//	int neverguessed = 0;
	//	int totalGoldPos = 0;
	//	int totalGoldNeg = 0;
	//	int antal = 0;

	private Sentiment interpretScores(List<PolarizationResult> polscores) {
		Sentiment s = Sentiment.NEUTRAL;
		double thispos = 0;
		double thisneg = 0;
		for (PolarizationResult polscore : polscores) {
			Pole ps = polscore.getPole();
			Float score = polscore.getValue();
			try {
				thispos += score * POSWEIGHTS.get(Sentiment.constrain(ps.getName()));
				thisneg += score * NEGWEIGHTS.get(Sentiment.constrain(ps.getName()));
			} catch (NullPointerException e) {}
		}			
		if (Math.abs(thispos - thisneg) < NEUTRAL_CORRIDOR) {
			s = Sentiment.NEUTRAL;
		} else if (thispos > thisneg - POSITIVE_DAMPER) {
			s = Sentiment.POSITIVE;
		} else if (thisneg > thispos - NEGATIVE_DAMPER) {
			s = Sentiment.NEGATIVE;
		} else {
			s = Sentiment.NEUTRAL;	
		}
		// ===============================================================
		// 
		//			if (ps.getId() == POSITIVEPOLE && score > 0) {thispos += score;}
		//			if (ps.getId() == NEGATIVEPOLE && score > 0) {thisneg += score;}
		//		}
		//		if (thispos > 0) {s = Sentiment.POSITIVE;}  
		//			else
		//				if (thisneg > 0) {s = Sentiment.NEGATIVE;}
		//				
		return s;
	}


	private void classify() {
		for (TextWithSentiment presult: results.keySet()) {
			Sentiment facit = presult.getSentiment();
			Sentiment responseSentiment = interpretScores(results.get(presult));
			//		boolean result = reportAndKeepBookOfAnalysis(out,antal,response.getUtterances(),response.getStatistics(),f.getSentiment(),s,f.getText());
			//		protected boolean reportAndKeepBookOfAnalysis(Writer out, int antal, List<Utterance> utterances,PolarizationStatistics stats,Sentiment facit,Sentiment responseSentiment,String txt) {	
			errors.increment(facit,responseSentiment);
		}
	}
	private String demonstrate() {
		TreeSet<Object> targets = new TreeSet<Object>();
		targets.add(Sentiment.POSITIVE);
		targets.add(Sentiment.NEGATIVE);
		String tagLabel = "null";
		if (testIdentifier != null) {
			if (testIdentifier.equals("replab")) tagLabel = "RepLab";
			if (testIdentifier.equals("oscar")) tagLabel = "T \\& McD";
			if (testIdentifier.equals("stanford")) tagLabel = "Stanford";
		}
		return errors + "\n" + errors.getItems() + "\n" + testCondition + "\n" +
		"tag &\t micro prec & \t micro rec & \t macro prec & \t macro rec & \t pos prec & \t pos rec & \t neg prec & \t neg rec \\\\ \n"+
		tagLabel  + 
		//		";\tmicp;\t"+errors.getMicroAveragePrecision(targets)+
		//		";\tmicr;\t"+errors.getMicroAverageRecall(targets)+
		//		";\tmacp;\t"+errors.getMacroAveragePrecision(targets)+
		//		";\tmacr;\t"+errors.getMacroAverageRecall(targets)+
		//		";\tprecPOS;\t"+errors.getPrecision(Sentiment.POSITIVE)+";\trecPOS;\t"+errors.getRecall(Sentiment.POSITIVE)+
		//		";\tprecNEG;\t"+errors.getPrecision(Sentiment.NEGATIVE)+";\trecNEG;\t"+errors.getRecall(Sentiment.NEGATIVE);
		" &\t"+errors.getMicroAveragePrecision(targets)+
		" &\t"+errors.getMicroAverageRecall(targets)+
		" &\t"+errors.getMacroAveragePrecision(targets)+
		" &\t"+errors.getMacroAverageRecall(targets)+
		" &\t"+errors.getPrecision(Sentiment.POSITIVE)+
		" &\t"+errors.getRecall(Sentiment.POSITIVE)+
		" &\t"+errors.getPrecision(Sentiment.NEGATIVE)+
		" &\t"+errors.getRecall(Sentiment.NEGATIVE)+
		"\\\\ \n";
	}

	public void setTestCondition(String testCondition) {
		this.testCondition = testCondition;
	}

	public static void main (String[] args) throws IOException {
		boolean debug = false;
		boolean accu = false;
		TestPolariser tp;
		if (debug) {
			tp = new TestPolariser();
			String testIdentifier = "monkeylearn";
			//			tp.setTestDirectory("dummy/"+testIdentifier+"/");
			//			tp.setTestFileName("dummy");
			tp.setTestIdentifier(testIdentifier);
			tp.polarise("dummy",false,false,"dummy","dummy", "dummy");
			tp.classify();
			System.out.println(tp.demonstrate());
			System.exit(0);
		}
		Writer out = new BufferedWriter(new FileWriter(new File("/home/jussi/Desktop/testpolariser.outfile")));
		//		boolean n = false,v = false,v2 = false, a = false,h = false, m = false, b = false, esa = false;
		String[] algos = {"base", "cont", "reverse"}; //,"esa"}; //{"base","but","cont","reverse","v","esa"}; //{"cont","base","reverse","v"}; //,"disc","base","esa"};
		String[] negations = {"noneg"}; //{"skip","flip","noneg"};
		String[] lexica = {"bingliu"}; //,"gavagai"};// {"mini","bingliu","gavagai"}; 
		String[] tests = {"replab","oscar","stanford"};
		boolean[] bb = {false, true};
		for (String lexicon: lexica) 
		for (String algo: algos) {
			for (String negation: negations) {
				for (boolean b : bb)
					for (boolean v: bb) {
						if (v && algo.equals("cont")) continue;
						if (v && algo.equals("reverse")) continue;				
//					if (algo.equals("esa") && negation.equals("noneg")) continue;
//					if (algo.equals("esa") && negation.equals("neg")) negation = "";
					String tag = lexicon+" "+algo+" "+negation+" "+ b + " "+v;
					out.write(tag+"\n");
					System.out.println(tag);
					tp = new TestPolariser();
					for (String testIdentifier: tests) {
						//						tp.setTestDirectory("/bigdata/evaluation/sentiment.polarization/en/"+testIdentifier+"/");
						tp.setTestIdentifier(testIdentifier);
						tp.polarise(algo, b, v, lexicon, negation ,tag);
						tp.classify();
						if (! accu) {
							out.write(tp.demonstrate());
							out.write("\n");
							out.flush();
							tp = new TestPolariser();
						}
					}
					//					tp.polarise(algo, lexicon, negation ,tag);
					out.write(tp.demonstrate());
					out.write("\n");
					out.flush();
					tp = new TestPolariser();
				}
			}
		}
		out.close();
	}
}
