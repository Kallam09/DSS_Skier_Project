package com.assignment.ClientPart1;



import com.assignment.ClientPart1.controller.ClientThread;


public class Client1
{
	private static final int NUM_THREADS = 32;
	private static final int NUM_POST_REQUESTS_PER_THREAD = 1000;
	
    public static void main( String[] args )
    {
        
        
        // At startup, you must create 32 threads that each send 1000 POST requests and terminate.
        ClientThread clientThread = new ClientThread(NUM_THREADS, NUM_POST_REQUESTS_PER_THREAD);
        clientThread.start();
        try {
			clientThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        System.out.println();
        
        // Once any of these have completed you are free to create as few or as many threads as you like until all the 10K POSTS have been sent.
        ClientThread clientThread2 = new ClientThread(100, 100);
        clientThread2.start();
        try {
			clientThread2.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        
        // Once any of these have completed you are free to create as few or as many threads as you like until all the 10K POSTS have been sent.
        ClientThread clientThread3 = new ClientThread(1, 500);
        clientThread3.start();
        try {
			clientThread3.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

    }
    
    
}
