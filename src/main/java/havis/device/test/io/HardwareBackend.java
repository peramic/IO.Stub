package havis.device.test.io;

import havis.device.io.Direction;
import havis.device.io.State;
import havis.device.test.hardware.HardwareMgmt;
import havis.device.test.hardware.IODirectionEnumeration;
import havis.device.test.hardware.IOStateEnumeration;
import havis.device.test.hardware.RequestAbstractType;
import havis.device.test.hardware.RequestCreateIOType;
import havis.device.test.hardware.RequestCreateIOsType;
import havis.device.test.hardware.RequestCreateSubscriberType;
import havis.device.test.hardware.RequestCreateSubscribersType;
import havis.device.test.hardware.RequestCreateType;
import havis.device.test.hardware.RequestDeleteIOsType;
import havis.device.test.hardware.RequestDeleteSubscribersType;
import havis.device.test.hardware.RequestDeleteType;
import havis.device.test.hardware.RequestReadType;
import havis.device.test.hardware.RequestType;
import havis.device.test.hardware.RequestUpdateType;
import havis.device.test.hardware.ResponseCreateType;
import havis.device.test.hardware.ResponseReadType;
import havis.device.test.hardware.ResponseType;
import havis.device.test.hardware.SubscriberIOType;
import havis.device.test.hardware.SubscriberIOsType;
import havis.device.test.hardware.SubscriberInstanceType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class HardwareBackend {

	private long operationId = 0;
	private static final String DEFAULT_CONFIG_ID = "default";
	private HardwareMgmt hwMgmt;
	private HashMap<Short, String> subscriberIdMap = new HashMap<>();

	public HardwareBackend(HardwareMgmt hwMgmt) {
		super();
		this.hwMgmt = hwMgmt;
		this.hwMgmt.process(createRequest(DEFAULT_CONFIG_ID,
				createCreateRequest()));
	}

	public List<RequestType> createRequest(String configId, RequestAbstractType choice) {
		List<RequestType> requests = new ArrayList<>();
		RequestType request = new RequestType();
		request.setConfigId(configId);
		request.getChoice().add(choice);
		requests.add(request);
		return requests;
	}

	public RequestCreateType createCreateRequest() {
		RequestCreateType create = new RequestCreateType();
		create.setOperationId(createOperationId());
		return create;
	}

	public RequestReadType createReadRequest() {
		RequestReadType read = new RequestReadType();
		read.setOperationId(createOperationId());
		return read;
	}

	public RequestDeleteType createDeleteRequest() {
		RequestDeleteType delete = new RequestDeleteType();
		delete.setOperationId(createOperationId());
		return delete;
	}

	public RequestUpdateType createUpdateRequest() {
		RequestUpdateType update = new RequestUpdateType();
		update.setOperationId(createOperationId());
		return update;
	}

	public String createOperationId() {
		return String.valueOf(operationId++);
	}

	public String createSubscriber(short id, boolean enabled,
			SubscriberInstanceType subscr) {
		RequestCreateSubscriberType subscriber = new RequestCreateSubscriberType();
		subscriber.setSubscriberId("" + id);
		SubscriberIOsType ios = new SubscriberIOsType();
		SubscriberIOType io = new SubscriberIOType();
		io.setIoId(id);
		io.setStateChanged(enabled);
		ios.getIo().add(io);
		subscriber.setIos(ios);
		SubscriberInstanceType instance = subscr;
		subscriber.setInstance(instance);

		RequestCreateType create = createCreateRequest();
		RequestCreateSubscribersType subscribers = new RequestCreateSubscribersType();
		subscribers.getSubscriber().add(subscriber);
		create.setSubscribers(subscribers);

		List<ResponseType> resps = hwMgmt.process(createRequest(
				DEFAULT_CONFIG_ID, create));
		String subscrId = ((ResponseCreateType) resps.get(0).getChoice().get(0))
				.getSubscribers().getSubscriberIds().get(0).getSubscriberId();
		subscriberIdMap.put(id, subscrId);
		return subscrId;
	}

	public void updateSubscriber(short id, boolean enable,
			SubscriberInstanceType subscr) {

		if (!subscriberIdMap.containsKey(id)) {
			createSubscriber(id, enable, subscr);
			return;
		}

		RequestCreateSubscriberType subscriber = new RequestCreateSubscriberType();
		subscriber.setSubscriberId(subscriberIdMap.get(id));

		SubscriberIOsType ios = new SubscriberIOsType();
		SubscriberIOType io = new SubscriberIOType();
		io.setIoId(id);
		io.setStateChanged(enable);
		ios.getIo().add(io);
		subscriber.setIos(ios);
		SubscriberInstanceType instance = subscr;
		subscriber.setInstance(instance);

		RequestUpdateType update = createUpdateRequest();
		RequestCreateSubscribersType subscribers = new RequestCreateSubscribersType();
		subscribers.getSubscriber().add(subscriber);
		update.setSubscribers(subscribers);

		hwMgmt.process(createRequest(DEFAULT_CONFIG_ID, update));
	}

	public void updateIO(RequestCreateIOType io) {
		RequestUpdateType update = createUpdateRequest();
		RequestCreateIOsType ios = new RequestCreateIOsType();
		ios.getIo().add(io);
		update.setIos(ios);
		hwMgmt.process(createRequest(DEFAULT_CONFIG_ID, update));
	}

	public RequestCreateSubscriberType getSubscriber(short id) {
		RequestReadType read = createReadRequest();
		read.setSubscribers(new RequestDeleteSubscribersType());
		List<ResponseType> resp = hwMgmt.process(createRequest(
				DEFAULT_CONFIG_ID, read));

		RequestCreateSubscribersType subscribers = ((ResponseReadType) resp
				.get(0).getChoice().get(0)).getSubscribers();
		if (subscribers != null) {
			for (RequestCreateSubscriberType subscriber : subscribers
					.getSubscriber()) {
				if (subscriber.getIos().getIo().get(0).getIoId() == id)
					return subscriber;
			}
		}

		return null;

	}

	public void deleteSubscriber(short id) {
		RequestDeleteSubscribersType subscribers = new RequestDeleteSubscribersType();
		subscribers.getSubscriberId().addAll(
				Arrays.asList(subscriberIdMap.get(id)));

		RequestDeleteType delete = createDeleteRequest();
		delete.setSubscribers(subscribers);

		hwMgmt.process(createRequest(DEFAULT_CONFIG_ID, delete));

		subscriberIdMap.remove(id);
	}

	public RequestCreateIOsType getIOs() {
		RequestReadType read = createReadRequest();
		read.setIos(new RequestDeleteIOsType());
		List<ResponseType> resp = hwMgmt.process(createRequest(
				DEFAULT_CONFIG_ID, read));

		RequestCreateIOsType ios = ((ResponseReadType) resp.get(0).getChoice()
				.get(0)).getIos();
		return ios == null ? new RequestCreateIOsType() : ios;
	}

	public RequestCreateIOType getIO(short id) {
		RequestCreateIOsType ios = getIOs();
		for (RequestCreateIOType io : ios.getIo())
			if (io.getIoId() == id)
				return io;

		return null;
	}

	public State map(IOStateEnumeration state) {
		switch (state) {
		case HIGH:
			return State.HIGH;
		case LOW:
			return State.LOW;
		default:
			return State.UNKNOWN;
		}
	}

	public IOStateEnumeration map(State state) {
		switch (state) {
		case HIGH:
			return IOStateEnumeration.HIGH;
		case LOW:
			return IOStateEnumeration.LOW;
		default:
			return IOStateEnumeration.UNKNOWN;
		}
	}

	public Direction map(IODirectionEnumeration direction) {
		switch (direction) {
		case INPUT:
			return Direction.INPUT;
		default:
			return Direction.OUTPUT;
		}
	}

	public IODirectionEnumeration map(Direction direction) {
		switch (direction) {
		case INPUT:
			return IODirectionEnumeration.INPUT;
		default:
			return IODirectionEnumeration.OUTPUT;
		}
	}

	public List<Short> getSubscriberKeys() {
		List<Short> subscrKeys = new ArrayList<>();
		subscrKeys.addAll(subscriberIdMap.keySet());
		return subscrKeys;
	}
}