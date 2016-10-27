package com.gavagai.logonaut;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gavagai.rabbit.api.monitoring.MonitoringApiRemote;
import com.gavagai.rabbit.api.monitoring.domain.AnalysisRequest;
import com.gavagai.rabbit.api.monitoring.domain.AnalysisResponse;
import com.gavagai.rabbit.api.monitoring.domain.CosResult;

public class GetNeighboursForWord {
	private String host;
	private String ear;
	private int wordspace;
	private MonitoringApiRemote service;
	private boolean mockup = false;

	public GetNeighboursForWord() {
		this(new Properties(),false);
	}
	public GetNeighboursForWord(Properties p) {
		this(p,false);
	}
	public GetNeighboursForWord(Properties p, boolean mockup) {
		setMockup(mockup);
		this.host = p.getProperty("host","stage-core5.gavagai.se");
		this.ear = p.getProperty("ear","stage-core5");
		this.wordspace = 1; //p.getProperty("wordspace",3);
		if (! mockup) 
			this.service = getMonitoringApi();
	}

	private Log logger = LogFactory.getLog(GetNeighboursForWord.class);
/**
	protected MonitoringApiRemote getMonitoringApi() {
		if (! mockup) {
		MonitoringApiRemote monitoringApi = null;
		try {
			// https://docs.jboss.org/author/display/AS71/EJB+invocations+from+a+remote+client+using+JNDI
			String url = "remote://"+host+":4447";
			Properties props = new Properties();
			props.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
			props.put(Context.PROVIDER_URL, url);
			props.put(Context.SECURITY_PRINCIPAL, "monitor");
			props.put(Context.SECURITY_CREDENTIALS, "monitor");
			props.put("jboss.naming.client.ejb.context", true); 			
			props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming"); 			
			logger.debug(props);
			Context ctx = new InitialContext(props);
			monitoringApi = (MonitoringApiRemote) ctx
					.lookup(ear+"/monitoring-rmi-api-impl/MonitoringApiImpl!com.gavagai.rabbit.api.monitoring.MonitoringApiRemote");

			logger.info("Hooked up to "+url+" with API: "+monitoringApi);

		} catch (NamingException e) {
			logger.error(e.getMessage(), e);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return monitoringApi;
		} else {
			return null;
		}
	}
**/
	protected MonitoringApiRemote getMonitoringApi() {
		MonitoringApiRemote monitoringApi = null;
		try {
			Properties props = new Properties();
			props.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
			props.put(Context.PROVIDER_URL, "remote://stage-core5.gavagai.se:4447");
			props.put(Context.SECURITY_PRINCIPAL, "monitor");
			props.put(Context.SECURITY_CREDENTIALS, "monitor");
			props.put("jboss.naming.client.ejb.context", true); 			//vad är denna till för?
			props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming"); // BEHÖVS EJ?			
			logger.info(props);
			Context ctx = new InitialContext(props);
			monitoringApi = (MonitoringApiRemote) ctx
					.lookup("rabbit-core/monitoring-rmi-api-impl/MonitoringApiImpl!com.gavagai.rabbit.api.monitoring.MonitoringApiRemote");
			logger.info("Hooked up to "+monitoringApi);
		} catch (NamingException e) {
			logger.error(e.getMessage(), e);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return monitoringApi;
	}

	public String[] getTopicalNeighbours(String word) throws Exception {
		AnalysisRequest request = new AnalysisRequest();
		request.setFindTopicalNeighbours(true);
		request.setWord(word);
		request.setWordSpaceId(wordspace);
		AnalysisResponse response;
		if (! mockup) {
			try {
				response = service.performAnalysis(request);
			} catch (IllegalStateException e) {
				service = getMonitoringApi(); // try again, once
				System.err.println("reconnect");
				response = service.performAnalysis(request);
			}
			List<CosResult> crs = response.getTopicalNeighbours();
			String[] res = new String[crs.size()];
			int i = 0;
			for (CosResult c: crs) {res[i] = c.getWord(); i++;}
			return res;
		} else {
			String[] res = {"a","b","c","d","e"};
			return res;
		}
	}
	public String[] getParadigmaticNeighbours(String word) throws Exception {
		return getParadigmaticNeighbours(word,0.0d);
	}
	public String[] getParadigmaticNeighbours(String word, double threshold) throws Exception {
		if (! mockup) {
			AnalysisRequest request = new AnalysisRequest();
			request.setFindParadigmaticNeighbours(true);
			request.setWord(word);
			request.setWordSpaceId(wordspace);
			AnalysisResponse response;
			try {
				response = service.performAnalysis(request);
			} catch (IllegalStateException e) {
				service = getMonitoringApi(); // try again, once
				System.err.println("reconnect");
				response = service.performAnalysis(request);
			}
			List<CosResult> crs = response.getParadigmaticNeighbours();
			String[] res = new String[crs.size()];
			int i = 0;
			for (CosResult c: crs) {if (c.getAngle() >= threshold) {res[i] = c.getWord(); i++;}}
			return res;
		} else {
			String[] res = {"a","b","c","d","e"};
			return res;
		}

	}
	public String[] getSyntagmaticNeighbours(String word) throws Exception {
		return getSyntagmaticNeighbours(word,true,true);
	}
	public String[] getSyntagmaticNeighbours(String word,boolean left, boolean right) throws Exception {
		if (!mockup) {
			AnalysisRequest request = new AnalysisRequest();
			request.setFindSyntagmaticNeighbours(true);
			request.setWord(word);
			request.setWordSpaceId(wordspace);
			AnalysisResponse response;
			try {
				response = service.performAnalysis(request);
			} catch (IllegalStateException e) {
				service = getMonitoringApi(); // try again, once
				System.err.println("reconnect");
				response = service.performAnalysis(request);
			}
			int n = 0;
			List<CosResult> crs = null, crss = null;
			if (left) { 
				crs = response.getPreSyntagmaticNeighbours();
				n = crs.size();
			} 
			if (right) {
				crss = response.getPostSyntagmaticNeighbours();
				n += crss.size();
			}
			String[] res = new String[n];
			int i = 0;
			if (left) {	for (CosResult c: crs) {res[i] = c.getWord(); i++;} }
			if (right) {	for (CosResult c: crss) {res[i] = c.getWord(); i++;} }
			return res;
		} else {
			String[] res = {"a","b","c","d","e"};
			return res;
		}

	}


	public static void main(String[] args) {
		String configfilename;
		Properties rabbitconfig = new Properties();
		if (args.length < 1) {
			configfilename = "logonaut.config";
		} else {
			configfilename = args[0];
		}
		try {	
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			rabbitconfig.load(classLoader.getResourceAsStream(configfilename));
		} catch (FileNotFoundException e1) {
			System.err.println(e1+": "+configfilename);
		} catch (IOException e1) {
			System.err.println(e1+": "+configfilename);
		} catch (NullPointerException e1) {
			System.err.println(e1+": "+configfilename);
		}
		GetNeighboursForWord spbt = new GetNeighboursForWord(rabbitconfig);
		try {
			String token = "warm";
			String[] f =		spbt.getParadigmaticNeighbours(token);
			for (String fs: f) {System.out.println(fs);}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public void setMockup(boolean mockup) {
		this.mockup = mockup;
	}
}
