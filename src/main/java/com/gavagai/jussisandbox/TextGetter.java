package com.gavagai.jussisandbox;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

import com.gavagai.jussiutil.Sentiment;
import com.gavagai.jussiutil.TextWithSentiment;
import com.gavagai.mockrabbit.Pole;
import com.gavagai.mockrabbit.PolePart;

public class TextGetter {
	private static int BUCKETSIZE = 1000000;

	private static String PREFIX =   "/home/jussi/source/"; //"/Users/jussi/aktuellt/Gavagai/"; 
	public static Hashtable<Integer,Pole> readPoles() {
		Hashtable<Integer,Pole> poles = new Hashtable<Integer,Pole>();
		poles.put(801,readAPole("pos",801,PREFIX+"rabbit-data/poles/en/enposBingLiu.list"));
		poles.put(802,readAPole("neg",802,PREFIX+"rabbit-data/poles/en/ennegBingLiu.list"));
		poles.put(101,readAPole("pos",101,PREFIX+"rabbit-data/poles/en/enposMINI.list"));
		poles.put(101,readAPole("neg",102,PREFIX+"rabbit-data/poles/en/ennegMINI.list"));
		poles.put(41,readAPole("pos",41,PREFIX+"rabbit-data/poles/en/enpos41.list"));
		poles.put(42,readAPole("neg",42,PREFIX+"rabbit-data/poles/en/enneg42.list"));
		poles.put(637,readAPole("boring",637,PREFIX+"rabbit-data/poles/en/enboring637.list"));
		poles.put(702,readAPole("skeptic",702,PREFIX+"rabbit-data/poles/en/enskeptic702.list"));
		poles.put(703,readAPole("fear",703,PREFIX+"rabbit-data/poles/en/enfear703.list"));
		poles.put(705,readAPole("hate",705,PREFIX+"rabbit-data/poles/en/enhate705.list"));
		poles.put(704,readAPole("love",704,PREFIX+"rabbit-data/poles/en/enlove704.list"));
		poles.put(29,readAPole("sexy",29,PREFIX+"rabbit-data/poles/en/ensexy29.list"));
		poles.put(98,readAPole("violent",98,PREFIX+"rabbit-data/poles/en/enbang98.list"));
		poles.put(764,readAPole("profanity",764,PREFIX+"rabbit-data/poles/en/enprofanity764.list"));
		poles.put(107,readAPole("negation",107,PREFIX+"rabbit-data/poles/en/ennegation107.list"));
		poles.put(999,readAPole("amplify",242,PREFIX+"rabbit-data/poles/en/enamplify242.list"));		
		poles.put(909,readAPole("subordinators",909,PREFIX+"rabbit-data/poles/en/ensubordinator909.list"));		
		return poles;
	}
	private static Pole readAPole(String name, int id, String filename) {
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
//			System.err.println("Not found: "+filename);
		}
		aPole.setMembers(members);
		return aPole;
	}

	
	public static Vector<TextWithSentiment> getTexts(String testIdentifier) throws IOException {
//		String testFileNamePath = "/bigdata/evaluation/sentiment.polarization/en/"+testIdentifier+"/";
		String testFileNamePath = "/home/jussi/evaluation/sentiment.polarization/en/"+testIdentifier+"/";
		Vector<TextWithSentiment> bucket = new Vector<TextWithSentiment>();
		if (testIdentifier.equals("oscar")) {
			testFileNamePath += "rvw-en-finegrained.txt";
			bucket = getOscarFragments(new File(testFileNamePath));
		} else if (testIdentifier.equals("monkeylearn")) {
			testFileNamePath += "monkeylearn.tsv";
			bucket = getMonkeyLearn(new File(testFileNamePath));
		} else if (testIdentifier.equals("stanford")) {
			testFileNamePath += "sentences_clean.txt";
			File file = new File(testFileNamePath);
			bucket = getStanfordSentences(file);
		} else if (testIdentifier.equals("replab")) {
			testFileNamePath += "replab2013-noquotes.txt";
			bucket = getRepLabFragments(new File(testFileNamePath));
		} else {
		testFileNamePath += "mini.txt";
		bucket = getRepLabFragments(new File(testFileNamePath));
	}
		return bucket;
	}

	private static Vector<TextWithSentiment> getOscarFragments(File testFile) throws FileNotFoundException {
		Vector<TextWithSentiment> vec = new Vector<TextWithSentiment>();
		Scanner fileScanner = new Scanner(new BufferedInputStream(
				new FileInputStream(testFile)));
		while (fileScanner.hasNextLine()) {
			String fileLine = fileScanner.nextLine();
			String bits[] = new String[2];
			bits = fileLine.split("\t");
			if (bits.length > 1) {
				if (bits[0].equalsIgnoreCase("pos")) {
					vec.add(new TextWithSentiment(Sentiment.POSITIVE,bits[1]));
				}
				if (bits[0].equalsIgnoreCase("neg")) {
					vec.add(new TextWithSentiment(Sentiment.NEGATIVE,bits[1]));					
				}
				if (bits[0].equalsIgnoreCase("neu")) {
					vec.add(new TextWithSentiment(Sentiment.NEUTRAL,bits[1]));
				}
				if (bits[0].equalsIgnoreCase("mix")) {
					vec.add(new TextWithSentiment(Sentiment.OTHER,bits[1]));
				}
				if (bits[0].equalsIgnoreCase("nr")) {
					vec.add(new TextWithSentiment(Sentiment.OTHER,bits[1]));
				}
			}
		}
		fileScanner.close();
		return vec;
	}
	private static Vector<TextWithSentiment> getStanfordSentences(File testFile) throws FileNotFoundException {
		Vector<TextWithSentiment> vec = new Vector<TextWithSentiment>();
		Scanner fileScanner = new Scanner(new BufferedInputStream(
				new FileInputStream(testFile)));
		while (fileScanner.hasNextLine()) {
			String fileLine = fileScanner.nextLine();
			String bits[] = new String[2];
			bits = fileLine.split("\t");
			if (bits.length > 1) {
				float score = Float.parseFloat(bits[1]);
				if (score > 0.6) {vec.add(new TextWithSentiment(Sentiment.POSITIVE,bits[0]));}
				if (score <= 0.4) {vec.add(new TextWithSentiment(Sentiment.NEGATIVE,bits[0]));}
				if (score > 0.4 && score <= 0.6) {vec.add(new TextWithSentiment(Sentiment.NEUTRAL,bits[0]));}
			}
		}
		fileScanner.close();
		return vec;
	}
	private static Vector<TextWithSentiment> getRepLabFragments(File testFile) throws FileNotFoundException {
		Vector<TextWithSentiment> vec = new Vector<TextWithSentiment>();
		FileInputStream fis = new FileInputStream(testFile);
		Scanner fileScanner = new Scanner(new BufferedInputStream(fis));
		while (fileScanner.hasNextLine()) {
			String fileLine = fileScanner.nextLine();
			String bits[] = fileLine.split("\t");
			if (bits.length > 5) {
				TextWithSentiment t = new TextWithSentiment(bits[6], bits[4]);
				vec.add(t);
			}
		}
		Vector <TextWithSentiment> vecc = prune(vec,BUCKETSIZE);
		fileScanner.close();
		return vecc;
	}
	protected static <T> Vector<T> prune(Vector<T> bigbucket,
			int targetsize) {
		if (bigbucket.size() < targetsize) {
			return bigbucket;
		} else {
			Vector<T> smallbucket = new Vector<T>(
					targetsize);
			while (smallbucket.size() < targetsize && bigbucket.size() > targetsize - smallbucket.size()) {
				T d = bigbucket.remove(((int) Math.round(Math
						.random() * (bigbucket.size() - 1))));
				smallbucket.add(d);
			}
			return smallbucket;
		}
	}
	
	public static Vector<TextWithSentiment> getMonkeyLearn(File filename) {
		Vector<TextWithSentiment> bucket = new Vector<TextWithSentiment>();
		try {
			Scanner fileScanner = new Scanner(new BufferedInputStream(new FileInputStream(filename)));
			while (fileScanner.hasNextLine()) {
				String fileLine = fileScanner.nextLine();
				String bits[] = new String[2];
				bits = fileLine.split(";");
				if (bits.length > 1) {
					bucket.add(new TextWithSentiment(bits[1].replace(" ", ""), bits[0]));
				}

			}
			fileScanner.close();
		} catch (FileNotFoundException e) {
			bucket.add(new TextWithSentiment(Sentiment.NEGATIVE, "this is an awful bloody disaster and so much stress and hate and fear and loathing"));
			bucket.add(new TextWithSentiment(Sentiment.NEUTRAL, "this is an event but it happened so so what"));
			bucket.add(new TextWithSentiment(Sentiment.POSITIVE, "this is an awful bloody disaster but so much fun and games woot woot woot"));
		}
		return bucket;
	}

}
