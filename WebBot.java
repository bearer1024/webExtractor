package uk.ac.le.cs.CO3090.cw1;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WebBot implements Runnable, StatisticalAnalysisInterface {
	
	public static int MAX_PAGES_NUM = 50;
	public static int TIME_OUT = 10000;
	public static int MAX_QUEUE_SIZE = 20000;
	public static int MAX_THREAD_NUM = 10;
	public static int MAX_CHAR_COUNT = 1000000;
	public static String ALPHABET = "abcdefghijklmnopqrstuvwxyz";

	String URL;
	
	HashSet<String> visitedURLs = new HashSet<String>();
	HashMap<String, LetterFrequency> letterFrequencyForEveryPage = new HashMap<String, LetterFrequency>();
	LetterFrequency letterFrequencyForAllPage = new LetterFrequency();

	public WebBot(String URL) {
		this.URL = URL;
	}
	
	@Override
	public void run() {
		try {
			count(this.URL);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void count(String URL) throws InterruptedException {
		long startTime = System.currentTimeMillis();
		
		ArrayDeque<String> urlQueue = new ArrayDeque<String>();
		ResourceQueue<WebBotWorker.WorkResult> workResultQueue = new ResourceQueue<WebBotWorker.WorkResult>();
		
	    ExecutorService executor = Executors.newFixedThreadPool(MAX_THREAD_NUM);

	    // Set base URL
		urlQueue.add(URL);

		while (!urlQueue.isEmpty()) {
			// Dispatch URL to worker
			WebBotWorker worker = new WebBotWorker(urlQueue.pop(), workResultQueue);
			executor.execute(worker);
			
			// Wait for worker to finish the work
			WebBotWorker.WorkResult workResult = workResultQueue.take();
			visitedURLs.add(workResult.URL);
			letterFrequencyForEveryPage.put(workResult.URL, workResult.letterFrequency);
			letterFrequencyForAllPage.merge(workResult.letterFrequency);
			
			// Check exit conditions
			if (visitedURLs.size() >= MAX_PAGES_NUM ||
				letterFrequencyForAllPage.getTotal() >= MAX_CHAR_COUNT ||
				(System.currentTimeMillis() - startTime) >= TIME_OUT)
			{
				break;
			}
			
			// Push new links to url queue
			for (String newUrl : workResult.queue) {
				if (!visitedURLs.contains(newUrl) && urlQueue.size() < MAX_QUEUE_SIZE) {
					urlQueue.add(newUrl);
				}
			}
		}
		
		// Thread pool cleanup
		executor.shutdown();
		try {
			long waitTime = Math.max(0, TIME_OUT - (System.currentTimeMillis() - startTime));
			executor.awaitTermination(waitTime, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	
	@Override
	public void showTotalStatistics() {
		System.out.println("Total number of characters:" + letterFrequencyForAllPage.getTotal());
		System.out.println("Pages visited:" + visitedURLs.size());
		System.out.println();

		for (int i = 0; i < ALPHABET.length(); i++) {
			char ch = ALPHABET.charAt(i);
			Double frequency = letterFrequencyForAllPage.getCharacterFrequency(ch);
			String percentage = String.format("%.3f", frequency * 100.0);
			System.out.println(ch + " = " + percentage + "%");
		}
	}
	
	public static void main(String[] args){
		String websiteURL = "http://www.bbc.co.uk/news";

		WebBot bot = new WebBot(websiteURL);
		try {
			bot.count(websiteURL);
			bot.showTotalStatistics();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class WebBotWorker implements Runnable {

	String URL;
	ResourceQueue<WorkResult> workResultQueue;
	
	class WorkResult {
		public String URL;
		public ArrayDeque<String> queue = new ArrayDeque<String>();
		public LetterFrequency letterFrequency =  new LetterFrequency();
	}
	
	public WebBotWorker(String URL, ResourceQueue<WorkResult> workResultQueue) {
		this.URL = URL;
		this.workResultQueue = workResultQueue;
	}
	
	@Override
	public void run() {	
		// Download the content of URL
		String content = WebExtractor.getTextFromAddress(URL);

		// Strips HTML tags, get plain text
		String text = WebExtractor.getPlainText(content);

		WorkResult workResult = new WorkResult();
		workResult.URL = URL;
		
		// Calculate letter frequency
		workResult.letterFrequency.calucate(text);
		
		// Put links into result queue
		ArrayList<String> urls = WebExtractor.extractHyperlinks(URL, content);
		for (String url : urls) {
			workResult.queue.add(url);
		}
		
		// Put work result into queue
		workResultQueue.put(workResult);
	}

}

class LetterFrequency {
	// NOTE: _int_ may not be enough for the count in general, BUT we have MAX_CHAR_NUM limitation.
	int count[] = new int[26];
	int total = 0;
	
	public void calucate(String text) {
		for (int i = 0; i < text.length(); ++i){
			encouterCharacter(text.charAt(i));
		}
	}
	
	public void merge(LetterFrequency other) {
		for (int i = 0; i < 26; ++i) {
			this.count[i] += other.count[i];
		}
		this.total += other.total;
	}
	
	public int getTotal() {
		return total;
	}
	
	public void encouterCharacter(char ch) {
		ch = Character.toLowerCase(ch);
		if (ch >= 'a' && ch <= 'z') {
			int i = ch - 'a';
			count[i] += 1;
			total += 1;
		}
	}
	
	public int getCharacterCount(char ch) {
		ch = Character.toLowerCase(ch);
		if (ch >= 'a' && ch <= 'z') {
			int i = ch - 'a';
			return count[i];
		}
		return 0;
	}
	
	public double getCharacterFrequency(char ch) {
		return getCharacterCount(ch) * 1.0 / total;
	}
}

class ResourceQueue<T> {
	ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<T>();
	
	public synchronized void put(T resource) {
		queue.add(resource);
		notifyAll();
	}
	
	public synchronized T take() {
		while (queue.isEmpty()) {
			try {
				wait();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	
		T resource = queue.poll();
		this.notifyAll();
		return resource;
	}
}