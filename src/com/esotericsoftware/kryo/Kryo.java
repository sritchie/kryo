
package com.esotericsoftware.kryo;

import static com.esotericsoftware.log.Log.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.esotericsoftware.kryo.serialize.ArraySerializer;
import com.esotericsoftware.kryo.serialize.BooleanSerializer;
import com.esotericsoftware.kryo.serialize.ByteSerializer;
import com.esotericsoftware.kryo.serialize.CharSerializer;
import com.esotericsoftware.kryo.serialize.CollectionSerializer;
import com.esotericsoftware.kryo.serialize.CustomSerializer;
import com.esotericsoftware.kryo.serialize.DoubleSerializer;
import com.esotericsoftware.kryo.serialize.EnumSerializer;
import com.esotericsoftware.kryo.serialize.FieldSerializer;
import com.esotericsoftware.kryo.serialize.FloatSerializer;
import com.esotericsoftware.kryo.serialize.IntSerializer;
import com.esotericsoftware.kryo.serialize.LongSerializer;
import com.esotericsoftware.kryo.serialize.MapSerializer;
import com.esotericsoftware.kryo.serialize.ShortSerializer;
import com.esotericsoftware.kryo.serialize.StringSerializer;
import com.esotericsoftware.kryo.util.ShortHashMap;

/**
 * Maps classes to serializers so object graphs can be serialized automatically.
 * @author Nathan Sweet <misc@n4te.com>
 */
public class Kryo {
	static private final byte ID_NULL_OBJECT = 0;

	private final ShortHashMap<RegisteredClass> idToRegisteredClass = new ShortHashMap(64);
	private final HashMap<Class, RegisteredClass> classToRegisteredClass = new HashMap(64);
	private short nextClassID = 1;
	private Object listenerLock = new Object();
	private KryoListener[] listeners = {};

	private final CustomSerializer customSerializer = new CustomSerializer();
	private final FieldSerializer fieldSerializer = new FieldSerializer(this);
	private final ArraySerializer arraySerializer = new ArraySerializer(this);
	private final EnumSerializer enumSerializer = new EnumSerializer();
	private final CollectionSerializer collectionSerializer = new CollectionSerializer(this);
	private final MapSerializer mapSerializer = new MapSerializer(this);

	private ThreadLocal<Context> contextThreadLocal = new ThreadLocal<Context>() {
		protected Context initialValue () {
			return new Context();
		}
	};

	public Kryo () {
		// Primitives.
		register(boolean.class, nextClassID++, new BooleanSerializer()).setCanBeNull(false);
		register(byte.class, nextClassID++, new ByteSerializer()).setCanBeNull(false);
		register(char.class, nextClassID++, new CharSerializer()).setCanBeNull(false);
		register(short.class, nextClassID++, new ShortSerializer()).setCanBeNull(false);
		register(int.class, nextClassID++, new IntSerializer()).setCanBeNull(false);
		register(long.class, nextClassID++, new LongSerializer()).setCanBeNull(false);
		register(float.class, nextClassID++, new FloatSerializer()).setCanBeNull(false);
		register(double.class, nextClassID++, new DoubleSerializer()).setCanBeNull(false);
		// Primitive wrappers.
		register(Boolean.class, nextClassID++, new BooleanSerializer());
		register(Byte.class, nextClassID++, new ByteSerializer());
		register(Character.class, nextClassID++, new CharSerializer());
		register(Short.class, nextClassID++, new ShortSerializer());
		register(Integer.class, nextClassID++, new IntSerializer());
		register(Long.class, nextClassID++, new LongSerializer());
		register(Float.class, nextClassID++, new FloatSerializer());
		register(Double.class, nextClassID++, new DoubleSerializer());
		// Other.
		register(String.class, nextClassID++, new StringSerializer());
	}

	private Serializer register (Class type, short id, Serializer serializer) {
		RegisteredClass registeredClass = new RegisteredClass(type, id, serializer);
		idToRegisteredClass.put(id, registeredClass);
		classToRegisteredClass.put(type, registeredClass);
		return serializer;
	}

