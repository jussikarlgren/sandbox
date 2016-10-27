package com.gavagai.logonaut;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;

import com.gavagai.mockrabbit.Permutation;
import com.gavagai.vectorgarden.GetVectorsForWord;
import com.gavagai.vectorgarden.SparseVector;
import com.gavagai.vectorgarden.VectorMath;

public class NegationExperiment {
	static String targetDirectory = "/Users/jussi/Desktop/2016.negationexperiment";
	static int NUMBEROFITEMS = 10;
	static int NUMBEROFAMPLIFIERS = 10;

	SparseVector[] itemvectors = new SparseVector[NUMBEROFITEMS];
	SparseVector[] amplifiervectors = new SparseVector[NUMBEROFAMPLIFIERS];

	public static void main(String[] args) throws FileNotFoundException {
		// getVectorsFromCore();
		NegationExperiment ne = new NegationExperiment();
		ne.readVectorsFromFile();
		Permutation permutation = Permutation.LEFT;
		ne.compareAmplifyVectorsWithTargetVectors(permutation);
		System.out.println(ne.results());
	}

	private String results() {
		return "done";
	}

	private void compareAmplifyVectorsWithTargetVectors(Permutation permutation) {
		float[] averages = new float[itemvectors.length];
		for (SparseVector s: itemvectors) {
			SparseVector closest = null;
			float closestcorr = 0f;
			float sum = 0;
			int i = 0;
			for (SparseVector a: amplifiervectors) {
				float thiscorr = VectorMath.cosineSimilarity(s.getArray(), a.getArray());
				if (thiscorr > closestcorr) {thiscorr = closestcorr; closest = a;}
				sum += thiscorr;
				i++;
			}
			averages[i] = sum/amplifiervectors.length;
		}
	}

	private void readVectorsFromFile() throws FileNotFoundException {
		String amplifierfilename = "";
		String itemfilename = "";
		Scanner fileScanner = new Scanner(new BufferedInputStream(
				new FileInputStream(targetDirectory + "/" + amplifierfilename)));
		int i = 0;
		while (fileScanner.hasNextLine() && i < NUMBEROFAMPLIFIERS) {
			String fileLine = fileScanner.nextLine();
			fileLine = fileLine.trim();
			String[] bits = fileLine.split("\t");
			SparseVector vector = new SparseVector();
			try {
				vector.parseString(bits[2]);
				vector.setToken(bits[0]);
				amplifiervectors[i] = vector;
				i++;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		fileScanner.close();
		fileScanner = new Scanner(new BufferedInputStream(new FileInputStream(
				targetDirectory + "/" + itemfilename)));
		i = 0;
		while (fileScanner.hasNextLine() && i < NUMBEROFITEMS) {
			String fileLine = fileScanner.nextLine();
			fileLine = fileLine.trim();
			String[] bits = fileLine.split("\t");
			SparseVector vector = new SparseVector();
			try {
				vector.parseString(bits[2]);
				vector.setToken(bits[0]);
				itemvectors[i] = vector;
				i++;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		fileScanner.close();
	}

	public static void getVectorsFromCore() {
		try {
			String tag = "canonical";
			String sourcefilename = "engPosLongSortNoSpace.txt";
			String amplifierfilename = "engAmplify.list";
			String logfilename = targetDirectory + "/" + tag + ".log";
			String outfilename = tag + ".vectors.ctx";
			String indexfilename = tag + ".vectors.idx";
			FileWriter logfile = new FileWriter(logfilename);
			final Properties esProperties = new Properties();
			esProperties.put("host", "core5.gavagai.se");
			esProperties.put("ear", "rabbit-core");
			esProperties.put("wordspace", "1");
			GetVectorsForWord g = new GetVectorsForWord(esProperties);
			int i = 0;

			// ComObjectDtoRedisIndexer
			// deltaComObject.addSyntagmaticNeighbour(prevComObjectRI,
			// Permutation.LEFT, 1, weight);

			// CommunicativeObject
			// public void addSyntagmaticNeighbour(short[] riv, Permutation
			// permutation, int permutationScale,
			// double weight) {
			// for (int index : riv) {
			// int position = VectorTool.calculatePosition(index, permutation,
			// permutationScale);
			// int value = (index < 0 ? -1 : 1);
			// contextVector[position] += (value * weight);
			// }
			// }

			Scanner fileScanner = new Scanner(
					new BufferedInputStream(new FileInputStream(targetDirectory
							+ "/" + sourcefilename)));
			FileWriter outfile = new FileWriter(targetDirectory + "/"
					+ outfilename);
			FileWriter indexfile = new FileWriter(targetDirectory + "/"
					+ indexfilename);
			while (fileScanner.hasNextLine()) {
				i++;
				String fileLine = fileScanner.nextLine();
				fileLine = fileLine.trim();
				String[] bits = fileLine.split(" ");
				String lemma = bits[0];
				logfile.write(lemma + "\n");
				logfile.flush();
				try {
					SparseVector indexVector = new SparseVector();
					indexVector.acceptFullVector(g.getIndexVector(lemma));
					indexfile.write(lemma + "\tindex\t"
							+ indexVector.toFullString() + "\n");
				} catch (NullPointerException e) {
					logfile.write(lemma + " failed index vector");
					indexfile.write(lemma + "\tindex\tnull\n");
				}
				try {
					SparseVector contextVector = new SparseVector();
					contextVector
							.acceptFullVector(g.getContextVector(fileLine));
					outfile.write(lemma + "\tcontext\t"
							+ contextVector.toFullString() + "\n");
				} catch (NullPointerException e) {
					logfile.write(lemma + " failed context vector");
					outfile.write(lemma + "\tcontext\tnull\n");
				}
				outfile.flush();
				indexfile.flush();
			}
			outfile.close();
			indexfile.close();
			fileScanner.close();
			fileScanner = new Scanner(new BufferedInputStream(
					new FileInputStream(amplifierfilename)));
			outfile = new FileWriter(targetDirectory + "/" + "ampl."
					+ outfilename);
			indexfile = new FileWriter(targetDirectory + "/" + "ampl."
					+ indexfilename);
			while (fileScanner.hasNextLine()) {
				i++;
				String fileLine = fileScanner.nextLine();
				fileLine = fileLine.trim();
				String lemma = fileLine;
				logfile.write(lemma + "\n");
				logfile.flush();
				try {
					SparseVector indexVector = new SparseVector();
					indexVector.acceptFullVector(g.getIndexVector(lemma));
					indexfile.write(lemma + "\tindex\t"
							+ indexVector.toFullString() + "\n");
				} catch (NullPointerException e) {
					logfile.write(lemma + " failed index vector");
					indexfile.write(lemma + "\tindex\tnull\n");
				}
				try {
					SparseVector contextVector = new SparseVector();
					contextVector
							.acceptFullVector(g.getContextVector(fileLine));
					outfile.write(lemma + "\tcontext\t"
							+ contextVector.toFullString() + "\n");
				} catch (NullPointerException e) {
					logfile.write(lemma + " failed context vector");
					outfile.write(lemma + "\tcontext\tnull\n");
				}
				outfile.flush();
				indexfile.flush();
			}
			outfile.close();
			indexfile.close();
			fileScanner.close();
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
