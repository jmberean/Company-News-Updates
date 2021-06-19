package com.stockScanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import fun.mike.dmp.Diff;
import fun.mike.dmp.DiffMatchPatch;
import fun.mike.dmp.Operation;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanStocks {
	public static void main(String[] args) {
		try {
			start();
		} catch (Exception e) {
			System.out.println("Main method Exception at: " + LocalTime.now() + " -- Exception: " + e.toString());
		}
	}
	public static void start() throws FileNotFoundException, InterruptedException {
		Toolkit.getDefaultToolkit().beep();
		System.out.printf("%-35s%s\n\n","ScanStocks starting now:", LocalTime.now());
		System.out.printf("%-35s%s\n","companyWebsiteThreadSleep1:", timeToRun.companyWebsiteThreadSleep1/1000 + " seconds");
		System.out.printf("%-35s%s\n","companyWebsiteThreadSleep2:", timeToRun.companyWebsiteThreadSleep2/1000 + " seconds");
		System.out.printf("%-35s%s\n\n","companyWebsiteThreadSleep3:", timeToRun.companyWebsiteThreadSleep3/1000 + " seconds");		
		System.out.printf("%-35s%s\n","googleThreadSleep1: ", timeToRun.googleThreadSleep1/1000 + " seconds");
		System.out.printf("%-35s%s\n","googleThreadSleep2: ", timeToRun.googleThreadSleep2/1000 + " seconds");
		System.out.printf("%-35s%s\n","googleThreadSleep3: ", timeToRun.googleThreadSleep3/1000/60 + " minutes");
		System.out.println("\nCollecting URL's:\n");
		List<String> urls = new ArrayList<String>();
		Scanner scanner = new Scanner(new File("C:\\Users\\Johnn\\Desktop\\PR Scanner\\urls.txt"));
		while (scanner.hasNextLine()) {
			String url = scanner.nextLine().trim();
			urls.add(url);
			System.out.printf("%-35s%s\n","Collected URL:", url);
		}
		System.out.println("\nCollecting companies for Google:\n");
		List<String> companies = new ArrayList<String>();
		Scanner scanner2 = new Scanner(new File("C:\\Users\\Johnn\\Desktop\\PR Scanner\\companies.txt"));
		while (scanner2.hasNextLine()) {
			String company = scanner2.nextLine().trim();
			companies.add(company);
			System.out.printf("%-35s%s\n","Collected Company:", company);
		}
		System.out.printf("\n%-35s%s\n\n","Starting threads:", LocalTime.now());
		java.lang.Thread.sleep(1000L);
		Toolkit.getDefaultToolkit().beep();
		ExecutorService executor = Executors.newFixedThreadPool(urls.size() + companies.size());
		for (int i = 0; i < urls.size(); i++) {
			executor.execute(new ScanStonks(urls.get(i)));
			executor.execute(new ScanGoogleNews(companies.get(i)));
		}
		executor.shutdown();
	}
}
class ScanStonks implements Runnable {
	String site;
	public ScanStonks(String site) {
		this.site = site;
	}
	public void run() {
		scanWebsitesForPR(this.site);
	}
	public static void scanWebsitesForPR(String site) {
		String s1 = null;
		String difference = null;
		while (true) {
			try {
				Document doc = Jsoup.connect(site).get();
				String result = doc.body().text();
				String s2 = result;				
				if (s1 == null) {
					s1 = s2;
				} else {
					if (!s1.equalsIgnoreCase(s2)) {
						List<Diff> diffs = new DiffMatchPatch().diff_main(s1, s2);
						for (Diff diff : diffs) {
							if (diff.operation == Operation.INSERT) {
								difference = diff.text;
							}
						}
						if(!difference.isBlank()) {
							sendDesktopAlert.sendDesktopCompanyAlert(difference,site);
						}
						s1 = s2;
					}
				}
				if (timeToRun.isItTimeToRun()) {
					java.lang.Thread.sleep(timeToRun.companyWebsiteThreadSleep1);
				} else if (LocalTime.now().isBefore(LocalTime.parse("09:35:00.000000000"))) {
					java.lang.Thread.sleep(timeToRun.companyWebsiteThreadSleep2);
				} else if (LocalTime.now().isAfter(LocalTime.parse("09:35:00.000000000")) && LocalTime.now().isBefore(LocalTime.parse("21:00:00.000000000"))) {
					java.lang.Thread.sleep(timeToRun.companyWebsiteThreadSleep3);
				} else if (LocalTime.now().isAfter(LocalTime.parse("09:35:00.000000000")) && !LocalTime.now().isBefore(LocalTime.parse("21:00:00.000000000"))) {
					break;
				}
			} catch (Exception e) {
				System.out.println("Exception at: "+LocalTime.now()+" for thread of site: " + site + " -- Exception: " +  e.toString() + "\n");		// SSLHandshakeException:Remote host terminated the handshake , SSLException:readHandshakeRecord
			}
		}
	}
}
class ScanGoogleNews implements Runnable {
	String company;
	public ScanGoogleNews(String company) {
		this.company = company;
	}
	public void run() {
		scanGoogleForPR(this.company);
	}
	public static void scanGoogleForPR(String company) {
		Map<String, String> hm1 = Collections.synchronizedMap(new LinkedHashMap<String, String>());
		Map<String, String> hm2 = Collections.synchronizedMap(new LinkedHashMap<String, String>());
		String searchURL = "https://www.google.com/search?q=" + "\"" + company + "\"" + "&tbs=sbd:1&tbm=nws&num=1";
		Document doc = null;
		Elements results = null;
		String headline = null;
		String url = null;
		while (true) {
			try {
				if (timeToRun.isItTimeToRun()) {
					java.lang.Thread.sleep(timeToRun.googleThreadSleep1);
				} else if (LocalTime.now().isBefore(LocalTime.parse("09:35:00.000000000"))) {
					java.lang.Thread.sleep(timeToRun.googleThreadSleep2);
					continue;
				} else if (LocalTime.now().isAfter(LocalTime.parse("09:35:00.000000000")) && LocalTime.now().isBefore(LocalTime.parse("21:00:00.000000000"))) {
					java.lang.Thread.sleep(timeToRun.googleThreadSleep3);
				} else if (LocalTime.now().isAfter(LocalTime.parse("09:35:00.000000000")) && !LocalTime.now().isBefore(LocalTime.parse("21:00:00.000000000"))) {
					break;
				}
				doc = Jsoup.connect(searchURL).userAgent("Mozilla/5.0").get();
				results = doc.select("a[href]:has(h3)");
				for (Element result : results) {
					String linkHref = result.attr("href");
					String linkText = result.text();
					hm2.put(linkText, linkHref.substring(7, linkHref.indexOf("&")));
				}
				if (hm1.size() == 0) {
					hm1 = Collections.synchronizedMap(new LinkedHashMap<String, String>(hm2));
				} else {
					if (!hm1.keySet().equals(hm2.keySet())) {
						for (String key : hm2.keySet()) {
							headline = key;
							url = hm2.get(key);
							break;
						}
						sendDesktopAlert.sendDesktopGoogleAlert(company, headline, url);
						hm1 = Collections.synchronizedMap(new LinkedHashMap<String, String>(hm2));
					}
				}
				hm2 = Collections.synchronizedMap(new LinkedHashMap<String, String>());
			} catch (HttpStatusException e) {
				Toolkit.getDefaultToolkit().beep();
				System.out.println("HttpStatusException at: " + LocalTime.now() + " for thread of Google site: " + company + " -- Exception: " + e.toString() + "\n");
				break;
			} catch (Exception e) {
				System.out.println("Exception at: " + LocalTime.now() + " for thread of Google site: " + company + " -- Exception: " + e.toString() + "\n");
			}
		}
	}
}
class timeToRun{
    final static long companyWebsiteThreadSleep1 = 2500L; 
    final static long companyWebsiteThreadSleep2 = 15000L; 
    final static long companyWebsiteThreadSleep3 = 60000L; 
    final static long googleThreadSleep1 = 15000L; 
    final static long googleThreadSleep2 = 30000L; 
    final static long googleThreadSleep3 = 1800000L; 
	public static boolean isItTimeToRun() {
		boolean flag = false;
		if(
			(LocalTime.now().isAfter(LocalTime.parse("06:55:00.000000000")) && LocalTime.now().isBefore(LocalTime.parse("07:05:00.000000000"))) ||
			(LocalTime.now().isAfter(LocalTime.parse("07:25:00.000000000")) && LocalTime.now().isBefore(LocalTime.parse("07:35:00.000000000"))) ||
			(LocalTime.now().isAfter(LocalTime.parse("07:55:00.000000000")) && LocalTime.now().isBefore(LocalTime.parse("08:05:00.000000000"))) ||
			(LocalTime.now().isAfter(LocalTime.parse("08:25:00.000000000")) && LocalTime.now().isBefore(LocalTime.parse("08:35:00.000000000"))) ||
			(LocalTime.now().isAfter(LocalTime.parse("08:55:00.000000000")) && LocalTime.now().isBefore(LocalTime.parse("09:05:00.000000000"))) ||
			(LocalTime.now().isAfter(LocalTime.parse("09:25:00.000000000")) && LocalTime.now().isBefore(LocalTime.parse("09:35:00.000000000"))) 
		) {
			flag = true;
		}
		return flag;
	}
}
class sendDesktopAlert {
	public static void sendDesktopCompanyAlert(String difference,String site) throws URISyntaxException, IOException, InterruptedException {
		Toolkit.getDefaultToolkit().beep();
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM-dd-yyyy hh:mm:ss.SSS a");
		LocalDateTime now = LocalDateTime.now();
		String dateTime = dtf.format(now);
		System.out.printf("%-35s%s\n","Something changed for URL:", site);
		System.out.printf("%-35s%s\n","At:", dateTime);
		System.out.printf("%-35s%s\n\n","Difference:", difference);
		sendAlert.sendEmailAndTextAlert("Something changed for URL:" + site + " at: " + dateTime, "Difference: " + difference);
		java.lang.Thread.sleep(5000L); 
		URI theURI = new URI(site);
		java.awt.Desktop.getDesktop().browse(theURI);
	}
	public static void sendDesktopGoogleAlert(String company,String headline, String url) throws URISyntaxException, IOException, InterruptedException {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM-dd-yyyy hh:mm:ss.SSS a");
		LocalDateTime now = LocalDateTime.now();
		String dateTime = dtf.format(now);
		System.out.printf("%-35s%s\n","Google News Update for company:", company);
		System.out.printf("%-35s%s\n","At:", dateTime);
		System.out.printf("%-35s%s\n","Headline:", headline);
		System.out.printf("%-35s%s\n\n","URL:", url);
		sendAlert.sendEmailAndTextAlert("Google News Update for Company: " + company + " at: " + dateTime, "Headline: " + headline + "\nURL: " + url);
		java.lang.Thread.sleep(5000L);
		URI theURI = new URI(url);
		java.awt.Desktop.getDesktop().browse(theURI);
	}
}
class sendAlert {
	public static void sendEmailAndTextAlert(String subject,String msg) {
		String to = "jmberean@gmail.com";
		String to2 = "8457090867@vtext.com";
		String from = "yaaaga123@gmail.com";
		String host = "smtp.gmail.com";
		Properties properties = System.getProperties();
		properties.put("mail.smtp.host", host);
		properties.put("mail.smtp.port", "465");
		properties.put("mail.smtp.ssl.enable", "true");
		properties.put("mail.smtp.auth", "true");
		Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication("yaaaga123@gmail.com", "johnnyAppleSeed123!");
			}
		});
		try {
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(from));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(to2));
			message.setSubject(subject);
			message.setText(msg);
			Transport.send(message);
		} catch (MessagingException e) {
			System.out.println("MessagingException: " + e.toString());
		}
	}
}