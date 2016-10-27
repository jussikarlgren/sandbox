package com.gavagai.jussisandbox;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import com.gavagai.jussiutil.HashtableI;
import com.gavagai.jussiutil.OrdMotOrd;
import com.gavagai.jussiutil.Sentiment;
import com.gavagai.jussiutil.TextWithSentiment;
import com.gavagai.mockrabbit.Language;
import com.gavagai.mockrabbit.Pole;
import com.gavagai.mockrabbit.PoleGroup;
import com.gavagai.mockrabbit.Target;
import com.gavagai.mockrabbit.TargetMonitor;
import com.gavagai.mockrabbit.Utterance;
import com.gavagai.mockrabbit.WordSpace;
import com.gavagai.rabbit.language.LanguageAnalyzerWrapper;

public class PositionPolarisationExplorationDataThingy {
	Vector<TextWithSentiment> bucket = new Vector<TextWithSentiment>();

	private Log logger = LogFactory.getLog(PositionPolarisationExplorationDataThingy.class);

	private final int SKIP_STEPS = 3;
	private final int AMPLIFY_STEPS = 3;
	private WordSpace wordSpace;
	private int bucketsize;
	private String lexicon;
	private int maxlength; 
	public boolean negation = false; 
	public boolean hedge = false;	
	public boolean amplifier = false;
	Language language = Language.EN; // default
	Hashtable<Integer,Pole> poles;
	int POSITIVEPOLE;
	int NEGATIVEPOLE;
	int SUBORDINATORPOLE = 909;
	int NEGATIONPOLE = 107;
	int AMPLIFIERPOLE = 999;
	String testIdentifier;
	static String PREFIX = "/home/jussi/Desktop/";
	Hashtable<TextWithSentiment,Hashtable<Pole,int[]>> results;
	int sections;

	public PositionPolarisationExplorationDataThingy() {
		wordSpace = new WordSpace();
		language = Language.EN;
		poles = TextGetter.readPoles();
		wordSpace.setAmplifierPole(poles.get(AMPLIFIERPOLE));
		wordSpace.setNegationPole(poles.get(NEGATIONPOLE));
		bucketsize = 100;
		lexicon = "gavagai";
		POSITIVEPOLE = 41;
		NEGATIVEPOLE = 42;
		maxlength = 0;
		sections = 4;
		results = new Hashtable<TextWithSentiment,Hashtable<Pole,int[]>>();
	}
	public void runTest(String testIdentifier, int bucketsize) throws IOException {
		this.testIdentifier = testIdentifier;
		getTexts(testIdentifier,bucketsize);
		WordSpace wordSpace = new WordSpace();
		LanguageAnalyzerWrapper languageAnalyzerWrapper = new LanguageAnalyzerWrapper();
		if (lexicon.equals("bingliu")) {POSITIVEPOLE = 801; NEGATIVEPOLE = 802;}
		if (lexicon.equals("mini")) {POSITIVEPOLE = 101; NEGATIVEPOLE = 102;}
		if (lexicon.equals("gavagai")) {POSITIVEPOLE = 41; NEGATIVEPOLE = 42;}
		PoleGroup domain = new PoleGroup();
		List<Pole> activepoles = new ArrayList<Pole>();
		activepoles.add(poles.get(POSITIVEPOLE));
		activepoles.add(poles.get(NEGATIVEPOLE));
		activepoles.add(poles.get(SUBORDINATORPOLE)); //i
		activepoles.add(poles.get(NEGATIONPOLE)); //i 
		activepoles.add(poles.get(AMPLIFIERPOLE));  //i
		for (Pole p: activepoles) {
		logger.info("Active pole: "+ p +" "+p.getMembers().size());
		}
		domain.setPoles(activepoles);
		TargetMonitor dummyTargetMonitor = new TargetMonitor();
		dummyTargetMonitor.setTarget(new Target());
		dummyTargetMonitor.setPoleGroup(domain);
		dummyTargetMonitor.setWordSpace(wordSpace);

		for (TextWithSentiment tws: bucket) {
			List<Utterance> utterances = new ArrayList<Utterance>();
			Utterance utterance = new Utterance();
			String utteranceAsText = tws.getText();
			TokenStream stream = (languageAnalyzerWrapper.getAnalyzer(language.name()))
					.tokenStream("thread-local-polarization-stream",new StringReader(utteranceAsText));
			CharTermAttribute termAtt = stream.getAttribute(CharTermAttribute.class);
			stream.reset();
			utterance.setUtteranceText(utteranceAsText);

			List<String> previousTerms = new ArrayList<String>();
			while (stream.incrementToken()) {
				//			String[] terms = utteranceAsText.split("\\s+");
				//			for (String utteranceTerm: terms)
				String utteranceTerm = termAtt.toString();
				String previousTerm = utteranceTerm;
				//			{ StringBuffer utt = new StringBuffer(utteranceTerm);
				//			
				utterance.getTokenizedTerms().add(utteranceTerm);
				//			logger.info(utteranceTerm);
				//		}
				previousTerms.add(0, previousTerm);
				if (previousTerms.size() == 2) {
					previousTerms.remove(previousTerms.size() - 1);
				}
			}			
			stream.close();
			utterances.add(utterance);
			List<String> expandedUtterances = expandUtterances(utterances);
			tws.setTokens(expandedUtterances);
//			logger.info(utterance);
			results.put(tws,generatePolarisationUnSophisticated(expandedUtterances, dummyTargetMonitor));
		}
	}

