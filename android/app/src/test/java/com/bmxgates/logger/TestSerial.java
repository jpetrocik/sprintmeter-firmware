package com.bmxgates.logger;

import android.os.Handler;
import android.os.Message;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TestSerial {

	@Mock
	Handler handler;

	AbstractSprintActivity uut = new AbstractSprintActivity() {

		@Override
		protected boolean processSplit(Message msg) {
			return false;
		}

		@Override
		protected void connectionFailed() {

		}

		@Override
		protected void connectionLost() {

		}

		@Override
		protected void connectionRestored() {

		}
	};

	@Test
	public void testValidCheckSum() {
		Message msg = new Message();
		msg.arg1 = 234540;
		msg.arg2 = 3456;

		//inital message
		uut.validateChecksum(msg);
		Assert.assertFalse(uut.checkSumError);

		//next message
		msg.arg2 = 3457;
		uut.validateChecksum(msg);
		Assert.assertFalse(uut.checkSumError);

		//message skipped
		msg.arg2 = 3459;
		uut.validateChecksum(msg);
		Assert.assertTrue(uut.checkSumError);

		//continue
		msg.arg2 = 3460;
		uut.validateChecksum(msg);
		Assert.assertFalse(uut.checkSumError);

		//rollover
		msg.arg2 = 0;
		uut.validateChecksum(msg);
		Assert.assertFalse(uut.checkSumError);


	}

	@Test
	public void testReadMessage() {

		Message msg = new Message();

		Mockito.when(handler.obtainMessage()).thenReturn(msg);
		Mockito.when(handler.sendMessage(Mockito.any())).thenReturn(true);
		BMXSprintApplication uut = new BMXSprintApplication();
		uut.setSerialHandler(handler);

		int read = uut.readBluetoothMessage(10, new byte[] {'B','M','X', (byte)0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF});

		Assert.assertEquals(9, read);
		Assert.assertEquals(Integer.MAX_VALUE, msg.arg1);
		Assert.assertEquals(65535, msg.arg2);
	}
}
