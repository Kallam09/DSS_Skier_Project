package com.assignment.ClientPart2;

import com.assignment.ClientPart2.controller.ClientThread;


public class Client2
{
    public static void main( String[] args )
    {
    	 // Once any of these have completed you are free to create as few or as many threads as you like until all the 10K POSTS have been sent.
        ClientThread clientThread2 = new ClientThread(100, 100);
        clientThread2.start();
        try {
			clientThread2.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }
}
