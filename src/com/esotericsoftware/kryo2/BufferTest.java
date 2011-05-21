
package com.esotericsoftware.kryo2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.SerializationException;

public class BufferTest extends KryoTestCase {
	public void testWriteGrowBuffers () {
		WriteBuffer buffer = new WriteBuffer(4, 8, 16);
		buffer.writeBytes(new byte[] {11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26});
		buffer.writeBytes(new byte[] {31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46});
		buffer.writeBytes(new byte[] {51, 52, 53, 54, 55, 56, 57, 58});
		buffer.writeBytes(new byte[] {61, 62, 63, 64, 65});
		buffer.flush();

		assertEquals(buffer.toBytes(), new byte[] { //
			11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, //
				31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, //
				51, 52, 53, 54, 55, 56, 57, 58, //
				61, 62, 63, 64, 65});
	}

	public void testWritePopBuffers () {
		WriteBuffer buffer = new WriteBuffer(4, 8, 16);
		buffer.writeBytes(new byte[] {11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26});
		buffer.writeBytes(new byte[] {31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46});
		buffer.writeBytes(new byte[] {51, 52, 53, 54, 55, 56, 57, 58});
		buffer.writeBytes(new byte[] {61, 62, 63, 64});
		buffer.flush();

		byte[] expected = new byte[] { //
		11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, //
			31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, //
			51, 52, 53, 54, 55, 56, 57, 58, //
			61, 62, 63, 64};
		assertEquals(expected.length, buffer.toBytes().length);
		for (int i = 0; i < expected.length;) {
			byte[] bytes = buffer.popBytes();
			assertEquals(4, bytes.length);
			assertEquals(expected[i++], bytes[0]);
			assertEquals(expected[i++], bytes[1]);
			assertEquals(expected[i++], bytes[2]);
			assertEquals(expected[i++], bytes[3]);
		}
	}

	public void testWriteMarks () {
		runWriteMarksTest(new WriteBuffer(512));
		runWriteMarksTest(new WriteBuffer(2, 100));
		runWriteMarksTest(new WriteBuffer(3, 2, 100));
	}

	public void runWriteMarksTest (WriteBuffer buffer) {
		buffer.writeBytes(new byte[] {11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26});

		int start = buffer.mark();
		buffer.writeBytes(new byte[] {31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46});
		int end = buffer.mark();

		buffer.positionToMark(start);
		buffer.writeBytes(new byte[] {51, 52, 53, 54, 55, 56, 57, 58});
		buffer.positionToMark(end);

		buffer.writeBytes(new byte[] {61, 62, 63, 64, 65});
		buffer.flush();

		assertEquals(buffer.toBytes(), new byte[] { //
			11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, //
				51, 52, 53, 54, 55, 56, 57, 58, 39, 40, 41, 42, 43, 44, 45, 46, //
				61, 62, 63, 64, 65});
	}

	public void testWriteBytes () {
		WriteBuffer buffer = new WriteBuffer(512);
		buffer.writeBytes(new byte[] {11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26});
		buffer.writeBytes(new byte[] {31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46});
		buffer.writeByte(51);
		buffer.writeBytes(new byte[] {52, 53, 54, 55, 56, 57, 58});
		buffer.writeByte(61);
		buffer.writeByte(62);
		buffer.writeByte(63);
		buffer.writeByte(64);
		buffer.writeByte(65);
		buffer.flush();

		assertEquals(buffer.toBytes(), new byte[] { //
			11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, //
				31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, //
				51, 52, 53, 54, 55, 56, 57, 58, //
				61, 62, 63, 64, 65});
	}

	public void testReadMarks () throws IOException {
		runReadMarksTest(1024, 1, 1);
		runReadMarksTest(2, 1, -1);
		runReadMarksTest(3, 1, 40);
	}

	public void runReadMarksTest (int bufferSize, int coreBuffers, int maxBuffers) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(new byte[] {11, 22, 33, 44, 55, 66, 77, 88, 99});
		final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		ReadBuffer buffer = new ReadBuffer(bufferSize, coreBuffers, maxBuffers) {
			protected int input (byte[] buffer) {
				try {
					return in.read(buffer);
				} catch (IOException ex) {
					throw new SerializationException(ex);
				}
			}
		};

