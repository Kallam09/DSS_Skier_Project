package com.assignment.controller;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.assignment.model.LiftRide;

@WebServlet("skiers/*")
public class SkierServlet extends HttpServlet{

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Gson gson = new Gson();
		JsonObject jsonObject;
		PrintWriter out = response.getWriter();
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		
		String[] pathParts = request.getRequestURI().split("/");
		// /skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}
		// get path parameter
	    if (pathParts[pathParts.length - 2].equals("skiers") && pathParts[pathParts.length-4].equals("days") && pathParts[pathParts.length-6].equals("seasons")) {
	    	// path parameter
	        String resortIDString = pathParts[pathParts.length - 7];
	        String seasonID = pathParts[pathParts.length - 5]; 
	        String dayID = pathParts[pathParts.length - 3]; 
	        String skierIDString = pathParts[pathParts.length - 1];
	        
	        // validation
	        int resortID, dayIDInt, skierID;
	        try {
	        	resortID = Integer.parseInt(resortIDString);
	        	dayIDInt = Integer.parseInt(dayID);
	        	skierID = Integer.parseInt(skierIDString);
	        	if(skierID < 1 || skierID > 100000) {
	        		throw new Exception("skierID should be in range [1, 100000]");
	        	}
	        	if(resortID < 1 || resortID > 10) {
	        		throw new Exception("resortID should be in range [1, 10]");
	        	}
	        	if(dayIDInt < 1 || dayIDInt > 366) {
	        		throw new Exception("dayID should be in range [1, 366]");
	        	}
	        	if(seasonID.isEmpty() || seasonID == null) {
	        		throw new Exception("season ID should not be empty");
	        	}
	        } catch (Exception e) {
	        	jsonObject = new JsonObject();
	        	jsonObject.addProperty("message","Invalid input: " + e.getMessage());
	        	String jsonString = gson.toJson(jsonObject);
	        	
	        	response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		        out.print(jsonString);
		        out.flush();
		        return;
	        }
	        
	        short liftID, time;
	        try {
		        StringBuilder sb = new StringBuilder();
		        BufferedReader reader = request.getReader();
		        String line;
		        while ((line = reader.readLine()) != null) {
		            sb.append(line);
		        }
		        String requestBody = sb.toString();
		        LiftRide liftRide= (LiftRide) gson.fromJson(requestBody, LiftRide.class);
		        liftID = liftRide.getLiftRideID();
		        time = liftRide.getLiftTime();
		        // validation
		        if(liftID < 1 || liftID > 40) {
		        	throw new Exception("liftID should be in range [1,40]");
		        }
		        if(time < 1 || time > 360) {
		        	throw new Exception("time should be in range [1,360]");
		        }
				response.setStatus(HttpServletResponse.SC_CREATED);
	        } catch (Exception e) {
	        	jsonObject = new JsonObject();
	        	jsonObject.addProperty("message","Invalid input: " + e.getMessage());
	        	String jsonString = gson.toJson(jsonObject);
	        	
	        	response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		        out.print(jsonString);
		        out.flush();
		        return;
	        }
	        
	        response.setStatus(HttpServletResponse.SC_CREATED);
	    }
	}
}
