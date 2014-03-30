package nachos.threads;
import nachos.machine.*;

import java.util.Iterator;
import java.util.LinkedList;


public class FCFSQueue extends ThreadQueue {
		FCFSQueue(){}
		
		

		public void waitForAccess(KThread thread) {
			waitQueue.add(thread);
		}

		public void acquire(KThread thread) {
			 Lib.assertTrue(Machine.interrupt().disabled());
		       
			 Lib.assertTrue(waitQueue.isEmpty());
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			if (waitQueue.isEmpty())
				return null;
			return waitQueue.removeFirst();
		}

		public void print(){}
		public void reorderPriorityQueue(){
			}
		
		public int getMaxThreadPriority(){
			if(waitQueue.isEmpty()){
				return -1;
			}
			int maxThreadPriority=waitQueue.getFirst().inherentPriority;
			for (KThread thread:waitQueue){
				if(thread.inherentPriority <maxThreadPriority ){
					maxThreadPriority=thread.inherentPriority;
				}
			}
			return maxThreadPriority;
			
		}
		
		public void ChangeLockOwnerOfWaitingThreadsTo(KThread newLockOwner){
			Iterator<KThread> iter=waitQueue.iterator();
			while(iter.hasNext()){
				KThread thread=iter.next();
				thread.WaitingOnlockThread=newLockOwner;
			}
			
		}
		
		public LinkedList<Integer> getThreadIdsOfLock(){
			if(waitQueue.isEmpty()){
				return null;
			}
			LinkedList<Integer> threadIds = new LinkedList<Integer>();
			for (KThread thread:waitQueue){
				threadIds.add(thread.id);
			}
			return threadIds;
		}
		private LinkedList<KThread> waitQueue = new LinkedList<KThread>();
		
	}