	public void setBucketsize(int bucketsize) {
		this.bucketsize = bucketsize;
	}
	
	
	
	
	public Hashtable<Pole,int[]> generatePositionPolarisationSections(List<String> expandedUtterances,
			TargetMonitor targetMonitor) {
		Hashtable<Pole,int[]> theseresults = new Hashtable<Pole,int[]>();
		Set<String> negations = new HashSet<String>();
		if (targetMonitor.getWordSpace().getNegationPole() != null) {
			negations = targetMonitor.getWordSpace().getNegationPole().getMembersAsSet();
		}
		Set<String> amplifiers = new HashSet<String>();
		if (targetMonitor.getWordSpace().getAmplifierPole() != null) {
			amplifiers = targetMonitor.getWordSpace().getAmplifierPole().getMembersAsSet();
		}

		for (Pole pole : targetMonitor.getPoleGroup().getPoles()) {
			if (pole == null) continue;
			int length = expandedUtterances.size();
			if (length > maxlength) {maxlength = length;}
			int[] positions = new int[length];
			logger.debug("Starting calculation for pole " + pole+" "+pole.getMembers().size());
			int index = 0;
			int skip = 0;
			int amplify = 0;
			//			int length = expandedUtterances.size();
			Set<String> poleMembers = new HashSet<String>((Set<String>) pole.getMembersAsSet());

			for (String utteranceTerm : expandedUtterances) {
				if (skip > 0 && negation) {
					skip--;
					continue; 
				}
				if (negation && negations.contains(utteranceTerm)) {
					skip = SKIP_STEPS;
					continue;
				}				
				if (amplify > 0) {
					amplify--;
				}
				if (amplifier && amplifiers.contains(utteranceTerm)) {
					amplify = AMPLIFY_STEPS;
				}
				float utteranceTermPoleProximity = (float) getPoleProximity(poleMembers, utteranceTerm);
				if (utteranceTermPoleProximity > 0) {
					positions[index]++;
				}
				if (amplifier) utteranceTermPoleProximity = utteranceTermPoleProximity * (float) (amplify > 0 ? 2 : 1);
				//					poleProximity += utteranceTermPoleProximity;
				index++;

			}
			int[] histogram = new int[sections];
			double midpoint = length / 2.0d;
			double lastbit = 3f * length / 4.0d;
			double firstbit = length / 4.0d;
			int section = 0;
			for (int i = 0; i < length; i++) {
				if (i > firstbit) {section = 1;}
				if (i > midpoint) {section = 2;}
				if (i > lastbit) {section = 3;}
				histogram[section] += positions[i];
			}
			theseresults.put(pole,histogram);
		}
		return theseresults;
	}
	public Hashtable<Pole,int[]> generatePositionPolarisation(List<String> expandedUtterances,
			TargetMonitor targetMonitor) {
		Hashtable<Pole,int[]> theseresults = new Hashtable<Pole,int[]>();
		Set<String> negations = new HashSet<String>();
		if (targetMonitor.getWordSpace().getNegationPole() != null) {
			negations = targetMonitor.getWordSpace().getNegationPole().getMembersAsSet();
		}
		int length = expandedUtterances.size();
		int[] positions = new int[length];
		for (Pole pole : targetMonitor.getPoleGroup().getPoles()) {
			positions = new int[length];
			if (pole == null) continue;
			if (length > maxlength) {maxlength = length;}
			logger.debug("Starting calculation for pole " + pole+" "+pole.getMembers().size());
			int index = 0;
			int skip = 0;
			Set<String> poleMembers = new HashSet<String>((Set<String>) pole.getMembersAsSet());
			for (String utteranceTerm : expandedUtterances) {
				if (skip > 0 && negation) {
					skip--;
					continue; 
				}
				if (negation && negations.contains(utteranceTerm)) {
					skip = SKIP_STEPS;
					continue;
				}				
				float utteranceTermPoleProximity = (float) getPoleProximity(poleMembers, utteranceTerm);
				if (utteranceTermPoleProximity > 0) positions[index]++; else positions[index] = 0;
				index++;
			}
			theseresults.put(pole,positions);
		}
		return theseresults;
	}
	public Hashtable<Pole,int[]> generatePolarisationUnSophisticated(List<String> expandedUtterances,
			TargetMonitor targetMonitor) {
		Hashtable<Pole,int[]> theseresults = new Hashtable<Pole,int[]>();
		int[] positions;
		for (Pole pole : targetMonitor.getPoleGroup().getPoles()) {
			positions = new int[1];
			if (pole == null) continue;
			Set<String> poleMembers = new HashSet<String>((Set<String>) pole.getMembersAsSet());
			for (String utteranceTerm : expandedUtterances) {				
				float utteranceTermPoleProximity = (float) getPoleProximity(poleMembers, utteranceTerm);
				if (utteranceTermPoleProximity > 0) positions[0]++;
			}
			theseresults.put(pole,positions);
		}
		return theseresults;
	}
	public void setNegation(boolean negation) {
		this.negation = negation;
	}

