package org.asf.razorwhip.sentinel.launcher.descriptors.xmls;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class PayloadEntryData {

	public String payloadID;

	public String payloadName;

	public String payloadVersion;

	public String payloadHashSha512;

	public String payloadURL;

}
