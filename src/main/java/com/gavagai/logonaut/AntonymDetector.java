package com.gavagai.logonaut;

import java.io.FileNotFoundException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import com.gavagai.vectorgarden.GetVectorsForWord;
import com.gavagai.vectorgarden.VectorMath;

public class AntonymDetector {

	private GetVectorsForWord vectorGetter;
	private Vector<SemanticAxle> dimensions;
	private Properties properties;

	public AntonymDetector(Properties properties) throws Exception {
		this.properties = properties;
		vectorGetter = new GetVectorsForWord(properties,true);
		setMockup();
		dimensions = new Vector<SemanticAxle>();
		addAxle("north","south");
		addAxle("hot","cold");
		addAxle("small", "large");
		addAxle("big","little");
		addAxle("up","down");		
		addAxle("old","new");		
		addAxle("good","bad");		
		addAxle("true","false");		
		addAxle("before","after");		
		addAxle("black","white");		
		addAxle("in","out");		
		addAxle("inside","outside");
		addAxle("dead","alive");
		addAxle("nice","evil");
	}

	private class SemanticAxle {
		String north;
		String south;
		float[] nv;
		float[] sv;
		float axis;
		public Triangle triangulate(float[] w) {
			Triangle t = new Triangle(north+" "+south,VectorMath.cosineSimilarity(w,nv), VectorMath.cosineSimilarity(w,sv)); return t;
		}
		public float top(float[] w) {
			float nn = VectorMath.cosineSimilarity(w,nv);
			float ss = VectorMath.cosineSimilarity(w,sv);
			if (nn > ss && nn > 0.1 && nn - ss > 0.1) {return nn-ss; } //  1;} 
			if (ss > nn && ss > 0.1 && ss - nn > 0.1) {return nn-ss; } // -1;}
			return 0f;
		}
		public String toString() {return north+"-"+"-"+south;}
	}

	private class Triangle {
		public Triangle(String l, float n, float s) {
			dn = n;
			ds = s;
			label = l;
		}
		String label;
		float dn;
		float ds;
		public String toString() {float d = Math.abs(dn-ds); return label+" "+dn+" "+ds;} 
	}

	private void addAxle(String n, String s) throws Exception {
		SemanticAxle newAxle = new SemanticAxle(); newAxle.north = n; newAxle.south = s; 
		newAxle.nv = vectorGetter.getContextVector(n);
		newAxle.sv = vectorGetter.getContextVector(s);
		newAxle.axis = VectorMath.cosineSimilarity(newAxle.nv,newAxle.sv);
		dimensions.add(newAxle);
	}

	public String triangulate(String probe, String[] candidates) throws Exception {
		float[] fv1 = vectorGetter.getContextVector(probe);
		Hashtable<String,float[]>fvs = new Hashtable<String,float[]>();
		Hashtable<String,Float>ds = new Hashtable<String,Float>();
		for (String s: candidates) {
			fvs.put(s,vectorGetter.getContextVector(s));
			ds.put(s,VectorMath.cosineSimilarity(fvs.get(s),fv1));
		}
		for (SemanticAxle a: dimensions) {
			System.out.println(probe+" "+a.triangulate(fv1));
			for (String s: fvs.keySet()) {
				System.out.println(probe+"<-("+ds.get(s)+")->"+s+" "+a.triangulate(fvs.get(s)));
			}	
		}
		return "";
	}

	public boolean flipflop(Hashtable<String,float[]> fvs) throws Exception {
		int nn=0, ss=0;
		for (SemanticAxle a: dimensions) {
			float p = 0;
			for (String cf: fvs.keySet()) {
				p += a.top(fvs.get(cf));
				System.out.println(a+" "+p);
			}	
			if (p > 0) {nn++;} else {ss++;}
		}
		return nn > ss;
	}

	public void setMockup() {
		vectorGetter.setMockup(true);
		
	}

	// topical
	//cold={"balm", "temperatures", "sodastream's", "wxia", "keurig", "single-serving", "weather", "carbonation", "coke's", "tavegyl", "lithotabs", "temperature", "warm", "bicalox", "countertop", "atlanta-based", "impri", "cefotaxime", "fevarin"};
	//hot={"tubs", "potholder", "weelists", "tub", "ravelry", "warm", "great", "enjoyed", "pf", "favorite", "enough", "something", "week", "potholders", "find", "thanks", "editor", "trivets", "scroller"};
	//warmer={"four-letter", "conked", "mcgarrigle", "back-breaking", "arriaga", "high-riding", "twice-weekly", "scentsy", "flutterby", "humvees", "randal", "temperatures", "digests", "patrolled", "godawful", "unknowns", "dishneau", "reveling", "doldrums"};
	//chilly={"delhiites", "loke", "chole", "notches", "enns", "kadhai", "2tsp", "1tsp", "chann", "chana", "concoct", "chilies", "masala", "pvs", "pindi", "tsp", "corriander", "movie-lovers", "bzh"};
	//frigid={"leaf-miner", "cryogenically", "perishables", "747s", "perishable", "mayerowitz", "corneas", "wide-body", "thump", "shippers", "cargo", "777s", "delta's", "specks", "coolers", "wilted", "sunflowers", "mammatus", "738"};