	public void setHedge(boolean hedge) {
		this.hedge = hedge;
	}

	public void setAmplifier(boolean amplifier) {
		this.amplifier = amplifier;
	}

	private double getPoleProximity(Set<String> poleMembers, String utteranceTerm) {
		return poleMembers.contains(utteranceTerm) ? 1 : 0;
	}

	// from PolarizationServiceBase
	protected List<String> expandUtterances(List<Utterance> utterances) {
		List<String> expandedTerms = new ArrayList<String>();

		for (Utterance utterance : utterances) {
			String previousTerm = null;
			for (String utteranceTerm : utterance.getTokenizedTerms()) {
				expandedTerms.add(utteranceTerm);
				if (previousTerm != null) {
					// bigram
					expandedTerms.add(previousTerm + " " + utteranceTerm);
				}
				previousTerm = utteranceTerm;
			}
		}
		return expandedTerms;
	}
	public void getTexts(String ti, int bucketsize) {
		try {
			bucket = TextGetter.getTexts(ti);
			bucket = TextGetter.prune(bucket, bucketsize);
		} catch (IOException e) {
			logger.error(e);
			logger.error(ti);
		}
	}


	private String demonstrate() {
		String r = "";
		HashtableI allwords = new HashtableI();
		HashtableI midwordsUp = new HashtableI();
		HashtableI midwordsBu = new HashtableI();
		HashtableI nearwords = new HashtableI();
		boolean thispos = false;
		boolean thisneg = false;
		boolean thisthispos = false;
		boolean thisthisneg = false;
		int[] thisposres = null;
		int[] thisnegres = null;
		int midwordsAntalBu = 0;
		int midwordsAntalUp = 0;
		int allwordsAntal = 0;
		int nearwordsAntal = 0;
		for (TextWithSentiment o: results.keySet()) {
			thispos = false;
			thisneg = false;
			thisthispos = false;
			thisthisneg = false;
			thisposres = null;
			thisnegres = null;					
			for (Pole p: results.get(o).keySet()) {
				if (p.getId() == POSITIVEPOLE) {
					thisposres = results.get(o).get(p);
				}
				if (p.getId() == NEGATIVEPOLE) {
					thisnegres = results.get(o).get(p);
				}
			}
			for (int ii: thisposres) {if (ii > 0) thispos = true;}
			for (int ii: thisnegres) {if (ii > 0) thisneg = true;}
			for (int i = 0; i < o.getTokens().size(); i++) {
				String ss = o.getTokens().get(i);
				if (ss.contains(" ")) continue;
				allwords.increment(ss);
				allwordsAntal++;
				if (thispos && thisneg) {
					nearwords.increment(ss);
					nearwordsAntal++;
				}
				if (thisposres[i] > 0) thisthispos = true;
				else if (thisnegres[i] > 0) thisthisneg = true;
				else if (thispos && thisneg) {
					if (thisthispos && ! thisthisneg) {midwordsBu.increment(ss); midwordsAntalBu++;}
					if (thisthisneg && ! thisthispos) {midwordsUp.increment(ss); midwordsAntalUp++;}
				}
			}
		}
		OrdMotOrd oo = new OrdMotOrd(2,2);
		for (Object s: midwordsUp.keySet()) {
			oo.inferMatrix(midwordsUp.get(s), allwords.get(s), midwordsAntalUp, allwordsAntal);
			double x2 = oo.khi2();
			String sep = midwordsUp.get(s)>oo.getIdeal()[0][0]?">":"<";
			if (x2 > 1) r += x2 + " "+ s + " ^ " + midwordsUp.get(s) + sep + Math.round(oo.getIdeal()[0][0]) + " " + allwords.get(s) + " " +lexicon+" "+testIdentifier+" "+"\n";
		}
		for (Object s: midwordsBu.keySet()) {
			oo.inferMatrix(midwordsBu.get(s), allwords.get(s), midwordsAntalBu, allwordsAntal);
			double x2 = oo.khi2();
			String sep = midwordsBu.get(s)>oo.getIdeal()[0][0]?">":"<";
			if (x2 > 1) r += x2 + " "+ s + " v " + midwordsBu.get(s) + sep + Math.round(oo.getIdeal()[0][0]) + " " + allwords.get(s) + " " +lexicon+" "+testIdentifier+" "+"\n";
		}
		return r;	
	}

