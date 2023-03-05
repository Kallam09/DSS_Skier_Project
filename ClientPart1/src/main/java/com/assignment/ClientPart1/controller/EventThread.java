package com.assignment.ClientPart1.controller;

import java.util.Random;
import java.util.concurrent.BlockingQueue;

import com.assignment.ClientPart1.model.LiftRide;
import com.assignment.ClientPart1.model.LiftRideEvent;

public class EventThread extends Thread {

	private int TOTAL_CLIENTS;
	private int REQUEST_PER_THREAD;
	private BlockingQueue<LiftRideEvent> rideEvents;

	public EventThread(BlockingQueue<LiftRideEvent> queue, int total_clients, int requests_per_thread) {
		this.rideEvents = queue;
		this.TOTAL_CLIENTS = total_clients;
		this.REQUEST_PER_THREAD = requests_per_thread;
	}
	
	@Override
	public void run() {
		for (int i = 0; i < TOTAL_CLIENTS * REQUEST_PER_THREAD; i++) {
			LiftRideEvent liftRideEvent = dataGeneration();
			rideEvents.add(liftRideEvent);
		}
	}
	
	// Random skier lift ride event that is used to form a POST request
	LiftRideEvent dataGeneration() {
		Random random = new Random();
		int resortID = random.nextInt(10) + 1;
		int skierID = random.nextInt(100000) + 1;
		String seasonID = "2022";
		String dayID = "1";
		short liftID = (short) (random.nextInt(40) + 1);
		short time = (short) (random.nextInt(360) + 1);
		LiftRide liftRide = new LiftRide(time, liftID);
		LiftRideEvent liftRideEvent = new LiftRideEvent(resortID, seasonID, dayID, skierID, liftRide);
		return liftRideEvent;
	}
}
