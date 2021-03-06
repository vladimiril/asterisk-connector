package ru.ntechs.asteriskconnector.eventchain;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import ru.ntechs.ami.Message;
import ru.ntechs.asteriskconnector.bitrix.BitrixTelephony;
import ru.ntechs.asteriskconnector.config.ConnectorConfig;

@Slf4j
@Component
public class EventDispatcher {
	final static int EVENT_LIFETIME = 600;

	private ConcurrentHashMap<String, EventChain> chains;
	private EventChain unmappableEvents;
	private int tickCount;

	private ConnectorConfig config;

	@Autowired
	private BitrixTelephony bitrixTelephony;

	public EventDispatcher(ConnectorConfig config) {
		this.tickCount = 0;
		this.chains = new ConcurrentHashMap<>();
		this.config = config;
		this.unmappableEvents = new EventChain(config.getRules());
	}

	public void dispatch(Message msg) {
		EventChain eventChain;

		String uniqueId = msg.getAttribute("Uniqueid");

		if (uniqueId != null) {
			eventChain = chains.get(uniqueId);

			if (eventChain == null) {
				eventChain = new EventChain(config.getRules());
				chains.put(uniqueId, eventChain);
			}
		}
		else
			eventChain = unmappableEvents;

		eventChain.enqueue(tickCount, msg);

//		ConnectorRule rule = eventChain.getLastMatched();

//		if (rule != null)
//			bitrixTelephony.registerCall();
	}

	public void collectGarbage() {
		tickCount++;

		for (Entry<String, EventChain> chainEntry : chains.entrySet()) {
//			log.info(chainEntry.getValue().toString());

			if ((tickCount - chainEntry.getValue().getTailBirthTicks()) > EVENT_LIFETIME)
				chains.remove(chainEntry.getKey());
		}

		unmappableEvents.garbageCollect(tickCount - EVENT_LIFETIME);

//		if (!unmappableEvents.isEmpty())
//			log.info(unmappableEvents.toString());
	}
}
