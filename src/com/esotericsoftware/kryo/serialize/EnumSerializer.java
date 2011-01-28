
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.minlog.Log.*;

import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.SerializationException;
import com.esotericsoftware.kryo.Serializer;

/**
 * Writes a 1-3 byte enum.
 * @author Nathan Sweet <misc@n4te.com>
 */
public class EnumSerializer extends Serializer {
	private Object[] enumConstants;

	public EnumSerializer (Class<? extends Enum> type) {
		enumConstants = type.getEnumConstants();
		if (enumConstants == null) throw new IllegalArgumentException("The type must be an enum: " + type);
	}

	public <T> T readObjectData (ByteBuffer buffer, Class<T> type) {
		int ordinal = IntSerializer.get(buffer, true);
		if (ordinal < 0 || ordinal > enumConstants.length - 1)
			throw new SerializationException("Invalid ordinal for enum \"" + type.getName() + "\": " + ordinal);
		Object constant = enumConstants[ordinal];
		if (TRACE) trace("kryo", "Read enum: " + constant);
		return (T)constant;
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		put(buffer, (Enum)object);
		if (TRACE) trace("kryo", "Wrote enum: " + object);
	}

	static public void put (ByteBuffer buffer, Enum object) {
		IntSerializer.put(buffer, object.ordinal(), true);
	}

	static public <T> T get (ByteBuffer buffer, Class<T> type) {
		T[] enumConstants = type.getEnumConstants();
		if (enumConstants == null) throw new SerializationException("Class is not an enum: " + type.getName());
		int ordinal = IntSerializer.get(buffer, true);
		if (ordinal < 0 || ordinal > enumConstants.length - 1)
			throw new SerializationException("Invalid ordinal for enum \"" + type.getName() + "\": " + ordinal);
		return enumConstants[ordinal];
	}
}
