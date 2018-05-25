package pcd.ass03.gameoflife.mailboxes;

import java.util.Comparator;

import com.typesafe.config.Config;

import akka.actor.ActorSystem;
import akka.dispatch.Envelope;
import akka.dispatch.UnboundedStablePriorityMailbox;
import pcd.ass03.gameoflife.actors.SchedulerActor;

public class PrioritySchedulerMailbox extends UnboundedStablePriorityMailbox {

	public PrioritySchedulerMailbox(final ActorSystem.Settings settings, final Config config) {
		super(new SchedulerMsgComparator());
	}
	
	public static class SchedulerMsgComparator implements Comparator<Envelope> {

		@Override
		public int compare(final Envelope o1, final Envelope o2) {
			if (o1.message() instanceof SchedulerActor.TickMsg
					&& !(o2.message() instanceof SchedulerActor.TickMsg)) {
				return 1;
			} else if (!(o1.message() instanceof SchedulerActor.TickMsg)
					&& o2.message() instanceof SchedulerActor.TickMsg) {
				return -1;
			} else {
				return 0;
			}
		}
		
	}
	
}
