package com.assignment.ClientPart1;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.assignment.ClientPart1.model.LiftRide;
import org.junit.Test;

import com.google.gson.Gson;

/**
 * Unit test for client1starter.
 */
public class Client1Test
{
    /**
     * Rigorous Test :-)
     */
	// calls the API before proceeding, to establish that you have connectivity.
		@Test
		public void testClient() {

			HttpClient client = HttpClient.newHttpClient();

			String serviceUrl =  "http://155.248.227.33:8080/skier_application/skiers/1/seasons/2022/days/111/skiers/1";
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
}
