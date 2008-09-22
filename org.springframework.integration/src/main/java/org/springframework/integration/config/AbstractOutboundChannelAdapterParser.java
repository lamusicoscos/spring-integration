/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.endpoint.OutboundChannelAdapter;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Base class for outbound Channel Adapter parsers.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractOutboundChannelAdapterParser extends AbstractChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {
		Element pollerElement = DomUtils.getChildElementByTagName(element, "poller");
		BeanDefinitionBuilder adapterBuilder = null;
		adapterBuilder =  BeanDefinitionBuilder.genericBeanDefinition(OutboundChannelAdapter.class);
		adapterBuilder.addConstructorArgReference(this.parseConsumer(element, parserContext));
		if (pollerElement != null) {
			if (!StringUtils.hasText(channelName)) {
				throw new ConfigurationException("outbound channel adapter with a 'poller' requires a 'channel' to poll");
			}
			IntegrationNamespaceUtils.configureSchedule(pollerElement, adapterBuilder);
			Element txElement = DomUtils.getChildElementByTagName(pollerElement, "transactional");
			if (txElement != null) {
				IntegrationNamespaceUtils.configureTransactionAttributes(txElement, adapterBuilder);
			}
		}
		adapterBuilder.addPropertyReference("inputChannel", channelName);
		return adapterBuilder.getBeanDefinition();
	}

	/**
	 * Subclasses must implement this method and return the bean name of the MessageConsumer.
	 */
	protected abstract String parseConsumer(Element element, ParserContext parserContext);

}
