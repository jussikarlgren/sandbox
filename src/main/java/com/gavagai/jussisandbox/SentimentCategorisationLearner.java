package com.gavagai.jussisandbox;

import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gavagai.jussiutil.Dataset;
import com.gavagai.jussiutil.Dataset.Individual;
import com.gavagai.quality.monitor.BenchmarkBase;
import com.gavagai.quality.monitor.SentimentBenchmark;
import com.gavagai.quality.monitor.BenchmarkBase.Sentiment;
import com.gavagai.quality.monitor.BenchmarkBase.TextWithSentiment;
import com.gavagai.rabbit.api.monitoring.MonitoringApiRemote;
import com.gavagai.rabbit.api.monitoring.domain.FragmentPolarizationRequest;
import com.gavagai.rabbit.api.monitoring.domain.FragmentPolarizationResponse;
import com.gavagai.rabbit.api.monitoring.domain.PolarizationAlgorithm;
import com.gavagai.rabbit.api.monitoring.domain.Pole;
import com.gavagai.rabbit.utils.StatsdLogger;


public class SentimentCategorisationLearner extends SentimentBenchmark {

	private Log logger = LogFactory.getLog(SentimentCategorisationLearner.class);
	protected int positivepole = 711; //708 41;
	protected int negativepole = 712; //709 42;
	protected int lovepole = 704;
	protected int hatepole = 705;
	protected int fearpole = 703;
	protected int sexypole = 29;
	protected int iffypole = 702;
	protected int violencepole = 98;
	protected int boringpole = 637;

