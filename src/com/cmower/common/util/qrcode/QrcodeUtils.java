package com.cmower.common.util.qrcode;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Binarizer;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class QrcodeUtils {
	private static final int DEFAULT_LENGTH = 400;// 生成二维码的默认边长，因为是正方形的，所以高度和宽度一致
	private static final String FORMAT = "jpg";// 生成二维码的格式

	private static Logger logger = LoggerFactory.getLogger(QrcodeUtils.class);

	/**
	 * 根据内容生成二维码数据
	 * 
	 * @param content
	 *            二维码文字内容[为了信息安全性，一般都要先进行数据加密]
	 * @param length
	 *            二维码图片宽度和高度
	 */
	private static BitMatrix createQrcodeMatrix(String content, int length) {
		Map<EncodeHintType, Object> hints = Maps.newEnumMap(EncodeHintType.class);
		// 设置字符编码
		hints.put(EncodeHintType.CHARACTER_SET, Charsets.UTF_8.name());
		// 指定纠错等级
		hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
		hints.put(EncodeHintType.MARGIN, 1);

		try {
			return new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, length, length, hints);
		} catch (Exception e) {
			logger.warn("内容为：【" + content + "】的二维码生成失败！", e);
			return null;
		}

	}

	/**
	 * 根据指定边长创建生成的二维码
	 * 
	 * @param content
	 *            二维码内容
	 * @param length
	 *            二维码的高度和宽度
	 * @param logoFile
	 *            logo 文件对象，可以为空
	 * @return 二维码图片的字节数组
	 */
	public static byte[] createQrcode(String content, int length, File logoFile) {
		if (logoFile != null && !logoFile.exists()) {
			throw new IllegalArgumentException("请提供正确的logo文件！");
		}

		BitMatrix qrCodeMatrix = createQrcodeMatrix(content, length);
		if (qrCodeMatrix == null) {
			throw new IllegalArgumentException("请提供正确的二维码图片地址");
		}
		try {
			File file = Files.createTempFile("qrcode_" + UUID.randomUUID(), "." + FORMAT).toFile();
			logger.debug(file.getAbsolutePath());

			MatrixToImageWriter.writeToFile(qrCodeMatrix, FORMAT, file);
			if (logoFile != null) {
				// 添加logo图片, 此处一定需要重新进行读取，而不能直接使用二维码的BufferedImage 对象
				BufferedImage img = ImageIO.read(file);
				overlapImage(img, FORMAT, file.getAbsolutePath(), logoFile, new MatrixToLogoImageConfig());
			}

			return toByteArray(file);
		} catch (Exception e) {
			logger.warn("内容为：【" + content + "】的二维码生成失败！", e);
			return null;
		}
	}

	public static byte[] createQrcode(MatrixToBgImageConfig config) {
		try {
			InputStream inputStream = Thread.currentThread().getContextClassLoader()
					.getResourceAsStream(config.getBgFile());
			File bgFile = Files.createTempFile("bg_", ".jpg").toFile();
			FileUtils.copyInputStreamToFile(inputStream, bgFile);

			logger.info("背景图 {}", bgFile);

			if (bgFile != null && !bgFile.exists()) {
				throw new IllegalArgumentException("请提供正确的背景文件！");
			}

			// 头像图
			File headimgFile = null;

			if (StringUtils.isNotEmpty(config.getHeadimgUrl())) {
				logger.info("网络图片{}" + config.getHeadimgUrl());

				CloseableHttpClient httpclient = HttpClientBuilder.create().build();
				HttpGet httpget = new HttpGet(config.getHeadimgUrl());
				httpget.addHeader("Content-Type", "text/html;charset=UTF-8");
				// 配置请求的超时设置
				RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(500)
						.setConnectTimeout(500).setSocketTimeout(500).build();
				httpget.setConfig(requestConfig);

				try (CloseableHttpResponse response = httpclient.execute(httpget);
						InputStream headimgStream = handleResponse(response);) {

					Header[] contentTypeHeader = response.getHeaders("Content-Type");
					if (contentTypeHeader != null && contentTypeHeader.length > 0) {
						if (contentTypeHeader[0].getValue().startsWith(ContentType.APPLICATION_JSON.getMimeType())) {

							// application/json; encoding=utf-8 下载媒体文件出错
							String responseContent = handleUTF8Response(response);

							logger.warn("下载网络头像出错{}", responseContent);
						}
					}

					headimgFile = createTmpFile(headimgStream, "headimg_" + UUID.randomUUID(), "jpg");
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					throw new Exception("头像文件读取有误！", e);
				} finally {
					httpget.releaseConnection();
				}

			} else {
				InputStream headimgStream = Thread.currentThread().getContextClassLoader()
						.getResourceAsStream(MatrixToBgImageConfig.DEFAULT_HEADIMGURL);

				headimgFile = Files.createTempFile("headimg_" + UUID.randomUUID(), ".jpg").toFile();
				FileUtils.copyInputStreamToFile(headimgStream, headimgFile);
			}

			logger.info("头像图 {}", headimgFile);

			if (headimgFile != null && !headimgFile.exists()) {
				throw new IllegalArgumentException("请提供正确的头像文件！");
			}

			BitMatrix qrCodeMatrix = createQrcodeMatrix(config.getQrcode_url(), config.getQrcode_height());
			if (qrCodeMatrix == null) {
				throw new IllegalArgumentException("请提供正确的二维码图片地址");
			}

			File file = Files.createTempFile("qrcode_" + UUID.randomUUID(), "." + FORMAT).toFile();
			logger.debug(file.getAbsolutePath());

			MatrixToImageWriter.writeToFile(qrCodeMatrix, FORMAT, file);
			if (bgFile != null) {
				// 添加背景图片, 此处一定需要重新进行读取，而不能直接使用二维码的BufferedImage 对象
				BufferedImage img = ImageIO.read(file);
				increasingImage(img, FORMAT, file.getAbsolutePath(), bgFile, config, headimgFile);
			}

			return toByteArray(file);
		} catch (Exception e) {
			logger.warn("内容为：【" + config.getQrcode_url() + "】的二维码生成失败！", e);
			return null;
		}
	}

	/**
	 * 创建生成默认高度(400)的二维码图片 可以指定是否贷logo
	 * 
	 * @param content
	 *            二维码内容
	 * @param logoFile
	 *            logo 文件对象，可以为空
	 * @return 二维码图片的字节数组
	 */
	public static byte[] createQrcode(String content, File logoFile) {
		return createQrcode(content, DEFAULT_LENGTH, logoFile);
	}

	/**
	 * 将文件转换为字节数组， 使用MappedByteBuffer，可以在处理大文件时，提升性能
	 * 
	 * @param file
	 *            文件
	 * @return 二维码图片的字节数组
	 */
	private static byte[] toByteArray(File file) {
		try (FileChannel fc = new RandomAccessFile(file, "r").getChannel();) {
			MappedByteBuffer byteBuffer = fc.map(MapMode.READ_ONLY, 0, fc.size()).load();
			byte[] result = new byte[(int) fc.size()];
			if (byteBuffer.remaining() > 0) {
				byteBuffer.get(result, 0, byteBuffer.remaining());
			}
			return result;
		} catch (Exception e) {
			logger.warn("文件转换成byte[]发生异常！", e);
			return null;
		}
	}

	/**
	 * 将logo添加到二维码中间
	 * 
	 * @param image
	 *            生成的二维码图片对象
	 * @param imagePath
	 *            图片保存路径
	 * @param logoFile
	 *            logo文件对象
	 * @param format
	 *            图片格式
	 * @throws Exception
	 */
	private static void overlapImage(BufferedImage image, String format, String imagePath, File logoFile,
			MatrixToLogoImageConfig logoConfig) throws Exception {
		try {
			BufferedImage logo = ImageIO.read(logoFile);
			Graphics2D g = image.createGraphics();
			// 考虑到logo图片贴到二维码中，建议大小不要超过二维码的1/5;
			int width = image.getWidth() / logoConfig.getLogoPart();
			int height = image.getHeight() / logoConfig.getLogoPart();
			// logo起始位置，此目的是为logo居中显示
			int x = (image.getWidth() - width) / 2;
			int y = (image.getHeight() - height) / 2;
			// 绘制图
			g.drawImage(logo, x, y, width, height, null);

			g.dispose();
			// 写入logo图片到二维码
			ImageIO.write(image, format, new File(imagePath));
		} catch (Exception e) {
			throw new Exception("二维码添加logo时发生异常！", e);
		}
	}

	private static void increasingImage(BufferedImage image, String format, String imagePath, File bgFile,
			MatrixToBgImageConfig config, File headimgFile) throws Exception {
		try {
			BufferedImage bg = ImageIO.read(bgFile);

			Graphics2D g = bg.createGraphics();

			// 二维码的高度和宽度如何定义
			int width = config.getQrcode_height();
			int height = config.getQrcode_height();

			// logo起始位置，此目的是为logo居中显示
			int x = config.getQrcode_x();
			int y = config.getQrcode_y();
			// 绘制图
			g.drawImage(image, x, y, width, height, null);

			BufferedImage headimg = ImageIO.read(headimgFile);

			int headimg_width = config.getHeadimg_height();
			int headimg_height = config.getHeadimg_height();

			int headimg_x = config.getHeadimg_x();
			int headimg_y = config.getHeadimg_y();

			// 绘制头像
			g.drawImage(headimg, headimg_x, headimg_y, headimg_width, headimg_height, null);

			// 绘制文字
			g.setColor(Color.GRAY);// 文字颜色
			Font font = new Font("宋体", Font.BOLD, 28);
			g.setFont(font);

			g.drawString(config.getRealname(), config.getRealname_x(), config.getRealname_y());

			g.dispose();
			// 写入二维码到bg图片
			ImageIO.write(bg, format, new File(imagePath));
		} catch (Exception e) {
			throw new Exception("二维码添加bg时发生异常！", e);
		}
	}

	/**
	 * 解析二维码
	 * 
	 * @param file
	 *            二维码文件内容
	 * @return 二维码的内容
	 */
	public static String decodeQrcode(File file) throws IOException, NotFoundException {
		BufferedImage image = ImageIO.read(file);
		LuminanceSource source = new BufferedImageLuminanceSource(image);
		Binarizer binarizer = new HybridBinarizer(source);
		BinaryBitmap binaryBitmap = new BinaryBitmap(binarizer);
		Map<DecodeHintType, Object> hints = Maps.newEnumMap(DecodeHintType.class);
		hints.put(DecodeHintType.CHARACTER_SET, Charsets.UTF_8.name());
		return new MultiFormatReader().decode(binaryBitmap, hints).getText();
	}

	public static InputStream handleResponse(final HttpResponse response) throws IOException {
		final StatusLine statusLine = response.getStatusLine();
		final HttpEntity entity = response.getEntity();
		if (statusLine.getStatusCode() >= 300) {
			EntityUtils.consume(entity);
			throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
		}
		return entity == null ? null : entity.getContent();
	}

	public static String handleUTF8Response(final HttpResponse response) throws IOException {
		final StatusLine statusLine = response.getStatusLine();
		final HttpEntity entity = response.getEntity();
		if (statusLine.getStatusCode() >= 300) {
			EntityUtils.consume(entity);
			throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
		}
		return entity == null ? null : EntityUtils.toString(entity, Consts.UTF_8);
	}

	public static File createTmpFile(InputStream inputStream, String name, String ext) throws IOException {
		File tmpFile = File.createTempFile(name, '.' + ext);

		tmpFile.deleteOnExit();

		try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
			int read = 0;
			byte[] bytes = new byte[1024 * 100];
			while ((read = inputStream.read(bytes)) != -1) {
				fos.write(bytes, 0, read);
			}

			fos.flush();
			return tmpFile;
		}
	}

}
