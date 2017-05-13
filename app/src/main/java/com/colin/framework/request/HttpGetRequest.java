package com.colin.framework.request;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;

/**
 * 网络请求封装类
 * 
 * @author xionghoumiao
 * @date 2014-8-12上午9:13:37
 */
public class HttpGetRequest extends HttpRequest {


	public HttpGetRequest(String url) {
		super(url);
	}

	@Override
	public String getCacheKey() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			baos.write(getRequestUrl().getBytes());
			MessageDigest digest = MessageDigest.getInstance("md5");
			byte[] result = digest.digest(baos.toByteArray());
			
			StringBuffer buffer = new StringBuffer();
			for (byte b : result) {
				// 每个byte做与运算
				int number = b & 0xff;// 不按标准加密，密码学：加盐
				// 转换成16进制
				String numberStr = Integer.toHexString(number);
				if (numberStr.length() == 1) {
					buffer.append("0");
				}
				buffer.append(numberStr);
			}
			// 就标准的md5加密的结果
			String cacheKey = buffer.toString();
			Log.d("HttpRequest", "cacheKey " + cacheKey);
			return cacheKey;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				baos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return "";
	}

}
