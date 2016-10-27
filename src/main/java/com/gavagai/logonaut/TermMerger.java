package com.gavagai.logonaut;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Vector;

public class TermMerger {
	Vector<String> vocabulary = new Vector<String>();
	public void insertVocabulary(String text) {
		String[] wds = text.toLowerCase().split("\\W");
		for (String w: wds) {
			if (! vocabulary.contains(w)) vocabulary.add(w);
		}
	}
	public String[] getSplits() {	
		CompoundSplitter cs = new CompoundSplitter("EN");
		for (String word: vocabulary) {
			cs.add(word);
			/**		
			run vocnet
			 **/	
		}
		return cs.splits();
	}
	public String[] getSyns() {	
		String[] ss = null;
		GetNeighboursForWord spbt = new GetNeighboursForWord();
		Vector<String> vs = new Vector<String>();
		for (String word: vocabulary) {
			String words = "";
			System.err.print("Getting synonyms for "+word+": ");
			try {	
				String[] f =		spbt.getParadigmaticNeighbours(word);
				for (String fs: f) {
					System.err.print(fs+" ");
					if (vocabulary.contains(fs)) words = words + fs; 
				}
				System.err.println();
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (words.length() > 0) {words = word +":"+words; vs.add(words);}
		}
		ss = new String[vs.size()];
		ss = vs.toArray(ss);
		return ss;
	}

	public static void main(String[] args) throws FileNotFoundException {
		Scanner in = new Scanner(System.in);
		TermMerger tm = new TermMerger();
		String inline = "   ";
		while (in.hasNextLine() && inline.length() > 2) {
			inline = in.nextLine();
			String[] words = inline.toLowerCase().split("\\W");
			for (String w:words) {
				if (! tm.vocabulary.contains(w)) tm.vocabulary.add(w);
			}
		}
		String[] ss = tm.getSplits();		
		for (String s: ss) {
			System.out.println(s);
		}
		String[] pss = tm.getSyns();		
		for (String s: pss) {
			System.out.println(s);
		}
	}
}