	public void initialiseDataSet() throws Exception {
		poleIds.add((long) positivepole);
		poleIds.add((long) negativepole);
		poleIds.add((long) lovepole);
		poleIds.add((long) hatepole);
		poleIds.add((long) fearpole);
		poleIds.add((long) sexypole);
		poleIds.add((long) iffypole);
		poleIds.add((long) violencepole);
		poleIds.add((long) boringpole);
		int numberOfFeatures = poleIds.size();
		bucket = getTexts(testIdentifier, testdirectory, testFileName, bucketsize);	
		logger.info(bucket.size()+" "+bucketsize);
		FragmentPolarizationRequest request = new FragmentPolarizationRequest();
		FragmentPolarizationResponse response = null;
		String[] sss = {"pos","neg","love","hate","fear","sexy","iffy","bang","dull"};
		Dataset<Individual> positivedataset = new Dataset<Individual>();
		positivedataset.setNumberOfFeatures(numberOfFeatures);
		positivedataset.setLegend(sss);
		Dataset<Individual> negativedataset = new Dataset<Individual>();
		negativedataset.setNumberOfFeatures(numberOfFeatures);
		negativedataset.setLegend(sss);
		request.setPolarizationAlgorithm(algorithm);
		request.setWordSpaceId(super.wordspace);
		request.setPoleIds(poleIds);

		String url = "remote://"+host+":4447";
		Properties props = new Properties();
		//			props.put(CORE_MONITORING_API_PROPERTY,STAGE_MONITORING_API);
		props.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
		props.put(Context.PROVIDER_URL, url);
		props.put(Context.SECURITY_PRINCIPAL, "monitor");
		props.put(Context.SECURITY_CREDENTIALS, "monitor");
		props.put("jboss.naming.client.ejb.context", true); 			//vad är denna till för?
		props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming"); // BEHÖVS EJ?			
		logger.debug(props);
		Context ctx = new InitialContext(props);
		MonitoringApiRemote service = null;
		try {
			 service = (MonitoringApiRemote) ctx
					.lookup(ear+"/monitoring-rmi-api-impl/MonitoringApiImpl!com.gavagai.rabbit.api.monitoring.MonitoringApiRemote");
			logger.info("Hooked up to "+service);
		} catch (NamingException e) {
			logger.error(e.getMessage(), e);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		
		logger.info("Performing " + testIdentifier + " sentiment polarization for word space " + wordspace);
		int antal = 0,noanswer = 0;
		int poscount = 0,negcount = 0;
		for (TextWithSentiment f : bucket) {
			request.setText(f.getText());
			response = null;
			try {
				response = service.polarizeFragment(request);
				System.gc();
				antal++;
			} catch (Exception e) {
				logger.debug("Sentiment Polarization disrupted by server hangup! "+e.getMessage());
				service = (MonitoringApiRemote) ctx
						.lookup(ear+"/monitoring-rmi-api-impl/MonitoringApiImpl!com.gavagai.rabbit.api.monitoring.MonitoringApiRemote");
				logger.info("Hooked up to "+service);

				noanswer++;
			}
			if (response != null) {
				Map<Pole, Float> sp = response.getStatistics().getScores();
				double ff[] = new double[poleIds.size()];
				for (Pole ps : sp.keySet()) {
					Float score = sp.get(ps);
					if (ps.getId() == positivepole) {	
						ff[0] = score;
					}
					if (ps.getId() == negativepole) {
						ff[1] = score;
					}
					if (ps.getId() == lovepole) {
						ff[2] = score;
					}
					if (ps.getId() == hatepole) {
						ff[3] = score;
					}
					if (ps.getId() == fearpole) {
						ff[4] = score;
					}
					if (ps.getId() == sexypole) {
						ff[5] = score;
					}
					if (ps.getId() == iffypole) {
						ff[6] = score;
					}
					if (ps.getId() == violencepole) {
						ff[7] = score;
					}
					if (ps.getId() == boringpole) {
						ff[8] = score;
					}
				}						
				positivedataset.add(positivedataset.new Individual(""+antal,ff,f.getSentiment().equals(Sentiment.POSITIVE)?true:false));
				negativedataset.add(positivedataset.new Individual(""+antal,ff,f.getSentiment().equals(Sentiment.NEGATIVE)?true:false));
			}
		}
double dd = 0.7;
double portion = 0.3;
double rate = 0.1;
int maxruns = 100;

logger.info("------------ POSITIVE -------------");
Perceptron p = new Perceptron(poleIds.size());
		p.setPortion(portion);
		p.setMaxruns(maxruns);
		p.setLearningRate(rate);
		p.setCost(dd);
		p.train(positivedataset);
logger.info("------------ NEGATIVE -------------");
		//		for (double dd = 0.5d; dd < 0.71; dd += 0.1) {
			Perceptron n = new Perceptron(poleIds.size());
			n.setPortion(portion);
			n.setMaxruns(maxruns);
			n.setLearningRate(rate);
			n.setCost(dd);
			n.train(negativedataset);
//		}
	}
	
	

	private boolean simplemaximumscore = true;
	private boolean weightedneutralcorridorscore = false;
	public void setSimplemaximumscore(boolean simplemaximumscore) {
		this.simplemaximumscore = simplemaximumscore;
	}
	public void setWeightedneutralcorridorscore(boolean weightedneutralcorridorscore) {
		this.weightedneutralcorridorscore = weightedneutralcorridorscore;
	}



	public static void main(String[] args) {
		System.setProperty("com.gavagai.rabbit.utils.StatsdLogger.enablelogger","true");
		StatsdLogger statsdLogger = new StatsdLogger(System.getProperty("com.gavagai.rabbit.utils.StatsdLogger.serverName","localhost"),Integer.parseInt(System.getProperty("com.gavagai.rabbit.utils.StatsdLogger.serverPort","33444")),true);
		String statsdLogger_tag;

		SentimentCategorisationLearner sbt = new SentimentCategorisationLearner();
		sbt.setUsername("monitor");
		sbt.setPassword("monitor");
		sbt.setHost(BenchmarkBase.COREHOST);
		sbt.setEar(BenchmarkBase.COREEAR);
		String testDirectory = "/bigdata/evaluation/sentiment.polarization/";
		String language = "en";
		sbt.setLanguage(language);
		sbt.setWordSpaceId(1);
		sbt.setPositivePole(41);
		sbt.setNegativePole(42);

		int bucketsize = 10000;
		sbt.setBucketSize(bucketsize);

		PolarizationAlgorithm p1 = PolarizationAlgorithm.EXACTSKIPAMPLIFY;
		PolarizationAlgorithm p2 = PolarizationAlgorithm.DISTANCE;
		boolean m1 = true, m2 = false, w1 = false, w2 = true;

		float o1 = 0,pl1 = 0,r1 = 0,o2 = 0,pl2 = 0,r2 = 0;


		String testIdentifier = "replab";
		try {
			sbt.setup(p1,m1,w1);
			sbt.setTestIdentifier(testIdentifier);
			sbt.setTestdirectory(testDirectory+language+"/"+testIdentifier+"/");
			sbt.setTestFilename("replab2013-lessjunk.txt");
//			sbt.setTestFilename("rvw-en-finegrained.txt");
			sbt.initialiseDataSet();
			o1 = sbt.getResult();
		} catch (Exception e) {
			e.printStackTrace();
		}
}}

