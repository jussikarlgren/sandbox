package com.gavagai.jussisandbox;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;


/**
 * Class to fetch files from the Riksdag API. 
 *
 */
public class RiksdagFileFetcher {

	public class Document {
private String uri;
private String title;
private Date publishedDate;
private String rawText;
public void setUri(String uri) {
	this.uri = uri;
}
public void setTitle(String title) {
	this.title = title;
}
public void setPublishedDate(Date publishedDate) {
	this.publishedDate = publishedDate;
}
public void setRawText(String rawText) {
	this.rawText = rawText;
}
		
	}
	private Log logger = LogFactory.getLog(RiksdagFileFetcher.class);
	
	private int maxNumberOfDocsPerFetch;
	private DateFormat dateformatter;
	private Date efterdatum;
	private Date tilldatum;
	private String datum;
	private String typ = "bet%2cbilaga%2ceunbil%2cds%2cdir%2ckf-lista%2ceunprot%2ceundok%2cfpm%2cfrsrdg%2cf-lista%2crir%2cip%2ckomm%2cminr%c3%a5d%2cmot%2cprop%2cprot%2crfr%2crskr%2cfr%2cskr%2cfrs%2csfs%2ct-lista%2curd%2curf";

	
	public RiksdagFileFetcher(
			int maxNumberOfDocsPerFetch,
			int year,
			int month,
			int day
			) throws ParseException {

		this.maxNumberOfDocsPerFetch = maxNumberOfDocsPerFetch;
		logger.info("Riksdag set up.");
		this.datum = year+"-"+month+"-"+day;
		this.dateformatter = new SimpleDateFormat("yyyy-M-dd");
		this.efterdatum = dateformatter.parse(datum);
		Calendar c = dateformatter.getCalendar();
		this.efterdatum = c.getTime();
		logger.info("Retrieving documents published after date "+efterdatum);
		c.roll(Calendar.DATE, true);
		this.tilldatum = c.getTime();
		logger.info("... and before date "+tilldatum);
	}

		
	private void getTheContentOfThisChunkOfDocuments(int index, int page) {
		Vector<Document> vec = new Vector<Document>();
		String url = "http://data.riksdagen.se/dokumentlista/?rm=&typ="+typ+"&d="+datum+"&sz="+maxNumberOfDocsPerFetch+"&sort=c&utformat=json&p="+page;
		logger.debug(url);
		String c = getHTML(url);
		try {
			JSONObject jin = new JSONObject(c);
			logger.info(jin);
			JSONObject jin2 = (JSONObject) jin.get("dokumentlista");
			int thisRetrievedChunkOfDocuments = ((JSONArray)jin2.get("dokument")).length();
			for (int i = 0; i < thisRetrievedChunkOfDocuments; i++) {
				JSONObject jj = (JSONObject)((JSONArray)jin2.get("dokument")).get(i);
				String title = (String) jj.get("titel");
				logger.info(title);
				String publikationsdatum = (String) jj.get("publicerad");
				if (dateformatter.parse(publikationsdatum).after(tilldatum)) {
					return ;}
				String textUrl = (String) ((JSONObject)jj).get("dokument_url_text");
				String d = getText(textUrl);
				Document document;
				document = createDocument(d,textUrl,title,efterdatum);
				vec.add(document);
				
			}
			if (maxNumberOfDocsPerFetch <= thisRetrievedChunkOfDocuments) {// gör en gång till
				getTheContentOfThisChunkOfDocuments(index+thisRetrievedChunkOfDocuments, page++);
			}
		} catch (ClassCastException e) {
			logger.error("...");
			logger.error(e.getMessage());
		} catch (JSONException e) {
			logger.error("---");
			logger.error(e.getMessage());
		} catch (ParseException e) {
			logger.error("===");
			logger.error(e.getMessage());
		}
		return;
	}

	public Document createDocument(String content, String url, String titleText, Date date) {
		Document document = new Document();
		document.setUri(url);
		document.setTitle(titleText);
		document.setPublishedDate(date);
		document.setRawText(content);
		return document;
	}


	public String getHTML(String url)   {
		String result = "";
		try {
			HttpClient client = new DefaultHttpClient();
			HttpParams httpParameters = client.getParams();
			HttpConnectionParams.setConnectionTimeout(httpParameters, 5000);
			HttpConnectionParams.setSoTimeout(httpParameters, 5000);
			HttpConnectionParams.setTcpNoDelay(httpParameters, true);
			HttpGet request = new HttpGet();
			request.setURI(new URI(url));
			HttpResponse response = client.execute(request);
			InputStream ips = response.getEntity().getContent();
			BufferedReader buf = new BufferedReader(new InputStreamReader(ips,"UTF-8"));
			StringBuilder sb = new StringBuilder();
			String s;
			while (true) {
				s = buf.readLine();
				if (s == null || s.length() == 0)
					break;
				sb.append(s);
			}
			buf.close();
			ips.close();
			result = sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	public String getText(String url) {
		String result = "";
		try {
			HttpClient client = new DefaultHttpClient();
			HttpParams httpParameters = client.getParams();
			HttpConnectionParams.setConnectionTimeout(httpParameters, 5000);
			HttpConnectionParams.setSoTimeout(httpParameters, 5000);
			HttpConnectionParams.setTcpNoDelay(httpParameters, true);
			HttpGet request = new HttpGet();
			request.setURI(new URI(url));
			HttpResponse response = client.execute(request);
			result = EntityUtils.toString(response.getEntity());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	public static void main(String[] args) throws JSONException, ParseException {
		// typ=bet%2cbilaga%2ceunbil%2cds%2cdir%2ckf-lista%2ceunprot%2ceundok%2cfpm%2cfrsrdg%2cf-lista%2crir%2cip%2ckomm%2cminr%c3%a5d%2cmot%2cprop%2cprot%2crfr%2crskr%2cfr%2cskr%2cfrs%2csfs%2ct-lista%2curd%2curf&
		//	Adress: http://data.riksdagen.se/dokumentlista/?sort=c&sz=100&typ=bet%2cbilaga%2ceunbil%2cds%2cdir%2ckf-lista%2ceunprot%2ceundok%2cfpm%2cfrsrdg%2cf-lista%2crir%2cip%2ckomm%2cminr%c3%a5d%2cmot%2cprop%2cprot%2crfr%2crskr%2cfr%2cskr%2cfrs%2csfs%2ct-lista%2curd%2curf&utformat=json
		// String url = "http://data.riksdagen.se/dokumentlista/?rm=&typ=mot&d=2013-10-02&ts=&sn=&parti=&iid=&bet=&org=&kat=&sz=100&sort=c&utformat=json&termlista=";
		RiksdagFileFetcher rc = new RiksdagFileFetcher(100,2013,11,03);
		rc.getTheContentOfThisChunkOfDocuments(0,1);
		
	}
}
