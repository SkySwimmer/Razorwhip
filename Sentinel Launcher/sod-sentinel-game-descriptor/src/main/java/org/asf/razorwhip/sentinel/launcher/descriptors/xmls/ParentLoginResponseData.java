package org.asf.razorwhip.sentinel.launcher.descriptors.xmls;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class ParentLoginResponseData extends CommonLoginInfo {

	@JsonProperty("LoginStatus")
	public LoginStatusType status;

	@JacksonXmlElementWrapper(useWrapping = false)
	public CommonLoginInfo[] childList;

	public boolean sendActivationReminder;

}
