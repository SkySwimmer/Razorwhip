package org.asf.razorwhip.sentinel.launcher.descriptors.xmls;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class ParentLoginData {

	@JsonProperty("UserName")
	public String username;

	public String password;

	public String locale = "en-US";

	public String age;

	public UUID childUserID;

	public UserPolicyBlock userPolicy;

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class UserPolicyBlock {

		public boolean termsAndConditions;

		public boolean privacyPolicy;

	}

}
