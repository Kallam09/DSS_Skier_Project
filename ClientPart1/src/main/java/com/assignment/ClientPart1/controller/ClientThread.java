package com.assignment.ClientPart1.controller;



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

import com.assignment.ClientPart1.model.LiftRide;
import com.assignment.ClientPart1.model.LiftRideEvent;

import com.google.gson.Gson;

public class ClientThread extends Thread{
	private int TOTAL_THREADS;
	private int REQUEST_PER_THREAD;
	private final int MAX_RETRIES = 5;
	private static final String serverURL = "http://155.248.227.33:8080/skier_application/";
	private BlockingQueue<LiftRideEvent> liftRideEvents = new LinkedBlockingQueue<>();

    public ClientThread(int total_threads, int requests_per_thread) {
		this.TOTAL_THREADS = total_threads;
		this.REQUEST_PER_THREAD = requests_per_thread;
    }
    
	@Override
	public void run() {
		HttpClient client = HttpClient.newHttpClient();
		if(simpleClientTest(client)) {
			System.out.println("\nConnection established\n");
		}else {
			System.out.println("Connection not formed\n");
			return;		
		}
		
		CountDownLatch successCount = new CountDownLatch(TOTAL_THREADS * REQUEST_PER_THREAD);
		CountDownLatch failureCount = new CountDownLatch(TOTAL_THREADS * REQUEST_PER_THREAD);

        List<Long> eachRequestTimes = Collections.synchronizedList(new ArrayList<Long>());

        ExecutorService executorService = Executors.newFixedThreadPool(TOTAL_THREADS);
        long startTime = System.currentTimeMillis();
        EventThread generatorThread = new EventThread(liftRideEvents, TOTAL_THREADS,REQUEST_PER_THREAD);
        generatorThread.start();
		for(int i = 0; i < TOTAL_THREADS; i++) {
			executorService.submit(()->{
				for(int j = 0; j < REQUEST_PER_THREAD; j++) {
					try {
						long requestStartTime = System.currentTimeMillis();
						LiftRideEvent liftRideEvent = liftRideEvents.take();
						String url = serverURL + "skiers/" + Integer.toString(liftRideEvent.getResortID()) +
								"/seasons/" + liftRideEvent.getSeasonID() +
								"/days/" + liftRideEvent.getDayID() +
								"/skiers/" +
								Integer.toString(liftRideEvent.getSkierID());
						String requestBody = new Gson().toJson(liftRideEvent.getLiftRide());
						
						
						HttpRequest request = HttpRequest.newBuilder()
								  .uri(URI.create(url))
								  .POST(HttpRequest.BodyPublishers.ofString(requestBody))
								  .build();
						
						HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
						eachRequestTimes.add(System.currentTimeMillis() - requestStartTime);
						
						int retries = 0;
						// Retry up to MAX_RETRIES times for 4XX and 5XX response codes
						while (response.statusCode() >= 400 && retries < MAX_RETRIES) {
							retries++;
							System.out.println("Request failed with status code " + response.statusCode() + ", retrying (attempt " + retries + ")");
							response = client.send(request, HttpResponse.BodyHandlers.ofString());
						}
						
						// Check if the request was successful
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
		try {
			generatorThread.join();
			executorService.shutdown();
			executorService.awaitTermination(10, TimeUnit.MINUTES);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		long endTime = System.currentTimeMillis();
		long wallTime = endTime - startTime;
		int successfulRequests = TOTAL_THREADS * REQUEST_PER_THREAD - (int)failureCount.getCount();
	    int unsuccessfulRequests = (int)failureCount.getCount();
	    double throughput = (double) TOTAL_THREADS * REQUEST_PER_THREAD / (wallTime / 1000.0);
	    
	    int totalRequestTime = 0;
	    for(long time: eachRequestTimes) {
	    	totalRequestTime += time;
	    }

	    double averageRequestTime = (double) totalRequestTime / eachRequestTimes.size();
		double expectedThroughput = TOTAL_THREADS / (averageRequestTime / 1000);


        System.out.println("1.Number of successful requests sent: " + successfulRequests);
        System.out.println("2.Number of unsuccessful requests sent: " + unsuccessfulRequests);
        System.out.println("3.The total run time (wall time) for all phases to complete: " + wallTime + " ms");
        System.out.println("4.The total throughput in requests per second: " + throughput + " requests/second");
		System.out.println("5.Latency per request: " + averageRequestTime + "ms");
        System.out.println("6.Expected throughput is: " + expectedThroughput + " requests/second");
	}

	private static double findMean(Long[] a) {
		long sum = 0;
		for (Long aLong : a) {
			sum += aLong;
		}
		return (double) sum / (double) a.length;
	}

	private static double median(Long[] values) {
		double median;
		int totalElements = values.length;
		if (totalElements % 2 == 0) {
			long sumOfMiddleElements = values[totalElements / 2] +
					values[totalElements / 2 - 1];
			median = ((double) sumOfMiddleElements) / 2;
		} else {
			median = (double) values[values.length / 2];
		}
		return median;
	}

    private boolean simpleClientTest(HttpClient client) {
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
