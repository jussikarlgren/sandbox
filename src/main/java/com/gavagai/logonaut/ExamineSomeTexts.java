package com.gavagai.logonaut;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Vector;

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
import com.gavagai.vectorgarden.GetVectorsForWord;

public class ExamineSomeTexts {
	private class TextWithId {
		private String text;
		public TextWithId(String id, String txt) {
			this.id = id;
			this.text = txt;
		}
		public String getText() {
			return text;
		}
		public void setText(String text) {
			this.text = text;
		}
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		private String id;
	}
	private Log logger = LogFactory
			.getLog(ExamineSomeTexts.class);
	private Properties environment;

	public ExamineSomeTexts(Properties env) {
		this.environment = env;
	}
	

	public void performTextExamination() {
		logger.info(" About to find texts to examine ...");
		try {
			String targetDirectory = environment.getProperty("targetdirectory","/home/jussi/Desktop");
			String sourceDirectory = environment.getProperty("targetdirectory",targetDirectory);
			String tag = environment.getProperty("tag","");
			String logfilename = targetDirectory+"/"+environment.getProperty("logfilename","gavagai-textexamination"+tag+".log");
			String outfilename = targetDirectory+"/"+environment.getProperty("outfilename","gavagai-textexamination"+tag+".list");
			String sourcefilename = //sourceDirectory+"/"+
					environment.getProperty("sourcefilename","/bigdata/research/replab/replab-data/replab2012-labeled-with-language.txt");
			int textField = Integer.parseInt(environment.getProperty("textfield","4"));
			int idField = Integer.parseInt(environment.getProperty("idfield","0"));
			int languageField = Integer.parseInt(environment.getProperty("languagefield","1"));

			String separator = environment.getProperty("fieldseparator","\t");
			Scanner fileScanner;
			fileScanner = new Scanner(new BufferedInputStream(new FileInputStream(sourcefilename)));
			logger.info("File scanner created");
			FileWriter logfile = new FileWriter(logfilename);
			FileWriter outfile = new FileWriter(outfilename);
			final Properties esProperties = new Properties();
			esProperties.setProperty("test", "association");
			esProperties.setProperty("wordspace", "1"); // 1: english
			esProperties.setProperty("userid","monitor");
			esProperties.setProperty("password","monitor");
			esProperties.setProperty("host","stage-core3.gavagai.se");
			esProperties.setProperty("ear","stage-core3");
			GetVectorsForWord g = new GetVectorsForWord(esProperties);
			GetPolesForWord p = new GetPolesForWord(esProperties);
			PolarExplorer pe = new PolarExplorer(esProperties);
			
			int i = 0;
			while (fileScanner.hasNextLine()) {
				if (Math.random() > 0.01) {continue;} // take a fraction of all texts
				i++;
				String fileLine = fileScanner.nextLine();

				String[] bits = new String[5];
				bits = fileLine.split("\t");
				String jn;
				if (bits.length >= textField) {
					if (bits[languageField].equalsIgnoreCase("en")) {
						try {
							logfile.write(bits[idField]+"\t"+bits[textField]+"\n");		
							logfile.flush();				
							Vector<String> submit = new Vector<String>();
							for (String s: bits[textField].split(" ")) { submit.add(s.toLowerCase());}
							String response = "";
							try {
//							 response = pe.coherence(g,submit);
							} catch (Exception e) {
								g = new GetVectorsForWord(esProperties);
//							 response = pe.coherence(g,submit);
							}
							
							String polarisation = "";
							try {
								polarisation = p.polarise(bits[textField]);
							} catch (Exception e) {
								p = new GetPolesForWord(esProperties);
								polarisation = p.polarise(bits[textField]);
							}
							
							outfile.write(fileLine+"\n");
							outfile.write(polarisation+"\n");	
//							outfile.write(bits[idField]+separator);		
							outfile.write(response);
							outfile.flush();

						} catch (ArrayIndexOutOfBoundsException e) {
							logfile.write("Error in line "+fileLine);
						}
					}
				}	
			}
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
		Properties polarisationconfig = new Properties();
		if (args.length < 1) {
			configfilename = "textexamination.config";
		} else {
			configfilename = args[0];
		}
		try {	
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			polarisationconfig.load(classLoader.getResourceAsStream(configfilename));
		} catch (FileNotFoundException e1) {
			System.err.println(e1+": "+configfilename);
		} catch (IOException e1) {
			System.err.println(e1+": "+configfilename);
		} catch (NullPointerException e1) {
			System.err.println(e1+": "+configfilename);
		}
		ExamineSomeTexts spbt = new ExamineSomeTexts(polarisationconfig);
		try {
			spbt.performTextExamination();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
