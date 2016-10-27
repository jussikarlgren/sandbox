package com.gavagai.jussisandbox;

/*
 *
	Perceptron.java - the Perceptron class implements a Perceptron.
	- first version downloade from net, written by Akshat Singhal 2007
	- modified by JiK 2013	
 */
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Comparator;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gavagai.jussiutil.ConfusionMatrix;
import com.gavagai.jussiutil.Dataset;
import com.gavagai.jussiutil.Dataset.Individual;

public class Perceptron {
	static private int DEFAULTNUMBEROFFEATURES = 27;
	private int numberOfFeatures = 27;
	static private int numberOfCategories = 2;
	double[] weights;
	double learningRate=0.0;
	double categorisationCriterion=0.0;
	double errorThreshold=0.0;    
	double[][] weightHistory;
	int HISTORYSIZE = 5, 
			historyCounter=0;
	private double currentError = 0;
	private double currentIterationError = 0;
	private int maxRuns = 10;

	private Log logger = LogFactory.getLog(Perceptron.class);
	private ConfusionMatrix errors;
	private Vector<Integer> index;
	private Dataset<Individual> dataset;
	private double portion;

	public Perceptron() {
		this(DEFAULTNUMBEROFFEATURES);
	}
	public Perceptron(int n) {
		this.numberOfFeatures = n;
		weights = new double[numberOfFeatures];
		weightHistory=new double[HISTORYSIZE][0];
		for (int i = 0; i < numberOfFeatures; i++)
			weights[i]=Math.random() - 0.5;
		errors = new ConfusionMatrix(numberOfCategories);
	}