	private String demonstrateUnSophisticated() {
		int purepos = 0, pureneg = 0, negpos = 0, negposbut = 0, negbut = 0, posbut = 0, onlybut = 0, negposnot = 0, negnot = 0, posnot = 0, onlynot = 0, notbut = 0, negnotbut = 0, posnotbut = 0, negposnotbut = 0;
		int pureposNEG = 0, purenegNEG = 0, negposNEG = 0, negposbutNEG = 0, negbutNEG = 0, posbutNEG = 0, onlybutNEG = 0, negposnotNEG = 0, negnotNEG = 0, posnotNEG = 0, onlynotNEG = 0, notbutNEG = 0, negnotbutNEG = 0, posnotbutNEG = 0, negposnotbutNEG = 0;
		int pureposPOS = 0, purenegPOS = 0, negposPOS = 0, negposbutPOS = 0, negbutPOS = 0, posbutPOS = 0, onlybutPOS = 0, negposnotPOS = 0, negnotPOS = 0, posnotPOS = 0, onlynotPOS = 0, notbutPOS = 0, negnotbutPOS = 0, posnotbutPOS = 0, negposnotbutPOS = 0;
		int thispos = 0;
		int thisneg = 0;
		int thissub = 0;
		int thisamp = 0;
		int thisnot = 0;
		for (TextWithSentiment o: results.keySet()) {
			 thispos = 0;
			 thisneg = 0;
			 thissub = 0;
			 thisamp = 0;
			 thisnot = 0;
			 for (Pole p: results.get(o).keySet()) {
				if (p.getId() == POSITIVEPOLE) {
					for (int ii: results.get(o).get(p)) {thispos += ii;};
				}
				if (p.getId() == NEGATIVEPOLE) {
					for (int ii: results.get(o).get(p)) {thisneg += ii;};
				}
				if (p.getId() == SUBORDINATORPOLE) {
					for (int ii: results.get(o).get(p)) {thissub += ii;};
				}
				if (p.getId() == NEGATIONPOLE) {
					for (int ii: results.get(o).get(p)) {thisnot += ii;};
				}
				if (p.getId() == AMPLIFIERPOLE) {
					for (int ii: results.get(o).get(p)) {thisamp += ii;};
				}
			 }
			 if (thispos  > 0 && thisneg  > 0) {negpos++;}
			 if (thispos  > 0 && thisneg == 0) {purepos++;}
			 if (thispos == 0 && thisneg  > 0) {pureneg++;}
			 if (thispos  > 0 && thisneg  > 0 && thissub  > 0) {negposbut++;}
			 if (thispos  > 0 && thisneg == 0 && thissub  > 0) {posbut++;}
			 if (thispos == 0 && thisneg  > 0 && thissub  > 0) {negbut++;}
			 if (thispos == 0 && thisneg == 0 && thissub  > 0) {onlybut++;}
			 if (thispos  > 0 && thisneg  > 0 && thisnot  > 0) {negposnot++;}
			 if (thispos  > 0 && thisneg == 0 && thisnot  > 0) {posnot++;}
			 if (thispos == 0 && thisneg  > 0 && thisnot  > 0) {negnot++;}
			 if (thispos == 0 && thisneg == 0 && thisnot  > 0) {onlynot++;}
			 if (thisnot  > 0 && thissub  > 0) {notbut++;}
			 if (thispos == 0 && thisneg  > 0 && thisnot  > 0 && thissub  > 0) {negnotbut++;}
			 if (thispos  > 0 && thisneg == 0 && thisnot  > 0 && thissub  > 0) {posnotbut++;}
			 if (thispos  > 0 && thisneg  > 0 && thisnot  > 0 && thissub  > 0) {negposnotbut++;}
		if (o.getSentiment() == Sentiment.POSITIVE) {
			  if (thispos  > 0 && thisneg  > 0) {negposPOS++;}
			  if (thispos  > 0 && thisneg == 0) {pureposPOS++;}
			  if (thispos == 0 && thisneg  > 0) {purenegPOS++;}
			  if (thispos  > 0 && thisneg  > 0 && thissub  > 0) {negposbutPOS++;}
			  if (thispos  > 0 && thisneg == 0 && thissub  > 0) {posbutPOS++;}
			  if (thispos == 0 && thisneg  > 0 && thissub  > 0) {negbutPOS++;}
			  if (thispos == 0 && thisneg == 0 && thissub  > 0) {onlybutPOS++;}
			  if (thispos  > 0 && thisneg  > 0 && thisnot  > 0) {negposnotPOS++;}
			  if (thispos  > 0 && thisneg == 0 && thisnot  > 0) {posnotPOS++;}
			  if (thispos == 0 && thisneg  > 0 && thisnot  > 0) {negnotPOS++;}
			  if (thispos == 0 && thisneg == 0 && thisnot  > 0) {onlynotPOS++;}
			  if (thisnot  > 0 && thissub  > 0) {notbutPOS++;}
			  if (thispos == 0 && thisneg  > 0 && thisnot  > 0 && thissub  > 0) {negnotbutPOS++;}
			  if (thispos  > 0 && thisneg == 0 && thisnot  > 0 && thissub  > 0) {posnotbutPOS++;}
			  if (thispos  > 0 && thisneg  > 0 && thisnot  > 0 && thissub  > 0) {negposnotbutPOS++;}
		}
		if (o.getSentiment() == Sentiment.NEGATIVE) {
			  if (thispos  > 0 && thisneg  > 0) {negposNEG++;}
			  if (thispos  > 0 && thisneg == 0) {pureposNEG++;}
			  if (thispos == 0 && thisneg  > 0) {purenegNEG++;}
			  if (thispos  > 0 && thisneg  > 0 && thissub  > 0) {negposbutNEG++;}
			  if (thispos  > 0 && thisneg == 0 && thissub  > 0) {posbutNEG++;}
			  if (thispos == 0 && thisneg  > 0 && thissub  > 0) {negbutNEG++;}
			  if (thispos == 0 && thisneg == 0 && thissub  > 0) {onlybutNEG++;}
			  if (thispos  > 0 && thisneg  > 0 && thisnot  > 0) {negposnotNEG++;}
			  if (thispos  > 0 && thisneg == 0 && thisnot  > 0) {posnotNEG++;}
			  if (thispos == 0 && thisneg  > 0 && thisnot  > 0) {negnotNEG++;}
			  if (thispos == 0 && thisneg == 0 && thisnot  > 0) {onlynotNEG++;}
			  if (thisnot  > 0 && thissub  > 0) {notbutNEG++;}
			  if (thispos == 0 && thisneg  > 0 && thisnot  > 0 && thissub  > 0) {negnotbutNEG++;}
			  if (thispos  > 0 && thisneg == 0 && thisnot  > 0 && thissub  > 0) {posnotbutNEG++;}
			  if (thispos  > 0 && thisneg  > 0 && thisnot  > 0 && thissub  > 0) {negposnotbutNEG++;}
		}
		}
		return 
				"purepos: " + purepos + "\n" + "pureneg: " + pureneg + "\n" + "negpos: " + negpos + "\n" + "negposbut: " + negposbut + "\n" + "negbut: " + negbut + "\n" + "posbut: " + posbut + "\n" + "onlybut: " + onlybut + "\n" + "negposnot: " + negposnot + "\n" + "negnot: " + negnot + "\n" + "posnot: " + posnot + "\n" + "onlynot: " + onlynot + "\n" + "notbut: " + notbut + "\n" + "negnotbut: " + negnotbut + "\n" + "posnotbut: " + posnotbut + "\n" + "negposnotbut: " + negposnotbut + "\n\n" +
				"pureposNEG: " + pureposNEG + "\n" + "purenegNEG: " + purenegNEG + "\n" + "negposNEG: " + negposNEG + "\n" + "negposbutNEG: " + negposbutNEG + "\n" + "negbutNEG: " + negbutNEG + "\n" + "posbutNEG: " + posbutNEG + "\n" + "onlybutNEG: " + onlybutNEG + "\n" + "negposnotNEG: " + negposnotNEG + "\n" + "negnotNEG: " + negnotNEG + "\n" + "posnotNEG: " + posnotNEG + "\n" + "onlynotNEG: " + onlynotNEG + "\n" + "notbutNEG: " + notbutNEG + "\n" + "negnotbutNEG: " + negnotbutNEG + "\n" + "posnotbutNEG: " + posnotbutNEG + "\n" + "negposnotbutNEG: " + negposnotbutNEG  + "\n\n" +
				"pureposPOS: " + pureposPOS + "\n" + "purenegPOS: " + purenegPOS + "\n" + "negposPOS: " + negposPOS + "\n" + "negposbutPOS: " + negposbutPOS + "\n" + "negbutPOS: " + negbutPOS + "\n" + "posbutPOS: " + posbutPOS + "\n" + "onlybutPOS: " + onlybutPOS + "\n" + "negposnotPOS: " + negposnotPOS + "\n" + "negnotPOS: " + negnotPOS + "\n" + "posnotPOS: " + posnotPOS + "\n" + "onlynotPOS: " + onlynotPOS + "\n" + "notbutPOS: " + notbutPOS + "\n" + "negnotbutPOS: " + negnotbutPOS + "\n" + "posnotbutPOS: " + posnotbutPOS + "\n" + "negposnotbutPOS: " + negposnotbutPOS  + "\n";

	}

