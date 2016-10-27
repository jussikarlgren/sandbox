package com.gavagai.jussisandbox;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Properties;
import java.util.Scanner;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gavagai.rabbit.api.monitoring.MonitoringApiRemote;
import com.gavagai.rabbit.api.monitoring.domain.WordSpace;

public class TestTheHangup {

	private final Log logger = LogFactory.getLog(TestTheHangup.class);

	private int getFragments(File testFile) throws FileNotFoundException {
		Scanner fileScanner = new Scanner(new BufferedInputStream(new FileInputStream(testFile)));
		logger.info("File scanner created");
		int i = 0;
		while (fileScanner.hasNextLine()) {
			String fileLine = fileScanner.nextLine();
			i++;
		}	
		fileScanner.close();
		logger.info("Number of cases in file: "+i);
		return i;
	}

	protected MonitoringApiRemote getMonitoringApi() {
		MonitoringApiRemote monitoringApi = null;
		try {
			Properties props = new Properties();
			props.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
			props.put(Context.PROVIDER_URL, "remote://core1.gavagai.se:4447");
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



	public void performTest() throws Exception {
	
		//getFragments(new File("/bigdata/evaluation/sentiment/sentiwordnet/SentiWordNet_3.0.0_20130122.txt"));
		
		MonitoringApiRemote service = getMonitoringApi();
		
		System.gc();
	getFragments(new File("/bigdata/evaluation/sentiment/sentiwordnet/SentiWordNet_3.0.0_20130122.txt"));
	System.gc();
		
		logger.info(service);
		for (WordSpace wordspace : service.findAllWordSpaces()) {
			try {
				Long hj = service.countAllComObjects(wordspace.getId());
				System.out.println("wordspace name=" + wordspace.getName() + " id=" + wordspace.getId() + " count="+hj);
			} catch (Exception e) {
				System.out.println("wordspace number "+wordspace.getId()+" on the fritz.");
			}
		}
	}


	public static void main(String[] args) {
		TestTheHangup spbt = new TestTheHangup();
		try {
			spbt.performTest();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}