	/**
	 * Registers a class to be serialized. The exact same classes and serializers must be registered in exactly the same order when
	 * the class is deserialized.
	 * <p>
	 * By default primitive types, primitive wrappers, and java.lang.String are registered. To transfer ANY other classes over the
	 * network, those classes must be registered. Note that even JDK classes like ArrayList, HashMap, etc must be registered. Also,
	 * array classes such as "int[].class" or "short[][].class" must be registered.
	 * <p>
	 * The {@link Serializer} specified will be used to serialize and deserialize objects of the specified type. Note that a
	 * serializer can be wrapped with a {@link Compressor}.
	 * <p>
	 * A serializer may not be registered with more than one Kryo instance.
	 * @see #register(Class)
	 * @see Serializer
	 * @see Compressor
	 */
	public void register (Class type, Serializer serializer) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		if (serializer == null) throw new IllegalArgumentException("serializer cannot be null.");
		if (serializer.kryo != null && serializer.kryo != this)
			throw new IllegalArgumentException("serializer must not be registered with another Kryo instance.");
		serializer.kryo = this;
		serializer.context = null;
		short id;
		RegisteredClass existingRegisteredClass = classToRegisteredClass.get(type);
		if (existingRegisteredClass != null)
			id = existingRegisteredClass.id;
		else
			id = nextClassID++;
		register(type, id, serializer);
		if (level <= TRACE) {
			String name = type.getName();
			if (type.isArray()) {
				Class elementClass = ArraySerializer.getElementClass(type);
				StringBuilder buffer = new StringBuilder(16);
				for (int i = 0, n = ArraySerializer.getDimensionCount(type); i < n; i++)
					buffer.append("[]");
				name = elementClass.getName() + buffer;
			}
			trace("kryo", "Registered class " + (nextClassID - 1) + ": " + name + " (" + serializer.getClass().getName() + ")");
		}
	}

	/**
	 * Registers the class, automatically determining the serializer to use. A serializer will be chosen according to this table:
	 * <p>
	 * <table>
	 * <tr>
	 * <th>Type</th>
	 * <th>Serializer</th>
	 * </tr>
	 * <tr>
	 * <td>array (any number of dimensions)</td>
	 * <td>{@link ArraySerializer}</td>
	 * </tr>
	 * <tr>
	 * <td>{@link Enum}</td>
	 * <td>{@link EnumSerializer}</td>
	 * </tr>
	 * <tr>
	 * <td>{@link Collection}</td>
	 * <td>{@link CollectionSerializer}</td>
	 * </tr>
	 * <tr>
	 * <td>{@link Map}</td>
	 * <td>{@link MapSerializer}</td>
	 * </tr>
	 * <tr>
	 * <td>{@link CustomSerialization}</td>
	 * <td>{@link CustomSerializer}</td>
	 * </tr>
	 * <tr>
	 * <td>any other class</td>
	 * <td>{@link FieldSerializer}</td>
	 * </tr>
	 * </table>
	 * <p>
	 * Note that some serializers allow additional information to be specified to make serialization more efficient in some cases
	 * (eg, {@link FieldSerializer#getField(Class, String)}).
	 * @see #register(Class, Serializer)
	 */
	public void register (Class type) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		Serializer serializer;
		if (type.isArray())
			serializer = arraySerializer;
		else if (CustomSerialization.class.isAssignableFrom(type))
			serializer = customSerializer;
		else if (Collection.class.isAssignableFrom(type))
			serializer = collectionSerializer;
		else if (Map.class.isAssignableFrom(type))
			serializer = mapSerializer;
		else if (Enum.class.isAssignableFrom(type))
			serializer = enumSerializer;
		else {
			serializer = fieldSerializer;
		}
		register(type, serializer);
	}

	public RegisteredClass getRegisteredClass (Class type) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		RegisteredClass registeredClass = classToRegisteredClass.get(type);
		if (registeredClass == null) {
			// If a Proxy class, treat it like an InvocationHandler because the concrete class for a proxy is generated.
			if (Proxy.isProxyClass(type)) return getRegisteredClass(InvocationHandler.class);
			if (type.isArray()) {
				Class elementClass = ArraySerializer.getElementClass(type);
				StringBuilder buffer = new StringBuilder(16);
				for (int i = 0, n = ArraySerializer.getDimensionCount(type); i < n; i++)
					buffer.append("[]");
				throw new IllegalArgumentException("Class is not registered: " + type.getName()
					+ "\nTo register this class use: Ninja.register(" + elementClass.getName() + buffer + ".class);");
			}
			throw new IllegalArgumentException("Class is not registered: " + type.getName());
		}
		return registeredClass;
	}

	public RegisteredClass getRegisteredClass (short classID) {
		RegisteredClass registeredClass = idToRegisteredClass.get(classID);
		if (registeredClass == null) throw new IllegalArgumentException("Class ID is not registered: " + classID);
		return registeredClass;
	}

	/**
	 * Writes the ID of the specified class to the buffer.
	 * @param type Can be null (writes a special class ID for a null object).
	 * @return The registered information for the class that was written.
	 */
	public RegisteredClass writeClass (ByteBuffer buffer, Class type) {
		RegisteredClass registeredClass = getRegisteredClass(type);
		ShortSerializer.put(buffer, registeredClass.id, true);
		if (level <= TRACE) trace("kryo", "Wrote class " + registeredClass.id + ": " + type.getName());
		return registeredClass;
	}

	/**
	 * Reads the class ID from the buffer.
	 * @return The registered information for the class that was read, or null if the data read from the buffer represented a null
	 *         object.
	 */
	public RegisteredClass readClass (ByteBuffer buffer) {
		short classID = ShortSerializer.get(buffer, true);
		if (classID == ID_NULL_OBJECT) {
			if (level <= TRACE) trace("kryo", "Read object: null");
			return null;
		}
		RegisteredClass registeredClass = idToRegisteredClass.get(classID);
		if (registeredClass == null) throw new SerializationException("Encountered unregistered class ID: " + classID);
		if (level <= TRACE) trace("kryo", "Read class " + classID + ": " + registeredClass.type.getName());
		return registeredClass;
	}

	/**
	 * Writes the object's class ID to the buffer, then uses the serializer registered for that class to write the object to the
	 * buffer.
	 * @param object Can be null (writes a special class ID for a null object instead).
	 */
	public void writeClassAndObject (ByteBuffer writeBuffer, Object object) {
		if (object == null) {
			writeBuffer.put(ID_NULL_OBJECT);
			if (level <= TRACE) trace("kryo", "Wrote object: null");
			return;
		}
		try {
			RegisteredClass registeredClass = writeClass(writeBuffer, object.getClass());
			registeredClass.serializer.writeObjectData(writeBuffer, object);
		} catch (SerializationException ex) {
			throw new SerializationException("Unable to serialize object of type: " + object.getClass().getName(), ex);
		}
	}

	/**
	 * Uses the serializer registered for the object's class to write the object to the buffer.
	 * @param object Can be null (writes a special class ID for a null object instead).
	 */
	public void writeObject (ByteBuffer writeBuffer, Object object) {
		if (object == null) {
			writeBuffer.put(ID_NULL_OBJECT);
			if (level <= TRACE) trace("kryo", "Wrote object: null");
			return;
		}
		try {
			getRegisteredClass(object.getClass()).serializer.writeObjectData(writeBuffer, object);
		} catch (SerializationException ex) {
			throw new SerializationException("Unable to serialize object of type: " + object.getClass().getName(), ex);
		}
	}

	/**
	 * Uses the serializer registered for the object's class to write the object to the buffer.
	 * @param object Cannot be null.
	 */
	public void writeObjectData (ByteBuffer writeBuffer, Object object) {
		try {
			getRegisteredClass(object.getClass()).serializer.writeObjectData(writeBuffer, object);
		} catch (SerializationException ex) {
			throw new SerializationException("Unable to serialize object of type: " + object.getClass().getName(), ex);
		}
	}

	/**
	 * Reads a class ID from the buffer and uses the serializer registered for that class to read an object from the buffer.
	 * @return The deserialized object, or null if the object read from the buffer was null.
	 */
	public Object readClassAndObject (ByteBuffer readBuffer) {
		RegisteredClass registeredClass = null;
		try {
			registeredClass = readClass(readBuffer);
			if (registeredClass == null) return null;
			return registeredClass.serializer.readObjectData(readBuffer, registeredClass.type);
		} catch (SerializationException ex) {
			if (registeredClass != null)
				throw new SerializationException("Unable to deserialize object of type: " + registeredClass.type.getName(), ex);
			throw new SerializationException("Unable to deserialize an object.", ex);
		}
	}

	/**
	 * Uses the serializer registered for the specified class to read an object from the buffer.
	 * @return The deserialized object, or null if the object read from the buffer was null.
	 */
	public <T> T readObject (ByteBuffer readBuffer, Class<T> type) {
		try {
			return getRegisteredClass(type).serializer.readObject(readBuffer, type);
		} catch (SerializationException ex) {
			throw new SerializationException("Unable to deserialize object of type: " + type.getName(), ex);
		}
	}

	/**
	 * Uses the serializer registered for the specified class to read an object from the buffer.
	 * @return The deserialized object, never null.
	 */
	public <T> T readObjectData (ByteBuffer readBuffer, Class<T> type) {
		try {
			return getRegisteredClass(type).serializer.readObjectData(readBuffer, type);
		} catch (SerializationException ex) {
			throw new SerializationException("Unable to deserialize object of type: " + type.getName(), ex);
		}
	}

	/**
	 * Returns the thread local context for serialization and deserialization.
	 * @see Context
	 */
	public Context getContext () {
		return contextThreadLocal.get();
	}

	public void addListener (KryoListener listener) {
		if (listener == null) throw new IllegalArgumentException("listener cannot be null.");
		synchronized (listenerLock) {
			KryoListener[] listeners = this.listeners;
			int n = listeners.length;
			for (int i = 0; i < n; i++)
				if (listener == listeners[i]) return;
			KryoListener[] newListeners = new KryoListener[n];
			newListeners[0] = listener;
			System.arraycopy(listeners, 0, newListeners, 1, n);
			this.listeners = newListeners;
		}
		if (level <= TRACE) trace("Kryo listener added: " + listener.getClass().getName());
	}

	public void removeListener (KryoListener listener) {
		if (listener == null) throw new IllegalArgumentException("listener cannot be null.");
		synchronized (listenerLock) {
			KryoListener[] listeners = this.listeners;
			int n = listeners.length;
			KryoListener[] newListeners = new KryoListener[n - 1];
			for (int i = 0, ii = 0; i < n; i++) {
				if (listener == listeners[i]) continue;
				if (ii == n - 1) return;
				newListeners[ii++] = listener;
			}
			System.arraycopy(listeners, 0, newListeners, 1, n);
			this.listeners = newListeners;
		}
		if (level <= TRACE) trace("Kryo listener removed: " + listener.getClass().getName());
	}

	/**
	 * Notifies all listeners that the remote entity with the specified ID will no longer be available.
	 * @see Context#getRemoteEntityID()
	 * @see #addListener(KryoListener)
	 */
	public void removeRemoteEntity (int remoteEntityID) {
		KryoListener[] listeners = this.listeners;
		if (level <= TRACE) trace("Remote ID removed: " + remoteEntityID);
		for (int i = 0, n = listeners.length; i < n; i++)
			listeners[i].remoteEntityRemoved(remoteEntityID);
	}

	/**
	 * Holds the registration information for a registered class.
	 */
	static public class RegisteredClass {
		public final Class type;
		public final short id;
		public Serializer serializer;

		RegisteredClass (Class type, short id, Serializer serializer) {
			this.type = type;
			this.id = id;
			this.serializer = serializer;
		}
	}
}
