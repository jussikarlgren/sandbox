package com.gavagai.logonaut;

import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

public class CompoundSplitter {
	HashMap<String,Vector<StringWithCost>> starts;
	HashMap<String,Vector<StringWithCost>> ends;
	HashMap<String,HashMap<String,Integer>> values;
	HashMap<String,Integer> cache;
	Vector<String> seencache;
	Fonotax f;

	public CompoundSplitter(String language) {
		starts = new HashMap<String,Vector<StringWithCost>>();
		ends = new HashMap<String,Vector<StringWithCost>>();
		values = new HashMap<String,HashMap<String,Integer>>();
		f = new Fonotax(language);
		cache = new HashMap<String,Integer>();
		seencache = new Vector<String>();
	}

	private void addIn(String bit, int bc, String hel, int hc) {
		Vector<StringWithCost> d;
		int cost = hc-bc;
		if (bit.length() < 3) return;
		if (starts.containsKey(bit))  {
			d = starts.get(bit);
		} else {
			d = new Vector<StringWithCost>();
		}
		d.add(new StringWithCost(hel,cost));
		starts.put(bit, d);
		if (ends.containsKey(bit))  {
			d = ends.get(bit);
		} else {
			d = new Vector<StringWithCost>();
		}
		d.add(new StringWithCost(hel,cost));
		ends.put(bit, d);
	}
	public void add(String word) {
		if (seencache.contains(word)) return;
		if (word.length() < 1) return;
		seencache.add(word);
		String[] ss = f.syllables(word);
		int hc = f.cost(word);
		for (String s : ss) {
			int bc ;
			if (cache.containsKey(s))
				bc = cache.get(s);
			else {
				bc = f.cost(s);
				cache.put(s,bc);
			}
			addIn(s,bc,word,hc);
		}
	}

	//	public void values(String word) {
	//		String[] ss = f.syllables(word);	
	//	}

	public String[] splits() {
		Vector<String> nn = new Vector<String>();
		String n;
		//		for (StringWithCost swc: starts.values()) {
		//			s = swc.w;
		//			if (s.length() < 1) continue;
		//			n = s+" ";
		//			for (StringWithCost swc: starts.get(s)) {
		//				n += swc+" ";
		//			}
		//			nn.add(n);
		//		}
		for (String v: starts.keySet()) {
			Vector<StringWithCost> ww = starts.get(v);
			for (StringWithCost swc0: ww) {
				if (! seencache.contains(swc0.w)) continue; // skip non-words, only full words considered
				for (StringWithCost swc1: ww) {
					if (swc0.w == swc1.w) continue;
					int cost = swc0.c+swc1.c;
					if (cost < 3) {
						//					System.out.println("#"+swc0.w+"<-"+cost+"->"+swc1.w);
						if (! values.containsKey(swc0.w)) values.put(swc0.w,new HashMap<String,Integer>());
						if (values.get(swc0.w).containsKey(swc1.w)) {
							if (values.get(swc0.w).get(swc1.w) < cost) {
								values.get(swc0.w).put(swc1.w,cost);
							}
						} else {
							values.get(swc0.w).put(swc1.w, cost);
						}
					}
				}
			}
		}
		for (String v: values.keySet()) {
			//			System.out.print(v+":");
			HashMap<String,Integer> hsi = values.get(v);
			Set<String> vhsi = hsi.keySet();
			for (String vvv: vhsi) {
				//				System.out.print(vvv+"-");
				v += ":"+vvv+" "+hsi.get(vvv);
			}
			//			System.out.println();
			if (vhsi.size() > 0) 
				nn.add(v);	
		}
		String[] ss = new String[nn.size()]; 
		ss = (String[]) nn.toArray(ss);
		return ss;	
	}

	public static void main(String[] args) {
		CompoundSplitter cs = new CompoundSplitter("SV");
		cs.add("karlgren");
		cs.add("karlberg");
		cs.add("berg");
		cs.add("norrberg");
		cs.add("norrgren");
		String[] css = cs.splits();
		for (String ss: css) {
			System.out.println(ss);
		}

		//		for (String s: cs.starts.keySet()) {
		//			System.out.print(s+" ");
		//			for (StringWithCost swc: cs.starts.get(s)) {
		//				System.out.print(swc+" ");
		//			}
		//			System.out.println();
		//		}
		//		System.out.println("---");
		//		for (String s: cs.starts.keySet()) {
		//			System.out.print(s+" ");
		//			for (StringWithCost swc: cs.ends.get(s)) {
		//				System.out.print(swc+" ");
		//			}
		//			System.out.println();
		//		}
	}

}