	// paradigmatic
	//cold={"inclement", "warmer", "chilly", "warm", "stormy", "speedwell", "severe", "wintery", "stormtracker", "wet", "unseasonably", "frigid", "bad", "standoffs", "20-degree", "upside-down", "braved", "kfvs12", "wncn"};
	//warmer={"inclement", "stormy", "speedwell", "cold", "stormtracker", "warm", "severe", "wintery", "chilly", "noaa", "60-degree", "upside-down", "kfvs12", "wncn", "jay's", "mankato", "unseasonably", "20-degree", "wcmh"};
	//chilly={"cold", "warmer", "warm", "frigid", "frosty", "stormy", "inclement", "tomorrow", "early", "sunday", "wet", "braved", "severe", "mild", "monday", "friday", "harsh", "bad", "snowy"};
	//afloat={"abreast", "hydrated", "entertained", "wrinting", "truckin", "tuned", "consitently", "joneses", "germ-free", "indoors", "аway", "apprised", "safe", "awɑy", "utilizing", "warm", "smilin", "mind", "elab"};
	//hot={"drinking", "soapy", "boiling", "bottled", "potable", "kangen", "brackish", "treading", "ionized", "desalinated", "undrinkable", "acre-feet", "fluoridated", "tankless", "drinkable", "distilled", "warm", "chlorinated", "sachet"};
	//frigid={"subzero", "sub-freezing", "below-freezing", "below-zero", "sub-zero", "bone-chilling", "spring-like", "cold", "warmer", "chilly", "warm", "colder-than-normal", "unseasonably", "springlike", "braved", "50-degree", "scorcher", "seasonable", "#extreme", "


	public void test(String w) throws Exception {
		GetNeighboursForWord gnfw = new GetNeighboursForWord(properties, true);
		setMockup();
		Hashtable<String,String[]> cfvs = new Hashtable<String,String[]>();
		Hashtable<String,float[]> fvs = new Hashtable<String,float[]>();
		//		for (String a: gnfw.getParadigmaticNeighbours(w)) {
		String[] selectedWords = {"cold","hot","warmer","chilly","frigid"}; //,"freezing","tepid","balmy","vibrant"};
		for (String a: selectedWords) {
			System.out.print(a+"\t");
			String[] bs = gnfw.getTopicalNeighbours(a);
			cfvs.put(a, bs);
			for (String b: bs) {
				System.out.print(b+" ");
				if (! fvs.containsKey(b)) {
					fvs.put(b,vectorGetter.getContextVector(b));
				}
			}
			System.out.println();
		}
		for (SemanticAxle axel: dimensions) { // each axis
			int nn=0, ss=0;
			Vector<String> northern = new Vector<String>();
			Vector<String> southern = new Vector<String>();
			for (String c: cfvs.keySet()) { // each neighbour
				float p = 0;
				for (String cf: cfvs.get(c)) {
					p += axel.top(fvs.get(cf));
				}	
				if (p > 0) {nn++; northern.add(c);} else if (p < 0) {ss++; southern.add(c);}
			}
			//		System.out.println();
			System.out.print(axel+" "+nn+" "+ss);
			int threshold = 0;
			if (nn > threshold & ss > threshold) {
				for (String an: northern) {System.out.print(" n:"+an);}
				for (String as: southern) {System.out.print(" s:"+as);}
			}
			System.out.println();
		} 
		// System.out.println();
		//	for (String a: gnfw.getTopicalNeighbours(w)) {System.out.print(a+" ");} System.out.println();		
	}
	public static void main(String[] args) throws FileNotFoundException {
		final Properties esProperties = new Properties();
		esProperties.setProperty("test", "association");
		esProperties.setProperty("wordspace", "1");
		esProperties.setProperty("userid","monitor");
		esProperties.setProperty("password","monitor");
		esProperties.setProperty("host","core3.gavagai.se");
		esProperties.setProperty("ear","core3");

		try {
			AntonymDetector pe = new AntonymDetector(esProperties);

			pe.test("warm");
			//	quick		String[] test = {"slow","greedy","fast","rapid","agile","speedy","black"};
			//	beautiful String[] test = {"cute","adorable","ugly","stylish"};
			//	String[] test = {"cold", "hot", "chilly", "frigid","warmer"};
			//		System.out.println(pe.triangulate("warm",test));
			//	System.out.println(pe.flipflop("quick",test));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}
}
