package org.cmxyzx.push.util;

public class TextUtil {
	
	public static boolean checkString(String str){
		if(str!=null && str.length()>0){
			return true;
		}
		return false;
	}

	public static boolean checkUUID(String str){
		if(checkString(str) && str.length() == 36){
			return true;
		}
		return false;
	}

}