		ByteArrayOutputStream read = new ByteArrayOutputStream();
		read.write(buffer.readByte());
		read.write(buffer.readByte());
		read.write(buffer.readByte());
		int start = buffer.mark();
		read.write(buffer.readByte());
		read.write(buffer.readByte());
		read.write(buffer.readByte());
		int end = buffer.mark();
		buffer.positionToMark(start);
		read.write(buffer.readByte());
		read.write(buffer.readByte());
		buffer.positionToMark(end);
		read.write(buffer.readByte());
		read.write(buffer.readByte());
		read.write(buffer.readByte());

		assertEquals(read.toByteArray(), new byte[] { //
			11, 22, 33, 44, 55, 66, 44, 55, 77, 88, 99});
	}

	public void testStrings () {
		runStringTest(new WriteBuffer());
		runStringTest(new WriteBuffer(2, 200));
	}

	public void runStringTest (WriteBuffer write) {
		String value1 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ\rabcdefghijklmnopqrstuvwxyz\n1234567890\t\"!`?'.,;:()[]{}<>|/@\\^$-%+=#_&~*";
		String value2 = "abcdef\u00E1\u00E9\u00ED\u00F3\u00FA\u1234";

		write.writeString("uno");
		write.writeString("dos");
		write.writeString("tres");
		write.writeString(value1);
		write.writeString(value2);

		ReadBuffer read = new ReadBuffer(write.toBytes());
		assertEquals("uno", read.readString());
		assertEquals("dos", read.readString());
		assertEquals("tres", read.readString());
		assertEquals(value1, read.readString());
		assertEquals(value2, read.readString());
	}

	public void testInts () {
		runIntTest(new WriteBuffer());
		runIntTest(new WriteBuffer(2, 200));
	}

	private void runIntTest (WriteBuffer write) {
		write.writeInt(0);
		write.writeInt(63);
		write.writeInt(64);
		write.writeInt(127);
		write.writeInt(128);
		write.writeInt(8192);
		write.writeInt(16384);
		write.writeInt(2097151);
		write.writeInt(1048575);
		write.writeInt(134217727);
		write.writeInt(268435455);
		write.writeInt(134217728);
		write.writeInt(268435456);
		write.writeInt(-2097151);
		write.writeInt(-1048575);
		write.writeInt(-134217727);
		write.writeInt(-268435455);
		write.writeInt(-134217728);
		write.writeInt(-268435456);
		assertEquals(1, write.writeInt(0, true));
		assertEquals(1, write.writeInt(0, false));
		assertEquals(1, write.writeInt(63, true));
		assertEquals(1, write.writeInt(63, false));
		assertEquals(1, write.writeInt(64, true));
		assertEquals(2, write.writeInt(64, false));
		assertEquals(1, write.writeInt(127, true));
		assertEquals(2, write.writeInt(127, false));
		assertEquals(2, write.writeInt(128, true));
		assertEquals(2, write.writeInt(128, false));
		assertEquals(2, write.writeInt(8191, true));
		assertEquals(2, write.writeInt(8191, false));
		assertEquals(2, write.writeInt(8192, true));
		assertEquals(3, write.writeInt(8192, false));
		assertEquals(2, write.writeInt(16383, true));
		assertEquals(3, write.writeInt(16383, false));
		assertEquals(3, write.writeInt(16384, true));
		assertEquals(3, write.writeInt(16384, false));
		assertEquals(3, write.writeInt(2097151, true));
		assertEquals(4, write.writeInt(2097151, false));
		assertEquals(3, write.writeInt(1048575, true));
		assertEquals(3, write.writeInt(1048575, false));
		assertEquals(4, write.writeInt(134217727, true));
		assertEquals(4, write.writeInt(134217727, false));
		assertEquals(4, write.writeInt(268435455, true));
		assertEquals(5, write.writeInt(268435455, false));
		assertEquals(4, write.writeInt(134217728, true));
		assertEquals(5, write.writeInt(134217728, false));
		assertEquals(5, write.writeInt(268435456, true));
		assertEquals(5, write.writeInt(268435456, false));
		assertEquals(1, write.writeInt(-64, false));
		assertEquals(5, write.writeInt(-64, true));
		assertEquals(2, write.writeInt(-65, false));
		assertEquals(5, write.writeInt(-65, true));
		assertEquals(2, write.writeInt(-8192, false));
		assertEquals(5, write.writeInt(-8192, true));
		assertEquals(3, write.writeInt(-1048576, false));
		assertEquals(5, write.writeInt(-1048576, true));
		assertEquals(4, write.writeInt(-134217728, false));
		assertEquals(5, write.writeInt(-134217728, true));
		assertEquals(5, write.writeInt(-134217729, false));
		assertEquals(5, write.writeInt(-134217729, true));

		ReadBuffer read = new ReadBuffer(write.toBytes());
		assertEquals(0, read.readInt());
		assertEquals(63, read.readInt());
		assertEquals(64, read.readInt());
		assertEquals(127, read.readInt());
		assertEquals(128, read.readInt());
		assertEquals(8192, read.readInt());
		assertEquals(16384, read.readInt());
		assertEquals(2097151, read.readInt());
		assertEquals(1048575, read.readInt());
		assertEquals(134217727, read.readInt());
		assertEquals(268435455, read.readInt());
		assertEquals(134217728, read.readInt());
		assertEquals(268435456, read.readInt());
		assertEquals(-2097151, read.readInt());
		assertEquals(-1048575, read.readInt());
		assertEquals(-134217727, read.readInt());
		assertEquals(-268435455, read.readInt());
		assertEquals(-134217728, read.readInt());
		assertEquals(-268435456, read.readInt());
		assertEquals(true, read.canReadInt());
		assertEquals(true, read.canReadInt());
		assertEquals(true, read.canReadInt());
		assertEquals(0, read.readInt(true));
		assertEquals(0, read.readInt(false));
		assertEquals(63, read.readInt(true));
		assertEquals(63, read.readInt(false));
		assertEquals(64, read.readInt(true));
		assertEquals(64, read.readInt(false));
		assertEquals(127, read.readInt(true));
		assertEquals(127, read.readInt(false));
		assertEquals(128, read.readInt(true));
		assertEquals(128, read.readInt(false));
		assertEquals(8191, read.readInt(true));
		assertEquals(8191, read.readInt(false));
		assertEquals(8192, read.readInt(true));
		assertEquals(8192, read.readInt(false));
		assertEquals(16383, read.readInt(true));
		assertEquals(16383, read.readInt(false));
		assertEquals(16384, read.readInt(true));
		assertEquals(16384, read.readInt(false));
		assertEquals(2097151, read.readInt(true));
		assertEquals(2097151, read.readInt(false));
		assertEquals(1048575, read.readInt(true));
		assertEquals(1048575, read.readInt(false));
		assertEquals(134217727, read.readInt(true));
		assertEquals(134217727, read.readInt(false));
		assertEquals(268435455, read.readInt(true));
		assertEquals(268435455, read.readInt(false));
		assertEquals(134217728, read.readInt(true));
		assertEquals(134217728, read.readInt(false));
		assertEquals(268435456, read.readInt(true));
		assertEquals(268435456, read.readInt(false));
		assertEquals(-64, read.readInt(false));
		assertEquals(-64, read.readInt(true));
		assertEquals(-65, read.readInt(false));
		assertEquals(-65, read.readInt(true));
		assertEquals(-8192, read.readInt(false));
		assertEquals(-8192, read.readInt(true));
		assertEquals(-1048576, read.readInt(false));
		assertEquals(-1048576, read.readInt(true));
		assertEquals(-134217728, read.readInt(false));
		assertEquals(-134217728, read.readInt(true));
		assertEquals(-134217729, read.readInt(false));
		assertEquals(-134217729, read.readInt(true));
		assertEquals(false, read.canReadInt());
	}

	public void testLongs () {
		runLongTest(new WriteBuffer());
		runLongTest(new WriteBuffer(2, 200));
	}

	private void runLongTest (WriteBuffer write) {
		write.writeLong(0);
		write.writeLong(63);
		write.writeLong(64);
		write.writeLong(127);
		write.writeLong(128);
		write.writeLong(8192);
		write.writeLong(16384);
		write.writeLong(2097151);
		write.writeLong(1048575);
		write.writeLong(134217727);
		write.writeLong(268435455);
		write.writeLong(134217728);
		write.writeLong(268435456);
		write.writeLong(-2097151);
		write.writeLong(-1048575);
		write.writeLong(-134217727);
		write.writeLong(-268435455);
		write.writeLong(-134217728);
		write.writeLong(-268435456);
		assertEquals(1, write.writeLong(0, true));
		assertEquals(1, write.writeLong(0, false));
		assertEquals(1, write.writeLong(63, true));
		assertEquals(1, write.writeLong(63, false));
		assertEquals(1, write.writeLong(64, true));
		assertEquals(2, write.writeLong(64, false));
		assertEquals(1, write.writeLong(127, true));
		assertEquals(2, write.writeLong(127, false));
		assertEquals(2, write.writeLong(128, true));
		assertEquals(2, write.writeLong(128, false));
		assertEquals(2, write.writeLong(8191, true));
		assertEquals(2, write.writeLong(8191, false));
		assertEquals(2, write.writeLong(8192, true));
		assertEquals(3, write.writeLong(8192, false));
		assertEquals(2, write.writeLong(16383, true));
		assertEquals(3, write.writeLong(16383, false));
		assertEquals(3, write.writeLong(16384, true));
		assertEquals(3, write.writeLong(16384, false));
		assertEquals(3, write.writeLong(2097151, true));
		assertEquals(4, write.writeLong(2097151, false));
		assertEquals(3, write.writeLong(1048575, true));
		assertEquals(3, write.writeLong(1048575, false));
		assertEquals(4, write.writeLong(134217727, true));
		assertEquals(4, write.writeLong(134217727, false));
		assertEquals(4, write.writeLong(268435455l, true));
		assertEquals(5, write.writeLong(268435455l, false));
		assertEquals(4, write.writeLong(134217728l, true));
		assertEquals(5, write.writeLong(134217728l, false));
		assertEquals(5, write.writeLong(268435456l, true));
		assertEquals(5, write.writeLong(268435456l, false));
		assertEquals(1, write.writeLong(-64, false));
		assertEquals(10, write.writeLong(-64, true));
		assertEquals(2, write.writeLong(-65, false));
		assertEquals(10, write.writeLong(-65, true));
		assertEquals(2, write.writeLong(-8192, false));
		assertEquals(10, write.writeLong(-8192, true));
		assertEquals(3, write.writeLong(-1048576, false));
		assertEquals(10, write.writeLong(-1048576, true));
		assertEquals(4, write.writeLong(-134217728, false));
		assertEquals(10, write.writeLong(-134217728, true));
		assertEquals(5, write.writeLong(-134217729, false));
		assertEquals(10, write.writeLong(-134217729, true));

		ReadBuffer read = new ReadBuffer(write.toBytes());
		assertEquals(0, read.readLong());
		assertEquals(63, read.readLong());
		assertEquals(64, read.readLong());
		assertEquals(127, read.readLong());
		assertEquals(128, read.readLong());
		assertEquals(8192, read.readLong());
		assertEquals(16384, read.readLong());
		assertEquals(2097151, read.readLong());
		assertEquals(1048575, read.readLong());
		assertEquals(134217727, read.readLong());
		assertEquals(268435455, read.readLong());
		assertEquals(134217728, read.readLong());
		assertEquals(268435456, read.readLong());
		assertEquals(-2097151, read.readLong());
		assertEquals(-1048575, read.readLong());
		assertEquals(-134217727, read.readLong());
		assertEquals(-268435455, read.readLong());
		assertEquals(-134217728, read.readLong());
		assertEquals(-268435456, read.readLong());
		assertEquals(0, read.readLong(true));
		assertEquals(0, read.readLong(false));
		assertEquals(63, read.readLong(true));
		assertEquals(63, read.readLong(false));
		assertEquals(64, read.readLong(true));
		assertEquals(64, read.readLong(false));
		assertEquals(127, read.readLong(true));
		assertEquals(127, read.readLong(false));
		assertEquals(128, read.readLong(true));
		assertEquals(128, read.readLong(false));
		assertEquals(8191, read.readLong(true));
		assertEquals(8191, read.readLong(false));
		assertEquals(8192, read.readLong(true));
		assertEquals(8192, read.readLong(false));
		assertEquals(16383, read.readLong(true));
		assertEquals(16383, read.readLong(false));
		assertEquals(16384, read.readLong(true));
		assertEquals(16384, read.readLong(false));
		assertEquals(2097151, read.readLong(true));
		assertEquals(2097151, read.readLong(false));
		assertEquals(1048575, read.readLong(true));
		assertEquals(1048575, read.readLong(false));
		assertEquals(134217727, read.readLong(true));
		assertEquals(134217727, read.readLong(false));
		assertEquals(268435455, read.readLong(true));
		assertEquals(268435455, read.readLong(false));
		assertEquals(134217728, read.readLong(true));
		assertEquals(134217728, read.readLong(false));
		assertEquals(268435456, read.readLong(true));
		assertEquals(268435456, read.readLong(false));
		assertEquals(-64, read.readLong(false));
		assertEquals(-64, read.readLong(true));
		assertEquals(-65, read.readLong(false));
		assertEquals(-65, read.readLong(true));
		assertEquals(-8192, read.readLong(false));
		assertEquals(-8192, read.readLong(true));
		assertEquals(-1048576, read.readLong(false));
		assertEquals(-1048576, read.readLong(true));
		assertEquals(-134217728, read.readLong(false));
		assertEquals(-134217728, read.readLong(true));
		assertEquals(-134217729, read.readLong(false));
		assertEquals(-134217729, read.readLong(true));
	}

	public void testShorts () {
		runShortTest(new WriteBuffer());
		runShortTest(new WriteBuffer(2, 200));
	}

	private void runShortTest (WriteBuffer write) {
		write.writeShort(0);
		write.writeShort(63);
		write.writeShort(64);
		write.writeShort(127);
		write.writeShort(128);
		write.writeShort(8192);
		write.writeShort(16384);
		write.writeShort(32767);
		write.writeShort(-63);
		write.writeShort(-64);
		write.writeShort(-127);
		write.writeShort(-128);
		write.writeShort(-8192);
		write.writeShort(-16384);
		write.writeShort(-32768);
		assertEquals(1, write.writeShort(0, true));
		assertEquals(1, write.writeShort(0, false));
		assertEquals(1, write.writeShort(63, true));
		assertEquals(1, write.writeShort(63, false));
		assertEquals(1, write.writeShort(64, true));
		assertEquals(1, write.writeShort(64, false));
		assertEquals(1, write.writeShort(127, true));
		assertEquals(1, write.writeShort(127, false));
		assertEquals(1, write.writeShort(128, true));
		assertEquals(3, write.writeShort(128, false));
		assertEquals(3, write.writeShort(8191, true));
		assertEquals(3, write.writeShort(8191, false));
		assertEquals(3, write.writeShort(8192, true));
		assertEquals(3, write.writeShort(8192, false));
		assertEquals(3, write.writeShort(16383, true));
		assertEquals(3, write.writeShort(16383, false));
		assertEquals(3, write.writeShort(16384, true));
		assertEquals(3, write.writeShort(16384, false));
		assertEquals(3, write.writeShort(32767, true));
		assertEquals(3, write.writeShort(32767, false));
		assertEquals(1, write.writeShort(-64, false));
		assertEquals(3, write.writeShort(-64, true));
		assertEquals(1, write.writeShort(-65, false));
		assertEquals(3, write.writeShort(-65, true));
		assertEquals(3, write.writeShort(-8192, false));
		assertEquals(3, write.writeShort(-8192, true));

		ReadBuffer read = new ReadBuffer(write.toBytes());
		assertEquals(0, read.readShort());
		assertEquals(63, read.readShort());
		assertEquals(64, read.readShort());
		assertEquals(127, read.readShort());
		assertEquals(128, read.readShort());
		assertEquals(8192, read.readShort());
		assertEquals(16384, read.readShort());
		assertEquals(32767, read.readShort());
		assertEquals(-63, read.readShort());
		assertEquals(-64, read.readShort());
		assertEquals(-127, read.readShort());
		assertEquals(-128, read.readShort());
		assertEquals(-8192, read.readShort());
		assertEquals(-16384, read.readShort());
		assertEquals(-32768, read.readShort());
		assertEquals(0, read.readShort(true));
		assertEquals(0, read.readShort(false));
		assertEquals(63, read.readShort(true));
		assertEquals(63, read.readShort(false));
		assertEquals(64, read.readShort(true));
		assertEquals(64, read.readShort(false));
		assertEquals(127, read.readShort(true));
		assertEquals(127, read.readShort(false));
		assertEquals(128, read.readShort(true));
		assertEquals(128, read.readShort(false));
		assertEquals(8191, read.readShort(true));
		assertEquals(8191, read.readShort(false));
		assertEquals(8192, read.readShort(true));
		assertEquals(8192, read.readShort(false));
		assertEquals(16383, read.readShort(true));
		assertEquals(16383, read.readShort(false));
		assertEquals(16384, read.readShort(true));
		assertEquals(16384, read.readShort(false));
		assertEquals(32767, read.readShort(true));
		assertEquals(32767, read.readShort(false));
		assertEquals(-64, read.readShort(false));
		assertEquals(-64, read.readShort(true));
		assertEquals(-65, read.readShort(false));
		assertEquals(-65, read.readShort(true));
		assertEquals(-8192, read.readShort(false));
		assertEquals(-8192, read.readShort(true));
	}

	public void testFloats () {
		runFloatTest(new WriteBuffer());
		runFloatTest(new WriteBuffer(2, 200));
	}

	private void runFloatTest (WriteBuffer write) {
		write.writeFloat(0);
		write.writeFloat(63);
		write.writeFloat(64);
		write.writeFloat(127);
		write.writeFloat(128);
		write.writeFloat(8192);
		write.writeFloat(16384);
		write.writeFloat(32767);
		write.writeFloat(-63);
		write.writeFloat(-64);
		write.writeFloat(-127);
		write.writeFloat(-128);
		write.writeFloat(-8192);
		write.writeFloat(-16384);
		write.writeFloat(-32768);
		assertEquals(1, write.writeFloat(0, 1000, true));
		assertEquals(1, write.writeFloat(0, 1000, false));
		assertEquals(3, write.writeFloat(63, 1000, true));
		assertEquals(3, write.writeFloat(63, 1000, false));
		assertEquals(3, write.writeFloat(64, 1000, true));
		assertEquals(3, write.writeFloat(64, 1000, false));
		assertEquals(3, write.writeFloat(127, 1000, true));
		assertEquals(3, write.writeFloat(127, 1000, false));
		assertEquals(3, write.writeFloat(128, 1000, true));
		assertEquals(3, write.writeFloat(128, 1000, false));
		assertEquals(4, write.writeFloat(8191, 1000, true));
		assertEquals(4, write.writeFloat(8191, 1000, false));
		assertEquals(4, write.writeFloat(8192, 1000, true));
		assertEquals(4, write.writeFloat(8192, 1000, false));
		assertEquals(4, write.writeFloat(16383, 1000, true));
		assertEquals(4, write.writeFloat(16383, 1000, false));
		assertEquals(4, write.writeFloat(16384, 1000, true));
		assertEquals(4, write.writeFloat(16384, 1000, false));
		assertEquals(4, write.writeFloat(32767, 1000, true));
		assertEquals(4, write.writeFloat(32767, 1000, false));
		assertEquals(3, write.writeFloat(-64, 1000, false));
		assertEquals(5, write.writeFloat(-64, 1000, true));
		assertEquals(3, write.writeFloat(-65, 1000, false));
		assertEquals(5, write.writeFloat(-65, 1000, true));
		assertEquals(4, write.writeFloat(-8192, 1000, false));
		assertEquals(5, write.writeFloat(-8192, 1000, true));

		ReadBuffer read = new ReadBuffer(write.toBytes());
		assertEquals(0f, read.readFloat());
		assertEquals(63f, read.readFloat());
		assertEquals(64f, read.readFloat());
		assertEquals(127f, read.readFloat());
		assertEquals(128f, read.readFloat());
		assertEquals(8192f, read.readFloat());
		assertEquals(16384f, read.readFloat());
		assertEquals(32767f, read.readFloat());
		assertEquals(-63f, read.readFloat());
		assertEquals(-64f, read.readFloat());
		assertEquals(-127f, read.readFloat());
		assertEquals(-128f, read.readFloat());
		assertEquals(-8192f, read.readFloat());
		assertEquals(-16384f, read.readFloat());
		assertEquals(-32768f, read.readFloat());
		assertEquals(0f, read.readFloat(1000, true));
		assertEquals(0f, read.readFloat(1000, false));
		assertEquals(63f, read.readFloat(1000, true));
		assertEquals(63f, read.readFloat(1000, false));
		assertEquals(64f, read.readFloat(1000, true));
		assertEquals(64f, read.readFloat(1000, false));
		assertEquals(127f, read.readFloat(1000, true));
		assertEquals(127f, read.readFloat(1000, false));
		assertEquals(128f, read.readFloat(1000, true));
		assertEquals(128f, read.readFloat(1000, false));
		assertEquals(8191f, read.readFloat(1000, true));
		assertEquals(8191f, read.readFloat(1000, false));
		assertEquals(8192f, read.readFloat(1000, true));
		assertEquals(8192f, read.readFloat(1000, false));
		assertEquals(16383f, read.readFloat(1000, true));
		assertEquals(16383f, read.readFloat(1000, false));
		assertEquals(16384f, read.readFloat(1000, true));
		assertEquals(16384f, read.readFloat(1000, false));
		assertEquals(32767f, read.readFloat(1000, true));
		assertEquals(32767f, read.readFloat(1000, false));
		assertEquals(-64f, read.readFloat(1000, false));
		assertEquals(-64f, read.readFloat(1000, true));
		assertEquals(-65f, read.readFloat(1000, false));
		assertEquals(-65f, read.readFloat(1000, true));
		assertEquals(-8192f, read.readFloat(1000, false));
		assertEquals(-8192f, read.readFloat(1000, true));
	}

	public void testDoubles () {
		runDoubleTest(new WriteBuffer());
		runDoubleTest(new WriteBuffer(2, 200));
	}

	private void runDoubleTest (WriteBuffer write) {
		write.writeDouble(0);
		write.writeDouble(63);
		write.writeDouble(64);
		write.writeDouble(127);
		write.writeDouble(128);
		write.writeDouble(8192);
		write.writeDouble(16384);
		write.writeDouble(32767);
		write.writeDouble(-63);
		write.writeDouble(-64);
		write.writeDouble(-127);
		write.writeDouble(-128);
		write.writeDouble(-8192);
		write.writeDouble(-16384);
		write.writeDouble(-32768);
		assertEquals(1, write.writeDouble(0, 1000, true));
		assertEquals(1, write.writeDouble(0, 1000, false));
		assertEquals(3, write.writeDouble(63, 1000, true));
		assertEquals(3, write.writeDouble(63, 1000, false));
		assertEquals(3, write.writeDouble(64, 1000, true));
		assertEquals(3, write.writeDouble(64, 1000, false));
		assertEquals(3, write.writeDouble(127, 1000, true));
		assertEquals(3, write.writeDouble(127, 1000, false));
		assertEquals(3, write.writeDouble(128, 1000, true));
		assertEquals(3, write.writeDouble(128, 1000, false));
		assertEquals(4, write.writeDouble(8191, 1000, true));
		assertEquals(4, write.writeDouble(8191, 1000, false));
		assertEquals(4, write.writeDouble(8192, 1000, true));
		assertEquals(4, write.writeDouble(8192, 1000, false));
		assertEquals(4, write.writeDouble(16383, 1000, true));
		assertEquals(4, write.writeDouble(16383, 1000, false));
		assertEquals(4, write.writeDouble(16384, 1000, true));
		assertEquals(4, write.writeDouble(16384, 1000, false));
		assertEquals(4, write.writeDouble(32767, 1000, true));
		assertEquals(4, write.writeDouble(32767, 1000, false));
		assertEquals(3, write.writeDouble(-64, 1000, false));
		assertEquals(10, write.writeDouble(-64, 1000, true));
		assertEquals(3, write.writeDouble(-65, 1000, false));
		assertEquals(10, write.writeDouble(-65, 1000, true));
		assertEquals(4, write.writeDouble(-8192, 1000, false));
		assertEquals(10, write.writeDouble(-8192, 1000, true));

		ReadBuffer read = new ReadBuffer(write.toBytes());
		assertEquals(0d, read.readDouble());
		assertEquals(63d, read.readDouble());
		assertEquals(64d, read.readDouble());
		assertEquals(127d, read.readDouble());
		assertEquals(128d, read.readDouble());
		assertEquals(8192d, read.readDouble());
		assertEquals(16384d, read.readDouble());
		assertEquals(32767d, read.readDouble());
		assertEquals(-63d, read.readDouble());
		assertEquals(-64d, read.readDouble());
		assertEquals(-127d, read.readDouble());
		assertEquals(-128d, read.readDouble());
		assertEquals(-8192d, read.readDouble());
		assertEquals(-16384d, read.readDouble());
		assertEquals(-32768d, read.readDouble());
		assertEquals(0d, read.readDouble(1000, true));
		assertEquals(0d, read.readDouble(1000, false));
		assertEquals(63d, read.readDouble(1000, true));
		assertEquals(63d, read.readDouble(1000, false));
		assertEquals(64d, read.readDouble(1000, true));
		assertEquals(64d, read.readDouble(1000, false));
		assertEquals(127d, read.readDouble(1000, true));
		assertEquals(127d, read.readDouble(1000, false));
		assertEquals(128d, read.readDouble(1000, true));
		assertEquals(128d, read.readDouble(1000, false));
		assertEquals(8191d, read.readDouble(1000, true));
		assertEquals(8191d, read.readDouble(1000, false));
		assertEquals(8192d, read.readDouble(1000, true));
		assertEquals(8192d, read.readDouble(1000, false));
		assertEquals(16383d, read.readDouble(1000, true));
		assertEquals(16383d, read.readDouble(1000, false));
		assertEquals(16384d, read.readDouble(1000, true));
		assertEquals(16384d, read.readDouble(1000, false));
		assertEquals(32767d, read.readDouble(1000, true));
		assertEquals(32767d, read.readDouble(1000, false));
		assertEquals(-64d, read.readDouble(1000, false));
		assertEquals(-64d, read.readDouble(1000, true));
		assertEquals(-65d, read.readDouble(1000, false));
		assertEquals(-65d, read.readDouble(1000, true));
		assertEquals(-8192d, read.readDouble(1000, false));
		assertEquals(-8192d, read.readDouble(1000, true));
	}

	public void testBooleans () {
		runBooleanTest(new WriteBuffer());
		runBooleanTest(new WriteBuffer(2, 200));
	}

	private void runBooleanTest (WriteBuffer write) {
		for (int i = 0; i < 100; i++) {
			write.writeBoolean(true);
			write.writeBoolean(false);
		}

		ReadBuffer read = new ReadBuffer(write.toBytes());
		for (int i = 0; i < 100; i++) {
			assertEquals(true, read.readBoolean());
			assertEquals(false, read.readBoolean());
		}
	}

	public void testChars () {
		runCharTest(new WriteBuffer());
		runCharTest(new WriteBuffer(2, 200));
	}

	private void runCharTest (WriteBuffer write) {
		write.writeChar((char)0);
		write.writeChar((char)63);
		write.writeChar((char)64);
		write.writeChar((char)127);
		write.writeChar((char)128);
		write.writeChar((char)8192);
		write.writeChar((char)16384);
		write.writeChar((char)32767);
		write.writeChar((char)65535);

		ReadBuffer read = new ReadBuffer(write.toBytes());
		assertEquals(0, read.readChar());
		assertEquals(63, read.readChar());
		assertEquals(64, read.readChar());
		assertEquals(127, read.readChar());
		assertEquals(128, read.readChar());
		assertEquals(8192, read.readChar());
		assertEquals(16384, read.readChar());
		assertEquals(32767, read.readChar());
		assertEquals(65535, read.readChar());
	}
}
