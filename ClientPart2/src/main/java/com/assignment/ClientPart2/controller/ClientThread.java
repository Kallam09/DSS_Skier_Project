package com.assignment.ClientPart2.controller;



import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.assignment.ClientPart2.util.WriteCSV;
import com.assignment.ClientPart2.model.LiftRide;
import com.assignment.ClientPart2.model.LiftRideEvent;

import com.google.gson.Gson;

public class ClientThread extends Thread{
	private int TOTAL_THREADS;
	private int REQUEST_PER_THREAD;
	private final int MAX_RETRIES = 5;
	private static final String serverURL = "http://155.248.227.33:8080/skier_application/";
	private WriteCSV writeCSV;
	private BlockingQueue<LiftRideEvent> queue = new LinkedBlockingQueue<>();
	// Create a CSV file
	String csvFilePath = "C:\\Users\\smile\\Downloads\\Skier-Project-Updated\\Skier-Project\\ClientPart2\\src\\main\\resources\\Profiling_Performance.csv";
    public ClientThread(int num_of_thread, int num_of_requests_each_thread) {
		this.TOTAL_THREADS = num_of_thread;
		this.REQUEST_PER_THREAD = num_of_requests_each_thread;
    }
    
	@Override
	public void run() {
		HttpClient client = HttpClient.newHttpClient();
		if(testClient(client)) {
			System.out.println("Connection established\n");
		}else {
			System.out.println("Connection not formed\n");
			return;		
		}
		
		CountDownLatch successCount = new CountDownLatch(TOTAL_THREADS * REQUEST_PER_THREAD);
		CountDownLatch failureCount = new CountDownLatch(TOTAL_THREADS * REQUEST_PER_THREAD);

        List<Long> eachRequestTimes = Collections.synchronizedList(new ArrayList<Long>());

        ExecutorService executorService = Executors.newFixedThreadPool(TOTAL_THREADS);
	    long startTime = System.currentTimeMillis();
	    try{
	    	writeCSV = new WriteCSV(csvFilePath);
	    	String[] headers = {"start time", "request type", "latency(ms)", "response code"};
	    	writeCSV.write(headers);
	        // Start the lift ride event generator thread
	        EventThread generatorThread = new EventThread(queue, TOTAL_THREADS, REQUEST_PER_THREAD);
	        generatorThread.start();
			for(int i = 0; i < TOTAL_THREADS; i++) {
				executorService.submit(()->{
					for(int j = 0; j < REQUEST_PER_THREAD; j++) {
						try {
							long requestStartTime = System.currentTimeMillis();
							String requestStartTimeString = Long.toString(requestStartTime);
							
							LiftRideEvent liftRideEvent = queue.take();
							String url = serverURL+"skiers/" +
									Integer.toString(liftRideEvent.getResortID()) +
									"/seasons/" + liftRideEvent.getSeasonID() +
									"/days/" + liftRideEvent.getDayID() +
									"/skiers/" + Integer.toString(liftRideEvent.getSkierID());
							String requestBody = new Gson().toJson(liftRideEvent.getLiftRide());
							
							
							HttpRequest request = HttpRequest.newBuilder()
									  .uri(URI.create(url))
									  .POST(HttpRequest.BodyPublishers.ofString(requestBody))
									  .build();
							
							HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
							long latency = System.currentTimeMillis() - requestStartTime;
							eachRequestTimes.add(latency);
							String latencyString = Long.toString(latency);
							
							int retries = 0;
							while (response.statusCode() >= 400 && retries < MAX_RETRIES) {
								retries++;
								System.out.println("Request failed with status code " + response.statusCode() + ", retrying (attempt " + retries + ")");
								response = client.send(request, HttpResponse.BodyHandlers.ofString());
							}
							
							String responseCode = Integer.toString(response.statusCode());
							String[] csvLine = {requestStartTimeString, "POST", latencyString, responseCode};
							writeCSV.write(csvLine);
							if (response.statusCode() >= 400) {
								System.out.println("Request failed after " + MAX_RETRIES + " retries with status code " + response.statusCode());
								successCount.countDown();
							} else {
								failureCount.countDown();
							}
						} catch (IOException | InterruptedException e) {
							successCount.countDown();
							System.out.println("Exception in thread: " + e.getMessage());
						}
					}
				});
			}
			
			generatorThread.join();
			executorService.shutdown();
			executorService.awaitTermination(10, TimeUnit.MINUTES);
			writeCSV.close();
		} catch (InterruptedException | IOException e1) {
			e1.printStackTrace();
		} 
	    
	    int totalResponseTime = 0;
	    for(long time: eachRequestTimes) {
	    	totalResponseTime += time;
	    }
	    
	    double meanResponseTime = (double) totalResponseTime / eachRequestTimes.size();
	    double medianResponseTime = calculateMedian(eachRequestTimes);
		
	    long endTime = System.currentTimeMillis();
		long wallTime = endTime - startTime;
	    double throughput = (double) TOTAL_THREADS * REQUEST_PER_THREAD / (wallTime / 1000.0);
	    
	    long p99ResponseTime = calculateP99(eachRequestTimes);
	    
	    long minResponseTime = Collections.min(eachRequestTimes);
	    long maxResponseTime = Collections.max(eachRequestTimes);

		System.out.println("1.Max response time: " + maxResponseTime + "ms");
		System.out.println("2.Min response time: " + minResponseTime + "ms");
	    System.out.println("3.Mean response time: " + meanResponseTime + "ms");
	    System.out.println("4.Median response time: " + medianResponseTime + "ms");
		System.out.println("5.Percentile99 response time: " + p99ResponseTime + "ms");
	    System.out.println("6.The total throughput in requests per second: " + throughput + " requests/second");
	}

	public static long calculateP99(List<Long> values) {
		Collections.sort(values);
		int index = (int) Math.ceil(values.size() * 0.99);
		return values.get(index - 1);
	}

	private static double calculateMedian(List<Long> eachRequestTimes) {
		Collections.sort(eachRequestTimes);
		int size = eachRequestTimes.size();
		int middle = size / 2;
		if (size % 2 == 0) {
			return (eachRequestTimes.get(middle - 1) + eachRequestTimes.get(middle)) / 2.0;
		} else {
			return eachRequestTimes.get(middle);
		}
	}

    private boolean testClient(HttpClient client) {
		String serviceUrl =  serverURL + "skiers/1/seasons/2022/days/111/skiers/1";
		LiftRide liftRide = new LiftRide((short)111,(short)11);
		String requestBody = new Gson().toJson(liftRide);
		HttpRequest request = HttpRequest.newBuilder()
				  .uri(URI.create(serviceUrl))
				  .POST(HttpRequest.BodyPublishers.ofString(requestBody))
				  .build();
		
		HttpResponse<String> response;
		try {
			response = client.send(request, HttpResponse.BodyHandlers.ofString());
			if(response.statusCode() == 201) {
				return true;
			}
		} catch (IOException | InterruptedException e) {
			return false;
		}
		return false;
	}

}
