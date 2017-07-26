package test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import com.cmower.common.util.qrcode.MatrixToBgImageConfig;
import com.cmower.common.util.qrcode.QrcodeUtils;

public class QrcodeUtilsTest {

	@Test
	public void testCreateQrcodeMatrixToBgImageConfig() throws IOException {
		MatrixToBgImageConfig config = new MatrixToBgImageConfig();

		config.setHeadimgUrl("https://avatars2.githubusercontent.com/u/6011374?v=4&u=7672049c1213f7663b79583d727e95ee739010ec&s=400");

		config.setQrcode_url("http://blog.csdn.net/qing_gee");

		config.setRealname("沉默王二");

		byte[] bytes = QrcodeUtils.createQrcode(config);
		Path path = Files.createTempFile("qrcode_with_bg_", ".jpg");
		Files.write(path, bytes);
	}

}
