package com.gavagai.logonaut;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;
import java.util.Scanner;
import java.util.Vector;

import com.gavagai.jussiutil.TextWithTone;
import com.gavagai.jussiutil.Tone;
import com.gavagai.vectorgarden.GetVectorsForWord;
import com.gavagai.vectorgarden.SparseVector;

public class CaseSynonymyExperiment {
	static String dataDirectory = "/home/jussi/Desktop/2016.swadesh/noun";
	static String sourceDirectory = "/home/jussi/Desktop/2016.swadesh/tammikuu";
	static String targetDirectory = "/home/jussi/Desktop/2016.swadesh/tammikuu";
	static String outfilename = "caseexperimenttammikuu.result";
	static String logfilename = "caseexperimenttammikuu.log";

	static int DIMENSIONALITY = 2000;

	Vector<SparseVector> cases;
	int actual, noshow, errors;
	String tag;
	String itemfilename;
	String itemstring;

	public CaseSynonymyExperiment(String tag, String itemfilename) {
		cases = new Vector<SparseVector>();
		this.tag = tag;
		this.itemfilename = itemfilename;
		actual = 0;
		noshow = 0;
		errors = 0;
		itemstring = "";
	}

	public static void oneExperiment(String tag, String dataDirectory,
			FileWriter outfile, FileWriter logfile) {
		try {
			File dir = new File(dataDirectory);
			System.err.println("Reading files from "+dataDirectory);
			for (String filename: // dir.list()) {

				dir.list(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.toLowerCase().contains("ctx");
					}	
				}))  {	


				CaseSynonymyExperiment cse = new CaseSynonymyExperiment(tag,filename);
				cse.readVectorsFromFile();
				int sum = cse.actual + cse.noshow + cse.errors;
//								System.err.println(filename+"\n"+cse.tag+"\n"+cse.itemstring+"\n"+cse.actual+"\t"+cse.noshow+"\t"+cse.errors+"\t=\t"+sum+"\n");
				logfile.write(cse.tag+"\n");
				logfile.write(cse.itemstring+"\n");
				logfile.write(cse.actual + " " + cse.noshow + " " + cse.errors
						+ " = " + sum+"\n");
				logfile.flush();
				outfile.write(cse.exploreSet()+"\n");
				outfile.flush();
			}
		} catch (FileNotFoundException ee) {
			System.err.println("No data file for " + tag+ " "+ee.getMessage());
		} catch (IOException ee) {
			System.err.println("Couldn't write to " + outfile.toString() + " or to " + logfile.toString());
		} catch (Exception ee) {
			System.err.println("Failed exploring " + tag);
		}

	}

	public static void main(String[] args) {
		FileWriter outfile,logfile;
		String tag = "tammikuu";
		boolean corr = false;
		if (corr) {
			try {
				outfile = new FileWriter(targetDirectory + "/" + outfilename);
				logfile = new FileWriter(targetDirectory + "/" + logfilename);
				oneExperiment(tag,dataDirectory,outfile,logfile);
				outfile.close();
				logfile.close();
			} catch (IOException e) {
				System.err.println("Couldn't write to "+targetDirectory+"/"+outfilename);
			}
		} else {
			String filename = "radvis.analyses";
			getVectorsFromCore(tag,filename);
		}
	}

	private String exploreSet() throws Exception {
		return PolarExplorer.coherenceSortedList(cases);
	}

	private void readVectorsFromFile() throws FileNotFoundException {
		Scanner fileScanner = new Scanner(new BufferedInputStream(
				new FileInputStream(dataDirectory + "/" + itemfilename)));
//		String prev = "";
		String fileLine = "";
		while (fileScanner.hasNextLine()) {
//			prev = fileLine;
			fileLine = fileScanner.nextLine();
			fileLine = fileLine.trim();
			String[] bits = fileLine.split("\t");
			String tt = bits[0].replace("‑", "");
			SparseVector vector = new SparseVector();
			vector.setDimensionality(DIMENSIONALITY);
			try {
				if (bits[2].equals("[]")) {
					noshow++;
				} else {
					vector.parseString(bits[2]);
					vector.setToken(tt);
					cases.add(vector);
					actual++;
					itemstring += tt + " ";
					// System.err.println(vector.toString());
				}
			} catch (ArrayIndexOutOfBoundsException e) {
//				System.err.println(tt+"\n"+bits[0]+"\n"+fileLine+"\n"+prev+"\narrayindex problem\n");
				errors++;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		itemstring.trim();
		fileScanner.close();
	}

	public static void getVectorsFromCore(String tag, String filename) {
		try {
			String logfilename = targetDirectory + "/" + tag + ".log";
			String outfilename = targetDirectory + "/" + tag + ".ctx";
			String indexfilename = targetDirectory + "/" + tag + ".idx";
			String associationfilename = targetDirectory + "/" + tag + ".ass";
			String sourcefilename = sourceDirectory + "/" + filename;
			Scanner fileScanner = new Scanner(new BufferedInputStream(
					new FileInputStream(sourcefilename)));
			FileWriter logfile = new FileWriter(logfilename);
			final Properties esProperties = new Properties();
			esProperties.put("host", "core3.gavagai.se");
			esProperties.put("ear", "rabbit-core");
			esProperties.put("wordspace", "9");
			GetVectorsForWord g = new GetVectorsForWord(esProperties);
			g.setWordspace(9);
			int i = 0;
			//			FileWriter outfile = new FileWriter(outfilename);
			FileWriter associationfile = new FileWriter(associationfilename);
			while (fileScanner.hasNextLine()) {
				i++;
				String fileLine = fileScanner.nextLine();
				fileLine = fileLine.trim();
				String[] bits = fileLine.split(":");
				String surfaceform = bits[0];
				logfile.write(surfaceform + "\t" + fileLine + "\n");
				logfile.flush();
				//				try {
				//					SparseVector contextVector = new SparseVector();
				//					contextVector.acceptFullVector(g.getContextVector(surfaceform));
				//					outfile.write(fileLine + "\tcontext\t"	+ contextVector.toFullString() + "\n");
				//				} catch (NullPointerException e) {
				//					logfile.write(surfaceform + " failed to get ctx vector\n");
				//					outfile.write(surfaceform + "\tcontext\tnull\n");
				//				}
				//				outfile.flush();
				try {
					SparseVector associationVector = new SparseVector();
					associationVector.acceptFullVector(g.getAssociationVector(surfaceform));
					associationfile.write(fileLine + "\tassociation\t"
							+ associationVector.toFullString() + "\n");
				} catch (NullPointerException e) {
					logfile.write(surfaceform + " failed to get assoc vector\n");
					associationfile.write(surfaceform + "\tassociation\tnull\n");
				}
				associationfile.flush();
			}
			//			outfile.close();
			associationfile.close();
			logfile.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
