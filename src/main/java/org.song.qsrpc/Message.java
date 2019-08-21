package org.song.qsrpc;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 *
 * @author song
 *
 * @Email vipqinsong@gmail.com
 *
 * @date 2019年3月1日 下午12:30:10
 *
 *       rpc消息
 *
 */
public class Message {

	private static int ID;

	private int id;

	private byte type;// 内容类型,预留(比如http消息,远程调用方法消息)

	private byte[] content;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public byte getType() {
		return type;
	}

	public void setType(byte type) {
		this.type = type;
	}

	public byte[] getContent() {
		return content;
	}

	public void setContent(byte[] content) {
		this.content = content;
	}

	public String getString() {
		if (content == null)
			return null;
		return new String(content);
	}

	public void setString(String s) {
		if (s != null)
			this.content = s.getBytes();
	}

	public JSONObject getJSONObject() {
		if (content == null)
			return null;
		return JSON.parseObject(getString());
	}

	public void setJSONObject(JSONObject json) {
		if (json != null)
			setString(json.toJSONString());
	}

	public static synchronized int createID() {
		return ++ID;
	}
}
