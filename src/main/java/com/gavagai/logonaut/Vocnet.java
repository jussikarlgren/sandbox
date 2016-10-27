package com.gavagai.logonaut;

import java.util.HashMap;
import java.util.Vector;

public class Vocnet {
	Fonotax f;
	static int EDITTHRESHOLD = 2;
	Vector<String> syllables;
	HashMap<Integer,Vector<Edge>> starts;
	HashMap<Integer,Vector<Edge>> ends;
	static int START = 0;
	static int END = 1;
	static int THRESHOLD = 2;
	int latest = END;
	Vector<Edge> edges;


	public Vocnet(String language) {
		f = new Fonotax(language);
		starts = new HashMap<Integer,Vector<Edge>>();
		ends = new HashMap<Integer,Vector<Edge>>();	

	}
	private class Edge {
		int start;
		int end;
		char c;
		int cost;
		public Edge(int s,char c,int cc) {
			this.start = s; this.c = c; this.cost = cc;
		}
		public Edge(int s,char c,int cc,int e) {
			this.start = s; this.c = c; this.end = e;this.cost = cc;
		}
	}
	private void addIn(int c,Edge e,HashMap<Integer,Vector<Edge>> s) {
		Vector<Edge> v;
		if (s.containsKey(c)) {v = s.get(c); v.add(e); s.put(c, v);} else {v = new Vector<Edge>(); v.add(e); s.put(c,v);}
	}
	private void removeFrom(int c,Edge e,HashMap<Integer,Vector<Edge>> s) {
		Vector<Edge> v;
		if (s.containsKey(c)) {v = s.get(c); v.remove(e); s.put(c, v);}	
	}

	private void introduce(char[] path) {
		int start = START;
		Edge e = null;
		for (char c : path) {
			e.end = latest;
			latest++;
			e = new Edge(start,c,0);
			edges.add(e);
			start = latest;
		}
		e.end = END;
	}
	private void populate() {
		for (Edge e: edges) {
			addIn(e.start,e,starts);
			addIn(e.end,e,ends);
		}
	}
	private void merge() {
		for (int k : starts.keySet()) {
			Vector<Edge> ve = starts.get(k);
			for (Edge e: ve) {
				for (Edge e2: ve) {
					if (e == e2) continue;
					if (e.c == e2.c) {
						removeFrom(e2.end, e2, ends);
						e2.end = e.end;
						addIn(e2.end, e2, ends);	
					}
				}
			}
		}
		for (int k : ends.keySet()) {
			Vector<Edge> ve = ends.get(k);
			for (Edge e: ve) {
				for (Edge e2: ve) {
					if (e == e2) continue;
					if (e.c == e2.c) {
						removeFrom(e2.start, e2, starts);
						e2.start = e.start;
						addIn(e2.start, e2, starts);	
					}
				}
			}
		}
	}
	private void fudge() {
		for (int k : starts.keySet()) {
			Vector<Edge> ve = starts.get(k);
			for (Edge e: ve) {
				for (Edge e2: ve) {
					if (e == e2) continue;
					if (e.c == e2.c) continue;
					int dd = f.difference(e.c,e2.c);
					if (dd < THRESHOLD) {
						Edge e3 = new Edge( e.start,e2.c,dd, e.end);
						Edge e4 = new Edge(e2.start, e.c,dd,e2.end);
						addIn(e2.start, e4, starts);	
						addIn(e2.end, e4, ends);	
						addIn( e.end, e3, ends);	
						addIn( e.start, e3, starts);	
					}
				}
			}
		}
		for (int k : ends.keySet()) {
			Vector<Edge> ve = ends.get(k);
			for (Edge e: ve) {
				for (Edge e2: ve) {
					if (e == e2) continue;
					if (e.c == e2.c) continue;
					int dd = f.difference(e.c,e2.c);
					if (dd < THRESHOLD) {
						Edge e3 = new Edge( e.start,e2.c,dd, e.end);
						Edge e4 = new Edge(e2.start, e.c,dd,e2.end);
						addIn(e2.start, e4, starts);	
						addIn(e2.end, e4, ends);	
						addIn( e.end, e3, ends);	
						addIn( e.start, e3, starts);	
					}
				}
			}
		}
	}
	public void process() {
		for (String syllable: syllables) {
			char[] cs = syllable.toCharArray();
			introduce(cs);
		}
		populate();
		merge();
		fudge();
	}
	public void addStrings(Vector<String> v) {
		for (String word: v) {
			for (String s: f.syllables(word)) { 
				syllables.add(s);
			}
		}
	}
	public static void main(String[] args) {
		String language = "SV";
		Vector<String> cs = new Vector<String>();
		cs.add("karlgren");
		cs.add("karlberg");
		cs.add("berg");
		cs.add("norrberg");
		cs.add("norrgren");
		Vocnet v = new Vocnet(language);
		v.addStrings(cs);
		v.process();
		v.getFriends("karlgren");
	}
	private void getFriends(String string) {
		char[] cs = string.toCharArray();
		System.out.print(string);
		Vector<StringWithCost> ss = follow(cs);	
		for (StringWithCost s : ss) {System.out.print(s+" ");}
		System.out.println();
	}
	private Vector<StringWithCost> follow(char[] cs) {
		Vector<StringWithCost> swcs = new Vector<StringWithCost>();
		int start = START;
		for (char c : cs) {
			if (starts.containsKey(start)) {
				Vector<Edge> ve = starts.get(start);
				for (Edge v: ve) {
					if (v.c == c) {
						
					}
				}
			}
		}
		return swcs;
	}
}