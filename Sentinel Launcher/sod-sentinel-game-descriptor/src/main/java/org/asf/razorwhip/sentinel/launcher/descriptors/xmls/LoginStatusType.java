package org.asf.razorwhip.sentinel.launcher.descriptors.xmls;

public enum LoginStatusType
{
	Success,
	InvalidUserName,
	InvalidPassword,
	InvalidEmail,
	
	UsernameRestriction,
	UsernameContainsBadWord,
	NoChildData,
	
	GuestAccountNotFound,
	InvalidGuestChildUserName,
	
	InvalidChildUserName,

	UserIsBanned,
	IPAddressBlocked,

	DuplicateUserName,
	DuplicateEmail,

	UserPolicyNotAccepted
}
