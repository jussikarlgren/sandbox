package com.gavagai.logonaut;

import java.util.Vector;

public class Fonotax {

	Vector<Character> vowels;
	Vector<Character> plosives;
	Vector<Character> liquids;
	Vector<Character> nasals;
	String language;
	public Fonotax(String l) {
		this.language = l;
		vowels = new Vector<Character>();
		liquids = new Vector<Character>();
		plosives = new Vector<Character>();
		nasals = new Vector<Character>();
		vowels.add('a');
		vowels.add('e');
		vowels.add('i');
		vowels.add('o');
		vowels.add('u');
		if (language.equals("SV")) {
			vowels.add('y');
			vowels.add('å');
			vowels.add('ä');
			vowels.add('ö');
			liquids.add('j');
		}
		if (language.equals("EN")) {
			liquids.add('y');
			plosives.add('j');
		}
		plosives.add('q'); // qu
		plosives.add('p');
		plosives.add('k');
		plosives.add('t');
		plosives.add('b');
		plosives.add('g');
		plosives.add('d');
		plosives.add('x');
		liquids.add('w');
		liquids.add('r');
		liquids.add('l');
		liquids.add('s');
		liquids.add('c');
		liquids.add('z');
		liquids.add('f');
		liquids.add('v');
		nasals.add('m'); // ng
		nasals.add('n');
		// h is for free
	}
	private int weight(char c) {
		if (vowels.contains(c)) {return 1;}
		if (plosives.contains(c)) {return 3;}
		if (liquids.contains(c)) {return 2;}		
		if (nasals.contains(c)) {return 2;}	
		return 0;
	}
	//	public int cost(String syllable) {
	//		int d = 0;
	//		char[] cs = syllable.toCharArray();
	//		for (char c: cs) {
	//			d += weight(c);
	//		}
	//		return d;		
	//	}
	public int cost(String syllable) {
		int d = 0;
		char[] cs = syllable.toCharArray();
		boolean invowelsequence = false;
		for (char c: cs) {
			if (vowels.contains(c)) {if (! invowelsequence) d++; invowelsequence = true;} else {invowelsequence = false;}
		}
		return d;		
	}

	boolean nucleus(char c) {
		if (vowels.contains(c)) {return true;} else {return false;}
	}
	private enum State {before, inside, after, split};
	public String[] syllables(String word) {
		Vector<String> s = new Vector<String>();
		int prev = 0;
		Vector<Integer> splits = new Vector<Integer>();
		splits.add(0);
		State state = State.before;
		for (int i=0;i<word.length();i++) {
			if (nucleus(word.charAt(i))) {
				if (state == State.before) {
					state = State.inside;
				}
				if (state == State.inside) {
					// nop
				}
				if (state == State.after) {
					state = State.split;
					splits.add(1-prev+1 / 2 + prev); // Math.floorDiv(i-prev+1, 2)+prev);
				}
				if (state == State.split) {
					// nop
				}
			} else {
				if (state == State.before) {
					// nop
				}
				if (state == State.inside) {
					state = State.after;	
					prev = i;
				}
				if (state == State.after) {
					// nop
				}
				if (state == State.split) {
					state = State.after;	
					prev = i;
				}		
			}
		}
		splits.add(word.length());
		for (int i = 0; i < splits.size(); i++) {
			for (int j = i+1; j < splits.size(); j++) {
				s.add(word.substring(splits.get(i), splits.get(j)));
			}
		}
		String[] ss = new String[s.size()]; 
		ss = (String[]) s.toArray(ss);
		return ss;
	}

	public static void main(String[] args) {
		Fonotax f = new Fonotax("SV");
		String[] ss = f.syllables("sjukskrivningsmiljarden");
		for (String s: ss) {System.out.println(s);}
	}
	public int difference(char c, char c2) {
		if (c == c2) return 0;
		if (vowels.contains(c) && vowels.contains(c2)) {return 1;}
		if (plosives.contains(c) && plosives.contains(c2)) {return 2;}
		if (liquids.contains(c) && liquids.contains(c2)) {return 2;}		
		if (nasals.contains(c) && nasals.contains(c2)) {return 2;}	
		return 3;
	}
}
