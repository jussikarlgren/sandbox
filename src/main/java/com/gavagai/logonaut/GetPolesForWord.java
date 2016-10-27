package com.gavagai.logonaut;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gavagai.rabbit.api.monitoring.MonitoringApiRemote;
import com.gavagai.rabbit.api.monitoring.domain.FragmentPolarizationRequest;
import com.gavagai.rabbit.api.monitoring.domain.FragmentPolarizationResponse;
import com.gavagai.rabbit.api.monitoring.domain.PolarizationAlgorithm;
import com.gavagai.rabbit.api.monitoring.domain.Pole;

public class GetPolesForWord {
	private String host;
	private String ear;
	private MonitoringApiRemote service;
	private Properties environment;

	public GetPolesForWord(Properties p) {
		//		String uid = rabbitconfig.getProperty("userid","monitor");
		//		String pw = rabbitconfig.getProperty("password","monitor");
		this.environment = p;
		this.host = p.getProperty("host","stage-core1.gavagai.se");
		this.ear = p.getProperty("ear","stage-core1");
		this.service = getMonitoringApi();
	}

	private Log logger = LogFactory.getLog(GetPolesForWord.class);

	protected MonitoringApiRemote getMonitoringApi() {
		MonitoringApiRemote monitoringApi = null;
		try {
			// https://docs.jboss.org/author/display/AS71/EJB+invocations+from+a+remote+client+using+JNDI
			String url = "remote://"+host+":4447";
			environment.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
			environment.put(Context.PROVIDER_URL, url);
			environment.put(Context.SECURITY_PRINCIPAL, "monitor");
			environment.put(Context.SECURITY_CREDENTIALS, "monitor");
			environment.put("jboss.naming.client.ejb.context", true); 			
			environment.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming"); 			
			logger.debug(environment);
			Context ctx = new InitialContext(environment);
			monitoringApi = (MonitoringApiRemote) ctx
			.lookup(ear+"/monitoring-rmi-api-impl/MonitoringApiImpl!com.gavagai.rabbit.api.monitoring.MonitoringApiRemote");

			logger.info("Hooked up to "+url+" with API: "+monitoringApi);
		} catch (NamingException e) {
			logger.error(e.getMessage(), e);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return monitoringApi;
	}

	public String polarise(String line) throws Exception {
		String r = "";
		logger.info(" About to run word polarization ... "+line);
		PolarizationAlgorithm algorithm = PolarizationAlgorithm.LEGACY;
		FragmentPolarizationRequest request = new FragmentPolarizationRequest();
		request.setPolarizationAlgorithm(algorithm);
		request.setWordSpaceId(3); // 3 sv, 1 en.	
		List<Long> poleIds = new ArrayList<Long>();
		poleIds.add(45l); // sv neg
		poleIds.add(44l); // sv pos
		poleIds.add(43l); // sv v�ld
		//poleIds.add(41l); // en neg
		//poleIds.add(42l); // en pos
		request.setPoleIds(poleIds);
		request.setText(line);
		request.setContext("null", "null");
		try {
		FragmentPolarizationResponse response = service.polarizeFragment(request);
		Map<Pole, Float> sp = response.getStatistics().getScores();
		for (Pole ps : sp.keySet()) {
			Float score = sp.get(ps);
			r = r + (ps.getName()+":"+score+"\t");
			if (ps.getName().toLowerCase().contains("pos")) {heja = score;}
			if (ps.getName().toLowerCase().contains("neg")) {usch = score;}
			if (ps.getName().toLowerCase().contains("vio")) {bang = score;}
		}
		} catch (Exception e) {
			service = getMonitoringApi();
		}
		return r;
	}
	float heja;
	float usch;
	float bang;
	Hashtable <String,Hashtable <String,Float>> lander = new Hashtable<String,Hashtable <String,Float>>();
	Hashtable <String,Hashtable <String,Float>> partier = new Hashtable<String,Hashtable <String,Float>>();
	Hashtable<String,Integer> pn = new Hashtable<String,Integer>();
	Hashtable<String,Integer> ln = new Hashtable<String,Integer>();

	private void update(String parti, String land) {
		if (lander.containsKey(land)) {
			ln.put(land,(ln.get(land)+1));
			Hashtable<String,Float> vals = lander.get(land);
			vals.put("heja",vals.get("heja")+heja);
			vals.put("bang",vals.get("bang")+bang);
			vals.put("usch",vals.get("usch")+usch);
		} else {
			ln.put(land,1);
			Hashtable<String,Float> vals = new Hashtable<String,Float>();
			vals.put("heja",heja);
			vals.put("bang",bang);
			vals.put("usch",usch);
			lander.put(land, vals);
		}
		if (partier.containsKey(parti)) {
			pn.put(parti,(pn.get(parti)+1));
			Hashtable<String,Float> vals = partier.get(parti);
			vals.put("heja",vals.get("heja")+heja);
			vals.put("bang",vals.get("bang")+bang);
			vals.put("usch",vals.get("usch")+usch);
		} else {
			pn.put(parti,1);
			Hashtable<String,Float> vals = new Hashtable<String,Float>();
			vals.put("heja",heja);
			vals.put("bang",bang);
			vals.put("usch",usch);
			partier.put(parti, vals);
		}
	}
	private void output(FileWriter outfile) throws IOException {
		for (String land: lander.keySet()) {
			Hashtable<String,Float> vals = lander.get(land);
			outfile.write(land+"\t"+ln.get(land)+"\t");
			for (String v: vals.keySet()) {
				outfile.write(v+" "+vals.get(v)+"\t");
			}
			outfile.write("\n");
		}
		for (String parti: partier.keySet()) {
			Hashtable<String,Float> vals = partier.get(parti);
			outfile.write(parti+"\t"+pn.get(parti)+"\t");
			for (String v: vals.keySet()) {
				outfile.write(v+" "+vals.get(v)+"\t");
			}
			outfile.write("\n");
		}
	}

	public void lander(Properties environment) {
		logger.info(" About to find words to examine ...");
		try {
			String targetDirectory = environment.getProperty("targetdirectory","/Users/jussi/Desktop");
			String sourceDirectory = environment.getProperty("targetdirectory",targetDirectory);
			String tag = environment.getProperty("tag","lander-parti-associationer-kuusi");
			String logfilename = targetDirectory+"/"+environment.getProperty("logfilename","gavagai-"+tag+".log");
			String outfilename = targetDirectory+"/"+environment.getProperty("outfilename","gavagai-"+tag+".list");
			String sourcefilename = environment.getProperty("sourcefilename","/Users/jussi/Desktop/associationer-del4.tsv");
			//sourceDirectory+"/"+
			//		environment.getProperty("sourcefilename","/bigdata/research/replab/replab-data/replab2012-labeled-with-language.txt");
			String separator = environment.getProperty("fieldseparator","\t");
			Scanner fileScanner;
			fileScanner = new Scanner(new BufferedInputStream(new FileInputStream(sourcefilename)));
			logger.info("File scanner created");
			FileWriter logfile = new FileWriter(logfilename);
			FileWriter outfile = new FileWriter(outfilename);

			int i = 0;
			while (fileScanner.hasNextLine()) {
				i++;
				String fileLine = fileScanner.nextLine();
				String[] bits = new String[7];
				bits = fileLine.split("\t");
				String parti = bits[1];
				String land = bits[2];
				String text = bits[3];
				try {
					logfile.write(parti+"\t"+land+"\t"+text+"\n");		
					logfile.flush();											
					String polarisation = "";
					try {
						polarisation = polarise(text);
						update(parti,land);
						outfile.write(parti+"\t"+land+"\t"+text+"\t"+polarisation+"\n");	

					} catch (Exception e) {
						logfile.write("service failed "+fileLine);
						service = getMonitoringApi();
						polarisation = polarise(text);
						update(parti,land);
						outfile.write(parti+"\t"+land+"\t"+text+"\t"+polarisation+"\n");	
					}
					outfile.flush();
				} catch (ArrayIndexOutOfBoundsException e) {
					logfile.write("Error in line "+fileLine);
				}
			}
			output(logfile);
			logfile.close();  
			outfile.close();  
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



	public static void main(String[] args) {
		String configfilename;
		Properties rabbitconfig = new Properties();
		rabbitconfig.setProperty("wordspace", "3"); // 1: english
		rabbitconfig.setProperty("userid","monitor");
		rabbitconfig.setProperty("password","monitor");
		rabbitconfig.setProperty("host","stage-core3.gavagai.se");
		rabbitconfig.setProperty("ear","stage-core3");

		//		if (args.length < 1) {
		//			configfilename = "logonaut.config";
		//		} else {
		//			configfilename = args[0];
		//		}
		//		try {	
		//			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		//			rabbitconfig.load(classLoader.getResourceAsStream(configfilename));
		//		} catch (FileNotFoundException e1) {
		//			System.err.println(e1+": "+configfilename);
		//		} catch (IOException e1) {
		//			System.err.println(e1+": "+configfilename);
		//		} catch (NullPointerException e1) {
		//			System.err.println(e1+": "+configfilename);
		//		}		

		GetPolesForWord spbt = new GetPolesForWord(rabbitconfig);
//		try {
//			spbt.lander(rabbitconfig);

					try {
						String s = spbt.polarise("nu k�r jag en liten provgrej med j�vla d�ligt och superfint j�ttebra.");//this is a test string which i love and hate");
						System.out.println(s);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
