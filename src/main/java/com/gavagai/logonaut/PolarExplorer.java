package com.gavagai.logonaut;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gavagai.vectorgarden.GetVectorsForWord;
import com.gavagai.vectorgarden.SparseVector;
import com.gavagai.vectorgarden.VectorMath;

public class PolarExplorer {
	Vector<SparseVector> vecs;
	Hashtable<Integer, Vector<Edge>> edges;
	Hashtable<String, int[]> valence;
	Integer[] steps = { 90, 75, 50, 30, 10 };
	int MINFREQ = 5;
	static float HORIZON = 0.5f;
	private Properties properties;
	private Log logger = LogFactory.getLog(PolarExplorer.class);

	public class Edge {
		public Edge(String l, String r, double corr) {
			this.left = l;
			this.right = r;
			this.weight = corr;
			this.length = 0;
		}

		String left;
		String right;
		int length;

		public int getLength() {
			return length;
		}

		public void setLength(int length) {
			this.length = length;
		}

		public double weight;

		public String toString() {
			return left + "-" + length + "-" + right;
		}

		public Color getUserDatum(Object demokey) {
			Color other = Color.BLACK;
			int THIN = 1;
			int THICK = 2;
			return Color.LIGHT_GRAY;
		}
	}

	public PolarExplorer(Properties properties) {
		this.properties = properties;
	}

	public PolarExplorer(String filename) throws FileNotFoundException {
		vecs = readFromFile(filename);
	}

	public void centrality() {

	}

	public void info(Object s) {
		System.out.println(s);
	}

	public void debug(Object s) {
		System.err.println(s);
	}

	public static void error(Object s) {
		System.err.println("***" + s);
	}

	public void makeNetwork() {
		edges = new Hashtable<Integer, Vector<Edge>>();
		valence = new Hashtable<String, int[]>();
		for (Integer i : steps) {
			edges.put(i, new Vector<Edge>());
		}
		for (SparseVector v : vecs) {
			if (v.getVals() != null && v.getFrequency() > MINFREQ) {
				if (!valence.containsKey(v.getToken())) {
					int[] h = new int[steps.length];
					valence.put(v.getToken(), h);
				}
				for (SparseVector u : vecs) {
					if (u != v && u.getVals() != null
							&& u.getFrequency() > MINFREQ) {
						if (!valence.containsKey(u.getToken())) {
							int[] h = new int[steps.length];
							valence.put(u.getToken(), h);
						}
						try {
							float d = VectorMath.cosineSimilarity(v.getVals(),
									u.getVals());
							for (int i = 0; i < steps.length; i++) { // (Integer
																		// threshold:
																		// steps)
																		// {
								if (100 * d > steps[i]) {
									Edge edge = new Edge(v.getToken(),
											u.getToken(), d);
									Vector<Edge> vv = edges.get(steps[i]);
									vv.add(edge);
									valence.get(v.getToken())[i]++;
									valence.get(u.getToken())[i]++;
								}
							}
						} catch (Exception e) {
							error(u + " ? " + v + " error: " + e);
						}
					}
				}
				v.setVals(null);
			}
		}
	}

	public static Vector<SparseVector> readFromFile(String filename)
			throws FileNotFoundException {
		Vector<SparseVector> vec = new Vector<SparseVector>();
		File testFile = new File(filename);
		Scanner fileScanner = new Scanner(new BufferedInputStream(
				new FileInputStream(testFile)));
		while (fileScanner.hasNextLine()) {
			String fileLine = fileScanner.nextLine();
			try {
				SparseVector s = new SparseVector();
				s.parseLispString(fileLine);
				vec.add(s);
			} catch (Exception e) {
				error("this one didn't scan: " + fileLine);
			}
		}
		fileScanner.close();
		return vec;
	}

	public static String coherence(Vector<SparseVector> vectors)
			throws Exception {
		System.out.println("About to start processing set of vectors.");
		String response = "";
		Vector<float[]> ff = new Vector<float[]>(vectors.size());
		for (SparseVector ss : vectors) {
			try {
				ff.add(ss.canonicalVector());
			} catch (NullPointerException npe) {
				System.err.println("beklagar " + ss.getToken());
			}
		}
		System.out.println("Vector set is of size " + vectors.size());
		float[] centroid = VectorMath.centroid(ff);
		String cmaxwd = "";
		double cmax = 0.0f;
		String cminwd = "";
		double cmin = 1f;
		int neighbourhood = 0;
		if (vectors.size() > 0) {
			for (SparseVector onevector : vectors) {
				double l = VectorMath.distance(onevector.canonicalVector(),
						centroid);
				// System.out.println(onevector.toString()+"\t"+l);
				if (l > cmax) {
					cmax = l;
					cmaxwd = onevector.getToken();
				}
				if (l < cmin) {
					cmin = l;
					cminwd = onevector.getToken();
				}
				if (l < HORIZON)
					neighbourhood++;
			}
		}
		int n = vectors.size();
		int i = 0;
		int j = 0;
		double max = 0d;
		double min = 1d;
		String minstr = "";
		String maxstr = "";
		Hashtable<String, double[]> nhood = new Hashtable<String, double[]>();
		String[] windex = new String[n];
		String[] wjndex = new String[n];
		float[] totdist = new float[n];
		int[] neighbourhoods = new int[n];
		float averageDistanceFromEachOther = 0.0f;
		double[][] corr = new double[n][n];
		for (SparseVector v1 : vectors) {
			windex[i] = v1.getToken();
			j = 0;
			double[] corrv = new double[n];
			for (SparseVector v2 : vectors) {
				double l = 0d;
				if (!v1.getToken().equals(v2.getToken())) {
					wjndex[j] = v2.getToken();
					l = VectorMath.distance(v1.canonicalVector(),
							v2.canonicalVector());
					corr[i][j] = l;
					corrv[j] = l;
					if (l < min && l > 0) {
						min = l;
						minstr = v1.getToken() + "<->" + v2.getToken();
					}
					if (l > max) {
						max = l;
						maxstr = v1.getToken() + "<->" + v2.getToken();
					}
					if (l < HORIZON) {
						neighbourhoods[i]++;
					}
					averageDistanceFromEachOther += l;
					totdist[i] += l;
				}
				j++;
			}
			nhood.put(v1.getToken(), corrv);
			i++;
		}
		for (int jj = 0; jj < wjndex.length; jj++) {
			response += "\t"+wjndex[jj];
		}
		response += "\n";
		for (int ii = 0; ii < windex.length; ii++) {
			response += windex[ii];
			for (int jj = 0; jj < wjndex.length; jj++) {
				response += "\t"+Math.round(corr[ii][jj]*100f)/100f;
			}
			response += "\n";
		}
		int bettercentroids = 0;
		String rimaxwd = "";
		float rimax = 0.0f;
		String riminwd = "";
		float rimin = 1000000000f;
		for (int ri = 0; ri < windex.length; ri++) {
			if (totdist[ri] > rimax) {
				rimax = totdist[ri];
				rimaxwd = windex[ri];
			}
			if (totdist[ri] < rimin) {
				rimin = totdist[ri];
				riminwd = windex[ri];
			}
			if (neighbourhoods[ri] > neighbourhood) {
				bettercentroids++;
			}
		}
		if (n > 1) {
			rimax = rimax / (n - 1);
			rimin = rimin / (n - 1);
			averageDistanceFromEachOther = averageDistanceFromEachOther
					/ (n * (n - 1));
		}
		response = response + "n: " + n + " (" + vectors.size() + ")" + "\n";
		response = response + "neighbourhood: " + neighbourhood + " ("
				+ bettercentroids + ")" + "\n";
		response = response + "pmin: " + minstr + " " + min + "\n";
		response = response + "pmax: " + maxstr + " " + max + "\n";
		response = response + "tmax: " + rimaxwd + " " + rimax + "\n";
		response = response + "tmin: " + riminwd + " " + rimin + "\n";
		response = response + "tave: " + averageDistanceFromEachOther + "\n";
		response = response + "cmax: " + cmaxwd + " " + cmax + "\n";
		response = response + "cmin: " + cminwd + " " + cmin + "\n";
		// response = response + "m1: "+VectorMath.divergence(centroid, ff) +
		// "\n";
		// response = response + "m2: "+VectorMath.sdev(centroid, ff) + "\n";
		// response = response + "m3: "+VectorMath.skewness(centroid, ff) +
		// "\n";
		// response = response + "m4: "+VectorMath.kurtosis(centroid, ff) +
		// "\n";
		return response;
	}

	public static String coherenceSortedList(Vector<SparseVector> vectors)
			throws Exception {
		TreeMap<Double,String> distances = new TreeMap<Double,String>();
		System.out.println("About to start processing set of vectors.");
		String response = "";
		Vector<float[]> ff = new Vector<float[]>(vectors.size());
		for (SparseVector ss : vectors) {
			try {
				ff.add(ss.canonicalVector());
			} catch (NullPointerException npe) {
				System.err.println("beklagar " + ss.getToken());
			}
		}
		System.out.println("Vector set is of size " + vectors.size());
		int n = vectors.size();
		int m = 0;
		float averageDistanceFromEachOther = 0.0f;
		Vector<String> seen = new Vector<String>(n);
		for (SparseVector v1 : vectors) {
			seen.add(v1.getToken());
			for (SparseVector v2 : vectors) {
				double l = 0d;
				if (! seen.contains(v2.getToken())) {
					l = VectorMath.cosineSimilarity(v1.canonicalVector(),
							v2.canonicalVector());
					distances.put(l, v1.getToken() + "<->" + v2.getToken());
					averageDistanceFromEachOther += l;
					m++;
				}
			}
		}
		if (m > 1) {
			averageDistanceFromEachOther = averageDistanceFromEachOther
					/ m;
		}
//		response = response + "n: " + n + "\n";
//		response = response + "tave: " + averageDistanceFromEachOther + "\n";
		for (Double s:distances.keySet()) {
			response += Math.round(s*100f)/100f+"\t"+distances.get(s)+"\n";
		}
		return response;
	}
	
