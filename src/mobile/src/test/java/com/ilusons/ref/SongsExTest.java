package com.ilusons.ref;

import android.text.TextUtils;

import com.ilusons.harmony.ref.SongsEx;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;

@RunWith(PowerMockRunner.class)
@PrepareForTest(TextUtils.class)
public class SongsExTest {

	@Before
	public void setup() {
		PowerMockito.mockStatic(TextUtils.class);
		PowerMockito.when(TextUtils.isEmpty(any(CharSequence.class))).thenAnswer(new Answer<Boolean>() {
			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				CharSequence a = (CharSequence) invocation.getArguments()[0];
				return !(a != null && a.length() > 0);
			}
		});
	}

	@Test
	public void getArtistAndTitleTest() {
		List<String> result;

		result = SongsEx.getArtistAndTitle("AFTER FOREVER - Energize Me (OFFICIAL VIDEO) ( 316 X 352 )");
		assertTrue(result.size() == 2);
		assertTrue(result.get(0).equalsIgnoreCase("AFTER FOREVER"));
		assertTrue(result.get(1).equalsIgnoreCase("Energize Me"));

		result = SongsEx.getArtistAndTitle("Nemesea - In Control [In Control].m4a");
		assertTrue(result.size() == 2);
		assertTrue(result.get(0).equalsIgnoreCase("Nemesea"));
		assertTrue(result.get(1).equalsIgnoreCase("In Control"));

		result = SongsEx.getArtistAndTitle("NEMESEA - Dance In The Fire.mp4");
		assertTrue(result.size() == 2);
		assertTrue(result.get(0).equalsIgnoreCase("NEMESEA"));
		assertTrue(result.get(1).equalsIgnoreCase("Dance In The Fire"));

	}


}