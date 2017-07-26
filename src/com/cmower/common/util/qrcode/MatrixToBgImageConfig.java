package com.cmower.common.util.qrcode;

public class MatrixToBgImageConfig {
	public MatrixToBgImageConfig(int qrcode_height, int qrcode_x, int qrcode_y) {
		this();
		this.qrcode_height = qrcode_height;
		this.qrcode_x = qrcode_x;
		this.qrcode_y = qrcode_y;
	}

	public MatrixToBgImageConfig() {
	}

	// logo默认边框颜色
	public static final String DEFAULT_HEADIMGURL = "default_headimg.jpg";

	private int qrcode_height = 291;// 二维码的高度
	private int qrcode_x = 246;// 二维码的X
	private int qrcode_y = 226;// 二维码的y

	private String headimgUrl;// 头像
	private int headimg_height = 200;// 头像的高度
	private int headimg_x = 37;// 头像的X
	private int headimg_y = 30;// 头像的y

	private String bgFile = "bg.png";// 背景图片
	private String qrcode_url; // 二维码url
	private String realname;// 用户名
	private int realname_x = 339;// 名字的x
	private int realname_y = 140;// 名字的y

	public int getQrcode_height() {
		return qrcode_height;
	}

	public void setQrcode_height(int qrcode_height) {
		this.qrcode_height = qrcode_height;
	}

	public int getQrcode_x() {
		return qrcode_x;
	}

	public void setQrcode_x(int qrcode_x) {
		this.qrcode_x = qrcode_x;
	}

	public int getQrcode_y() {
		return qrcode_y;
	}

	public void setQrcode_y(int qrcode_y) {
		this.qrcode_y = qrcode_y;
	}

	public String getBgFile() {
		return bgFile;
	}

	public void setBgFile(String bgFile) {
		this.bgFile = bgFile;
	}

	public String getQrcode_url() {
		return qrcode_url;
	}

	public void setQrcode_url(String qrcode_url) {
		this.qrcode_url = qrcode_url;
	}

	public String getRealname() {
		return realname;
	}

	public void setRealname(String realname) {
		this.realname = realname;
	}

	public String getHeadimgUrl() {
		return headimgUrl;
	}

	public void setHeadimgUrl(String headimgUrl) {
		this.headimgUrl = headimgUrl;
	}

	public int getHeadimg_height() {
		return headimg_height;
	}

	public void setHeadimg_height(int headimg_height) {
		this.headimg_height = headimg_height;
	}

	public int getHeadimg_x() {
		return headimg_x;
	}

	public void setHeadimg_x(int headimg_x) {
		this.headimg_x = headimg_x;
	}

	public int getHeadimg_y() {
		return headimg_y;
	}

	public void setHeadimg_y(int headimg_y) {
		this.headimg_y = headimg_y;
	}

	public int getRealname_x() {
		return realname_x;
	}

	public void setRealname_x(int realname_x) {
		this.realname_x = realname_x;
	}

	public int getRealname_y() {
		return realname_y;
	}

	public void setRealname_y(int realname_y) {
		this.realname_y = realname_y;
	}

}
