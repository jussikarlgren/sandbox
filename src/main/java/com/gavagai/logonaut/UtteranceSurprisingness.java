package com.gavagai.logonaut;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.Scanner;
import java.util.Vector;

import com.gavagai.vectorgarden.GetVectorsForWord;
import com.gavagai.vectorgarden.VectorMath;

public class UtteranceSurprisingness {

//	private Log logger = LogFactory.getLog(UtteranceSurprisingness.class);
	private Properties environment;
	private GetVectorsForWord g;
//	private GetPolesForWord p;

	public UtteranceSurprisingness(Properties environment) {
		this.environment = environment;
		g = new GetVectorsForWord(environment);
//		p = new GetPolesForWord(environment);
	}

	public float gradeUtterance(String[] utterance) {
		//		Vector<String> submit = new Vector<String>();
		//		for (String s: utterance) { submit.add(s.toLowerCase());}
		Vector<float[]> vectors = new Vector<float[]>();
		float d = 0f;
		try {
			g = new GetVectorsForWord(environment);
			for (String s: utterance) { vectors.add(g.getAssociationVector(s));}
			float[] dd = VectorMath.centroid(vectors);
			for (float[] ff: vectors) {d += VectorMath.distance(ff, dd)*VectorMath.sdev(dd, vectors);}
//			for (String s: utterance) { 
//				float[] idx = g.getIndexVector(s);
//				float[] ctx = g.getContextVector(s);
//				d += VectorMath.cosineSimilarity(idx, VectorMath.permute(-1,ctx));
//				}
		} catch (Exception e) {
			
		}
		if (utterance.length > 0) d = d / utterance.length;
		return d;
	}	

	public static void main(String[] args) {
		Properties environment = new Properties();
		environment.setProperty("test", "association");
		environment.setProperty("wordspace", "1"); 
		environment.setProperty("userid","monitor");
		environment.setProperty("password","monitor");
		environment.setProperty("host","core5.gavagai.se");
		environment.setProperty("ear","rabbit-core");
try {
		UtteranceSurprisingness spbt = new UtteranceSurprisingness(environment);
		Scanner fileScanner = new Scanner(new BufferedInputStream(new FileInputStream("/home/jussi/Desktop/coconut.txt")));
		while (fileScanner.hasNextLine()) {
			String sentence = fileScanner.nextLine();
			String[] utterance = sentence.split(" ");
				System.out.println(sentence+": "+spbt.gradeUtterance(utterance));
			}
		fileScanner.close();
} catch (Exception e) {
	e.printStackTrace();
}		


	}
}
