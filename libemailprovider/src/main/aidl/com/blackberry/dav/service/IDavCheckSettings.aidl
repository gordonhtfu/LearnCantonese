package com.blackberry.dav.service;

interface IDavCheckSettings {
	boolean hasCalendar(String host, String username, String password);
	boolean hasContacts(String host, String username, String password);

}