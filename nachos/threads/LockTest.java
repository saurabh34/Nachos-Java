package nachos.threads;

import nachos.machine.Machine;


public class LockTest {

	private static class AcessSharedData implements Runnable {
		AcessSharedData() { }

		public void run() {
			S.V();
			lock.acquire();
			System.out.println(KThread.currentThread().getName()+" acquiring lock1 and entering critical section");
			sharedData=sharedData+1;
			System.out.println("shared data value: "+sharedData);
			
			KThread.currentThread().yield();
			System.out.println(KThread.currentThread().getName()+" releasing lock1 and exiting critical section");
			lock.release();
			
		}
	}
	
	private static class AcessSharedData2 implements Runnable {
		AcessSharedData2() { }

		public void run() {
			S.V();
			lock2.acquire();
			System.out.println(KThread.currentThread().getName()+" acquiring lock2 and entering critical section");
			sharedData2=sharedData2+1;
			System.out.println("shared data value: "+sharedData2);
			KThread.currentThread().yield();
			System.out.println(KThread.currentThread().getName()+" releasing lock2 and exiting critical section");
			lock2.release();
			
		}
	}
	
	private static class AcessSharedData21 implements Runnable {
		AcessSharedData21() { }

		public void run() {
			
			lock2.acquire();
			System.out.println(KThread.currentThread().getName()+" acquiring lock2 and entering critical section");
			S.V();
			lock.acquire();
			System.out.println(KThread.currentThread().getName()+" acquiring lock1 and entering critical section");
			sharedData2=sharedData2+1;
			System.out.println("shared data value: "+sharedData2);
			KThread.currentThread().yield();
			System.out.println(KThread.currentThread().getName()+" releasing lock1 and exiting critical section");
			lock.release();
			System.out.println(KThread.currentThread().getName()+" releasing lock2 and exiting critical section");
			lock2.release();
			
		}
	}
	
	private static class AcessSharedData3 implements Runnable {
		AcessSharedData3() { }

		public void run() {
			S.V();
			lock3.acquire();
			System.out.println(KThread.currentThread().getName()+" acquiring lock3 and entering critical section");
			sharedData2=sharedData2+1;
			System.out.println("shared data value: "+sharedData2);
			KThread.currentThread().yield();
			System.out.println(KThread.currentThread().getName()+" releasing lock3 and exiting critical section");
			lock3.release();
			
		}
	}
	
	private static class AcessSharedData31 implements Runnable {
		AcessSharedData31() { }

		public void run() {
			
			lock3.acquire();
			System.out.println(KThread.currentThread().getName()+" acquiring lock3 and entering critical section");
			S.V();
			lock.acquire();
			System.out.println(KThread.currentThread().getName()+" acquiring lock1 and entering critical section");
			sharedData2=sharedData2+1;
			System.out.println("shared data value: "+sharedData2);
			KThread.currentThread().yield();
			System.out.println(KThread.currentThread().getName()+" releasing lock1 and exiting critical section");
			lock.release();
			System.out.println(KThread.currentThread().getName()+" releasing lock3 and exiting critical section");
			lock3.release();
			
		}
	}
	
	private static class AcessSharedData321 implements Runnable {
		AcessSharedData321() { }

