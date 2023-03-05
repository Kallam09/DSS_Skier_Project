package com.assignment.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

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

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

//import org.eclipse.jetty.client.HttpClient;
//import org.eclipse.jetty.client.HttpResponse;
//import org.eclipse.jetty.client.api.ContentResponse;
//import org.eclipse.jetty.client.api.Request;
//import org.eclipse.jetty.client.util.StringContentProvider;
//import org.eclipse.jetty.http.HttpHeader;
//import org.eclipse.jetty.http.HttpMethod;
//import org.eclipse.jetty.http.MimeTypes;
//import org.eclipse.jetty.util.ssl.SslContextFactory;
//import org.eclipse.jetty.util.thread.QueuedThreadPool;
import com.assignment.model.LiftRide;
import com.assignment.model.LiftRideEvent;


class ClientTest {

	private static final String serverURL = "http://155.248.227.33:8080/skier_application/";

	
	// calls the API before proceeding, to establish that you have connectivity.
	@Test
	void simpleClientTest() {
		
		HttpClient client = HttpClient.newHttpClient();
		
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
			assertThat(response.statusCode(), equalTo(201));
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// create 32 threads that each send 1000 POST requests and terminate.
	@Test
	void test32ThreadsEach1000POST() throws Exception {
		final int NUM_CLIENTS = 32;
		final int NUM_POST_REQUESTS_PER_CLIENT = 1000;
		final int MAX_RETRIES = 5;
		
		CountDownLatch successLatch = new CountDownLatch(NUM_CLIENTS * NUM_POST_REQUESTS_PER_CLIENT);
		CountDownLatch failureLatch = new CountDownLatch(NUM_CLIENTS * NUM_POST_REQUESTS_PER_CLIENT);

        BlockingQueue<LiftRideEvent> queue = new LinkedBlockingQueue<>();

        List<Long> eachRequestTimes = Collections.synchronizedList(new ArrayList<>());

        ExecutorService executorService = Executors.newFixedThreadPool(NUM_CLIENTS);
        
        long startTime = System.currentTimeMillis();

        EventThread generatorThread = new EventThread(queue,NUM_CLIENTS,NUM_POST_REQUESTS_PER_CLIENT);
        generatorThread.start();
		for(int i = 0; i < NUM_CLIENTS; i++) {
			executorService.submit(()->{
				HttpClient client = HttpClient.newHttpClient();

				for(int j = 0; j < NUM_POST_REQUESTS_PER_CLIENT; j++) {
					long requestStartTime = System.currentTimeMillis();
					try {
						LiftRideEvent liftRideEvent = queue.take();
						String url = serverURL + "skiers/" + Integer.toString(liftRideEvent.getResortID()) + "/seasons/" + liftRideEvent.getSeasonID() + "/days/" + liftRideEvent.getDayID() + "/skiers/" + Integer.toString(liftRideEvent.getSkierID());
						String requestBody = new Gson().toJson(liftRideEvent.getLiftRide());
						
						
						HttpRequest request = HttpRequest.newBuilder()
								  .uri(URI.create(url))
								  .POST(HttpRequest.BodyPublishers.ofString(requestBody))
								  .build();
						
						HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
						eachRequestTimes.add(System.currentTimeMillis() - requestStartTime);
						
						int retries = 0;
						while (response.statusCode() >= 400 && retries < MAX_RETRIES) {
							retries++;
							System.out.println("Request failed with status code " + response.statusCode() + ", retrying (attempt " + retries + ")");
							response = client.send(request, HttpResponse.BodyHandlers.ofString());
						}

						if (response.statusCode() >= 400) {
							System.out.println("Request failed after " + MAX_RETRIES + " retries with status code " + response.statusCode());
							successLatch.countDown();
						} else {
							assertThat(response.statusCode(), equalTo(201));
							failureLatch.countDown();
						}
						
					} catch (IOException | InterruptedException e) {
						successLatch.countDown();
						System.out.println("Exception in thread: " + e.getMessage());
					}
				}
			});
		}
		generatorThread.join();
		executorService.shutdown();
		executorService.awaitTermination(10, TimeUnit.MINUTES);
		
		long endTime = System.currentTimeMillis();
		long wallTime = endTime - startTime;
		int successfulRequests = NUM_CLIENTS * NUM_POST_REQUESTS_PER_CLIENT - (int)failureLatch.getCount();
	    int unsuccessfulRequests = (int)failureLatch.getCount();
	    double throughput = (double) NUM_CLIENTS * NUM_POST_REQUESTS_PER_CLIENT / (wallTime / 1000.0);
	    
	    int totalRequestTime = 0;
	    for(long time: eachRequestTimes) {
	    	totalRequestTime += time;
	    }

		double averageRequestTime = (double) totalRequestTime / eachRequestTimes.size();
		double expectedThroughput = NUM_CLIENTS / (averageRequestTime / 1000);


		System.out.println("1.Number of successful requests sent: " + successfulRequests);
		System.out.println("2.Number of unsuccessful requests sent: " + unsuccessfulRequests);
		System.out.println("3.The total run time (wall time) for all phases to complete: " + wallTime + " ms");
		System.out.println("4.The total throughput in requests per second: " + throughput + " requests/second");
		System.out.println("5.Latency per request: " + averageRequestTime + "ms");
		System.out.println("6.Expected throughput is: " + expectedThroughput + " requests/second");
	}
	
	// You should test how long a single request takes to estimate this latency. 
	// Run a simple test and send eg 500 requests from a single thread to do this.
	@Test
	void singleThreadWith500Requests() throws Exception {
        List<Long> eachRequestTimes = Collections.synchronizedList(new ArrayList<>());
		
		HttpClient client = HttpClient.newHttpClient();
		
		String serviceUrl = serverURL + "skiers/1/seasons/2022/days/111/skiers/1";
		LiftRide liftRide = new LiftRide((short)111,(short)11);
		String requestBody = new Gson().toJson(liftRide);
		HttpRequest request = HttpRequest.newBuilder()
				  .uri(URI.create(serviceUrl))
				  .POST(HttpRequest.BodyPublishers.ofString(requestBody))
				  .build();
		
		for (int i = 0; i < 500; i++) {
			long requestStartTime = System.currentTimeMillis();
			HttpResponse<String> response;
			try {
				response = client.send(request, HttpResponse.BodyHandlers.ofString());
				assertThat(response.statusCode(), equalTo(201));
				eachRequestTimes.add(System.currentTimeMillis() - requestStartTime);
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
		int totalRequestTime = 0;
	    for(long time: eachRequestTimes) {
	    	totalRequestTime += time;
	    }
	    
	    // each request latency
	    double averageTimeEachRequest = (double) totalRequestTime / eachRequestTimes.size();
	    System.out.println("Test of each request latency: " + averageTimeEachRequest + "ms");
	}
}