	public void setCategorisationCriterion(double threshold){
		this.categorisationCriterion=threshold;
	}
	public void setLearningRate(double rate){
		this.learningRate=rate;
	}
	public double getError(){
		return currentIterationError;
	}
	public void setMaxruns(int m) {
		maxRuns = m;
	}
	// ni hör väl alla att klockan slår men ni kan inte hitta kläppen ja tiden kommer och tiden går och jag har en spricka i läppen
	/*
	 *   predict() - perceptron maxi action bonanza: true if it fires on this individual
	 */
	public boolean predict(Individual individual) {
		double value=0;
		double[] individualFeatures = individual.getFeatures();
		for (int i=0;i<numberOfFeatures;i++){
			value += ((double)individualFeatures[i])*weights[i];
		}
		if (value > categorisationCriterion)
			return true;
		else
			return false;
	}
	/*
	    train() - trains the perceptron on a number of input sets      
	 */
	public void train(int iteration, Dataset<Individual> trainingset) {
		currentIterationError = 0;		
		while (trainingset.size() > 0) { // as long as we have unseen items in input set
			double[] newweights = weights;
			Individual d = trainingset.drawRandomIndividual();
			boolean perceptronFires = predict(d);
			// if item is target and perceptron fires - no learning. 
			// if item is target and no reaction? positive reinforcement. 
			// if not target and perceptron fires? negative 
			currentError = impact(perceptronFires)*((d.isTarget() == perceptronFires)?0:1*(perceptronFires?-1:1));
			currentIterationError += Math.abs(currentError);
			if (currentError != 0) {
				for (int j = 0; j < numberOfFeatures; j++){
					//	if (d.getFeature(j) > 0) {
					// PERCEPTRON
//					newweights[j] += learningRate*currentError*d.getFeature(j);
					// LOGLOSS
//					newweights[j] += learningRate*Math.log(currentError*d.getFeature(j));
					// HINGE
					double l = 1-currentError*d.getFeature(j);
					newweights[j] += l > 0?l:0;
					//		logger.info(coefficients(d.getFeatures())+"*"+coefficients(weights)+"->"+ perceptronFires+"<-"+currentError+"->"+d.isTarget()+"->"+coefficients(newweights));
					setWeights(newweights);
					//}
				}	
			}
		}
		logger.debug("Iteration "+iteration+" completed with error "+currentIterationError+" "+configuration()+".");
	}
	private double cost = 0d;
	public void setCost(double cost) {
		this.cost = cost;
	}
	// not used now - cost function for firing in error vs not firing when should've
	private double impact(boolean category) {
		return category?1d:(1-cost);
	}
	/*
	 *	    trainOnFiles() - trains the perceptron on a gold standard enabled file
	 */
	public void trainOnFile(String datafile) {
		logger.info("Starting perceptron training from "+datafile);
		dataset = new Dataset<Individual>(datafile);
		train(dataset);
	}
	/*
	 *	    train() - trains the perceptron on a gold standard enabled dataset
	 */
	public void train(Dataset d) {
		dataset = d;
		logger.info("Starting perceptron training with "+configuration());
		logger.info("Number of positive targets: "+dataset.getNumberOfTargets());
		int iteration=0;	
		dataset.partition(portion);
		currentIterationError = dataset.getTrainingset().size();
		logger.info("Number of positive to train: "+dataset.getTrainingset().getNumberOfTargets()+" of "+dataset.getTrainingset().size());
		logger.info("Number of positive to test: "+dataset.getTestset().getNumberOfTargets()+" of "+dataset.getTestset().size());
		while(getError() > errorThreshold && iteration < maxRuns) {
			iteration++;
			train(iteration,(Dataset<Individual>) dataset.getTrainingset().clone());		    
		}
		logger.info("Completed training cycle  " +configuration() + " after " + iteration + " iterations and " + currentIterationError + " error.");
		logger.info("Heaviest coefficients "+briefCoefficients(5,weights,dataset.getLegend()));
		if (dataset.getTestset().size() > 0) {
			logger.debug("Testing on "+dataset.getTestset().size()+" items.");
			testOnData(dataset.getTestset());
		}
	}
	/*
	 * testOnData() - Runs the perceptron on test set 
	 */    
	int error = 0;
	int miss = 0;
	public void testOnData(Dataset<Individual> testcases) {
		logger.info("Starting perceptron run for "+testcases.size()+" sized data set with "+configuration());
		currentIterationError = testcases.size();
		fireAway(testcases);
		double precision = 1, recall = 0;
		if (testcases.getNumberOfTargets() > 0) {recall = Math.round(10000*((double)(testcases.getNumberOfTargets() - miss)/testcases.getNumberOfTargets()))/100;}
		if (testcases.getNumberOfTargets()-miss+error > 0) {		
			precision = Math.round(10000*((double)(testcases.getNumberOfTargets() - miss)/(testcases.getNumberOfTargets()-miss+error)))/100;}
		logger.info("Completed test run with " + error + " errors (precision: " + precision + ") and " + miss + " misses (recall: " + recall + ") out of "+testcases.getNumberOfTargets() + " examples in a data set of " + dataset.size() +".");
		logger.info(errors);
	}
	public int fireAway(Dataset<Individual> testcases) {
		for (Individual i: testcases) {
			boolean perceptronFires = predict(i);
			miss += (i.isTarget() && ! perceptronFires)?1:0;
			error += (! i.isTarget() && perceptronFires)?1:0;
			errors.increment(i.isTarget()?"Target":"Other",perceptronFires?"Target":"Other");
		}
		return error;
	}
	/*
	 * 	    printStatusLine() - In order to print the debugging output of this 
	 *      Perceptron's training, prints one line for one  epoch and one input set
	 */    
	public void printStatusLine(int epoch, double[] input, 
			int output_actual, int output_desired){
		System.err.printf("%d\t", epoch);
		for (int i=0;i<numberOfFeatures;i++)
			System.err.printf("%+10.2f\t",input[i]);
		System.err.printf("%d\t",output_desired);
		System.err.printf("%d\t",output_actual);
		System.err.printf("%+10.2f\t",currentError);
		for (int i=0;i<numberOfFeatures;i++)
			System.err.printf("%.2f\t",weights[i]);
		System.err.print("\n");	
	}
	public String coefficients(double[] vector) {
		String ws = "";
		for (int i=0;i<vector.length;i++)
			ws += 
			//dataset.getLegend()[i]+":"+
			Math.round(100d*vector[i])/100d + " ";
		return " [ " +ws  + "]";		
	}
	public String briefCoefficients(int briefness, double[] vector, String[] labels) {
		class Item {
			String name; double value;
			Item(String n, double d) {this.name = n; this.value = d;}
			public String toString() {return this.name+":"+this.value; }
		}
		String brief = "";
		if (briefness > dataset.getNumberOfFeatures()) {
			briefness = dataset.getNumberOfFeatures();
		}
		class Valuecomp implements Comparator<Item> {
			public int compare(Item o1, Item o2) {
				if (Math.abs(o1.value) > Math.abs(o2.value)) return 1;
				else if (Math.abs(o1.value) < Math.abs(o2.value)) return -1;
				else return 0;
			}
		}
		TreeSet<Item> maxitems = new TreeSet<Item>(new Valuecomp());
		for (int k = 0; k < vector.length; k++) {maxitems.add(new Item(labels[k],vector[k]));}
		NavigableSet<Item> maxdrops = maxitems.descendingSet();
		for (int i = 0; i < briefness; i++) 
			try {brief += maxdrops.pollFirst().toString()+" ";} catch (NullPointerException ee) {brief += "..:..";}
		return brief;
	}
	public String configuration() {	
		return numberOfFeatures + " " + learningRate + " " + categorisationCriterion +  coefficients(weights);
	}

	/*
	    setWeights() - update the weight matrix of the Perceptron
	 */
	private void setWeights(double[] weights) {
		if (weights.length == numberOfFeatures){
			weightHistory[(historyCounter++) % HISTORYSIZE] = this.weights;
			this.weights = weights;	    
		} else {
			IndexOutOfBoundsException e = new IndexOutOfBoundsException();
			throw e;
		}
	}
	/*
	 * @param args
	 */
	public static void main(String[] args) throws IOException, FileNotFoundException {
		String file = "/bigdata/research/experiments/2014.10.gamergate/k1.simple";
		//		String file = "/Users/jussi/aktuellt/Gavagai/projekt/replab2013/discrim/scoresen";
		double floor = 0.6;
		double roof = 0.61;
		double learningRate = 0.1; 
		double xvalportion = 0.1;
		for (double threshold=floor; threshold < roof; threshold += 0.1) {
			Perceptron p = new Perceptron();
			p.setLearningRate(learningRate);
			p.setCategorisationCriterion(threshold);
			p.setMaxruns(1);
			p.setPortion(xvalportion);
			p.trainOnFile(file);
			//+" "+
			//					p.runInputFile(file));
			//		System.out.print(p.errors);
		}
	}

	public void setPortion(double portion) {
		this.portion = portion;
	}
}
