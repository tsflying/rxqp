package com.ddz.common.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.ddz.common.constants.CommonConstants;

public class CommonUtils {

	public static String getLocalIp() {
		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return CommonConstants.DEFAULT_IP;
		}
	}
}