	public String coherenceX(GetVectorsForWord vectorGetter,
			Vector<String> candidateWords) throws Exception {
		String response = "";
		Hashtable<String, float[]> v = new Hashtable<String, float[]>();
		// GetVectorsForWord vectorGetter = new GetVectorsForWord(properties);
		Vector<String> words = new Vector<String>();
		for (String w : candidateWords) {
			float[] fv = vectorGetter.getContextVector(w);
			if (fv != null) {
				v.put(w, fv);
				words.add(w);
			}
		}
		String cmaxwd = "";
		double cmax = 0.0f;
		String cminwd = "";
		double cmin = 1f;
		int neighbourhood = 0;
		float[] centroid = VectorMath.centroid(v.values());
		if (words.size() > 0) {
			for (String w : words) {
				double l = VectorMath.distance(v.get(w), centroid);
				if (l > cmax) {
					cmax = l;
					cmaxwd = w;
				}
				if (l < cmin) {
					cmin = l;
					cminwd = w;
				}
				if (l < HORIZON)
					neighbourhood++;
			}
		}
		int n = words.size();
		int i = 0;
		int j = 0;
		double max = 0d;
		double min = 1d;
		String minstr = "";
		String maxstr = "";
		double[][] corr = new double[words.size()][words.size()];
		String[] windex = new String[words.size()];
		float[] totdist = new float[words.size()];
		int[] neighbourhoods = new int[words.size()];
		float averageDistanceFromEachOther = 0.0f;
		int ii = 0;
		for (String w1 : words) {
			windex[i] = w1;
			j = 0;
			for (String w2 : words) {
				if (!w1.equals(w2)) {
					// int i = 0; for (float ff: v.get(w1)) {if (ff>0)
					// {System.out.print(i+":"+ff+" ");}
					// i++;}System.out.println();
					// int j = 0; for (float ff: v.get(w2)) {if (ff>0)
					// {System.out.print(j+":"+ff+" ");}
					// j++;}System.out.println();
					double l = VectorMath.distance(v.get(w1), v.get(w2));
					// if (l > 0.1f)
					// System.out.println(i+" "+j+" "+w1+" "+w2+" "+l); //
					// Math.round(100000*VectorMath.cosineSimilarity(v.get(w1),v.get(w2)))/100000d);
					corr[i][j] = l;
					if (l < min) {
						min = l;
						minstr = w1 + "<->" + w2;
					}
					if (l > max) {
						max = l;
						maxstr = w1 + "<->" + w2;
					}
					if (l < HORIZON) {
						neighbourhoods[i]++;
					}
					averageDistanceFromEachOther += l;
					ii++;
					totdist[i] += l;
				}
				j++;
			}
			i++;
		}
		int bettercentroids = 0;
		String rimaxwd = "";
		float rimax = 0.0f;
		String riminwd = "";
		float rimin = 1000000000f;
		for (int ri = 0; ri < windex.length; ri++) {
			if (totdist[ri] > rimax) {
				rimax = totdist[ri];
				rimaxwd = windex[ri];
			}
			if (totdist[ri] < rimin) {
				rimin = totdist[ri];
				riminwd = windex[ri];
			}
			if (neighbourhoods[ri] > neighbourhood) {
				bettercentroids++;
			}
		}
		if (n > 1) {
			rimax = rimax / (n - 1);
			rimin = rimin / (n - 1);
			averageDistanceFromEachOther = averageDistanceFromEachOther
					/ (n * (n - 1));
		}
		response = response + "n: " + n + " (" + candidateWords.size() + ")"
				+ "\n";
		response = response + "neighbourhood: " + neighbourhood + " ("
				+ bettercentroids + ")" + "\n";
		response = response + "pmin: " + minstr + " " + min + "\n";
		response = response + "pmax: " + maxstr + " " + max + "\n";
		response = response + "tmax: " + rimaxwd + " " + rimax + "\n";
		response = response + "tmin: " + riminwd + " " + rimin + "\n";
		response = response + "tave: " + averageDistanceFromEachOther + "\n";
		response = response + "cmax: " + cmaxwd + " " + cmax + "\n";
		response = response + "cmin: " + cminwd + " " + cmin + "\n";
		response = response + "m1: "
				+ VectorMath.divergence(centroid, v.values()) + "\n";
		response = response + "m2: " + VectorMath.sdev(centroid, v.values())
				+ "\n";
		response = response + "m3: "
				+ VectorMath.skewness(centroid, v.values()) + "\n";
		response = response + "m4: "
				+ VectorMath.kurtosis(centroid, v.values()) + "\n";
		return response;
	}

