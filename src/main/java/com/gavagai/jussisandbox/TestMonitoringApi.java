package com.gavagai.jussisandbox;

import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

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
import com.gavagai.rabbit.api.monitoring.domain.WordSpace;

public class TestMonitoringApi {

	private final Log logger = LogFactory.getLog(TestMonitoringApi.class);

	protected MonitoringApiRemote getMonitoringApi() {
		MonitoringApiRemote monitoringApi = null;
		try {
			Properties props = new Properties();
			props.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
			props.put(Context.PROVIDER_URL, "remote://core5.gavagai.se:4447");
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
		MonitoringApiRemote service = getMonitoringApi();
		logger.info(service);
		for (WordSpace wordspace : service.findAllWordSpaces()) {
			try {
				Long hj = service.countAllComObjects(wordspace.getId());
				System.out.println("wordspace name=" + wordspace.getName() + " id=" + wordspace.getId() + " count="+hj);
			} catch (Exception e) {
				System.out.println("wordspace number "+wordspace.getId()+" on the fritz.");
			}
		}
		try {
			String s = runJustTheOne(service,"i really like to eat wonderful food and to play tough but then i maybe got sued");
			System.out.println(s);
		} catch (Exception e) {
			System.out.println("no polarisation for you ");
			e.printStackTrace();
		}
	}

	public String runJustTheOne(MonitoringApiRemote service, String txt) {
		FragmentPolarizationRequest request = new FragmentPolarizationRequest();
		FragmentPolarizationResponse response = null;
		request.setPolarizationAlgorithm(PolarizationAlgorithm.EXACTSKIPAMPLIFY);
		request.setWordSpaceId(1);
		ArrayList<Long> poleIds = new ArrayList<Long>();
		poleIds.add(41l);
		poleIds.add(42l);
		poleIds.add(704l);
		poleIds.add(705l);
		poleIds.add(703l);
		poleIds.add(29l);
		poleIds.add(702l);
		poleIds.add(98l);
		poleIds.add(637l);
		request.setPoleIds(poleIds);
		request.setText(txt);
		logger.info("Here's the text: "+txt);
		response = null;
		response = service.polarizeFragment(request);
		System.gc();
		String s = "no response";
		if (response != null) {
			s = "";
			Map<Pole, Float> sp = response.getStatistics().getScores();
			for (Pole ps : sp.keySet()) {
				Float score = sp.get(ps);
				s += ps.getName()+" "+score;
			}
		}
		return s;
	}

	public static void main(String[] args) {
		TestMonitoringApi spbt = new TestMonitoringApi();
		try {
			spbt.performTest();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}