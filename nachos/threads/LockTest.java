package nachos.threads;

import nachos.machine.Machine;


public class LockTest {

	private static class AcessSharedData implements Runnable {
		AcessSharedData() { }

		public void run() {
			
			lock.acquire();
			System.out.println(KThread.currentThread().getName()+" acquiring lock and entering critical section");
			sharedData=sharedData+1;
			System.out.println("shared data value: "+sharedData);
			S.V();
			KThread.currentThread().yield();
			lock.release();
			System.out.println(KThread.currentThread().getName()+" releasing lock and exiting critical section");
		}
	}

	private static class simpleThread implements Runnable {
		simpleThread() { }

		public void run() {
			for(int i=0;i<99999999;i++){}
		}
	}

	
	public static void simpleTest() {
		System.out.println("-----------AcessSharedData------------------");
		//KThread.yield(); // set lastScheduled

		KThread t1 = new KThread(new AcessSharedData()).setName("t1");
		KThread t2 = new KThread(new AcessSharedData()).setName("t2");
		KThread t3 = new KThread(new AcessSharedData()).setName("t3");
		KThread t4 = new KThread(new AcessSharedData()).setName("t4");
		

	    boolean oldInterrupStatus = Machine.interrupt().disable();
	    ThreadedKernel.scheduler.setPriority(t1, 8);
	    ThreadedKernel.scheduler.setPriority(t2, 10);
	    ThreadedKernel.scheduler.setPriority(t3, 2);
	    ThreadedKernel.scheduler.setPriority(t4, 2);
	    Machine.interrupt().restore(oldInterrupStatus);
		t1.fork();
		S.P();
		//KThread.yield();
		t2.fork();
	    t3.fork();
	    t4.fork();
	    //KThread.yield();
	     
	    t4.join();
		

	}

	
	
	
	public static void simpleTest2() {
		System.out.println("-----------AcessSharedData------------------");
		//KThread.yield(); // set lastScheduled

		KThread L = new KThread(new AcessSharedData()).setName("L");
		KThread M = new KThread(new simpleThread()).setName("M");
		KThread H = new KThread(new AcessSharedData()).setName("H");
	

	    boolean oldInterrupStatus = Machine.interrupt().disable();
	    ThreadedKernel.scheduler.setPriority(L, 8);
	    ThreadedKernel.scheduler.setPriority(M, 4);
	    ThreadedKernel.scheduler.setPriority(H, 2);
	   
	    Machine.interrupt().restore(oldInterrupStatus);
		
	    L.fork();
		S.P();
		//KThread.yield();
		M.fork();
	    H.fork();
	   
	    //KThread.yield();
	     
	     H.join();
		

	}
	
	
	
	static Lock lock = new Lock();
	static int sharedData = 0;
	private static Semaphore S = new Semaphore(0);

}