	public static void getVectorsFromCore(String[] args)
			throws FileNotFoundException {
		final Properties esProperties = new Properties();
		esProperties.setProperty("test", "association");
		esProperties.setProperty("wordspace", "3");
		esProperties.setProperty("userid", "monitor");
		esProperties.setProperty("password", "monitor");
		esProperties.setProperty("host", "stage-core1.gavagai.se");
		esProperties.setProperty("ear", "stage-core1");

		Vector<String> words = new Vector<String>();
		String[] aux = { "vill", "kan", "måste", "bör", "borde", "skulle",
				"ville" };
		String[] mat = { "korv", "biff", "hamburgare", "potatis", "torsk",
				"kolja", "ris", "spaghetti", "ketchup", "pizza", "tomat" };
		String[] ideologier = { "grön", "feminist", "liberal", "rasist",
				"konservativ", "nationalist", "socialist", "nazist" };
		String[] bang = { "anfall", "anfallen", "anfaller", "anfallet",
				"anföll", "attack", "attacker", "attackera", "attackerad",
				"attackerade", "attackerades", "attackerar", "attackeras",
				"attackerat", "avrätta", "avrättad", "avrättar", "avrättas",
				"avrättat", "bomb", "bomba", "bombad", "bombar", "bombat",
				"bombats", "brinn", "brinna", "bränd", "bränn", "bränner",
				"däng", "dö", "död", "döda", "dödar", "dödas", "explodera",
				"exploderade", "exploderar", "gisslan", "handgripligheter",
				"helvete", "hot", "hota", "hotade", "hotar", "hotas", "hugga",
				"hugger", "hänga", "hänger", "hängs", "ihjäl", "illa",
				"knivskuren", "knivskära", "kravall", "kravaller",
				"kravallerna", "krig", "kriga", "krigande", "krigar", "kriget",
				"käften", "körd", "kört", "misshandel", "misshandla",
				"misshandlade", "misshandlar", "misshandlas", "missil",
				"missilen", "missiler", "missilerna", "mord", "mordet",
				"mörda", "mördar", "mördas", "pisk", "piska", "piskar",
				"piskas", "pyrt", "raket", "rakter", "rakterna", "risigt",
				"rådäng", "råpisk", "skada", "skadad", "skadade", "skadar",
				"skadas", "skadats", "skjut", "skjuta", "skjutande", "skjuten",
				"skjuter", "skjuts", "skuren", "skära", "slagen", "slut",
				"slå", "slår", "smälla", "smäller", "sparka", "sparkar",
				"sparkas", "spräng", "spränga", "sprängd", "spränger",
				"sprängmedel", "sprängs", "sprängt", "spöstraff", "stack",
				"stick", "sticker", "straffa", "straffad", "straffar",
				"straffas", "stryk", "strypa", "stryper", "stryps", "strypt",
				"stucken", "terror", "terrorisera", "terroriserande",
				"terroriserar", "terroriserat", "terrorism", "terrorist",
				"terrorister", "terroristerna", "terroristernas", "tortera",
				"torterad", "torterade", "torterat", "tortyr", "utrota",
				"utrotade", "utrotades", "utrotar", "utrotas", "våld",
				"våldet", "våldsamheter", "våldsamheterna", "våldsamma",
				"våldsamt" };
		String[] partier = { "moderaterna", "grön", "socialdemokrat",
				"socialdemokraterna", "folkpartiet", "miljöpartiet", "sverige",
				"riksdagen", "svpol", "centerpartiet", "centern", "liberal",
				"liberalerna", "vänstern", "vänsterpartiet", "höger",
				"vänster", "rasist", "kristdemokraterna",
				"sverigedemokraterna", "valet", "piratpartiet", "konservativ",
				"nationalist", "sverige", "socialist", "nazist" };
		String[] mammals = { "hund", "katt", "ko", "häst", "ponny", "hamster",
				"marsvin", "korv" };
		String[] iffySV = { "ambivalent", "ambivalenta", "ana", "anade",
				"anar", "anat", "anta", "antag", "antaga", "antagit",
				"antagligen", "antar", "antog", "avvakta", "avvaktade",
				"avvaktande", "avvaktar", "avvaktat", "betvivla", "betvivlade",
				"betvivlar", "betvivlat", "betänk", "betänka", "betänker",
				"betänkligheter", "betänkligheterna", "betänkligheternas",
				"betänkt", "blygt", "bortkommen", "bortkommet", "bortkomna",
				"chans", "chansa", "chansade", "chansar", "chansat", "chansen",
				"chansens", "chanser", "chanserna", "chansernas", "chansers",
				"dubier", "dubierna", "dubiernas", "eventuell", "eventuella",
				"eventuellt", "farofylld", "farofyllda", "farofylldt",
				"förbryllad", "förbryllade", "förbryllat", "förlägen",
				"förläget", "förlägna", "förmoda", "förmodad", "förmodade",
				"förmodande", "förmodar", "förmodat", "förmodligen",
				"förutsatt", "förutsatte", "förutsätta", "förutsätter",
				"förvirrad", "förvirrade", "förvirrat", "gissa", "gissade",
				"gissar", "gissat", "huttlade", "huttlar", "huttlat", "hymla",
				"hymlade", "hymlar", "hymlat", "ifrågasatt", "ifrågasatta",
				"ifrågasatte", "ifrågasätta", "ifrågasätter", "instabil",
				"instabila", "instabilt", "kanhända", "kanske", "konfunderad",
				"konfunderade", "konfunderat", "konfys", "konfysa", "konfyst",
				"labil", "labila", "labilt", "misstro", "misstänka",
				"misstänker", "misstänkt", "misstänkta", "misstänkte",
				"måhända", "möjlig", "möjliga", "möjligen", "möjligt",
				"möjligtvis", "nervös", "nervösa", "nervöst", "nja", "njae",
				"nog", "obeslutsam", "obeslutsamma", "obeslutsamt",
				"opålitlig", "opålitliga", "opålitligt", "ostadig", "ostadiga",
				"ostadigt", "osäker", "osäkerhet", "osäkerheten",
				"osäkerhetens", "osäkert", "osäkra", "oviss", "ovissa",
				"ovisshet", "ovissheten", "ovisshetens", "ovisst", "ponera",
				"preliminär", "preliminära", "preliminärt", "riskabel",
				"riskabelt", "rådvill", "rådvilla", "rådvillt", "sannolik",
				"sannolika", "sannolikt", "skepsis", "skepticism",
				"skepticismen", "skepticismens", "skeptisk", "skeptiska",
				"skeptiskt", "skrupler", "skruplerna", "skruplernas",
				"skruplers", "svävande", "tippa", "tippade", "tippat", "tro",
				"trodde", "trolig", "troliga", "troligt", "tror", "trott",
				"tvehågsen", "tvehågset", "tvehågsna", "tveka", "tvekade",
				"tvekan", "tvekans", "tvekar", "tvekat", "tveksam",
				"tveksamma", "tveksamt", "tvivel", "tvivelaktig",
				"tvivelaktiga", "tvivelaktigt", "tvivelsmål", "tvivelsmålet",
				"tvivelsmålets", "tvivla", "tvivlade", "tvivlar", "tvivlat",
				"utflippad", "utflippade", "utflippat", "vackla", "vacklande",
				"vacklar", "vacklat", "vankelmodig", "vankelmodiga",
				"vankelmodigt", "vansklig", "vanskliga", "vanskligt", "velade",
				"velar", "velig", "veliga", "veligt", "villrådig",
				"villrådiga", "villrådigt" };
		String[] posSV = { "(*^^*)", "(*^_^*)", "(-:", "(-;", "(8", "(:", "(;",
				"(;^_^a", "(=", "(=:", "(^-^", "(^-^)", "(^-^)/", "(^-^)v",
				"(^-^;", "(^_^", "(^_^)", "(^_^)/", "(^_^)v", "(^_^;",
				"(^_^;)", "(o:", "*^_^*", "...^_^", "..^_^", ".^_^", "8)",
				"8*)", "8-)", "8-]", "8-d", "8]", "8d", "8p", ":)", ":*)",
				":-)", ":-]", ":-d", ":-p", ":-}", ":=)", ":]", ":d", ":o)",
				":od", ":p", ":}", ";)", ";*)", ";-)", ";-]", ";-d", ";-p",
				";-}", ";=)", ";]", ";d", ";o)", ";od", ";op", ";p", ";}",
				"<3", "=)", "=]", "=^_^=", "=d", "=}", "[8", "[:", "[;", "[=",
				"^-^", "^_^", "^_^)", "^_^.", "^_^;", "alert", "alerta",
				"alertare", "alertast", "alertaste", "ambitiös", "ambitiösa",
				"ambitiösare", "ambitiösast", "ambitiösaste", "ambitiöst",
				"angenäm", "angenäma", "angenämare", "angenämast",
				"angenämaste", "angenämt", "ansvarsfull", "ansvarsfulla",
				"ansvarsfullare", "ansvarsfullast", "ansvarsfullaste",
				"ansvarsfullt", "användbar", "användbara", "användbarare",
				"användbarast", "användbaraste", "användbart", "aptitlig",
				"aptitliga", "aptitligare", "aptitligast", "aptitligaste",
				"asbra", "assnygg", "attrahera", "attraherad", "attraherade",
				"attraherar", "attraheras", "attraherat", "attraherats",
				"attraktion", "attraktionen", "attraktiv", "attraktiva",
				"attraktivare", "attraktivast", "attraktivaste", "attraktivt",
				"avguda", "avgudade", "avgudar", "avgudas", "avgudat",
				"avlasta", "avlastad", "avlastade", "avlastar", "avlastas",
				"avlastat", "avlastats", "avspänd", "avspända", "avspändare",
				"avspändast", "avspändaste", "avspänt", "balanserad", "ball",
				"balla", "ballare", "ballast", "ballt", "bastant", "bastanta",
				"bastantare", "bastantast", "bastantaste", "beaktansvärd",
				"beaktansvärda", "beaktansvärdare", "beaktansvärdast",
				"beaktansvärdaste", "beaktansvärt", "befria", "befriad",
				"befriade", "befriande", "befriar", "befrias", "befriat",
				"befriats", "befrämja", "befrämjad", "befrämjade",
				"befrämjande", "befrämjar", "befrämjas", "befrämjat",
				"befrämjats", "begåvad", "begåvade", "begåvat", "behaglig",
				"behagliga", "behagligare", "behagligast", "behagligaste",
				"behagligt", "bekväm", "bekväma", "bekvämare", "bekvämast",
				"bekvämaste", "bekvämt", "bekymmerslös", "bekymmerslösa",
				"bekymmerslösare", "bekymmerslösast", "bekymmerslösaste",
				"bekymmerslöst", "beläst", "belästa", "belåten", "belåtet",
				"belåtna", "betrygga", "betryggade", "betryggande",
				"betryggar", "betryggas", "betryggat", "betydande",
				"betydelsefull", "betydelsefulla", "betydelsefullt",
				"beundransvärd", "beundransvärda", "beundransvärdare",
				"beundransvärdast", "beundransvärdaste", "beundransvärt",
				"blomstra", "blomstrande", "blomstrar", "blomstrat", "bra",
				"braig", "braighet", "braigt", "briljant", "briljanta",
				"briljantare", "briljantast", "briljantaste", "brilliant",
				"brillianta", "bussig", "bussiga", "bussigare", "bussigast",
				"bussigaste", "bussigt", "bäst", "bästa", "bästaste", "bättre",
				"celeber", "celebera", "celeberare", "celeberast",
				"celeberaste", "celebert", "cool", "coola", "coolare",
				"coolast", "coolaste", "coolers", "coolness", "coolt",
				"d(^_^o)", "distingerad", "distingerade", "distingerat",
				"duglig", "dugliga", "dugligare", "dugligast", "dugligaste",
				"dugligt", "duktig", "duktiga", "duktigare", "duktigast",
				"duktigaste", "duktigt", "dyrbar", "dyrbara", "dyrbarare",
				"dyrbarast", "dyrbaraste", "dyrbart", "effektiv", "effektiva",
				"effektivare", "effektivast", "effektivaste", "effektivt",
				"elegant", "eleganta", "elegantare", "elegantast",
				"elegantaste", "eminens", "eminent", "eminenta", "eminentare",
				"eminentast", "eminentaste", "enastående", "engagerad",
				"engagerade", "engagerar", "engagerat", "euforisk",
				"euforiska", "euforiskare", "euforiskast", "euforiskaste",
				"euforiskt", "excellent", "excellenta", "excellentare",
				"excellentast", "excellentaste", "exellent", "exellenta",
				"exellentare", "exellentast", "exellentaste", "exklusiv",
				"exklusiva", "exklusivare", "exklusivast", "exklusivaste",
				"exklusivt", "f^_^;", "f^_^;)", "fantastisk", "fantastiska",
				"fantastiskare", "fantastiskast", "fantastiskaste",
				"fantastiskt", "felfri", "felfria", "felfriare", "felfriast",
				"felfriaste", "felfritt", "fenomenal", "fenomenala",
				"fenomenalare", "fenomenalast", "fenomenalaste", "fenomenalt",
				"fiffig", "fiffiga", "fiffigare", "fiffigast", "fiffigaste",
				"fiffigt", "fiiiiiin", "fin", "fina", "finemang", "finfin",
				"finfina", "finfinare", "finfinast", "finfinaste", "finfint",
				"finnemang", "finsate", "finurlig", "finurliga", "finurligare",
				"finurligast", "finurligaste", "finurligt", "flitig",
				"flitiga", "flitigare", "flitigast", "flitigaste", "flitigt",
				"flott", "flotta", "flottare", "flottast", "flottaste",
				"framgång", "framgångar", "framgångarna", "framgångarnas",
				"framgången", "framgångens", "framgångssaga", "framstående",
				"framtidstro", "fredlig", "fredliga", "fredligare", "fredligt",
				"fredsam", "frejdig", "frejdiga", "frejdigare", "frejdigast",
				"frejdigaste", "frejdigt", "fresta", "frestade", "frestande",
				"frestar", "frestas", "frestat", "frestats", "fria",
				"frigjord", "frigjorda", "frigjordare", "frigjordast",
				"frigjordaste", "frigjorde", "frigjort", "frigjorts",
				"frigöra", "frigörande", "frisk", "friska", "friskare",
				"friskast", "friskaste", "friskt", "fryntlig", "fryntliga",
				"fryntligare", "fryntligast", "fryntligaste", "fryntligt",
				"främja", "främjande", "främjar", "främjas", "främjat",
				"främjats", "fräsch", "fräscha", "fräschare", "fräschast",
				"fräschaste", "fräscht", "fräsig", "fräsiga", "fräsigare",
				"fräsigast", "fräsigaste", "fräsigt", "fröjd", "fröjdefull",
				"fröjden", "fröjder", "fullgod", "fullgoda", "fullgodare",
				"fullgodast", "fullgodaste", "fullgott", "fullkomlig",
				"fullkomliga", "fullkomligare", "fullkomligast",
				"fullkomligaste", "fullärd", "fullärda", "fullödig",
				"fullödiga", "fullödigare", "fullödigast", "fullödigaste",
				"fullödigt", "fördel", "fördelaktig", "fördelaktiga",
				"fördelaktigare", "fördelaktigast", "fördelaktigaste",
				"fördelaktigt", "fördelar", "fördelarna", "fördelen",
				"förmånlig", "förmånliga", "förmånligare", "förmånligast",
				"förmånligaste", "förmånligt", "förnämlig", "förnämliga",
				"förnämligare", "förnämligast", "förnämligaste", "förnämligt",
				"förnöja", "förnöjd", "förnöjda", "förnöjdare", "förnöjdast",
				"förnöjdaste", "förnöjt", "förstklassig", "förstklassiga",
				"förstklassigare", "förstklassigast", "förstklassigaste",
				"förstklassigt", "förtjusande", "förtjänstfull",
				"förtjänstfulla", "förtjänstfullare", "förtjänstfullast",
				"förtjänstfullaste", "förtjänstfullt", "förtroende",
				"förtroendeingivande", "förträfflig", "förträffliga",
				"förträffligare", "förträffligast", "förträffligaste",
				"förträffligt", "förtröstan", "förtröstande", "förtröstat",
				"gagn", "gagna", "gagnad", "gagnade", "gagnande", "gagnar",
				"gagnas", "gagnat", "gagnats", "garnnare", "gedigen",
				"gediget", "gedigna", "gedignare", "gedignast", "gedignaste",
				"gemytlig", "gemytliga", "gemytligare", "gemytligast",
				"gemytligaste", "gemytligt", "gilla", "gillad", "gillade",
				"gillande", "gillar", "gillas", "gillat", "gillats", "givande",
				"glad", "glada", "gladare", "gladast", "gladaste", "gladdes",
				"glatt", "glädja", "glädjas", "glädje", "glädjen", "glädjens",
				"glädjs", "goaste", "god", "goda", "godare", "godast",
				"godaste", "gosa", "gosade", "gosar", "gosig", "gosigaste",
				"gosigt", "gott", "gotta", "gottad", "gottade", "gottar",
				"gottas", "gottat", "gottats", "gottig", "gottiga", "gottigt",
				"grann", "granna", "grannast", "grannaste", "gudomlig",
				"gudomliga", "gudomligare", "gudomligast", "gudomligaste",
				"gudomligt", "gullig", "gulliga", "gulligare", "gulligast",
				"gulligaste", "gulligt", "gynna", "gynnad", "gynnade",
				"gynnande", "gynnar", "gynnas", "gynnat", "gynnats", "gött",
				"götta", "göttade", "göttig", "göttiga", "göttigt",
				"harmonisk", "harmoniska", "harmoniskt", "hederlig",
				"hederliga", "hederligare", "hederligast", "hederligaste",
				"hederligt", "helad", "helade", "helande", "helar", "helas",
				"helast", "helaste", "helat", "helats", "het", "himmelsk",
				"himmelska", "himmelskare", "himmelskast", "himmelskaste",
				"himmelskt", "hjälpsam", "hjälpsamma", "hjälpsammare",
				"hjälpsammast", "hjälpsammaste", "hjälpsamt", "hjärtlig",
				"hjärtliga", "hjärtligare", "hjärtligast", "hjärtligaste",
				"hjärtligt", "hoppfull", "hoppfulla", "hoppfullare",
				"hoppfullast", "hoppfullaste", "hoppfullt", "hoppingivande",
				"hurra", "hurrahurra", "hurrahurrahurra", "hygglig",
				"hyggliga", "hyggligare", "hyggligast", "hyggligaste",
				"hyggligt", "hyvens", "hälsosam", "hälsosamma", "hälsosammare",
				"hälsosammast", "hälsosammaste", "hälsosamt", "härlig",
				"härliga", "härligare", "härligast", "härligaste", "härligt",
				"hållbar", "hållbara", "hållbarare", "hållbarast",
				"hållbaraste", "hållbart", "högtryck", "högtrycket",
				"idealisk", "idealiska", "idealiskare", "idealiskast",
				"idealiskaste", "idealiskt", "inbjudande", "insiktsfull",
				"insiktsfulla", "insiktsfullare", "insiktsfullast",
				"insiktsfullaste", "insiktsfullt", "intressant", "intressanta",
				"intressantare", "intressantast", "intressantaste", "jakande",
				"jippi", "jippie", "jippy", "justa", "justare", "justast",
				"justaste", "juste", "juva", "juvare", "juvast", "juvaste",
				"juvlig", "juvliga", "juvligare", "juvligast", "juvligaste",
				"juvt", "jättebra", "jättefin", "jättefina", "jättefinare",
				"jättefinast", "jättefinaste", "jättefint", "kalas", "kalasa",
				"kalasade", "kalasas", "kalasbra", "kalasfin", "kalasfint",
				"kalasig", "kanon", "kanonbra", "kanonfin", "kanonfint",
				"kanonpris", "kapabelt", "kapablare", "kapablast",
				"kapablaste", "kewl", "kewla", "kewlare", "kewlast",
				"kewlaste", "klockren", "klockrena", "klockrenare",
				"klockrenast", "klockrenaste", "klockrent", "klok", "kloka",
				"klokare", "klokast", "klokaste", "klokt", "komfortabel",
				"komfortabelt", "komfortabla", "komfortablare",
				"komfortablast", "komfortablaste", "kompetent", "kompetenta",
				"kompetentare", "kompetentast", "kompetentaste", "kul",
				"kulig", "kuliga", "kuligare", "kuligast", "kuligaste",
				"kuligt", "kung", "kunglig", "kungligt", "kunnig", "kunniga",
				"kunnigare", "kunnigast", "kunnigaste", "kunnigt",
				"kvalificerade", "kvalificerat", "käck", "käcka", "käckare",
				"käckast", "käckaste", "käckt", "lajban", "lajbans", "lattja",
				"lattjo", "lattjolajban", "lattjolajbans", "like", "likes",
				"livsglädje", "livsglädjen", "livsglädjens", "ljusning",
				"ljust", "ljuv", "ljuva", "ljuvare", "ljuvast", "ljuvaste",
				"ljuvlig", "ljuvliga", "ljuvligare", "ljuvligast",
				"ljuvligaste", "ljuvligt", "ljuvt", "lovande", "lovord",
				"lovordad", "lovordade", "lovordas", "lovordat", "lovordats",
				"lugn", "lugna", "lugnande", "lugnare", "lugnast", "lugnaste",
				"lust", "lusta", "lustade", "lustande", "lustar", "lustas",
				"lustat", "lustats", "lustfull", "lustfulla", "lustfullare",
				"lustfullast", "lustfullaste", "lustfylld", "lustfyllda",
				"lustfyllt", "lycka", "lyckade", "lyckat", "lycklig",
				"lyckliga", "lyckligare", "lyckligast", "lyckligaste",
				"lyckligt", "lyckosam", "lyckosama", "lyckosamma",
				"lyckosammare", "lyckosammast", "lyckosammaste", "lyckosamt",
				"lysande", "lysten", "lystet", "lystna", "lystnare",
				"lystnast", "lystnaste", "lyx", "lyxen", "lyxig", "lyxigt",
				"läcker", "läckert", "läckra", "läckrare", "läckrast",
				"läckraste", "lätthanterlig", "lätthanterliga",
				"lätthanterligare", "lätthanterligast", "lätthanterligaste",
				"lätthanterligt", "lätthet", "lättheten", "magnifik",
				"magnifika", "magnifikare", "magnifikast", "magnifikaste",
				"magnifikt", "makalös", "makalösa", "makalösare", "makalösast",
				"makalösaste", "makalöst", "medgång", "medgången",
				"megahärligt", "megakul", "meningsfull", "meningsfulla",
				"meningsfullare", "meningsfullast", "meningsfullaste",
				"meningsfullt", "meriterad", "meriterade", "meriterande",
				"miljövänlig", "miljövänliga", "mirakulös", "mirakulösa",
				"mirakulösare", "mirakulösast", "mirakulösaste", "mirakulöst",
				"munter", "munterhet", "muntra", "muntrare", "muntrast",
				"muntraste", "mysig", "mysigaste", "mysigt", "myspys", "najs",
				"najsa", "najsare", "najsast", "najsaste", "niceare",
				"niceast", "niceaste", "njut", "njuta", "njutande", "njuter",
				"njutning", "njutningen", "njutningens", "njutningsfull",
				"njutningsfulla", "njutningsfullt", "njutningsfylld",
				"njutningsfyllda", "nobel", "nobla", "noblare", "noblast",
				"noblaste", "nytta", "nyttig", "nyttiga", "nyttigare",
				"nyttigast", "nyttigaste", "nyttigt", "nöjd", "nöjda",
				"nöjdare", "nöjdast", "nöjdaste", "nöje", "nöjen", "nöjena",
				"o(^-^)o", "oantastlig", "oantastliga", "oantastligare",
				"oantastligast", "oantastligaste", "oantastligt", "obekymra",
				"obekymrad", "obekymrade", "obekymrat", "ok", "okej", "okeja",
				"okejad", "okejade", "okejar", "okejas", "okejat", "okejats",
				"optimism", "optimismen", "optimismens", "optimist",
				"optimisten", "optimistens", "optimister", "optimisterna",
				"optimisternas", "optimistisk", "optimistiska",
				"optimistiskare", "optimistiskast", "optimistiskasta",
				"optimistiskt", "ordentlig", "ordentliga", "ordentligare",
				"ordentligast", "ordentligaste", "ordentligt", "otvungen",
				"otvunget", "otvungna", "otvungnare", "otvungnast",
				"otvungnaste", "pampig", "pampiga", "pampigare", "pampigast",
				"pampigaste", "pampigt", "paradis", "paradiset", "paradisisk",
				"paradisiska", "paradisiskare", "paradisiskast",
				"paradisiskaste", "paradisiskt", "passion", "passionen",
				"passionerad", "perfa", "perfekt", "perfekta", "perfektare",
				"perfektast", "perfektaste", "plus", "plussa", "plussade",
				"plussar", "plussas", "plussat", "plussats", "plussen",
				"plusset", "plussida", "plussidan", "positiv", "positiva",
				"positivare", "positivast", "positivaste", "positivitet",
				"positiviteten", "positivitetens", "positivt", "potent",
				"potenta", "potentare", "potentast", "potentaste", "praktfull",
				"praktfulla", "praktfullare", "praktfullast", "praktfullaste",
				"praktfullt", "premiera", "premierad", "premierade",
				"premierande", "premierar", "premieras", "premierat",
				"premierats", "prima", "professionell", "professionella",
				"professionellare", "professionellast", "professionellaste",
				"professionellt", "proper", "propert", "propra", "proprare",
				"proprast", "propraste", "prudentlig", "prudentliga",
				"prudentligare", "prudentligast", "prudentligaste",
				"prudentligt", "prydlig", "prydliga", "prydligare",
				"prydligast", "prydligaste", "prydligt", "pålitlig",
				"pålitliga", "pålitligare", "pålitligast", "pålitligaste",
				"pålitligt", "rar", "rara", "rarare", "rarast", "raraste",
				"rart", "redig", "rediga", "redigare", "redigast", "redigaste",
				"redigt", "regera", "regerade", "regerande", "regerar",
				"regerat", "rejäl", "rejäla", "rejälare", "rejälast",
				"rejälaste", "rejält", "relevant", "relevanta", "relevantare",
				"relevantast", "relevantaste", "renhårig", "renhåriga",
				"renhårigare", "renhårigast", "renhårigaste", "renhårigt",
				"revansch", "revanschen", "revanschens", "revanscher",
				"revanschera", "revanscherad", "revanscherade",
				"revanscherades", "revanscherar", "revanscheras",
				"revanscherna", "revanschernas", "riskfri", "riskfria",
				"riskfriare", "riskfriast", "riskfriaste", "riskfritt",
				"robust", "robusta", "robustare", "robustast", "robustaste",
				"rolig", "roliga", "roligare", "roligast", "roligaste",
				"roligt", "rular", "rule", "rules", "rutinera", "rutinerade",
				"rutinerat", "ryktbar", "ryktbara", "ryktbarare", "ryktbarast",
				"ryktbaraste", "ryktbart", "räddar", "räddas", "räddat",
				"räddats", "räddning", "räddningen", "rättfärdig",
				"rättfärdiga", "rättfärdigt", "sagolik", "sagolika",
				"sagolikare", "sagolikast", "sagolikaste", "sagolikt", "salig",
				"saliga", "saligare", "saligast", "saligaste", "saligt",
				"schysst", "schyssta", "schysstare", "schysstast",
				"schysstaste", "seger", "segern", "segrar", "segrare",
				"segraren", "segrarna", "segrarnas", "seriös", "seriösa",
				"sexig", "sexiga", "sexigare", "sexigaste", "sexigt", "sjysst",
				"sjyssta", "sjysstare", "sjysstast", "sjysstaste", "skicklig",
				"skickliga", "skickligare", "skickligast", "skickligaste",
				"skickligt", "skitbra", "skitnice", "skitschysst",
				"skitsjysst", "skitsmart", "skitsmarta", "skitsnygg",
				"skitsnygga", "skojig", "skojiga", "skojigare", "skojigast",
				"skojigaste", "skojigt", "skojsig", "skojsiga", "skälig",
				"skäliga", "skäligt", "skön", "sköna", "skönare", "skönast",
				"skönaste", "skönt", "skötsam", "skötsamma", "skötsammare",
				"skötsammast", "skötsammaste", "skötsamt", "smakfull",
				"smakfulla", "smakfullare", "smakfullast", "smakfullaste",
				"smakfullt", "smart", "smarta", "smartare", "smartast",
				"smartaste", "snitsig", "snitsiga", "snitsigare", "snitsigast",
				"snitsigaste", "snitsigt", "snygg", "snygga", "snyggare",
				"snyggast", "snyggaste", "snygghet", "snygging", "snyggingen",
				"snyggo", "snyggt", "snäll", "snälla", "snällare", "snällast",
				"snällaste", "snällhet", "snällt", "soft", "softa", "softade",
				"softar", "softare", "softast", "softaste", "softat",
				"softish", "solid", "solida", "solidare", "solidast",
				"solidaste", "solitt", "sorglös", "sorglösa", "sorglösare",
				"sorglösast", "sorglösaste", "sorglöst", "sprallig",
				"spralliga", "spralligare", "spralligast", "spralligaste",
				"spralligt", "sprudla", "sprudlad", "sprudlade", "sprudlande",
				"sprudlar", "sprudlat", "sprudlats", "stabil", "stabila",
				"stabilare", "stabilast", "stabilaste", "stabilt", "stadig",
				"stadiga", "stadigare", "stadigast", "stadigaste", "stadigt",
				"stark", "starka", "starkare", "starkast", "starkaste",
				"starkt", "stilig", "stiliga", "stiligare", "stiligast",
				"stiligaste", "stiligt", "stimulera", "stimulerande",
				"stimulerar", "stimulerat", "storartad", "storartade",
				"storartat", "storslagen", "storslaget", "storslagna",
				"storslagnare", "storslagnast", "storslagnaste", "strålande",
				"styrka", "styrkan", "stärkande", "stärker", "stärks",
				"stärkt", "stärkts", "ståtlig", "ståtliga", "ståtligare",
				"ståtligast", "ståtligaste", "ståtligt", "succe", "succeen",
				"succeerna", "succen", "succerna", "succet", "succn", "succé",
				"succéerna", "succén", "sund", "sunda", "sundare", "sundast",
				"sundaste", "sunt", "superb", "superba", "superbra", "superbt",
				"superhärligt", "superkul", "supernice", "supersjysst",
				"supersmart", "supersmarta", "suverän", "suveräna",
				"suveränare", "suveränast", "suveränaste", "suveränt",
				"sympatisk", "sympatiska", "sympatiskare", "sympatiskast",
				"sympatiskaste", "sympatiskt", "söt", "söta", "sötare",
				"sötast", "sötaste", "tacksam", "tacksamm", "tacksamma",
				"tacksammare", "tacksammast", "tacksammaste", "tacksammt",
				"tacksamt", "taggad", "taggade", "talangfull", "talangfulla",
				"talangfullare", "talangfullast", "talangfullaste",
				"talangfullt", "tillfreds", "tillförlitlig", "tillförlitliga",
				"tillförlitligare", "tillförlitligast", "tillförlitligaste",
				"tillförlitligt", "tillförsikt", "tillförsikten",
				"tillförsiktens", "tillit", "tilliten", "tillitens", "tilltro",
				"tilltron", "tilltrons", "tillväxt", "tillväxten",
				"tillväxtens", "tjoho", "tjusig", "tjusiga", "tjusigare",
				"tjusigast", "tjusigaste", "tjusigt", "toppen", "toppenbra",
				"toppenfin", "trevlig", "trevliga", "trevligare", "trevligast",
				"trevligaste", "trevligt", "triumf", "triumfen", "triumfera",
				"triumferande", "triumferar", "triumferat", "trivsam",
				"trivsamma", "trivsammare", "trivsammast", "trivsammaste",
				"trivsamt", "trygg", "trygga", "tryggare", "tryggast",
				"tryggaste", "tryggt", "tutilurfräs", "underbar", "underbara",
				"underbarare", "underbarast", "underbaraste", "underbart",
				"underhållande", "underlätta", "underlättad", "underlättade",
				"underlättande", "underlättar", "underlättas", "underlättat",
				"underlättats", "underskön", "undersköna", "underskönare",
				"underskönast", "underskönaste", "undsätta", "undsätter",
				"undsättning", "undsätts", "uppbyggelig", "uppbyggeliga",
				"uppbyggeligare", "uppbyggeligast", "uppbyggeligaste",
				"uppbygglig", "uppbyggliga", "uppbyggligare", "uppbyggligast",
				"uppbyggligaste", "uppgång", "uppgången", "uppgångens",
				"uppmuntra", "uppmuntrad", "uppmuntrade", "uppmuntran",
				"uppmuntrande", "uppmuntrar", "uppmuntras", "uppmuntrat",
				"uppmuntrats", "uppmuntring", "uppmuntringen", "upprymd",
				"upprymda", "upprymdare", "upprymdast", "upprymdaste",
				"upprymdhet", "uppsluppen", "uppsluppet", "uppsluppna",
				"uppsluppnare", "uppsluppnast", "uppsluppnaste", "uppspelt",
				"uppspelta", "uppspeltare", "uppspeltast", "uppspeltaste",
				"uppsving", "uppsvinget", "uppåt", "uppåtgående",
				"uppåtriktad", "urbra", "utmärkt", "utmärkta", "utmärktare",
				"utmärktast", "utmärktaste", "utomordentlig", "utomordentliga",
				"utomordentligare", "utomordentligast", "utomordentligaste",
				"utomordentligt", "utsökt", "utsökta", "utsöktare",
				"utsöktast", "utsöktaste", "vacker", "vackert", "vackra",
				"vackrare", "vackrast", "vackraste", "vann", "vanns", "viktig",
				"viktiga", "viktigare", "viktigast", "viktigaste", "viktigt",
				"vinna", "vinner", "vinst", "vinsten", "vinster", "vinsterna",
				"vinsternas", "välbehag", "välbehaga", "välbehaglig",
				"välbehagliga", "välbehagligare", "välbehagligast",
				"välbehagligaste", "välbehagligt", "välbehagt", "välgjord",
				"välgjorda", "välgjordare", "välgjordast", "välgjordaste",
				"välgjort", "välgång", "välgången", "välgångens", "välgörande",
				"vällust", "vällusten", "vällustens", "välsignar",
				"välsignelse", "välsignelsen", "välsingnar", "välsingnelse",
				"välskött", "välskötta", "välsköttare", "välsköttast",
				"välsköttaste", "välstånd", "välvillig", "välvilliga",
				"välvilligare", "välvilligast", "välvilligaste", "välvilligt",
				"vänlig", "vänliga", "vänligare", "vänligast", "vänligaste",
				"vänligt", "värdefull", "värdefulla", "värdefullare",
				"värdefullast", "värdefullaste", "värdefullt", "winner",
				"woho", "wohoo", "woohoo", "xd", "yeah", "yes", "ypperlig",
				"ypperliga", "ypperligare", "ypperligast", "ypperligaste",
				"ypperligt", "yrkesskicklig", "yrkesskickliga",
				"yrkesskickligare", "yrkesskickligast", "yrkesskickligaste",
				"yrkesskickligt", "yster", "ystra", "ystrare", "ystrast",
				"ystraste", "{:", "ädel", "ädla", "ädlare", "ädlast",
				"ädlaste", "älska", "älskad", "älskade", "älskande", "älskar",
				"älskas", "älskat", "älskats", "älskvärd", "älskvärda",
				"älskvärdare", "älskvärdast", "älskvärdaste", "älskvärt",
				"ärlig", "ärliga", "ärligare", "ärligast", "ärligaste",
				"ärligt", "ömhet", "ömheten", "ömsint", "ömsinta", "ömsintare",
				"ömsintast", "ömsintaste", "ömsinthet", "ömt" };
		String[] negSV = { ")-:", ")8", "):", ");", ")=", "/8", "/:", "/;",
				"/=", "8(", "8-(", "8-/", "8/", ":&apos;(", ":(", ":*(", ":,(",
				":-(", ":-/", ":-[", ":-{", ":-|", ":/", ":=(", ":?", ":[",
				":o(", ":o/", ":{", ":|", ";(", ";-(", ";-/", ";/", ";[",
				";o(", ";{", "=(", "=/", "=[", "={", "=|", "]:", "];", "]=",
				"absurd", "absurda", "absurdast", "absurdaste", "absurdaste",
				"absurdism", "absurt", "allvarlig", "allvarliga",
				"allvarligare", "allvarligast", "allvarligaste", "allvarligt",
				"ambivalent", "ambivalenta", "ambivalentare", "ambivalentast",
				"ambivalentaste", "anskrämlig", "anskrämliga", "anskrämligast",
				"anskrämligaste", "anskrämligt", "arg", "arga", "argare",
				"argast", "argaste", "argsint", "asdum", "asdum", "asdumma",
				"asdummare", "asdummast", "asdummaste", "asdummaste", "asful",
				"asful", "asfula", "asfulare", "asfulast", "asfulaste",
				"asfulaste", "avbräck", "avbräcka", "avbräckt", "avog",
				"avoga", "avogt", "avsky", "avskydd", "avskydde", "avskyn",
				"avskys", "avskys", "avvisast", "avvisasts", "bakslag",
				"bakslag", "bakslagen", "bakslaget", "bakåt", "bakåtsträva",
				"bakåtsträvade", "bakåtsträvande", "bakåtsträvare",
				"bakåtsträvat", "bakåtsträvat", "bedröva", "bedröva",
				"bedrövad", "bedrövade", "bedrövande", "bedrövar", "bedrövat",
				"bedrövats", "bedrövliga", "bedrövligt", "befara", "befarad",
				"befarade", "befarar", "befaras", "befarat", "befarat",
				"befarats", "beklagansvärd", "beklagansvärda",
				"beklagansvärdare", "beklagansvärdare", "beklagansvärdast",
				"beklagansvärt", "bekymmer", "bekymmersam", "bekymmersamt",
				"bekymra", "bekymra", "bekymrade", "bekymrande", "bekymrar",
				"bekymrat", "bekymrats", "bekymren", "bekymrens", "bestört",
				"bestörta", "bestörtare", "bestörtast", "bestörtaste",
				"besvikelse", "besvikelse", "besvikelsen", "besvikelser",
				"besvikelserna", "besviken", "besviknare", "besviknare",
				"besviknast", "besvärad", "besvärar", "besvärar", "besvärlig",
				"besvärliga", "besvärligare", "besvärligast", "besvärligaste",
				"besvärligt", "betydelselös", "betydelselös",
				"betydelselösare", "betydelselösast", "betydelselösast",
				"betydelselösaste", "betydelselöst", "betydelselöst", "bister",
				"bistert", "bistert", "bistra", "bistrare", "bistrare",
				"bistrast", "bistraste", "bistraste", "blessyr", "blessyrer",
				"blessyrerna", "bläigare", "bläigast", "bläigaste",
				"bottenkänning", "bottennapp", "bottennapp", "bottennappen",
				"bottennappet", "bottennappet", "bristande", "bristerna",
				"bristfälligast", "bristfälligt", "bräcklig", "bräcklig",
				"bräckligare", "bräckligare", "bräckligast", "bräckligt",
				"buade", "buande", "buar", "buas", "buat", "butter", "buttert",
				"buttert", "buttra", "buttrare", "buttrast", "buttraste",
				"bäva", "bävade", "bävande", "bävar", "bävas", "bökig",
				"bökiga", "bökiga", "bökigare", "bökigast", "bökigaste",
				"bökigaste", "bökigt", "bökigt", "cancer", "cancern",
				"cancersvulst", "cancersvulsten", "cancersvulster",
				"cancersvulsterna", "chansera", "chanserad", "chanserade",
				"chanserade", "chanserande", "chanserar", "chanseras",
				"chanseras", "d:", "d;", "d=", "defekt", "defekta", "defekta",
				"defekter", "deformera", "deformerad", "deformerade",
				"deformerande", "deformerar", "deformerar", "deformeras",
				"deformerat", "degradera", "degraderad", "degraderade",
				"degraderande", "degraderar", "degraderas", "degraderat",
				"demolerad", "demolerade", "demolerade", "demolerande",
				"demolerar", "demoleras", "demolerat", "deppade", "deppar",
				"deppat", "deppat", "deppg", "deppig", "deppiga", "deppigare",
				"deppigast", "deppigaste", "deppigaste", "deppighet",
				"deppigt", "depression", "depressionen", "depressionen",
				"depressioner", "depressioner", "depressiva", "depressivare",
				"depressivast", "depressivaste", "depressivt", "deprimerad",
				"deprimerad", "deprimerade", "deprimerande", "deprimerat",
				"deprimerat", "djävliga", "djävligare", "djävligaste",
				"djävligt", "djävul", "do:", "dryg", "dysterkvist",
				"dysterkvistar", "dysterkvistarna", "dysterkvisten", "dystert",
				"dystra", "dystrare", "dålig", "dåligt", "dö", "döende", "dör",
				"elak", "elaka", "elakartade", "elakast", "elakaste", "elakt",
				"elände", "eländes", "eländet", "eländig", "eländiga",
				"eländigast", "eländigaste", "eländigt", "ett skämt", "faran",
				"farhåga", "farhågor", "farlig", "farliga", "farligare",
				"farligast", "farligaste", "farligt", "farofylld",
				"farofyllda", "farofyllt", "faror", "farorna", "farsot",
				"farsoten", "farsoter", "fiffel", "fiffla", "fifflade",
				"fifflar", "fifflare", "fifflats", "fly", "flydde", "flyende",
				"flytt", "frukta", "fruktad", "fruktade", "fruktande",
				"fruktansvärda", "fruktar", "fruktas", "fruktat", "fruktats",
				"fruktlös", "fruktlösa", "fruktlösare", "fruktlösast",
				"fruktlösaste", "fruktlöst", "frustration", "frustrationen",
				"frustrera", "frustrerad", "frustrerade", "frustrerade",
				"frustrerat", "ful", "fula", "fular", "fulare", "fulast",
				"fulaste", "fulhet", "fulheten", "fult", "fybubblan",
				"fybövelen", "fyböveln", "fyfan", "fälla", "fällande", "fälld",
				"fällde", "fälldes", "fäller", "fällt", "förakt", "förakta",
				"föraktad", "föraktade", "föraktande", "föraktar", "föraktas",
				"föraktat", "föraktet", "föraktets", "föraktfull",
				"föraktfulla", "föraktfullare", "föraktfullast",
				"föraktfullaste", "föraktfullt", "föraktligt", "förbannad",
				"förbannade", "förbannar", "förbannat", "förbise", "förbises",
				"förbisett", "förbisetts", "förbisåg", "förbruka",
				"förbrukade", "förbrukas", "förbrukat", "förbrukats",
				"fördjävlig", "fördjävliga", "fördjävligare", "fördjävligast",
				"fördjävligaste", "fördärv", "fördärva", "fördärvad",
				"fördärvade", "fördärvar", "fördärvas", "fördärvat",
				"fördärvats", "fördärvet", "förfall", "förfallets",
				"förfallit", "förfallits", "förfära", "förfärad", "förfärad",
				"förfärade", "förfärades", "förfärande", "förfäras",
				"förfärat", "förfärats", "förfärlig", "förfärliga",
				"förfärligare", "förfärligast", "förfärligaste", "förfärligt",
				"förföll", "förgjorde", "förgjordes", "förgjort", "förgjorts",
				"förgöra", "förgöras", "förhatlig", "förhatliga",
				"förhatligare", "förhatligast", "förhatligt", "förjävlig",
				"förjävliga", "förjävligare", "förjävligare", "förjävligast",
				"förjävligaste", "förjävligaste", "förkasta", "förkastad",
				"förkastad", "förkastar", "förkastat", "förkastat",
				"förkastats", "förkastats", "förkastlig", "förkastliga",
				"förkastligare", "förkastligast", "förkastligast",
				"förkastligt", "förkrossa", "förkrossad", "förkrossad",
				"förkrossade", "förkrossande", "förkrossande", "förkrossar",
				"förkrossat", "förkrossat", "förlora", "förlora", "förlorad",
				"förlorade", "förlorade", "förlorane", "förlorar", "förlorare",
				"förlorarens", "förlorat", "förlorats", "förluster",
				"förlusterna", "förlusternas", "förnedra", "förnedrad",
				"förnedrade", "förnedrande", "förnedrar", "förnedras",
				"förnedrat", "förnedrats", "förnedring", "förnedring",
				"förnedringen", "förnedringen", "förnedringens",
				"förnedringens", "förryckt", "förryckta", "förrycktare",
				"förrycktast", "förrycktaste", "förryckts", "försvaga",
				"försvaga", "försvagad", "försvagade", "försvagades",
				"försvagande", "försvagande", "försvagar", "försvagas",
				"försvagat", "försvagats", "försvåra", "försvårade",
				"försvårade", "försvårande", "försvårar", "försvåras",
				"försvårat", "försvårats", "förtret", "förtretet", "förtryck",
				"förtryck", "förtrycka", "förtrycka", "förtryckande",
				"förtryckare", "förtryckas", "förtryckas", "förtrycker",
				"förtrycks", "förtryckt", "förtryckta", "förtryckta",
				"förtryckts", "förtryckts", "förtvivla", "förtvivla",
				"förtvivlad", "förtvivlade", "förtvivlan", "förtvivlat",
				"förtvivlat", "föröda", "förödande", "förödat", "förödat",
				"förödda", "gagnlös", "gagnlösa", "gagnlösare", "gagnlösast",
				"gagnlösaste", "gagnlöst", "gemen", "gemena", "gemenare",
				"gemenast", "gemenast", "gemenaste", "gemenaste", "gement",
				"gement", "genomdålig", "genomrutten", "genomrutten",
				"genomusel", "grinig", "griniga", "griniga", "grinigare",
				"grinigast", "grinigaste", "grinigt", "grinigt", "grotesk",
				"groteska", "groteskare", "groteskast", "groteskaste",
				"groteskaste", "groteskt", "grämde", "grämer", "gräms",
				"grämt", "grämts", "gräslig", "gräsliga", "gräsligare",
				"gräsligast", "gräsligaste", "gräsligaste", "gräsligt", "grät",
				"gråta", "gråtande", "gråter", "gråtfärdig", "gråtfärdiga",
				"gråtfärdigt", "harm", "harm", "harma", "harmade", "harmande",
				"harmar", "harmat", "harmat", "hat", "hata", "hatad", "hatade",
				"hatande", "hatar", "hatas", "hatat", "hatats", "hatfull",
				"hatfulla", "hatfullt", "hatfylld", "hatfyllt", "haverera",
				"havererad", "havererade", "havererar", "havererar",
				"havererat", "havererat", "helkass", "helkassa", "helkasst",
				"hemsk", "hemska", "hemskare", "hemskast", "hemskast",
				"hemskaste", "hemskheter", "hemskt", "hinder", "hindra",
				"hindrad", "hindrade", "hindrar", "hindras", "hindrat",
				"hindrats", "hindren", "hopplös", "hopplösa", "hopplösa",
				"hopplösare", "hopplösast", "hopplösaste", "hopplöshet",
				"hopplösheten", "hopplöst", "huvudbry", "hyckel", "hycklare",
				"hycklaren", "hycklares", "hyckleri", "hycklerska", "hängig",
				"hängiga", "hängigare", "hängigast", "hängigaste", "hängigt",
				"hån", "håna", "håna", "hånad", "hånade", "hånar", "hånar",
				"hånas", "hånat", "hånats", "idiot", "idiot", "idioten",
				"idioter", "idioterna", "idioternas", "idioti", "idiotin",
				"idiotisk", "idiotiska", "idiotiskare", "idiotiskast",
				"idiotiskaste", "idiotiskt", "ignorant", "ignorant",
				"ignoranta", "ignorantare", "ignorantast", "ignorantaste",
				"ignorera", "ignorerad", "ignorerad", "ignorerade",
				"ignorerades", "ignorerar", "ignoreras", "ignorerat",
				"ignorerats", "illa", "illaluktande", "illamåendet",
				"illasinnad", "illasinnad", "illasinnade", "illasinnat",
				"illavarslande", "illvillig", "illvilliga", "illvilligare",
				"illvilligast", "illvilligaste", "illvilligt", "ineffektiv",
				"ineffektiva", "ineffektivare", "ineffektivast",
				"ineffektivaste", "ineffektivitet", "ineffektivt", "inkapabel",
				"inkapabelt", "inkapabla", "inkapablare", "inkapablast",
				"inkapablaste", "inkompetent", "inkompetenta",
				"inkompetentare", "inkompetentast", "inkompetentaste",
				"inskränk", "inskränk", "inskränka", "inskränkning",
				"inskränkt", "inskränkta", "inskränkthet", "inskränkthet",
				"inskränktheten", "inskränkts", "insolvens", "insolvensen",
				"insolvensens", "instabil", "instabila", "instabila",
				"instabilare", "instabilast", "instabilaste", "instabilt",
				"irrelevant", "irrelevanta", "irrelevantare", "irrelevantast",
				"jobbig", "jobbiga", "jobbigare", "jobbigast", "jobbigaste",
				"jobbighet", "jobbigheter", "jobbigheterna", "jobbigt",
				"jobbigt", "jättedålig", "jättedålig", "jättedåliga",
				"jättedåligt", "jätteful", "jättekass", "jättekassa",
				"jättekasst", "jätteledsen", "jävlarna", "jävlig", "jävliga",
				"jävligare", "jävligast", "jävligaste", "jävligaste",
				"jävligt", "jävul", "kaos", "kaosa", "kaosade", "kaosar",
				"kaosat", "kaoset", "kaoset", "kass", "kassa", "kasst",
				"katastrof", "katastrof", "katastrofal", "katastrofala",
				"katastrofalt", "katastrofen", "katastrofens", "katastrofens",
				"katastrofer", "klaga", "klagade", "klagande", "klagar",
				"klagas", "klagat", "klagats", "klen", "klena", "klenare",
				"klenast", "klenast", "klenaste", "klent", "knäckt", "knäckte",
				"knäckts", "kolera", "kollaps", "kollapsar", "kollapsat",
				"kollapsens", "kollapsens", "kollapser", "konstalde",
				"konstla", "konstlad", "konstlade", "konstlat", "krascha",
				"kraschade", "kraschar", "kraschas", "kraschat", "kraschats",
				"kraschen", "kraschens", "krasslig", "krassliga",
				"krassligare", "krassligast", "krassligaste", "krassligt",
				"krevera", "kreverade", "kreverar", "kreverat", "kris",
				"krisa", "krisa", "krisade", "krisande", "krisar", "krisat",
				"krisen", "krisens", "kriser", "kriserna", "krisernas",
				"kritisk", "kritiska", "kritiskare", "kritiskast",
				"kritiskaste", "kritiskt", "krämpa", "krämpan", "krämpan",
				"krämpor", "krämporna", "kuksugare", "kuksugaren",
				"kuksugarens", "kuksugarna", "kuksugarnas", "kvadda",
				"kvaddar", "kvaddas", "kvaddat", "kvaddat", "kvälja",
				"kväljande", "kväljde", "kväljning", "kväljning",
				"kväljningar", "kväljningar", "kväljningarna",
				"kväljningarnas", "kväljningen", "kväljningens", "kålsupare",
				"labil", "labila", "labilare", "labilast", "labilaste",
				"labilt", "led", "ledsen", "ledsna", "ledsna", "ledsnare",
				"ledsnare", "ledsnast", "ledsnaste", "ledsnat", "lessen",
				"lessen", "lessna", "lessnade", "lessnare", "lessnast",
				"lessnaste", "lessnat", "lida", "lidande", "lider",
				"likgiltig", "likgiltiga", "likgiltigare", "likgiltigast",
				"likgiltigast", "likgiltigaste", "likgiltigt", "livrädd",
				"livrädda", "livräddare", "livräddast", "livräddaste", "ljuga",
				"ljugit", "ljög", "lyssnar inte", "läskig", "läskig",
				"läskiga", "läskigare", "läskigare", "läskigast", "läskigaste",
				"läskighet", "läskigheter", "läskigheter", "läskigheterna",
				"läskigheternas", "läskigt", "läskigt", "lättlurade",
				"lågkonjunktur", "lögn", "lögnare", "lögnaren", "lögnarens",
				"lögnarna", "lögnarnas", "lögnen", "lögnens", "lögner",
				"lögners", "lögnhals", "lögns", "löje", "löjet", "löjets",
				"löjeväckande", "lönlös", "lönlösa", "lönlösare", "lönlösast",
				"lönlösaste", "lönlöst", "mackulera", "mackulerad",
				"mackulerade", "mackulerar", "mackuleras", "mackulerat",
				"mackulerats", "makulera", "makulerad", "makulerade",
				"makulerar", "makuleras", "makulerat", "makulerats",
				"meningslös", "meningslös", "meningslösa", "meningslösare",
				"meningslösare", "meningslösast", "meningslösaste",
				"meningslöshet", "meningslösheten", "meningslöshetens",
				"meningslösheter", "meningslösheterna", "meningslösheternas",
				"menlig", "menligare", "menligast", "menligaste", "menligt",
				"menlös", "menlösa", "menlösare", "menlösare", "menlösast",
				"menlöst", "miljöovänlig", "miljöovänliga", "minus", "minuset",
				"minussida", "minussida", "minussidan", "minussidan",
				"miserabel", "miserabelt", "miserabla", "miserablare",
				"miserablast", "miserablaste", "missbelåten", "missbelåtet",
				"missbelåtna", "missbelåtnare", "missbelåtnaste", "misslynt",
				"misslynta", "misslyntare", "misslyntast", "misslyntaste",
				"missnöjd", "missnöjda", "missnöjdare", "missnöjdast",
				"missnöjdaste", "missnöjdheten", "missnöjdhetens", "missnöje",
				"missnöjen", "missnöjets", "missnöjt", "misär", "misär",
				"misären", "misären", "mjäkig", "mjäkig", "mjäkiga", "mjäkiga",
				"mjäkigare", "mjäkigare", "mjäkigast", "mjäkigaste",
				"mjäkigaste", "mjäkigt", "motbjudande", "motgång", "motgångar",
				"motgångarna", "motgångarnas", "motgången", "motgångens",
				"motgångens", "murken", "murkna", "murknare", "murknast",
				"murknaste", "människofientlig", "mörk", "mörka", "mörkare",
				"mörkast", "mörkaste", "mörkna", "mörkt", "nackdel", "nackdel",
				"nackdelar", "nackdelarna", "nackdelen", "naturovänlig",
				"naturovänliga", "ned", "nederlag", "nederlagen", "nederlagen",
				"nedgång", "nedgången", "nedrig", "nedriga", "nedrigare",
				"nedrigare", "nedrigaste", "nedrigt", "nedstämda",
				"nedstämdare", "nedstämdare", "nedstämdast", "nedstämdaste",
				"nedstämdaste", "nedstämdhet", "nedstämdhet", "nedstämdheten",
				"nedstämdheten", "nedsätta", "nedsätta", "nedsättande",
				"nedsättande", "nedsättning", "nedåt", "nedåtgående",
				"negativ", "negativa", "negativare", "negativast",
				"negativast", "negativaste", "negativaste", "negative",
				"negativitet", "negativt", "negativt", "ner", "nere",
				"nergång", "nervös", "nervösa", "nervösa", "nervösare",
				"nervösare", "nervösast", "nervösast", "nervösaste",
				"nervösaste", "nervöst", "neråt", "oangelägen", "oangelägen",
				"oangeläget", "oangeläget", "oangelägna", "oangelägnare",
				"oangelägnare", "oangelägnast", "oangelägnaste",
				"oangelägnaste", "oanständig", "oanständigare",
				"oanständigaste", "oanständigaste", "oanständigt",
				"oanvändbar", "oanvändbar", "oanvändbara", "oanvändbara",
				"oanvändbarare", "oanvändbarare", "oanvändbaraste",
				"oanvändbart", "oaptitligare", "oaptitligast", "oaptitligt",
				"oattraktiv", "oattraktiva", "oattraktivare", "oattraktivast",
				"oattraktivast", "oattraktivaste", "oattraktivaste",
				"oattraktivt", "obehag", "obehag", "obehaga", "obehaga",
				"obehaglig", "obehaglig", "obehagliga", "obehagliga",
				"obehagligare", "obehagligare", "obehagligast", "obehagligast",
				"obehagligaste", "obehagligaste", "obehagligt", "obehagligt",
				"obekväm", "obekväma", "obekvämare", "obekvämare",
				"obekvämast", "obekvämast", "obekvämaste", "obekvämaste",
				"obestånd", "obestånden", "obestånden", "obeståndet",
				"obeståndet", "obetydlig", "obetydliga", "obetydligare",
				"obetydligast", "obetydligaste", "obetydligt", "obilda",
				"obilda", "obildad", "obildade", "obildat", "obrukbar",
				"obrukbara", "obrukbarare", "obrukbarare", "obrukbarast",
				"obrukbarast", "obrukbart", "oduglig", "oduglig", "odugliga",
				"odugliga", "odugligare", "odugligare", "odugligaste",
				"oerfarenhet", "oerfarenhet", "oerfaret", "oerfaret",
				"oerfarna", "oerfarna", "ofin", "ofin", "ofina", "ofinare",
				"ofinare", "ofinast", "ofinast", "ofinaste", "ofinaste",
				"ofint", "ofint", "ofördelaktig", "ofördelaktig",
				"ofördelaktigare", "ofördelaktigare", "ofördelaktigast",
				"ofördelaktigaste", "ofördelaktigaste", "ofördelaktigt",
				"oförmögen", "oförmögen", "oförmögenhet", "oförmögenhet",
				"oförmögna", "oförmögna", "oförmögnare", "oförmögnaste",
				"ogynnsamma", "ohälsa", "ohälsosam", "ohälsosam",
				"ohälsosamma", "ohälsosamma", "ohälsosammare", "ohälsosammast",
				"ohälsosammast", "ohälsosamt", "ohälsosamt", "ohärlig",
				"ohärliga", "ohärligare", "ohärligast", "ohärligaste",
				"ohärligaste", "ohärligt", "ohärligt", "okvalificera",
				"okvalificera", "okvalificerade", "olidlig", "olidliga",
				"olidligare", "olidligast", "olidligaste", "olidligt",
				"olidligt", "olycka", "olycka", "olyckan", "olyckan",
				"olyckans", "olyckans", "olycklig", "olycklig", "olyckliga",
				"olyckliga", "olyckligare", "olyckligare", "olyckligast",
				"olyckligast", "olyckligaste", "olyckligaste", "olyckligt",
				"olägenhet", "olägenheter", "olägenheterna", "olägenheterna",
				"oläglig", "olägliga", "olägligare", "olägligare",
				"olägligast", "olägligaste", "olägligt", "olägligt",
				"olämplig", "olämplig", "olämpliga", "olämpliga",
				"olämpligare", "olämpligast", "olämpligast", "olämpligaste",
				"olämpligaste", "olämpligt", "olämpligt", "omiljövänlig",
				"omiljövänliga", "omöjlig", "omöjlig", "omöjliga", "omöjliga",
				"omöjligare", "omöjligare", "omöjligast", "omöjligast",
				"omöjlighet", "omöjlighet", "omöjligheter", "omöjligt", "ond",
				"onda", "onda", "ondare", "ondare", "ondast", "ondast",
				"ondaste", "ondaste", "ondsint", "ondsint", "ondsinta",
				"ondsinta", "ondsintare", "ondsintare", "ondsintast",
				"ondsintast", "ondsintaste", "ondska", "ondskan", "ondskans",
				"ondskans", "ondskefull", "ondskefull", "ondskefulla",
				"ondskefullare", "ondskefullaste", "ondskefullhet",
				"ondskefullhet", "ondskefullt", "onyttiga", "onyttigast",
				"onyttigaste", "onyttigaste", "onyttigt", "onödig", "onödiga",
				"onödigare", "onödigare", "onödigast", "onödigaste", "onödigt",
				"opasslig", "opassliga", "opassligt", "oprofessionell",
				"oprofessionell", "oprofessionellare", "oprofessionellast",
				"oprofessionellast", "oprofessionellaste", "oprofessionellt",
				"oprofessionellt", "oro", "oroa", "oroa", "oroad", "oroad",
				"oroade", "oroade", "oroande", "oroande", "oroar", "oroar",
				"oroas", "oroas", "oroat", "oroat", "oroats", "orolig",
				"oroliga", "oroliga", "oroligare", "oroligare", "oroligast",
				"oroligast", "oroligaste", "oroligaste", "orolighet",
				"oroligheterna", "oroligheternas", "oroligheternas", "oron",
				"orons", "orosmoln", "orosmolnets", "orsaka cancer",
				"orsakar cancer", "oskön", "osköna", "oskönast", "oskönt",
				"osnygg", "osnygg", "osnygga", "osnyggare", "osnyggast",
				"osnyggast", "osnyggaste", "osnyggt", "ostabil", "ostabila",
				"ostabilare", "ostabilaste", "ostabilt", "ostadig", "ostadiga",
				"ostadigare", "ostadigast", "ostadigaste", "ostadigt", "osund",
				"osund", "osunda", "osundare", "osundast", "osundaste",
				"osunt", "osympatisk", "osympatiska", "osympatiskare",
				"osympatiskaste", "osympatiskt", "osäkerhetens",
				"osäkerhetsfaktor", "osäkert", "osäkra", "osäkrare",
				"osäkrast", "osäkraste", "otjänlig", "otjänliga",
				"otjänligare", "otjänligast", "otjänligaste", "otjänligt",
				"otrevlig", "otrevliga", "otrevligare", "otrevligast",
				"otrevligaste", "otrevligheter", "otrevligt", "otrivsam",
				"otrivsamma", "otrivsammare", "otrivsammast", "otrivsammaste",
				"otrivsamt", "otur", "oturen", "oturens", "oturlig",
				"oturliga", "oturligare", "oturligast", "oturligaste",
				"oturligt", "otursförföljd", "otursförföljda",
				"otursförföljdare", "otursförföljdast", "otursförföljdaste",
				"otursförföljt", "otydlig", "otydliga", "otydligare",
				"otydligast", "otydligaste", "otydligt", "otäck", "otäckast",
				"otäckaste", "otäckingar", "otäckt", "outhärdlig",
				"outhärdligare", "outhärdligast", "outhärdligast",
				"outhärdligaste", "oviktigast", "oviktigaste", "oviktigt",
				"oväsentlig", "oväsentligare", "oväsentligast",
				"oväsentligast", "oväsentligaste", "oväsentligt", "oärlig",
				"oärliga", "oärligare", "oärligast", "oärligaste", "oärlighet",
				"oärligheter", "oärligheterna", "oärligt", "pervers",
				"perversa", "perversare", "perversare", "perversast",
				"perversaste", "perverst", "pervertera", "perverterad",
				"perverterade", "perverterande", "perverterar", "perverteras",
				"perverterat", "pessimism", "pessimismen", "pessimismens",
				"pessimist", "pessimister", "pessimisterna", "pessimisternas",
				"pessimistisk", "pessimistiska", "pessimistiskare",
				"pessimistiskast", "pessimistiskaste", "pest", "pesten",
				"pestens", "pinsam", "pinsamhet", "pinsamheter",
				"pinsamheterna", "pinsamma", "pinsammare", "pinsammast",
				"pinsammaste", "pinsamt", "pucko", "puckon", "puckona",
				"puckot", "purkna", "pyrt", "ras", "rasa", "rasar", "rasat",
				"rasats", "raserande", "raserane", "raserar", "raseras",
				"raserat", "raserats", "rev", "revs", "ringakta", "ringaktad",
				"ringaktade", "ringaktande", "ringaktar", "ringaktas",
				"ringaktat", "ringaktats", "risig", "risiga", "risigare",
				"risigast", "risigaste", "risigt", "risk", "risken", "riskens",
				"risker", "riskerna", "riskfull", "riskfullt", "riskfylld",
				"riskfyllda", "ruffig", "ruffiga", "ruffigare", "ruffigast",
				"ruffigaste", "ruffigt", "ruin", "ruinen", "ruinens",
				"ruinera", "ruinerad", "ruinerade", "ruinerar", "ruineras",
				"ruinerat", "ruinerats", "ruskiga", "ruskigare", "ruskigare",
				"ruskigast", "ruskigast", "ruskigaste", "ruskigt", "ruskigt",
				"rutten", "ruttet", "ruttna", "ruttnad", "ruttnade", "ruttnar",
				"ruttnare", "ruttnast", "ruttnaste", "ruttnat", "ryslig",
				"rysliga", "rysliga", "rysligare", "rysligare", "rysligast",
				"rysligast", "rysligaste", "rysligaste", "rysligt", "rysligt",
				"rädd", "rädda", "räddare", "räddast", "räddast", "räddaste",
				"räddhågad", "räddhågare", "räddhågast", "räddhågaste",
				"räddhågat", "rädsla", "rädsla", "rädslan", "rädslan",
				"rädslans", "rädslans", "rädslor", "rädslor", "rädslorna",
				"rädslorna", "rädslornas", "rädslornas", "råtta", "shit",
				"sjaskig", "sjaskig", "sjaskiga", "sjaskiga", "sjaskigare",
				"sjaskigare", "sjaskigast", "sjaskigast", "sjaskigaste",
				"sjaskigaste", "sjaskigt", "sjuk", "sjuka", "sjuka", "sjukare",
				"sjukare", "sjukast", "sjukast", "sjukaste", "sjukaste",
				"sjukdom", "sjukdom", "sjukdomen", "sjukdomen", "sjukdomens",
				"sjukdomsfall", "sjukdomsfall", "sjukdomsfallen",
				"sjukdomsfallen", "sjukdomsfallet", "sjukdomsfallet",
				"sjukdomstecken", "sjukdomstecken", "sjukdomstecknen",
				"sjukdomstecknen", "sjuklig", "sjukliga", "sjukligare",
				"sjukligast", "sjukligaste", "sjukligaste", "sjukligt",
				"sjukligts", "sjukligts", "sjukt", "sjukt", "skit", "skitar",
				"skitar", "skitarna", "skitarna", "skitdålig", "skitdålig",
				"skitdåliga", "skitdåliga", "skitdåligare", "skitdåligare",
				"skitdåligast", "skitdåligast", "skitdåligaste", "skitdåligt",
				"skitdåligt", "skiten", "skiten", "skitens", "skitens",
				"skitful", "skitful", "skitkass", "skitkass", "skitkassa",
				"skitkassa", "skitkassare", "skitkassare", "skitkassast",
				"skitkassast", "skitkassaste", "skitkassaste", "skitkasst",
				"skitkasst", "skraj", "skraj", "skraja", "skraja", "skrajare",
				"skrajare", "skrajast", "skrajast", "skrajaste", "skrajaste",
				"skrajt", "skral", "skrala", "skralare", "skralast",
				"skralaste", "skraltig", "skrota", "skrotad", "skrotade",
				"skrotar", "skrotas", "skrotat", "skrotats", "skröplig",
				"skröpliga", "skröpligare", "skröpligast", "skröpligaste",
				"skröpligt", "skämd", "skämda", "skämdare", "skämdast",
				"skämdaste", "skämde", "skämma", "skämskuddar", "skämskuddars",
				"skämskudde", "skämskudden", "skämskuddens", "skämskuddes",
				"slut", "sluta", "slutade", "slutar", "slutat", "slö", "slöa",
				"slöare", "slöast", "slöaste", "slött", "smärta", "smärtade",
				"smärtan", "smärtande", "smärtans", "smärtans", "smärtat",
				"smärtsam", "smärtsamma", "smärtsamma", "smärtsammare",
				"smärtsammast", "smärtsammast", "smärtsammaste", "smärtsamt",
				"smärtsamt", "snusk", "snusket", "snuskets", "snuskig",
				"snuskiga", "snuskigare", "snuskigast", "snuskigaste",
				"snuskighet", "snuskigheter", "snuskigheterna", "snuskigt",
				"snuskigt", "sorg", "sorgen", "sorgens", "sorglig", "sorgliga",
				"sorgligare", "sorgligast", "sorgligaste", "sorgliget",
				"sorgligeter", "sorgligeterrna", "sorgligeterrnas", "sorgligt",
				"spe", "spefull", "spefulla", "spefullt", "splittra",
				"splittrad", "splittrade", "splittrar", "splittras",
				"splittrat", "splittrats", "spoliera", "spoliera", "spolierad",
				"spolierade", "spolierar", "spolierar", "spolieras",
				"spolierat", "spolierats", "spänd", "spända", "spändare",
				"spändast", "spändaste", "spänts", "störa", "störande",
				"störd", "störda", "stördare", "stördast", "stördes", "störts",
				"superdålig", "superkassa", "superkasst", "sur", "sura",
				"surade", "surar", "surare", "surast", "suraste", "suraste",
				"surt", "surt", "surtanten", "svacka", "svackan", "svackans",
				"svackor", "svackorna", "svackornas", "svag", "svaga", "svaga",
				"svagare", "svagast", "svagaste", "svagt", "svagt", "svajig",
				"svajiga", "svajigare", "svajigare", "svajigast", "svajigaste",
				"svajigt", "svajigt", "svart", "svarta", "svartare",
				"svartast", "svartaste", "svartsyn", "svartsynen",
				"svartsynens", "svin", "svina", "svinade", "svinaktiga",
				"svinaktigt", "svinar", "svinat", "svinig", "svinigt", "svår",
				"svåra", "svårare", "svårast", "svåraste", "svårighet",
				"svårigheter", "svårigheterna", "svårnedbrytbar",
				"svårnedbrytbara", "svårnerbrytbar", "sämre", "sämsta", "sög",
				"sörja", "sörjd", "sörjde", "sörjdes", "sörjer", "sörjt",
				"sörjts", "tarvlig", "tarvliga", "tarvligare", "tarvligast",
				"tarvligaste", "tarvligt", "taskig", "taskiga", "taskigare",
				"taskigast", "taskigaste", "taskigaste", "taskigt",
				"tillbakagång", "tillbakagångar", "tillbakagångarna",
				"tillbakagångarnas", "tillbakagången", "tillbakagångens",
				"tillgjord", "tillgjorda", "tillgjort", "torftig", "torftiga",
				"torftigare", "torftigare", "torftigast", "torftigt", "trasig",
				"trasiga", "trasigare", "trasigast", "trasigast", "trasigaste",
				"trasigt", "trassligast", "trassligaste", "trassligt", "trist",
				"trista", "tristare", "tristast", "tristast", "tristaste",
				"tristess", "trubbel", "trubblen", "trubblet", "trubblet",
				"trälig", "trälig", "träliga", "träliga", "träligare",
				"träligast", "träligaste", "träligt", "tråka", "tråka",
				"tråkad", "tråkade", "tråkar", "tråkas", "tråkat", "tråkats",
				"tråkats", "tråkig", "tråkiga", "tråkigare", "tråkigast",
				"tråkigaste", "tråkigheter", "tråkigt", "trött", "tröttare",
				"tuff", "tuffa", "tuffare", "tuffast", "tuffaste", "tufft",
				"tumult", "tumultet", "tung", "tunga", "tungare", "tungast",
				"tungaste", "turbulent", "turbulenta", "turbulentare",
				"turbulentast", "turbulentast", "turbulentaste", "tveksam",
				"tveksamma", "tveksammare", "tveksammast", "tveksammast",
				"tveksammaste", "tveksamt", "tveksamt", "tära", "tära", "tärd",
				"undergräva", "undergrävde", "undergrävdes", "undergräver",
				"undergrävt", "underminera", "underminerade", "underminerar",
				"underminerat", "undermålig", "undermåliga", "undermåligare",
				"undermåligast", "undermåligaste", "undermåligt", "undflyende",
				"uppriven", "upprivet", "upprivna", "upprivnare", "upprivnast",
				"upprivnaste", "upprör", "uppröra", "upprörande", "upprörd",
				"upprörda", "upprörde", "upprördes", "upprördhet", "upprörs",
				"upprört", "urbota", "urdåliga", "urdåligt", "urusel",
				"uruselt", "urusla", "uruslare", "uruslast", "uruslaste",
				"usch", "uschlig", "uschliga", "uschligare", "uschligast",
				"uschligaste", "uschligt", "usel", "uselt", "ush", "usla",
				"uslare", "uslast", "uslaste", "vackla", "vacklande",
				"vacklar", "vacklat", "vanartig", "vanartiga", "vanartigare",
				"vanartigast", "vanartigaste", "vanartigt", "vilket skämt",
				"vilseleda", "vilseledande", "vilseledd", "vilselett", "x-(",
				"|:", "|;", "}:", "};", "}=", "ångest/panikattacker",
				"ångestattacker", "ångestattackerna", "ångestladdade",
				"överskattad", "överskattade", "överskattades", "överskattas",
				"överskattat", "överskattats" };
		String[] samma = { "häst", "hund", "hunden", "hästen" };
		String[] foods = { "chestnut", "chestnuts", "chestnut's", "chestnuts'" };
		for (String w : foods) {
			words.add(w);
		}
		GetVectorsForWord g = new GetVectorsForWord(esProperties);
		PolarExplorer pe = new PolarExplorer(esProperties);
		try {
			// System.out.println(pe.coherence(g,words));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// String datadir = "/Users/jussi/aktuellt/Gavagai/projekt/2013.geo/";
		// String wordspacefile = "platser.worddocspace";
		// PolarExplorer pe = new PolarExplorer(datadir+wordspacefile);
		// pe.makeNetwork();
		// System.out.println(pe.valence.keySet().size());
		// for (int i = 0; i < pe.steps.length; i++) {
		// System.out.println("===========================================");
		// System.out.println(pe.steps[i]+" "+pe.edges.get(pe.steps[i]).size());
		// int j = 0;
		// for (String v: pe.valence.keySet()) {
		// if (pe.valence.get(v)[i] > 1) j++;
		// }
		// System.out.println(pe.steps[i]+" "+j+" "+pe.valence.keySet().size());
		// }
		// for (int i2 = 0; i2 < pe.steps.length; i2++) {
		// System.out.println("===========================================");
		// int j1 = 0;
		// for (Edge e: pe.edges.get(pe.steps[i2])) {
		// System.out.println(j1+" "+pe.steps[i2]+" "+e);
		// j1++;
		// }
		// int j2 = 0;
		// for (String v: pe.valence.keySet()) {
		// if (pe.valence.get(v)[i2] > 1) {
		// System.out.println(j2+" "+pe.steps[i2]+" "+v+" "+pe.valence.get(v)[i2]);
		// j2++;
		// }
		// }
		// }
	}
}

// Vector<Node> nodes;
// Vector<EdgeN> edgesN;
// private class Node {
// String token;
// int valence;
// public Node(String s) {this.token = s;}
// }
// private class EdgeN {
// public EdgeN(Node l, Node r, float w) {this.left = l; this.right = r;
// this.weight = w;}
// Node left;
// Node right;
// float weight;
// public String toString() {return left.token+"-"+weight+"-"+right.token;}
// }
// public void makeNetworkN() {
// Hashtable<String,Node> index = new Hashtable<String,Node>();
// nodes = new Vector<Node>();
// edgesN = new Vector<EdgeN>();
// for (SparseVector v: vecs) {
// if (v.vals != null && v.frequency > 5) {
// Node nv;
// if (index.contains(v.token)) {
// nv = index.get(v.token);
// } else {
// nv = new Node(v.token);
// index.put(v.token,nv);
// nodes.add(nv);
// }
// for (SparseVector u: vecs) {
// if (u != v && u.vals != null && u.frequency > 5) {
// Node nu;
// if (index.contains(u.token)) {
// nu = index.get(u.token);
// } else {
// nu = new Node(u.token);
// index.put(u.token,nu);
// nodes.add(nu);
// }
// try {
// float d = VectorMath.cosineSimilarity(v.vals, u.vals);
// if (d > threshold) {
// EdgeN edge = new EdgeN(nv,nu,d);
// edgesN.add(edge);
// // debug(edge);
// }
// } catch (Exception e) {
// error(u + " ? " + v);
// }
// }
// }
// v.vals = null;
// }
// }
// }

