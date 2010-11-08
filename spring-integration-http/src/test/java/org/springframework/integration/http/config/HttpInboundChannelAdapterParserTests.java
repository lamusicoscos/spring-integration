/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.http.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.integration.Message;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.http.MockHttpServletRequest;
import org.springframework.integration.http.inbound.HttpRequestHandlingController;
import org.springframework.integration.http.inbound.HttpRequestHandlingMessagingGateway;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.MultiValueMap;

/**
 * @author Mark Fisher
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class HttpInboundChannelAdapterParserTests {

	@Autowired
	private PollableChannel requests;

	@Autowired
	private HttpRequestHandlingMessagingGateway defaultAdapter;

	@Autowired
	private HttpRequestHandlingMessagingGateway postOnlyAdapter;

	@Autowired
	private HttpRequestHandlingMessagingGateway putOrDeleteAdapter;

	@Autowired
	private HttpRequestHandlingController inboundController;


	@Test
	@SuppressWarnings("unchecked")
	public void getRequestOk() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setParameter("foo", "bar");
		MockHttpServletResponse response = new MockHttpServletResponse();
		defaultAdapter.handleRequest(request, response);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		Message<?> message = requests.receive(0);
		assertNotNull(message);
		Object payload = message.getPayload();
		assertTrue(payload instanceof MultiValueMap);
		MultiValueMap<String, String> map = (MultiValueMap<String, String>) payload;
		assertEquals(1, map.size());
		assertEquals("foo", map.keySet().iterator().next());
		assertEquals(1, map.get("foo").size());
		assertEquals("bar", map.getFirst("foo"));
	}

	@Test
	public void getRequestNotAllowed() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setParameter("foo", "bar");
		MockHttpServletResponse response = new MockHttpServletResponse();
		postOnlyAdapter.handleRequest(request, response);
		assertEquals(HttpServletResponse.SC_METHOD_NOT_ALLOWED, response.getStatus());
		Message<?> message = requests.receive(0);
		assertNull(message);
	}

	@Test
	public void postRequestWithTextContentOk() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContent("test".getBytes());
		request.setContentType("text/plain");
		MockHttpServletResponse response = new MockHttpServletResponse();
		postOnlyAdapter.handleRequest(request, response);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		Message<?> message = requests.receive(0);
		MessageHistory history = MessageHistory.read(message);
		assertNotNull(history);
		Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "postOnlyAdapter", 0);
		assertNotNull(componentHistoryRecord);
		assertEquals("http:inbound-channel-adapter", componentHistoryRecord.get("type"));
		assertNotNull(message);
		assertEquals("test", message.getPayload());
	}

	@Test
	public void postRequestWithSerializedObjectContentOk() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		Object obj = new TestObject("testObject");
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		new ObjectOutputStream(byteStream).writeObject(obj);
		request.setContent(byteStream.toByteArray());
		request.setContentType("application/x-java-serialized-object");
		MockHttpServletResponse response = new MockHttpServletResponse();
		postOnlyAdapter.handleRequest(request, response);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		Message<?> message = requests.receive(0);
		assertNotNull(message);
		assertTrue(message.getPayload() instanceof TestObject);
		assertEquals("testObject", ((TestObject) message.getPayload()).text);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void putOrDeleteMethodsSupported() throws Exception {
		DirectFieldAccessor accessor = new DirectFieldAccessor(putOrDeleteAdapter);
		List<String> supportedMethods = (List<String>) accessor.getPropertyValue("supportedMethods");
		assertEquals(2, supportedMethods.size());
		assertTrue(supportedMethods.contains(HttpMethod.PUT));
		assertTrue(supportedMethods.contains(HttpMethod.DELETE));
	}

	@Test
	public void testController() throws Exception {
		DirectFieldAccessor accessor = new DirectFieldAccessor(inboundController);
		String errorCode =  (String) accessor.getPropertyValue("errorCode");
		assertEquals("oops", errorCode);
	}


	@SuppressWarnings("serial")
	private static class TestObject implements Serializable {

		String text;

		TestObject(String text) {
			this.text = text;
		}
	}

}
