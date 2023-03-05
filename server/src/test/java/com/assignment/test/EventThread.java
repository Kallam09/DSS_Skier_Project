package com.assignment.test;

import java.util.Random;
import java.util.concurrent.BlockingQueue;

import com.assignment.model.LiftRide;
import com.assignment.model.LiftRideEvent;

public class EventThread extends Thread {
	
	private BlockingQueue<LiftRideEvent> rideEvents;
	private int TOTAL_CLIENTS;
	private int REQUEST_PER_THREAD;
	
	public EventThread(BlockingQueue<LiftRideEvent> queue, int total_clients, int requests_per_thread) {
		// TODO Auto-generated constructor stub
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
