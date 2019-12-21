package com.bmxgates.logger;

import org.junit.Assert;
import org.junit.Test;

public class TestMath {

	@Test
	public void testSmoothing() {
		SprintGraphFragment uut = new SprintGraphFragment();

		double value = uut.smooth(1);
		Assert.assertEquals(1, value, 0);

		value = uut.smooth(2);
		Assert.assertEquals(1.5, value, 0);

		value = uut.smooth(3);
		Assert.assertEquals(2, value, 0);

		value = uut.smooth(4);
		Assert.assertEquals(2.5, value, 0);

		value = uut.smooth(5);
		Assert.assertEquals(3, value, 0);

		value = uut.smooth(6);
		Assert.assertEquals(3.5, value, 0);

		value = uut.smooth(7);
		Assert.assertEquals(4.5, value, 0);

		value = uut.smooth(8);
		Assert.assertEquals(5.5, value, 0);

		value = uut.smooth(9);
		Assert.assertEquals(6.5, value, 0);
	}
}
