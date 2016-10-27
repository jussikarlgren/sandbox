package com.gavagai.jussisandbox;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Vector;

import com.gavagai.vectorgarden.SparseVector;
import com.gavagai.vectorgarden.VectorMath;

public class GeoLocator {
	public Vector<SparseVector> places;
	public HashMap<String,SparseVector> lexicon;

	public GeoLocator() {
		places = new Vector<SparseVector>();
		lexicon = new HashMap<String,SparseVector>();
	}

	public String findPlace(SparseVector f) {
		String place = "nowhere";
		float max = 0f;
		for (SparseVector p: places) {
			float d = VectorMath.cosineSimilarity(f.canonicalVector(), p.canonicalVector());
			if (d > max) {max = d; place = p.getToken()+"("+Math.round(d*100)/100.0+")";}
		}
		return place;
	}
	public static void main(String[] args) throws Exception {
		String w = "stockholm";
		GeoLocator g = new GeoLocator();
		//		final Properties esProperties = new Properties();
		//		esProperties.setProperty("test", "association");
		//		esProperties.setProperty("wordspace", "2");
		//		esProperties.setProperty("userid","monitor");
		//		esProperties.setProperty("password","monitor");
		//		esProperties.setProperty("host","core3.gavagai.se");
		//		esProperties.setProperty("ear","core3");
		//GetVectorsForWord vectorGetter = new GetVectorsForWord(esProperties,true);
		//float[] f = vectorGetter.getAssociationVector(w);
		Scanner fileScanner;
		fileScanner = new Scanner(new BufferedInputStream(new FileInputStream("/Users/jussi/Desktop/2014.geo/orter.wordspace")));
		while (fileScanner.hasNextLine()) {
			String fileLine = fileScanner.nextLine();
			SparseVector sv = new SparseVector();
			sv.parseLispString(fileLine,5,3); // tok ind dir doc freq
			g.places.add(sv);
		}
		fileScanner.close();
		fileScanner = new Scanner(new BufferedInputStream(new FileInputStream("/Users/jussi/Desktop/2014.geo/hundra.wordspace")));
		while (fileScanner.hasNextLine()) {
			String fileLine = fileScanner.nextLine();
			SparseVector sv = new SparseVector();
			sv.parseLispString(fileLine,5,3); // tok ind dir doc freq
//			System.out.println(sv.toFullString());
			g.lexicon.put(sv.getToken(),sv);
		}
		fileScanner.close();
		fileScanner = new Scanner(new BufferedInputStream(new FileInputStream("/Users/jussi/Desktop/2014.geo/hundra.list")));
//		while (fileScanner.hasNextLine()) {
			String[] probes = {"södermalm", "kth", "norrmalm", "slussen", "skansen", "liseberg", "nytorget", "enskede", "majorna", "haga", "masthugget", "hisingen", "gamlestaden", "vasaplatsen", "stadsgården", "tunnelbanan", "tunnelbana", "spårvagnen", "tricken", "skeppsbron", "munkbron", "stadshuset", "riksdagen", "nattbuss", "förorten", "orten"};
			for (String fileLine: probes) { //fileScanner.nextLine();
			System.out.println(fileLine);
			String[] words = fileLine.split(" ");
			for (String ss: words) {
				System.out.print(" . ");
				if (g.lexicon.containsKey(ss)) {
					System.out.print(g.findPlace(g.lexicon.get(ss))+" ");
				}
			}
			System.out.println();
//		}
		fileScanner.close();
	}
}
}
