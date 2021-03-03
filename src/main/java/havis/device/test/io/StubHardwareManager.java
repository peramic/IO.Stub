package havis.device.test.io;

import havis.device.io.Direction;
import havis.device.io.State;
import havis.device.io.StateEvent;
import havis.device.io.StateListener;
import havis.device.io.common.HardwareManager;
import havis.device.test.hardware.HardwareMgmt;
import havis.device.test.hardware.HardwareMgmtSubscriber;
import havis.device.test.hardware.IOStateEnumeration;
import havis.device.test.hardware.NotificationIOType;
import havis.device.test.hardware.NotificationType;
import havis.device.test.hardware.RequestCreateIOType;
import havis.device.test.hardware.RequestCreateIOsType;
import havis.device.test.hardware.SubscriberInstanceType;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StubHardwareManager implements HardwareManager {

	private static final Logger logger = Logger.getLogger(StubHardwareManager.class.getName());

	private StateListener listener;
	private HardwareBackend hwApi;

	private static HardwareMgmt hwMgmt;

	public static void setHardwareMgmt(HardwareMgmt hwMgmt) {
		if (logger.isLoggable(Level.FINE))
			logger.log(Level.FINE, "Setting HardwareMgmt instance to {0}.", hwMgmt);
		StubHardwareManager.hwMgmt = hwMgmt;
	}

	public StubHardwareManager() {
		super();
		if (hwMgmt == null)
			throw new NullPointerException("HardwareManagement instance has not been set yet. You may want to call setHardwareMgmt first.");
		this.hwApi = new HardwareBackend(hwMgmt);

		if (logger.isLoggable(Level.FINE))
			logger.log(Level.FINE, "StubHardwareManager instanciated.");

	}

	@Override
	public State getState(short id) {
		RequestCreateIOType io = hwApi.getIO(id);
		return io != null ? hwApi.map(io.getState()) : State.UNKNOWN;
	}

	@Override
	public void setState(short id, State state) throws IllegalArgumentException {
		if (logger.isLoggable(Level.FINE))
			logger.log(Level.FINE, "Setting state of port {0} to {1}.", new Object[] { id, state });

		RequestCreateIOType io = hwApi.getIO(id);
		if (io != null) {
			io.setState(hwApi.map(state));
			hwApi.updateIO(io);
		}
	}

	@Override
	public Direction getDirection(short id) {
		RequestCreateIOType io = hwApi.getIO(id);
		return io != null ? hwApi.map(io.getDirection()) : null;
	}

	@Override
	public void setDirection(short id, Direction direction) {
		if (logger.isLoggable(Level.FINE))
			logger.log(Level.FINE, "Setting direction of port {0} to {1}.", new Object[] { id, direction });

		RequestCreateIOType io = hwApi.getIO(id);
		if (io != null) {
			io.setDirection(hwApi.map(direction));
		}
	}

	@Override
	public boolean getEnable(short id) throws IllegalArgumentException {
		return hwApi.getSubscriber(id).getIos().getIo().get(0).isStateChanged();
	}

	@Override
	public void setEnable(short id, boolean enable) throws IllegalArgumentException {

		if (logger.isLoggable(Level.FINE))
			logger.log(Level.FINE, "Setting enabled state of port {0} to {1}.", new Object[] { id, enable });

		hwApi.updateSubscriber(id, enable, new Subscriber());
	}

	@Override
	public short getCount() {
		return (short) hwApi.getIOs().getIo().size();
	}

	@Override
	public void setListener(StateListener listener) {
		if (logger.isLoggable(Level.FINE))
			logger.log(Level.FINE, "Setting listener to {0}.", listener);

		if (listener == null) {
			List<Short> subscrKeys = hwApi.getSubscriberKeys();
			for (Short key : subscrKeys)
				hwApi.deleteSubscriber(key);
		} else {
			RequestCreateIOsType ios = hwApi.getIOs();
			for (RequestCreateIOType io : ios.getIo())
				hwApi.createSubscriber((short) io.getIoId(), false, new Subscriber());
		}

		this.listener = listener;
	}

	private void fireStateChangedEvent(short id, State state) {
		if (listener == null) {
			if (logger.isLoggable(Level.FINE))
				logger.log(Level.FINE, "State of port {0} has changed to {1}, but not listener is registered.", new Object[] { id, state });
			return;
		}

		StateEvent se = new StateEvent();
		se.setState(state);
		se.setId(id);
		listener.stateChanged(se);

		if (logger.isLoggable(Level.FINE))
			logger.log(Level.FINE, "State of port {0} has changed to {1}, listener {2} notified.", new Object[] { id, state, listener });
	}

	public class Subscriber extends SubscriberInstanceType implements HardwareMgmtSubscriber {

		@Override
		public void notify(NotificationType notification) {
			for (NotificationIOType io : notification.getIos().getIo()) {
				IOStateEnumeration newState = io.getStateChanged().getNew();
				fireStateChangedEvent((short) io.getIoId(), hwApi.map(newState));
			}
		}
	}
}