	private String demonstrateSections() {
		String r = "";
		int n = 1;
		int dd = 0;
		int[] tot = new int[sections];
		int[] posres = new int[sections];
		int[] negres = new int[sections];
		int[] thisposres = new int[sections];
		int[] thisnegres = new int[sections];
		int[] rightres = new int[sections];
		int[] wrongres = new int[sections];
		HashMap<String,Integer> hep = new HashMap<String,Integer>(); 
		HashMap<Sentiment,Integer> hop = new HashMap<Sentiment,Integer>(); 
		HashMap<Sentiment,Integer> hup = new HashMap<Sentiment,Integer>(); 
		int thispos = 0;
		int thisneg = 0;
		int bw = 0;
		int br = 0;
		int ii = 0;
		for (TextWithSentiment o: results.keySet()) {
			thispos = 0;
			thisneg = 0;
			ii = 0;
			thisposres = new int[sections];
			thisnegres = new int[sections];
			for (Pole p: results.get(o).keySet()) {
				for (int i=0; i < results.get(o).get(p).length; i++) {
					if (p.getId() == POSITIVEPOLE) {
						thispos += results.get(o).get(p)[i]; 
						thisposres[i] += results.get(o).get(p)[i];
					}
					if (p.getId() == NEGATIVEPOLE) {
						thisneg += results.get(o).get(p)[i];
						thisnegres[i] += results.get(o).get(p)[i];
					}
					ii = i;
				}
			}
			if (thispos > 0 && thisneg > 0) {
				for (int i=0; i <= ii; i++) {
					if (o.getSentiment() == Sentiment.POSITIVE) {
						rightres[i] += thisposres[i];
						wrongres[i] += thisnegres[i];
					}
					if (o.getSentiment() == Sentiment.NEGATIVE) {
						rightres[i] += thisnegres[i];
						wrongres[i] += thisposres[i];
					}
				}
			}
		}
		//		for (TextWithSentiment o: results.keySet()) {
		//			for (Pole p: results.get(o).keySet()) {
		//				for (int i=0; i < results.get(o).get(p).length; i++) {
		//					tot[i] += results.get(o).get(p)[i];
		//					//					if (results.get(o).get(p)[i] > 0)
		//					//					logger.info(p.getId()+" "+i+" "+o+" "+results.get(o).get(p)[i]);
		//					if (p.getId() == POSITIVEPOLE) posres[i] += results.get(o).get(p)[i];
		//					if (p.getId() == NEGATIVEPOLE) negres[i] += results.get(o).get(p)[i];
		//					if (p.getId() == POSITIVEPOLE) thisposres[i] += results.get(o).get(p)[i];
		//					if (p.getId() == NEGATIVEPOLE) thisnegres[i] += results.get(o).get(p)[i];
		//					if (p.getId() == POSITIVEPOLE) thispos += results.get(o).get(p)[i];
		//					if (p.getId() == NEGATIVEPOLE) thisneg += results.get(o).get(p)[i];
		//					ii = i;
		//				}
		//			}
		//			n++;
		//			if (thispos > 0 && thisneg > 0) {
		//				dd++;
		//				if (hop.containsKey(o.getSentiment())) {
		//					int nnn = hop.get(o.getSentiment());
		//					hop.put(o.getSentiment(), nnn+1);
		//				} else {
		//					hop.put(o.getSentiment(), 1);
		//				}		
		//				//				if ((thispos >= thisneg && o.getSentiment() == Sentiment.NEGATIVE) || 
		//				//						(thispos <= thisneg && o.getSentiment() == Sentiment.POSITIVE)) {
		//				if (o.getSentiment() == Sentiment.NEGATIVE) {
		//					String s = o.getSentiment()+"\t"+thispos+"\t"+thisneg+"\t";
		//					int pp = 0;
		//					int nn = 0;
		//					boolean isRight = false;
		//					boolean becameRight = false;
		//					boolean isWrong = false;
		//					boolean becameWrong = false;
		//					String t = "-";
		//					for (int i=0; i<=ii;i++) {
		//						pp += thisposres[i];
		//						nn += thisnegres[i];
		//						if (pp > nn) { 
		//							t += "P"; 
		//							if (isWrong == false) {becameWrong = true;becameRight = false;} 
		//							isWrong = true; isRight = false;}
		//						if (pp == nn) {
		//							t += "0"; 
		//							if (isWrong == true) {becameRight = true; becameWrong = false;} 
		//							if (isRight == true) {becameWrong = true; becameRight = false;} 
		//							isWrong = false; 
		//							isRight = false;}
		//						if (pp < nn) { 
		//							t += "N"; 
		//							if (isRight == false) {becameWrong = false;becameRight = true;} 
		//							isWrong = false; isRight = true;}
		//						//						s += i+":"+thisposres[i]+":"+thisnegres[i]+"\t";
		//					}	
		////					logger.info(s+t+"\t"+o.getText());
		//					if (hep.containsKey(t)) {
		//						int nnn = hep.get(t);
		//						hep.put(t, nnn+1);
		//					} else {
		//						hep.put(t, 1);
		//					}					
		//					if (becameWrong) {bw++;}
		//					if (becameRight) {
		//						br++;
		//						if (hup.containsKey(o.getSentiment())) {
		//							int nnn = hup.get(o.getSentiment());
		//							hup.put(o.getSentiment(), nnn+1);
		//						} else {
		//							hup.put(o.getSentiment(), 1);
		//						}	
		//					}
		//				}
		//				if (o.getSentiment() == Sentiment.POSITIVE) {
		//					String s = o.getSentiment()+"\t"+thispos+"\t"+thisneg+"\t";
		//					int pp = 0;
		//					int nn = 0;
		//					String t = "+";
		//					boolean isRight = false;
		//					boolean becameRight = false;
		//					boolean isWrong = false;
		//					boolean becameWrong = false;
		//					for (int i=0; i<=ii;i++) {
		//						pp += thisposres[i];
		//						nn += thisnegres[i];
		//						if (pp < nn) { 
		//							t += "N"; 
		//							if (isWrong == false) {becameWrong = true;becameRight = false;} 
		//							isWrong = true; isRight = false;}
		//						if (pp == nn) {
		//							t += "0"; 
		//							if (isWrong == true) {becameRight = true; becameWrong = false;} 
		//							if (isRight == true) {becameWrong = true; becameRight = false;} 
		//							isWrong = false; 
		//							isRight = false;}
		//						if (pp > nn) { 
		//							t += "P"; 
		//							if (isRight == false) {becameWrong = false;becameRight = true;} 
		//							isWrong = false; isRight = true;}
		//
		//						//						s += i+":"+thisposres[i]+":"+thisnegres[i]+"\t";
		//					}	
		////					logger.info(s+t+"\t"+o.getText());
		//					if (hep.containsKey(t)) {
		//						int nnn = hep.get(t);
		//						hep.put(t, nnn+1);
		//					} else {
		//						hep.put(t, 1);
		//					}					
		//					if (becameWrong) {bw++;}
		//					if (becameRight) {
		//						br++;
		//						if (hup.containsKey(o.getSentiment())) {
		//							int nnn = hup.get(o.getSentiment());
		//							hup.put(o.getSentiment(), nnn+1);
		//						} else {
		//							hup.put(o.getSentiment(), 1);
		//						}	
		//					}
		//				}
		//			}
		//			thispos = 0;
		//			ii = 0;
		//			thisneg = 0;
		//			thisposres = new int[sections];
		//			thisnegres = new int[sections];
		//		}
		//		r += "\n"+testIdentifier+"\tpos & ";
		//		for (int i = 0; i < posres.length; i++) {
		//			//			r += i+":"+posres[i];
		//			r += posres[i];
		//			r += " &\t";
		//		}
		//		r += "\\\\ \n"+testIdentifier+"\tneg & ";
		//		for (int i = 0; i < negres.length; i++) {
		//			//			r += i+":"+negres[i];
		//			r += negres[i];
		//			r += " &\t";
		//		}
		//		r += "\\\\ \n";
		//		//		for (int i = 0; i < tot.length; i++) { // i < maxlength; i++) {
		//		//			if (tot[i] > 0) r += i+":"+tot[i];
		//		//			//			float q = tot[i]/(0f+n);
		//		//			//			r += ":"+q;
		//		//			r += " &\t";
		//		//		}
		//		logger.info(n+" "+r);
		//		String ss = "\n";
		//		for (String t: hep.keySet()) {ss += t+"\t"+hep.get(t)+"\n";}
		//		for (Sentiment t: hop.keySet()) {ss += t+"\t"+hop.get(t)+"\n";}
		//		for (Sentiment t: hup.keySet()) {ss += t+"\t"+hup.get(t)+"\n";}
		//		logger.info(ss);
		//		logger.info("became right: "+br+" became wrong: "+bw+" of "+dd);
		r += "\n"+testIdentifier+"\tright & ";
		for (int i = 0; i < rightres.length; i++) {
			r += rightres[i];
			r += " &\t";
		}
		r += "\\\\ \n"+testIdentifier+"\twrong & ";
		for (int i = 0; i < wrongres.length; i++) {
			r += wrongres[i];
			r += " &\t";
		}
		r += "\\\\ \n";
		logger.info(r);
		return r;
	}
	public static void main (String[] args) throws IOException {
		PositionPolarisationExplorationDataThingy ppedt = new PositionPolarisationExplorationDataThingy();
		//		boolean debug = false;
		//		boolean accu = false;
		Writer out = new BufferedWriter(new FileWriter(new File(ppedt.PREFIX+"unsophisticatedpolarisation.outfile")));
		String[] lexica = {"bingliu"}; //{"mini","bingliu","gavagai"}; 
		String[] tests = {"oscar","replab","stanford"};
//		String[] tests = {"mini"};
		for (String lexicon: lexica) {
			for (String testIdentifier: tests) {
				String tag = lexicon+" "+testIdentifier;
				out.write(tag+"\n");
				System.out.println(tag);
				ppedt.setLexicon(lexicon);
				ppedt.runTest(testIdentifier,10000);
				out.write(ppedt.demonstrateUnSophisticated());
				out.write("\n");
				out.flush();
				ppedt = new PositionPolarisationExplorationDataThingy();
			}
		}
		out.close();
	}

	private void setLexicon(String lexicon2) {
		lexicon = lexicon2;		
	}
}

