package cto.hmi.processor.nlu.soda.classification;

import java.util.List;

import opennlp.model.Event;
import opennlp.model.EventStream;

public class ListEventStream implements EventStream {
	  List<Event> events;
	  int currentIndex = 0;
	  int numEvents;

	  public ListEventStream (List<Event> events) {
	    this.events = events;
	    numEvents = events.size();
	  }
	  
	  public Event next () {
	    return events.get(currentIndex++);
	  }
	  
	  public boolean hasNext () {
	    return currentIndex < numEvents;
	  }
	  
}
