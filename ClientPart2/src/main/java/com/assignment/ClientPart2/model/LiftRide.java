package com.assignment.ClientPart2.model;

public class LiftRide {
	private short liftTime;
	private short liftRideID;
	
	public LiftRide() {
	}
	
	public LiftRide(short time, short liftID) {
		this.liftTime = time;
		this.liftRideID = liftID;
	}

	public short getLiftTime() {
		return liftTime;
	}
	public void setLiftTime(short liftTime) {
		this.liftTime = liftTime;
	}
	public short getLiftRideID() {
		return liftRideID;
	}
	public void setLiftRideID(short liftRideID) {
		this.liftRideID = liftRideID;
	}
	@Override
	public String toString() {
		return "LiftRide [time=" + liftTime + ", liftID=" + liftRideID + "]";
	}
}