		public void run() {
			S.V();
			lock3.acquire();
			System.out.println(KThread.currentThread().getName()+" acquiring lock3 and entering critical section");
			lock2.acquire();
			System.out.println(KThread.currentThread().getName()+" acquiring lock2 and entering critical section");
			lock.acquire();
			System.out.println(KThread.currentThread().getName()+" acquiring lock1 and entering critical section");
			sharedData2=sharedData2+1;
			System.out.println("shared data value: "+sharedData2);
			KThread.currentThread().yield();
			System.out.println(KThread.currentThread().getName()+" releasing lock1 and exiting critical section");
			lock.release();
			System.out.println(KThread.currentThread().getName()+" releasing lock2 and exiting critical section");
			lock2.release();
			System.out.println(KThread.currentThread().getName()+" releasing lock3 and exiting critical section");
			lock3.release();
			
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
	    ThreadedKernel.scheduler.setPriority(t3, 6);
	    ThreadedKernel.scheduler.setPriority(t4, 4);
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
		KThread M = new KThread(new AcessSharedData21()).setName("M");
		KThread H = new KThread(new AcessSharedData2()).setName("H");
		
		KThread L1 = new KThread(new AcessSharedData31()).setName("L1");
		KThread H1 = new KThread(new AcessSharedData3()).setName("H1");
		
		
	    boolean oldInterrupStatus = Machine.interrupt().disable();
	    
	    ThreadedKernel.scheduler.setPriority(L, 8);
	    ThreadedKernel.scheduler.setPriority(M, 4);
	    ThreadedKernel.scheduler.setPriority(H, 2);
	    ThreadedKernel.scheduler.setPriority(H1, 1);
	    ThreadedKernel.scheduler.setPriority(L1, 6);
	    
	    Machine.interrupt().restore(oldInterrupStatus);
		
	    L.fork();
		S.P();
		L1.fork();
		S.P();
		M.fork();
		S.P();
	    H.fork();
	    S.P();
	    H1.fork();
	    //KThread.yield();
	     
	     H.join();
		

	}
	
	
	public static void simpleTest3() {
		System.out.println("-----------AcessSharedData------------------");
		//KThread.yield(); // set lastScheduled

		KThread L = new KThread(new AcessSharedData()).setName("L");
		KThread M = new KThread(new AcessSharedData21()).setName("M");
		KThread H = new KThread(new AcessSharedData2()).setName("H");
	

	    boolean oldInterrupStatus = Machine.interrupt().disable();
	    ThreadedKernel.scheduler.setPriority(L, 8);
	    ThreadedKernel.scheduler.setPriority(M, 4);
	    ThreadedKernel.scheduler.setPriority(H, 2);
	   
	    Machine.interrupt().restore(oldInterrupStatus);
		
	    L.fork();
	    S.P();
	    M.fork();
		//KThread.yield();
	    S.P();
	    H.fork();
	   
	    //KThread.yield();
	     
	     H.join();
		

	}
	
	
	public static void simpleTest4() {
		System.out.println("-----------AcessSharedData------------------");
		//KThread.yield(); // set lastScheduled

		KThread L = new KThread(new AcessSharedData()).setName("L");
		KThread M = new KThread(new AcessSharedData()).setName("M");
		KThread H = new KThread(new AcessSharedData21()).setName("H");
		KThread H1 = new KThread(new AcessSharedData2()).setName("H1");

	    boolean oldInterrupStatus = Machine.interrupt().disable();
	    ThreadedKernel.scheduler.setPriority(L, 8);
	    ThreadedKernel.scheduler.setPriority(M, 6);
	    ThreadedKernel.scheduler.setPriority(H, 4);
	    ThreadedKernel.scheduler.setPriority(H1, 3);
	    
	    Machine.interrupt().restore(oldInterrupStatus);
		
	    L.fork();
	    S.P();
	    M.fork();
		S.P();
	    H.fork();
	    S.P();
	    H1.fork();
	    //KThread.yield();
	     
	    H1.join();
		

	}
	
	public static void simpleTest5() {
		System.out.println("-----------AcessSharedData------------------");
		//KThread.yield(); // set lastScheduled

		KThread L = new KThread(new AcessSharedData()).setName("L");
		KThread M = new KThread(new AcessSharedData()).setName("M");
		KThread H = new KThread(new AcessSharedData321()).setName("H");
		KThread H1 = new KThread(new AcessSharedData2()).setName("H1");
		KThread H2 = new KThread(new AcessSharedData3()).setName("H2");
		KThread H3 = new KThread(new AcessSharedData3()).setName("H3");
		
	    boolean oldInterrupStatus = Machine.interrupt().disable();
	    ThreadedKernel.scheduler.setPriority(L, 8);
	    ThreadedKernel.scheduler.setPriority(M, 6);
	    ThreadedKernel.scheduler.setPriority(H, 5);
	    ThreadedKernel.scheduler.setPriority(H1, 3);
	    ThreadedKernel.scheduler.setPriority(H2, 2);
	    ThreadedKernel.scheduler.setPriority(H3, 1);
	    
	    Machine.interrupt().restore(oldInterrupStatus);
		
	    L.fork();
	    S.P();
	    M.fork();
		S.P();
	    H.fork();
	    S.P();
	    H1.fork();
	    S.P();
	    H2.fork();
	    S.P();
	    H3.fork();
	    //KThread.yield();
	     
	    H1.join();
		

	}
	
	public static void simpleTest6() {
		System.out.println("-----------AcessSharedData------------------");
		//KThread.yield(); // set lastScheduled

		KThread L = new KThread(new AcessSharedData()).setName("L");
		KThread M = new KThread(new AcessSharedData()).setName("M");
		KThread H = new KThread(new AcessSharedData321()).setName("H");
		KThread H1 = new KThread(new AcessSharedData2()).setName("H1");
		KThread H2 = new KThread(new AcessSharedData3()).setName("H2");
		
	    boolean oldInterrupStatus = Machine.interrupt().disable();
	    ThreadedKernel.scheduler.setPriority(L, 8);
	    ThreadedKernel.scheduler.setPriority(M, 6);
	    ThreadedKernel.scheduler.setPriority(H, 4);
	    ThreadedKernel.scheduler.setPriority(H1, 2);
	    ThreadedKernel.scheduler.setPriority(H2, 3);
	    
	    Machine.interrupt().restore(oldInterrupStatus);
		
	    L.fork();
	    S.P();
	    M.fork();
		S.P();
	    H.fork();
	    S.P();
	    H1.fork();
	    S.P();
	    H2.fork();
	    
	    //KThread.yield();
	     
	    H2.join();
		

	}
	
	static Lock lock = new Lock();
	static Lock lock2 = new Lock();
	static Lock lock3 = new Lock();
	static int sharedData = 0;
	static int sharedData2 = 0;
	private static Semaphore S = new Semaphore(0);

}
