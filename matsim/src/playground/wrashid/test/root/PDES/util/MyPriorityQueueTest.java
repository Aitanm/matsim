package playground.wrashid.test.root.PDES.util;

import java.util.LinkedList;
import java.util.PriorityQueue;

import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.PDES.util.ConcurrentListMPDSC;
import playground.wrashid.PDES.util.MyPriorityQueue;
import playground.wrashid.PDES2.Message;
import playground.wrashid.PDES2.StartingLegMessage;

public class MyPriorityQueueTest extends MatsimTestCase {

	public void testCompareTo(){
		t_compareTo(1,2);
		t_compareTo(2,1);
	}
	
	public void t_compareTo(double timeMessage1,double timeMessage2){
		PriorityQueue<MyPriorityQueue> queue=new PriorityQueue<MyPriorityQueue>();
		MyPriorityQueue mpq1=new MyPriorityQueue(new PriorityQueue());
		MyPriorityQueue mpq2=new MyPriorityQueue(new PriorityQueue());
		
		StartingLegMessage m1=new StartingLegMessage(null,null);
		m1.messageArrivalTime=timeMessage1;
		
		mpq1.getQueue().add(m1);
		
		m1=new StartingLegMessage(null,null);
		m1.messageArrivalTime=timeMessage1;
		
		mpq2.getQueue().add(m1);
		
		queue.add(mpq1);
		queue.add(mpq2);
		
		if (timeMessage1<timeMessage2){
			assert(queue.peek()==mpq1);
		} else if (timeMessage1>timeMessage2) {
			assert(queue.peek()==mpq2);
		}
	}
	
	
}
