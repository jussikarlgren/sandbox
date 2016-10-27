package com.gavagai.jussisandbox;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.gavagai.rabbit.api.monitoring.MonitoringApiRemote;
import com.gavagai.rabbit.api.monitoring.domain.WordSpace;

public class TestClient {

	public static void main(String[] args) {
		try {
			runTestClient();
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}

	public static void runTestClient() throws NamingException {

		Properties jndiProps = new Properties();
		jndiProps.put(Context.INITIAL_CONTEXT_FACTORY,
				"org.jboss.naming.remote.client.InitialContextFactory");
		jndiProps.put(Context.PROVIDER_URL, "remote://core5.gavagai.se:4447");
		// username
		jndiProps.put(Context.SECURITY_PRINCIPAL, "monitor");
		// password
		jndiProps.put(Context.SECURITY_CREDENTIALS, "monitor");
		jndiProps.put("jboss.naming.client.ejb.context", true);
		// create a context passing these properties
		System.out.println(jndiProps);
		Context ctx = new InitialContext(jndiProps);
System.out.println(ctx.getEnvironment().keySet());
System.out.println(ctx.getEnvironment().values());

		MonitoringApiRemote monitoringApi = (MonitoringApiRemote) ctx
				.lookup("rabbit-core/monitoring-rmi-api-impl/MonitoringApiImpl!com.gavagai.rabbit.api.monitoring.MonitoringApiRemote");

		System.out.println("Hooked up to "+monitoringApi);
		for (WordSpace wordspace : monitoringApi.findAllWordSpaces()) {
			try {
				Long hj = monitoringApi.countAllComObjects(wordspace.getId());
				System.out
				.println("wordspace name=" + wordspace.getName() + " id=" + wordspace.getId() + " count="+hj);
			} catch (Exception e) {
				System.out.println("wordspace number "+wordspace.getId()+" on the fritz.");
			}
		}

	}
}

