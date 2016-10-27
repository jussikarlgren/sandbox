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

import com.gavagai.jussiutil.TextWithSentiment;
import com.gavagai.mockrabbit.Language;
import com.gavagai.mockrabbit.Pole;
import com.gavagai.mockrabbit.PoleGroup;
import com.gavagai.mockrabbit.Target;
import com.gavagai.mockrabbit.TargetMonitor;
import com.gavagai.mockrabbit.Utterance;
import com.gavagai.mockrabbit.WordSpace;
import com.gavagai.rabbit.language.LanguageAnalyzerWrapper;

public class NegationPolarisationExplorationDataThingy {
	Vector<TextWithSentiment> bucket = new Vector<TextWithSentiment>();

	private Log logger = LogFactory.getLog(NegationPolarisationExplorationDataThingy.class);

	private final int SKIP_STEPS = 3;
	private final int AMPLIFY_STEPS = 3;
	private WordSpace wordSpace;
	private int bucketsize;
	private String lexicon;
	private int maxlength; 
	public boolean hedge = false;	
	public boolean amplifier = false;
	Language language = Language.EN; // default
	Hashtable<Integer,Pole> poles;
	int POSITIVEPOLE;
	int NEGATIVEPOLE;
	String testIdentifier;
	static String PREFIX = "/home/jussi/Desktop/";
	Hashtable<TextWithSentiment,Hashtable<Pole,Float>> results;

	public NegationPolarisationExplorationDataThingy() {
		wordSpace = new WordSpace();
		language = Language.EN;
		poles = TextGetter.readPoles();
		wordSpace.setAmplifierPole(poles.get(999));
		wordSpace.setNegationPole(poles.get(-1));
		bucketsize = 100;
		lexicon = "gavagai";
		POSITIVEPOLE = 41;
		NEGATIVEPOLE = 42;
		maxlength = 0;
		results = new Hashtable<TextWithSentiment,Hashtable<Pole,Float>>();
	}
	public void runTest(String testIdentifier, int bucketsize) throws IOException {
		this.testIdentifier = testIdentifier;
		getTexts(testIdentifier,bucketsize);
//		WordSpace wordSpace = new WordSpace();
		LanguageAnalyzerWrapper languageAnalyzerWrapper = new LanguageAnalyzerWrapper();
		if (lexicon.equals("bingliu")) {POSITIVEPOLE = 801; NEGATIVEPOLE = 802;}
		if (lexicon.equals("mini")) {POSITIVEPOLE = 101; NEGATIVEPOLE = 102;}
		if (lexicon.equals("gavagai")) {POSITIVEPOLE = 41; NEGATIVEPOLE = 42;}
		PoleGroup domain = new PoleGroup();
		List<Pole> activepoles = new ArrayList<Pole>();
		activepoles.add(poles.get(POSITIVEPOLE));
		activepoles.add(poles.get(NEGATIVEPOLE));
		logger.info("Positive pole: "+POSITIVEPOLE+" "+poles.get(POSITIVEPOLE).getMembers().size());
		logger.info("Negative pole: "+NEGATIVEPOLE+" "+poles.get(NEGATIVEPOLE).getMembers().size());
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
			results.put(tws,computePolarisationScore(expandedUtterances, dummyTargetMonitor));
		}
	}

	public void setBucketsize(int bucketsize) {
		this.bucketsize = bucketsize;
	}
	
	
	public Hashtable<Pole,Float> computePolarisationScore(List<String> expandedUtterances,
			TargetMonitor targetMonitor) {
		Hashtable<Pole,Float> theseresults = new Hashtable<Pole,Float>();
		Set<String> negations = new HashSet<String>();
		if (targetMonitor.getWordSpace().getNegationPole() != null) {
			negations = targetMonitor.getWordSpace().getNegationPole().getMembersAsSet();
		}
		int length = expandedUtterances.size();
		for (Pole pole : targetMonitor.getPoleGroup().getPoles()) {
			String stretch = "";
			if (pole == null) continue;
			Pole negpole = new Pole();
			negpole.setName(pole.getName()+"+NEGATED");
			negpole.setId(pole.getId()*-1);
			if (length > maxlength) {maxlength = length;}
			float utteranceTermPoleProximity = 0;
			float negatedUtteranceTermPoleProximity = 0;
			logger.debug("Starting calculation for pole " + pole+" "+pole.getMembers().size());
			int skip = 0;
			Set<String> poleMembers = new HashSet<String>((Set<String>) pole.getMembersAsSet());
			for (String utteranceTerm : expandedUtterances) {
				if (skip > 0) {
					skip--;
				}
				if (negations.contains(utteranceTerm)) {
					skip = SKIP_STEPS;
					stretch = ">"+utteranceTerm +": ";
					continue;
				}		
				float f = (float) getPoleProximity(poleMembers, utteranceTerm);
				if (skip > 0) {
					stretch = stretch + " "+utteranceTerm;
					negatedUtteranceTermPoleProximity += f; 
					if (f > 0) {System.out.println(stretch); stretch = "";}
				} else 
					utteranceTermPoleProximity += f;
			}
			theseresults.put(pole,utteranceTermPoleProximity);
			theseresults.put(negpole,negatedUtteranceTermPoleProximity);
		}
		return theseresults;
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

	private String redovisa() {
		float posres = 0;
		float negres = 0;
		float negposres = 0;
		float negnegres = 0;
		float pc = 0;
		float nc = 0;
		float npc = 0;
		float nnc = 0;
		for (TextWithSentiment o: results.keySet()) {
			for (Pole p: results.get(o).keySet()) {
				if (p.getId() == POSITIVEPOLE) {
					posres += results.get(o).get(p);
				}
				if (p.getId() == NEGATIVEPOLE) {
					negres += results.get(o).get(p);
				}
				if (p.getId() == -POSITIVEPOLE) {
					negposres += results.get(o).get(p);
					if (results.get(o).get(p) > 0) System.out.println(o.getText()+" "+o.getSentiment());
				}
				if (p.getId() == -NEGATIVEPOLE) {
					negnegres += results.get(o).get(p);
					if (results.get(o).get(p) > 0) System.out.println(o.getText()+" "+o.getSentiment());
				}
			}
		}
		return posres +" "+ negres +" "+negposres+" "+negnegres+"\n";	
	}

	public static void main (String[] args) throws IOException {
		NegationPolarisationExplorationDataThingy ppedt;
		//		boolean debug = false;
		//		boolean accu = false;
		String[] lexica = {"bingliu"}; //{"mini","bingliu","gavagai"}; 
		String[] tests = {"stanford","oscar","replab"};
		for (String lexicon: lexica) {
			for (String testIdentifier: tests) {
				ppedt = new NegationPolarisationExplorationDataThingy();
				String tag = lexicon+" "+testIdentifier;
				System.out.println(tag);
				ppedt.setLexicon(lexicon);
				ppedt.runTest(testIdentifier,10000);
				System.out.println(ppedt.redovisa());
			}
		}
	}

	private void setLexicon(String lexicon2) {
		lexicon = lexicon2;		
	}
}

